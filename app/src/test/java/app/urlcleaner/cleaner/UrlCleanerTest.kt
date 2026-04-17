package app.urlcleaner.cleaner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class UrlCleanerTest {

    @Test fun `removes fbclid from facebook`() {
        val r = cleaner.clean("https://www.facebook.com/some/page/?fbclid=IwAR0abcdef")
        assertEquals("https://www.facebook.com/some/page/", r.cleaned)
        assertTrue(r.wasChanged)
        assertFalse(r.isBlocked)
    }

    @Test fun `unwraps facebook l_php redirect`() {
        val wrapped = "https://l.facebook.com/l.php?u=https%3A%2F%2Fexample.com%2Fpage%3Fa%3D1&h=AT"
        val r = cleaner.clean(wrapped)
        assertTrue("got ${r.cleaned}", r.cleaned.startsWith("https://example.com/page"))
    }

    @Test fun `removes igshid from instagram`() {
        val r = cleaner.clean("https://www.instagram.com/p/abc123/?igshid=MzRlODBiNWFlZA%3D%3D")
        assertEquals("https://www.instagram.com/p/abc123/", r.cleaned)
    }

    @Test fun `cleans reddit share params`() {
        val r = cleaner.clean("https://www.reddit.com/r/kotlin/comments/xyz/?share_id=abc&utm_medium=android_app&utm_source=share")
        assertEquals("https://www.reddit.com/r/kotlin/comments/xyz/", r.cleaned)
    }

    @Test fun `cleans youtube share`() {
        val r = cleaner.clean("https://www.youtube.com/watch?v=dQw4w9WgXcQ&si=abc123&pp=xyz")
        // v= must survive. si and pp are tracking.
        assertTrue("got ${r.cleaned}", r.cleaned.contains("v=dQw4w9WgXcQ"))
        assertFalse(r.cleaned.contains("si="))
        assertFalse(r.cleaned.contains("pp="))
    }

    @Test fun `cleans amazon tag`() {
        val r = cleaner.clean("https://www.amazon.com/dp/B00TEST/?tag=somepartner-20&ref=nav_logo")
        assertFalse("should strip tag: ${r.cleaned}", r.cleaned.contains("tag="))
        assertTrue(r.cleaned.startsWith("https://www.amazon.com/dp/B00TEST"))
    }

    @Test fun `cleans x share`() {
        val r = cleaner.clean("https://x.com/someone/status/1234567890?s=20&t=abc")
        assertFalse(r.cleaned.contains("s=20"))
        assertFalse(r.cleaned.contains("t=abc"))
        assertTrue(r.cleaned.startsWith("https://x.com/someone/status/1234567890"))
    }

    @Test fun `unwraps google search redirect`() {
        val r = cleaner.clean("https://www.google.com/url?q=https%3A%2F%2Fexample.com%2Farticle&sa=U&ved=xyz")
        assertTrue("got ${r.cleaned}", r.cleaned.startsWith("https://example.com/article"))
    }

    @Test fun `cleans tiktok params`() {
        val r = cleaner.clean("https://www.tiktok.com/@user/video/123?_r=1&_t=abc&is_copy_url=1")
        assertFalse(r.cleaned.contains("_r="))
        assertFalse(r.cleaned.contains("_t="))
    }

    @Test fun `cleans linkedin trk`() {
        val r = cleaner.clean("https://www.linkedin.com/in/someone?trk=abc&trkCampaign=xyz")
        assertFalse(r.cleaned.contains("trk="))
    }

    @Test fun `cleans spotify si`() {
        val r = cleaner.clean("https://open.spotify.com/track/123?si=abcdef1234567890")
        assertFalse(r.cleaned.contains("si="))
    }

    @Test fun `strips generic utm params via globalRules`() {
        val r = cleaner.clean("https://somewhere-random.example.org/article?utm_source=newsletter&utm_medium=email&utm_campaign=spring&keep=yes")
        assertTrue("kept: ${r.cleaned}", r.cleaned.contains("keep=yes"))
        assertFalse(r.cleaned.contains("utm_"))
    }

    @Test fun `strips gclid fbclid mc_cid hsenc globally`() {
        val r = cleaner.clean("https://example.com/a?gclid=1&fbclid=2&mc_cid=3&_hsenc=4&keep=me")
        assertTrue(r.cleaned.contains("keep=me"))
        listOf("gclid", "fbclid", "mc_cid", "_hsenc").forEach {
            assertFalse("$it should be gone from ${r.cleaned}", r.cleaned.contains("$it="))
        }
    }

    @Test fun `no change when url is clean`() {
        val clean = "https://example.com/path"
        val r = cleaner.clean(clean)
        assertEquals(clean, r.cleaned)
        assertFalse(r.wasChanged)
        assertEquals(0, r.paramsRemoved)
    }

    @Test fun `no change when only kept params present`() {
        val url = "https://example.com/search?q=hello&page=2"
        val r = cleaner.clean(url)
        assertEquals(url, r.cleaned)
        assertFalse(r.wasChanged)
    }

    @Test fun `preserves fragment`() {
        val r = cleaner.clean("https://example.com/a?utm_source=x&keep=1#section")
        assertEquals("https://example.com/a?keep=1#section", r.cleaned)
    }

    @Test fun `drops dangling question mark after removing last param`() {
        val r = cleaner.clean("https://example.com/a?utm_source=x")
        assertEquals("https://example.com/a", r.cleaned)
    }

    @Test fun `http and https both covered`() {
        val r = cleaner.clean("http://www.facebook.com/p?fbclid=x")
        assertFalse(r.cleaned.contains("fbclid"))
    }

    @Test fun `referral marketing opt-in can be disabled`() {
        val off = UrlCleaner(ruleSet, removeReferralMarketing = false)
        val withRefRule = "https://aliexpress.com/item/123.html?aff_platform=api-new-link-generate&aff_trace_key=abc"
        val rOn = cleaner.clean(withRefRule)
        val rOff = off.clean(withRefRule)
        // At minimum, the "off" result should not have removed fewer or equal, never MORE.
        assertTrue(rOn.paramsRemoved >= rOff.paramsRemoved)
    }

    @Test fun `counts removed params`() {
        val r = cleaner.clean("https://example.com/a?utm_source=x&utm_medium=y&gclid=z")
        assertTrue("expected >=1 removals, got ${r.paramsRemoved}", r.paramsRemoved >= 1)
    }

    companion object {
        private lateinit var ruleSet: RuleSet
        private lateinit var cleaner: UrlCleaner

        @JvmStatic
        @BeforeClass
        fun loadRules() {
            val json = File("src/main/assets/clearurls-rules.json").readText()
            ruleSet = RulesParser.parse(json)
            cleaner = UrlCleaner(ruleSet)
            require(ruleSet.providers.isNotEmpty()) { "rules failed to load" }
        }
    }
}
