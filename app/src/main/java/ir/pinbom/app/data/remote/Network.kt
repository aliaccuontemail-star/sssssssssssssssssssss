package ir.pinbom.app.data.remote

import android.util.Log
import ir.pinbom.app.BuildConfig
import ir.pinbom.app.data.SessionManager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/* Sends every call to the server the user picked, whichever address that is.
 *
 * Retrofit needs *a* base URL at build time, and the app cannot know the server
 * then — setup happens on first launch. So the calls are addressed to a
 * placeholder and this interceptor re-addresses each one for real: scheme, host,
 * port and (if the user's address carries one) path prefix, with the request's own
 * path appended. Nothing about the API layer changes; only the destination.
 *
 * Debug builds log the resolved URL and the status for every call under
 * `PinBomNet`, which is what turns "the request never arrived" into a single
 * readable line. */
class HostRewriteInterceptor @Inject constructor(private val session:SessionManager):Interceptor{
    override fun intercept(chain:Interceptor.Chain):Response{
        val req=chain.request()
        val base=session.snapshotServer()
        if(base.isBlank()){
            if(BuildConfig.DEBUG)Log.d(TAG,"${req.method} ${req.url.encodedPath} — no server set yet, using the placeholder host")
            return chain.proceed(req)
        }
        val url=resolveServerUrl(base,req.url)
        if(url==null){
            if(BuildConfig.DEBUG)Log.d(TAG,"${req.method} ${req.url.encodedPath} — server host is unusable")
            return chain.proceed(req)
        }

        val response=chain.proceed(req.newBuilder().url(url).build())
        if(BuildConfig.DEBUG)Log.d(TAG,"${req.method} ${url.encodedPath} → ${response.code}")
        return response
    }
    private companion object{const val TAG="PinBomNet"}
}

class AuthInterceptor @Inject constructor(private val session:SessionManager):Interceptor{override fun intercept(chain:Interceptor.Chain):Response{val t=session.accessToken?:return chain.proceed(chain.request());return chain.proceed(chain.request().newBuilder().header("Authorization","Bearer $t").build())}}

class RefreshAuthenticator @Inject constructor(private val session:SessionManager):Authenticator{
    private val json=Json{ignoreUnknownKeys=true;coerceInputValues=true}
    override fun authenticate(route:Route?,response:Response):Request?{
        if(responseCount(response)>=2)return null
        val refresh=session.refreshToken ?: return null
        val base=session.snapshotServer().trimEnd('/')
        if(base.isBlank())return null
        return try{
            val body=json.encodeToString(mapOf("refresh_token" to refresh)).toRequestBody("application/json".toMediaType())
            val request=Request.Builder().url("$base/v1/auth/refresh").post(body).build()
            OkHttpClient.Builder().connectTimeout(10,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).build().newCall(request).execute().use{r->
                if(!r.isSuccessful)return null
                val pair=json.decodeFromString<TokenPair>(r.body?.string() ?: return null)
                runBlocking{session.setTokens(pair.accessToken,pair.refreshToken)}
                response.request.newBuilder().header("Authorization","Bearer ${pair.accessToken}").build()
            }
        }catch(_:Exception){null}
    }
    private fun responseCount(response:Response):Int{var count=1;var prior=response.priorResponse;while(prior!=null){count++;prior=prior.priorResponse};return count}
}

/* Two Retrofit instances, one ApiService. [api] is the everyday one: half a minute
 * of patience for a JSON round trip. [uploadApi] exists because a pin upload is the
 * only call in the app expected to push megabytes through a phone's uplink — a 4 MB
 * photo on a slow connection needs more than the default read/write windows, and
 * making every other call wait that long would only hide a stalled request.
 *
 * Both share the same interceptors and the same RefreshAuthenticator, so the server
 * override, the bearer token and the 401 → refresh → retry path behave identically
 * whichever instance a call came through. The retry replays the request body, which
 * is why the upload body is file-backed rather than a one-shot stream (Repository.upload). */
@Singleton class ApiFactory @Inject constructor(private val session:SessionManager,private val host:HostRewriteInterceptor,private val auth:AuthInterceptor,private val refresher:RefreshAuthenticator){
    private val json=Json{ignoreUnknownKeys=true;coerceInputValues=true}

    private fun client(connectSeconds:Long,readSeconds:Long,writeSeconds:Long):OkHttpClient=OkHttpClient.Builder()
        .addInterceptor(host)
        .addInterceptor(auth)
        .authenticator(refresher)
        .connectTimeout(connectSeconds,TimeUnit.SECONDS)
        .readTimeout(readSeconds,TimeUnit.SECONDS)
        .writeTimeout(writeSeconds,TimeUnit.SECONDS)
        .build()

    private fun retrofit(client:OkHttpClient):ApiService=Retrofit.Builder()
        .baseUrl("http://127.0.0.1/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ApiService::class.java)

    val api:ApiService by lazy{retrofit(client(connectSeconds=10,readSeconds=30,writeSeconds=60))}

    /** createPin only: two minutes each way, so a big photo is not cut off. */
    val uploadApi:ApiService by lazy{retrofit(client(connectSeconds=30,readSeconds=120,writeSeconds=120))}
}
