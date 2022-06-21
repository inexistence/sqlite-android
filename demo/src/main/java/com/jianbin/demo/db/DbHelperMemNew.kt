package com.jianbin.demo.db

import android.content.Context
import android.provider.BaseColumns
import io.requery.android.database.sqlite.SQLiteDatabase
import io.requery.android.database.sqlite.SQLiteOpenHelper

/**
 * Created by huangjianbin on 2022/6/17
 */
class DbHelperMemNew(context: Context) : SQLiteOpenHelper(context.applicationContext, null, null, 1) {
    companion object {
        const val TABLE_TEST = "test"

        const val COLUMNS_INT = "int_column"
        const val COLUMNS_TEXT = "text_column"
        const val COLUMNS_LONG = "long_column"
        const val COLUMNS_TEXT2 = "text_column2"
        const val COLUMNS_TEXT3 = "text_column3"
        const val COLUMNS_TEXT4 = "text_column4"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS " + TABLE_TEST
                    + " (" + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMNS_INT + " INTEGER,"
                    + COLUMNS_TEXT + " TEXT,"
                    + COLUMNS_TEXT2 + " TEXT,"
                    + COLUMNS_TEXT3 + " TEXT,"
                    + COLUMNS_TEXT4 + " TEXT,"
                    + COLUMNS_LONG + " LONG"
                    + ");"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }
}