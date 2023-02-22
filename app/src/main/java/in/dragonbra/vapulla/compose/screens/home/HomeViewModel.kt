package `in`.dragonbra.vapulla.compose.screens.home

import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class HomeState(
    val isDrawerOpen: DrawerValue = DrawerValue.Closed
)

sealed class HomeEvent {
    // data class DrawerState(val value: DrawerValue) : HomeEvent()
}

class HomeViewModel : ViewModel() {

    var homeState by mutableStateOf(HomeState())

    fun onEvent(event: HomeEvent) {
        when (event) {
            else -> Unit
            // is HomeEvent.DrawerState ->
            //     homeState = homeState.copy(isDrawerOpen = event.value)
        }
    }
}