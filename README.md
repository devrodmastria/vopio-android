# vopio-android

**Project Goals**:  to make on-campus lectures more fulfilling to students and instructors.

**SRS**: Software Requirements Specification document available upon request.

**Etymology**: the word vopio is a combination of **v** for voice and **oppia**, which means "to learn" in the Finnish language.

## Demo Video
https://youtube.com/shorts/y0wFcO1Slsg?feature=share

## Tech Stack
### Platform
* Android Studio
* Kotlin (most of the app)
* Java (for integration with Speech API)
* Fragments
* AndroidX

### Debugging and Performance tools
* Timber (log generator)
* Android R8 enabled for reduced footprint
* Proguard: R8 print mapping file enabled for deobfuscation

### A.I. Services
* Google Cloud Speech API

### Firebase
* Cloud Messaging
* Remote Config
* Crashlytics
* Analytics
* Database
* Auth

### Networking and Interface
* OkHttp for Speech API
* zxing Barcode Scanner

### Version control
* Git CLI
* Android Studio's Git tools

### Accessibility (WCAG 2.1)
* Compliance for contrast ratios, readability, and clicking areas sizes

### Testing and Distribution
* Google Play Console > Release > Testing > Pre-launch report > Overview items were considered
* App is available in Google Play > Open Testing track
