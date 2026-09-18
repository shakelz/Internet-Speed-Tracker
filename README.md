# 🚀 NetSpeed Pro — Real-Time Internet Speed Tracker & Data Usage Monitor

A modern, feature-packed native Android application built with Kotlin, Jetpack components, Room Database, and Material Design 3. NetSpeed Pro tracks real-time network speeds, displays a live dynamic status bar indicator beside the clock/date, breaks down daily SIM & Wi-Fi consumption, monitors per-app data usage, runs in-depth speed tests, and provides 30-day analytics with CSV export.

---

## ✨ Features

- **⚡ Real-Time Status Bar Speed Indicator:**
  - Displays dynamic download & upload speed numbers directly in the top Android status bar beside the clock/date even when the notification shade is closed.
  - Updates seamlessly every second using low-overhead native TrafficStats.

- **📊 Comprehensive Notification Drawer:**
  - Expanded notification card displaying live Download and Upload rates.
  - Today's Mobile (SIM) and Wi-Fi data consumption side-by-side.
  - Active network name and connection status.

- **📱 Smart Dashboard:**
  - Real-time speedometer cards with smooth animations.
  - Separate counters for SIM Mobile Data (detects carrier name like Jio, Airtel, Vi, freenet, etc.) and Wi-Fi networks.
  - Live network diagnostics: Signal strength (dBm), link speed, and network frequency (4G / 5G LTE / Wi-Fi).

- **🔍 Per-App Data Usage Tracker:**
  - Accurate breakdown of data consumed by each installed app.
  - Filtering by Today, Yesterday, Last 7 Days, and This Month.
  - Visual progress indicators for high-bandwidth apps.

- **🏎️ Animated In-App Speed Test:**
  - Built-in multi-threaded speed test engine.
  - Measures Ping latency (ms), Jitter, Download speed (Mbps), and Upload speed (Mbps).
  - Custom canvas-rendered interactive speedometer gauge.

- **📈 History & Analytics:**
  - 7-day visual bar chart comparing daily Mobile vs Wi-Fi usage.
  - 30-day detailed usage history table.
  - One-click CSV export to device storage.

- **⚙️ Pro Customization & Alerts:**
  - Configurable daily data limit quota warnings (80%, 90%, 100% threshold notifications).
  - Unit toggle: Bytes/sec (`KB/s`, `MB/s`) or Bits/sec (`Kbps`, `Mbps`).
  - Auto-start on boot with battery optimization guidance.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin
- **Architecture:** MVVM (Model-View-ViewModel) with Clean Separation
- **UI & Design:** Material Design 3, Jetpack ViewBinding, Navigation Component
- **Persistence:** Room Database (SQLite) + Encrypted SharedPreferences
- **Asynchrony:** Kotlin Coroutines & StateFlow
- **Background Processing:** Android Foreground Service with SpecialUse FGS subtype (Android 14+ targetSdk 34 ready)
- **APIs:** Android `NetworkStatsManager`, `TrafficStats`, `ConnectivityManager`, `TelephonyManager`

---

## 📲 Installation & Build

### Prerequisites
- Android Studio Hedgehog / Iguana / Jellyfish or newer
- JDK 17
- Android SDK 34

### Building from Source
```bash
git clone https://github.com/shakelz/Internet-Speed-Tracker.git
cd Internet-Speed-Tracker
./gradlew assembleDebug
```

### Installing via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License
This project is open source and available under the [MIT License](LICENSE).
