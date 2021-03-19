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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.zhihu.matisse.internal.entity.CaptureStrategy
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MediaStoreCompat(val context: Context) {
    private var mCaptureStrategy: CaptureStrategy? = null
    private var mCurrentPhotoUri: Uri? = null
    private var mCurrentPhotoPath: String? = null


    companion object {
        /**
         * Checks whether the device has a camera feature or not.
         *
         * @param context a context to check for camera feature.
         * @return true if the device has a camera feature. false otherwise.
         */
        fun hasCameraFeature(context: Context): Boolean {
            val pm = context.applicationContext.packageManager
            return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
        }
    }


    fun setCaptureStrategy(strategy: CaptureStrategy) {
        mCaptureStrategy = strategy
    }

    fun getCurrentPhotoUri(): Uri? {
        return mCurrentPhotoUri
    }

    fun getCurrentPhotoPath(): String? {
        return mCurrentPhotoPath
    }

    fun dispatchCaptureIntent(context: Context, requestCode: Int) {
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (captureIntent.resolveActivity(context.packageManager) == null) return

        val photoFile = createImageFile()
        val authority = mCaptureStrategy?.authority
        require(authority != null) {
            "CaptureStrategy is required when capture(true)"
        }

        mCurrentPhotoPath = photoFile.absolutePath
        mCurrentPhotoUri = FileProvider.getUriForFile(context, authority, photoFile)
        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentPhotoUri)
        captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val resInfoList = context.packageManager
                .queryIntentActivities(captureIntent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(
                    packageName, mCurrentPhotoUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        if (context is Activity) {
            context.startActivityForResult(captureIntent, requestCode)
        }
        if (context is Fragment) {
            context.startActivityForResult(captureIntent, requestCode)
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())

        val imageFileName = String.format("JPEG_%s.jpg", timeStamp)

        val cacheDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val strategy = mCaptureStrategy ?: return File(cacheDir, imageFileName)
        var storageDir = if (strategy.isPublic) publicDir else cacheDir
        if (strategy.directory != null) {
            storageDir = File(storageDir, strategy.directory)
        }
        if (!storageDir.exists()) storageDir.mkdirs()
        // Avoid joining path components manually
        return File(storageDir, imageFileName)
    }
}
