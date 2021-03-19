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
 * distributed under the License is distributed on an &quotAS IS&quot BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.internal.entity

import android.content.Context
import android.widget.Toast
import androidx.annotation.IntDef
import androidx.fragment.app.FragmentActivity
import com.zhihu.matisse.internal.ui.widget.IncapableDialog

const val TOAST = 0x00
const val DIALOG = 0x01
const val NONE = 0x02

@Retention(AnnotationRetention.SOURCE)
@IntDef(TOAST, DIALOG, NONE)
annotation class Form

@SuppressWarnings("unused")
class IncapableCause(val message: String? = null, val title: String? = null, @Form val form: Int = TOAST) {


    fun handleCause(context: Context) {
        when (form) {
            NONE -> {
                // do nothing.
            }
            DIALOG -> {
                val incapableDialog = IncapableDialog.newInstance(title, message)
                incapableDialog.show(
                    (context as FragmentActivity).supportFragmentManager,
                    IncapableDialog::class.java.name
                )
            }
            TOAST -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            else -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}
