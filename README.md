# WindowMirror

Installation - Android
-
1. Install Android Studio (or IntelliJ with the Android plugin)
2. Import the `Butterfly.iml` project file (recommended) or Import the outer-most `build.gradle` file
3. Add data required to the `local.properties` file (see below)
4. Sync Gradle and run!

Configuring `local.properties`
-
The top directory of this app should contain a `local.properties` file after importing into Android Studio. If it does not, you may create one.

The following key/values MUST be added to this file for the project to run:

`speechKey=MISCROSOFT_SPEECH_KEY_HERE`

Note that the pattern for each line of this file is:
`key=value`