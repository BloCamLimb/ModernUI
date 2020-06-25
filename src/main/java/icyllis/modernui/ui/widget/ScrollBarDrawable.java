/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.widget;

import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.renderer.Canvas;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ScrollBarDrawable extends Drawable {

    @Nullable
    private Drawable verticalTrack;
    @Nullable
    private Drawable verticalThumb;
    @Nullable
    private Drawable horizontalTrack;
    @Nullable
    private Drawable horizontalThumb;

    private boolean vertical = false;
    private boolean visible = false;

    @Override
    public void draw(@Nonnull Canvas canvas) {

    }
}
