package com.bitmovin.analytics.exoplayer

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

// System test for basic playing and error scenario using exoplayer player
// This tests assume a phone with api level 30 for validations
// Tests can be run automatically with gradle managed device through running ./runSystemTests.sh` in the root folder
// Tests use logcat logs to get the sent analytics samples
@RunWith(AndroidJUnit4::class)
class PhoneBasicScenariosTest {

    @Test
    fun test_basicPlayPauseScenario_Should_sendCorrectSamples() {
    }
}
