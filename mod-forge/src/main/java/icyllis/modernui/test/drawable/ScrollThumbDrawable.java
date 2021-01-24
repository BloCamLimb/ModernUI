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

package icyllis.modernui.test.drawable;

import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.Canvas;

import javax.annotation.Nonnull;

public class ScrollThumbDrawable extends Drawable {

    @Override
    public void draw(@Nonnull Canvas canvas) {
        canvas.moveTo(this);
        canvas.setColor(128, 128, 128, 128);
        canvas.drawRect(0, 0, getWidth(), getHeight());
    }

    @Override
    public int getIntrinsicWidth() {
        return 10;
    }

    @Override
    public int getIntrinsicHeight() {
        return 10;
    }
}
