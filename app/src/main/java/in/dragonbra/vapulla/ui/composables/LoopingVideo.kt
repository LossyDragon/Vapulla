package `in`.dragonbra.vapulla.ui.composables

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import timber.log.Timber

/**
 * A short video from the network that plays silently, over and over, and fills its bounds,
 * cropped at the edges where its shape differs: a moving picture more than a video. It is
 * drawn with Android's own `MediaPlayer`, so no player library is needed.
 *
 * It fades in with its first frame; until then, and when it cannot be played, nothing is
 * drawn and what is underneath shows. [onError] tells of the latter, on the main thread.
 */
@Composable
fun LoopingVideo(url: String, modifier: Modifier = Modifier, onError: () -> Unit = {}) {
    val playback = remember(url) { VideoPlayback(url, onError) }
    DisposableEffect(playback) {
        onDispose { playback.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextureView(context).apply {
                // Invisible until the first frame, so what is underneath shows through.
                isOpaque = false
                alpha = 0f
                surfaceTextureListener = playback
                playback.view = this
            }
        },
    )
}

private const val FADE_IN_MS = 400L

/**
 * One video playing on the surface of a [TextureView]: the player and the surface it draws
 * to, which have to be let go of together. Everything here runs on the main thread.
 */
private class VideoPlayback(private val url: String, private val onError: () -> Unit) :
    TextureView.SurfaceTextureListener {

    var view: TextureView? = null

    private var player: MediaPlayer? = MediaPlayer()
    private var surface: Surface? = null

    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        val player = player ?: return

        runCatching {
            surface = Surface(texture).also(player::setSurface)
            player.setDataSource(url)
            player.isLooping = true
            player.setVolume(0f, 0f)
            player.setOnVideoSizeChangedListener { _, videoWidth, videoHeight ->
                fill(videoWidth, videoHeight)
            }
            player.setOnPreparedListener {
                // The size is known by now. Cropped before the first frame is drawn, the
                // video does not start out stretched and then jump into place.
                fill(it.videoWidth, it.videoHeight)
                it.start()
            }
            player.setOnInfoListener { _, what, _ ->
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    view?.animate()?.alpha(1f)?.setDuration(FADE_IN_MS)?.start()
                }
                false
            }
            player.setOnErrorListener { _, what, extra ->
                Timber.w("Cannot play $url: $what / $extra")
                onError()
                true
            }
            player.prepareAsync()
        }.onFailure {
            Timber.w(it, "Cannot play $url")
            onError()
        }
    }

    /**
     * A TextureView stretches the video to its own shape. This undoes the stretch along one
     * axis, which leaves the video filling the view with the overflow cut off.
     */
    private fun fill(videoWidth: Int, videoHeight: Int) {
        val view = view ?: return
        if (videoWidth == 0 || videoHeight == 0 || view.width == 0 || view.height == 0) return

        val viewRatio = view.width.toFloat() / view.height
        val videoRatio = videoWidth.toFloat() / videoHeight
        val scaleX = if (videoRatio > viewRatio) videoRatio / viewRatio else 1f
        val scaleY = if (videoRatio > viewRatio) 1f else viewRatio / videoRatio

        view.setTransform(
            Matrix().apply { setScale(scaleX, scaleY, view.width / 2f, view.height / 2f) },
        )
    }

    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        player?.let { fill(it.videoWidth, it.videoHeight) }
    }

    /**
     * The view is on its way out. The player is stopped before the surface goes: one still
     * playing would go on handing frames to a surface that no longer takes them.
     */
    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        release()
        return true
    }

    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit

    /** Lets go of the player and then of its surface. Harmless when done before. */
    fun release() {
        player?.release()
        player = null
        surface?.release()
        surface = null
        view = null
    }
}
