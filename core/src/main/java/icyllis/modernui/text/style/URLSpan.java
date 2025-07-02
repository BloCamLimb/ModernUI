/*
 * Modern UI.
 * Copyright (C) 2022-2025 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *   Copyright (C) 2006 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package icyllis.modernui.text.style;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Core;
import icyllis.modernui.text.ParcelableSpan;
import icyllis.modernui.text.TextUtils;
import icyllis.modernui.util.Parcel;
import icyllis.modernui.view.View;

/**
 * Implementation of the {@link ClickableSpan} that allows setting a url string. When
 * selecting and clicking on the text to which the span is attached, the <code>URLSpan</code>
 * will try to open the url, by calling {@link Core#openURI(String)}.
 * <p>
 * For example, a <code>URLSpan</code> can be used like this:
 * <pre>
 * SpannableString string = new SpannableString("Text with a url span");
 * string.setSpan(new URLSpan("https://google.com"), 12, 15, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
 * </pre>
 * <img src="https://developer.android.com/reference/android/images/text/style/urlspan.png" />
 * <figcaption>Text with <code>URLSpan</code>.</figcaption>
 */
public class URLSpan extends ClickableSpan implements ParcelableSpan {

    private final String mURL;

    /**
     * Constructs a {@link URLSpan} from an url string.
     *
     * @param url the url string
     */
    public URLSpan(String url) {
        mURL = url;
    }

    /**
     * Constructs a {@link URLSpan} from a parcel.
     */
    public URLSpan(@NonNull Parcel src) {
        mURL = src.readString();
    }

    @Override
    public int getSpanTypeId() {
        return TextUtils.URL_SPAN;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mURL);
    }

    /**
     * Get the url string for this span.
     *
     * @return the url string.
     */
    public String getURL() {
        return mURL;
    }

    @Override
    public void onClick(@NonNull View widget) {
        Core.openURI(mURL);
    }

    @Override
    public String toString() {
        return "URLSpan{" + "URL='" + getURL() + '\'' + '}';
    }
}
