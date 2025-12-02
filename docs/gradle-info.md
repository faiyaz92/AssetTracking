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

### Other Dependencies
- **Material3**: Included in Compose BOM
- **Lifecycle**: Included in Compose BOM
- **Activity Compose**: Included in Compose BOM

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

## Important Notes

1. **Version Compatibility**: These specific versions have been tested and work together without conflicts. Do not change versions unless absolutely necessary.

2. **Gradle Cache**: If you experience repeated downloads, clear the Gradle cache:
   ```bash
   ./gradlew cleanBuildCache
   rm -rf ~/.gradle/caches
   ```

3. **Java Version**: Ensure Java 21.0.4 is used. Other versions may cause compilation issues.

4. **Android Studio**: Use Android Studio with built-in JDK or configure to use the same Java version.

5. **Device Installation**: App has been tested on Motorola Edge 50 Pro device. Use `adb devices` to verify device connection before installation.

## Troubleshooting

- **Gradle Sync Issues**: Ensure all versions match exactly as documented above.
- **Build Failures**: Check that Java 21.0.4 is the active JDK.
- **Dependency Conflicts**: All dependencies are compatible with the listed versions.
- **Cache Issues**: Clear Gradle cache and restart Android Studio if builds fail unexpectedly.

## Last Updated
This configuration was validated on: [Current Date]
Working build confirmed with successful device installation.