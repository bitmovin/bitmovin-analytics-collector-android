package com.bitmovin.analytics.utils

import com.bitmovin.analytics.data.SpeedMeasurement
import org.assertj.core.api.Assertions
import org.junit.Test

class DownloadSpeedMeterTest {
    var measurement1 = SpeedMeasurement(20, 1000)
    var measurement2 = SpeedMeasurement(30, 1500)
    var measurement3= SpeedMeasurement(50, 2000)
    var measurement4 = SpeedMeasurement(20, 2000)
    var measurement5 = SpeedMeasurement(20, 1500);
    var meter = DownloadSpeedMeter();


    @Test
    fun testMeterAddAndRest(){
        meter.reset();
        meter.addMeasurement(measurement1);
        meter.addMeasurement(measurement2);
        meter.addMeasurement(measurement3);
        meter.addMeasurement(measurement4);
        meter.addMeasurement(measurement5);

        var info = meter.getInfo();
        // total of 5 measurements
        Assertions.assertThat(info.segmentsDownloadCount).isEqualTo(5);

        meter.reset();
        info = meter.getInfo();
        // total of 5 measurements
        Assertions.assertThat(info.segmentsDownloadCount).isEqualTo(0);
    }


    @Test
    fun testSpeedMeasurements(){
        meter.reset();
        meter.addMeasurement(measurement1);
        meter.addMeasurement(measurement2);
        meter.addMeasurement(measurement3);
        meter.addMeasurement(measurement4);
        meter.addMeasurement(measurement5);

        var info = meter.getInfo();
        // total of 5 measurements
        Assertions.assertThat(info.segmentsDownloadCount).isEqualTo(5);
        // sum of all durations
        Assertions.assertThat(info.segmentsDownloadTime).isEqualTo(140);
        // sum of all sizes
        Assertions.assertThat(info.segmentsDownloadSize).isEqualTo(8000);
        // slowest download -> measurement4
        Assertions.assertThat(info.minDownloadSpeed).isEqualTo(800.0f);
        // fastest download -> measurement3
        Assertions.assertThat(info.maxDownloadSpeed).isEqualTo(320.0f);
        Assertions.assertThat(info.avgDownloadSpeed).isEqualTo(504.0f);
    }

}
