package com.bitmovin.analytics.testutils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.bitmovin.analytics.data.persistence.Transaction

class TestDatabase(
    context: Context,
    name: String = "test.sqlite",
) : SQLiteOpenHelper(
    /* context = */ context.applicationContext,
    /* name = */ name,
    /* factory = */ null,
    /* version = */ 1,
) {
    override fun onCreate(p0: SQLiteDatabase?) {}
    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {}
}

internal fun <R> TestDatabase.transaction(block: (Transaction.() -> R)): R = Transaction(
    writableDatabase,
).block()
