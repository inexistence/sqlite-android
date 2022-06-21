# Android SQLite support library

### 我的说明
1. android 内部实现的 sqlite 不同版本间存在性能差异
2. sqlite 官方实现的 sqlite android 比 android 内部的性能更优，

### 我的修改
1. 编译选项（sqlite_flags）由`-O3`修改为`-Os`，减小包体。（暂未发现性能上的差异）
2. 新增 SQLiteDirectCursor，取消 SQLiteCursor 中 WindowCursor 的代理，当数据大小超过2M，向前查询速度更快。相对的，其向后查询性能很差。
     * 原因：WindowCursor 存在缓存和内存限制（2M），每次到达内存上限会重新触发一次fillWindow内容填充，且该行为需从头开始遍历。所以当读取的数据超过2M，性能将变差，内容越大性能越差
     * 修改：感觉 WindowCursor 的做法没啥意义。去除 WindowCursor 代理，直接操作 sqlite native 进行位移和读取数据。不同点在于没有了缓存操作和内存大小限制。
     * 缺点：往前查询（moveToNext 或新 position 比旧的大）的情况下会比 CursorWindow 快。但是由于 sqlite 只提供了向前步进的方法，如果是向后查询每次都需要从头遍历到目标位置，这时由于没有缓存性能较 WindowCursor 会差很多

### 性能对比

设备为一加5T（Android 10），代码见demo

| 类型                               | 行为                | 耗时（ms）   |
| ---------------------------------- | ------------------- | ------------ |
| sqlite官方                         | 插入50000条相同数据 | 2787         |
| android官方                        | 插入50000条相同数据 | 3538         |
| ---分割线---                       | ---分割线---        | ---分割线--- |
| sqlite官方                         | 遍历向前查询全部    | 1928         |
| 基于sqlite官方的SQLiteDirectCursor | 遍历向前查询全部    | 715          |
| android官方                        | 遍历向前查询全部    | 2498         |
| ---分割线---                       | ---分割线---        | ---分割线--- |
| sqlite官方                         | 遍历向后查询全部    | 3471         |
| 基于sqlite官方的SQLiteDirectCursor | 遍历向后查询全部    | ANR          |
| android官方                        | 遍历向后查询全部    | 3943         |




### 下面是原官方文档

This is an Android specific distribution of the latest versions of SQLite. It contains the latest
SQLite version and the Android specific database APIs derived from AOSP packaged as an AAR
library distributed on jcenter.

Why?
----

- **Consistent**
- **Faster**
- **Up-to-date**

Even the latest version of Android is several versions behind the latest version of SQLite.
Theses versions do not have the bug fixes, performance improvements, or new features present in
current versions of SQLite. This problem is worse the older the version of the OS the device has.
Using this library you can keep up to date with the latest versions of SQLite and provide a
consistent version across OS versions and devices.

Use new SQLite features:

- **[JSON1 extension](https://www.sqlite.org/json1.html)**
- **[Common Table expressions](https://www.sqlite.org/lang_with.html)**
- **[Indexes on expressions](https://www.sqlite.org/expridx.html)**
- **[Full Text Search 5](https://www.sqlite.org/fts5.html)**
- **[Generated Columns](https://www.sqlite.org/gencol.html)**
- **[DROP COLUMN support](https://www.sqlite.org/lang_altertable.html#altertabdropcol)**

Usage
-----

Follow the guidelines from [jitpack.io](https://jitpack.io) to add the JitPack repository to your build file if you have not.

Typically, this means an edit to your `build.gradle` file to add a new `repository` definition in the `allprojects` block, like this:

```gradle
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

Then add the sqlite-android artifact from this repository as a dependency:

```gradle
dependencies {
    implementation 'com.github.requery:sqlite-android:3.38.4'
}
```
Then change usages of `android.database.sqlite.SQLiteDatabase` to
`io.requery.android.database.sqlite.SQLiteDatabase`, similarly extend
`io.requery.android.database.sqlite.SQLiteOpenHelper` instead of
`android.database.sqlite.SQLiteOpenHelper`. Note similar changes maybe required for classes that
depended on `android.database.sqlite.SQLiteDatabase` equivalent APIs are provided in the
`io.requery.android.database.sqlite` package.

If you expose `Cursor` instances across processes you should wrap the returned cursors in a
[CrossProcessCursorWrapper](http://developer.android.com/reference/android/database/CrossProcessCursorWrapper.html)
for performance reasons the cursors are not cross process by default.

### Support library compatibility

The library implements the SupportSQLite interfaces provided by the support library. Use
`RequerySQLiteOpenHelperFactory` to obtain an implementation of `(Support)SQLiteOpenHelper` based
on a `SupportSQLiteOpenHelper.Configuration` and `SupportSQLiteOpenHelper.Callback`.

This also allows you to use sqlite-android with libraries like Room by passing an instance
of `RequerySQLiteOpenHelperFactory` to them.


CPU Architectures
-----------------

The native library is built for the following CPU architectures:

- `armeabi-v7a` ~1.4 MB
- `arm64-v8a` ~2 MB
- `x86` ~2.1 MB
- `x86_64` ~2.1 MB

However you may not want to include all binaries in your apk. You can exclude certain variants by
using `packagingOptions`:

```gradle
android {
    packagingOptions {
        exclude 'lib/armeabi-v7a/libsqlite3x.so'
        exclude 'lib/arm64-v8a/libsqlite3x.so'
        exclude 'lib/x86/libsqlite3x.so'
        exclude 'lib/x86_64/libsqlite3x.so'
    }
}
```

The size of the artifacts with only the armeabi-v7a binary is **~1.4 MB**. In general you can use
armeabi-v7a on the majority of Android devices including Intel Atom which provides a native
translation layer, however performance under the translation layer is worse than using the x86
binary.

Note that starting August 1, 2019, your apps published on Google Play will [need to support 64-bit architectures](https://developer.android.com/distribute/best-practices/develop/64-bit).

Requirements
------------

The min SDK level is API level 14 (Ice Cream Sandwich).

Versioning
----------

The library is versioned after the version of SQLite it contains. For changes specific to just the
wrapper API a revision number is added e.g. 3.12.0-X, where X is the revision number.

Acknowledgements
----------------
This project is based on the AOSP code and the [Android SQLite bindings](https://www.sqlite.org/android/doc/trunk/www/index.wiki)
No official distributions are made from the Android SQLite bindings it and it has not been updated
in a while, this project starts there and makes significant changes:

Changes
-------

- **Fast read performance:** The original SQLite bindings filled the CursorWindow using it's
  Java methods from native C++. This was because there is no access to the native CursorWindow
  native API from the NDK. Unfortunately this slowed read performance significantly (roughly 2x
  worse vs the android database API) because of extra JNI roundtrips. This has been rewritten
  without the JNI to Java calls (so more like the original AOSP code) and also using a local memory
  CursorWindow.
- Reuse of android.database.sqlite.*, the original SQLite bindings replicated the entire
  android.database.sqlite API structure including exceptions & interfaces. This project does not
  do that, instead it reuses the original classes/interfaces when possible in order to simplify
  migration and/or use with existing code.
- Unit tests added
- Compile with [clang](http://clang.llvm.org/) toolchain
- Compile with FTS3, FTS4, & JSON1 extension
- Migrate to gradle build
- buildscript dynamically fetches and builds the latest sqlite source from sqlite.org
- Added consumer proguard rules
- Use androidx-core version of `CancellationSignal`
- Fix bug in `SQLiteOpenHelper.getDatabaseLocked()` wrong path to `openOrCreateDatabase`
- Fix removed members in AbstractWindowCursor
- Made the AOSP code (mostly) warning free but still mergable from source
- Deprecated classes/methods removed
- Loadable extension support

License
-------

    Copyright (C) 2017-2021 requery.io
    Copyright (C) 2005-2012 The Android Open Source Project
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
