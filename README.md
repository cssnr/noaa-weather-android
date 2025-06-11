[![GitHub Downloads](https://img.shields.io/github/downloads/cssnr/noaa-weather-android/total?logo=github)](https://github.com/cssnr/noaa-weather-android/releases/latest/download/app-release.apk)
[![GitHub Release Version](https://img.shields.io/github/v/release/cssnr/noaa-weather-android?logo=github)](https://github.com/cssnr/noaa-weather-android/releases/latest)
[![Lint](https://img.shields.io/github/actions/workflow/status/cssnr/noaa-weather-android/lint.yaml?logo=github&logoColor=white&label=lint)](https://github.com/cssnr/noaa-weather-android/actions/workflows/lint.yaml)
[![GitHub Top Language](https://img.shields.io/github/languages/top/cssnr/noaa-weather-android?logo=htmx)](https://github.com/cssnr/noaa-weather-android)
[![GitHub Last Commit](https://img.shields.io/github/last-commit/cssnr/noaa-weather-android?logo=github&label=updated)](https://github.com/cssnr/noaa-weather-android/graphs/commit-activity)
[![GitHub Repo Size](https://img.shields.io/github/repo-size/cssnr/noaa-weather-android?logo=bookstack&logoColor=white&label=repo%20size)](https://github.com/cssnr/noaa-weather-android)
[![GitHub Discussions](https://img.shields.io/github/discussions/cssnr/noaa-weather-android)](https://github.com/cssnr/noaa-weather-android/discussions)
[![GitHub Forks](https://img.shields.io/github/forks/cssnr/noaa-weather-android?style=flat&logo=github)](https://github.com/cssnr/noaa-weather-android/forks)
[![GitHub Repo Stars](https://img.shields.io/github/stars/cssnr/noaa-weather-android?style=flat&logo=github)](https://github.com/cssnr/noaa-weather-android/stargazers)
[![GitHub Org Stars](https://img.shields.io/github/stars/cssnr?style=flat&logo=github&label=org%20stars)](https://cssnr.com/)
[![Discord](https://img.shields.io/discord/899171661457293343?logo=discord&logoColor=white&label=discord&color=7289da)](https://discord.gg/wXy6m2X8wY)

# NOAA Weather Android

[![GitHub Release](https://img.shields.io/github/v/release/cssnr/noaa-weather-android?style=for-the-badge&logo=android&label=Download%20Android%20APK&color=A4C639)](https://github.com/cssnr/noaa-weather-android/releases/latest/download/app-release.apk)

- [Install](#Install)
  - [Setup](#Setup)
- [Features](#Features)
  - [Planned](#Planned)
  - [Known Issues](#Known-Issues)
- [Development](#Development)
  - [Android Studio](#Android-Studio)
  - [Command Line](#Command-Line)
- [Support](#Support)
- [Contributing](#Contributing)

NOAA Weather Android Application.

- Supports Android 8 (API 26) 2017 +

## Install

> [!TIP]  
> To install, download and open the [latest release](https://github.com/cssnr/noaa-weather-android/releases/latest).
>
> [![GitHub Release](https://img.shields.io/github/v/release/cssnr/noaa-weather-android?style=for-the-badge&logo=android&label=Download%20Android%20APK&color=A4C639)](https://github.com/cssnr/noaa-weather-android/releases/latest/download/app-release.apk)

<details><summary>View QR Code 📸</summary>

[![QR Code](https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/noaa-weather/qr-code-dark.png)](https://github.com/cssnr/noaa-weather-android/releases/latest/download/app-release.apk)

</details>

_Note: Until published on the play store, you may need to allow installation of apps from unknown sources._

Downloading and Installing the [apk](https://github.com/cssnr/noaa-weather-android/releases/latest/download/app-release.apk)
should take you to the settings area to allow installation if not already enabled.
For more information, see [Release through a website](https://developer.android.com/studio/publish#publishing-website).

<details><summary>View Manual Steps to Install from Unknown Sources</summary>

1. Go to your device settings.
2. Search for "Install unknown apps" or similar.
3. Choose the app you will install the apk file from.
   - Select your web browser to install directly from it.
   - Select your file manager to open it, locate the apk and install from there.
4. Download the [Latest Release](https://github.com/cssnr/noaa-weather-android/releases/latest/download/app-release.apk).
5. Open the download apk in the app you selected in step #3.
6. Choose Install and Accept any Play Protect notifications.
7. The app is now installed. Proceed to the [Setup](#Setup) section below.

</details>

### Setup

1. [Install](#Install) and open the app on your device.
2. Go to the Stations page and click the Add button.
3. Click Locate, allow permissions, select a station.
4. You can also add the widget to your home screen.

## Features

- Add Multiple NOAA Stations with Search or GPS
- Widget with Current Conditions from Selected Station
- Custom Widget Update Interval and Temperature Unit

### Planned

- [Submit a Feature Request](https://github.com/cssnr/noaa-weather-android/discussions/categories/feature-requests)

### Known Issues

- [Open an Issue](https://github.com/cssnr/noaa-weather-android/issues)

# Development

This section briefly covers running and building in [Android Studio](#Android-Studio) and the [Command Line](#Command-Line).

## Android Studio

1. Download and Install Android Studio.

https://developer.android.com/studio

2. Ensure that usb or wifi debugging is enabled in the Android developer settings and verify.

3. Then build or run the app on your device.
   - Import the Project
   - Run Gradle Sync

To Run: Select a device and press Play ▶️

To Build:

- Select the Build Variant (debug or release)
- Build > Generate App Bundles or APK > Generate APKs

## Command Line

_Note: This section is a WIP! For more details see the [release.yaml](.github/workflows/release.yaml)._

You will need to have [ADB](https://developer.android.com/tools/adb) installed.

1. Download and Install the Android SDK Platform Tools.

https://developer.android.com/tools/releases/platform-tools#downloads

Ensure that `adb` is in your PATH.

2. List and verify the device is connected with:

```shell
$ adb devices
List of devices attached
RF9M33Z1Q0M     device
```

3. Build a debug or release apk.

```shell
./gradlew assemble
./gradlew assembleRelease
```

_Note: Use `gradlew.bat` for Windows._

4. Then install the apk to your device with adb.

```shell
$ cd app/build/outputs/apk/debug
$ adb -s RF9M33Z1Q0M install app-debug.apk
```

```shell
$ cd app/build/outputs/apk/release
$ adb -s RF9M33Z1Q0M install app-release-unsigned.apk
```

_Note: you may have to uninstall before installing due to different certificate signatures._

For more details, see the [ADB Documentation](https://developer.android.com/tools/adb#move).

# Support

For general help or to request a feature, see:

- Q&A Discussion: https://github.com/cssnr/noaa-weather-android/discussions/categories/q-a
- Request a Feature: https://github.com/cssnr/noaa-weather-android/discussions/categories/feature-requests

If you are experiencing an issue/bug or getting unexpected results, you can:

- Report an Issue: https://github.com/cssnr/noaa-weather-android/issues
- Chat with us on Discord: https://discord.gg/wXy6m2X8wY
- Provide General Feedback: [https://cssnr.github.io/feedback/](https://cssnr.github.io/feedback/?app=NOAA%20Weather%20Android)

# Contributing

Currently, the best way to contribute to this project is to star this project on GitHub.

You can also support other related projects:

- [Django Files Android](https://github.com/django-files/android-client?tab=readme-ov-file#readme)
- [Zipline Android](https://github.com/cssnr/zipline-android?tab=readme-ov-file#readme)
- [NOAA Weather Android](https://github.com/cssnr/noaa-weather-android?tab=readme-ov-file#readme)
- [Remote Wallpaper Android](https://github.com/cssnr/tibs3dprints-android?tab=readme-ov-file#readme)
- [Tibs3DPrints Android](https://github.com/cssnr/tibs3dprints-android?tab=readme-ov-file#readme)
