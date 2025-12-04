/*
 * Modern UI.
 * Copyright (C) 2024-2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.pipeline;

import icyllis.arc3d.sketch.Canvas;
import icyllis.arc3d.sketch.Paint;
import icyllis.arc3d.sketch.PaintFilterCanvas;
import icyllis.modernui.annotation.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * @hidden
 */
@ApiStatus.Internal
public class AlphaFilterCanvas extends PaintFilterCanvas {

    private final float mAlphaMultiplier;

    public AlphaFilterCanvas(@NonNull Canvas canvas, float alphaMultiplier) {
        super(canvas);
        mAlphaMultiplier = alphaMultiplier;
    }

    @Override
    public boolean onFilter(Paint paint) {
        paint.setAlpha(paint.getAlpha() * mAlphaMultiplier);
        return true;
    }
}
