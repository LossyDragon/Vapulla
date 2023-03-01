package `in`.dragonbra.vapulla.chat

import java.util.*

// class PaperPlane(val context: Context, private val emoteSizeDp: Float) {
//
//    companion object {
//        // val EMOTE_PATTERN: Pattern = Pattern.compile("\\[emoticon]([a-zA-Z0-9]+)\\[/emoticon]")
//        val EMOTE_PATTERN: Pattern =
//            Pattern.compile("\\u02D0([a-zA-Z\\d]+)\\u02D0")
//        val STICKER_PATTERN: Pattern =
//            Pattern.compile("\\[sticker type=\"([a-zA-Z\\d]+)\".limit=\"0\"]\\[/sticker]")
//    }
//
//    private val targets: MutableMap<TextView, MutableList<Any>> = HashMap()
//
//    fun load(view: TextView, message: String, showUrl: Boolean, showStickers: Boolean) {
//        clear(view)
//
//        val spannable = SpannableString(message)
//
//        //region [Region] URL
//        if (showUrl) {
//            val urlMatcher = Patterns.WEB_URL.matcher(message)
//            while (urlMatcher.find()) {
//                val result = urlMatcher.toMatchResult()
//
//                val nextSpace = message.indexOf(' ', result.start())
//                val end = if (nextSpace == -1) message.length else nextSpace
//                spannable.setSpan(
//                    URLSpan(result.group()),
//                    result.start(),
//                    end,
//                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
//                )
//            }
//
//            view.movementMethod = LinkMovementMethod.getInstance()
//        }
//
//        view.text = spannable
//        view.requestLayout()
//        //endregion
//
//        //region [Region] Emoji
//        val emoteMatcher = EMOTE_PATTERN.matcher(message)
//
//        while (emoteMatcher.find()) {
//            if (!targets.containsKey(view)) {
//                targets[view] = LinkedList()
//            }
//
//            val result = emoteMatcher.toMatchResult()
//
//            val emote = result.group(1)
//
//            val target = EmoteTarget(
//                context,
//                view,
//                spannable,
//                result.start(),
//                result.end(),
//                emoteSizeDp,
//                targets[view]
//            )
//
//            val request = ImageRequest.Builder(context)
//                .scale(Scale.FIT)
//                .data("$EMOTE_URL:$emote:")
//                .target(target)
//                .build()
//
//            CoroutineScope(Dispatchers.Default + Job()).launch {
//                context.imageLoader.execute(request)
//            }
//        }
//        //endregion
//
//        //region [Region] Sticker
//        val stickerMatcher = STICKER_PATTERN.matcher(message)
//
//        if (!showStickers && stickerMatcher.matches()) {
//            val sticker = stickerMatcher.toMatchResult().group(1)
//            view.text = context.getString(R.string.messageSentSticker, sticker)
//        } else {
//            while (stickerMatcher.find()) {
//                if (!targets.containsKey(view)) {
//                    targets[view] = LinkedList()
//                }
//
//                val result = stickerMatcher.toMatchResult()
//
//                val sticker = result.group(1)
//
//                val target = StickerTarget(
//                    context,
//                    view,
//                    spannable,
//                    result.start(),
//                    result.end(),
//                    targets[view]
//                )
//
//                val imageLoader = ImageLoader.Builder(context)
//                    .components {
//                        add(AnimatedPngDecoder())
//                    }
//                    .build()
//
//                val request = ImageRequest.Builder(context)
//                    .scale(Scale.FIT)
//                    .data("$STICKER_URL$sticker")
//                    .target(target)
//                    .build()
//
//                CoroutineScope(Dispatchers.Default + Job()).launch {
//                    context.imageLoader.execute(request)
//                }
//            }
//        }
//        //endregion
//    }
//
//    fun clear(view: TextView) {
//        view.text = ""
//
//        targets[view]?.forEach {
//            when (it) {
//                is StickerTarget -> it.cancel()
//                is EmoteTarget -> it.cancel()
//            }
//        }
//        targets[view]?.clear()
//    }
//
//    fun clearAll() {
//        targets.entries.forEach { targets ->
//            targets.value.forEach { value ->
//                when (value) {
//                    is StickerTarget -> value.cancel()
//                    is EmoteTarget -> value.cancel()
//                }
//            }
//        }
//        targets.clear()
//    }
// }
