package `in`.dragonbra.vapulla.util.helpers

import java.util.EnumSet

inline fun <reified E : Enum<E>> emptyEnumSet(): EnumSet<E> {
    return EnumSet.noneOf(E::class.java)
}
