package ir.pinbom.app.data

import androidx.room.Room
import ir.pinbom.app.data.local.LocalDb
import ir.pinbom.app.data.local.PinEntity
import ir.pinbom.app.data.remote.*
import ir.pinbom.app.work.ImagePreparer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class PinBomRepository @Inject constructor(private val apiFactory:ApiFactory,private val session:SessionManager,private val db:LocalDb){val api get()=apiFactory.api
    suspend fun login(id:String,p:String){val t=api.login(LoginRequest(id,p));session.setTokens(t.accessToken,t.refreshToken)}
    suspend fun register(u:String,email:String,p:String){val t=api.register(RegisterRequest(u,email.ifBlank{null},null,p,u));session.setTokens(t.accessToken,t.refreshToken)}
    suspend fun requestOtp(id:String)=api.requestOtp(OtpRequest(id))
    suspend fun verifyOtp(id:String,code:String){val t=api.verifyOtp(OtpVerifyRequest(id,code));session.setTokens(t.accessToken,t.refreshToken)}
    suspend fun pin(id:Long)=api.pin(id)
    suspend fun feed(kind:String,cursor:String="")=when(kind){"following"->api.following(cursor);"trending"->api.trending(cursor);else->api.home(cursor)}
    suspend fun cache(page:FeedPage){db.pinDao().upsertAll(page.items.map{PinEntity(it.id,it.title,it.description,it.image_url,it.width,it.height,it.author?.display_name.orEmpty(),it.created_at)})}
    fun cachedPins():Flow<List<PinEntity>> = db.pinDao().observe()
    suspend fun search(q:String,cursor:String="")=api.search(q,"pins",cursor)
    suspend fun searchUsers(q:String,cursor:String="")=api.searchUsers(q,"users",cursor)
    suspend fun searchBoards(q:String,cursor:String="")=api.searchBoards(q,"boards",cursor)
    suspend fun related(id:Long)=api.related(id)
    suspend fun save(id:Long,board:Long?)=api.save(id,SaveRequest(board))
    suspend fun unsave(id:Long)=api.unsave(id)
    suspend fun like(id:Long)=api.like(id)
    suspend fun unlike(id:Long)=api.unlike(id)
    suspend fun comment(id:Long,body:String)=api.addComment(id,CommentRequest(body))
    suspend fun comments(id:Long)=api.comments(id)
    suspend fun notifications()=api.notifications()
    suspend fun readNotifications(){api.markNotificationsRead(mapOf("ids" to emptyList()))}
    suspend fun deleteMe(){api.deleteMe();session.clear()}
    suspend fun logout(){session.refreshToken?.let{runCatching{api.logout(mapOf("refresh_token" to it))}};session.clear()}
    suspend fun profile(u:String)=api.user(u)
    suspend fun boards(u:String)=api.userBoards(u)
    suspend fun userPins(u:String)=api.userPins(u)
    suspend fun savedPins(u:String)=api.savedPins(u)
    suspend fun patchMe(name:String,bio:String)=api.patchMe(mapOf("display_name" to name,"bio" to bio))
    suspend fun uploadProfileMedia(file:File,kind:String){
        withContext(Dispatchers.IO){
            check(kind=="avatar"||kind=="cover"){"Unknown profile image type"}
            check(file.isFile&&file.length()>0L){"The prepared profile image is missing or empty"}
            check(file.length() <= ImagePreparer.MAX_UPLOAD_BYTES) { "The image exceeds the 10 MiB upload limit" }
            val body=ProgressRequestBody(file,"image/jpeg".toMediaType()){_,_->}
            val part=MultipartBody.Part.createFormData("file",file.name,body)
            apiFactory.uploadApi.uploadProfileMedia(kind,part)
        }
    }
    suspend fun follow(id:Long)=api.follow(id)
    suspend fun unfollow(id:Long)=api.unfollow(id)
    suspend fun createBoard(title:String,desc:String)=api.createBoard(CreateBoard(title,desc))
    suspend fun board(id:Long)=api.board(id)
    suspend fun patchBoard(id:Long,title:String,desc:String)=api.patchBoard(id,CreateBoard(title,desc))
    suspend fun deleteBoard(id:Long)=api.deleteBoard(id)
    /* The upload reports 0..100 through [onProgress] (default no-op, so existing callers
     * keep compiling). The transfer itself is capped at 99: the last stretch is the
     * server accepting and processing the file, and it must not look finished before
     * the response is in. 100 is only sent after createPin returns.
     *
     * The body is the *file* the screen already prepared (see ImagePreparer), not a
     * byte array: OkHttp streams it from disk, reads its length off the file so the
     * multipart part carries a real Content-Length, and can replay it unchanged when
     * a 401 sends the Authenticator off to refresh the token and try again. A whole
     * file read into memory could do none of that — which is why a large camera photo
     * used to fail. The call also goes through its own Retrofit instance
     * (see ApiFactory.uploadApi), the only one with room for a slow transfer. */
    suspend fun upload(file:File,title:String,desc:String,tags:String,boardId:String,onProgress:(Int)->Unit={}){
        withContext(Dispatchers.IO){
            /* A missing or empty file can never become a pin; failing here, with a plain
             * IllegalStateException, is final (the worker does not retry it), whereas
             * sending an empty body would only come back as a 400 five times over. */
            check(file.isFile&&file.length()>0L){"The prepared image is missing or empty: ${file.name}"}
            check(file.length() <= ImagePreparer.MAX_UPLOAD_BYTES) { "The image exceeds the 10 MiB upload limit" }

            val jpeg="image/jpeg".toMediaType()
            val text="text/plain; charset=utf-8".toMediaType()
            val counted=ProgressRequestBody(file,jpeg){written,total->onProgress(if(total>0L)((written*100L)/total).toInt().coerceIn(0,99) else 0)}
            val part=MultipartBody.Part.createFormData("file",file.name,counted)
            apiFactory.uploadApi.createPin(
                part,
                title.toRequestBody(text),
                desc.toRequestBody(text),
                "".toRequestBody(text),
                tags.toRequestBody(text),
                boardId.toRequestBody(text),
            )
        }
        onProgress(100)
    }
}
