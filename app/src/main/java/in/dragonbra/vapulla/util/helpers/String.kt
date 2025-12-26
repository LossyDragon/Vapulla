package `in`.dragonbra.vapulla.util.helpers

import java.util.Locale

fun String.capitalize(): String = this.replaceFirstChar {
    if (it.isLowerCase()) {
        it.titlecase(Locale.getDefault())
    } else {
        it.toString()
    }
}
