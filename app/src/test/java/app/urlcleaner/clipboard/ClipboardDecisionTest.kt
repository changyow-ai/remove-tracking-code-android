package app.urlcleaner.clipboard

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-logic tests for the "should we act on this clipboard change?" decision.
 * Mirrors the guard conditions in ClipboardWatcherService without requiring an
 * Android context.
 */
class ClipboardDecisionTest {

    /** Mirrors the decide logic from ClipboardWatcherService. */
    private fun decide(
        text: String?,
        mode: ClipboardWatchMode,
        lastWritten: String?,
        wasChanged: Boolean,
    ): Action {
        if (mode == ClipboardWatchMode.OFF) return Action.Skip
        if (text.isNullOrBlank()) return Action.Skip
        if (text == lastWritten) return Action.Skip      // loop guard
        if (!wasChanged) return Action.Skip              // URL already clean
        return when (mode) {
            ClipboardWatchMode.AUTO_CLEAN -> Action.AutoClean
            ClipboardWatchMode.ASK -> Action.ShowBubble
            ClipboardWatchMode.OFF -> Action.Skip
        }
    }

    enum class Action { Skip, AutoClean, ShowBubble }

    @Test fun `OFF mode always skips`() {
        assertEquals(Action.Skip, decide("https://example.com?utm_source=x", ClipboardWatchMode.OFF, null, true))
    }

    @Test fun `null text skips`() {
        assertEquals(Action.Skip, decide(null, ClipboardWatchMode.AUTO_CLEAN, null, true))
    }

    @Test fun `blank text skips`() {
        assertEquals(Action.Skip, decide("   ", ClipboardWatchMode.AUTO_CLEAN, null, true))
    }

    @Test fun `matches lastWritten — loop guard skips`() {
        val url = "https://example.com"
        assertEquals(Action.Skip, decide(url, ClipboardWatchMode.AUTO_CLEAN, url, true))
    }

    @Test fun `URL was already clean — skips`() {
        assertEquals(Action.Skip, decide("https://example.com", ClipboardWatchMode.AUTO_CLEAN, null, false))
    }

    @Test fun `AUTO_CLEAN mode triggers auto clean`() {
        assertEquals(Action.AutoClean, decide("https://example.com?utm_source=x", ClipboardWatchMode.AUTO_CLEAN, null, true))
    }

    @Test fun `ASK mode triggers bubble`() {
        assertEquals(Action.ShowBubble, decide("https://example.com?fbclid=x", ClipboardWatchMode.ASK, null, true))
    }

    @Test fun `lastWritten null does not block`() {
        assertEquals(Action.AutoClean, decide("https://x.com?s=20", ClipboardWatchMode.AUTO_CLEAN, null, true))
    }

    @Test fun `different from lastWritten proceeds normally`() {
        assertEquals(Action.AutoClean, decide("https://x.com?s=20", ClipboardWatchMode.AUTO_CLEAN, "https://other.com", true))
    }
}
