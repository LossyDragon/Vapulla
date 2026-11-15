package `in`.dragonbra.vapulla.util

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Adds [items] to the end of the list this flow holds and drops the oldest ones beyond [max]:
 * the in-memory logs of the Under the Hood screens all grow this way.
 */
fun <T> MutableStateFlow<ImmutableList<T>>.appendCapped(items: List<T>, max: Int) {
    if (items.isEmpty()) return
    update { current -> (current + items).takeLast(max).toImmutableList() }
}

/** Adds one [item], see the list overload. */
fun <T> MutableStateFlow<ImmutableList<T>>.appendCapped(item: T, max: Int) =
    appendCapped(listOf(item), max)
