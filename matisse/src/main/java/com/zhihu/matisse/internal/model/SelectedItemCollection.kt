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
package com.zhihu.matisse.internal.model

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.entity.IncapableCause
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec
import com.zhihu.matisse.internal.ui.widget.CheckView
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils
import com.zhihu.matisse.internal.utils.filePath
import java.util.*

@SuppressWarnings("unused")
class SelectedItemCollection(private val context: Context) {

    companion object {
        const val STATE_SELECTION = "state_selection"
        const val STATE_COLLECTION_TYPE = "state_collection_type"

        /**
         * Empty collection
         */
        const val COLLECTION_UNDEFINED = 0x00

        /**
         * Collection only with images
         */
        const val COLLECTION_IMAGE = 0x01

        /**
         * Collection only with videos
         */
        const val COLLECTION_VIDEO = 0x01.shl(1)

        /**
         * Collection with images and videos.
         */
        const val COLLECTION_MIXED = COLLECTION_IMAGE or COLLECTION_VIDEO
    }


    private var mItems = LinkedHashSet<Item>()
    private var mCollectionType = COLLECTION_UNDEFINED


    fun onCreate(bundle: Bundle?) {
        if (bundle != null) {
            val saved = bundle.getParcelableArrayList<Item>(STATE_SELECTION)
            mItems = LinkedHashSet(saved)
            mCollectionType = bundle.getInt(STATE_COLLECTION_TYPE, COLLECTION_UNDEFINED)
        }
    }

    fun setDefaultSelection(uris: List<Item>) {
        mItems.addAll(uris)
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(STATE_SELECTION, ArrayList(mItems))
        outState.putInt(STATE_COLLECTION_TYPE, mCollectionType)
    }

    fun getDataWithBundle(): Bundle {
        val bundle = Bundle()
        bundle.putParcelableArrayList(STATE_SELECTION, ArrayList(mItems))
        bundle.putInt(STATE_COLLECTION_TYPE, mCollectionType)
        return bundle
    }

    fun add(item: Item): Boolean {
        if (typeConflict(item)) {
            throw  IllegalArgumentException("Can't select images and videos at the same time.")
        }
        val added = mItems.add(item)
        if (added) {
            when (mCollectionType) {
                COLLECTION_UNDEFINED -> {
                    when {
                        item.isImage() -> {
                            mCollectionType = COLLECTION_IMAGE
                        }
                        item.isVideo() -> {
                            mCollectionType = COLLECTION_VIDEO
                        }
                    }
                }
                COLLECTION_IMAGE -> {
                    if (item.isVideo()) {
                        mCollectionType = COLLECTION_MIXED
                    }
                }
                COLLECTION_VIDEO -> {
                    if (item.isImage()) {
                        mCollectionType = COLLECTION_MIXED
                    }
                }
            }
        }
        return added
    }

    fun remove(item: Item): Boolean {
        val removed = mItems.remove(item)
        if (removed) {
            when {
                mItems.size == 0 -> mCollectionType = COLLECTION_UNDEFINED
                mCollectionType == COLLECTION_MIXED -> refineCollectionType()
            }
        }
        return removed
    }

    fun overwrite(items: ArrayList<Item>, collectionType: Int) {
        mCollectionType = if (items.size == 0) COLLECTION_UNDEFINED else collectionType
        mItems.clear()
        mItems.addAll(items)
    }


    fun asList(): List<Item> {
        return ArrayList(mItems)
    }

    fun asListOfUri(): List<Uri> {
        return mItems.map { it.uri }
    }

    fun asListOfString(): List<String> {
        return mItems.map { it.uri.filePath(context) ?: "" }
    }

    fun isEmpty(): Boolean {
        return mItems.isEmpty()
    }

    fun isSelected(item: Item): Boolean {
        return mItems.contains(item)
    }

    fun isAcceptable(item: Item): IncapableCause? {
        return when {
            maxSelectableReached() -> {
                val maxSelectable = currentMaxSelectable()
                val cause = try {
                    context.resources.getQuantityString(R.plurals.error_over_count, maxSelectable, maxSelectable)
                } catch (e: Exception) {
                    context.getString(R.string.error_over_count, maxSelectable)
                }
                IncapableCause(cause)
            }
            typeConflict(item) -> {
                IncapableCause(context.getString(R.string.error_type_conflict))
            }
            else -> PhotoMetadataUtils.isAcceptable(context, item)
        }
    }

    fun maxSelectableReached(): Boolean {
        return mItems.size == currentMaxSelectable()
    }

    // depends
    private fun currentMaxSelectable(): Int {
        return when {
            SelectionSpec.maxSelectable > 0 -> SelectionSpec.maxSelectable
            mCollectionType == COLLECTION_IMAGE -> SelectionSpec.maxImageSelectable
            mCollectionType == COLLECTION_VIDEO -> SelectionSpec.maxVideoSelectable
            else -> SelectionSpec.maxSelectable
        }
    }

    fun getCollectionType(): Int {
        return mCollectionType
    }

    private fun refineCollectionType() {
        var hasImage = false
        var hasVideo = false
        for (i in mItems) {
            if (i.isImage() && !hasImage) hasImage = true
            if (i.isVideo() && !hasVideo) hasVideo = true
        }
        when {
            hasImage && hasVideo -> mCollectionType = COLLECTION_MIXED
            hasImage -> mCollectionType = COLLECTION_IMAGE
            hasVideo -> mCollectionType = COLLECTION_VIDEO
        }
    }

    /**
     * Determine whether there will be conflict media types. A user can only select images and videos at the same time
     * while {@link SelectionSpec#mediaTypeExclusive} is set to false.
     */
    fun typeConflict(item: Item): Boolean {
        return SelectionSpec.mediaTypeExclusive
                && ((item.isImage() && (mCollectionType == COLLECTION_VIDEO || mCollectionType == COLLECTION_MIXED))
                || (item.isVideo() && (mCollectionType == COLLECTION_IMAGE || mCollectionType == COLLECTION_MIXED)))
    }

    fun count(): Int {
        return mItems.size
    }

    fun checkedNumOf(item: Item): Int {
        val index = ArrayList(mItems).indexOf(item)
        return if (index == -1) CheckView.UNCHECKED else index + 1
    }
}
