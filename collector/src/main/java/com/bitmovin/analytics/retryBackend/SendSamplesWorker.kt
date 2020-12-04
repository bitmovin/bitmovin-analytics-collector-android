//package com.bitmovin.analytics.retryBackend
//
//import android.content.Context
//import android.os.Handler
//import android.util.Log
//import androidx.work.Worker
//import androidx.work.WorkerParameters
//import com.bitmovin.analytics.data.Backend
//
//class SendSamplesWorker(val context: Context, workerParams: WorkerParameters, val backend: Backend)
//    : Worker(context, workerParams) {
//    val TAG = "SendSampleWorker"
//    val handler: Handler = Handler()
//    //todo move somewhere constants
//    val delay = 500
//
//    override fun doWork(): Result {
//        try {
////
////            handler.postDelayed(object : Runnable {
////                override fun run() {
////                    val sample = BufferedSamplesQueue.getSampleFromBuffer()
////                    Log.d(TAG, "sample")
////                    if (sample != null) {
////                        backend.send(sample.eventData);
////                        handler.post(this)
////                    }
////
////                    else {
////                        Log.d(TAG, "post delayed")
////                        handler.postDelayed(this, delay.toLong())
////                    }
////                    //todo do we need to retry this worker under some conditions?
////                }
////            }, delay.toLong())
////
////            return Result.success()
////
////
////        }
////        catch (e: Exception){
////            return  Result.failure()
////        }
//    }
//
//
//}