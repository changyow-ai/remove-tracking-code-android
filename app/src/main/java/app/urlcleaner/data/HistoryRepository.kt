package app.urlcleaner.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

data class HistoryEntry(
    val timestampMs: Long,
    val original: String,
    val cleaned: String,
)

/**
 * Simple TSV-backed history log. Format per line: `<timestampMs>\t<original>\t<cleaned>`.
 * Newlines in URLs are practically non-existent; we still encode `\n` and `\t` as literal
 * `\\n` / `\\t` on write, decode on read, so an adversarial URL can't corrupt neighbours.
 */
class HistoryRepository(context: Context) {

    private val file: File = File(context.filesDir, "history.tsv")

    private val _entries = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val entries: Flow<List<HistoryEntry>> = _entries.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        _entries.value = readAll()
    }

    suspend fun append(original: String, cleaned: String) = withContext(Dispatchers.IO) {
        val entry = HistoryEntry(System.currentTimeMillis(), original, cleaned)
        file.appendText(encode(entry) + "\n")
        _entries.value = _entries.value + entry
    }

    suspend fun delete(entry: HistoryEntry) = withContext(Dispatchers.IO) {
        val next = _entries.value.filterNot { it === entry || (it.timestampMs == entry.timestampMs && it.original == entry.original && it.cleaned == entry.cleaned) }
        rewrite(next)
        _entries.value = next
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        if (file.exists()) file.delete()
        _entries.value = emptyList()
    }

    private fun readAll(): List<HistoryEntry> {
        if (!file.exists()) return emptyList()
        return file.readLines().mapNotNull { decode(it) }
    }

    private fun rewrite(entries: List<HistoryEntry>) {
        file.writeText(entries.joinToString("\n", postfix = if (entries.isEmpty()) "" else "\n") { encode(it) })
    }

    private fun encode(e: HistoryEntry): String =
        "${e.timestampMs}\t${escape(e.original)}\t${escape(e.cleaned)}"

    private fun decode(line: String): HistoryEntry? {
        val parts = line.split('\t')
        if (parts.size != 3) return null
        val ts = parts[0].toLongOrNull() ?: return null
        return HistoryEntry(ts, unescape(parts[1]), unescape(parts[2]))
    }

    private fun escape(s: String): String = s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n")
    private fun unescape(s: String): String {
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (val n = s[i + 1]) {
                    'n' -> { sb.append('\n'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    '\\' -> { sb.append('\\'); i += 2 }
                    else -> { sb.append(n); i += 2 }
                }
            } else {
                sb.append(c); i++
            }
        }
        return sb.toString()
    }
}
