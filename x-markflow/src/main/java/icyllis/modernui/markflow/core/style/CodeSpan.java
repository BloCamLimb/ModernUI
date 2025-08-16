/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.markflow.core.style;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.markflow.MarkflowTheme;
import icyllis.modernui.text.TextPaint;
import icyllis.modernui.text.style.MetricAffectingSpan;

public class CodeSpan extends MetricAffectingSpan {

    private final MarkflowTheme mTheme;

    public CodeSpan(@NonNull MarkflowTheme theme) {
        mTheme = theme;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint paint) {
        super.updateDrawState(paint);
        int color = mTheme.getCodeTextColor();
        if (color != 0) {
            paint.setColor(color);
        }
        paint.bgColor = mTheme.getCodeBackgroundColor();
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint paint) {
        paint.setTypeface(mTheme.getCodeTypeface());
        int textSize = mTheme.getCodeTextSize();
        if (textSize > 0) {
            paint.setTextSize(textSize);
        } else {
            float textSizeMultiplier = mTheme.getCodeTextSizeMultiplier();
            if (textSizeMultiplier > 0) {
                paint.setTextSize(paint.getTextSize() * textSizeMultiplier);
            }
        }
    }
}
