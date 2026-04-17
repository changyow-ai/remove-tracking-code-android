package app.urlcleaner.util

/**
 * Finds the first http(s) URL inside arbitrary text. Share intents sometimes deliver
 * "Look at this https://example.com/x very cool" and we only want the URL out.
 */
object UrlExtractor {
    private val URL_REGEX = Regex("""https?://[^\s<>"'\[\]{}|\\^`]+""", RegexOption.IGNORE_CASE)
    private val TRAILING_PUNCT = ".,;:!?)]"

    fun firstUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val raw = URL_REGEX.find(text)?.value ?: return null
        return raw.trimEnd { it in TRAILING_PUNCT }.ifEmpty { null }
    }
}
