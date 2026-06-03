# Run Systemtests automatically on a local machine
# call './runSystemTests.sh' in order to execute all tests
#
# local setup requires gradle and android sdk
# (if android development with android studio works, this should be the case)


# accept all sdk licenses
yes | sdkmanager --licenses

# run systemtest for collector core
## clean test to make sure we run test everytime
./gradlew :collector:cleanpixel6api36DebugAndroidTest
## run system tests
./gradlew :collector:pixel6api36DebugAndroidTest || exit

# run systemtest for media3-exoplayer
## clean test to make sure we run test everytime
./gradlew :collector-media3-exoplayer:cleanpixel6api36DebugAndroidTest
## run test
./gradlew :collector-media3-exoplayer:pixel6api36DebugAndroidTest || exit

# run systemtest for bitmovin-player
## clean test to make sure we run test everytime
./gradlew :collector-bitmovin-player:cleanpixel6api36DebugAndroidTest
## run test
./gradlew :collector-bitmovin-player:pixel6api36DebugAndroidTest || exit

# run systemtest for THEOplayer
## clean test to make sure we run test everytime
./gradlew :collector-theoplayer:cleanpixel6api36DebugAndroidTest
## run test
./gradlew :collector-theoplayer:pixel6api36DebugAndroidTest || exit


## Commands that can be used for local testing of specific test classes and tests

## commands to run specific test classes
#./gradlew :collector-bitmovin-player:pixel6api36DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bitmovin.analytics.bitmovin.player.PhoneBasicScenariosTest || exit
#./gradlew :collector-bitmovin-player:pixel6api36DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bitmovin.analytics.bitmovin.player.apiv2.PhoneBasicScenariosTest || exit
#./gradlew :collector-bitmovin-player:pixel6api36DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bitmovin.analytics.bitmovin.player.apiv2.AttachingScenariosTest || exit
#./gradlew :collector-bitmovin-player:pixel6api36DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bitmovin.analytics.bitmovin.player.BundledAnalyticsTest || exit
#./gradlew :collector-bitmovin-player:pixel6api36DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bitmovin.analytics.bitmovin.player.BundledAnalyticsTest || exit

# command to run specific test (with regex) in a loop (can be used to verify flaky test is stable)
#for i in {1..50}; do
#   echo "RUN $i/50"
#  ./gradlew :collector-bitmovin-player:cleanpixel6api36DebugAndroidTest
#  ./gradlew :collector-bitmovin-player:pixel6api36DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.tests_regex=com.bitmovin.analytics.bitmovin.player.SsaiScenariosTest
#done

#  ./gradlew :collector-theoplayer:pixel6api36DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.tests_regex=com.bitmovin.analytics.theoplayer.ErrorScenariosTest || exit


#for i in {1..50}; do
#   echo "RUN $i/50"
#  ./gradlew :collector-media3-exoplayer:cleanpixel6api36DebugAndroidTest
#  ./gradlew :collector-media3-exoplayer:pixel6api36DebugAndroidTest  -Pandroid.testInstrumentationRunnerArguments.tests_regex=com.bitmovin.analytics.media3.exoplayer.ErrorScenariosTest.test_playerReleaseDuringStartup_Should_sendEbvsSample || exit
#done


