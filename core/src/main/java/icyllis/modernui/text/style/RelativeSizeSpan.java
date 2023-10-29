/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.text.*;
import icyllis.modernui.util.Parcel;

/**
 * Uniformly scales the size of the text to which it's attached by a certain proportion.
 * <p>
 * For example, a <code>RelativeSizeSpan</code> that increases the text size by 50% can be
 * constructed like this:
 * <pre>{@code
 * SpannableString string = new SpannableString("Text with relative size span");
 * string.setSpan(new RelativeSizeSpan(1.5f), 10, 24, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}</pre>
 */
public class RelativeSizeSpan extends MetricAffectingSpan implements ParcelableSpan {

    private final float mProportion;

    /**
     * Creates a {@link RelativeSizeSpan} based on a proportion.
     *
     * @param proportion the proportion with which the text is scaled.
     */
    public RelativeSizeSpan(float proportion) {
        mProportion = proportion;
    }

    /**
     * Creates a {@link RelativeSizeSpan} from a parcel.
     */
    public RelativeSizeSpan(@NonNull Parcel src) {
        mProportion = src.readFloat();
    }

    @Override
    public int getSpanTypeId() {
        return TextUtils.RELATIVE_SIZE_SPAN;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeFloat(mProportion);
    }

    /**
     * @return the proportion with which the text size is changed.
     */
    public float getSizeChange() {
        return mProportion;
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint paint) {
        paint.setTextSize(paint.getTextSize() * mProportion);
    }
}
