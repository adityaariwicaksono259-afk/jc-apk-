package app.javin.codex.data.repository

import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.model.*
import app.javin.codex.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ============================================
// RESULT wrapper
// ============================================
sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val code: String? = null) : AuthResult<Nothing>()
}

class AuthRepository(private val session: SessionManager) {

    private val api = ApiClient.get()

    // ============================================
    // REGISTER — daftar akun baru
    // ============================================
    suspend fun register(
        email: String,
        password: String,
        name: String?
    ): AuthResult<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = api.register(RegisterRequest(email, password, name))
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                if (body.ok) AuthResult.Success(body)
                else AuthResult.Error(body.message ?: "Gagal daftar", body.code)
            } else {
                val errBody = resp.errorBody()?.string()
                AuthResult.Error(errBody ?: "HTTP ${resp.code()}")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    // ============================================
    // LOGIN PASSWORD — verify + kirim OTP
    // ============================================
    suspend fun loginPassword(
        email: String,
        password: String
    ): AuthResult<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = api.loginPassword(LoginPasswordRequest(email, password))
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                if (body.ok) AuthResult.Success(body)
                else AuthResult.Error(body.message ?: "Gagal login", body.code)
            } else {
                // 401 = password salah, dapat body dengan code WRONG_PASSWORD
                if (resp.code() == 401) {
                    val errBody = resp.errorBody()?.string()
                    // Parse error JSON
                    try {
                        val code = Regex("\"code\"\\s*:\\s*\"([^\"]+)\"").find(errBody ?: "")?.groupValues?.get(1)
                        val msg = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(errBody ?: "")?.groupValues?.get(1)
                        AuthResult.Error(msg ?: "Password salah", code)
                    } catch (e: Exception) {
                        AuthResult.Error("Password salah", "WRONG_PASSWORD")
                    }
                } else {
                    val errBody = resp.errorBody()?.string()
                    AuthResult.Error(errBody ?: "HTTP ${resp.code()}")
                }
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    // ============================================
    // VERIFY OTP — return session token + simpan
    // ============================================
    suspend fun verifyOtp(
        email: String,
        code: String
    ): AuthResult<User> = withContext(Dispatchers.IO) {
        try {
            val resp = api.verifyOtp(VerifyOtpRequest(email, code))
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                if (body.ok && body.user != null && body.sessionToken != null) {
                    // Simpan session ke DataStore
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
                val msg = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(errBody ?: "")?.groupValues?.get(1)
                AuthResult.Error(msg ?: "OTP salah")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    // ============================================
    // RESET PASSWORD — kirim link reset
    // ============================================
    suspend fun resetPassword(email: String): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            val resp = api.resetPassword(ResetPasswordRequest(email))
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                if (body.ok) AuthResult.Success(body.message ?: "Link reset terkirim")
                else AuthResult.Error(body.message ?: "Gagal kirim link")
            } else {
                AuthResult.Error("HTTP ${resp.code()}")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    // ============================================
    // RESET CONFIRM — set password baru
    // ============================================
    suspend fun resetConfirm(token: String, newPassword: String): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            val resp = api.resetConfirm(ResetConfirmRequest(newPassword, token))
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                if (body.ok) AuthResult.Success(body.message ?: "Password berhasil direset")
                else AuthResult.Error(body.message ?: "Gagal reset password")
            } else {
                AuthResult.Error("HTTP ${resp.code()}")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    // ============================================
    // CHECK LOGIN — cek session valid
    // ============================================
    suspend fun checkLogin(): AuthResult<User> = withContext(Dispatchers.IO) {
        try {
            val token = session.getToken()
            if (token.isNullOrBlank()) return@withContext AuthResult.Error("Belum login", "NO_SESSION")

            val resp = api.me()
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                if (body.ok && body.loggedIn && body.user != null) {
                    AuthResult.Success(body.user)
                } else {
                    AuthResult.Error("Session expired", "SESSION_EXPIRED")
                }
            } else {
                AuthResult.Error("HTTP ${resp.code()}")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    // ============================================
    // LOGOUT
    // ============================================
    suspend fun logout(): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            try { api.logout() } catch (e: Exception) { /* ignore server error */ }
            session.clearSession()
            AuthResult.Success("Logout berhasil")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Logout error")
        }
    }

    // ============================================
    // IS LOGGED IN
    // ============================================
    suspend fun isLoggedIn(): Boolean = session.isLoggedIn()
}
