# Focus Island: Islands Design & Engineering Guide

This document provides a comprehensive technical overview and engineering guide of the **Focus Island** design, graphics rendering, gamification, and widget synchronization system. It covers how the application leverages custom Android Canvas pipelines, trigonometric layouts, and off-screen drawing to maintain a fluid, immersive, and mathematically synchronized 2.5D ecosystem on both the host application screens and the Android Home Screen widget.

---

## 🏛️ Architecture Overview

The system is designed with a highly modular, reactive architecture running on a local Room database with real-time UI/Widget state updates. The core architecture is broken into four distinct parts:

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Local SQLite (Room DB)                        │
└─────────────────────────────────┬──────────────────────────────────────┘
                                  │ Reactive Streams
                                  ▼
┌─────────────────────────────────┴──────────────────────────────────────┐
│                    Forest && Community ViewModels                      │
└────────────────┬──────────────────────────────────────┬────────────────┘
                 │ StateFlow                            │ Broadcast Trigger
                 ▼                                      ▼
┌────────────────┼──────────────────────┐ ┌─────────────┼────────────────┐
│  ForestBackground (2.5D Engine UI)     │ │ ExamCountdownWidgetReceiver   │
│  - 5-layer vector parallax            │ │ - Background Android Widget    │
│  - Wavy fog math, Fireflies particle  │ │ - Off-screen Canvas to Bitmap  │
│  - Light/Dark theme transitions       │ │ - Identical deterministic tree │
└───────────────────────────────────────┘ └──────────────────────────────┘
```

1. **Local State Engine**: `AppDatabase` tracks completed focus sessions and points.
2. **Dynamic UI Backdrop**: `ForestBackground` wraps around the target screen using `ForestScaffold`, placing a multi-layered parallax-style landscape behind interactive layers.
3. **Interactive Island Map**: `CommunityScreen` serves as a high-fidelity coordinates-based 2.5D canvas, computing and representing social community hubs.
4. **Offline Sync Widget**: `ExamCountdownWidgetReceiver` replicates the custom forest rendering within a standard `AppWidgetProvider` using programmatic canvas painting onto memory buffers (`Bitmap`), aligning home-screen visuals with the active in-app state.

---

## 🌲 Part 1: Dynamic 2.5D Forest Backdrop (`ForestBackground.kt`)

The underlying canvas utilizes a custom-drawn, five-layered parallax-style forest landscape, responding natively to the user's completed focus statistics (represented as `treeCount`).

### 1. 2.5D Multi-Layered Depth Shading
The forest scene is grouped into 6 logical depth layers (from index 0 to 5) providing natural scale perspective:
* **Background Layers (0 - 2)**: Scaled smaller, tinted in soft, desaturated hues (shading to teal/night blue) with higher density caps, representing dense distant groves.
* **Foreground Layers (3 - 5)**: Rendered larger, colored in highly-saturated organic forest green profiles with lower density caps, representing near trees.

Each layer holds a group of dynamically placed `DeterministicTree` items that use pseudo-random seeds mapped to unique tree hashes. This achieves a natural, organic look without introducing overlapping layout stuttering:

```kotlin
val seedX = (itemIndex * 19349663L) xor 0x5DEECE66DL
val seedH = (itemIndex * 38260237L) xor 0x5DEECE66DL
val seedW = (itemIndex * 85038241L) xor 0x5DEECE66DL
```

### 2. Aesthetic Shading, Ambient Glows & Day/Night Transitions
State transitions (such as waking or nightfalls) trigger beautiful, 3000ms long crossfades on the canvas colors:
* **Sun/Moon Cycles**: Features a warm yellow radial gradient glow with a golden body during the day, which shifts cleanly to a midnight crescent moon backed by deep indigo celestial corona halos at night.
* **Shaded Depth**: Every tree drawn in detailed mode is rendered in three distinct passes to simulate organic 3D light wrapping:
  1. **Full Canopy Path**: Painted in the direct depth-layer background scale color.
  2. **Right-Side Shadow Path**: Overlayed on the right half of the tree canopy using a semi-transparent, deep shadow brush (`Color.Black.copy(alpha = 0.22f) / 0.28f`).
  3. **Left Rim Highlight Path**: Overlayed on the left boundary using soft glow highlights (`Color.White.copy(alpha = 0.12f)`) to make the canopy borders pop.

### 3. Trigonometric Weather & Fluid Dynamics
To bring the environment to life, the canvas implements animated overlays powered by a ticking state loop:
* **Wavy Fog/Mist Rendering**: Programmatic fog is drawn as a closed-loop polygon. It calculates its vertical outline across 25 horizontal steps using multi-wavelength trigonometric wave summation:
  $$\text{waveY} = \text{baseY} + 20 \cdot \sin(0.005x + 1.5\phi) + 12 \cdot \cos(0.012x - 0.7\phi) + 6 \cdot \sin(0.025x + 2.2\phi)$$
* **Bioluminescent Particle Sparks (Fairy Fireflies)**: At night, trees trigger magical floating gold and lavender bioluminescent sparks. They drift upwards following a combined sine-cosine wave trajectory relative to their anchor coordinates:
  $$\Delta x = \sin(\psi) \cdot w_{\text{tree}} \cdot 0.6 + \cos(0.5\psi) \cdot w_{\text{tree}} \cdot 0.2$$
  $$\Delta y = -((\psi \cdot 15 + p \cdot 30) \pmod{h_{\text{tree}}})$$

### 4. GPU Optimization Practices
* **Path Caching**: Tree paths (`Path()`), fog geometry, and highlights are recycled and cached across frame compositions (`cachedTreePath.reset()`), cutting resource allocations to zero during frame rendering.
* **Adaptive Detailing**: High tree count milestones trigger a fallback option to `drawSimplifiedPineTree` containing 3 simplified canopy tiers instead of 14, protecting high frame rates on lower-spec mobile units.
* **Lifecycle-Aware Driving**: `DisposableEffect` tracks user screen visibility (`LifecycleEventObserver`), freezing drawing threads and ticker state animations instantly when the app is minimized.

---

## 🏝️ Part 2: Interactive 2.5D Community Island Map (`CommunityScreen.kt`)

The community dashboard is an aesthetic, interactive 2.5D coordinate space. Users can view their own customized personal cove, see neighbor/friend islands, check stats via frosted-glass overlays, and inspect active focus study states.

### 1. Trigonometrical Island Orbits
The map tab displays a visual coordinate center where player islands orbit around the parent terminal island. Distances and visual spreads are calculated dynamically based on custom ratios of the global drawing canvas bounds:

```kotlin
private fun islandPosition(index: Int, total: Int, size: Size): Offset {
    val totalCount = if (total == 0) 1 else total
    val angle = (2 * Math.PI / totalCount * index) - Math.PI / 2
    val radius = minOf(size.width, size.height) * 0.35f
    return Offset(
        x = size.width / 2f + (cos(angle) * radius).toFloat(),
        y = size.height / 2f + (sin(angle) * radius).toFloat()
    )
}
```

* **Central Cove (`YOUR COVE`)**: Anchored securely to the visual center of the coordinate system `Offset(size.width / 2f, size.height / 2f)`.
* **Outer Islands (`FRIEND COVES`)**: Distributed symmetrically along circular outer radial trails utilizing trig division.

### 2. 2.5D Island Topography
Each island element represents an island landmass painted in nested layers:
1. **Island Drop Shadow**: A dark, translucent circle shifted downwards and rightwards by an offset vector (`Offset(6f, 6f)`) of the radius size.
2. **Sandy Coastline Ring**: Main circle representing the sand ring, painted in sandy-gold hues (`Color(0xFFD4A853)` during day or `Color(0xFF6B5D39)` at night).
3. **Vegetative Grass Core**: A concentric offset circle representing the fertile inner island grass core. It is shrunk to `82%` radius and shifted upward by a minor offset of the radius dimension to mimic vertical 2.5D perspective height.
4. **Decorative Miniature Groves**: Tiny layered tree nodes (drawn with a trunk and leafy top) are arranged around the grass core. Their density directly scales with the user's focus stats (`treeCount`), allowing players to visually recognize high-performing friends at a glance.

### 3. Canvas Hitbox Vector Math
To avoid using heavy standard Compose UI nodes for thousands of points, user interaction on the custom vector map is resolved manually in the canvas layer. Tap coordinates are evaluated using simple distance metrics against calculated visual centers:

```kotlin
.pointerInput(friends) {
    detectTapGestures { tap ->
        friends.forEachIndexed { i, friend ->
            val pos = islandPosition(i, friends.size, Size(size.width.toFloat(), size.height.toFloat()))
            // Resolve if tap coordinates fall inside selection radius (90 pixels standard)
            if ((tap - pos).getDistance() < 90f) {
                selectedFriend = friend
            }
        }
    }
}
```

### 4. Interactive Frosted Glass Cards (`GlassCard.kt`)
Selected islands show detailed statistics using sleek frosted-glass overlay layouts.
* **Aesthetic Pairing**: Built with high backdrop blur transparency effects, custom thin borders, and dark visual themes to let the radial sea background show through.
* **Live Focus Waveform**: If a friend is currently active in a study cycle with a radio stream playing, the glass panel triggers a live, multi-bar synchronized graphic equalizer wave (`PlayingWaveIndicator.kt`).

---

## 📲 Part 3: Offline Home Screen Widget Sync (`ExamCountdownWidgetReceiver.kt`)

Keeping dynamic visuals updated at the Android OS level requires a specialized widget syncing engine. The countdown and focus widget provides real-time status updates from the home screen using a headless graphics pipeline.

### 1. Programmatic Canvas-to-Bitmap Pipeline
Because remote widget layouts (`RemoteViews`) cannot run Jetpack Compose UI trees natively, the widget engine spins up an off-screen unitless drawing canvas in a background coroutine:

```kotlin
// Create a hardware-backed Bitmap drawing canvas
val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
val androidCanvas = android.graphics.Canvas(bitmap)
val composeCanvas = Canvas(ImageBitmap(width, height)) // Wrapped Compose instance

val drawScope = CanvasDrawScope()
drawScope.draw(
    density = Density(context),
    layoutDirection = LayoutDirection.Ltr,
    canvas = composeCanvas,
    size = Size(width.toFloat(), height.toFloat())
) {
    // Programmatic draw tree layers matching in-app assets exactly!
}
```

* This pipeline generates a high-performance **Bitmap** off-screen.
* It uses the same mathematical layouts (`DeterministicTree`) and identical depth-color formulas (`Color(0xFF286532)`) as the host app, bypassing system layout limitations.
* Once generated, the Bitmap is bound directly to the widget's layout image placeholder:  
  `views.setImageViewBitmap(R.id.widget_background, forestBitmap)`

### 2. Dynamic Widget Theme Adapters
Depending on the user's local clock state (6:00 to 17:59 = Day, otherwise Night), the widget dynamically shifts both its vector background and layout theme colors:
* **Day Theme**: Paints a light sky gradient, soft blue-grey mist, and switches widget texts to a sharp, high-contrast forest green theme (`#1B4324`) on light overlay backgrounds.
* **Night Theme**: Draws deep night starry skies, high stellar clusters, glowing moons, and changes text displays to highly legible glowing white and muted blue palettes to prevent screen glare.

---

## 🛠️ Island Design Rules for Developers

When modifying or expanding the island and forest mechanics, follow these development guidelines:

1. **Keep Mathematical Consistency**: Ensure any changes to tree coordinate math, shapes, or colors in `/app/src/main/java/com/example/ui/components/ForestBackground.kt` are accurately ported to `getForestWidgetBitmap` within `/app/src/main/java/com/example/widget/ExamCountdownWidgetReceiver.kt`. This ensures the home screen widget and the active app screen always look identical.
2. **Optimize Path Operations**: Avoid allocating `Path()` objects inside the `Canvas` draw execution loop. Realize allocations lazily at compile/composition time and perform clean resets (`path.reset()`) during tick updates.
3. **Respect 2.5D Perspective**: When positioning island elements or custom canvas decorations, always scale width and height dimensions relative to drawing bounds. Avoid using hardcoded offsets that break layout responsiveness on landscape tablets or folding devices.
4. **Follow Theme Specs**: Apply Material Design 3 and frosted glass layouts for overlay widgets. This keeps the aesthetic clean, spacious, and highly legible against dark backgrounds.
