package `in`.dragonbra.vapulla.ui.screens.web

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.ui.composables.BackButton
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.compose.getKoin
import timber.log.Timber

/**
 * The handler that opens an address in the device's browser. Inside the app
 * [LocalUriHandler] may send Steam's own pages to [WebPageScreen] instead; this one never
 * does, and is what content written by other people, such as chat messages, opens links with.
 */
val LocalExternalUriHandler = staticCompositionLocalOf<UriHandler?> { null }

/** Opens links in [content] in the device's browser, whatever the app does elsewhere. */
@Composable
fun ExternalLinks(content: @Composable () -> Unit) {
    val external = LocalExternalUriHandler.current

    if (external == null) {
        content()
    } else {
        CompositionLocalProvider(LocalUriHandler provides external, content = content)
    }
}

/** Which addresses belong to Steam's websites, and the sign-in cookie they take. */
object SteamPages {

    /** The sites a link has to point at to be opened in the app, signed in. */
    private val SIGNED_IN_HOSTS = setOf(
        "steamcommunity.com",
        "www.steamcommunity.com",
        "store.steampowered.com",
        "help.steampowered.com",
    )

    /** The sites that are given the sign-in cookie. The checkout is left out on purpose. */
    private val COOKIE_SITES = listOf(
        "https://steamcommunity.com",
        "https://store.steampowered.com",
        "https://help.steampowered.com",
    )

    private const val SIGN_IN_COOKIE = "steamLoginSecure"

    /** True for an `https` address on Steam's community, store or help site. */
    fun isSignedInPage(address: String): Boolean {
        val uri = Uri.parse(address)
        return uri.scheme == "https" && uri.host?.lowercase() in SIGNED_IN_HOSTS
    }

    /** True for any of Steam's own sites: a page may move between them freely. */
    fun isSteamSite(uri: Uri): Boolean {
        val host = uri.host?.lowercase() ?: return false
        return uri.scheme == "https" && STEAM_DOMAINS.any { host == it || host.endsWith(".$it") }
    }

    private val STEAM_DOMAINS = listOf("steamcommunity.com", "steampowered.com", "s.team")

    /** Signs the web view's pages in with [cookieValue]. */
    fun signIn(cookieValue: String) {
        val cookies = CookieManager.getInstance()
        COOKIE_SITES.forEach { site ->
            cookies.setCookie(site, "$SIGN_IN_COOKIE=$cookieValue; Path=/; Secure; HttpOnly")
        }
        cookies.flush()
    }

    /** Forgets every cookie the web view holds, the sign-in among them. */
    suspend fun signOut() = suspendCancellableCoroutine { done ->
        CookieManager.getInstance().removeAllCookies { done.resume(Unit) }
    }
}

/**
 * One of Steam's web pages inside the app, signed in to the account that the app is signed
 * in to. Only reached when the setting for it is on.
 *
 * The sign-in is a cookie holding an access token, set just before the page loads and
 * removed again when the screen is left. Pages outside Steam's own sites are not opened here
 * but in the device's browser, so the cookie's reach stays with Steam.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebPageScreen(url: String, onBack: () -> Unit) {
    val connection: ServiceConnection = getKoin().get()
    val external = LocalExternalUriHandler.current ?: LocalUriHandler.current

    var webView by remember { mutableStateOf<WebView?>(null) }
    var ready by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var address by remember { mutableStateOf(url) }
    var progress by remember { mutableFloatStateOf(0f) }
    var canGoBack by remember { mutableStateOf(false) }

    // Old cookies go first, so a page never opens with an earlier sign-in.
    LaunchedEffect(Unit) {
        SteamPages.signOut()

        runCatching { requireNotNull(connection.steamService?.apis).web.signInCookie() }
            .onSuccess(SteamPages::signIn)
            .onFailure {
                Timber.w(it, "No sign-in for the web page")
                notice = "Could not sign the page in, it is shown signed out"
            }
        ready = true
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
            CookieManager.getInstance().removeAllCookies(null)
        }
    }

    // Back steps through the pages visited before it leaves the screen.
    BackHandler(enabled = canGoBack) { webView?.goBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = Uri.parse(address).host.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                },
                navigationIcon = { BackButton(onClick = onBack) },
                actions = {
                    IconButton(onClick = { external.openUri(address) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in browser",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (progress < 1f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            notice?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (!ready) {
                LoadingBox(modifier = Modifier.fillMaxSize())
                return@Column
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    steamWebView(
                        context = WebView(context),
                        onTitle = { title = it },
                        onAddress = { page, back ->
                            address = page
                            canGoBack = back
                        },
                        onProgress = { progress = it },
                        onLeaveSteam = external::openUri,
                    ).also {
                        webView = it
                        it.loadUrl(url)
                    }
                },
            )
        }
    }
}

/**
 * Sets [context] up for Steam's pages: scripts on, since the sites need them, and everything
 * a page could use to reach the device (files, content providers, new windows) off.
 */
@SuppressLint("SetJavaScriptEnabled")
private fun steamWebView(
    context: WebView,
    onTitle: (String) -> Unit,
    onAddress: (address: String, canGoBack: Boolean) -> Unit,
    onProgress: (Float) -> Unit,
    onLeaveSteam: (String) -> Unit,
): WebView = context.apply {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    settings.setSupportMultipleWindows(false)
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

    webViewClient = object : WebViewClient() {
        // A link that leaves Steam's sites is handed to the browser, without the cookie.
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!request.isForMainFrame || SteamPages.isSteamSite(request.url)) return false

            onLeaveSteam(request.url.toString())
            return true
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            onAddress(url, view.canGoBack())
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
            onAddress(url, view.canGoBack())
        }
    }

    webChromeClient = object : WebChromeClient() {
        override fun onReceivedTitle(view: WebView, title: String?) {
            onTitle(title.orEmpty())
        }

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            onProgress(newProgress / FULL_PROGRESS)
        }
    }
}

private const val FULL_PROGRESS = 100f
