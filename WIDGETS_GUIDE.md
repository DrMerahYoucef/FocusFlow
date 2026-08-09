# FocusFlow Home Screen Widgets Guide

This document provides a comprehensive architecture, setup guide, and full source code for the two Android Home Screen widgets built into FocusFlow:

1. **Exam Countdown & Focus Stats Widget (`ExamCountdownWidgetReceiver`)**
2. **Year Progress Exam Matrix Grid Widget (`ExamMatrixWidgetReceiver`)**

---

## 🌟 Key Features & Design System

- **Neumorphic Translucent Glass UI**: Both widgets render a soft drop-shadow, top-left specular highlight glow, translucent background fill (90% opacity), and a glassy rim border.
- **Dynamic Automatic Day / Night Mode**:
  - **Day Mode (6:00 AM – 5:59 PM)**: Clean frosted white card `#EDF3F6F9`, dark charcoal slate text `#111827`, soft slate icons `#334155`, vivid blue accent `#2563EB`.
  - **Night Mode (6:00 PM – 5:59 AM)**: Dark obsidian glass card `#ED0F131A`, bright off-white text `#F9FAFB`, light slate icons `#E2E8F0`, electric blue accent `#60A5FA`.
- **Real-Time Database Sync**: Reads directly from Room Database (`examDao` & `sessionDao`) for live exam countdowns, focus time, total points, session counts, and streak tracking.
- **Background Automatic Refresh**: Uses `AlarmManager` repeating broadcast triggers every 30 minutes to seamlessly transition between Day and Night modes without requiring app foreground activity.

---

## 📁 Architecture & File Overview

| Component | Path / File | Purpose |
| :--- | :--- | :--- |
| **Countdown Receiver** | `app/src/main/java/com/example/widget/ExamCountdownWidgetReceiver.kt` | Renders upcoming exam countdowns & focus stats onto a Neumorphic card bitmap. |
| **Matrix Receiver** | `app/src/main/java/com/example/widget/ExamMatrixWidgetReceiver.kt` | Renders a 12-month dot matrix grid (365 days) highlighting past days, current day, exam run-up, and exam targets. |
| **Countdown Layout** | `app/src/main/res/layout/widget_countdown_layout.xml` | XML layout container with vector icons and text views. |
| **Matrix Layout** | `app/src/main/res/layout/widget_exam_matrix_layout.xml` | XML layout container holding the dynamic matrix bitmap view. |
| **Widget Info XMLs** | `app/src/main/res/xml/exam_countdown_widget_info.xml`<br>`app/src/main/res/xml/exam_matrix_widget_info.xml` | `AppWidgetProvider` configuration metadata files. |
| **Vector Icons** | `app/src/main/res/drawable/ic_widget_*.xml` | High quality vector icons for calendar, stats, clock, star, check, and flame. |

---

## 🛠️ 1. Manifest & Provider Metadata Configuration

### `AndroidManifest.xml` (Receiver Declarations)
```xml
<!-- Exam Countdown Widget Receiver -->
<receiver
    android:name="com.example.widget.ExamCountdownWidgetReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
        <action android:name="com.example.ACTION_WIDGET_AUTO_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/exam_countdown_widget_info" />
</receiver>

<!-- Exam Matrix Widget Receiver -->
<receiver
    android:name="com.example.widget.ExamMatrixWidgetReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
        <action android:name="com.example.ACTION_WIDGET_AUTO_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/exam_matrix_widget_info" />
</receiver>
```

### `res/xml/exam_countdown_widget_info.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="180dp"
    android:minHeight="110dp"
    android:updatePeriodMillis="3600000"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen"
    android:description="@string/widget_description"
    android:initialLayout="@layout/widget_countdown_layout" />
```

### `res/xml/exam_matrix_widget_info.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="260dp"
    android:minHeight="140dp"
    android:updatePeriodMillis="3600000"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen"
    android:description="@string/matrix_widget_description"
    android:initialLayout="@layout/widget_exam_matrix_layout" />
```

---

## 🎨 2. Layout XML Source Code

### `res/layout/widget_countdown_layout.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/widget_container"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- Neumorphic Translucent Rendered Background Image -->
    <ImageView
        android:id="@+id/widget_background"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:scaleType="fitXY" />

    <!-- Overlay Content Container -->
    <LinearLayout
        android:id="@+id/widget_overlay"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@android:color/transparent"
        android:orientation="horizontal"
        android:padding="16dp">

        <!-- Column Left: Upcoming Exams -->
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1.25"
            android:orientation="vertical"
            android:layout_marginEnd="8dp">

            <!-- Title Header with Calendar Icon -->
            <LinearLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical">

                <ImageView
                    android:id="@+id/widget_icon_exams_header"
                    android:layout_width="13dp"
                    android:layout_height="13dp"
                    android:src="@drawable/ic_widget_calendar"
                    android:layout_marginEnd="5dp"
                    android:contentDescription="Exams Header Icon" />

                <TextView
                    android:id="@+id/widget_title_label"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="UPCOMING EXAMS"
                    android:textColor="#111827"
                    android:textSize="9.5sp"
                    android:textStyle="bold"
                    android:letterSpacing="0.08" />
            </LinearLayout>

            <TextView
                android:id="@+id/widget_no_exams_text"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:gravity="center_vertical"
                android:text="No upcoming exams listed. Great time to study!"
                android:textColor="#64748B"
                android:textSize="11.5sp"
                android:textStyle="italic"
                android:visibility="gone" />

            <LinearLayout
                android:id="@+id/widget_exams_list_container"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:orientation="vertical"
                android:gravity="center_vertical"
                android:layout_marginTop="4dp">

                <!-- Exam Slot 1 -->
                <LinearLayout
                    android:id="@+id/widget_exam1_container"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:gravity="center_vertical"
                    android:paddingTop="3dp"
                    android:paddingBottom="3dp"
                    android:visibility="visible">
                    <TextView
                        android:id="@+id/widget_exam1_name"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Math 101"
                        android:textColor="#111827"
                        android:textSize="14sp"
                        android:textStyle="bold"
                        android:maxLines="1"
                        android:ellipsize="end" />
                    <TextView
                        android:id="@+id/widget_exam1_days"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="3 Days"
                        android:textColor="#2563EB"
                        android:textSize="12.5sp"
                        android:textStyle="bold"
                        android:layout_marginStart="4dp" />
                </LinearLayout>

                <!-- Exam Slot 2 -->
                <LinearLayout
                    android:id="@+id/widget_exam2_container"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:gravity="center_vertical"
                    android:paddingTop="2dp"
                    android:paddingBottom="2dp"
                    android:visibility="visible">
                    <TextView
                        android:id="@+id/widget_exam2_name"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Physics II"
                        android:textColor="#111827"
                        android:textSize="12sp"
                        android:maxLines="1"
                        android:ellipsize="end" />
                    <TextView
                        android:id="@+id/widget_exam2_days"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="12d"
                        android:textColor="#2563EB"
                        android:textSize="11sp"
                        android:layout_marginStart="4dp" />
                </LinearLayout>

                <!-- Exam Slot 3 -->
                <LinearLayout
                    android:id="@+id/widget_exam3_container"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:gravity="center_vertical"
                    android:paddingTop="2dp"
                    android:paddingBottom="2dp"
                    android:visibility="visible">
                    <TextView
                        android:id="@+id/widget_exam3_name"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Chemistry"
                        android:textColor="#111827"
                        android:textSize="12sp"
                        android:maxLines="1"
                        android:ellipsize="end" />
                    <TextView
                        android:id="@+id/widget_exam3_days"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="18d"
                        android:textColor="#2563EB"
                        android:textSize="11sp"
                        android:layout_marginStart="4dp" />
                </LinearLayout>

            </LinearLayout>
        </LinearLayout>

        <!-- Vertical Divider -->
        <View
            android:id="@+id/widget_vertical_divider"
            android:layout_width="1dp"
            android:layout_height="match_parent"
            android:layout_marginTop="2dp"
            android:layout_marginBottom="2dp"
            android:background="#20000000" />

        <!-- Column Right: Focus Stats -->
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1.0"
            android:orientation="vertical"
            android:layout_marginStart="10dp"
            android:gravity="center_vertical">

            <!-- Stats Header -->
            <LinearLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:layout_marginBottom="6dp">

                <ImageView
                    android:id="@+id/widget_icon_stats_header"
                    android:layout_width="13dp"
                    android:layout_height="13dp"
                    android:src="@drawable/ic_widget_stats"
                    android:layout_marginEnd="5dp"
                    android:contentDescription="Stats Header Icon" />

                <TextView
                    android:id="@+id/widget_stats_title_label"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="FOCUS STATS"
                    android:textColor="#111827"
                    android:textSize="9.5sp"
                    android:textStyle="bold"
                    android:letterSpacing="0.08" />
            </LinearLayout>

            <!-- Stat Row: Focus -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginBottom="3dp"
                android:gravity="center_vertical">

                <ImageView
                    android:id="@+id/widget_icon_focus"
                    android:layout_width="12dp"
                    android:layout_height="12dp"
                    android:src="@drawable/ic_widget_clock"
                    android:layout_marginEnd="4dp"
                    android:contentDescription="Focus Icon" />

                <TextView
                    android:id="@+id/widget_stat_focus_label"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Focus"
                    android:textColor="#64748B"
                    android:textSize="11sp" />
                <TextView
                    android:id="@+id/widget_stat_focus_value"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="0h"
                    android:textColor="#111827"
                    android:textSize="11sp"
                    android:textStyle="bold" />
            </LinearLayout>

            <!-- Stat Row: Points -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginBottom="3dp"
                android:gravity="center_vertical">

                <ImageView
                    android:id="@+id/widget_icon_points"
                    android:layout_width="12dp"
                    android:layout_height="12dp"
                    android:src="@drawable/ic_widget_star"
                    android:layout_marginEnd="4dp"
                    android:contentDescription="Points Icon" />

                <TextView
                    android:id="@+id/widget_stat_points_label"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Points"
                    android:textColor="#64748B"
                    android:textSize="11sp" />
                <TextView
                    android:id="@+id/widget_stat_points_value"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="0"
                    android:textColor="#111827"
                    android:textSize="11sp"
                    android:textStyle="bold" />
            </LinearLayout>

            <!-- Stat Row: Sessions -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginBottom="3dp"
                android:gravity="center_vertical">

                <ImageView
                    android:id="@+id/widget_icon_sessions"
                    android:layout_width="12dp"
                    android:layout_height="12dp"
                    android:src="@drawable/ic_widget_check"
                    android:layout_marginEnd="4dp"
                    android:contentDescription="Sessions Icon" />

                <TextView
                    android:id="@+id/widget_stat_sessions_label"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Sessions"
                    android:textColor="#64748B"
                    android:textSize="11sp" />
                <TextView
                    android:id="@+id/widget_stat_sessions_value"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="0"
                    android:textColor="#111827"
                    android:textSize="11sp"
                    android:textStyle="bold" />
            </LinearLayout>

            <!-- Stat Row: Streak -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical">

                <ImageView
                    android:id="@+id/widget_icon_streak"
                    android:layout_width="12dp"
                    android:layout_height="12dp"
                    android:src="@drawable/ic_widget_flame"
                    android:layout_marginEnd="4dp"
                    android:contentDescription="Streak Icon" />

                <TextView
                    android:id="@+id/widget_stat_streak_label"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Streak"
                    android:textColor="#64748B"
                    android:textSize="11sp" />
                <TextView
                    android:id="@+id/widget_stat_streak_value"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="0d"
                    android:textColor="#111827"
                    android:textSize="11sp"
                    android:textStyle="bold" />
            </LinearLayout>

        </LinearLayout>

    </LinearLayout>
</FrameLayout>
```

### `res/layout/widget_exam_matrix_layout.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/widget_matrix_container"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@android:color/transparent">

    <ImageView
        android:id="@+id/widget_matrix_image"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:adjustViewBounds="true"
        android:scaleType="fitCenter" />

</FrameLayout>
```

---

## 🖼️ 3. Vector Drawables (`res/drawable/`)

### `res/drawable/ic_widget_calendar.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
  <path
      android:fillColor="#FFFFFFFF"
      android:pathData="M19,4h-1V2h-2v2H8V2H6v2H5C3.89,4 3.01,4.9 3.01,6L3,20c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2V6C21,4.9 20.1,4 19,4zM19,20H5V10h14V20zM19,8H5V6h14V8z"/>
</vector>
```

### `res/drawable/ic_widget_stats.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
  <path
      android:fillColor="#FFFFFFFF"
      android:pathData="M5,9.2h3V19H5V9.2zM10.6,5h2.8v14h-2.8V5zM16.2,13H19v6h-2.8V13z"/>
</vector>
```

### `res/drawable/ic_widget_clock.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
  <path
      android:fillColor="#FFFFFFFF"
      android:pathData="M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8zM12.5,7H11v6l5.25,3.15 0.75,-1.23 -4.5,-2.67z"/>
</vector>
```

### `res/drawable/ic_widget_star.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
  <path
      android:fillColor="#FFFFFFFF"
      android:pathData="M12,17.27L18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2L9.19,8.63L2,9.24l5.46,4.73L5.82,21z"/>
</vector>
```

### `res/drawable/ic_widget_check.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
  <path
      android:fillColor="#FFFFFFFF"
      android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM10,17l-5,-5 1.41,-1.41L10,14.17l7.59,-7.59L19,8l-9,9z"/>
</vector>
```

### `res/drawable/ic_widget_flame.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
  <path
      android:fillColor="#FFFFFFFF"
      android:pathData="M13.5,0.67s0.74,2.65 0.74,4.8c0,2.06 -1.35,3.73 -3.41,3.73 -2.07,0 -3.63,-1.67 -3.63,-3.73l0.03,-0.36C5.21,7.51 4,10.62 4,14c0,4.42 3.58,8 8,8s8,-3.58 8,-8c0,-4.79 -2.57,-9.12 -6.5,-11.33zM12,20c-3.31,0 -6,-2.69 -6,-6 0,-2.39 1.39,-4.62 3.32,-5.66 0.17,1.83 1.63,3.32 3.49,3.32 1.93,0 3.5,-1.57 3.5,-3.50 0,-0.27 -0.04,-0.53 -0.1,-0.78C17.26,8.71 18,11.23 18,14c0,3.31 -2.69,6 -6,6z"/>
</vector>
```

---

## 💻 4. Kotlin Source Code

### `ExamCountdownWidgetReceiver.kt`
```kotlin
package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.db.entity.SessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ExamCountdownWidgetReceiver : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleDayNightAlarm(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        scheduleDayNightAlarm(context)
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        scheduleDayNightAlarm(context)
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, ExamCountdownWidgetReceiver::class.java))
        updateAllWidgets(context, manager, ids)
    }

    private fun updateAllWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return

        val database = AppDatabase.getDatabase(context)
        val examDao = database.examDao()
        val sessionDao = database.sessionDao()

        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        CoroutineScope(Dispatchers.IO).launch {
            val exams = examDao.getUpcomingExams(todayCalendar.timeInMillis)
            val completedSessions = sessionDao.getAllSessionsList().filter { it.completed }

            // Compute statistics
            val totalSeconds = completedSessions.sumOf { it.durationSeconds }
            val totalFocusHours = totalSeconds / 3600.0
            val totalPoints = completedSessions.sumOf { it.focusScore }
            val sessionsCount = completedSessions.size
            val currentStreak = calculateStreak(completedSessions)

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isDay = hour in 6..17

            val neumorphicBitmap = getNeumorphicWidgetBitmap(context, isDay)

            for (i in 0 until appWidgetIds.size) {
                val widgetId = appWidgetIds[i]
                val views = RemoteViews(context.packageName, R.layout.widget_countdown_layout)

                // Assign Neumorphic background bitmap
                views.setImageViewBitmap(R.id.widget_background, neumorphicBitmap)

                if (isDay) {
                    views.setInt(R.id.widget_overlay, "setBackgroundColor", android.graphics.Color.TRANSPARENT)
                    
                    // Headers
                    views.setTextColor(R.id.widget_title_label, android.graphics.Color.parseColor("#111827"))
                    views.setTextColor(R.id.widget_stats_title_label, android.graphics.Color.parseColor("#111827"))
                    views.setTextColor(R.id.widget_no_exams_text, android.graphics.Color.parseColor("#64748B"))

                    // Exam Slots
                    views.setTextColor(R.id.widget_exam1_name, android.graphics.Color.parseColor("#111827"))
                    views.setTextColor(R.id.widget_exam1_days, android.graphics.Color.parseColor("#2563EB"))
                    views.setTextColor(R.id.widget_exam2_name, android.graphics.Color.parseColor("#374151"))
                    views.setTextColor(R.id.widget_exam2_days, android.graphics.Color.parseColor("#2563EB"))
                    views.setTextColor(R.id.widget_exam3_name, android.graphics.Color.parseColor("#374151"))
                    views.setTextColor(R.id.widget_exam3_days, android.graphics.Color.parseColor("#2563EB"))

                    // Stats Values & Labels
                    views.setTextColor(R.id.widget_stat_focus_value, android.graphics.Color.parseColor("#111827"))
                    views.setTextColor(R.id.widget_stat_focus_label, android.graphics.Color.parseColor("#64748B"))
                    views.setTextColor(R.id.widget_stat_points_value, android.graphics.Color.parseColor("#111827"))
                    views.setTextColor(R.id.widget_stat_points_label, android.graphics.Color.parseColor("#64748B"))
                    views.setTextColor(R.id.widget_stat_sessions_value, android.graphics.Color.parseColor("#111827"))
                    views.setTextColor(R.id.widget_stat_sessions_label, android.graphics.Color.parseColor("#64748B"))
                    views.setTextColor(R.id.widget_stat_streak_value, android.graphics.Color.parseColor("#111827"))
                    views.setTextColor(R.id.widget_stat_streak_label, android.graphics.Color.parseColor("#64748B"))

                    // Icons
                    val iconColor = android.graphics.Color.parseColor("#334155")
                    views.setInt(R.id.widget_icon_exams_header, "setColorFilter", iconColor)
                    views.setInt(R.id.widget_icon_stats_header, "setColorFilter", iconColor)
                    views.setInt(R.id.widget_icon_focus, "setColorFilter", iconColor)
                    views.setInt(R.id.widget_icon_points, "setColorFilter", iconColor)
                    views.setInt(R.id.widget_icon_sessions, "setColorFilter", iconColor)
                    views.setInt(R.id.widget_icon_streak, "setColorFilter", iconColor)

                    views.setInt(R.id.widget_vertical_divider, "setBackgroundColor", android.graphics.Color.parseColor("#20000000"))
                } else {
                    views.setInt(R.id.widget_overlay, "setBackgroundColor", android.graphics.Color.TRANSPARENT)

                    // Headers
                    views.setTextColor(R.id.widget_title_label, android.graphics.Color.parseColor("#F9FAFB"))
                    views.setTextColor(R.id.widget_stats_title_label, android.graphics.Color.parseColor("#F9FAFB"))
                    views.setTextColor(R.id.widget_no_exams_text, android.graphics.Color.parseColor("#9CA3AF"))

                    // Exam Slots
                    views.setTextColor(R.id.widget_exam1_name, android.graphics.Color.parseColor("#F9FAFB"))
                    views.setTextColor(R.id.widget_exam1_days, android.graphics.Color.parseColor("#60A5FA"))
                    views.setTextColor(R.id.widget_exam2_name, android.graphics.Color.parseColor("#E5E7EB"))
                    views.setTextColor(R.id.widget_exam2_days, android.graphics.Color.parseColor("#60A5FA"))
                    views.setTextColor(R.id.widget_exam3_name, android.graphics.Color.parseColor("#E5E7EB"))
                    views.setTextColor(R.id.widget_exam3_days, android.graphics.Color.parseColor("#60A5FA"))

                    // Stats Values & Labels
                    views.setTextColor(R.id.widget_stat_focus_value, android.graphics.Color.parseColor("#F9FAFB"))
                    views.setTextColor(R.id.widget_stat_focus_label, android.graphics.Color.parseColor("#9CA3AF"))
                    views.setTextColor(R.id.widget_stat_points_value, android.graphics.Color.parseColor("#F9FAFB"))
                    views.setTextColor(R.id.widget_stat_points_label, android.graphics.Color.parseColor("#9CA3AF"))
                    views.setTextColor(R.id.widget_stat_sessions_value, android.graphics.Color.parseColor("#F9FAFB"))
                    views.setTextColor(R.id.widget_stat_sessions_label, android.graphics.Color.parseColor("#9CA3AF"))
                    views.setTextColor(R.id.widget_stat_streak_value, android.graphics.Color.parseColor("#F9FAFB"))
                    views.setTextColor(R.id.widget_stat_streak_label, android.graphics.Color.parseColor("#9CA3AF"))

                    // Icons
                    val iconColor = android.graphics.Color.parseColor("#E2E8F0")
                    views.setInt(R.id.widget_icon_exams_header, "setColorFilter", iconColor)
                    views.setInt(R.id.widget_icon_stats_header, "setColorFilter", iconColor)
                    views.setInt(R.id.widget_icon_focus, "setColorFilter", iconColor)
                    views.setInt(R.id.widget_icon_points, "setColorFilter", iconColor)
                    views.setInt(R.id.widget_icon_sessions, "setColorFilter", iconColor)
                    views.setInt(R.id.widget_icon_streak, "setColorFilter", iconColor)

                    views.setInt(R.id.widget_vertical_divider, "setBackgroundColor", android.graphics.Color.parseColor("#1FFFFFFF"))
                }

                // Format numbers
                val focusStr = if (totalFocusHours >= 10.0) {
                    "${totalFocusHours.toInt()}h"
                } else {
                    "${totalSeconds / 60}m"
                }

                views.setTextViewText(R.id.widget_stat_focus_value, focusStr)
                views.setTextViewText(R.id.widget_stat_points_value, "$totalPoints")
                views.setTextViewText(R.id.widget_stat_sessions_value, "$sessionsCount")
                views.setTextViewText(R.id.widget_stat_streak_value, "${currentStreak}d")

                // Bind list of exams
                if (exams.isEmpty()) {
                    views.setViewVisibility(R.id.widget_no_exams_text, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_exams_list_container, View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_no_exams_text, View.GONE)
                    views.setViewVisibility(R.id.widget_exams_list_container, View.VISIBLE)

                    if (exams.size > 0) {
                        views.setViewVisibility(R.id.widget_exam1_container, View.VISIBLE)
                        val ex = exams[0]
                        views.setTextViewText(R.id.widget_exam1_name, ex.name)
                        views.setTextViewText(R.id.widget_exam1_days, getDaysLeftStringSpecial(ex.examDate, todayCalendar.timeInMillis))
                    } else views.setViewVisibility(R.id.widget_exam1_container, View.GONE)

                    if (exams.size > 1) {
                        views.setViewVisibility(R.id.widget_exam2_container, View.VISIBLE)
                        val ex = exams[1]
                        views.setTextViewText(R.id.widget_exam2_name, ex.name)
                        views.setTextViewText(R.id.widget_exam2_days, getDaysLeftString(ex.examDate, todayCalendar.timeInMillis))
                    } else views.setViewVisibility(R.id.widget_exam2_container, View.GONE)

                    if (exams.size > 2) {
                        views.setViewVisibility(R.id.widget_exam3_container, View.VISIBLE)
                        val ex = exams[2]
                        views.setTextViewText(R.id.widget_exam3_name, ex.name)
                        views.setTextViewText(R.id.widget_exam3_days, getDaysLeftString(ex.examDate, todayCalendar.timeInMillis))
                    } else views.setViewVisibility(R.id.widget_exam3_container, View.GONE)
                }

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    private fun getDaysLeftString(examTime: Long, todayTime: Long): String {
        val examCalendar = Calendar.getInstance().apply {
            timeInMillis = examTime
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val diffMs = examCalendar.timeInMillis - todayTime
        val daysLeft = (diffMs / (24 * 60 * 60 * 1000L)).toInt()
        return when {
            daysLeft < 0 -> "Passed"
            daysLeft == 0 -> "Today"
            else -> "${daysLeft}d"
        }
    }

    private fun getDaysLeftStringSpecial(examTime: Long, todayTime: Long): String {
        val examCalendar = Calendar.getInstance().apply {
            timeInMillis = examTime
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val diffMs = examCalendar.timeInMillis - todayTime
        val daysLeft = (diffMs / (24 * 60 * 60 * 1000L)).toInt()
        val adjustedDays = daysLeft + 2
        return when {
            adjustedDays < 0 -> "Passed"
            adjustedDays == 0 -> "Today"
            adjustedDays == 1 -> "1 Day"
            else -> "$adjustedDays Days"
        }
    }

    private fun calculateStreak(completedSessions: List<SessionEntity>): Int {
        if (completedSessions.isEmpty()) return 0
        val calendar = Calendar.getInstance()
        val uniqueDays = completedSessions.map {
            calendar.timeInMillis = it.date
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }.distinct().sortedDescending()

        if (uniqueDays.isEmpty()) return 0
        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val checkTime = todayCalendar.timeInMillis
        val newestDay = uniqueDays.first()

        if (newestDay != checkTime && newestDay != (checkTime - oneDayMillis)) return 0

        var currentStreak = 1
        var lastDay = newestDay
        for (i in 1 until uniqueDays.size) {
            if (lastDay - uniqueDays[i] == oneDayMillis) {
                currentStreak++
                lastDay = uniqueDays[i]
            } else if (lastDay - uniqueDays[i] > oneDayMillis) {
                break
            }
        }
        return currentStreak
    }

    private fun getNeumorphicWidgetBitmap(context: Context, isDay: Boolean): Bitmap {
        val W = 600f
        val H = 400f
        try {
            val imageBitmap = ImageBitmap(W.toInt(), H.toInt())
            val composeCanvas = Canvas(imageBitmap)
            val drawScope = CanvasDrawScope()

            drawScope.draw(
                density = androidx.compose.ui.unit.Density(context),
                layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
                canvas = composeCanvas,
                size = Size(W, H)
            ) {
                val margin = 12f
                val cardRect = Offset(margin, margin)
                val cardSize = Size(W - margin * 2, H - margin * 2)
                val corner = androidx.compose.ui.geometry.CornerRadius(28f, 28f)

                if (isDay) {
                    // Soft dark drop shadow
                    drawRoundRect(
                        color = Color(0x288C9BAE),
                        topLeft = Offset(margin + 5f, margin + 5f),
                        size = cardSize,
                        cornerRadius = corner
                    )
                    // Soft white top-left highlight
                    drawRoundRect(
                        color = Color(0xF0FFFFFF),
                        topLeft = Offset(margin - 3f, margin - 3f),
                        size = cardSize,
                        cornerRadius = corner
                    )
                    // Translucent clean white body
                    drawRoundRect(
                        color = Color(0xEDF3F6F9),
                        topLeft = cardRect,
                        size = cardSize,
                        cornerRadius = corner,
                        style = androidx.compose.ui.graphics.drawscope.Fill
                    )
                    // Glassy rim border
                    drawRoundRect(
                        color = Color(0x90FFFFFF),
                        topLeft = cardRect,
                        size = cardSize,
                        cornerRadius = corner,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f)
                    )
                } else {
                    // Soft deep dark drop shadow
                    drawRoundRect(
                        color = Color(0x95000000),
                        topLeft = Offset(margin + 5f, margin + 5f),
                        size = cardSize,
                        cornerRadius = corner
                    )
                    // Soft light highlight glow
                    drawRoundRect(
                        color = Color(0x28FFFFFF),
                        topLeft = Offset(margin - 2f, margin - 2f),
                        size = cardSize,
                        cornerRadius = corner
                    )
                    // Translucent clean obsidian body
                    drawRoundRect(
                        color = Color(0xED0F131A),
                        topLeft = cardRect,
                        size = cardSize,
                        cornerRadius = corner,
                        style = androidx.compose.ui.graphics.drawscope.Fill
                    )
                    // Glassy rim border
                    drawRoundRect(
                        color = Color(0x22FFFFFF),
                        topLeft = cardRect,
                        size = cardSize,
                        cornerRadius = corner,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8f)
                    )
                }
            }
            return imageBitmap.asAndroidBitmap()
        } catch (e: Exception) {
            val fallback = Bitmap.createBitmap(W.toInt(), H.toInt(), Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(fallback)
            val paint = android.graphics.Paint()
            paint.color = if (isDay) android.graphics.Color.parseColor("#EDF3F6F9") else android.graphics.Color.parseColor("#ED0F131A")
            canvas.drawRoundRect(12f, 12f, W - 12f, H - 12f, 28f, 28f, paint)
            return fallback
        }
    }

    companion object {
        fun triggerWidgetUpdate(context: Context) {
            val intent1 = Intent(context, ExamCountdownWidgetReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent1)

            val intent2 = Intent(context, ExamMatrixWidgetReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent2)

            val intent3 = Intent("com.example.ACTION_WIDGET_AUTO_UPDATE")
            context.sendBroadcast(intent3)
        }

        fun scheduleDayNightAlarm(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
                val intent = Intent(context, ExamCountdownWidgetReceiver::class.java).apply {
                    action = "com.example.ACTION_WIDGET_AUTO_UPDATE"
                }
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    9901,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                val now = System.currentTimeMillis()
                val nextHour = now + (30 * 60 * 1000L)
                alarmManager.setInexactRepeating(
                    android.app.AlarmManager.RTC,
                    nextHour,
                    android.app.AlarmManager.INTERVAL_HALF_HOUR,
                    pendingIntent
                )
            } catch (e: Exception) {
                android.util.Log.e("ExamCountdownWidget", "Error scheduling alarm: ${e.message}")
            }
        }
    }
}
```

### `ExamMatrixWidgetReceiver.kt`
```kotlin
package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.db.entity.ExamEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ExamMatrixWidgetReceiver : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        ExamCountdownWidgetReceiver.scheduleDayNightAlarm(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        ExamCountdownWidgetReceiver.scheduleDayNightAlarm(context)
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        ExamCountdownWidgetReceiver.scheduleDayNightAlarm(context)
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, ExamMatrixWidgetReceiver::class.java))
        updateAllWidgets(context, manager, ids)
    }

    private fun updateAllWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return

        val database = AppDatabase.getDatabase(context)
        val examDao = database.examDao()

        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isDay = hour in 6..17

        CoroutineScope(Dispatchers.IO).launch {
            val upcomingExams = examDao.getUpcomingExams(todayCalendar.timeInMillis)
            val matrixBitmap = generateMatrixBitmap(todayCalendar, upcomingExams, isDay)

            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                101,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_exam_matrix_layout)
                views.setImageViewBitmap(R.id.widget_matrix_image, matrixBitmap)
                views.setOnClickPendingIntent(R.id.widget_matrix_container, pendingIntent)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    private fun generateMatrixBitmap(
        todayCal: Calendar,
        upcomingExams: List<ExamEntity>,
        isDay: Boolean
    ): Bitmap {
        val width = 880f
        val height = 480f
        val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Neumorphic Translucent Card Background
        val margin = 16f
        val cardRect = RectF(margin, margin, width - margin, height - margin)
        val corner = 32f

        if (isDay) {
            val darkShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#288C9BAE")
                style = Paint.Style.FILL
            }
            val lightGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#F0FFFFFF")
                style = Paint.Style.FILL
            }
            val bodyFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#EDF3F6F9")
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#90FFFFFF")
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
            }

            canvas.drawRoundRect(RectF(margin + 6f, margin + 6f, width - margin + 6f, height - margin + 6f), corner, corner, darkShadowPaint)
            canvas.drawRoundRect(RectF(margin - 4f, margin - 4f, width - margin - 4f, height - margin - 4f), corner, corner, lightGlowPaint)
            canvas.drawRoundRect(cardRect, corner, corner, bodyFillPaint)
            canvas.drawRoundRect(cardRect, corner, corner, borderPaint)
        } else {
            val darkShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#95000000")
                style = Paint.Style.FILL
            }
            val lightGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#28FFFFFF")
                style = Paint.Style.FILL
            }
            val bodyFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#ED0F131A")
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#22FFFFFF")
                style = Paint.Style.STROKE
                strokeWidth = 2.2f
            }

            canvas.drawRoundRect(RectF(margin + 6f, margin + 6f, width - margin + 6f, height - margin + 6f), corner, corner, darkShadowPaint)
            canvas.drawRoundRect(RectF(margin - 3f, margin - 3f, width - margin - 3f, height - margin - 3f), corner, corner, lightGlowPaint)
            canvas.drawRoundRect(cardRect, corner, corner, bodyFillPaint)
            canvas.drawRoundRect(cardRect, corner, corner, borderPaint)
        }

        // 2. Compute Matrix Dates
        val monthLabels = arrayOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        val currentYear = todayCal.get(Calendar.YEAR)
        val currentMonth = todayCal.get(Calendar.MONTH)
        val todayDayOfYear = todayCal.get(Calendar.DAY_OF_YEAR)
        val totalDaysInYear = todayCal.getActualMaximum(Calendar.DAY_OF_YEAR).toFloat()
        val yearProgressPercent = ((todayDayOfYear / totalDaysInYear) * 100).toInt()

        val nextExam = upcomingExams.firstOrNull()
        val nextExamCal = nextExam?.let { Calendar.getInstance().apply { timeInMillis = it.examDate } }

        val daysUntilExam = if (nextExam != null) {
            val examStart = Calendar.getInstance().apply {
                timeInMillis = nextExam.examDate
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val diffMs = examStart.timeInMillis - todayCal.timeInMillis
            (diffMs / (1000 * 60 * 60 * 24)).coerceAtLeast(0).toInt()
        } else -1

        val examDayOfYears = upcomingExams.mapNotNull { exam ->
            val cal = Calendar.getInstance().apply { timeInMillis = exam.examDate }
            if (cal.get(Calendar.YEAR) == currentYear) cal.get(Calendar.DAY_OF_YEAR) else null
        }.toSet()

        val nextExamDayOfYear = nextExamCal?.let {
            if (it.get(Calendar.YEAR) == currentYear) it.get(Calendar.DAY_OF_YEAR) else null
        }

        // 3. Dot Grid Painting Logic
        val activeMonthColor = if (isDay) android.graphics.Color.parseColor("#111827") else android.graphics.Color.parseColor("#F9FAFB")
        val dimMonthColor = if (isDay) android.graphics.Color.parseColor("#64748B") else android.graphics.Color.parseColor("#9CA3AF")

        val labelPaintActive = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = activeMonthColor; textSize = 21f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val labelPaintDim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dimMonthColor; textSize = 21f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        val passedColor = if (isDay) android.graphics.Color.parseColor("#334155") else android.graphics.Color.parseColor("#E5E7EB")
        val todayRingColor = if (isDay) android.graphics.Color.parseColor("#2563EB") else android.graphics.Color.parseColor("#60A5FA")
        val runupColor = if (isDay) android.graphics.Color.parseColor("#7C3AED") else android.graphics.Color.parseColor("#A78BFA")
        val examOuterColor = if (isDay) android.graphics.Color.parseColor("#DC2626") else android.graphics.Color.parseColor("#EF4444")
        val futureColor = if (isDay) android.graphics.Color.parseColor("#CBD5E1") else android.graphics.Color.parseColor("#374151")

        val dotPassedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = passedColor; style = Paint.Style.FILL }
        val dotTodayRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = todayRingColor; style = Paint.Style.STROKE; strokeWidth = 2.5f }
        val dotTodayInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = todayRingColor; style = Paint.Style.FILL }
        val dotExamRunupPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = runupColor; style = Paint.Style.FILL }
        val dotExamDayOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = examOuterColor; style = Paint.Style.STROKE; strokeWidth = 2.5f }
        val dotExamDayInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = examOuterColor; style = Paint.Style.FILL }
        val dotFuturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = futureColor; style = Paint.Style.FILL }

        val gridTop = 38f; val gridHeight = 358f; val rowHeight = gridHeight / 12f
        val startX = 142f; val endX = width - 42f; val availableWidth = endX - startX
        val dotStepX = availableWidth / 31f
        val tempCal = Calendar.getInstance()

        for (m in 0..11) {
            val rowY = gridTop + (m * rowHeight) + (rowHeight / 2f) + 4f
            val isCurrentMonth = (m == currentMonth)

            canvas.drawText(monthLabels[m], 42f, rowY + 6f, if (isCurrentMonth) labelPaintActive else labelPaintDim)

            tempCal.set(currentYear, m, 1)
            val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

            for (d in 1..daysInMonth) {
                tempCal.set(currentYear, m, d)
                val dDayOfYear = tempCal.get(Calendar.DAY_OF_YEAR)
                val dotX = startX + (d - 1) * dotStepX + (dotStepX / 2f)

                val isToday = (dDayOfYear == todayDayOfYear)
                val isExamDay = examDayOfYears.contains(dDayOfYear)
                val isExamRunup = nextExamDayOfYear != null && (dDayOfYear > todayDayOfYear && dDayOfYear < nextExamDayOfYear)
                val isPast = (dDayOfYear < todayDayOfYear)

                when {
                    isExamDay -> {
                        canvas.drawCircle(dotX, rowY, 6.5f, dotExamDayOuterPaint)
                        canvas.drawCircle(dotX, rowY, 3.8f, dotExamDayInnerPaint)
                    }
                    isToday -> {
                        canvas.drawCircle(dotX, rowY, 5.5f, dotTodayRingPaint)
                        canvas.drawCircle(dotX, rowY, 2.2f, dotTodayInnerPaint)
                    }
                    isExamRunup -> {
                        canvas.drawCircle(dotX, rowY, 4.2f, dotExamRunupPaint)
                    }
                    isPast -> {
                        canvas.drawCircle(dotX, rowY, 4.0f, dotPassedPaint)
                    }
                    else -> {
                        canvas.drawCircle(dotX, rowY, 3.6f, dotFuturePaint)
                    }
                }
            }
        }

        // 4. Footer Status Bar
        val footerLineY = 414f
        val dividerColor = if (isDay) android.graphics.Color.parseColor("#20000000") else android.graphics.Color.parseColor("#1FFFFFFF")
        canvas.drawLine(42f, footerLineY, endX, footerLineY, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dividerColor; strokeWidth = 1.5f })

        val footerY = 450f
        val textLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDay) android.graphics.Color.parseColor("#111827") else android.graphics.Color.parseColor("#F9FAFB")
            textSize = 21f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDay) android.graphics.Color.parseColor("#2563EB") else android.graphics.Color.parseColor("#60A5FA")
            textSize = 20f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val footerLeftText: String
        val footerRightText: String

        if (nextExam != null) {
            val examTitle = if (nextExam.name.length > 18) nextExam.name.take(16) + "…" else nextExam.name
            footerLeftText = "NEXT EXAM: ${examTitle.uppercase()}"
            footerRightText = if (daysUntilExam == 0) "TODAY!" else "IN $daysUntilExam DAYS"
        } else {
            footerLeftText = "YEAR PROGRESS - $yearProgressPercent%"
            footerRightText = "NO UPCOMING EXAMS"
        }

        canvas.drawText(footerLeftText, 42f, footerY, textLeftPaint)
        val rightWidth = textRightPaint.measureText(footerRightText)
        canvas.drawText(footerRightText, endX - rightWidth, footerY, textRightPaint)

        return bitmap
    }

    companion object {
        fun triggerWidgetUpdate(context: Context) {
            val intent = Intent(context, ExamMatrixWidgetReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent)
        }
    }
}
```

---

## 🚀 How to Add Widgets to Your Android Home Screen

1. Long-press on any empty space on your phone's **Home Screen**.
2. Tap **Widgets**.
3. Scroll down and locate **FocusFlow**.
4. Select either:
   - **Exam Countdown & Focus Stats**
   - **Exam Matrix & Year Progress**
5. Drag and place the widget on your Home Screen.
6. The widget automatically reads your real-time exam schedule and focus statistics, updating seamlessly between **Day** and **Night** themes according to local time!
