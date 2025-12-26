package `in`.dragonbra.vapulla.util.helpers

import java.util.EnumSet

inline fun <reified E : Enum<E>> emptyEnumSet(): EnumSet<E> = EnumSet.noneOf(E::class.java)
