package app.javin.codex.data.api

import app.javin.codex.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ============================================
    // AUTH — REGISTER
    // ============================================
    @POST("api/auth/password/register")
    suspend fun register(
        @Body body: RegisterRequest
    ): Response<AuthResponse>

    // ============================================
    // AUTH — LOGIN (verify password, kirim OTP)
    // ============================================
    @POST("api/auth/password/login")
    suspend fun loginPassword(
        @Body body: LoginPasswordRequest
    ): Response<AuthResponse>

    // ============================================
    // AUTH — VERIFY OTP → return session token
    // ============================================
    @POST("api/auth/email/verify-code")
    suspend fun verifyOtp(
        @Body body: VerifyOtpRequest
    ): Response<VerifyResponse>

    // ============================================
    // AUTH — RESET PASSWORD (kirim link)
    // ============================================
    @POST("api/auth/password/reset")
    suspend fun resetPassword(
        @Body body: ResetPasswordRequest
    ): Response<SimpleResponse>

    // ============================================
    // AUTH — RESET CONFIRM (set password baru)
    // ============================================
    @POST("api/auth/password/reset-confirm")
    suspend fun resetConfirm(
        @Body body: ResetConfirmRequest
    ): Response<SimpleResponse>

    // ============================================
    // AUTH — FORGOT PASSWORD
    // ============================================
    @POST("api/auth/password/forgot-request")
    suspend fun forgotRequest(
        @Body body: ForgotRequest
    ): Response<AuthResponse>

    @POST("api/auth/password/forgot-confirm")
    suspend fun forgotConfirm(
        @Body body: ForgotConfirmRequest
    ): Response<SimpleResponse>

    // ============================================
    // AUTH — ME (cek login status)
    // ============================================
    @GET("api/auth/me")
    suspend fun me(): Response<MeResponse>

    // ============================================
    // AUTH — LOGOUT
    // ============================================
    @POST("api/auth/logout")
    suspend fun logout(): Response<SimpleResponse>

    // ============================================
    // USER — PROFILE + PRIVACY
    // ============================================
    @POST("api/user/update-profile")
    suspend fun updateProfile(
        @Body body: UpdateProfileRequest
    ): Response<UpdateProfileResponse>

    @GET("api/user/privacy")
    suspend fun getPrivacy(): Response<PrivacyResponse>

    @POST("api/user/privacy")
    suspend fun updatePrivacy(
        @Body body: PrivacyUpdateRequest
    ): Response<PrivacyResponse>

    // ============================================
    // PROXY — CALL TOOL (proxy-ext)
    // ============================================
    @GET("api/proxy-ext")
    suspend fun callTool(
        @Query("id") catalogId: String,
        @Query("q") query: String
    ): Response<okhttp3.ResponseBody>

    @GET("api/proxy-ext")
    suspend fun callToolDynamic(
        @Query("id") catalogId: String,
        @QueryMap params: Map<String, String>
    ): Response<okhttp3.ResponseBody>

    // ============================================
    // TOOLS — AM (custom endpoints)
    // ============================================
    @GET("api/amfinder")
    suspend fun amFinder(@Query("url") tiktokUrl: String): Response<okhttp3.ResponseBody>

    @GET("api/amsearch")
    suspend fun amSearch(@Query("q") query: String): Response<okhttp3.ResponseBody>

    @POST("api/premium/amprem")
    suspend fun amPrem(@Body body: okhttp3.RequestBody): Response<okhttp3.ResponseBody>

    // ============================================
    // APP — VERSION CHECK (auto-update)
    // ============================================
    @GET("api/app/version")
    suspend fun getAppVersion(): Response<app.javin.codex.data.model.AppVersion>

    // ============================================
    // ENDPOINTS — LIST (247 tools)
    // ============================================
    @GET("endpoints.json")
    suspend fun getEndpoints(): Response<List<Map<String, Any>>>

    @GET("api/tools/working")
    suspend fun getWorkingTools(): Response<okhttp3.ResponseBody>

    // ============================================
    // CHAT
    // ============================================
    @GET("api/chat/rooms")
    suspend fun getChatRooms(): Response<okhttp3.ResponseBody>

    @POST("api/chat/rooms")
    suspend fun postChatRoom(@Body body: okhttp3.RequestBody): Response<okhttp3.ResponseBody>

    @GET("api/chat/messages")
    suspend fun getChatMessages(
        @Query("room_id") roomId: Long,
        @Query("since") since: Long = 0
    ): Response<okhttp3.ResponseBody>

    @POST("api/chat/messages")
    suspend fun postChatMessage(@Body body: okhttp3.RequestBody): Response<okhttp3.ResponseBody>

    @GET("api/chat/contacts")
    suspend fun getChatContacts(): Response<okhttp3.ResponseBody>

    @POST("api/chat/typing")
    suspend fun postChatTyping(@Body body: okhttp3.RequestBody): Response<okhttp3.ResponseBody>

    @GET("api/chat/typing")
    suspend fun getChatTyping(@Query("room_id") roomId: Long): Response<okhttp3.ResponseBody>

    @POST("api/chat/block")
    suspend fun postChatBlock(@Body body: okhttp3.RequestBody): Response<okhttp3.ResponseBody>

    @POST("api/chat/heartbeat")
    suspend fun postChatHeartbeat(): Response<okhttp3.ResponseBody>
}
