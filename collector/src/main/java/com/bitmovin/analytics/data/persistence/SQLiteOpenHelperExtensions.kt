package com.bitmovin.analytics.data.persistence

import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.core.database.sqlite.transaction

internal fun <T> SQLiteOpenHelper.catchingTransaction(block: Transaction.() -> T): T? {
    return try {
        writableDatabase.transaction {
            Transaction(this).block()
        }
    } catch (e: Exception) {
        // database exception -> transaction is cancelled, just log (should never happen on real devices)
        Log.d("catchingTransaction", "Transaction failed", e)
        e.printStackTrace()
        null
    }
}
