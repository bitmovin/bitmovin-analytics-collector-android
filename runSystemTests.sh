# Run Systemtests automatically on a local machine
# call './runSystemTests.sh' in order to execute all tests
#
# local setup requires gradle and android sdk
# (if android development with android studio works, this should be the case)


# accept all sdk licenses
yes | sdkmanager --licenses

# run systemtest for collector core
## clean test to make sure we run test everytime
./gradlew :collector:cleanPixel6api35DebugAndroidTest
## run system tests
./gradlew :collector:pixel6api35DebugAndroidTest || exit
#
# run systemtest for ivs
## clean test to make sure we run test everytime
./gradlew :collector-amazon-ivs:cleanPixel6api35DebugAndroidTest
## run all ivs tests except TV scenario, since TVs are not supported by gradle automated tests
./gradlew :collector-amazon-ivs:pixel6api35DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.notClass=com.bitmovin.analytics.amazon.ivs.TVBasicScenariosTest || exit

# run systemtest for exoplayer
## clean test to make sure we run test everytime
./gradlew :collector-exoplayer:cleanPixel6api35DebugAndroidTest
## run test
./gradlew :collector-exoplayer:pixel6api35DebugAndroidTest || exit

# run systemtest for media3-exoplayer
## clean test to make sure we run test everytime
./gradlew :collector-media3-exoplayer:cleanPixel6api35DebugAndroidTest
## run test
./gradlew :collector-media3-exoplayer:pixel6api35DebugAndroidTest || exit

# run systemtest for bitmovin-player
## clean test to make sure we run test everytime
./gradlew :collector-bitmovin-player:cleanPixel6api35DebugAndroidTest
## run test
./gradlew :collector-bitmovin-player:pixel6api35DebugAndroidTest || exit


## Commands that can be used for local testing of specific test classes and tests

## commands to run specific test classes
#./gradlew :collector-bitmovin-player:pixel6api35DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bitmovin.analytics.bitmovin.player.PhoneBasicScenariosTest || exit
#./gradlew :collector-bitmovin-player:pixel6api35DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bitmovin.analytics.bitmovin.player.apiv2.PhoneBasicScenariosTest || exit
#./gradlew :collector-bitmovin-player:pixel6api35DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bitmovin.analytics.bitmovin.player.apiv2.AttachingScenariosTest || exit
#./gradlew :collector-bitmovin-player:pixel6api35DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bitmovin.analytics.bitmovin.player.BundledAnalyticsTest || exit
#./gradlew :collector-bitmovin-player:pixel6api35DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bitmovin.analytics.bitmovin.player.BundledAnalyticsTest || exit

# command to run specific test (with regex) in a loop (can be used to verify flaky test is stable)
#for i in {1..50}; do
#   echo "RUN $i/50"
#  ./gradlew :collector-bitmovin-player:cleanPixel6api35DebugAndroidTest
#  ./gradlew :collector-bitmovin-player:pixel6api35DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.tests_regex=com.bitmovin.analytics.bitmovin.player.SsaiScenariosTest
#done

#  ./gradlew :collector-exoplayer:pixel6api35DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.tests_regex=com.bitmovin.analytics.exoplayer.ErrorScenariosTest.test_vodWithDrm_wrongConfig || exit


#for i in {1..50}; do
#   echo "RUN $i/50"
#  ./gradlew :collector-media3-exoplayer:cleanPixel6api35DebugAndroidTest
#  ./gradlew :collector-media3-exoplayer:pixel6api35DebugAndroidTest  -Pandroid.testInstrumentationRunnerArguments.tests_regex=com.bitmovin.analytics.media3.exoplayer.ErrorScenariosTest.test_playerReleaseDuringStartup_Should_sendEbvsSample || exit
#done


