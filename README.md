# Secu3droid

[![License](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)

**Secu3droid** is a powerful Android client for the **Secu-3 engine control unit (ECU)**. It allows enthusiasts and professionals to monitor, configure, and manage their Secu-3 units directly from their mobile devices.

---

## 🚀 Features

- **Multi-Interface Connectivity**: Connect via **Bluetooth** or **USB** serial interfaces.
- **Real-time Dashboard**: Monitor engine parameters (RPM, MAP, Temperature, etc.) using interactive gauges and indicators.
- **On-the-go Configuration**: Change Secu-3 unit settings and maps without needing a laptop.
- **Data Logging**: Keep track of engine performance and diagnostic data.
- **Modern UI**: Built with Jetpack Compose, Material 3, and Navigation Compose.
- **Persistent settings**: Existing preferences remain in the Android default shared-preferences store, including settings created by older app versions.

## 🛠 Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
- **Database**: [Room](https://developer.android.com/training/data-storage/room)
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & OkHttp
- **Serialization**: Gson
- **Navigation**: [Navigation Compose](https://developer.android.com/develop/ui/compose/navigation)
- **Hardware Interaction**: Bluetooth API & `usb-serial-for-android`
- **Dashboard**: Compose-based gauges and indicators backed by the Room database

## 📦 Getting Started

### Prerequisites

- Android Studio with Android SDK 37 and build tools 36.1.0 installed
- JDK 21 for the Gradle 9.6.1 build; app bytecode targets Java 17
- A Secu-3 unit with Bluetooth or USB

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/kosharskyi/secu3droid.git
   ```
2. Open the project in Android Studio.

### Build

Debug builds use the default Android debug signing configuration and do not require
`keystore.properties`:

```bash
./gradlew :app:assembleDebug
```

Release builds require signing values in `keystore.properties` or the corresponding `RELEASE_*` environment variables:

```properties
KEY_ALIAS=your-key-alias
KEY_PASSWORD=your-key-password
KEYSTORE_PASSWORD=your-keystore-password
KEYSTORE_PATH=/path/to/release-keystore.jks
```

### Checks

Run unit tests, build both debug APKs, and run Android lint with:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

See [Android lint triage](knowledge/lint-triage.md) for the remaining warning categories and why they were deferred.

Instrumentation tests require a connected Android device or emulator:

```bash
./gradlew :app:connectedDebugAndroidTest
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request or open an issue for bugs and feature requests.

## 📄 License

This project is licensed under the **GNU General Public License v3.0**. See the [license](license) file for details.

---
*Maintained by [Vitalii Kosharskyi](https://github.com/kosharskyi)*
