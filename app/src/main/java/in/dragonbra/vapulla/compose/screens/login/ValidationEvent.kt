package `in`.dragonbra.vapulla.compose.screens.login

sealed class ValidationEvent {
    object BindService : ValidationEvent()
    object CancelService : ValidationEvent()
    object StartService : ValidationEvent()
}
