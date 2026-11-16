package `in`.dragonbra.vapulla.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the device is on a network that reaches the internet, as Android reports it.
 * Lets the sign-in screen tell "no internet" apart from "Steam cannot be reached", and try
 * again by itself when the network is back.
 *
 * An app-wide singleton: its callback stays registered for the life of the process.
 */
class NetworkMonitor(context: Context) {

    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    private val _isOnline = MutableStateFlow(
        connectivity.getNetworkCapabilities(connectivity.activeNetwork).reachesInternet(),
    )
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        connectivity.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities,
                ) {
                    _isOnline.value = capabilities.reachesInternet()
                }

                override fun onLost(network: Network) {
                    _isOnline.value = false
                }
            },
        )
    }

    /** Validated means Android has checked that the network really gets out. */
    private fun NetworkCapabilities?.reachesInternet(): Boolean = this != null &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
