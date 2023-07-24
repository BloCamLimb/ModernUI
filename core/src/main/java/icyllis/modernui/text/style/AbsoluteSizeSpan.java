/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

import icyllis.modernui.text.*;

import javax.annotation.Nonnull;
import java.io.*;

/**
 * A span that changes the size of the text it's attached to.
 * <p>
 * For example, the size of the text can be changed to 55dp like this:
 * <pre>{@code
 * SpannableString string = new SpannableString("Text with absolute size span");
 * string.setSpan(new AbsoluteSizeSpan(55, true), 10, 23, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}</pre>
 */
public class AbsoluteSizeSpan extends MetricAffectingSpan implements ParcelableSpan {

    private final int mSize;
    private final boolean mScaled;

    /**
     * Set the text size to <code>size</code> physical pixels.
     */
    public AbsoluteSizeSpan(int size) {
        this(size, false);
    }

    /**
     * Set the text size to <code>size</code> physical pixels, or to <code>size</code>
     * device-independent pixels if <code>scaled</code> is true.
     */
    public AbsoluteSizeSpan(int size, boolean scaled) {
        mSize = size;
        mScaled = scaled;
    }

    /**
     * Creates an {@link AbsoluteSizeSpan} from a stream.
     */
    public AbsoluteSizeSpan(@Nonnull DataInput src) throws IOException {
        mSize = src.readInt();
        mScaled = src.readBoolean();
    }

    @Override
    public int getSpanTypeId() {
        return TextUtils.ABSOLUTE_SIZE_SPAN;
    }

    @Override
    public void write(@Nonnull DataOutput dest) throws IOException {
        dest.writeInt(mSize);
        dest.writeBoolean(mScaled);
    }

    /**
     * Get the text size. This is in physical pixels if {@link #isScaled()} returns false or in
     * device-independent pixels if {@link #isScaled()} returns true.
     *
     * @return the text size, either in physical pixels or device-independent pixels.
     * @see AbsoluteSizeSpan#AbsoluteSizeSpan(int, boolean)
     */
    public int getSize() {
        return mSize;
    }

    /**
     * Returns whether the size is in device-independent pixels or not, depending on the
     * <code>scaled</code> flag passed in {@link #AbsoluteSizeSpan(int, boolean)}
     *
     * @return <code>true</code> if the size is in device-independent pixels, <code>false</code>
     * otherwise
     * @see #AbsoluteSizeSpan(int, boolean)
     */
    public boolean isScaled() {
        return mScaled;
    }

    @Override
    public void updateMeasureState(@Nonnull TextPaint paint) {
        if (mScaled) {
            paint.setTextSize(mSize * paint.density);
        } else {
            paint.setTextSize(mSize);
        }
    }
}
