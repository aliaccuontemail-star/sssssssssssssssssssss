package ir.pinbom.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.map
import android.util.Base64
import ir.pinbom.app.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val Context.sessionStore by preferencesDataStore("pinbom_session")
@Singleton class SessionManager @Inject constructor(@ApplicationContext private val context:Context){
    @Volatile var accessToken:String?=null; @Volatile var refreshToken:String?=null; @Volatile var baseUrl:String= ""; @Volatile var username:String=""
    suspend fun load(){
        val p=context.sessionStore.data.first()
        accessToken=p[ACCESS]
        refreshToken=p[REFRESH]
        // Server selection is fixed for this build. Migrate older installations that
        // still have a user-entered/local URL so foreground and background requests
        // all target the same host, including uploads and token refreshes.
        baseUrl=BuildConfig.DEFAULT_BASE_URL.trimEnd('/')
        if (p[SERVER] != baseUrl) context.sessionStore.edit { it[SERVER]=baseUrl }
        username=accessToken?.let(::tokenUsername).orEmpty()
    }
    suspend fun setServer(url:String){
        // Kept for compatibility with older call sites; never allow a stale setup
        // screen to redirect API traffic away from the configured production host.
        baseUrl=BuildConfig.DEFAULT_BASE_URL.trimEnd('/')
        context.sessionStore.edit { it[SERVER]=baseUrl }
    }
    suspend fun setTokens(a:String,r:String){accessToken=a;refreshToken=r;username=tokenUsername(a);context.sessionStore.edit{it[ACCESS]=a;it[REFRESH]=r}}
    suspend fun clear(){accessToken=null;refreshToken=null;username="";context.sessionStore.edit{it.remove(ACCESS);it.remove(REFRESH)}}
    private fun tokenUsername(token:String):String=usernameFromToken(token)
    fun snapshotServer():String=baseUrl
    companion object{private val ACCESS=stringPreferencesKey("access_token");private val REFRESH=stringPreferencesKey("refresh_token");private val SERVER=stringPreferencesKey("server_url")}
}

/**
 * The signed-in username, read from the access token's payload.
 *
 * The backend names that claim `usr` (see auth.Claims); the app used to look for
 * `username` only, so the session always came out blank — and with a blank name
 * the profile tab asks for nobody and sits on its loading skeleton for good. Both
 * spellings are accepted. The payload is base64url *without* padding, which the
 * decoder is not guaranteed to tolerate on every Android release, so it is
 * re-padded first.
 */
internal fun usernameFromToken(token:String):String=try{
    val payload=token.split(".")[1]
    val padded=payload+"=".repeat((4-payload.length%4)%4)
    val claims=Json.parseToJsonElement(String(Base64.decode(padded,Base64.URL_SAFE or Base64.NO_WRAP))).jsonObject
    (claims["usr"] ?: claims["username"])?.jsonPrimitive?.content.orEmpty()
}catch(_:Exception){""}
