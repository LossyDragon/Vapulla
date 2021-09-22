package `in`.dragonbra.vapulla.extension

import androidx.appcompat.widget.SearchView

fun SearchView.setOnQueryTextListener(
    onQueryTextSubmit: ((query: String?) -> Boolean)? = null,
    onQueryTextChange: ((newText: String?) -> Boolean)? = null
): SearchView.OnQueryTextListener {
    val listener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            return onQueryTextSubmit?.invoke(query) ?: false
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            return onQueryTextChange?.invoke(newText) ?: false
        }
    }

    setOnQueryTextListener(listener)
    return listener
}
