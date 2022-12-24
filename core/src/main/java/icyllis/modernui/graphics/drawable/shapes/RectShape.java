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

package icyllis.modernui.graphics.drawable.shapes;

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.RectF;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Defines a rectangle shape.
 * <p>
 * The rectangle can be drawn to a Canvas with its own draw() method,
 * but more graphical control is available if you instead pass
 * the RectShape to a {@link icyllis.modernui.graphics.drawable.ShapeDrawable}.
 */
public class RectShape extends Shape {

    private RectF mRect = new RectF();

    public RectShape() {
    }

    @Override
    public void draw(@Nonnull Canvas canvas, @Nonnull Paint paint) {
        canvas.drawRect(mRect, paint);
    }

    @Override
    protected void onResize(float width, float height) {
        mRect.set(0, 0, width, height);
    }

    /**
     * Returns the RectF that defines this rectangle's bounds.
     */
    protected final RectF rect() {
        return mRect;
    }

    @Override
    public RectShape clone() {
        final RectShape shape = (RectShape) super.clone();
        shape.mRect = new RectF(mRect);
        return shape;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RectShape rectShape = (RectShape) o;

        return Objects.equals(mRect, rectShape.mRect);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + mRect.hashCode();
        return result;
    }
}
