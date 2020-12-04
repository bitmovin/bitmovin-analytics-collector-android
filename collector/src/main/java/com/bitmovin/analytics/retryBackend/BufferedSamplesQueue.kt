package com.bitmovin.analytics.retryBackend

import android.util.Log
import com.bitmovin.analytics.data.EventData
import java.util.*
import kotlin.math.pow

object BufferedSamplesQueue {
//    val TAG = "BufferedSamplesQueue"
//    private var bufferedSamplesList = mutableListOf<RetrySample>()
//
//
//    fun addSampleToBuffer (sample: EventData) {
//
//        sample.retry++
//
//        if(sample.retry > 9){
//            return;
//        }
//
//        val time = System.currentTimeMillis() + minOf( 2.toDouble().pow(sample.retry).toInt() , 64)
//        // TODO move 100 somewhere to consts
//        if(bufferedSamplesList.size < 100) {
//            Log.d(TAG, "add sample")
//            bufferedSamplesList.add(RetrySample(sample, time));
//        }
//        else{
//            Log.d(TAG, "add & remove sample")
//            bufferedSamplesList.removeAt(bufferedSamplesList.lastIndex)
//            bufferedSamplesList.add(RetrySample(sample, time))
//        }
//
//        bufferedSamplesList.sortBy { it.bufferedTime }
//    }
//
//    fun getSampleFromBuffer(): RetrySample? {
//        Log.d(TAG, "get sample")
//
//        if(bufferedSamplesList.size > 0 && bufferedSamplesList[0].bufferedTime <= System.currentTimeMillis()) {
//            val sample = bufferedSamplesList[0]
//            bufferedSamplesList.removeAt(0)
//            return sample
//        }
//        return null;
//    }

}