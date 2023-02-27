package `in`.dragonbra.vapulla.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.vapulla.BuildConfig
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.screens.login.LoginActivity
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.threading.executeAsyncTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.Closeable
import java.util.LinkedList

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var steamService: SteamService

    private val subs: MutableList<Closeable?> = LinkedList()

    private lateinit var prefs: SharedPreferences

    private var counter = 0

    private val args: Bundle
        get() = requireArguments()

    private val ctx: Context
        get() = requireContext()

    private val act: Activity
        get() = requireActivity()

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as SteamService.SteamBinder
            steamService = binder.getService()
            subs.add(steamService.subscribe<DisconnectedCallback> { onDisconnected() })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        setupPreferences()
    }

    override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
        if (arguments != null) {
            setPreferencesFromResource(R.xml.pref_general, args.getString("rootKey"))
        } else {
            setPreferencesFromResource(R.xml.pref_general, rootKey)
        }
    }

    override fun onStart() {
        super.onStart()
        activity?.bindService(
            Intent(context, SteamService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        super.onStop()
        activity?.unbindService(connection)
        subs.forEach { it?.close() }
    }

    @SuppressLint("InflateParams", "CheckResult")
    private fun setupPreferences() {
        val accountManager = (activity as SettingsActivity).accountManager

        // addPreferencesFromResource(R.xml.pref_general)

        val changeUserPreference: Preference? = findPreference("pref_change_user")
        changeUserPreference?.summary =
            getString(R.string.prefSummaryChangeUser, accountManager.username)
        changeUserPreference?.click {
            MaterialDialog(ctx).show {
                title(R.string.dialogTitleChangeUser)
                message(R.string.dialogMessageChangeUser)
                positiveButton(R.string.dialogYes) {
                    lifecycleScope.executeAsyncTask(
                        doInBackground = { steamService.disconnect() },
                        onPostExecute = { clearData() }
                    )
                }
                negativeButton(R.string.dialogNo)
            }
            true
        }

        val changeProfileName: Preference? = findPreference("pref_change_profile_name")
        changeProfileName?.summary = accountManager.nickname
        changeProfileName?.click {
            MaterialDialog(ctx).show {
                title(R.string.dialogTitleNickname)
                input(
                    hint = accountManager.nickname,
                    waitForPositiveButton = true,
                    allowEmpty = false
                ) { _, text ->
                    if (text.isEmpty()) {
                        return@input
                    }
                    lifecycleScope.executeAsyncTask {
                        steamService.getHandler<SteamFriends>().setPersonaName(text.toString())
                    }
                    changeProfileName.summary = text
                }
                positiveButton(R.string.dialogSet)
                negativeButton(R.string.dialogCancel)
            }
            true
        }

        // region About
        val prefVersion: Preference? = findPreference("pref_version")
        prefVersion?.summary = BuildConfig.VERSION_NAME

        val prefRateApp: Preference? = findPreference("pref_rate_app")
        prefRateApp?.click {
            try {
                browse("market://details?id=${act.packageName}")
            } catch (e: ActivityNotFoundException) {
                browse("https://play.google.com/store/apps/details?id=${act.packageName}")
            }
            true
        }

        val sourceCode: Preference? = findPreference("pref_source_code")
        sourceCode?.click {
            browse("https://github.com/Longi94/Vapulla")
            true
        }

        val licences: Preference = findPreference("pref_licences")!!
        licences.click {
            browse("https://raw.githubusercontent.com/Longi94/Vapulla/master/third_party.txt")
            true
        }

        // endregion

        val recents: Preference? = findPreference("pref_friends_list_recents")
        SettingsActivity.bindPreferenceSummaryToValue(recents)
    }

    private fun clearData() {
        (activity as SettingsActivity).accountManager.clear()

        CoroutineScope(Dispatchers.IO).launch {
            (activity as SettingsActivity).db.steamFriendDao().delete()
            (activity as SettingsActivity).db.chatMessageDao().delete()
            (activity as SettingsActivity).db.emoticonDao().delete()
        }

        prefs.edit().clear().apply()
        PreferenceManager.setDefaultValues(requireContext(), R.xml.pref_general, false)
    }

    fun onDisconnected() {
        val loginIntent = Intent(
            requireContext(),
            LoginActivity::class.java
        )
        loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(loginIntent)
    }

    private fun browse(url: String) {
        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
        )
    }
}
