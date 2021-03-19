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
package com.zhihu.matisse.internal.loader

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.database.MergeCursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.loader.content.CursorLoader
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.internal.entity.Album
import com.zhihu.matisse.internal.entity.SelectionSpec

/**
 * Load all albums (grouped by bucket_id) into a single cursor.
 */
class AlbumLoader(context: Context, selection: String, vararg selectionArgs: String) : CursorLoader(
    context,
    MediaStore.Files.getContentUri("external"),
    if (beforeAPI29()) PROJECTION else PROJECTION_29,
    selection,
    selectionArgs,
    BUCKET_ORDER_BY
) {

    companion object {
        const val COLUMN_URI = "uri"
        const val COLUMN_COUNT = "count"

        private const val COLUMN_BUCKET_ID = "bucket_id"
        private const val COLUMN_BUCKET_DISPLAY_NAME = "bucket_display_name"
        private const val COLUMN_MEDIA_TYPE = "media_type"
        private const val COLUMN_SIZE = "_size"
        private const val MEDIA_TYPE_VIDEO = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        private const val MEDIA_TYPE_IMAGE = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString()

        private const val BUCKET_ORDER_BY = "datetaken DESC"

        private val COLUMNS = arrayOf(
            MediaStore.Files.FileColumns._ID,
            COLUMN_BUCKET_ID,
            COLUMN_BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            COLUMN_URI,
            COLUMN_COUNT
        )

        private val PROJECTION = arrayOf(
            MediaStore.Files.FileColumns._ID,
            COLUMN_BUCKET_ID,
            COLUMN_BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            "COUNT(*) AS $COLUMN_COUNT"
        )

        private val PROJECTION_29 = arrayOf(
            MediaStore.Files.FileColumns._ID,
            COLUMN_BUCKET_ID,
            COLUMN_BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE
        )

        // === params for showSingleMediaType: false ===
        private const val SELECTION =
            "($COLUMN_MEDIA_TYPE=? OR $COLUMN_MEDIA_TYPE=?) AND $COLUMN_SIZE>0) GROUP BY (bucket_id"
        private const val SELECTION_29 =
            "($COLUMN_MEDIA_TYPE=? OR $COLUMN_MEDIA_TYPE=?) AND $COLUMN_SIZE>0"
        private val SELECTION_ARGS = arrayOf(MEDIA_TYPE_IMAGE, MEDIA_TYPE_VIDEO)
        // =============================================

        // === params for showSingleMediaType: true ===
        private const val SINGLE_MEDIA_TYPE =
            "$COLUMN_MEDIA_TYPE=? AND $COLUMN_SIZE>0) GROUP BY (bucket_id"
        private const val SINGLE_MEDIA_TYPE_29 =
            "$COLUMN_MEDIA_TYPE=? AND $COLUMN_SIZE>0"

        // =============================================

        // === params for showSingleMediaType: true ===
        private const val GIF_TYPE =
            "$COLUMN_MEDIA_TYPE=? AND $COLUMN_SIZE>0 AND ${MediaStore.MediaColumns.MIME_TYPE}=?) GROUP BY (bucket_id"
        private const val GIF_TYPE_29 =
            "$COLUMN_MEDIA_TYPE=? AND $COLUMN_SIZE>0 AND ${MediaStore.MediaColumns.MIME_TYPE}=?"

        // =============================================


        /**
         * @return 是否是 Android 10 （Q） 之前的版本
         */
        private fun beforeAPI29(): Boolean {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        }


        fun newInstance(context: Context): CursorLoader {
            return when {
                SelectionSpec.onlyShowGif() -> {
                    AlbumLoader(
                        context,
                        if (beforeAPI29()) GIF_TYPE else GIF_TYPE_29,
                        MEDIA_TYPE_IMAGE,
                        "image/gif"
                    )
                }
                SelectionSpec.onlyShowImages() -> {
                    AlbumLoader(
                        context,
                        if (beforeAPI29()) SINGLE_MEDIA_TYPE else SINGLE_MEDIA_TYPE_29,
                        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    )
                }
                SelectionSpec.onlyShowVideos() -> {
                    AlbumLoader(
                        context,
                        if (beforeAPI29()) SINGLE_MEDIA_TYPE else SINGLE_MEDIA_TYPE_29,
                        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
                    )
                }
                else -> {
                    AlbumLoader(
                        context,
                        if (beforeAPI29()) SELECTION else SELECTION_29,
                        *SELECTION_ARGS
                    )
                }
            }
        }
    }

    private fun getUri(cursor: Cursor): Uri {
        val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID))
        val mimeType = cursor.getString(
            cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
        )

        val contentUri = when {
            MimeType.isImage(mimeType) -> {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            MimeType.isVideo(mimeType) -> {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            else -> {
                // ?
                MediaStore.Files.getContentUri("external")
            }
        }
        return ContentUris.withAppendedId(contentUri, id)
    }


    override fun loadInBackground(): Cursor {
        val albums = super.loadInBackground()
        val allAlbum = MatrixCursor(COLUMNS)

        if (beforeAPI29()) {
            var totalCount = 0
            var allAlbumCoverUri:Uri? = null
            val otherAlbums = MatrixCursor(COLUMNS)
            if (albums != null) {
                while (albums.moveToNext()) {
                    val fileId = albums.getLong(
                        albums.getColumnIndex(MediaStore.Files.FileColumns._ID)
                    )
                    val bucketId = albums.getLong(
                        albums.getColumnIndex(COLUMN_BUCKET_ID)
                    )
                    val bucketDisplayName = albums.getString(
                        albums.getColumnIndex(COLUMN_BUCKET_DISPLAY_NAME)
                    )
                    val mimeType = albums.getString(
                        albums.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                    )
                    val uri = getUri(albums)
                    val count = albums.getInt(albums.getColumnIndex(COLUMN_COUNT))

                    otherAlbums.addRow(
                        arrayOf(
                            fileId.toString(),
                            bucketId.toString(),
                            bucketDisplayName,
                            mimeType,
                            uri.toString(),
                            count.toString()
                        )
                    )
                    totalCount += count
                }
                if (albums.moveToFirst()) {
                    allAlbumCoverUri = getUri(albums)
                }
            }

            allAlbum.addRow(
                arrayOf(
                    Album.ALBUM_ID_ALL, Album.ALBUM_ID_ALL, Album.ALBUM_NAME_ALL, null,
                    allAlbumCoverUri?.toString(),
                    totalCount.toString()
                )
            )

            return MergeCursor(arrayOf(allAlbum, otherAlbums))
        } else {
            var totalCount = 0L
            var allAlbumCoverUri: Uri? = null

            // Pseudo GROUP BY
            val countMap = HashMap<Long, Long>()
            if (albums != null) {
                while (albums.moveToNext()) {
                    val bucketId = albums.getLong(albums.getColumnIndex(COLUMN_BUCKET_ID))
                    var count = countMap[bucketId]
                    if (count == null) {
                        count = 1L
                    } else {
                        count++
                    }
                    countMap[bucketId] = count
                }
            }

            val otherAlbums = MatrixCursor(COLUMNS)
            if (albums != null) {
                if (albums.moveToFirst()) {
                    allAlbumCoverUri = getUri(albums)

                    val done = HashSet<Long>()

                    do {
                        val bucketId = albums.getLong(albums.getColumnIndex(COLUMN_BUCKET_ID))

                        if (done.contains(bucketId)) {
                            continue
                        }

                        val fileId = albums.getLong(
                            albums.getColumnIndex(MediaStore.Files.FileColumns._ID)
                        )
                        val bucketDisplayName = albums.getString(
                            albums.getColumnIndex(COLUMN_BUCKET_DISPLAY_NAME)
                        )
                        val mimeType = albums.getString(
                            albums.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                        )
                        val uri = getUri(albums)
                        val count = countMap[bucketId]!!

                        otherAlbums.addRow(
                            arrayOf(
                                fileId.toString(),
                                bucketId.toString(),
                                bucketDisplayName,
                                mimeType,
                                uri.toString(),
                                count.toString()
                            )
                        )
                        done.add(bucketId)

                        totalCount += count
                    } while (albums.moveToNext())
                }
            }

            allAlbum.addRow(
                arrayOf(
                    Album.ALBUM_ID_ALL,
                    Album.ALBUM_ID_ALL, Album.ALBUM_NAME_ALL, null,
                    allAlbumCoverUri?.toString(),
                    totalCount.toString()
                )
            )

            return MergeCursor(arrayOf(allAlbum, otherAlbums))
        }
    }


    override fun onContentChanged() {
        // FIXME a dirty way to fix loading multiple times
    }


}