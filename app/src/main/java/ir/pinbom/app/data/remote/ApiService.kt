package ir.pinbom.app.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {
    @POST("v1/auth/register") suspend fun register(@Body body:RegisterRequest):TokenPair
    @POST("v1/auth/login") suspend fun login(@Body body:LoginRequest):TokenPair
    @POST("v1/auth/refresh") suspend fun refresh(@Body body:Map<String,String>):TokenPair
    @POST("v1/auth/logout") suspend fun logout(@Body body:Map<String,String>):SimpleResult
    @POST("v1/auth/otp/request") suspend fun requestOtp(@Body body:OtpRequest):OtpResponse
    @DELETE("v1/users/me") suspend fun deleteMe():SimpleResult
    @POST("v1/auth/otp/verify") suspend fun verifyOtp(@Body body:OtpVerifyRequest):TokenPair
    @GET("v1/users/{username}") suspend fun user(@Path("username") username:String):UserProfile
    @PATCH("v1/users/me") suspend fun patchMe(@Body body:Map<String,String>):SimpleResult
    @Multipart @POST("v1/users/me/{kind}") suspend fun uploadProfileMedia(@Path("kind") kind:String,@Part file:MultipartBody.Part):SimpleResult
    @GET("v1/users/{username}/boards") suspend fun userBoards(@Path("username") username:String):BoardPage
    @GET("v1/users/{username}/pins") suspend fun userPins(@Path("username") username:String):FeedPage
    @GET("v1/users/{username}/saved") suspend fun savedPins(@Path("username") username:String):FeedPage
    @POST("v1/users/{id}/follow") suspend fun follow(@Path("id") id:Long):SimpleResult
    @DELETE("v1/users/{id}/follow") suspend fun unfollow(@Path("id") id:Long):SimpleResult
    @GET("v1/pins/{id}") suspend fun pin(@Path("id") id:Long):Pin
    @GET("v1/boards/{id}") suspend fun board(@Path("id") id:Long):BoardDetail
    @PATCH("v1/boards/{id}") suspend fun patchBoard(@Path("id") id:Long,@Body body:CreateBoard):SimpleResult
    @POST("v1/boards") suspend fun createBoard(@Body body:CreateBoard):Board
    @DELETE("v1/boards/{id}") suspend fun deleteBoard(@Path("id") id:Long):SimpleResult
    @POST("v1/pins/{id}/save") suspend fun save(@Path("id") id:Long,@Body body:SaveRequest=SaveRequest()):SimpleResult
    @DELETE("v1/pins/{id}/save") suspend fun unsave(@Path("id") id:Long):SimpleResult
    @POST("v1/pins/{id}/like") suspend fun like(@Path("id") id:Long):SimpleResult
    @DELETE("v1/pins/{id}/like") suspend fun unlike(@Path("id") id:Long):SimpleResult
    @GET("v1/pins/{id}/comments") suspend fun comments(@Path("id") id:Long):CommentPage
    @POST("v1/pins/{id}/comments") suspend fun addComment(@Path("id") id:Long,@Body body:CommentRequest):SimpleResult
    @Multipart
    @POST("v1/pins") suspend fun createPin(@Part file:MultipartBody.Part,@Part("title") title:RequestBody,@Part("description") description:RequestBody,@Part("source_url") source:RequestBody,@Part("tags") tags:RequestBody,@Part("board_id") boardId:RequestBody):CreatePinResponse
    @GET("v1/feed/home") suspend fun home(@Query("cursor") cursor:String=""):FeedPage
    @GET("v1/feed/following") suspend fun following(@Query("cursor") cursor:String=""):FeedPage
    @GET("v1/feed/trending") suspend fun trending(@Query("cursor") cursor:String=""):FeedPage
    @GET("v1/feed/related/{id}") suspend fun related(@Path("id") id:Long):FeedPage
    @GET("v1/search") suspend fun search(@Query("q") q:String,@Query("type") type:String="pins",@Query("cursor") cursor:String=""):SearchPage
    @GET("v1/search") suspend fun searchUsers(@Query("q") q:String,@Query("type") type:String="users",@Query("cursor") cursor:String=""):UserSearchPage
    @GET("v1/search") suspend fun searchBoards(@Query("q") q:String,@Query("type") type:String="boards",@Query("cursor") cursor:String=""):BoardSearchPage
    @GET("v1/tags/{tag}/pins") suspend fun tagPins(@Path("tag") tag:String):SearchPage
    @GET("v1/notifications") suspend fun notifications():NotificationPage
    @POST("v1/notifications/read") suspend fun markNotificationsRead(@Body body:Map<String,List<Long>>):SimpleResult
}
