#undef LOG_TAG
#define LOG_TAG "SQLiteDirectQuery"

#include <JNIHelp.h>
#include <jni.h>
#include <sqlite3.h>
#include "android_database_SQLiteCommon.h"

namespace android {
    static jlong
    nativeGetLong(JNIEnv *env, jclass cls, jlong statementPtr, jint column)
    {
            sqlite3_stmt *stmt = (sqlite3_stmt *) (intptr_t) statementPtr;
            return sqlite3_column_int64(stmt, column);
    }

    static jdouble
    nativeGetDouble(JNIEnv *env, jclass cls, jlong statementPtr, jint column)
    {
            sqlite3_stmt *stmt = (sqlite3_stmt *) (intptr_t) statementPtr;
            return sqlite3_column_double(stmt, column);
    }

    static jstring
    nativeGetString(JNIEnv *env, jclass cls, jlong statementPtr, jint column)
    {
            sqlite3_stmt *stmt = (sqlite3_stmt *) (intptr_t) statementPtr;

            // Use sqlite internal UTF-8 to UTF-16 conversion instead of NewStringUTF.
            int len = sqlite3_column_bytes16(stmt, column) / 2;
            const jchar *str = (const jchar *) sqlite3_column_text16(stmt, column);
            return env->NewString(str, len);
    }

    static jbyteArray
    nativeGetBlob(JNIEnv *env, jclass cls, jlong statementPtr, jint column)
    {
            sqlite3_stmt *stmt = (sqlite3_stmt *) (intptr_t) statementPtr;

            const jbyte *blob = (const jbyte *) sqlite3_column_blob(stmt, column);
            int len = sqlite3_column_bytes(stmt, column);

            jbyteArray result = env->NewByteArray(len);
            env->SetByteArrayRegion(result, 0, len, blob);
            return result;
    }

    static jint
    nativeGetType(JNIEnv *env, jclass cls, jlong statementPtr, jint column)
    {
            sqlite3_stmt *stmt = (sqlite3_stmt *) (intptr_t) statementPtr;
            return sqlite3_column_type(stmt, column);
    }

    static jint nativeStep(JNIEnv *env, jclass cls, jlong statementPtr, jint count)
    {
            sqlite3_stmt *stmt = (sqlite3_stmt *) (intptr_t) statementPtr;

            jint i;
            for (i = 0; i < count; i++) {
                    int ret = sqlite3_step(stmt);
                    if (ret == SQLITE_DONE) {
                            // Reach end of result, return i as total rows steped.
                            break;
                    } else if (ret != SQLITE_ROW) {
                            throw_sqlite3_exception(env, sqlite3_db_handle(stmt));
                            return -1;
                    }
            }

            return i;
    }

    static JNINativeMethod sMethods[] = {
            {"nativeGetLong", "(JI)J", (void *) nativeGetLong},
            {"nativeGetDouble", "(JI)D", (void *) nativeGetDouble},
            {"nativeGetString", "(JI)Ljava/lang/String;", (void *) nativeGetString},
            {"nativeGetBlob", "(JI)[B", (void *) nativeGetBlob},
            {"nativeGetType", "(JI)I", (void *) nativeGetType},
            {"nativeStep", "(JI)I", (void *) nativeStep},
    };

    int register_android_database_SQLiteDirectQuery(JNIEnv *env) {
        return jniRegisterNativeMethods(env,
                                        "io/requery/android/database/sqlite/SQLiteDirectQuery", sMethods,
                                        NELEM(sMethods));
    }
}