package `in`.dragonbra.vapulla.service

import android.os.Binder

class SteamServiceBinder(private val steamService: SteamService) : Binder() {
    fun getService(): SteamService = steamService
}
