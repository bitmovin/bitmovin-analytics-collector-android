## accept all sdk licenses
yes | /Users/$USER/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager --licenses

## run systemtest for ivs
./gradlew cleanPixel6api30DebugAndroidTest :collector-amazon-ivs-example:connectedAndroidTest pixel6api30DebugAndroidTest