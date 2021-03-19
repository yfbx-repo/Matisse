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
package com.zhihu.matisse.internal.utils

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.entity.IncapableCause
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

object PhotoMetadataUtils {
    private val TAG = PhotoMetadataUtils::class.java.simpleName
    private const val MAX_WIDTH = 1600
    private const val SCHEME_CONTENT = "content"


    fun getBitmapSize(uri: Uri, activity: Activity): Point {
        val resolver = activity.contentResolver
        val imageSize = getBitmapBound(resolver, uri)
        var w = imageSize.x
        var h = imageSize.y
        if (shouldRotate(resolver, uri)) {
            w = imageSize.y
            h = imageSize.x
        }
        if (h == 0) return Point(MAX_WIDTH, MAX_WIDTH)
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        val widthScale = screenWidth * 1.0f / w
        val heightScale = screenHeight * 1.0f / h
        if (widthScale > heightScale) {
            return Point((w * widthScale).toInt(), (h * heightScale).toInt())
        }
        return Point((w * widthScale).toInt(), (h * heightScale).toInt())
    }

    fun getBitmapBound(resolver: ContentResolver, uri: Uri): Point {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        val input = resolver.openInputStream(uri)
        BitmapFactory.decodeStream(input, null, options)
        val width = options.outWidth
        val height = options.outHeight
        input?.close()
        return Point(width, height)
    }

    fun getPath(resolver: ContentResolver, uri: Uri?): String? {
        if (uri == null) return null
        if (SCHEME_CONTENT == uri.scheme) {
            val cursor = resolver.query(uri, arrayOf(MediaStore.Images.ImageColumns.DATA), null, null, null)
            if (cursor == null || !cursor.moveToFirst()) {
                return null
            }
            val path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA))
            cursor.close()
            return path
        }
        return uri.path
    }

    fun isAcceptable(context: Context, item: Item): IncapableCause? {
        if (!isSelectableType(context, item)) {
            return IncapableCause(context.getString(R.string.error_file_type))
        }
        SelectionSpec.filters?.forEach {
            val incapableCause = it.filter(context, item)
            if (incapableCause != null) {
                return incapableCause
            }
        }
        return null
    }

    private fun isSelectableType(context: Context, item: Item): Boolean {
        val resolver = context.contentResolver
        SelectionSpec.mimeTypeSet?.forEach {
            if (it.checkType(resolver, item.contentUri)) {
                return true
            }
        }
        return false
    }

    private fun shouldRotate(resolver: ContentResolver, uri: Uri): Boolean {
        val path = getPath(resolver, uri) ?: return false
        val exif = ExifInterfaceCompat.newInstance(path)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
        return orientation == ExifInterface.ORIENTATION_ROTATE_90
                || orientation == ExifInterface.ORIENTATION_ROTATE_270
    }

    fun getSizeInMB(sizeInBytes: Long): Float {
        val df = NumberFormat.getNumberInstance(Locale.US) as DecimalFormat
        df.applyPattern("0.0")
        var result = df.format(sizeInBytes / 1024.0f / 1024.0f)
        Log.e(TAG, "getSizeInMB: $result")
        result = result.replace(",", ".") // in some case , 0.0 will be 0,0
        return result.toFloat()
    }
}
