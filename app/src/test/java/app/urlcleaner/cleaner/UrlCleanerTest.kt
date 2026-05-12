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

    @Test fun `cleans threads xmt and slof`() {
        val r = cleaner.clean("https://www.threads.com/@u/post/X?xmt=AQABCDEF&slof=1")
        assertEquals("https://www.threads.com/@u/post/X", r.cleaned)
    }

    @Test fun `cleans threads net alternate tld`() {
        val r = cleaner.clean("https://www.threads.net/@u/post/X?xmt=abc")
        assertEquals("https://www.threads.net/@u/post/X", r.cleaned)
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

    @Test fun `cleans yahoo news guce consent params`() {
        val r = cleaner.clean("https://tw.news.yahoo.com/article-title-123.html?guccounter=1&guce_referrer=aHR0cHM6Ly9leGFtcGxlLmNvbQ&guce_referrer_sig=AQAAA")
        assertEquals("https://tw.news.yahoo.com/article-title-123.html", r.cleaned)
        assertTrue(r.wasChanged)
    }

    @Test fun `cleans yahoo soc_src and ncid`() {
        val r = cleaner.clean("https://news.yahoo.com/some-article.html?soc_src=social-sh&soc_trk=tw&ncid=txtlnkusaolp00000058")
        assertEquals("https://news.yahoo.com/some-article.html", r.cleaned)
    }

    @Test fun `cleans yahoo dot-tsrc`() {
        val r = cleaner.clean("https://finance.yahoo.com/news/foo.html?.tsrc=fin-srch")
        assertEquals("https://finance.yahoo.com/news/foo.html", r.cleaned)
    }

    @Test fun `cleans engadget guccounter`() {
        val r = cleaner.clean("https://www.engadget.com/some-post-120000123.html?guccounter=1")
        assertEquals("https://www.engadget.com/some-post-120000123.html", r.cleaned)
    }

    @Test fun `cleans bbc at_medium and at_campaign`() {
        val r = cleaner.clean("https://www.bbc.com/news/articles/abc123?at_medium=custom7&at_campaign=64&at_custom1=part&keep=yes")
        assertTrue(r.cleaned.contains("keep=yes"))
        assertFalse(r.cleaned.contains("at_medium"))
        assertFalse(r.cleaned.contains("at_campaign"))
        assertFalse(r.cleaned.contains("at_custom1"))
    }

    @Test fun `cleans daily mail ito`() {
        val r = cleaner.clean("https://www.dailymail.co.uk/news/article-12345/headline.html?ito=social-facebook_news&keep=1")
        assertTrue(r.cleaned.contains("keep=1"))
        assertFalse(r.cleaned.contains("ito="))
    }

    @Test fun `cleans nyt smid and smtyp`() {
        val r = cleaner.clean("https://www.nytimes.com/2024/01/01/world/article.html?smid=tw-share&smtyp=cur")
        assertEquals("https://www.nytimes.com/2024/01/01/world/article.html", r.cleaned)
    }

    @Test fun `cleans xtor at-internet tracker`() {
        val r = cleaner.clean("https://www.example-news.fr/article?xtor=RSS-200&keep=2")
        assertTrue(r.cleaned.contains("keep=2"))
        assertFalse(r.cleaned.contains("xtor="))
    }

    @Test fun `cleans webtrends WT dotted params`() {
        val r = cleaner.clean("https://www.example.com/page?WT.mc_id=email&WT.tsrc=newsletter&keep=ok")
        assertTrue(r.cleaned.contains("keep=ok"))
        assertFalse("WT.mc_id leaked: ${r.cleaned}", r.cleaned.contains("WT.mc_id"))
        assertFalse(r.cleaned.contains("WT.tsrc"))
    }

    @Test fun `cleans intcmp internal campaign`() {
        val r = cleaner.clean("https://www.bloomberg.com/news/articles/2024-01-01/foo?intcmp=storyline&keep=1")
        assertTrue(r.cleaned.contains("keep=1"))
        assertFalse(r.cleaned.contains("intcmp"))
    }

    companion object {
        private lateinit var ruleSet: RuleSet
        private lateinit var cleaner: UrlCleaner

        @JvmStatic
        @BeforeClass
        fun loadRules() {
            val main = RulesParser.parse(File("src/main/assets/clearurls-rules.json").readText())
            val supp = RulesParser.parse(File("src/main/assets/supplemental-rules.json").readText())
            ruleSet = RuleSet(main.providers + supp.providers)
            cleaner = UrlCleaner(ruleSet)
            require(ruleSet.providers.isNotEmpty()) { "rules failed to load" }
        }
    }
}
