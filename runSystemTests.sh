# accept all sdk licenses
yes | /Users/$USER/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager --licenses

## run systemtest for ivs
## clean test to make sure we run test everytime
./gradlew :collector-amazon-ivs:cleanPixel6api30DebugAndroidTest
## run PhoneBasicScenariosTest
./gradlew :collector-amazon-ivs:pixel6api30DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bitmovin.analytics.amazon.ivs.PhoneBasicScenariosTest || exit

## run systemtest for exoplayer
### clean test to make sure we run test everytime
#./gradlew :collector-exoplayer:cleanPixel6api30DebugAndroidTest
### run test
#./gradlew :collector-exoplayer:pixel6api30DebugAndroidTest || exit

# run systemtest for bitmovin-player
## clean test to make sure we run test everytime
./gradlew :collector-bitmovin-player:cleanPixel6api30DebugAndroidTest
## run test
./gradlew :collector-bitmovin-player:pixel6api30DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bitmovin.analytics.bitmovin.player.PhoneBasicScenariosTest || exit
