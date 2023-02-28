package `in`.dragonbra.vapulla.compose.screens.chat

import android.os.Bundle
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.vapulla.VapullaBaseActivity
import timber.log.Timber

@AndroidEntryPoint
class ChatActivity : VapullaBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("onCreate")
        setContent {
            ChatScreen()
        }
    }
}
