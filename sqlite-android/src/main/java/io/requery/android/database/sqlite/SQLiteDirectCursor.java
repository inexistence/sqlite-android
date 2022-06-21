package io.requery.android.database.sqlite;

import android.database.Cursor;
import android.util.Log;

import androidx.core.os.CancellationSignal;

import io.requery.android.database.AbstractCursor;

/**
 * 跳过WindowCursor的代理
 * Created by huangjianbin on 2022/6/21
 */
public class SQLiteDirectCursor extends AbstractCursor {
    private static final String TAG = "SQLiteDirectCursor";

    private final SQLiteCursorDriver mDriver;
    private final SQLiteDirectQuery mQuery;
    private final String[] mColumns;

    private int mCount;
    private boolean mCountFinished;

    public SQLiteDirectCursor(SQLiteCursorDriver driver, String editTable, SQLiteDirectQuery query) {
        mQuery = query;
        mDriver = driver;

        mColumns = query.getColumnNames();

        mCount = -1;
        mCountFinished = false;
    }

    @Override
    public int getCount() {
        if (!mCountFinished) {
//            Log.w(TAG, "Count query on SQLiteDirectCursor is slow. Iterate through the end to get count " +
//                    "or use other implementations.");

            int actualPos = mPos + mQuery.step(Integer.MAX_VALUE);
            mCount = actualPos + 1;
            mCountFinished = true;

            mQuery.reset(false);

            // Update mPos in case of data set changed during last query.
            mPos = mQuery.step(mPos + 1) - 1;
        }

        return mCount;
    }
    @Override
    public void close() {
        super.close();

        mQuery.close();
        mDriver.cursorClosed();
    }

    @Override
    public void deactivate() {
        super.deactivate();
        mDriver.cursorDeactivated();
    }

    @Override
    public String[] getColumnNames() {
        return mColumns;
    }

    @Override
    public String getString(int column) {
        return mQuery.getString(column);
    }

    @Override
    public byte[] getBlob(int column) {
        return mQuery.getBlob(column);
    }

    @Override
    public short getShort(int column) {
        return (short) mQuery.getLong(column);
    }

    @Override
    public int getInt(int column) {
        return (int) mQuery.getLong(column);
    }

    @Override
    public long getLong(int column) {
        return mQuery.getLong(column);
    }

    @Override
    public float getFloat(int column) {
        return (float) mQuery.getDouble(column);
    }

    @Override
    public double getDouble(int column) {
        return mQuery.getDouble(column);
    }

    @Override
    public int getType(int column) {
        return mQuery.getType(column);
    }

    @Override
    public boolean isNull(int column) {
        return getType(column) == FIELD_TYPE_NULL;
    }

    @Override
    public boolean onMove(int oldPosition, int position) {
        int actualPos;

        if (position < 0) {
            mQuery.reset(false);
            mPos = -1;
            return false;
        }

        if (mCountFinished && position >= mCount) {
            mPos = mCount;
            return false;
        } else if (position < mPos) {
            Log.w(TAG, "Moving backward on SQLiteDirectCursor is slow. Get rid of backward movement "
                    + "or use other implementations.");

            mQuery.reset(false);
            actualPos = mQuery.step(position + 1) - 1;
        } else if (position == mPos) {
            return true;
        } else { // position > mPos
            actualPos = mPos + mQuery.step(position - mPos);
        }

        if (actualPos < position) {
            // Returned position is smaller than requested. This is caused by reaching
            // the end of the result, so we mark counting finished.
            mCount = actualPos + 1;
            mCountFinished = true;
            mPos = mCount;
        } else {
            mPos = actualPos;
            if (actualPos >= mCount) {
                // We are not finished counting, update count accordingly.
                mCount = actualPos + 1;
                mCountFinished = false;
            }
        }

        return mPos < mCount;
    }

    /**
     * Static factory object of {@link SQLiteDirectCursor}.
     *
     * Pass to {@link SQLiteDatabase#queryWithFactory(SQLiteDatabase.CursorFactory, boolean, String, String[], String, Object[], String, String, String, String)}
     * to get a cursor object of {@link SQLiteDirectCursor} .
     */
    public static final SQLiteDatabase.CursorFactory FACTORY = new SQLiteDatabase.CursorFactory() {

        @Override
        public SQLiteProgram newQuery(SQLiteDatabase db, String sql, Object[] selectionArgs, CancellationSignal cancellationSignal) {
            return new SQLiteDirectQuery(db, sql, selectionArgs, cancellationSignal);
        }

        @Override
        public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery,
                                String editTable, SQLiteProgram query) {
            return new SQLiteDirectCursor(masterQuery, editTable, (SQLiteDirectQuery) query);
        }
    };
}
