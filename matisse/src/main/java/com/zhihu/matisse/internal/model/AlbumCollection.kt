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
package com.zhihu.matisse.internal.model

import android.database.Cursor
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.zhihu.matisse.internal.loader.AlbumLoader
import java.lang.ref.WeakReference

class AlbumCollection(
    activity: FragmentActivity,
    private val onAlbumLoad: (Cursor?) -> Unit,
    private val onAlbumReset: () -> Unit,
) : LoaderManager.LoaderCallbacks<Cursor> {

    companion object {
        private const val LOADER_ID = 1
        private const val STATE_CURRENT_SELECTION = "state_current_selection"
    }

    private val mContext = WeakReference(activity)
    private val mLoaderManager = LoaderManager.getInstance(activity)
    private var mCurrentSelection: Int = 0
    private var mLoadFinished: Boolean = false


    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val context = mContext.get()
        require(context != null) { "Context is Null" }
        mLoadFinished = false
        return AlbumLoader.newInstance(context)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        mContext.get() ?: return
        if (!mLoadFinished) {
            mLoadFinished = true
            onAlbumLoad.invoke(data)
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        mContext.get() ?: return
        onAlbumReset.invoke()
    }

    fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            return
        }

        mCurrentSelection = savedInstanceState.getInt(STATE_CURRENT_SELECTION)
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_CURRENT_SELECTION, mCurrentSelection)
    }

    fun onDestroy() {
        mLoaderManager.destroyLoader(LOADER_ID)
    }

    fun loadAlbums() {
        mLoaderManager.initLoader(LOADER_ID, null, this)
    }

    fun getCurrentSelection(): Int {
        return mCurrentSelection
    }

    fun setStateCurrentSelection(currentSelection: Int) {
        mCurrentSelection = currentSelection
    }
}
