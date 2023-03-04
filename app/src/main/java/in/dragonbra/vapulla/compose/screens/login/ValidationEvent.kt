package `in`.dragonbra.vapulla.compose.screens.login

sealed class ValidationEvent {
    object StartService : ValidationEvent()
    object BindService : ValidationEvent()
}
