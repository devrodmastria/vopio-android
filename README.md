# vopio-android

## Overview

**Project Description**: Interactive captions for college lectures.

**SRS**: Software Requirements Specification document available upon request.

**Etymology/Dictionary**: 
* The name _vopio_ is a combination of **v** for voice and **oppia**, which means "to learn" in the Finnish language.
* The name _Audios_ represents a previous version of this project
* ASR stands for Automatic Speech Recognition

## Demo Video on YouTube
[![App Demo Video](https://img.youtube.com/vi/y0wFcO1Slsg/0.jpg)](https://www.youtube.com/watch?v=y0wFcO1Slsg)

## Installation Instructions
### For the App
Visit the vopio website to download the beta version: https://vopio.tech/download/
### For the Android Studio Project
To launch on the emulator, this project requires a credential file from your Firebase account.

## Known Issues (related to performance, UX, and scaling)
(Onboarding issues)
* User registration to become a host is done manually via online form
* New users are unable to login in the app due to Firebase/SHA certificate issues
(ASR issues)
* ASR service stops working after about 10 minutes of usage (lectures require about 1 hour)
* ASR accuracy is inconsistent for users with non-native accent
* ASR results don't show up on the screen until the host is done saying each sentence
* Speaker diarisation/identity is not configured

## How To Contribute
Please follow the git-flow model for naming your branch: feature/name or hotfix/name

## Tech Stack
### Platform
* Android Studio
* Kotlin (most of the app)
* Java (for integration with Speech API)
* Fragments
* AndroidX
* Media Recorder
* Background Service (Audios repo)
* Custom fonts (Audios repo)
* Googlecode MP4Parser to remix audio files (Audios repo)
* Navigation Graph with safe args

### Debugging and Performance tools
* Timber (log generator)
* Android R8 enabled for reduced footprint
* Proguard: R8 print mapping file enabled for de-obfuscation

### Cloud Services
* Google Cloud Speech API
* Google Play Billing Library (Audios repo)

### Firebase
* Cloud Messaging (Audios repo)
* Remote Config
* Crashlytics
* Analytics
* Database
* Auth

### Networking and Interface
* OkHttp for Speech API
* zxing Barcode Scanner

### Version Control
* Git CLI
* Android Studio's Git tools
* git-flow branching model: feature/name, develop, release/vX, master, hotfix/name

### Accessibility Compliance (WCAG 2.1)
* App designed with consideration for: contrast ratio; font size and spacing; button click area size

### Testing and Distribution
* Test results obtained from Google Play Console > Release > Testing > Pre-launch report > Overview
* App available from Google Play > Open Testing track
