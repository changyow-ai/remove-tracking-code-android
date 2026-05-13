package app.urlcleaner.clipboard

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlin.math.hypot

/**
 * A floating overlay bubble drawn via WindowManager.
 *
 * Gestures:
 *  - Tap (< 400ms, < 8dp travel)        → onClean() then dismiss
 *  - Swipe (> 80dp travel before 400ms)  → dismiss without cleaning
 *  - Long-press (≥ 400ms) + drag         → reposition; saves new position on finger-up
 *
 * Auto-dismisses after [AUTO_DISMISS_MS] of inactivity.
 */
internal class CleanBubble(
    private val context: Context,
    private val windowManager: WindowManager,
    private val initialX: Int,
    private val initialY: Int,
    private val onClean: () -> Unit,
    private val onPositionChanged: (Int, Int) -> Unit,
) {
    private val lifecycleOwner = ServiceLifecycleOwner()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = initialX
        y = initialY
    }

    // touch state
    private var touchRawX = 0f
    private var touchRawY = 0f
    private var windowStartX = 0
    private var windowStartY = 0
    private var isDragging = false
    private var isGestureConsumed = false

    private val longPressRunnable = Runnable { isDragging = true }
    private val autoDismissRunnable = Runnable { dismiss() }

    private val view: ComposeView = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        lifecycleOwner.start()
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        setContent { BubbleContent() }
        setOnTouchListener { _, event -> handleTouch(event) }
    }

    fun show() {
        windowManager.addView(view, params)
        mainHandler.postDelayed(autoDismissRunnable, AUTO_DISMISS_MS)
    }

    fun dismiss() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        mainHandler.removeCallbacks(longPressRunnable)
        runCatching { windowManager.removeView(view) }
        lifecycleOwner.stop()
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mainHandler.removeCallbacks(autoDismissRunnable)
                touchRawX = event.rawX
                touchRawY = event.rawY
                windowStartX = params.x
                windowStartY = params.y
                isDragging = false
                isGestureConsumed = false
                mainHandler.postDelayed(longPressRunnable, LONG_PRESS_MS)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - touchRawX
                val dy = event.rawY - touchRawY
                if (isDragging) {
                    params.x = windowStartX + dx.toInt()
                    params.y = windowStartY + dy.toInt()
                    runCatching { windowManager.updateViewLayout(view, params) }
                } else if (!isGestureConsumed && hypot(dx, dy) > SWIPE_THRESHOLD_PX) {
                    // Fast swipe — dismiss without cleaning.
                    mainHandler.removeCallbacks(longPressRunnable)
                    isGestureConsumed = true
                    dismiss()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mainHandler.removeCallbacks(longPressRunnable)
                if (isDragging) {
                    onPositionChanged(params.x, params.y)
                    isDragging = false
                } else if (!isGestureConsumed) {
                    onClean()
                    dismiss()
                }
            }
        }
        return true
    }

    companion object {
        private const val LONG_PRESS_MS = 400L
        private const val AUTO_DISMISS_MS = 6_000L
        private const val SWIPE_THRESHOLD_PX = 80f
    }
}

@Composable
private fun BubbleContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "bubble")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.00f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    val cyan = Color(0xFF00FFFF)
    val deepTeal = Color(0xFF001A1F)

    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(scale)
            .drawBehind {
                // Multi-layer outer glow
                for (layer in 4 downTo 1) {
                    drawCircle(
                        color = cyan.copy(alpha = glowAlpha * layer * 0.06f),
                        radius = size.minDimension / 2 + layer * 10f,
                    )
                }
            }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0AFFFF), deepTeal),
                ),
            )
            .border(width = 2.dp, color = cyan.copy(alpha = 0.8f), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.AutoFixHigh,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp),
        )
    }
}
