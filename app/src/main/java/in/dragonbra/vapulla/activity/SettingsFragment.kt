package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.vapulla.BuildConfig
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.anim.VectorAnimCompat
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.service.ImgurAuthService
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.threading.runOnBackgroundThread
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.widget.ImageView
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.input.input
import java.io.Closeable
import java.util.*

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

        prefs = PreferenceManager.getDefaultSharedPreferences(context)

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

    fun updateImgurPref() {
        val imgurAuthService = (activity as SettingsActivity).imgurAuthService

        val pref: Preference = findPreference("pref_imgur")!!
        if (prefs.contains(ImgurAuthService.KEY_IMGUR_USERNAME)) {
            pref.title = getString(R.string.prefTitleImgurLinked)
            pref.summary = getString(R.string.prefSummaryImgurLinked,
                    imgurAuthService.getUsername()
            )

            pref.setOnPreferenceClickListener {
                imgurAuthService.clear()
                updateImgurPref()
                true
            }
        } else {
            pref.title = getString(R.string.prefTitleImgur)
            pref.summary = null

            pref.setOnPreferenceClickListener {
                browse(imgurAuthService.getAuthUrl())
                true
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun setupPreferences() {
        val accountManager = (activity as SettingsActivity).accountManager

        // addPreferencesFromResource(R.xml.pref_general)

        updateImgurPref()

        val changeUserPreference: Preference? = findPreference("pref_change_user")
        changeUserPreference?.summary =
                getString(R.string.prefSummaryChangeUser, accountManager.username)
        changeUserPreference?.click {
            MaterialDialog(ctx).show {
                title(R.string.dialogTitleChangeUser)
                message(R.string.dialogMessageChangeUser)
                positiveButton(R.string.dialogYes) {
                    runOnBackgroundThread {
                        runOnBackgroundThread { steamService.disconnect() }
                        clearData()
                    }
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
                    runOnBackgroundThread {
                        steamService.getHandler<SteamFriends>()?.setPersonaName(text.toString())
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
        prefVersion?.click {
            counter++

            if (counter == 5) {
                val dialog = MaterialDialog(ctx, BottomSheet(LayoutMode.WRAP_CONTENT))
                        .customView(R.layout.view_easteregg, scrollable = false)

                val customView = dialog.getCustomView()
                val handler = Handler()
                customView.findViewById<ImageView>(R.id.vapullaLogoMiddle).drawable as Animatable
                val d = customView.findViewById<ImageView>(
                        R.id.vapullaLogoMiddle).drawable as Animatable
                val d2 = customView.findViewById<ImageView>(
                        R.id.vapullaLogoBottom).drawable as Animatable
                VectorAnimCompat.registerAnimationCallback(
                        d,
                        object : Animatable2Compat.AnimationCallback() {
                            override fun onAnimationEnd(drawable: Drawable) {
                                d.start()
                                handler.postDelayed({
                                    d2.stop()
                                    d2.start()
                                }, 300)
                            }
                        })
                d.start()
                handler.postDelayed({ d2.start() }, 300)
                dialog.onDismiss {
                    handler.removeCallbacksAndMessages(null)
                    VectorAnimCompat.clearAnimationCallbacks(d)
                    VectorAnimCompat.clearAnimationCallbacks(d2)
                    d.stop()
                    d2.stop()
                }
                dialog.show()

                counter = 0
            }

            true
        }

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
        (activity as SettingsActivity).imgurAuthService.clear()

        (activity as SettingsActivity).db.steamFriendDao().delete()
        (activity as SettingsActivity).db.chatMessageDao().delete()
        (activity as SettingsActivity).db.emoticonDao().delete()

        prefs.edit().clear().apply()
        PreferenceManager.setDefaultValues(context, R.xml.pref_general, false)
    }

    fun onDisconnected() {
        val loginIntent = Intent(context, LoginActivity::class.java)
        loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(loginIntent)
    }

    private fun browse(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        })
    }
}
