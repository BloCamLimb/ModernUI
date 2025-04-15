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

package icyllis.modernui.graphics;

import icyllis.modernui.annotation.FloatRange;
import icyllis.modernui.annotation.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * Defines a simple shape, used for bounding graphical regions.
 * <p>
 * Can be computed for a View, or computed by a Drawable, to drive the shape of
 * shadows cast by a View, or to clip the contents of the View.
 *
 * @since 3.12
 */
public final class Outline {
    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static final int TYPE_NONE = 0;
    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static final int TYPE_EMPTY = 1;
    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static final int TYPE_ROUND_RECT = 3;

    private boolean mShouldClip;
    private int mType;
    private final Rect mBounds = new Rect();
    private float mRadius;
    private float mAlpha;

    /**
     * Outline is only allowed to be constructed internally.
     *
     * @hidden
     */
    @ApiStatus.Internal
    public Outline() {
    }

    /**
     * Sets the outline to be empty.
     *
     * @see #isEmpty()
     */
    public void setEmpty() {
        mType = TYPE_EMPTY;
        mBounds.setEmpty();
        mRadius = 0.0f;
    }

    /**
     * Returns whether the Outline is empty.
     * <p>
     * Outlines are empty when constructed, or if {@link #setEmpty()} is called,
     * until a setter method is called
     *
     * @see #setEmpty()
     */
    public boolean isEmpty() {
        return mType == TYPE_EMPTY;
    }

    /**
     * Sets the alpha represented by the Outline - the degree to which the
     * producer is guaranteed to be opaque over the Outline's shape.
     * <p>
     * An alpha value of <code>0.0f</code> either represents completely
     * transparent content, or content that isn't guaranteed to fill the shape
     * it publishes.
     * <p>
     * Content producing a fully opaque (alpha = <code>1.0f</code>) outline is
     * assumed by the drawing system to fully cover content beneath it,
     * meaning content beneath may be optimized away.
     */
    public void setAlpha(@FloatRange(from=0.0, to=1.0) float alpha) {
        mAlpha = alpha;
    }

    /**
     * Returns the alpha represented by the Outline.
     */
    public float getAlpha() {
        return mAlpha;
    }

    /**
     * Sets the Outline to the rounded rect defined by the input coordinates and corner radius.
     * <p>
     * Passing a zero radius is equivalent to calling {@link #setRect(int, int, int, int)}
     */
    public void setRoundRect(int left, int top, int right, int bottom, float radius) {
        if (left >= right || top >= bottom) {
            setEmpty();
            return;
        }

        mType = TYPE_ROUND_RECT;
        mBounds.set(left, top, right, bottom);
        mRadius = radius;
    }

    /**
     * Convenience for {@link #setRoundRect(int, int, int, int, float)}
     */
    public void setRoundRect(@NonNull Rect rect, float radius) {
        setRoundRect(rect.left, rect.top, rect.right, rect.bottom, radius);
    }

    /**
     * Sets the Outline to the rect defined by the input coordinates.
     */
    public void setRect(int left, int top, int right, int bottom) {
        setRoundRect(left, top, right, bottom, 0.0f);
    }

    /**
     * Convenience for {@link #setRect(int, int, int, int)}
     */
    public void setRect(@NonNull Rect rect) {
        setRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    /**
     * Populates {@code outBounds} with the outline bounds, if set, and returns
     * {@code true}. If no outline bounds are set, returns {@code false}.
     *
     * @param outRect the rect to populate with the outline bounds, if set
     * @return {@code true} if {@code outBounds} was populated with outline
     *         bounds, or {@code false} if no outline bounds are set
     */
    public boolean getRect(@NonNull Rect outRect) {
        if (mType != TYPE_ROUND_RECT) {
            return false;
        }
        outRect.set(mBounds);
        return true;
    }

    /**
     * Returns the rounded rect radius, if set. A return value of {@code 0}
     * indicates a non-rounded rect.
     *
     * @return the rounded rect radius
     */
    public float getRadius() {
        return mRadius;
    }

    @ApiStatus.Internal
    public void setNone() {
        mType = TYPE_NONE;
        mAlpha = 0.0f;
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public void setShouldClip(boolean clip) {
        mShouldClip = clip;
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public boolean getShouldClip() {
        return mShouldClip;
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public int getType() {
        return mType;
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public Rect getBounds() {
        return mBounds;
    }
}
