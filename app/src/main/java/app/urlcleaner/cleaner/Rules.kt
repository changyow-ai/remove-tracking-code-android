package app.urlcleaner.cleaner

/**
 * Parsed ClearURLs rule set. Mirrors the structure of `data.min.json`:
 * `{ providers: { <name>: Provider, ... } }`.
 */
data class RuleSet(val providers: List<Provider>)

data class Provider(
    val name: String,
    val urlPattern: Regex,
    val completeProvider: Boolean,
    val rules: List<Regex>,
    val referralMarketing: List<Regex>,
    val rawRules: List<Regex>,
    val exceptions: List<Regex>,
    val redirections: List<Regex>,
    val forceRedirection: Boolean,
)
