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
- **Modern UI**: Built with Jetpack Compose for a fluid and responsive experience.

## 🛠 Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) & Material 3
- **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
- **Database**: [Room](https://developer.android.com/training/data-storage/room)
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & OkHttp
- **Serialization**: Gson
- **Navigation**: Jetpack Navigation Component
- **Hardware Interaction**: Bluetooth API & `usb-serial-for-android`
- **Visualization**: [SpeedView](https://github.com/anastr/SpeedView) for dashboard gauges

## 📦 Getting Started

### Prerequisites

- Android Studio Koala or newer
- JDK 17+
- A Secu-3 unit with Bluetooth or USB-Serial adapter

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/kosharskyi/secu3droid.git
   ```
2. Open the project in Android Studio.

### Quick Setup

You can use the provided setup script to automatically create the `keystore.properties` file:

- **Linux/macOS**:
  ```bash
  chmod +x setup.sh
  ./setup.sh
  ```
- **Windows**:
  ```batch
  setup.bat
  ```

Alternatively, you can manually create the file:

1. Create a file named `keystore.properties` in the project root.
2. Add the following content:

```properties
KEY_ALIAS=debug
KEY_PASSWORD=debug
KEYSTORE_PASSWORD=debug
KEYSTORE_PATH=debug.jks
```

*Note: Ensure you have a `debug.jks` file in the root if you are using these values (default for debug builds).*

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request or open an issue for bugs and feature requests.

## 📄 License

This project is licensed under the **GNU General Public License v3.0**. See the [license](license) file for details.

---
*Maintained by [Vitalii Kosharskyi](https://github.com/kosharskyi)*
