package io.requery.android.database.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteException;

import androidx.core.os.CancellationSignal;

/**
 * Created by huangjianbin on 2022/6/21
 */
public final class SQLiteDirectQuery extends SQLiteProgram {
    private final CancellationSignal mCancellationSignal;

    public SQLiteDirectQuery(SQLiteDatabase db, String sql, Object[] bindArgs, CancellationSignal cancellationSignalForPrepare) {
        super(db, sql, bindArgs, cancellationSignalForPrepare);
        mCancellationSignal = cancellationSignalForPrepare;
    }
    public long getLong(int column) {
        return nativeGetLong(mPreparedStatement.mStatementPtr, column);
    }

    public double getDouble(int column) {
        return nativeGetDouble(mPreparedStatement.mStatementPtr, column);
    }

    public String getString(int column) {
        return nativeGetString(mPreparedStatement.mStatementPtr, column);
    }

    public byte[] getBlob(int column) {
        return nativeGetBlob(mPreparedStatement.mStatementPtr, column);
    }

    public int getType(int column) {
        return SQLITE_TYPE_MAPPING[nativeGetType(mPreparedStatement.mStatementPtr, column)];
    }

    public int step(int count) {
        try {
            if (acquirePreparedStatement()) {
                mPreparedStatement.beginOperation("directQuery", getBindArgs());
                mPreparedStatement.attachCancellationSignal(mCancellationSignal);
            }

            return nativeStep(mPreparedStatement.mStatementPtr, count);
        } catch (RuntimeException e) {
            if (e instanceof SQLiteException) {
//                Log.e(TAG, "Got exception on stepping: " + e.getMessage() + ", SQL: " + getSql());
                checkCorruption((SQLiteException) e);
            }

            // Mark operation failed and release prepared statement.
            if (mPreparedStatement != null) {
                mPreparedStatement.detachCancellationSignal(mCancellationSignal);
                mPreparedStatement.failOperation(e);
            }
            releasePreparedStatement();
            throw e;
        }
    }

    public synchronized void reset(boolean release) {
        if (mPreparedStatement != null) {
            mPreparedStatement.reset(false);

            if (release) {
                mPreparedStatement.detachCancellationSignal(mCancellationSignal);
                mPreparedStatement.endOperation(null);
                releasePreparedStatement();
            }
        }
    }

    @Override
    protected void onAllReferencesReleased() {
        synchronized (this) {
            if (mPreparedStatement != null) {
                mPreparedStatement.detachCancellationSignal(mCancellationSignal);
                mPreparedStatement.endOperation(null);
            }
        }
        super.onAllReferencesReleased();
    }


    private static final int[] SQLITE_TYPE_MAPPING = new int[] {
            Cursor.FIELD_TYPE_STRING,      // 0, INVALID VALUE, default to STRING
            Cursor.FIELD_TYPE_INTEGER,     // SQLITE_INTEGER = 1
            Cursor.FIELD_TYPE_FLOAT,       // SQLITE_FLOAT = 2
            Cursor.FIELD_TYPE_STRING,      // SQLITE_STRING = 3
            Cursor.FIELD_TYPE_BLOB,        // SQLITE_BLOB = 4
            Cursor.FIELD_TYPE_NULL         // SQLITE_NULL = 5
    };


    private static native long nativeGetLong(long statementPtr, int column);
    private static native double nativeGetDouble(long statementPtr, int column);
    private static native String nativeGetString(long statementPtr, int column);
    private static native byte[] nativeGetBlob(long statementPtr, int column);
    private static native int nativeGetType(long statementPtr, int column);
    private static native int nativeStep(long statementPtr, int count);
}
