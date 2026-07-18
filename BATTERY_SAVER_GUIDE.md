# Battery Saver Screen Implementation Guide

This guide provides a comprehensive overview of how **Focus Island** implements its highly optimized, burn-in-preventative, and ultra-immersive **OLED/Battery Saver** mode.

It details the architectural choices, the specific UI requirements, and the low-level Android window hacking needed to achieve a true, display-cutout-aware, edge-to-edge fullscreen overlay in Jetpack Compose.

---

## Table of Contents
1. [Overview & Purpose](#1-overview--purpose)
2. [User Experience (UX) Flow](#2-user-experience-ux-flow)
3. [The Layout Challenge (System Bars & Notch Cutouts)](#3-the-layout-challenge-system-bars--notch-cutouts)
4. [Technical Solution Breakdown](#4-technical-solution-breakdown)
5. [Complete Source Code Reference](#5-complete-source-code-reference)
6. [Best Practices & Maintenance](#6-best-practices--maintenance)

---

## 1. Overview & Purpose

During deep focus or pomodoro sessions, users often leave their devices on their desks with the screen turned on. If left uncontrolled, this can cause:
1. **High Battery Drain**: Rendering complex, colorful graphics continuously consumes high GPU and display power.
2. **OLED Screen Burn-in**: Static, high-contrast UI elements (such as white timer text on a dark background) can permanently burn into OLED/AMOLED panels over prolonged sessions.

To solve this, **Focus Island** introduces a dedicated **Battery Saver / Screen Sleep Mode** overlay. This screen is optimized for AMOLED displays:
- It uses a **pure pitch-black background** (`#000000`), allowing OLED displays to turn off pixels entirely, dropping battery consumption to absolute minimums.
- It keeps the screen on (`FLAG_KEEP_SCREEN_ON`) so users can glance at the remaining focus time without the device auto-sleeping.
- It displays only vital, dim, moving, or highly simplified elements, eliminating burn-in risks while retaining maximum utility.

---

## 2. User Experience (UX) Flow

1. **Activation**: While a focus session is active, the user taps the **Battery Saver (lightning bolt / battery)** icon on the timer screen.
2. **Overlay Display**: A full-screen overlay fades in instantly. The screen is pitch black, displaying only:
    - Simplified active session timer (e.g., `25:00`) in a subtle, soft grey color.
    - Battery percentage indicator to check charging status or current level.
    - Low-intensity media controls (Play/Pause/Skip/Stop) or ambient sound indicator.
3. **Deactivation**: To prevent accidental wake-ups (e.g., from a pocket or desk touch), the overlay requires a **double-tap gesture** anywhere on the screen to dismiss and return to the main Focus Island UI.

---

## 3. The Layout Challenge (System Bars & Notch Cutouts)

A standard Android Dialog or Popup in Jetpack Compose is bounded by the system's default layout boundaries. This leads to two critical problems:
1. **Top Status Bar Uncovered**: The status bar at the top of the screen (carrying system icons like the clock, cellular signals, and notifications) remains visible or draws a separate status bar background, spoiling the pure-black AMOLED immersion.
2. **Display Cutouts (Notches)**: Devices with front-facing camera cutouts (punched holes or notches) reserve a safe zone. Normal dialogs shrink or align themselves below this cutout, leaving a visible horizontal colored strip.

To achieve a **100% immersive, pure black cover** that sweeps edge-to-edge behind the notch and under the status bar, we must access and control the window hosting the Dialog.

---

## 4. Technical Solution Breakdown

To override the system's default Dialog constraints, we employ a hybrid Compose-and-View-hierarchy approach inside `TimerScreen.kt`.

### A. Traversing to the Host Dialog Window
When Compose displays a `Dialog` or full-screen custom window, it instantiates an internal native Android `Dialog`. We can get a reference to this window by ascending the view hierarchy from the current local composition view:
```kotlin
val dialogView = LocalView.current
val dialogWindow = remember(dialogView) {
    var parent = dialogView.parent
    var windowProvider: DialogWindowProvider? = null
    while (parent != null) {
        if (parent is DialogWindowProvider) {
            windowProvider = parent
            break
        }
        parent = parent.parent
    }
    windowProvider?.window ?: (dialogView.context as? Activity)?.window
}
```

### B. Overriding Dialog Window Layout Flags
Once the raw `android.view.Window` is acquired, we apply key system flags dynamically inside a `LaunchedEffect`:
1. **Keep Screen On**: Prevent the phone from automatically sleeping during focus.
   ```kotlin
   dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
   ```
2. **Layout No Limits & Fullscreen**: Instruct the window layout engine to ignore safe-margins and draw pixels edge-to-edge over system bars.
   ```kotlin
   dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
   dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
   ```
3. **Short Edges Cutout Handling**: Tell the OS to render content straight through the camera notch or punch hole (API 28+).
   ```kotlin
   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
       dialogWindow.attributes = dialogWindow.attributes.apply {
           layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
       }
   }
   ```
4. **Transparent System Bars**: Clear status and navigation bar colors to transparent so they don't block layout rendering.
   ```kotlin
   dialogWindow.statusBarColor = android.graphics.Color.TRANSPARENT
   dialogWindow.navigationBarColor = android.graphics.Color.TRANSPARENT
   ```

### C. Hiding System Bars via WindowInsetsController
To completely remove clutter, we hide the status and navigation bars using the modern `WindowInsetsControllerCompat`. We configure it to hide all system bars, while allowing users to briefly swipe them into view (transient behavior):
```kotlin
val controller = WindowCompat.getInsetsController(dialogWindow, dialogWindow.decorView)
controller.hide(WindowInsetsCompat.Type.systemBars())
controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
```

### D. Forcing Root MATCH_PARENT Size
Even if a dialog's window size is expanded, the underlying platform View hierarchy may default to content-wrapping. We force the root platform layout parameters to `MATCH_PARENT` both vertically and horizontally:
```kotlin
val lp = dialogView.layoutParams
if (lp != null) {
    lp.width = WindowManager.LayoutParams.MATCH_PARENT
    lp.height = WindowManager.LayoutParams.MATCH_PARENT
    dialogView.layoutParams = lp
}
```

---

## 5. Complete Source Code Reference

The following snippet demonstrates the complete declaration of the Full-Screen Battery Saver Dialog overlay within `TimerScreen.kt`:

```kotlin
import android.app.Activity
import android.content.ContextWrapper
import android.graphics.Color
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun BatterySaverOverlay(
    onDismiss: () -> Unit,
    batteryLevel: Int,
    remainingTimeFormatted: String,
    ambientSoundTitle: String,
    onPlayPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    onSkipClick: () -> Unit,
    isPlaying: Boolean
) {
    // 1. Create a platform Dialog with edge-to-edge properties
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false // Fills maximum screen space
        )
    ) {
        val dialogView = LocalView.current
        
        // 2. Resolve the underlying Window using hierarchy traversal
        val dialogWindow = remember(dialogView) {
            var parent = dialogView.parent
            var windowProvider: DialogWindowProvider? = null
            while (parent != null) {
                if (parent is DialogWindowProvider) {
                    windowProvider = parent
                    break
                }
                parent = parent.parent
            }
            val window = windowProvider?.window
            if (window != null) {
                window
            } else {
                var context = dialogView.context
                while (context is ContextWrapper) {
                    if (context is Activity) {
                        break
                    }
                    context = context.baseContext
                }
                (context as? Activity)?.window
            }
        }

        // 3. Apply low-level window flags and full-screen controllers
        LaunchedEffect(dialogWindow, dialogView) {
            dialogWindow?.let { window ->
                // Keep the panel active without auto-sleep
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                
                // Allow overlaying status bars and punch holes
                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.attributes = window.attributes.apply {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }

                // Transparent status & navigation regions
                window.statusBarColor = Color.TRANSPARENT
                window.navigationBarColor = Color.TRANSPARENT

                WindowCompat.setDecorFitsSystemWindows(window, false)

                // Hide System Status Bars completely
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

            // Force match_parent layout bounds on the dialog frame container
            val lp = dialogView.layoutParams
            if (lp != null) {
                lp.width = WindowManager.LayoutParams.MATCH_PARENT
                lp.height = WindowManager.LayoutParams.MATCH_PARENT
                dialogView.layoutParams = lp
            }
        }

        // 4. Pure OLED Black Compose UI Shell
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black)
                .pointerInput(Unit) {
                    // Tap controls
                    detectTapGestures(
                        onDoubleTap = {
                            onDismiss() // Dismiss screen on double tap
                        }
                    )
                }
        ) {
            // Immersive components rendered here (Timer, battery %, media control buttons)
            // ...
        }
    }
}
```

---

## 6. Best Practices & Maintenance

- **Release Flags Properly**: When the overlay is dismissed, always clear flags like `FLAG_KEEP_SCREEN_ON` and `FLAG_LAYOUT_NO_LIMITS` on the dialog window to return the user's phone to its standard system configurations. This is handled dynamically via `onDispose` within `DisposableEffect`.
- **Contrast Ratios**: When rendering components inside the pitch-black layout, keep font colors in subtle tints (e.g., `#BBBBBB` or low-opacity whites) rather than high-intensity bright colors to further improve AMOLED efficiency.
- **Support Swiping to Leave**: Since navigation keys are completely hidden, ensure an intuitive exit mechanic is clearly described on screen (e.g., text showing `"Double tap anywhere to return"`).
