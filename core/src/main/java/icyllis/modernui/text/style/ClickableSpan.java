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
 *   Copyright (C) 2008 The Android Open Source Project
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
import icyllis.modernui.text.TextPaint;
import icyllis.modernui.view.View;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * If an object of this type is attached to the text of a TextView
 * with a movement method of LinkMovementMethod, the affected spans of
 * text can be selected. If selected and clicked, the {@link #onClick} method will
 * be called.
 * <p>
 * The text with a <code>ClickableSpan</code> attached will be underlined and the link color will be
 * used as a text color. The default link color is the theme's accent color or
 * <code>android:textColorLink</code> if this attribute is defined in the theme.
 * For example, considering that we have a <code>CustomClickableSpan</code> that extends
 * <code>ClickableSpan</code>, it can be used like this:
 * <pre>{@code SpannableString string = new SpannableString("Text with clickable text");
 * string.setSpan(new CustomClickableSpan(), 10, 19, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}</pre>
 * <img src="https://developer.android.com/reference/android/images/text/style/clickablespan.png" />
 * <figcaption>Text with <code>ClickableSpan</code>.</figcaption>
 */
public abstract class ClickableSpan extends CharacterStyle implements UpdateAppearance {

    private static final AtomicInteger sIdCounter = new AtomicInteger();

    private int mId = sIdCounter.getAndIncrement();

    /**
     * Performs the click action associated with this span.
     */
    public abstract void onClick(@NonNull View widget);

    /**
     * Makes the text underlined and in the link color.
     */
    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        ds.setUnderline(true);
        ds.setColor(ds.linkColor);
    }

    /**
     * Get the unique ID for this span.
     *
     * @return The unique ID.
     * @hidden
     */
    @ApiStatus.Internal
    public final int getId() {
        return mId;
    }

    @Override
    public String toString() {
        return "ClickableSpan{}";
    }

    @Override
    public ClickableSpan clone() {
        final ClickableSpan span = (ClickableSpan) super.clone();
        span.mId = sIdCounter.getAndIncrement();
        return span;
    }
}
