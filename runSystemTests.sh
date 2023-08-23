# accept all sdk licenses
yes | /Users/$USER/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager --licenses

# run systemtest for collector core
## clean test to make sure we run test everytime
./gradlew :collector:cleanPixel6api30DebugAndroidTest
## run system tests
./gradlew :collector:pixel6api30DebugAndroidTest || exit

# run systemtest for ivs
## clean test to make sure we run test everytime
./gradlew :collector-amazon-ivs:cleanPixel6api30DebugAndroidTest
## run PhoneBasicScenariosTest
./gradlew :collector-amazon-ivs:pixel6api30DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bitmovin.analytics.amazon.ivs.PhoneBasicScenariosTest || exit

# run systemtest for exoplayer
## clean test to make sure we run test everytime
./gradlew :collector-exoplayer:cleanPixel6api30DebugAndroidTest
## run test
./gradlew :collector-exoplayer:pixel6api30DebugAndroidTest || exit

# run systemtest for bitmovin-player
## clean test to make sure we run test everytime
./gradlew :collector-bitmovin-player:cleanPixel6api30DebugAndroidTest

## run test
./gradlew :collector-bitmovin-player:pixel6api30DebugAndroidTest



## Commands that can be used for local testing of specific test classes and tests

## commands to run specific test classes
#./gradlew :collector-bitmovin-player:pixel6api30DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bitmovin.analytics.bitmovin.player.PhoneBasicScenariosTest || exit
#./gradlew :collector-bitmovin-player:pixel6api30DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bitmovin.analytics.bitmovin.player.apiv2.PhoneBasicScenariosTest || exit
#./gradlew :collector-bitmovin-player:pixel6api30DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bitmovin.analytics.bitmovin.player.apiv2.AttachingScenariosTest || exit
#./gradlew :collector-bitmovin-player:pixel6api30DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bitmovin.analytics.bitmovin.player.BundledAnalyticsTest || exit
#./gradlew :collector-bitmovin-player:pixel6api30DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bitmovin.analytics.bitmovin.player.BundledAnalyticsTest || exit

## command to run specific test (with regex) in a loop (can be used to verify flaky test is stable)
#for i in {1..50}; do
#  ./gradlew :collector-bitmovin-player:cleanPixel6api30DebugAndroidTest
#  ./gradlew :collector-bitmovin-player:pixel6api30DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.tests_regex=com.bitmovin.analytics.bitmovin.player.PhoneBasicScenariosTest.test_wrongAnalyticsLicense_ShouldNotInterfereWithPlayer || exit
#done
