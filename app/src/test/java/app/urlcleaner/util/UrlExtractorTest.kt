package app.urlcleaner.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlExtractorTest {

    @Test fun `plain url`() {
        assertEquals("https://example.com/a", UrlExtractor.firstUrl("https://example.com/a"))
    }

    @Test fun `url surrounded by text`() {
        val got = UrlExtractor.firstUrl("Look at this https://example.com/a?fbclid=x pretty cool")
        assertEquals("https://example.com/a?fbclid=x", got)
    }

    @Test fun `trailing punctuation stripped`() {
        assertEquals("https://example.com", UrlExtractor.firstUrl("Go to https://example.com."))
        assertEquals("https://example.com/a", UrlExtractor.firstUrl("(see https://example.com/a)"))
    }

    @Test fun `first url wins`() {
        val got = UrlExtractor.firstUrl("one https://a.example two https://b.example")
        assertEquals("https://a.example", got)
    }

    @Test fun `blank or missing`() {
        assertNull(UrlExtractor.firstUrl(""))
        assertNull(UrlExtractor.firstUrl("no url here"))
        assertNull(UrlExtractor.firstUrl(null))
    }

    @Test fun `http ok`() {
        assertEquals("http://example.com", UrlExtractor.firstUrl("http://example.com"))
    }
}
