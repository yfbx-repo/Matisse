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

import android.content.pm.ActivityInfo

import androidx.annotation.StyleRes

import com.zhihu.matisse.MimeType
import com.zhihu.matisse.R
import com.zhihu.matisse.engine.ImageEngine
import com.zhihu.matisse.engine.impl.GlideEngine
import com.zhihu.matisse.filter.Filter
import com.zhihu.matisse.listener.OnCheckedListener
import com.zhihu.matisse.listener.OnSelectedListener

object SelectionSpec {

    var mimeTypeSet: Set<MimeType>? = null
    var mediaTypeExclusive: Boolean = true
    var showSingleMediaType: Boolean = false

    @StyleRes
    var themeId: Int = R.style.Matisse_Zhihu
    var orientation: Int = 0
    var countable: Boolean = false
    var maxSelectable: Int = 1
    var maxImageSelectable: Int = 0
    var maxVideoSelectable: Int = 0
    var filters: List<Filter>? = null
    var capture: Boolean = false
    var captureStrategy: CaptureStrategy? = null
    var spanCount: Int = 3
    var gridExpectedSize: Int = 0
    var thumbnailScale: Float = 0.5f
    var imageEngine: ImageEngine = GlideEngine()
    var hasInited: Boolean = true
    var onSelectedListener: OnSelectedListener? = null
    var originalable: Boolean = false
    var autoHideToobar: Boolean = false
    var originalMaxSize: Int = Int.MAX_VALUE
    var onCheckedListener: OnCheckedListener? = null
    var showPreview: Boolean = true


    private fun reset() {
        mimeTypeSet = null
        mediaTypeExclusive = true
        showSingleMediaType = false
        themeId = R.style.Matisse_Zhihu
        orientation = 0
        countable = false
        maxSelectable = 1
        maxImageSelectable = 0
        maxVideoSelectable = 0
        filters = null
        capture = false
        captureStrategy = null
        spanCount = 3
        gridExpectedSize = 0
        thumbnailScale = 0.5f
        imageEngine = GlideEngine()
        hasInited = true
        originalable = false
        autoHideToobar = false
        originalMaxSize = Integer.MAX_VALUE
        showPreview = true
    }

    fun singleSelectionModeEnabled(): Boolean {
        return !countable && (maxSelectable == 1 || (maxImageSelectable == 1 && maxVideoSelectable == 1))
    }

    fun needOrientationRestriction(): Boolean {
        return orientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    fun onlyShowImages(): Boolean {
        return mimeTypeSet?.let { showSingleMediaType && MimeType.ofImage().containsAll(it) } ?: false
    }

    fun onlyShowVideos(): Boolean {
        return mimeTypeSet?.let { showSingleMediaType && MimeType.ofVideo().containsAll(it) } ?: false
    }

    fun onlyShowGif(): Boolean {
        return showSingleMediaType && MimeType.ofGif().equals(mimeTypeSet)
    }

}
