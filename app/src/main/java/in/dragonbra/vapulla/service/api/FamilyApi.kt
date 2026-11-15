package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFamilygroupsSteamclient.CFamilyGroups_GetFamilyGroupForUser_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFamilygroupsSteamclient.CFamilyGroups_GetSharedLibraryApps_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFamilygroupsSteamclient.EFamilyGroupRole
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFamilygroupsSteamclient.ESharedLibraryExcludeReason
import `in`.dragonbra.javasteam.rpc.service.FamilyGroups
import `in`.dragonbra.javasteam.types.SteamID
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber

/**
 * The account's Steam Family, via the `FamilyGroups` unified service: who is in it and which
 * games it shares. Only the calls that read are used. The service can also change a family
 * (invite, remove, leave, delete), and those carry cooldowns of up to a year, so they are
 * left out on purpose.
 */
class FamilyApi(
    private val familyGroups: FamilyGroups,
    private val profile: ProfileApi,
    private val localSteamId: () -> SteamID?,
) {

    /** The family the account is in, or null when it is in none or Steam does not answer. */
    suspend fun family(): Family? {
        val steamId = localSteamId() ?: return null
        val request = CFamilyGroups_GetFamilyGroupForUser_Request.newBuilder()
            .setSteamid(steamId.convertToUInt64())
            .setIncludeFamilyGroupResponse(true)
            .build()

        val response = familyGroups.getFamilyGroupForUser(request).await()
        if (response.result != EResult.OK) {
            Timber.w("GetFamilyGroupForUser failed: ${response.result}")
            return null
        }

        val body = response.body
        if (body.isNotMemberOfAnyGroup || body.familyGroupid == 0L) return null

        val group = body.familyGroup
        val names = profile.names(group.membersList.map { it.steamid })

        return Family(
            id = body.familyGroupid,
            name = group.name,
            freeSpots = group.freeSpots,
            members = group.membersList
                .map { member ->
                    FamilyMember(
                        steamId = member.steamid,
                        name = names[member.steamid].orEmpty(),
                        role = when (member.role) {
                            EFamilyGroupRole.k_EFamilyGroupRole_Adult -> "Adult"
                            EFamilyGroupRole.k_EFamilyGroupRole_Child -> "Child"
                            else -> "Member"
                        },
                        joinedAt = member.timeJoined.toUInt().toLong(),
                        isSelf = member.steamid == steamId.convertToUInt64(),
                    )
                }
                // The account itself first, then by how long they have been in the family.
                .sortedWith(compareByDescending<FamilyMember> { it.isSelf }.thenBy { it.joinedAt })
                .toImmutableList(),
        )
    }

    /**
     * The games the family [familyId] shares with the account, by name. The account's own
     * games are left out, and so are the ones their owner or publisher keeps from sharing.
     * Empty when Steam does not answer.
     */
    suspend fun sharedLibrary(familyId: Long): List<SharedGame> {
        val steamId = localSteamId() ?: return emptyList()
        val request = CFamilyGroups_GetSharedLibraryApps_Request.newBuilder()
            .setFamilyGroupid(familyId)
            .setSteamid(steamId.convertToUInt64())
            .setIncludeOwn(false)
            .setIncludeExcluded(false)
            .setIncludeNonGames(true)
            .setLanguage(LANGUAGE)
            .build()

        val response = familyGroups.getSharedLibraryApps(request).await()
        if (response.result != EResult.OK) {
            Timber.w("GetSharedLibraryApps failed: ${response.result}")
            return emptyList()
        }

        return response.body.appsList
            .filter { it.excludeReason == ESharedLibraryExcludeReason.k_ESharedLibrary_Included }
            .map { app ->
                SharedGame(
                    appId = app.appid,
                    name = app.name,
                    owners = app.ownerSteamidsList.toImmutableList(),
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    private companion object {
        const val LANGUAGE = "english"
    }
}
