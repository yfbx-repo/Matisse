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

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore
import com.zhihu.matisse.MimeType

class Item(
    val id: Long,
    val mimeType: String?,
    val uri: Uri,
    val size: Long,
    val duration: Long// only for video, in ms
) : Parcelable {


    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString(),
        parcel.readParcelable(Uri::class.java.classLoader)!!,
        parcel.readLong(),
        parcel.readLong()
    )

    fun isCapture(): Boolean {
        return id == ITEM_ID_CAPTURE
    }

    fun isImage(): Boolean {
        return MimeType.isImage(mimeType)
    }

    fun isGif(): Boolean {
        return MimeType.isGif(mimeType)
    }

    fun isVideo(): Boolean {
        return MimeType.isVideo(mimeType)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Item) return false
        return (id == other.id
                && (mimeType != null && mimeType == other.mimeType
                || (mimeType == null && other.mimeType == null)))
                && (uri == other.uri) && size == other.size && duration == other.duration
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + id.hashCode()
        if (mimeType != null) {
            result = 31 * result + mimeType.hashCode()
        }
        result = 31 * result + uri.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + duration.hashCode()
        return result
    }


    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(mimeType)
        parcel.writeParcelable(uri, flags)
        parcel.writeLong(size)
        parcel.writeLong(duration)
    }

    companion object CREATOR : Parcelable.Creator<Item> {
        const val ITEM_ID_CAPTURE = -1L
        const val ITEM_DISPLAY_NAME_CAPTURE = "Capture"


        override fun createFromParcel(parcel: Parcel): Item {
            return Item(parcel)
        }

        override fun newArray(size: Int): Array<Item?> {
            return arrayOfNulls(size)
        }
    }
}


fun Cursor.item(): Item {
    val id = getLong(getColumnIndex(MediaStore.Files.FileColumns._ID))
    val mimeType = getString(getColumnIndex(MediaStore.MediaColumns.MIME_TYPE))
    val size = getLong(getColumnIndex(MediaStore.MediaColumns.SIZE))
    val duration = getLong(getColumnIndex("duration"))
    val mUri = when {
        MimeType.isImage(mimeType) -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        MimeType.isVideo(mimeType) -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else -> MediaStore.Files.getContentUri("external")
    }
    val uri = ContentUris.withAppendedId(mUri, id)
    return Item(id, mimeType, uri, size, duration)
}