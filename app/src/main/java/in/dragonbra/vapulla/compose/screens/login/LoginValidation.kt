package `in`.dragonbra.vapulla.compose.screens.login

data class LoginValidationResult(
    val isSuccessful: Boolean,
    val errorMessage: String = ""
)

class LoginValidation {
    fun validateUsername(username: String): LoginValidationResult {
        return if (username.isBlank()) {
            LoginValidationResult(false, "Username is Blank")
        } else {
            LoginValidationResult(true)
        }
    }

    fun validatePassword(password: String): LoginValidationResult {
        return if (password.isBlank()) {
            LoginValidationResult(false, "Password is Blank")
        } else if (password.length < 6) {
            LoginValidationResult(false, "Password is less than 6 characters")
        } else if (!password.any { it.isLetterOrDigit() }) {
            LoginValidationResult(false, "Password requires at least one letter or digit")
        } else {
            LoginValidationResult(true)
        }
    }

    fun validateSteamGuard(code: String): LoginValidationResult {
        return if (code.length < 5) {
            LoginValidationResult(false, "Code must be 5 characters long")
        } else {
            LoginValidationResult(true)
        }
    }
}
