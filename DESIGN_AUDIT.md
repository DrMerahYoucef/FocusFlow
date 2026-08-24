# FocusFlow UI Design Audit

## 1. App Overview
- **Description**: All-in-one productivity and study app combining a Pomodoro focus timer with ambient soundscapes and study radio, spaced repetition (SRS) flashcards with camera OCR/cropping, daily tree-planting gamification with analytics, social leaderboard/island map, app blocking, exam countdowns, and home screen widgets.
- **Target user**: High school and university students, self-learners, and knowledge workers studying for exams or requiring structured deep-work focus blocks.
- **UI toolkit**: Jetpack Compose (100% declarative UI for in-app screens; XML Views + Canvas RemoteViews strictly for Android App Widgets).
- **Design system**: Material 3 base (`androidx.compose.material3`) augmented with a custom dual-paradigm layer: Neumorphic soft-bevel styling (`NeumorphicColors`, `Modifier.neumorphicShadow`) and Glassmorphic frosted translucent styling (`GlassCard`, `GlassButton`, `WidgetTheme`). Applied inconsistently across screens.
- **SDK versions**: Min SDK `26` (Android 8.0 Oreo), Target SDK `35` (Android 15), Compile SDK `35`.
- **Dark mode**: Supported. Dynamic switching via system dark mode flag and `LocalAppThemeColors`. Includes a dedicated pure-black AMOLED Battery Saver mode.
- **Third-party UI / Graphics libraries**:
  - `io.coil-kt:coil-compose` (2.5.0) for image loading and thumbnails
  - `androidx.camera:camera-camera2`, `androidx.camera:camera-lifecycle`, `androidx.camera:camera-view` (1.3.1) for camera capture viewfinder
  - `com.google.mlkit:text-recognition` (16.0.0) for OCR bounding boxes and text extraction
  - `androidx.media3:media3-exoplayer` (1.2.1) for audio streaming visualizer state
  - `com.google.android.material:material` (1.11.0) for platform components and widget attributes

---

## 2. Design Tokens (current state)

### Colors
Defined in `NeumorphicColors.kt`, `Color.kt`, `Theme.kt`, and `WidgetPalette.kt`:

#### Core & Semantic Tokens
- `Primary`: `#6C5CE7` (Light theme main accent, buttons, active progress rings, selected chips)
- `PrimaryDark`: `#8B7CF8` (Dark theme main accent, elevated highlights)
- `Secondary`: `#A29BFE` (Light pastel lavender, subtitle chips, secondary progress bars)
- `SecondaryDark`: `#B8B2FF` (Dark pastel lavender)
- `Accent Coral`: `#FF7675` (Muted coral red, break indicators, streak flames, favorite badges)
- `Background Light`: `#E0E5EC` (Neumorphic canvas grey, window surface)
- `Background Dark`: `#1E2026` (Neumorphic dark slate canvas)
- `Surface Light`: `#E0E5EC` (Default light card face) / `#E8ECF2` (Theme light card face variant)
- `Surface Dark`: `#252830` (Default dark card face) / `#2C303A` (Theme dark card face variant)
- `Text Primary (Light)`: `#2D3436` (High-contrast dark charcoal text)
- `Text Primary (Dark)`: `#F5F6FA` (High-contrast off-white text)
- `Text Secondary (Light)`: `#636E72` (Medium neutral grey text)
- `Text Secondary (Dark)`: `#A4B0BE` (Muted cool grey text)
- `Success`: `#00B894` (Emerald green, session completions, easy SRS grade, healthy tree nodes)
- `Warning`: `#FDCB6E` (Marigold amber yellow, hard SRS grade, points badge)
- `Error / Critical`: `#D63031` (Crimson red, again SRS grade, delete account action)
- `Due Badge Red`: `#DC2626` (Bright red, flashcard due count corner pill on deck chips)
- `OLED Pure Black`: `#000000` (100% black, Battery Saver mode, camera viewfinder scrim, crop editor canvas)

#### Hardcoded Hex Variants Found Across Screens
- `#9482FF` (Timer battery saver focus arc)
- `#8BCA9A` (Timer battery saver short break arc)
- `#FF829C` (Timer battery saver long break arc)
- `#55E6C1` (Flow analytics time badge)
- `#74B9FF` (Session history tag blue)
- `#FF9F43` (Morning block orange / SRS hard grade orange)
- `#0984E3` (Afternoon block blue)
- `#00CEC9` (Night block teal)
- `#FFD23F` (Flow analytics star yellow)
- `#6750A4` (Settings screen matrix widget action button purple)
- `#0C0C0C`, `#0F0F12`, `#141414`, `#1D1D1D` (Battery saver container dark shades)

#### Widget Color Palette (`WidgetPalette.kt` & `colors.xml`)
- `wgt_surface_base (Day)`: `#E8EDF5` (Composited at 55% alpha: `#8CE8EDF5`)
- `wgt_surface_base (Night)`: `#0B0F17` (Composited at 60% alpha: `#990B0F17`)
- `wgt_on_surface (Day)`: `#0F172A` (Contrast guarded >= 4.5:1)
- `wgt_on_surface (Night)`: `#F1F5F9` (Contrast guarded >= 4.5:1)
- `wgt_on_surface_variant (Day)`: `#475569`
- `wgt_on_surface_variant (Night)`: `#94A3B8`
- `wgt_accent (Day)`: `#4F46E5` (Indigo)
- `wgt_accent (Night)`: `#818CF8` (Soft Indigo)
- `wgt_accent_soft`: `#A5B4FC` (Day) / `#6366F1` (Night)
- `wgt_critical`: `#EF4444` (Day/Night overdue text)
- `wgt_rim_base`: `#FFFFFF` at 55% alpha (Day) / `#FFFFFF` at 20% alpha (Night)
- `wgt_divider`: `#0F172A` at 12% alpha (Day) / `#F1F5F9` at 15% alpha (Night)

### Typography
- **Font family**: System Default Sans-Serif (`FontFamily.Default` / Roboto).
- **Global override**: `LocalDensity` is locked to `fontScale = 1.0f` in `Theme.kt`.
- **Text styles and usages**:
  - `Display Large`: `72sp` or `64sp`, Weight `Thin` (100) or `ExtraLight` (200), Line Height `72sp`, Letter Spacing `0sp`. Used for: Main timer countdown digits (`TimerScreen`, `BatterySaverScreen`).
  - `Display Medium`: `56sp`, Weight `Thin` (100). Used for: Compact timer countdown display.
  - `Headline Large`: `24sp`, Weight `Black` (900), Letter Spacing `4sp` (ALL CAPS). Used for: `ANALYTICS` screen title.
  - `Headline Medium`: `24sp`, Weight `Bold` (700), Letter Spacing `0sp`. Used for: `Revision Flashcards` screen header, `AuthScreen` welcome title.
  - `Headline Small`: `20sp`, Weight `Black` (900), Letter Spacing `2sp` (ALL CAPS). Used for: `DASHBOARD CONFIG` settings header.
  - `Title Large`: `20sp` / `22sp`, Weight `Bold` (700), Letter Spacing `0sp`. Used for: Flashcard front question, session complete title, exam modal titles.
  - `Title Medium`: `16sp` / `18sp`, Weight `SemiBold` (600), Letter Spacing `0.15sp`. Used for: Card titles, section headers, dialog titles.
  - `Body Large`: `14sp` / `15sp`, Weight `Normal` (400) / `Medium` (500), Line Height `20sp`, Letter Spacing `0.25sp`. Used for: Flashcard answer markdown body, descriptions, note previews.
  - `Body Small`: `12sp` / `13sp`, Weight `Normal` (400), Line Height `16sp`, Letter Spacing `0.4sp`. Used for: Subtitles, timestamps, helper labels, empty state captions.
  - `Label Large`: `13sp` / `14sp`, Weight `Bold` (700), Letter Spacing `1.0sp` to `1.5sp`. Used for: Action buttons (`NeumorphicButton`, `GlassButton`), filter chips.
  - `Label Small / Micro`: `9sp`, `10sp`, `11sp`, Weight `Bold` (700), Letter Spacing `0.5sp` to `2sp` (ALL CAPS). Used for: Category tags, micro badges, corner due count badge, widget column headers.

### Spacing
Dimensions and occurrences across composables:
- `0.dp`: Fullscreen viewfinders, edge-to-edge canvasses (8 occurrences)
- `2.dp`: Micro divider margins, badge vertical paddings (18 occurrences)
- `3.dp` / `4.dp`: Inner chip offsets, mini indicator spacings, stroke widths (42 occurrences)
- `6.dp` / `7.dp`: Badge corner paddings, micro button row gaps (24 occurrences)
- `8.dp`: Standard component gap, chip spacing, icon-to-text margin (136 occurrences)
- `10.dp`: Intermediate card padding, island avatar margins (19 occurrences)
- `12.dp`: Inner card padding, list item vertical gaps, stat grid spacing (88 occurrences)
- `14.dp`: Text field paddings, search bar insets (15 occurrences)
- `16.dp`: Standard screen horizontal padding, card content insets, list dividers (164 occurrences)
- `20.dp`: Large card paddings, modal dialog internal insets, screen edge margins (72 occurrences)
- `24.dp`: Outer screen padding, bottom sheet top insets (54 occurrences)
- `32.dp`: Section group separators, hero header margins (28 occurrences)
- `48.dp`: Minimum interactive touch target boundary (32 occurrences)

### Shape (Corner Radii)
- `CircleShape` (`50%`): Play/pause buttons, FABs, avatar nodes, indicator dots, corner due badges.
- `32.dp`: Modal bottom sheet top corners (`FlowAnalyticsSheet`).
- `24.dp`: Flashcard flip card, modal dialogs, widget outer card background.
- `20.dp`: Daily tree summary card, audio dialog container, hero banners.
- `16.dp`: Standard `NeumorphicCard`, `GlassCard`, list item containers.
- `14.dp`: `OutlinedTextField`, search input boxes.
- `12.dp`: Standard buttons, preset duration chips, expandable section headers.
- `8.dp` / `10.dp`: Micro tags, category labels, interval badges.

### Elevation & Shadow
Custom dual-light source bevel shadows via `Modifier.neumorphicShadow`:
- `0.dp`: Pressed buttons, flat inset input fields, disabled buttons.
- `2.dp`: Interactive filter chips, subtle badges.
- `4.dp`: Standard list items, expandable accordion cards.
- `6.dp`: Elevated cards, daily tree container.
- `8.dp` to `12.dp`: Main timer center dial, primary action FABs.
- `Light theme offset / blur`: Top-Left `(-elevation, -elevation)` with `#FFFFFF` at 90% alpha; Bottom-Right `(+elevation, +elevation)` with `#A3B1C6` at 60% alpha; blur radius `elevation * 1.5f`.
- `Dark theme offset / blur`: Top-Left `(-elevation, -elevation)` with `#2C303A` at 60% alpha; Bottom-Right `(+elevation, +elevation)` with `#141519` at 80% alpha; blur radius `elevation * 1.5f`.

### Iconography
- **Icon set**: Material Symbols / Material Icons (`androidx.compose.material.icons.Icons.Default`, `Filled`, `Outlined`, `AutoMirrored`).
- **Standard icon sizes**:
  - Micro / inline: `12.dp` / `14.dp`
  - Standard action / list item: `20.dp` / `24.dp`
  - Playback / dial controls: `32.dp` / `36.dp`
  - Hero illustration / empty state: `48.dp` / `64.dp`

---

## 3. Navigation Map

### Destinations & Routing
- `timer` (Home)
  - `timer → radio` (via: tap Radio icon on top bar)
  - `timer → BatterySaverOverlay` (via: tap Battery icon on top bar or inactivity timeout)
  - `timer → AudioSelectorDialog` (via: tap soundscape pill or speaker icon)
  - `timer → TreePlantedCelebration` (via: automatic trigger upon 100% Pomodoro cycle completion)
- `revisions` (Flashcards Home)
  - `revisions → revisions_capture` (via: tap FAB → Camera OCR Card)
  - `revisions → revisions_crop/{imagePath}` (via: gallery photo pick or camera capture)
  - `revisions → revisions_session/{deckId}` (via: tap "Review" button on header or deck row)
  - `revisions → revisions_deck/{deckId}` (via: tap deck card in manage list)
  - `revisions → revisions_note/{noteId}` (via: tap flashcard list item)
- `analytics` (Stats & Daily Trees)
  - `analytics → day_detail_modal` (via: tap specific date tree node)
- `community` (Island Map & Leaderboard)
  - `community → friend_profile_glasscard` (via: tap avatar pin on island canvas)
- `settings` (Dashboard Configuration)
  - `settings → exams` (via: tap "Exam Workload" section item)
  - `settings → app_blocker` (via: tap "App Blocker" section item)
  - `settings → auth` (via: tap "Log Out" or "Switch Account")
- `BatterySaverOverlay`
  - `BatterySaverOverlay → FlowAnalyticsSheet` (via: swipe up gesture from bottom)
  - `BatterySaverOverlay → timer` (via: double tap gesture or back button)

### Navigation Pattern
- Persistent bottom navigation bar hosted inside `ForestScaffold` displaying 5 tabs:
  1. `Timer` (`Icons.Default.Timer`)
  2. `Revisions` (`Icons.Default.School`)
  3. `Analytics` (`Icons.Default.BarChart`)
  4. `Community` (`Icons.Default.Public`)
  5. `Settings` (`Icons.Default.Settings`)
- Secondary screens (`revisions_capture`, `revisions_crop`, `revisions_session`, `radio`, `exams`, `app_blocker`, `auth`) push onto the NavController stack without the bottom navigation bar.

### Cold Launch Behavior
- First run (Unauthenticated): Starts at `auth` (AuthScreen) requesting Google Sign-In or guest continuation.
- Returning user (Authenticated): Starts at `timer` (TimerScreen).

### System Back Handling
- Top-level 5 tabs: Exits app / minimizes to Android launcher.
- Secondary screens: Pops backstack to previous route.
- `CaptureScreen` & `CropEditorScreen`: Cancels camera/crop operations and returns to `revisions`.
- `RevisionSessionScreen`: Prompts confirmation dialog before exiting in-progress review session.
- `BatterySaverOverlay`: Intercepted to dismiss overlay and restore standard `TimerScreen`.

---

## 4. Screen Inventory

---

### Screen: Timer Screen (Home)
- **Purpose**: Manage and run Pomodoro focus cycles, switch task categories, adjust study audio, and track daily streaks.
- **Entry points**: Cold launch (returning user), tap "Timer" bottom navigation tab, or dismiss Battery Saver.
- **Exit points**: Tap top bar icons (`radio`, `battery_saver`), tap soundscape selector, or switch bottom bar tab.
- **Layout, top to bottom**:
  - Status bar: Transparent with dark/light icons matching theme.
  - Top header row (`padding: 20.dp` H, `12.dp` V):
    - Left: Fire streak icon (`#FF9F43`, `20.dp`) + text `${streak}d` (`13.sp Bold`).
    - Center: Capsule phase switcher (`Focus`, `Short Break`, `Long Break`, height `36.dp`, corner radius `18.dp`).
    - Right: Battery saver toggle button (`40.dp x 40.dp`) and Study Radio button (`40.dp x 40.dp`).
  - Spacing: `24.dp` gap.
  - Center Dial Section (centered vertically):
    - Neumorphic circular dial (280dp x 280dp outer box, 8dp bevel shadow).
    - Progress ring Canvas (260dp diameter, 12dp stroke width, round caps).
    - Dial center content stack:
      - Phase label: `14.sp Bold`, uppercase, tracking `2.sp`.
      - Time digits: `56.sp Thin`, line height `56.sp`.
      - Task category chip: Clickable pill (height `28.dp`, corner radius `14.dp`, icon + text).
      - Audio status chip: Mini speaker icon + stream name or "Silence 🔇".
  - Spacing: `32.dp` gap.
  - Controls row (centered horizontally):
    - Reset button: Circular `NeumorphicButton` (52dp x 52dp, Stop icon `24.dp`).
    - Main toggle button: Circular `NeumorphicButton` (72dp x 72dp, Play/Pause icon `36.dp`, tint `#6C5CE7`).
    - Skip button: Circular `NeumorphicButton` (52dp x 52dp, SkipNext icon `24.dp`).
  - Spacing: `20.dp` gap.
  - Quick duration presets row: Horizontal row of 4 capsule buttons (`15m`, `25m`, `45m`, `60m`, height `32.dp`).
  - Bottom bar: Persistent 5-tab navigation bar.
- **Primary action**: Play/Pause circular button (middle third, center).
- **Secondary actions**: Reset, Skip, phase selector chips, duration preset pills, category selector, battery saver toggle, radio launcher.
- **States**:
  - `loading`: MISSING.
  - `empty`: Shows default task "General Focus" with book icon.
  - `error`: MISSING.
  - `success`: Triggers `TreePlantedCelebration` modal.
  - `offline`: Operates fully offline; local timer ticks via coroutines.
- **Data shown**: Remaining seconds, active phase name, current task category, audio stream title, daily streak days.
- **Animations/transitions**: Animated progress sweep angle (`animateFloatAsState`), pulsating center glow when active, button scale compression (`0.96f`) on press.
- **Known problems**: 280dp dial overflows vertically on smaller Android devices (screens < 640dp height) when keyboard opens or on compact landscape mode.

---

### Screen: Battery Saver / AMOLED Overlay
- **Purpose**: Maximize battery efficiency on OLED screens during long study blocks with low-luminance UI and quick gesture controls.
- **Entry points**: Tap battery icon on Timer screen or auto-trigger after idle duration.
- **Exit points**: Double-tap screen, swipe back, or tap exit button.
- **Layout, top to bottom**:
  - Background: 100% Solid Black (`#000000`).
  - Top status bar (`16.dp` padding):
    - Left: Text `BATTERY SAVER ACTIVE 🔋` (`11.sp Bold`, tint `#60FFFFFF`).
    - Right: Battery percentage readout (`$batteryLevel%`, `13.sp Bold`).
  - Center stage:
    - 280dp Box with 260dp thin arc Canvas (6dp stroke, dimmed colored arc).
    - Phase label: `14.sp Bold`, letter spacing `3.sp`.
    - Time digits: `64.sp Thin`, `#FFFFFF`.
    - Audio stream title: `13.sp Medium`, `#60FFFFFF`.
  - Audio mode selector capsule: 3-segment pill (`Radio`, `Ambient`, `Mute`, height `32.dp`).
  - Stats row: `COMPLETED: $sessions` | `FOCUS TIME: $minutes m` (`13.sp SemiBold`).
  - Bottom playback controls: Dimmed Play/Pause (54dp) and Stop (54dp) border buttons (`#10FFFFFF` fill, `1.dp #40FFFFFF` rim).
  - Bottom peek bar: `Next milestone: 50%` + `All Notifications Dimmed 🤫` (`11.sp`).
- **Primary action**: Play/Pause button (bottom third).
- **Secondary actions**: Audio mode toggle, double-tap dismiss, swipe up for analytics, right-edge brightness slider.
- **States**:
  - `loading`: MISSING.
  - `empty`: Shows 0 sessions.
  - `error`: MISSING.
  - `success`: Smooth color shift on phase completion.
  - `offline`: 100% offline.
- **Data shown**: Countdown time, battery level %, sessions count, total minutes, audio status.
- **Animations/transitions**: 0.04f alpha radial gradient breath animation; swipe-up transition to `FlowAnalyticsSheet`.
- **Known problems**: Brightness slider on right edge can conflict with Android system back gesture navigation.

---

### Screen: Revisions / Flashcards Home Screen
- **Purpose**: Manage flashcard decks, search cards, monitor due counts, create cards via OCR/voice/text, and launch spaced repetition review sessions.
- **Entry points**: Tap "Revisions" bottom navigation tab.
- **Exit points**: Tap "Review" button (`revisions_session`), tap card item (`revisions_note`), tap deck (`revisions_deck`), tap FAB items (`revisions_capture`).
- **Layout, top to bottom**:
  - Header row (`20.dp` H, `16.dp` V):
    - Title: `Revision Flashcards` (`24.sp Bold`).
    - Subtitle: `${dueCount} due for review today` (`13.sp TextSecondary`).
    - Right button: `Review` button (Height `40.dp`, Primary `#6C5CE7`, Play icon + text).
  - Spacing: `12.dp` gap.
  - Search bar: `OutlinedTextField` (`14.dp` rounded corners, leading Search icon, trailing Clear icon).
  - Spacing: `12.dp` gap.
  - Decks horizontal scroll row (`LazyRow`, `8.dp` spacing):
    - "All" filter chip with top-right red badge pill (`#DC2626`, 10sp Bold text).
    - Individual deck chips with top-right red badge pills showing due count per deck.
    - "Add Deck" chip (`+` icon, outlined style).
  - Spacing: `12.dp` gap.
  - Flashcard list (`LazyColumn`, `12.dp` spacing, content padding `bottom = 88.dp`):
    - Card items: `NeumorphicCard` (`16.dp` corner radius, `4.dp` elevation).
    - Item content: Type icon or 48dp x 48dp thumbnail, Title (`16.sp Bold`), preview snippet (`13.sp`, max 2 lines), due status tag, next interval badge.
  - Floating Action Button (bottom-right, `16.dp` margin): Speed dial FAB (`+` icon expanding into Camera OCR, Gallery, Voice, Manual).
- **Primary action**: "Review" button (top-right header) or FAB `+` (bottom-right).
- **Secondary actions**: Search input, deck filter chips, flashcard list items, speed dial options.
- **States**:
  - `loading`: Centered circular progress indicator.
  - `empty`: Centered `School` icon (`48.dp`), text "No flashcards in this deck yet", "Create First Card" button.
  - `error`: MISSING.
  - `success`: Flashcards list populated.
  - `offline`: Full local Room database access.
- **Data shown**: Total card count, due card count, deck names, card titles, answer snippets, intervals.
- **Animations/transitions**: Speed dial expansion stagger animation; red badge entrance scale transition.
- **Known problems**: Large decks (> 500 cards) without pagination experience minor frame drops when scrolling fast with uncompressed image thumbnails.

---

### Screen: Capture Screen (Camera Viewfinder)
- **Purpose**: Capture textbook pages, handwritten notes, or exam sheets for OCR extraction and flashcard creation.
- **Entry points**: Tap "Camera OCR Card" from Revisions speed dial FAB.
- **Exit points**: Tap Back arrow (returns to Revisions), or take photo (navigates to `CropEditorScreen`).
- **Layout, top to bottom**:
  - Viewfinder: Fullscreen CameraX `PreviewView` (100% black background).
  - Top overlay bar (`16.dp` padding, `statusBarsPadding()`):
    - Back button: Circular icon button (`Icons.Default.ArrowBack`, tint white).
    - Flash toggle: Circular icon button (`FlashOn` / `FlashOff`, tint white).
    - Manual mode shortcut: Text button `Manual`.
  - Center: Framing guide crosshairs (subtle white border rect, `2.dp` dash).
  - Bottom overlay bar (`24.dp` padding, `navigationBarsPadding()`):
    - Left: Gallery picker button (Circular button, `PhotoLibrary` icon, `52.dp`).
    - Center: Shutter button (76dp x 76dp outer white ring, 64dp inner white filled circle).
    - Right: Voice record toggle button (Circular button, `Mic` icon, `52.dp`).
- **Primary action**: Shutter button (bottom third, center).
- **Secondary actions**: Gallery picker, flash toggle, back navigation.
- **States**:
  - `loading`: Black viewfinder with spinner during camera binding.
  - `empty`: MISSING.
  - `error` / `permission_denied`: Centered message "Permission caméra requise" with "Accorder l'accès" button.
  - `success`: Camera feed active.
  - `offline`: Works fully offline.
- **Data shown**: Live camera preview stream.
- **Animations/transitions**: Shutter button press scale-down (`0.88f`) and flash screen white blink.
- **Known problems**: Permission denied message is hardcoded in French ("Permission caméra requise") while the app default is English.

---

### Screen: Crop Editor Screen
- **Purpose**: Crop captured image using Rectangle, Lasso, or Quadrilateral perspective tools and run ML Kit OCR text recognition.
- **Entry points**: Take photo in `CaptureScreen` or pick image from device gallery.
- **Exit points**: Tap "Cancel" (returns to Revisions/Capture), or tap "Process OCR & Save" (opens save dialog).
- **Layout, top to bottom**:
  - Background: Pure Black (`#000000`).
  - Top mode selector bar (`12.dp` padding, `statusBarsPadding()`):
    - Row of 3 filter chips: `Rectangle`, `Lasso`, `Quadrilateral` (Height `32.dp`, active chip `#6C5CE7`).
  - Center Canvas: Interactive pinch/drag bitmap viewport with semi-transparent black scrim (`0.6f` alpha) and draggable corner/edge handles (Radius `12.dp`, stroke `2.dp` white).
  - Bottom action bar (`16.dp` padding, `navigationBarsPadding()`):
    - Left: "Cancel" text button (white text).
    - Right: "Process OCR & Save" FAB (Primary `#6C5CE7`, Check icon `28.dp`).
- **Primary action**: "Process OCR & Save" FAB (bottom-right).
- **Secondary actions**: Mode selector chips, drag handles, "Cancel" button.
- **States**:
  - `loading`: Fullscreen modal spinner with text "Extracting text with OCR...".
  - `empty`: MISSING.
  - `error`: Toast "Impossible de charger l'image" if file corrupted.
  - `success`: Opens Card Save Dialog with pre-filled question and OCR extracted answer markdown.
  - `offline`: ML Kit runs on-device without network.
- **Data shown**: Cropped image bitmap preview, interactive crop path.
- **Animations/transitions**: Smooth corner pin snapping; mode switcher fade transition.
- **Known problems**: Quadrilateral perspective transformation does not un-warp trapezoidal distortion on the output bitmap; it only crops the bounding box.

---

### Screen: Revision Session Screen (Active SRS Review)
- **Purpose**: Review flashcards using the SuperMemo SM-2 spaced repetition algorithm with front/back card flip and 4-grade rating.
- **Entry points**: Tap "Review" button on Revisions home screen or deck detail screen.
- **Exit points**: Tap Back icon (exit prompt dialog) or complete all cards (shows Session Complete card).
- **Layout, top to bottom**:
  - Top progress header (`16.dp` padding):
    - Left: Back icon button.
    - Center: Progress text `Card $currentIndex / $totalCount` (`14.sp Bold`).
    - Bottom: Linear progress bar (`4.dp` height, primary `#6C5CE7`).
  - Spacing: `16.dp` gap.
  - Center stage: 3D Flip Card Container (`Modifier.fillMaxWidth().weight(1f).padding(16.dp)`):
    - `NeumorphicCard` (`24.dp` corner radius, `8.dp` elevation).
    - **Front layout**:
      - Deck label badge (`11.sp Bold`, top-left).
      - Prompt / Question text (`20.sp Bold`, centered, Markdown formatted).
      - Cropped image attachment thumbnail (if present, tap to zoom fullscreen).
      - Bottom hint: "Tap card to reveal answer" with Flip icon (`12.sp`).
    - **Back layout** (visible upon flip):
      - Green badge "ANSWER" (`11.sp Bold`).
      - Answer body text / Markdown (`15.sp Normal`, scrollable).
      - Audio playback button (if voice note attached).
  - Spacing: `16.dp` gap.
  - Bottom Grading Bar (visible only when card is flipped, `padding: 16.dp`):
    - 4-button horizontal row (`weight(1f)` each, height `48.dp`, corner radius `12.dp`):
      1. `Again` (Red `#D63031`, interval `<10m`)
      2. `Hard` (Orange `#FF9F43`, interval `1d`)
      3. `Good` (Primary `#6C5CE7`, interval `3d`)
      4. `Easy` (Emerald `#00B894`, interval `7d`)
- **Primary action**: Card flip tap (middle third) -> Rating button tap (bottom third).
- **Secondary actions**: Back icon, image zoom tap, audio replay tap.
- **States**:
  - `loading`: Spinner while loading deck cards.
  - `empty`: "No cards due for review in this deck today!"
  - `error`: MISSING.
  - `success` / `completed`: Green checkmark icon (`64.dp`, `#00B894`), headline `Session complete!`, text `You've reviewed every card scheduled for this deck.`, and "Back to Decks" button.
  - `offline`: Full offline functionality.
- **Data shown**: Card question, card answer, image URI, audio recording, SRS interval preview labels.
- **Animations/transitions**: 3D Y-axis card flip animation (`rotationY` spring animation from 0° to 180°), grading buttons slide-up entrance transition.
- **Known problems**: Long markdown answers containing large tables or long code snippets can push the rating buttons down or cause layout clipping on small screens if vertical scroll is not engaged.

---

### Screen: Analytics Screen
- **Purpose**: Review historical focus time, points earned, session streaks, and browse the daily planted forest timeline.
- **Entry points**: Tap "Analytics" bottom navigation tab.
- **Exit points**: Switch bottom navigation tab or tap a specific day node for detailed breakdown.
- **Layout, top to bottom**:
  - Header: `ANALYTICS` (`24.sp Black`, letter spacing `4.sp`, `20.dp` H, `16.dp` V).
  - Spacing: `12.dp` gap.
  - 2x2 Metric Grid (`Row` of 2 columns, `12.dp` spacing):
    - Card 1: `Total Focus` (Clock icon, `${hours}h ${min}m`, `18.sp Bold`).
    - Card 2: `Total Points` (Star icon `#FDCB6E`, `${points} pts`, `18.sp Bold`).
    - Card 3: `Sessions Done` (Check icon `#00B894`, `${count}`, `18.sp Bold`).
    - Card 4: `Current Streak` (Flame icon `#FF7675`, `${days} days`, `18.sp Bold`).
  - Spacing: `16.dp` gap.
  - Daily Forest Card:
    - `NeumorphicCard` (`20.dp` corner radius, `6.dp` elevation, `16.dp` padding).
    - Card title: `DAILY TREES PLANTED 🌲` (`14.sp Bold`).
    - Subtitle: "Scroll horizontally to view past days. Tap a day to see statistics." (`12.sp`).
    - Horizontal scroll row (`LazyRow`, `10.dp` spacing):
      - Daily nodes: Date pill (e.g. `Oct 14`), Pine tree illustration canvas (healthy green if sessions completed, withered sprout if 0 sessions), session count badge.
      - Selected node: Surrounded by primary border highlight (`#6C5CE7`, `2.dp`).
  - Spacing: `16.dp` gap.
  - Selected Day Detail Container:
    - Shows selected date header, total focus minutes, and a vertical list of completed session entities with start time and duration badges.
- **Primary action**: Tap daily tree node to view date breakdown (middle third).
- **Secondary actions**: Horizontal scroll on tree timeline.
- **States**:
  - `loading`: MISSING.
  - `empty`: "No focus activity recorded yet. Start a session on the Timer tab!"
  - `error`: MISSING.
  - `success`: Fully populated stat cards and tree timeline.
  - `offline`: Reads from local Room database.
- **Data shown**: Total seconds, total points, total sessions, streak, daily tree health statuses, session start/end timestamps.
- **Animations/transitions**: Tree node selection border scale bounce.
- **Known problems**: Tree canvas graphics use fixed pixel offsets that do not scale dynamically with system display density variations.

---

### Screen: Community & Social Screen
- **Purpose**: View friends on an interactive 3D-styled archipelago island map, cheer active study sessions, and compare ranks on daily/weekly/all-time leaderboards.
- **Entry points**: Tap "Community" bottom navigation tab.
- **Exit points**: Switch bottom navigation tab.
- **Layout, top to bottom**:
  - Top tab switcher capsule (`20.dp` padding):
    - 2-segment selector: `🏝️ Island Map` | `🏆 Leaderboard` (Height `40.dp`, corner radius `20.dp`).
  - **Tab 1: Island Map**:
    - Fullscreen interactive Canvas representing ocean and islands.
    - Draggable / pinch-to-zoom map area with customized friend avatar pins.
    - Active friend pin: Pulsating green ring indicator + live soundwave icon.
    - Tap friend pin: Opens `GlassCard` dialog with user name, study subject, tree count, and "Send Cheer 🎉" button.
  - **Tab 2: Leaderboard**:
    - Timeframe pill row: `Daily` | `Weekly` | `All Time`.
    - Podium section: Top 3 avatars on gold/silver/bronze pedestals with crown graphics.
    - Ranked list (`LazyColumn`, `8.dp` spacing):
      - Rank number, Avatar thumbnail, Display name, Streak fire count, Total focus hours.
      - Current user row: Distinct primary tinted background with "YOU" badge.
  - Search & add friends bar (bottom anchored): `OutlinedTextField` with Add Friend icon button.
- **Primary action**: Tap friend pin on island (Tab 1) or view ranking (Tab 2).
- **Secondary actions**: Send cheer button, timeframe switcher, search friend by username.
- **States**:
  - `loading`: Centered spinner.
  - `empty`: "No friends added yet. Search by username to grow your study circle!"
  - `error` / `offline`: "Connect to internet to sync global leaderboard."
  - `success`: Island canvas rendered with friend pins; leaderboard populated.
- **Data shown**: Friend display names, avatars, current study status, total points, leader ranks.
- **Animations/transitions**: Live soundwave wave animation on active pins; podium pedestal bounce entrance.
- **Known problems**: Pinch-to-zoom on Island Map can feel jittery on low-end devices due to full canvas re-draw passes during multi-touch gestures.

---

### Screen: Radio & Soundscapes Screen
- **Purpose**: Browse and stream curated lofi beats, ambient soundscapes, classical music, and manage favorite stations.
- **Entry points**: Tap Radio icon button on Timer screen header.
- **Exit points**: Tap Back button (returns to Timer).
- **Layout, top to bottom**:
  - Top bar (`16.dp` padding):
    - Back button (`NeumorphicCard`, 40dp x 40dp, ArrowBack icon).
    - Title: `Study Radio` (`18.sp Bold`).
    - Right: Favorites badge (Heart icon `#FF7675` + count).
  - Spacing: `12.dp` gap.
  - Now Playing Hero Card (`GlassCard`, `20.dp` corner radius):
    - Visible when streaming: Station logo (56dp x 56dp), Station title (`16.sp Bold`), Genre pill (e.g. `Lofi Beats`), animated equalizer visualizer (`PlayingWaveIndicator`), large Play/Stop toggle button.
  - Spacing: `12.dp` gap.
  - Genre filter chips row (`LazyRow`, `8.dp` spacing): `All`, `Lofi`, `Ambient`, `Classical`, `Nature`, `Jazz`.
  - Spacing: `12.dp` gap.
  - Station list (`LazyColumn`, `10.dp` spacing):
    - Item card (`NeumorphicCard`, `12.dp` corner radius): Station thumbnail (44dp x 44dp), Title (`14.sp Bold`), Bitrate tag (`128 kbps`), Favorite heart toggle icon button, Play/Stop button.
- **Primary action**: Play/Stop button on station list item or now-playing hero card.
- **Secondary actions**: Genre filter chips, Favorite heart toggle, Back button.
- **States**:
  - `loading`: Animated wave shimmer on station item while stream buffers.
  - `empty`: "No radio stations found in this category."
  - `error`: Error snackbar "Unable to connect to stream. Check internet connection."
  - `success`: Audio streams via ExoPlayer; animated wave pulses.
  - `offline`: Radio streams unavailable; suggests switching to built-in Ambient sounds.
- **Data shown**: Station titles, genres, stream bitrates, favorite statuses, live playback status.
- **Animations/transitions**: 4-bar equalizer wave height oscillating animation (`infiniteRepeatable`).
- **Known problems**: ExoPlayer buffer stall on unstable mobile network causes audio stutter without a prominent buffering indicator on the list item.

---

### Screen: Settings & Dashboard Config Screen
- **Purpose**: Adjust Pomodoro intervals, manage exam workloads, configure app blocker, backup/restore data, and manage account.
- **Entry points**: Tap "Settings" bottom navigation tab.
- **Exit points**: Switch bottom navigation tab or tap links to `exams`, `app_blocker`, `auth`.
- **Layout, top to bottom**:
  - Header: `DASHBOARD CONFIG` (`20.sp Black`, letter spacing `2.sp`, `20.dp` padding).
  - Expandable accordion sections list (`LazyColumn`, `12.dp` spacing):
    - **Section 1: EXAM WORKLOAD**: Upcoming trials count, link to `ExamsScreen`, and launcher buttons for `Stats Widget` (`#6C5CE7`) and `Matrix Widget` (`#6750A4`).
    - **Section 2: TIMER INTERVALS**: Sliders for Focus Duration (`5..120 min`), Short Break (`1..30 min`), Long Break (`5..60 min`), and Long Break Interval counter.
    - **Section 3: SYSTEM & HARDWARE**: Switches for Auto-start breaks, Auto-start focus, Keep screen awake, AMOLED Battery Saver auto-trigger, and Haptic feedback selector.
    - **Section 4: APP BLOCKER**: Blocked apps count, link to `AppBlockerScreen`, Strict Mode switch.
    - **Section 5: DATA BACKUP & RESTORE**: Buttons for Export JSON Backup, Export Flashcards ZIP, Import Backup.
    - **Section 6: LIVE WALLPAPER**: Install and preview toggle for Day/Night Live Wallpaper.
    - **Section 7: ACCOUNT & PROFILE**: User email display, Google avatar, Log Out button, Delete Account button (`#D63031`).
    - **Section 8: ABOUT**: Version string, build ID, open source licenses.
- **Primary action**: Adjust interval sliders or tap section links.
- **Secondary actions**: Switches, backup export/import buttons, account log out.
- **States**:
  - `loading`: MISSING.
  - `empty`: MISSING.
  - `error`: MISSING.
  - `success`: Setting values persisted immediately to DataStore / SharedPreferences.
  - `offline`: Fully functional offline.
- **Data shown**: Timer interval values, blocked app count, exam count, user profile info, version number.
- **Animations/transitions**: Accordion section expand/collapse height animation (`animateContentSize`).
- **Known problems**: Settings screen is very long (> 2300 lines of UI code) with dense accordion sections, making specific settings hard to locate quickly.

---

### Screen: Exams & Countdown Schedule Screen
- **Purpose**: Add, edit, and track countdown days and target study goals for upcoming academic exams.
- **Entry points**: Tap "Exam Workload" link in Settings screen.
- **Exit points**: Tap Back button (returns to Settings) or tap Add Exam `+`.
- **Layout, top to bottom**:
  - Top bar (`16.dp` padding): Back button, title `Exam Schedule` (`18.sp Bold`), Add button (`+`, `40.dp`).
  - Exams list (`LazyColumn`, `12.dp` spacing):
    - Item: `NeumorphicCard` with vertical color band on left edge (`6.dp` width).
    - Item layout:
      - Subject name (`16.sp Bold`, e.g. "Organic Chemistry Final").
      - Date & Time (`13.sp TextSecondary`, e.g. "Dec 18, 2026 • 09:00 AM").
      - Countdown badge pill: High-contrast capsule (e.g. `14 DAYS LEFT` in `#6C5CE7` or `DUE TOMORROW` in `#D63031`).
      - Study progress bar: Horizontal bar (`6.dp` height) showing `studiedHours / targetHours`.
  - Add / Edit Exam Modal Dialog (triggered via `+` or item tap):
    - Subject name field, target study hours field, Date picker trigger, Time picker trigger, color swatch picker (6 colors), "Save Exam" button.
- **Primary action**: Add Exam `+` button (top-right).
- **Secondary actions**: Tap exam card to edit, delete exam swipe/button, back button.
- **States**:
  - `loading`: MISSING.
  - `empty`: Calendar graphic, text "No upcoming exams scheduled. Tap + to add an exam countdown!", "Add Exam" button.
  - `error`: MISSING.
  - `success`: Exams list displayed in chronological order.
  - `offline`: Stored in Room database.
- **Data shown**: Exam names, dates, days remaining, target study hours, color codes.
- **Animations/transitions**: Dialog scale pop-in; countdown badge color shift when < 3 days remaining.
- **Known problems**: Date/Time pickers use system dialogs which clash visually with the app's custom Neumorphic styling.

---

### Screen: App Blocker Screen
- **Purpose**: Select installed applications to block during active focus sessions to eliminate phone distractions.
- **Entry points**: Tap "App Blocker" link in Settings screen.
- **Exit points**: Tap Back button (returns to Settings).
- **Layout, top to bottom**:
  - Top bar (`16.dp` padding): Back button, title `Distraction Blocker` (`18.sp Bold`).
  - Permission Warning Card (if Usage Access / Accessibility not granted): Red tinted card (`#D63031` at 10% alpha), warning text, "Grant Permission" action button.
  - Search & category filter row (`12.dp` padding): Search apps input field, filter chips (`All Apps`, `Social Media`, `Games`, `Blocked Only`).
  - Installed apps list (`LazyColumn`, `8.dp` spacing):
    - Item card (`NeumorphicCard`, `12.dp` corner radius): App icon (40dp x 40dp), App title (`15.sp Bold`), Package name (`11.sp TextSecondary`), Block toggle Switch (`Switch`).
- **Primary action**: Toggle Switch on app row to block/unblock.
- **Secondary actions**: Search input, category filter chips, grant permission button.
- **States**:
  - `loading`: Shimmer list while scanning installed packages.
  - `empty`: "No apps match your search."
  - `error` / `permission_denied`: Persistent top banner requiring system accessibility authorization.
  - `success`: Installed apps loaded with accurate package icons and toggle states.
  - `offline`: 100% on-device package manager queries.
- **Data shown**: App label, package name, app icon drawable, blocked toggle state.
- **Animations/transitions**: Switch toggle slide animation.
- **Known problems**: Initial package query can freeze the UI thread for 200–400ms on devices with > 150 installed applications if not dispatched strictly on IO thread.

---

### Screen: Authentication & Onboarding Screen
- **Purpose**: Authenticate user via Google Sign-In with Credential Manager or allow offline guest access.
- **Entry points**: First cold launch or tapping "Log Out" in Settings.
- **Exit points**: Successful sign-in or guest bypass (navigates to `timer`).
- **Layout, top to bottom**:
  - Centered hero column (`24.dp` horizontal padding):
    - App logo illustration (80dp x 80dp pine tree / dial icon).
    - App title: `FocusFlow` (`28.sp Black`, letter spacing `2.sp`).
    - Tagline: `Master your study cycles & spaced revisions` (`14.sp TextSecondary`).
  - Spacing: `48.dp` gap.
  - Action buttons column:
    - Google Sign-In button: Large `NeumorphicButton` (Height `52.dp`, Google 'G' icon, text "Sign in with Google", font `15.sp Bold`).
    - Guest mode button: Text button "Continue as Guest (Offline Mode)" (`13.sp TextSecondary`).
  - Bottom disclaimer: "By signing in, you agree to sync your focus analytics with your study circle." (`11.sp`, centered).
- **Primary action**: "Sign in with Google" button (middle third).
- **Secondary actions**: "Continue as Guest" button.
- **States**:
  - `loading`: Spinner inside Google button during Credential Manager token exchange.
  - `empty`: MISSING.
  - `error`: Error snackbar "Sign-in failed. Please try again."
  - `success`: Navigates to `timer` home screen.
  - `offline`: Guest mode active.
- **Data shown**: App branding, user sign-in status.
- **Animations/transitions**: Logo subtle floating bob animation (`infiniteRepeatable`).
- **Known problems**: If Google Play Services is missing (e.g. AOSP / emulator), the Credential Manager throws an unhandled exception rather than gracefully falling back to Guest mode automatically.

---

### Widget: Exam Countdown & Focus Stats Widget (`ExamCountdownWidgetReceiver.kt`)
- **Purpose**: Display upcoming academic exam deadlines with daily countdowns alongside all-time study statistics on the Android Home Screen.
- **Entry points**: Added by user from Android Home Screen widget picker or launched via Settings screen widget action button.
- **Exit points**: Tapping anywhere on the widget container launches `MainActivity` with `FLAG_ACTIVITY_CLEAR_TOP`.
- **Layout, left to right (2-column horizontal card)**:
  - Background container: `FrameLayout` with `@drawable/widget_card_glass` (`wgt_surface` glass translucent background with 24dp corner outline radius on Android 12+).
  - Left column (Upcoming Exams, `layout_weight = 1.25`, `layout_marginEnd = 8dp`):
    - Header row: Calendar icon (`13dp x 13dp`, `@drawable/ic_widget_calendar`), uppercase title `UPCOMING EXAMS` (`9.5sp Bold`, letter spacing `0.08`, text color `wgt_on_surface`).
    - Empty state view: `TextView` ("No upcoming exams scheduled", `11.5sp Italic`, `wgt_on_surface_variant`, `visibility = gone`).
    - Exam slots list (up to 3 slots):
      - Slot 1: Exam name (`14sp Bold`, `wgt_on_surface`, 1 line ellipsize) + Days left badge (`12.5sp Bold`, `wgt_accent`, e.g. "3 Days").
      - Slot 2: Exam name (`12sp Normal`, `wgt_on_surface_variant`) + Days left badge (`11sp Bold`, `wgt_accent`, e.g. "12d").
      - Slot 3 (hidden in compact height < 130dp): Exam name (`12sp Normal`, `wgt_on_surface_variant`) + Days left badge (`11sp Bold`, `wgt_accent`, e.g. "18d").
  - Center vertical divider: 1dp width line (`wgt_divider` color, `2dp` vertical margin).
  - Right column (Focus Stats, `layout_weight = 1.0`, `layout_marginStart = 10dp`):
    - Header row: Stats icon (`13dp x 13dp`, `@drawable/ic_widget_stats`), uppercase title `FOCUS STATS` (`9.5sp Bold`, letter spacing `0.08`, `wgt_on_surface`).
    - Stat Row 1 (Focus): Clock icon (`12dp x 12dp`), label "Focus" (`11sp`, `wgt_on_surface_variant`), value e.g. "14h" / "45m" (`11sp Bold`, `wgt_on_surface`).
    - Stat Row 2 (Points): Star icon (`12dp x 12dp`), label "Points" (`11sp`, `wgt_on_surface_variant`), value e.g. "350" (`11sp Bold`, `wgt_on_surface`).
    - Stat Row 3 (Sessions): Check icon (`12dp x 12dp`), label "Sessions" (`11sp`, `wgt_on_surface_variant`), value e.g. "28" (`11sp Bold`, `wgt_on_surface`).
    - Stat Row 4 (Streak): Flame icon (`12dp x 12dp`), label "Streak" (`11sp`, `wgt_on_surface_variant`), value e.g. "6d" (`11sp Bold`, `wgt_on_surface`).
- **Primary action**: Tapping widget container launches app.
- **Secondary actions**: None (RemoteViews limitation).
- **States**:
  - `loading` / `fallback`: Renders default blank glass layout.
  - `empty_exams`: Shows "No upcoming exams scheduled" on left column; stats on right column still render real numbers.
  - `compact`: When widget height is resized < 130dp, Slot 3 is hidden automatically.
  - `offline`: 100% functional offline (reads local Room database).
- **Data shown**: Exam names, calculated days remaining, total completed focus hours/minutes, focus points, session count, streak days.
- **Animations/transitions**: None (standard Android RemoteViews).
- **Known problems**: Re-renders only at midnight or upon app session completion; manual home screen resize does not trigger instant re-layout until next broadcast update.

---

### Widget: Exam Matrix Calendar Widget (`ExamMatrixWidgetReceiver.kt`)
- **Purpose**: Render a full-year 12x31 dot matrix calendar on the Android Home Screen highlighting past days, current day, and upcoming exam dates.
- **Entry points**: Added by user from Android Home Screen widget picker or launched via Settings screen widget action button.
- **Exit points**: Tapping anywhere on the container launches `MainActivity`.
- **Layout (Canvas-rendered bitmap into RemoteViews ImageView)**:
  - Container: `FrameLayout` with `@drawable/widget_card_glass` and `ImageView` (`widget_matrix_image`).
  - Generated bitmap dimensions: 1000f width x 560f height (ARGB_8888).
  - Left column: 12 vertical month labels (`JAN` through `DEC`, `Paint` with bold uppercase typeface, active current month highlighted in `wgt_accent`, inactive months in `wgt_on_surface_variant` at 40% alpha).
  - Matrix grid area (12 rows x 31 columns of circular dots):
    - Past days: Small muted grey dots (Radius `unit * 0.45f`, color `wgt_on_surface_variant` at 25% alpha).
    - Current day (Today): Glowing accent dot with ring outline (Radius `unit * 0.9f`, color `wgt_accent`).
    - Exam days: Bright highlighted red/accent circular badge (Radius `unit * 0.85f`, color `wgt_critical` or `wgt_accent`).
    - Future non-exam days: Dim empty track dots (Radius `unit * 0.35f`, color `wgt_track`).
    - Non-existent month days (e.g. Feb 30, Apr 31): Omitted / blank.
  - Footer summary bar (`height = 560f * 0.135f`):
    - Left text: `YEAR PROGRESS: $yearProgressPercent%` (`wgt_on_surface_variant`).
    - Right text: Next upcoming exam countdown pill (e.g. `NEXT: Organic Chemistry in 14d`, `wgt_accent`).
- **Primary action**: Tapping widget container launches app.
- **Secondary actions**: None.
- **States**:
  - `loading` / `fallback`: Blank fallback layout.
  - `empty_exams`: Grid renders normally with year progress; footer displays "No upcoming exams".
  - `offline`: 100% functional offline.
- **Data shown**: 365/366 day matrix dots, current year progress percentage, next exam subject and days left.
- **Animations/transitions**: None (static Canvas bitmap rendered into RemoteViews).
- **Known problems**: On very narrow launcher grid columns (e.g. 2x2 widget size), the 1000x560 bitmap scales down significantly, making individual 31-day dots hard to distinguish.

---

## 5. Reusable Components

### `NeumorphicCard` (`ui/components/NeumorphicComponents.kt`)
- **Variants**: Standard Elevated (`elevation = 4.dp`), Flat Inset (`elevation = 0.dp`), Interactive Pressable.
- **Dimensions**: Flexible width/height via `Modifier`.
- **Internal padding**: `16.dp` default (configurable to `12.dp` or `20.dp`).
- **Corner radius**: `16.dp` default.
- **Colors**: Fill `themeColors.surface` (`#E8ECF2` Light / `#252830` Dark), shadow top-left `#FFFFFF`/`#2C303A`, shadow bottom-right `#A3B1C6`/`#141519`.
- **Screens using it**: `TimerScreen`, `RevisionsHomeScreen`, `RevisionSessionScreen`, `AnalyticsScreen`, `SettingsScreen`, `ExamsScreen`, `RadioScreen`, `AppBlockerScreen`.
- **One-off deviations**: `BatterySaverScreen` builds raw `Box` with `border(1.dp, Color(0x40FFFFFF))` instead of using `NeumorphicCard`.

### `NeumorphicButton` (`ui/components/NeumorphicComponents.kt`)
- **Variants**: Circular Icon Button (40dp, 52dp, 72dp), Capsule Action Button (height 40dp, 48dp, 52dp).
- **Dimensions**: Width variable, height 40dp–52dp, circular variants equal aspect ratio.
- **Internal padding**: `16.dp` horizontal, `12.dp` vertical.
- **Corner radius**: `12.dp` (buttons), `CircleShape` (icon buttons).
- **Colors**: Fill `themeColors.surface`, text `themeColors.textPrimary` or `themeColors.primary`.
- **Text style**: `14.sp Bold`, letter spacing `1.sp`.
- **Screens using it**: `TimerScreen`, `RevisionsHomeScreen`, `RevisionSessionScreen`, `AnalyticsScreen`, `SettingsScreen`, `AuthScreen`.
- **One-off deviations**: `CropEditorScreen` uses raw `FloatingActionButton` with solid `#6C5CE7` fill.

### `GlassCard` (`ui/components/GlassCard.kt`)
- **Variants**: Default Frosted Card, Hero Player Card.
- **Dimensions**: Flexible via `Modifier`.
- **Internal padding**: `16.dp` / `20.dp`.
- **Corner radius**: `16.dp` / `20.dp`.
- **Colors**: Fill `Color.White.copy(alpha = 0.08f)` (Dark) / `Color.White.copy(alpha = 0.65f)` (Light), border `1.dp Color.White.copy(alpha = 0.15f)`.
- **Screens using it**: `CommunityScreen` (friend popups), `RadioScreen` (Now Playing hero), `TimerScreen` (soundscape selector dialog).
- **One-off deviations**: `FlowAnalyticsSheet` in `TimerScreen.kt` re-implements raw glassmorphic styling inside a `ModalBottomSheet` rather than wrapping `GlassCard`.

### `GlassButton` (`ui/components/GlassButton.kt`)
- **Variants**: Action Button, Icon Button.
- **Dimensions**: Height `40.dp` / `48.dp`.
- **Internal padding**: `14.dp` horizontal, `10.dp` vertical.
- **Corner radius**: `12.dp` / `20.dp`.
- **Colors**: Background `Color.White.copy(alpha = 0.12f)`, text `Color.White`.
- **Text style**: `13.sp SemiBold`.
- **Screens using it**: `CommunityScreen`, `BatterySaverScreen`.
- **One-off deviations**: `AuthScreen` guest mode button uses standard `TextButton`.

### `ConfirmDeleteDialog` (`ui/components/ConfirmDeleteDialog.kt`)
- **Variants**: Single destructive confirmation modal.
- **Dimensions**: Dialog width `Modifier.fillMaxWidth(0.9f)`.
- **Internal padding**: `20.dp`.
- **Corner radius**: `20.dp`.
- **Colors**: Container `themeColors.surface`, warning icon `#D63031`, confirm button container `#D63031`.
- **Text style**: Title `18.sp Bold`, Body `14.sp Normal`.
- **Screens using it**: `RevisionsHomeScreen` (Delete Deck), `SettingsScreen` (Delete Account, Clear Data), `ExamsScreen` (Delete Exam).

---

## 6. Honest Weak Points

1. **Dual / Tri-Paradigm Styling Conflict**: The UI mixes Neumorphism (soft, extruded grey bevels on `TimerScreen` and `SettingsScreen`), Glassmorphism (crisp frosted translucent borders on `CommunityScreen` and `RadioScreen`), and pure OLED Black (`#000000` with solid neon strokes on `BatterySaverScreen` and `CaptureScreen`), creating visual incoherence.
2. **Hardcoded Monolithic Composables**: `TimerScreen.kt` (2815 lines), `SettingsScreen.kt` (2374 lines), and `RevisionsHomeScreen.kt` (1228 lines) contain massive inline composables with deeply nested state logic and inline styling, impeding modular redesign.
3. **Hardcoded Color Hex Bypasses**: Dozens of UI elements directly hardcode hex values (e.g. `Color(0xFF9482FF)`, `Color(0xFF55E6C1)`, `Color(0xFF6750A4)`) rather than querying `LocalAppThemeColors`, causing color mismatches when switching between light and dark themes.
4. **Hardcoded Non-English Strings**: `CaptureScreen.kt` and `CropEditorScreen.kt` contain hardcoded French strings (`"Permission caméra requise"`, `"Accorder l'accès"`, `"Impossible de charger l'image"`) while the remainder of the application is strictly English.
5. **System Font Scaling Disabled**: `Theme.kt` forcibly clamps `fontScale = 1.0f` to prevent circular timer clipping, breaking Android accessibility standards for users who require larger system text sizes.
6. **Missing Empty & Error States**: `CropEditorScreen` provides no guided retry UI if OCR extraction produces blank text (only a generic Toast); `CommunityScreen` Island Map lacks an offline placeholder state when Firebase Firestore cannot be reached.
7. **Small Screen Layout Overflow**: The fixed 280dp x 280dp center dial on `TimerScreen` and the 4-button horizontal grading bar on `RevisionSessionScreen` clip or overflow vertically on small displays (< 640dp height).
8. **Quadrilateral Crop Perspective Defect**: The Quadrilateral crop tool in `CropEditorScreen` allows dragging 4 independent perspective pins on canvas, but the underlying bitmap cropping logic only takes the rectangular bounding box, failing to perform actual 4-point perspective un-warping.
9. **UI Thread Package Query Hitching**: `AppBlockerScreen` queries all installed Android packages on launch, causing a visible 200–400ms frame drop before the list appears on devices with many installed apps.
10. **System Date/Time Dialog Disconnect**: The date and time pickers in `ExamsScreen` use Android's default Material AlertDialogs, clashing with the custom Neumorphic styling of the rest of the app.

---

## 7. Constraints for a Redesign

### Non-Negotiable Core Features & Requirements
- **Spaced Repetition (SRS) Engine**: The SuperMemo SM-2 formula and 4-grade rating system (`Again`, `Hard`, `Good`, `Easy`) and associated Room database schema (`revision_decks`, `revision_notes`) must remain functional.
- **Local Persistence & Room DB**: Database entities (`SessionEntity`, `ExamEntity`, `DeckEntity`, `RevisionNoteEntity`, `BlockedAppEntity`) must be preserved without destructive schema migrations.
- **Firebase Authentication & Cloud Sync**: Authentication must use Google Sign-In via Jetpack `CredentialManager` (`GetSignInWithGoogleOption`). Anonymous authentication is prohibited.
- **Camera & OCR Pipeline**: CameraX capture and ML Kit text recognition integration must remain operational.
- **Background Services & Audio**: ExoPlayer stream playback service, Android `AccessibilityService` / `UsageStatsManager` for app blocking, and `DayNightLiveWallpaperService` must remain intact.
- **Android App Widgets**: `ExamCountdownWidgetReceiver` and `ExamMatrixWidgetReceiver` layouts and update schedulers must be preserved.

### Platform & Architecture Constraints
- **Kotlin & Jetpack Compose**: All screens must use Compose with Material 3 foundation.
- **Target SDK**: Android 15 (API 35), Min SDK: Android 8.0 (API 26).
- **Offline-First Resilience**: All core timer, revision, analytics, and widget features must function without an active network connection.
