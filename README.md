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

```
apiUrl=https://BACKEND_URL
speechKey=MISCROSOFT_SPEECH_KEY_HERE
auth0ClientId=AUTH0_CLIENT_ID
auth0Domain=AUTH0_DOMAIN
```

Note that the pattern for each line of this file is:
`key=value`

Configuring Start/Stop Words
-
Start/stop phrases are configured in `SphynxService.java` as `START_PHRASE` and `STOP_PHRASE`.
Note that you cannot drop any words in here. These words must first be defined in the local dictionary here: `/app/assets/sync/cmudict-en-us.dict` which can be opened in any text editor.  
See PocketSphynx docs for information on building dictionaries.
 
Currently defined words:

```
okay OW K EY
window W IH N D OW
mirror M IH R ER
mir M IH R
thank TH AE NG K
thanks TH AE NG K S
you Y UW
hay HH EY
hello HH EH L OW
butterfly B AH T ER F L AY
```

Always Listening
-
By default, the app has voice recognition enabled at all times. To turn this off you may disable "Always Listening" in the menu. When this is off, the voice recognition will only work when the app is in the foreground.