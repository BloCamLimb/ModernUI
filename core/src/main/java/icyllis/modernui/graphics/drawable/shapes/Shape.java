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

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Defines a generic graphical "shape."
 * <p>
 * Any Shape can be drawn to a Canvas with its own draw() method, but more
 * graphical control is available if you instead pass it to a
 * {@link icyllis.modernui.graphics.drawable.ShapeDrawable}.
 * <p>
 * Custom Shape classes must implement {@link #clone()} and return an instance
 * of the custom Shape class.
 */
public abstract class Shape implements Cloneable {

    private float mWidth;
    private float mHeight;

    /**
     * Returns the width of the Shape.
     */
    public final float getWidth() {
        return mWidth;
    }

    /**
     * Returns the height of the Shape.
     */
    public final float getHeight() {
        return mHeight;
    }

    /**
     * Draws this shape into the provided Canvas, with the provided Paint.
     * <p>
     * Before calling this, you must call {@link #resize(float, float)}.
     *
     * @param canvas the Canvas within which this shape should be drawn
     * @param paint  the Paint object that defines this shape's characteristics
     */
    public abstract void draw(@Nonnull Canvas canvas, @Nonnull Paint paint);

    /**
     * Resizes the dimensions of this shape.
     * <p>
     * Must be called before {@link #draw(Canvas, Paint)}.
     *
     * @param width  the width of the shape (in pixels)
     * @param height the height of the shape (in pixels)
     */
    public final void resize(float width, float height) {
        if (width < 0) {
            width = 0;
        }
        if (height < 0) {
            height = 0;
        }
        if (mWidth != width || mHeight != height) {
            mWidth = width;
            mHeight = height;
            onResize(width, height);
        }
    }

    /**
     * Checks whether the Shape is opaque.
     * <p>
     * Default impl returns {@code true}. Override if your subclass can be
     * opaque.
     *
     * @return true if any part of the drawable is <em>not</em> opaque.
     */
    public boolean hasAlpha() {
        return true;
    }

    /**
     * Callback method called when {@link #resize(float, float)} is executed.
     *
     * @param width  the new width of the Shape
     * @param height the new height of the Shape
     */
    protected void onResize(float width, float height) {
    }

    @Override
    public Shape clone() throws CloneNotSupportedException {
        return (Shape) super.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Shape shape = (Shape) o;
        return Float.compare(shape.mWidth, mWidth) == 0
                && Float.compare(shape.mHeight, mHeight) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mWidth, mHeight);
    }
}
