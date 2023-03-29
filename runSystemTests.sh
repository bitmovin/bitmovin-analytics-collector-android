# accept all sdk licenses
yes | /Users/$USER/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager --licenses

# run systemtest for ivs
## clean test to make sure we run test on everytime
./gradlew :collector-amazon-ivs:cleanPixel6api30DebugAndroidTest
## run PhoneBasicScenariosTest
./gradlew :collector-amazon-ivs:pixel6api30DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bitmovin.analytics.amazon.ivs.PhoneBasicScenariosTest || exit
