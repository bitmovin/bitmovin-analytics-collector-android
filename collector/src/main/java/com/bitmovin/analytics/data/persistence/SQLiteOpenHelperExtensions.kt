package com.bitmovin.analytics.data.persistence

import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import com.bitmovin.analytics.utils.BitmovinLog

internal fun <T> SQLiteOpenHelper.catchingTransaction(block: Transaction.() -> T): T? {
    return try {
        writableDatabase.transaction {
            Transaction(this).block()
        }
    } catch (e: Exception) {
        // database exception -> transaction is cancelled, just log (should never happen on real devices)
        BitmovinLog.e("catchingTransaction", "Transaction failed", e)
        e.printStackTrace()
        null
    }
}
