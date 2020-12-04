package com.bitmovin.analytics.retryBackend

import com.bitmovin.analytics.data.EventData
import okhttp3.*


abstract class BufferedSampleHttpRequestCallback<T>(val data: T) : Callback {
}


//    abstract fun onFailure(call: Call, e: IOException)
//    {
//        if(e.message.equals("timeout", true)){
//            sampleData?.let { BufferedSamplesQueue.addSampleToBuffer(it) }
//        }

//    }

//    abstract fun onResponse(call: Call, response: Response)
//    {
//        Log.d(TAG, "on response")
//    }
