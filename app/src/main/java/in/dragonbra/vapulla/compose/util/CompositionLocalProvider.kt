package `in`.dragonbra.vapulla.compose.util

import androidx.activity.ComponentActivity
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The CompositionLocal containing the current [ComponentActivity].
 */
val LocalActivity = staticCompositionLocalOf<ComponentActivity> {
    noLocalProvidedFor("LocalActivity")
}

@Suppress("SameParameterValue")
private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}
