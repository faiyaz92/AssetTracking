# Gradle Build Configuration Documentation

This document contains all the working versions and configurations for the AssetTracking Android project to ensure consistent builds without version conflicts or unnecessary Gradle downloads.

## Build System Versions

### Gradle
- **Gradle Version**: 8.5
- **Distribution URL**: https://services.gradle.org/distributions/gradle-8.5-bin.zip
- **Gradle Wrapper Properties**: Located in `gradle/wrapper/gradle-wrapper.properties`

### Android Gradle Plugin (AGP)
- **Version**: 8.1.4
- **Applied in**: Root `build.gradle` file

### Kotlin
- **Version**: 1.9.22
- **Applied in**: Root `build.gradle` file

## Java Runtime
- **Java Version**: 21.0.4 (Eclipse Adoptium/Temurin)
- **JVM**: OpenJDK 64-Bit Server VM

## Android SDK Configuration
- **compileSdk**: 34
- **minSdk**: 23
- **targetSdk**: 34

## Key Dependencies

### Compose BOM
- **Version**: 2023.03.00

### Room Database
- **Version**: 2.6.1

### Hilt (Dependency Injection)
- **Version**: 2.51.1

### Kotlin Coroutines
- **Version**: 1.6.4

### ZXing (Barcode Scanning)
- **Version**: 4.3.0

### Chainway C72 UHF RFID SDK
- **AAR File**: `DeviceAPI_ver20250209_release.aar`
- **Location**: `app/libs/DeviceAPI_ver20250209_release.aar`
- **Gradle Integration**: `implementation files('libs/DeviceAPI_ver20250209_release.aar')`
- **Package**: `com.rscja.deviceapi`
- **Main Class**: `RFIDWithUHFUART`
- **Compatibility**: Android API 23+ (minSdk 23)
- **Hardware**: Chainway C72 UHF RFID Reader (UART interface)
- **Features**: Read, Write, Inventory scanning for UHF RFID tags

### Other Dependencies
- **Material3**: Included in Compose BOM
- **Lifecycle**: Included in Compose BOM
- **Activity Compose**: Included in Compose BOM
- **Thermal Printer**: ESCPOS-ThermalPrinter-Android v3.2.0

## Gradle Properties Configuration

Located in `gradle.properties`:

```
# Project-wide Gradle settings.
# IDE (e.g. Android Studio) users:
# Gradle settings configured through the IDE *will override*
# any settings specified in this file.
# For more details on how to configure your build environment visit
# http://www.gradle.org/docs/current/userguide/build_environment.html
# Specifies the JVM arguments used for the daemon process.
# The setting is particularly useful for tweaking memory settings.
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
# When configured, Gradle will run in incubating parallel mode.
# This option should only be used with decoupled projects. More details, visit
# http://www.gradle.org/docs/current/userguide/multi_project_builds.html#sec:decoupled_projects
# org.gradle.parallel=true
# AndroidX package structure to make it clearer which packages are bundled with the
# Android operating system, and which are packaged with your app's APK
# https://developer.android.com/topic/libraries/support-library/androidx-r8
android.useAndroidX=true
# Kotlin code style for this project: "official" or "obsolete":
kotlin.code.style=official
# Enables namespacing of each library's R class so that its R class includes only the
# resources declared in the library itself and none from the library's dependencies,
# thereby reducing the size of the R class for that library
android.nonTransitiveRClass=true
```

## Build Commands

### Clean Build
```bash
./gradlew clean
```

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Build Release APK
```bash
./gradlew assembleRelease
```

### Install Debug APK on Connected Device
```bash
./gradlew installDebug
```

### Run Tests
```bash
./gradlew test
```

### Quick Debug Build and Install
```bash
./gradlew clean assembleDebug installDebug
```

## Current Implementation Features

### Core Functionality
- **Asset Management**: Room database for storing asset information
- **RFID Integration**: Real Chainway C72 UHF RFID reader support
- **Barcode Scanning**: ZXing library for QR code and barcode reading
- **Bluetooth Connectivity**: For peripheral device communication
- **Thermal Printing**: ESCPOS-compatible receipt printing

### RFID Operations (Real Hardware)
- **Read Tags**: Single tag EPC reading with `inventorySingleTag()`
- **Write Tags**: EPC data writing to UHF RFID tags
- **Inventory Scan**: Multi-tag detection with continuous scanning
- **Tag Management**: Asset ID assignment and tracking

### UI Framework
- **Jetpack Compose**: Modern Android UI with Material 3 design
- **Navigation**: Single-activity architecture with Compose navigation
- **Dependency Injection**: Hilt for clean architecture
- **ViewModels**: Lifecycle-aware state management

### Testing & Quality
- **Unit Tests**: JUnit 4 for business logic testing
- **UI Tests**: Compose UI testing support
- **Instrumentation Tests**: Espresso for integration testing

## Important Notes

1. **Version Compatibility**: These specific versions have been tested and work together without conflicts. Do not change versions unless absolutely necessary.

2. **Chainway RFID SDK AAR File**: The `DeviceAPI_ver20250209_release.aar` file must be present in `app/libs/` directory for RFID functionality. This AAR file contains the native libraries and Java classes for Chainway C72 UHF RFID reader communication.

3. **RFID Hardware Requirements**:
   - Chainway C72 UHF RFID reader connected via USB/UART
   - Android device with USB host support (or UART serial connection)
   - UHF RFID tags compatible with the reader
   - Required permissions: CAMERA, BLUETOOTH, LOCATION, EXTERNAL_STORAGE, USB_HOST

4. **SDK Dependencies**: The RFID SDK depends on:
   - Android API level 23+ (Android 6.0+)
   - USB host API for hardware communication
   - Location permissions for UHF RFID regulatory compliance

5. **Gradle Cache**: If you experience repeated downloads, clear the Gradle cache:
   ```bash
   ./gradlew cleanBuildCache
   rm -rf ~/.gradle/caches
   ```

6. **Java Version**: Ensure Java 21.0.4 is used. Other versions may cause compilation issues.

7. **Android Studio**: Use Android Studio with built-in JDK or configure to use the same Java version.

8. **Device Installation**: App has been tested on Motorola Edge 50 Pro device. Use `adb devices` to verify device connection before installation.

## Troubleshooting

- **Gradle Sync Issues**: Ensure all versions match exactly as documented above.
- **Build Failures**: Check that Java 21.0.4 is the active JDK.
- **Dependency Conflicts**: All dependencies are compatible with the listed versions.
- **Cache Issues**: Clear Gradle cache and restart Android Studio if builds fail unexpectedly.
- **RFID SDK Issues**:
  - Ensure `DeviceAPI_ver20250209_release.aar` is in `app/libs/` directory
  - Verify USB permissions are granted for RFID reader connection
  - Check that device supports USB host mode (required for some RFID readers)
  - Confirm RFID reader is properly powered and connected
- **RFID Functionality**: If RFID operations fail, check device logs for `C72RfidReader` class errors
- **AAR File Missing**: Download the correct Chainway SDK version if AAR file is missing

## RFID SDK Integration Details

### SDK Classes Used
- `com.rscja.deviceapi.RFIDWithUHFUART` - Main RFID reader class
- `com.rscja.deviceapi.entity.UHFTAGInfo` - RFID tag data structure

### Key Methods
- `getInstance()` - Get singleton instance
- `init(context)` - Initialize with Android context
- `inventorySingleTag()` - Read single RFID tag
- `writeData(password, bank, ptr, len, data)` - Write to RFID tag
- `free()` - Release resources

### Hardware Compatibility
- **Reader**: Chainway C72 series
- **Interface**: UART serial communication
- **Frequency**: UHF (860-960 MHz)
- **Protocol**: EPC Class 1 Gen 2 compatible

### Permissions Required
```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-feature android:name="android.hardware.usb.host" android:required="false" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

## Last Updated
This configuration was validated on: December 6, 2025
Working build confirmed with successful device installation and RFID SDK integration.
- ✅ Real Chainway RFID SDK integration completed
- ✅ AAR file dependency properly configured
- ✅ Hardware testing confirmed on Motorola Edge 50 Pro
- ✅ All RFID operations (read/write/inventory) functional with real hardware