/*
 * Modern UI.
 * Copyright (C) 2024 BloCamLimb. All rights reserved.
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
 * The span that moves the position of the text baseline lower.
 * <p>
 * Unlike Android, this Span also affects the relative text size.
 */
public class SubscriptSpan extends MetricAffectingSpan implements ParcelableSpan {

    /**
     * Creates a {@link SubscriptSpan}.
     */
    public SubscriptSpan() {
    }

    /**
     * Creates a {@link SubscriptSpan} from a parcel.
     */
    public SubscriptSpan(@NonNull Parcel src) {
    }

    @Override
    public int getSpanTypeId() {
        return TextUtils.SUBSCRIPT_SPAN;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint textPaint) {
        textPaint.setTextSize(textPaint.getTextSize() * 2f / 3f);
        applyBaselineShift(textPaint);
    }

    protected void applyBaselineShift(@NonNull TextPaint textPaint) {
        // at the moment, we choose textSize instead of ascent for performance
        textPaint.baselineShift += (int) (textPaint.getTextSize() * 0.125f);
    }

    @Override
    public String toString() {
        return "SubscriptSpan{}";
    }
}
