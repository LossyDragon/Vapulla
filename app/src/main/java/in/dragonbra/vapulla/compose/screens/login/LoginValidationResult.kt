package `in`.dragonbra.vapulla.compose.screens.login

data class LoginValidationResult(
    val isSuccessful: Boolean,
    val errorMessage: String = ""
)
