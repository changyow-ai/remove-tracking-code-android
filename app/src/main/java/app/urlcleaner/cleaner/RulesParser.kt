package app.urlcleaner.cleaner

import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses ClearURLs `data.min.json` into [RuleSet].
 * Patterns are compiled with [RegexOption.IGNORE_CASE]; invalid patterns are skipped
 * so a single malformed entry can't blow up the whole load.
 */
object RulesParser {

    fun parse(json: String): RuleSet {
        val root = JSONObject(json)
        val providersObj = root.optJSONObject("providers") ?: return RuleSet(emptyList())
        val providers = mutableListOf<Provider>()
        val keys = providersObj.keys()
        while (keys.hasNext()) {
            val name = keys.next()
            val p = providersObj.optJSONObject(name) ?: continue
            val urlPattern = compile(p.optString("urlPattern", "")) ?: continue
            providers += Provider(
                name = name,
                urlPattern = urlPattern,
                completeProvider = p.optBoolean("completeProvider", false),
                rules = compileArray(p.optJSONArray("rules")),
                referralMarketing = compileArray(p.optJSONArray("referralMarketing")),
                rawRules = compileArray(p.optJSONArray("rawRules")),
                exceptions = compileArray(p.optJSONArray("exceptions")),
                redirections = compileArray(p.optJSONArray("redirections")),
                forceRedirection = p.optBoolean("forceRedirection", false),
            )
        }
        // ClearURLs applies globalRules first; keep that order so rawRule artifacts
        // don't accumulate in a counterintuitive sequence.
        providers.sortBy { if (it.name == "globalRules") 0 else 1 }
        return RuleSet(providers)
    }

    private fun compileArray(arr: JSONArray?): List<Regex> {
        if (arr == null) return emptyList()
        val out = ArrayList<Regex>(arr.length())
        for (i in 0 until arr.length()) {
            compile(arr.optString(i, ""))?.let { out += it }
        }
        return out
    }

    private fun compile(pattern: String): Regex? {
        if (pattern.isEmpty()) return null
        return try {
            Regex(pattern, RegexOption.IGNORE_CASE)
        } catch (_: Throwable) {
            null
        }
    }
}
