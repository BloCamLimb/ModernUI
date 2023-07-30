/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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
 */

package icyllis.modernui.text.style;

import icyllis.modernui.text.TextPaint;
import icyllis.modernui.view.View;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
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

    private final int mId = sIdCounter.getAndIncrement();

    /**
     * Performs the click action associated with this span.
     */
    public abstract void onClick(@Nonnull View widget);

    /**
     * Makes the text underlined and in the link color.
     */
    @Override
    public void updateDrawState(@Nonnull TextPaint ds) {
        ds.setUnderline(true);
        ds.setColor(ds.linkColor);
    }

    /**
     * Get the unique ID for this span.
     *
     * @return The unique ID.
     */
    @ApiStatus.Internal
    public final int getId() {
        return mId;
    }
}
