package app.urlcleaner.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast

/** One place to centralize the system clipboard + Toast + re-share behaviours. */
object ShareActions {

    /** Android 13 (API 33) shows its own "copied" chip, so our Toast on success is redundant
     *  except in two edge cases — see plan. */
    private val systemShowsCopyChip: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun copyToClipboard(context: Context, text: String, label: String = "URL") {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    /**
     * Toast after a cleaning result. Skipped on Android 13+ when the system already
     * displays its "copied" chip and there is nothing extra to say — see the plan.
     */
    fun toastAfterCleaning(
        context: Context,
        paramsRemoved: Int,
        isBlocked: Boolean,
        forceShow: Boolean = false,
    ) {
        val msg: String = when {
            isBlocked -> "整段是追蹤跳轉，未複製"
            paramsRemoved == 0 -> "無追蹤碼，URL 未變"
            else -> "已清理 $paramsRemoved 個追蹤參數"
        }
        val shouldShow = when {
            forceShow -> true
            isBlocked -> true              // clipboard wasn't written; user must know
            !systemShowsCopyChip -> true   // pre-13: no system chip, always toast
            paramsRemoved == 0 -> true     // 13+ edge case: confirm the app ran
            else -> false                  // 13+: rely on system "copied" chip
        }
        if (shouldShow) Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    /** Always-shown toast for explicit user copies (single entry, copy-all in history). */
    fun toastCopied(context: Context, count: Int) {
        val msg = if (count <= 1) "已複製" else "已複製 $count 筆"
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun reShare(context: Context, url: String, excludeSelf: Boolean = true) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(send, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (excludeSelf && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val self = ComponentName(context, "app.urlcleaner.ShareHandlerActivity")
                putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(self))
            }
        }
        context.startActivity(chooser)
    }
}
