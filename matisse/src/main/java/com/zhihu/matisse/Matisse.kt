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
package com.zhihu.matisse

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import com.zhihu.matisse.ui.MatisseActivity
import java.lang.ref.WeakReference

/**
 * Entry for Matisse's media selection.
 */
class Matisse private constructor(activity: Activity?, fragment: Fragment?) {

    private val mContext = WeakReference(activity)
    private val mFragment = WeakReference(fragment)

    companion object {
        /**
         * Start Matisse from an Activity.
         * <p>
         * This Activity's {@link Activity#onActivityResult(int, int, Intent)} will be called when user
         * finishes selecting.
         *
         * @param activity Activity instance.
         * @return Matisse instance.
         */
        fun from(activity: Activity): Matisse {
            return Matisse(activity, null)
        }

        /**
         * Start Matisse from a Fragment.
         * <p>
         * This Fragment's {@link Fragment#onActivityResult(int, int, Intent)} will be called when user
         * finishes selecting.
         *
         * @param fragment Fragment instance.
         * @return Matisse instance.
         */
        fun from(fragment: Fragment): Matisse {
            return Matisse(null, fragment)
        }

        /**
         * Obtain user selected media' {@link Uri} list in the starting Activity or Fragment.
         *
         * @param data Intent passed by {@link Activity#onActivityResult(int, int, Intent)} or
         *             {@link Fragment#onActivityResult(int, int, Intent)}.
         * @return User selected media' {@link Uri} list.
         */
        fun obtainResult(data: Intent): List<Uri> {
            return data.getParcelableArrayListExtra(MatisseActivity.EXTRA_RESULT_SELECTION)
        }

        /**
         * Obtain user selected media path list in the starting Activity or Fragment.
         *
         * @param data Intent passed by {@link Activity#onActivityResult(int, int, Intent)} or
         *             {@link Fragment#onActivityResult(int, int, Intent)}.
         * @return User selected media path list.
         */
        fun obtainPathResult(data: Intent): List<String>? {
            return data.getStringArrayListExtra(MatisseActivity.EXTRA_RESULT_SELECTION_PATH)
        }

        /**
         * Obtain state whether user decide to use selected media in original
         *
         * @param data Intent passed by {@link Activity#onActivityResult(int, int, Intent)} or
         *             {@link Fragment#onActivityResult(int, int, Intent)}.
         * @return Whether use original photo
         */
        fun obtainOriginalState(data: Intent): Boolean {
            return data.getBooleanExtra(MatisseActivity.EXTRA_RESULT_ORIGINAL_ENABLE, false)
        }
    }


    /**
     * MIME types the selection constrains on.
     * <p>
     * Types not included in the set will still be shown in the grid but can't be chosen.
     *
     * @param mimeTypes MIME types set user can choose from.
     * @return {@link SelectionCreator} to build select specifications.
     * @see MimeType
     * @see SelectionCreator
     */
    fun choose(mimeTypes: Set<MimeType>): SelectionCreator {
        return this.choose(mimeTypes, true)
    }

    /**
     * MIME types the selection constrains on.
     * <p>
     * Types not included in the set will still be shown in the grid but can't be chosen.
     *
     * @param mimeTypes          MIME types set user can choose from.
     * @param mediaTypeExclusive Whether can choose images and videos at the same time during one single choosing
     *                           process. true corresponds to not being able to choose images and videos at the same
     *                           time, and false corresponds to being able to do this.
     * @return {@link SelectionCreator} to build select specifications.
     * @see MimeType
     * @see SelectionCreator
     */
    fun choose(mimeTypes: Set<MimeType>, mediaTypeExclusive: Boolean): SelectionCreator {
        return SelectionCreator(this, mimeTypes, mediaTypeExclusive)
    }

    fun getActivity(): Activity? {
        return mContext.get()
    }

    fun getFragment(): Fragment? {
        return mFragment.get()
    }

}
