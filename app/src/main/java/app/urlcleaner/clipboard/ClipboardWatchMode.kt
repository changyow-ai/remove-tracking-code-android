package app.urlcleaner.clipboard

enum class ClipboardWatchMode {
    /** No background clipboard monitoring. */
    OFF,
    /** Silently overwrite clipboard with the cleaned URL and show a Toast. */
    AUTO_CLEAN,
    /** Show a floating sci-fi bubble; user taps to clean, swipes to dismiss. */
    ASK,
}
