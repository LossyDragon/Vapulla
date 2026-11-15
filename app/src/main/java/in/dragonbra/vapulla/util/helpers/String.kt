package `in`.dragonbra.vapulla.util.helpers

import java.util.Locale

/** The text with its first letter in upper case, for showing names Steam gives in lower case. */
fun String.capitalize(): String = this.replaceFirstChar {
    if (it.isLowerCase()) {
        it.titlecase(Locale.getDefault())
    } else {
        it.toString()
    }
}
