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

package icyllis.modernui.ui.view;

import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.renderer.Canvas;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Underlying class, used in {@link icyllis.modernui.ui.widget.ScrollBar}
 */
public class ScrollBarDrawable extends Drawable {

    @Nullable
    private Drawable track;
    @Nullable
    private Drawable thumb;

    @Override
    public void draw(@Nonnull Canvas canvas) {
        if (track != null) {
            track.draw(canvas);
        }
        if (thumb != null) {
            thumb.draw(canvas);
        }
    }

    public void setParameters(float range, float offset, float extent, boolean vertical) {
        if (thumb != null) {
            int totalLength = vertical ? getHeight() : getWidth();
            int barThickness = vertical ? getWidth() : getHeight();
            int barLength = Math.round((float) totalLength * extent / range);
            int barOffset = Math.round((float) (totalLength - barLength) * offset / (range - extent));

            barLength = Math.max(barLength, barThickness << 1);
            barOffset = Math.min(barOffset, totalLength - barLength);

            if (vertical) {
                thumb.setBounds(getLeft(), getTop() + barOffset, getRight(), getTop() + barOffset + barLength);
            } else {
                thumb.setBounds(getLeft() + barOffset, getTop(), getLeft() + barOffset + barLength, getBottom());
            }
        }
    }

    @Override
    protected void onBoundsChanged() {
        if (track != null) {
            track.setBounds(getLeft(), getTop(), getRight(), getBottom());
        }
    }

    public void setTrack(@Nullable Drawable track) {
        this.track = track;
    }

    public void setThumb(@Nullable Drawable thumb) {
        this.thumb = thumb;
    }
}
