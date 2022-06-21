package com.jianbin.demo

import android.content.ContentValues
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.jianbin.demo.db.DbHelperMem
import com.jianbin.demo.db.DbHelperMemNew
import io.requery.android.database.sqlite.SQLiteDatabase
import io.requery.android.database.sqlite.SQLiteDirectCursor

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "SqliteTest"

        // 测试：生成的数据量
        const val MOCK_SIZE = 50000

        // 测试：是否向前遍历查询，否则向后遍历查询
        // direct 的向后查询性能很差
        const val QUERY_FORWARD = true
    }

    private val db by lazy {
        DbHelperMem(this).writableDatabase
    }

    private val dbNew by lazy {
        DbHelperMemNew(this).writableDatabase
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.btn_write).setOnClickListener {
            Thread {
                createData(databaseOld = db)
            }.start()

            Thread {
                createData(databaseNew = dbNew)
            }.start()
        }

        findViewById<View>(R.id.btn_read1).setOnClickListener {
            query(databaseNew = dbNew)
        }

        findViewById<View>(R.id.btn_read2).setOnClickListener {
            query(databaseOld = db)
        }

        findViewById<View>(R.id.btn_read3).setOnClickListener {
            query(databaseNew = dbNew, factoryNew = SQLiteDirectCursor.FACTORY)
        }
    }

    private fun createData(
        databaseOld: android.database.sqlite.SQLiteDatabase? = null,
        databaseNew: SQLiteDatabase? = null
    ) {
        val db = databaseNew ?: databaseOld ?: return
        val name = if (databaseNew != null) {
            "new"
        } else {
            "old"
        }
        val start = SystemClock.elapsedRealtime()
        for (i in 0..MOCK_SIZE) {
            val values = ContentValues().apply {
                val longText = "anjcfiwno" +
                        "ngkzhgonanjcfi" +
                        "ngkzhgonanjcfi" +
                        "ngkzhgonanjcfi" +
                        "ngkzhgonanjcfi" +
                        "ngkzhgonanjcfi" +
                        "ngkzhgonanjcfi" +
                        "ngkzhgonanjcfi" +
                        "wnongkzhgonanjcfiwnongkzhgo" +
                        "wnongkzhgonanjcfiwnongkzhgo" +
                        "wnongkzhgonanjcfiwnongkzhgo" +
                        "wnongkzhgonanjcfiwnongkzhgo" +
                        "nanjcfiwnongkzhgonanjcfiwnongkzh" +
                        "gonanjcfiwnongkz" +
                        "hgonanjcfiwnongkzhgonanj" +
                        "cfiwnong" +
                        "kzhgonanjcfiwnongkzhgonanjc" +
                        "fiwnongkzhgon"
                put(DbHelperMem.COLUMNS_INT, 1)
                put(DbHelperMem.COLUMNS_TEXT, longText)
                put(DbHelperMem.COLUMNS_LONG, 100000)
                put(DbHelperMem.COLUMNS_TEXT2, longText)
                put(DbHelperMem.COLUMNS_TEXT3, longText)
                put(DbHelperMem.COLUMNS_TEXT4, longText)
            }
            if (db is android.database.sqlite.SQLiteDatabase) {
                db.insert(DbHelperMem.TABLE_TEST, null, values)
            } else if (db is SQLiteDatabase) {
                db.insert(DbHelperMem.TABLE_TEST, null, values)
            }
        }
        Log.d(TAG, "insert $name cost=" + (SystemClock.elapsedRealtime() - start))
    }


    private fun query(
        databaseOld: android.database.sqlite.SQLiteDatabase? = null,
        databaseNew: SQLiteDatabase? = null,
        factoryOld:  android.database.sqlite.SQLiteDatabase.CursorFactory? = null,
        factoryNew: SQLiteDatabase.CursorFactory? = null,
        forward: Boolean = QUERY_FORWARD /* 是否向前查询 */
    ): Int {
        val name = if (databaseNew != null) {
            "new"
        } else {
            "old"
        }
        Log.d(TAG, "start query $name")
        val start = SystemClock.elapsedRealtime()
        val cursor = databaseOld?.queryWithFactory(
            factoryOld,
            false,
            DbHelperMem.TABLE_TEST,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ) ?: databaseNew?.queryWithFactory(
            factoryNew,
            false,
            DbHelperMem.TABLE_TEST,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ) ?: return -1

        return cursor.use {
            if (!it.moveToFirst()) {
                return@use 0
            }

            if (forward) {
                do {
                    it.getInt(it.getColumnIndexOrThrow(DbHelperMem.COLUMNS_INT))
                    it.getLong(it.getColumnIndexOrThrow(DbHelperMem.COLUMNS_LONG))
                    it.getString(it.getColumnIndexOrThrow(DbHelperMem.COLUMNS_TEXT))
                    it.getString(it.getColumnIndexOrThrow(DbHelperMem.COLUMNS_TEXT2))
                    it.getString(it.getColumnIndexOrThrow(DbHelperMem.COLUMNS_TEXT3))
                    it.getString(it.getColumnIndexOrThrow(DbHelperMem.COLUMNS_TEXT4))
                } while (it.moveToNext())
            } else {
                var pos = it.count
                while (pos-- > 0 && it.moveToPosition(pos)) {
                    it.getInt(it.getColumnIndexOrThrow(DbHelperMem.COLUMNS_INT))
                    it.getLong(it.getColumnIndexOrThrow(DbHelperMem.COLUMNS_LONG))
                    it.getString(it.getColumnIndexOrThrow(DbHelperMem.COLUMNS_TEXT))
                    it.getString(it.getColumnIndexOrThrow(DbHelperMem.COLUMNS_TEXT2))
                    it.getString(it.getColumnIndexOrThrow(DbHelperMem.COLUMNS_TEXT3))
                    it.getString(it.getColumnIndexOrThrow(DbHelperMem.COLUMNS_TEXT4))
                }
            }

            Log.d(TAG, "after query $name(${it.count}) " + " diff=" + (SystemClock.elapsedRealtime() - start))
            return@use it.count
        }
    }
}