package `in`.dragonbra.vapulla.extension

import androidx.preference.Preference

fun Preference.click(l: (preference: Preference) -> Boolean) {
    this.setOnPreferenceClickListener(l)
}