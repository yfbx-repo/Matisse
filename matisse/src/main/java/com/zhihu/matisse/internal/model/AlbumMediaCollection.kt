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
import com.zhihu.matisse.internal.entity.Album
import com.zhihu.matisse.internal.loader.AlbumMediaLoader
import java.lang.ref.WeakReference

class AlbumMediaCollection(
    context: FragmentActivity,
    private val onAlbumMediaLoad: (Cursor?) -> Unit,
    private val onAlbumMediaReset: () -> Unit
) : LoaderManager.LoaderCallbacks<Cursor> {

    companion object {
        private const val LOADER_ID = 2
        private const val ARGS_ALBUM = "args_album"
        private const val ARGS_ENABLE_CAPTURE = "args_enable_capture"
    }

    private val mContext = WeakReference(context)
    private val mLoaderManager = LoaderManager.getInstance(context)

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val context = mContext.get()
        require(context != null) { "Context is Null" }

        val album = args?.getParcelable<Album>(ARGS_ALBUM)
        require(album != null) { "album is Null" }

        return AlbumMediaLoader.newInstance(
            context, album,
            album.isAll() && args.getBoolean(ARGS_ENABLE_CAPTURE, false)
        )
    }


    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        mContext.get() ?: return
        onAlbumMediaLoad.invoke(data)
    }


    override fun onLoaderReset(loader: Loader<Cursor>) {
        mContext.get() ?: return
        onAlbumMediaReset.invoke()
    }


    fun onDestroy() {
        mLoaderManager.destroyLoader(LOADER_ID)
    }

    fun load(target: Album?) {
        load(target, false)
    }

    fun load(target: Album?, enableCapture: Boolean) {
        val args = Bundle()
        args.putParcelable(ARGS_ALBUM, target)
        args.putBoolean(ARGS_ENABLE_CAPTURE, enableCapture)
        mLoaderManager.initLoader(LOADER_ID, args, this)
    }
}
