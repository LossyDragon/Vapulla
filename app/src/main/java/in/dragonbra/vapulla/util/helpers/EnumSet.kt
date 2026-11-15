package `in`.dragonbra.vapulla.util.helpers

import java.util.EnumSet

/** An empty [EnumSet] whose enum type is taken from where it is used. */
inline fun <reified E : Enum<E>> emptyEnumSet(): EnumSet<E> = EnumSet.noneOf(E::class.java)
