package com.zhihu.matisse.internal.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri

/**
 * @author 工藤
 * @email gougou@16fan.com
 * create at 2018年10月23日12:17:59
 * description:媒体扫描
 */
fun Context.singleMediaScanner(path: String, onScanFinish: () -> Unit) {
    var scanner: MediaScannerConnection? = null
    scanner = MediaScannerConnection(this, object : MediaScannerConnection.MediaScannerConnectionClient {
        override fun onMediaScannerConnected() {
            scanner?.scanFile(path, null)
        }

        override fun onScanCompleted(path: String?, uri: Uri?) {
            scanner?.disconnect()
            onScanFinish.invoke()
        }
    })
    scanner.connect()
}