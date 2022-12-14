# OWL Face and QRcode Detection Camera lib for Flutter.

A Flutter plugin for IOS and Android allowing access to the device cameras for detecting face and QRcode automatically.

IOS:

![image](https://github.com/foxdeep/owl_detection_camera/blob/release/screenshot/ios.GIF)

Android:

![image](https://github.com/foxdeep/owl_detection_camera/blob/release/screenshot/android.GIF)

## Features

* Display live camera preview in a widget.
* Capture face photo.
* QRCode content result.
* Adjust screen brightness.
* Support landscape orientation on Android.

## Installation
      dependencies:
         owl_detection_camera: ^0.1.8
### iOS

The owl_detect_camera functionality works on iOS 13.0 or higher. If compiling for any version lower than 13.0, make sure to programmatically check the version of iOS running on the device before using any owl_camera plugin features. The permission_handler plugin, for example, can be used to check permission.

Add three rows to the `ios/Runner/Info.plist`:

* Privacy - Camera Usage Description..
* Privacy - Photo Library Additions Usage Description.
* Privacy - Photo Library Usage Description

### Android

Change the minimum Android sdk version to 23 (or higher) in your android/app/build.gradle file.

minSdkVersion 23

### Usage

Look example.

### License

AGPL-3.0 License