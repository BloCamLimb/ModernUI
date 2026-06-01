/*
 * ModernUI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.text.style;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.text.*;
import icyllis.modernui.util.Parcel;

/**
 * The span that moves the position of the text baseline higher.
 * <p>
 * Unlike Android, this Span also affects the relative text size.
 */
public class SuperscriptSpan extends MetricAffectingSpan implements ParcelableSpan {

    /**
     * Creates a {@link SuperscriptSpan}.
     */
    public SuperscriptSpan() {
    }

    /**
     * Creates a {@link SuperscriptSpan} from a parcel.
     */
    public SuperscriptSpan(@NonNull Parcel src) {
    }

    @Override
    public int getSpanTypeId() {
        return TextUtils.SUPERSCRIPT_SPAN;
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
        textPaint.baselineShift -= (int) (textPaint.getTextSize() * 0.375f);
    }

    @Override
    public String toString() {
        return "SuperscriptSpan{}";
    }
}
