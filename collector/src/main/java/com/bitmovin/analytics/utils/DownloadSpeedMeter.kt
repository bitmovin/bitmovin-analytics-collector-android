package com.bitmovin.analytics.utils;

import com.bitmovin.analytics.data.DownloadSpeedInfo
import com.bitmovin.analytics.data.SpeedMeasurement

class DownloadSpeedMeter {

    private var measures = ArrayList<Measure>();
    private val thresholdBytes = 37500000; // 300 Megabit per second in bytes per second

    fun reset(): Unit {
       measures.clear();
    }

    fun addMeasurement(measurement: SpeedMeasurement): Unit{
        if(measurement.httpStatus >= 400) {
            return;
        }

        val measure = Measure(measurement);

        if (measure.getSpeed() >= thresholdBytes) {
            return;
        }

        measures.add(measure);
    }

    fun getInfo(): DownloadSpeedInfo {
        var info = DownloadSpeedInfo();
        info.segmentsDownloadCount = measures.size;
        info.segmentsDownloadSize = measures.map { it.getSize() }.sum();
        info.segmentsDownloadTime = totalTime();
        info.avgDownloadSpeed = avgSpeed();
        info.minDownloadSpeed = minSpeed();
        info.maxDownloadSpeed = maxSpeed();
        info.avgTimeToFirstByte = avgTimeToFirstByte();
        return info;
    }


    private fun avgSpeed(): Float {
        if (measures.size === 0) {
            return 0.0f;
        }
        val totalSpeed = measures.map {it.getSpeed()}.sum();

        return totalSpeed.div(measures.size).times(8); // bytes per millisecond to kbps
    }

    private fun minSpeed(): Float? {
        if (measures.size === 0) {
            return 0.0f;
        }
        // the slowest one to download
        return measures.map {it.getSpeed()}.max()?.times(8) // bytes per millisecond to kbps
    }

    private fun maxSpeed(): Float? {
        if (measures.size === 0) {
            return 0.0f;
        }
        // the fastest one to download
        return measures.map {it.getSpeed()}.min()?.times(8); // bytes per millisecond to kbps
    }

    private fun totalTime(): Long {
        if (this.measures.size === 0) {
            return 0;
        }
        return this.measures.map{it.getDuration()}.sum();
    }

    private fun avgTimeToFirstByte(): Float {
        if (measures.size === 0) {
            return 0.0f;
        }

        return measures.map {it.getTimeToFirstByte()}.sum().div(measures.size);
    }
}