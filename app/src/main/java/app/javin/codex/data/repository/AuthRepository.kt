package app.javin.codex.data.repository

import app.javin.codex.data.api.ApiClient
import com.google.gson.Gson
import app.javin.codex.data.model.*
import app.javin.codex.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val code: String? = null) : AuthResult<Nothing>()
}

class AuthRepository(private val session: SessionManager) {

    private val api = ApiClient.get()

    // ============================================
    // REGISTER — daftar akun baru (masih pakai OTP verifikasi)
    // ============================================
    suspend fun register(email: String, password: String, name: String?): AuthResult<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = api.register(RegisterRequest(email, password, name))
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                if (body.ok) {
                    // Kalau mode=login (existing user), auto-save session
                    if (body.mode == "login" && body.sessionToken != null && body.user != null) {
                        session.saveSession(
                            token = body.sessionToken,
                            userCode = body.user.userCode ?: "",
                            userName = body.user.name,
                            userEmail = body.user.email,
                            userAvatar = body.user.avatar
                        )
                    }
                    AuthResult.Success(body)
                } else {
                    AuthResult.Error(body.message ?: "Gagal daftar", body.code)
                }
            } else {
                val errBody = resp.errorBody()?.string()
                AuthResult.Error(parseMsg(errBody) ?: "HTTP ${resp.code()}", parseCode(errBody))
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    // ============================================
    // LOGIN PASSWORD — VERIFY → SESSION LANGSUNG (NO OTP)
    // ============================================
    suspend fun loginPassword(email: String, password: String): AuthResult<User> = withContext(Dispatchers.IO) {
        try {
            val resp = api.loginPassword(LoginPasswordRequest(email, password))

            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                val user = body.user
                val token = body.sessionToken

                if (body.ok && user != null && !token.isNullOrBlank()) {
                    session.saveSession(
                        token = token,
                        userCode = user.userCode ?: "",
                        userName = user.name,
                        userEmail = user.email,
                        userAvatar = user.avatar
                    )
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error(body.message ?: "Login gagal", body.code)
                }
            } else {
                val errBody = resp.errorBody()?.string()
                AuthResult.Error(parseMsg(errBody) ?: "Login gagal", parseCode(errBody))
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    // ============================================
    // VERIFY OTP (buat register — dipakai di RegisterScreen)
    // ============================================
    suspend fun verifyOtp(email: String, code: String): AuthResult<User> = withContext(Dispatchers.IO) {
        try {
            val resp = api.verifyOtp(VerifyOtpRequest(email, code))
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                if (body.ok && body.user != null && body.sessionToken != null) {
                    session.saveSession(
                        token = body.sessionToken,
                        userCode = body.user.userCode ?: "",
                        userName = body.user.name,
                        userEmail = body.user.email,
                        userAvatar = body.user.avatar
                    )
                    AuthResult.Success(body.user)
                } else {
                    AuthResult.Error(body.message ?: "OTP salah")
                }
            } else {
                val errBody = resp.errorBody()?.string()
                AuthResult.Error(parseMsg(errBody) ?: "OTP salah")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    // ============================================
    // RESET PASSWORD
    // ============================================
    suspend fun resetPassword(email: String): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            val resp = api.resetPassword(ResetPasswordRequest(email))
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                if (body.ok) AuthResult.Success(body.message ?: "Link reset terkirim")
                else AuthResult.Error(body.message ?: "Gagal kirim link")
            } else AuthResult.Error("HTTP ${resp.code()}")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun resetConfirm(token: String, newPassword: String): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            val resp = api.resetConfirm(ResetConfirmRequest(newPassword, token))
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                if (body.ok) AuthResult.Success(body.message ?: "Password berhasil direset")
                else AuthResult.Error(body.message ?: "Gagal reset password")
            } else AuthResult.Error("HTTP ${resp.code()}")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    // ============================================
    // FORGOT PASSWORD — STEP 1: verify email + name, kirim OTP
    // ============================================
    suspend fun forgotRequest(email: String, name: String): AuthResult<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = api.forgotRequest(ForgotRequest(email, name))
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                if (body.ok) AuthResult.Success(body)
                else AuthResult.Error(body.message ?: "Gagal kirim OTP", body.code)
            } else {
                val errBody = resp.errorBody()?.string()
                AuthResult.Error(parseMsg(errBody) ?: "Gagal kirim OTP", parseCode(errBody))
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    // ============================================
    // FORGOT PASSWORD — STEP 2: verify OTP + set password baru
    // ============================================
    suspend fun forgotConfirm(email: String, otp: String, newPassword: String): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            val resp = api.forgotConfirm(ForgotConfirmRequest(email, otp, newPassword))
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                if (body.ok) AuthResult.Success(body.message ?: "Password berhasil direset")
                else AuthResult.Error(body.message ?: "Gagal reset password")
            } else {
                val errBody = resp.errorBody()?.string()
                AuthResult.Error(parseMsg(errBody) ?: "Gagal reset password")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun checkLogin(): AuthResult<User> = withContext(Dispatchers.IO) {
        try {
            val token = session.getToken()
            if (token.isNullOrBlank()) return@withContext AuthResult.Error("Belum login", "NO_SESSION")
            val resp = api.me()
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                if (body.ok && body.loggedIn && body.user != null) AuthResult.Success(body.user)
                else AuthResult.Error("Session expired", "SESSION_EXPIRED")
            } else AuthResult.Error("HTTP ${resp.code()}")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun logout(): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            try { api.logout() } catch (e: Exception) {}
            session.clearSession()
            AuthResult.Success("Logout berhasil")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Logout error")
        }
    }

    suspend fun isLoggedIn(): Boolean = session.isLoggedIn()

    // ============================================
    // HELPERS — extract token & user dari response
    // ============================================


    private val gson = Gson()

    private fun parseCode(json: String?): String? {
        if (json.isNullOrBlank()) return null
        return try {
            val map = gson.fromJson(json, Map::class.java)
            map["code"] as? String
        } catch (e: Exception) {
            // Fallback regex
            Regex("\"code\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
        }
    }

    private fun parseMsg(json: String?): String? {
        if (json.isNullOrBlank()) return null
        return try {
            val map = gson.fromJson(json, Map::class.java)
            map["message"] as? String
        } catch (e: Exception) {
            // Fallback regex
            Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
        }
    }
}
