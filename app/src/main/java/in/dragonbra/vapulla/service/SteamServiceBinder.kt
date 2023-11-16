package `in`.dragonbra.vapulla.service

import android.os.Binder

class SteamServiceBinder(val service: SteamService) : Binder()
