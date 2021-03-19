/*
 * Copyright (C) 2014 nohana, Inc.
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quotAS IS&quot BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.internal.entity

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.loader.AlbumLoader

class Album(
    val id: String,
    val coverUri: Uri,
    val albumName: String,
    var count: Long
) : Parcelable {

    fun addCaptureCount() {
        count++
    }

    fun getDisplayName(context: Context): String {
        if (isAll()) {
            return context.getString(R.string.album_name_all)
        }
        return albumName
    }

    fun isAll(): Boolean {
        return ALBUM_ID_ALL == id
    }

    fun isEmpty(): Boolean {
        return count == 0L
    }


    companion object CREATOR : Parcelable.Creator<Album> {

        const val ALBUM_ID_ALL = "-1"
        const val ALBUM_NAME_ALL = "All"

        override fun createFromParcel(parcel: Parcel): Album {
            return Album(parcel)
        }

        override fun newArray(size: Int): Array<Album?> {
            return arrayOfNulls(size)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readParcelable(Uri::class.java.classLoader)!!,
        parcel.readString()!!,
        parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeParcelable(coverUri, flags)
        parcel.writeString(albumName)
        parcel.writeLong(count)
    }
}


/**
 * Constructs a new {@link Album} entity from the {@link Cursor}.
 * This method is not responsible for managing cursor resource, such as close, iterate, and so on.
 */
fun Cursor.album(): Album {
    val id = getString(getColumnIndex("bucket_id"))
    val column = getString(getColumnIndex(AlbumLoader.COLUMN_URI))
    val uri = column?.let { Uri.parse(it) } ?: Uri.parse("")
    val albumName = getString(getColumnIndex("bucket_display_name"))
    val count = getLong(getColumnIndex(AlbumLoader.COLUMN_COUNT))
    return Album(id, uri, albumName, count)
}