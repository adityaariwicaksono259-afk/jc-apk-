package app.javin.codex.data.model

import com.google.gson.annotations.SerializedName

// ============================================
// REQUEST MODELS
// ============================================

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String? = null
)

data class LoginPasswordRequest(
    val email: String,
    val password: String
)

data class VerifyOtpRequest(
    val email: String,
    val code: String
)

data class ResetPasswordRequest(
    val email: String
)

data class ResetConfirmRequest(
    @SerializedName("new_password")
    val newPassword: String,
    val token: String
)

data class ForgotRequest(
    val email: String,
    val name: String
)

data class ForgotConfirmRequest(
    val email: String,
    val otp: String,
    @SerializedName("new_password")
    val newPassword: String
)

// ============================================
// RESPONSE MODELS
// ============================================

data class AuthResponse(
    val ok: Boolean,
    val mode: String? = null,
    @SerializedName("user_code")
    val userCode: String? = null,
    val email: String? = null,
    val message: String? = null,
    val code: String? = null,
    @SerializedName("can_reset")
    val canReset: Boolean? = null,
    @SerializedName("expires_in")
    val expiresIn: Int? = null,
    val user: User? = null,
    @SerializedName("session_token")
    val sessionToken: String? = null
)

data class VerifyResponse(
    val ok: Boolean,
    val mode: String? = null,
    val user: User? = null,
    @SerializedName("session_token")
    val sessionToken: String? = null,
    val message: String? = null
)

data class User(
    val id: Int? = null,
    @SerializedName("user_code")
    val userCode: String? = null,
    val name: String? = null,
    val email: String? = null,
    val avatar: String? = null,
    val bio: String? = null,
    @SerializedName("extra_limit")
    val extraLimit: Int? = null,
    val tier: String? = null,
    @SerializedName("tier_expires_at")
    val tierExpiresAt: Long? = null,
    @SerializedName("api_key")
    val apiKey: String? = null,
    val credits: Int? = null,
    val onboarded: Int? = null
)

data class MeResponse(
    val ok: Boolean,
    @SerializedName("logged_in")
    val loggedIn: Boolean = false,
    @SerializedName("is_demo")
    val isDemo: Boolean = false,
    val user: User? = null
)

data class SimpleResponse(
    val ok: Boolean,
    val message: String? = null
)


// ============================================
// USER PROFILE
// ============================================
data class UpdateProfileRequest(
    val name: String? = null,
    val bio: String? = null
)

data class UpdateProfileResponse(
    val ok: Boolean,
    val message: String? = null,
    val user: User? = null
)

data class PrivacySettings(
    @SerializedName("last_seen")
    val lastSeen: String = "everyone",
    @SerializedName("profile_photo")
    val profilePhoto: String = "everyone",
    val status: String = "everyone",
    @SerializedName("read_receipts")
    val readReceipts: Boolean = true
)

data class PrivacyResponse(
    val ok: Boolean,
    val privacy: PrivacySettings? = null,
    val message: String? = null
)

data class PrivacyUpdateRequest(
    @SerializedName("last_seen")
    val lastSeen: String? = null,
    @SerializedName("profile_photo")
    val profilePhoto: String? = null,
    val status: String? = null,
    @SerializedName("read_receipts")
    val readReceipts: Boolean? = null
)
