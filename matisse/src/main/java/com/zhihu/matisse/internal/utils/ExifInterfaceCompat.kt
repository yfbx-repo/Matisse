/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.internal.utils

import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Bug fixture for ExifInterface constructor.
 */
object ExifInterfaceCompat {

    private val TAG = ExifInterfaceCompat::class.java.simpleName

    /**
     * Creates new instance of {@link ExifInterface}.
     * Original constructor won't check filename value, so if null value has been passed,
     * the process will be killed because of SIGSEGV.
     * Google Play crash report system cannot perceive this crash, so this method will throw
     * {@link NullPointerException} when the filename is null.
     *
     * @param filename a JPEG filename.
     * @return {@link ExifInterface} instance.
     */
    fun newInstance(filename: String): ExifInterface {
        return ExifInterface(filename)
    }

    private fun getExifDateTime(filepath: String): Date? {
        val exif = newInstance(filepath)
        val date = exif.getAttribute(ExifInterface.TAG_DATETIME)
        if (date.isNullOrEmpty()) {
            return null
        }
        try {
            val formatter = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            return formatter.parse(date)
        } catch (e: ParseException) {
            Log.d(TAG, "failed to parse date taken", e)
        }
        return null
    }

    /**
     * Read exif info and get datetime value of the photo.
     *
     * @param filepath to get datetime
     * @return when a photo taken.
     */
    fun getExifDateTimeInMillis(filepath: String): Long {
        val datetime = getExifDateTime(filepath) ?: return -1
        return datetime.time
    }

    /**
     * Read exif info and get orientation value of the photo.
     *
     * @param filepath to get exif.
     * @return exif orientation value
     */
    fun getExifOrientation(filepath: String): Int {
        val exif = newInstance(filepath)
        // We only recognize a subset of orientation tag values.
        return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }
}