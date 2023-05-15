package `in`.dragonbra.vapulla.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class InviteTokenItem(
    val inviteToken: String,
    val inviteLimit: Long,
    val inviteDuration: Long,
    val timeCreated: Long,
    val isValid: Boolean
) : Parcelable
