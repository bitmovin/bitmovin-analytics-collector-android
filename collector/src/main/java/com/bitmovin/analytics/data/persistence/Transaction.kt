package com.bitmovin.analytics.data.persistence

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

@JvmInline
internal value class Transaction(val db: SQLiteDatabase)

internal fun Transaction.insert(
    tableName: String,
    nullColumnHack: String? = null,
    values: ContentValues,
) = db.insert(
    tableName,
    nullColumnHack,
    values,
)

internal fun Transaction.query(
    tableName: String,
    columns: List<String>,
    selection: String? = null,
    selectionArgs: List<String>? = null,
    groupBy: String? = null,
    having: String? = null,
    orderBy: String? = null,
    limit: String? = null,
) = db.query(
    tableName,
    columns.toTypedArray(),
    selection,
    selectionArgs?.toTypedArray(),
    groupBy,
    having,
    orderBy,
    limit,
)

internal fun Transaction.delete(
    tableName: String,
    whereClause: String? = null,
    whereArgs: List<String>? = null,
) = db.delete(
    tableName,
    whereClause,
    whereArgs?.toTypedArray(),
)
