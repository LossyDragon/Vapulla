package `in`.dragonbra.vapulla.core

import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.Friend

/**
 * Util functions for [Friend]
 */

fun EFriendRelationship.isFriend() = this == EFriendRelationship.Friend
fun EFriendRelationship.isRequest() = this == EFriendRelationship.RequestRecipient
