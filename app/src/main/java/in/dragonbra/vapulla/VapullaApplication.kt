package `in`.dragonbra.vapulla

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import dagger.hilt.android.HiltAndroidApp
import `in`.dragonbra.javasteam.util.log.LogListener
import `in`.dragonbra.javasteam.util.log.LogManager
import `in`.dragonbra.vapulla.compose.util.AnimatedPngDecoder
import timber.log.Timber

@HiltAndroidApp
class VapullaApplication : Application(), LogListener, ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        LogManager.addListener(this)
    }

    override fun onLog(clazz: Class<*>?, message: String?, throwable: Throwable?) {
        Timber.d("${clazz?.simpleName ?: "Unknown Class"} + $message")
    }

    override fun onError(clazz: Class<*>?, message: String?, throwable: Throwable?) {
        Timber.d("${clazz?.simpleName ?: "Unknown Class"} + $message")
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader(this).newBuilder()
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.1)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .maxSizePercent(0.03)
                    .directory(cacheDir.resolve("image_cache"))
                    .build()
            }
            .components {
                add(AnimatedPngDecoder.Factory())
            }
            .logger(DebugLogger())
            .build()
    }
}
