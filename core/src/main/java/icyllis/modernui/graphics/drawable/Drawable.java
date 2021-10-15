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

package icyllis.modernui.graphics.drawable;

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.math.Rect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

/**
 * A Drawable represents something that can be drawn within its bounds.
 */
public abstract class Drawable {

    private static final Rect ZERO_BOUNDS_RECT = new Rect();

    @Nonnull
    private Rect mBounds = ZERO_BOUNDS_RECT;
    @Nullable
    private WeakReference<Callback> mCallback;

    private int mLayoutDirection;

    /**
     * Draw in its bounds (set via setBounds) respecting optional effects such
     * as alpha (set via setAlpha) and color filter (set via setColorFilter).
     *
     * @param canvas the canvas to draw things
     */
    public abstract void draw(@Nonnull Canvas canvas);

    /**
     * Set the bounds of this drawable for drawing
     *
     * @param left   left bound
     * @param top    top bound
     * @param right  right bound
     * @param bottom bottom bound
     */
    public void setBounds(int left, int top, int right, int bottom) {
        Rect bounds = mBounds;
        if (bounds == ZERO_BOUNDS_RECT) {
            bounds = mBounds = new Rect();
        }
        if (bounds.left != left || bounds.top != top ||
                bounds.right != right || bounds.bottom != bottom) {
            if (!bounds.isEmpty()) {
                invalidateSelf();
            }
            mBounds.set(left, top, right, bottom);
            onBoundsChange(mBounds);
        }
    }

    /**
     * Specify a bounding rectangle for the Drawable. This is where the drawable
     * will draw when its draw() method is called.
     */
    public void setBounds(@Nonnull Rect bounds) {
        setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
    }

    /**
     * Return a copy of the drawable's bounds in the specified Rect (allocated
     * by the caller). The bounds specify where this will draw when its draw()
     * method is called.
     *
     * @param bounds Rect to receive the drawable's bounds (allocated by the
     *               caller).
     */
    public final void copyBounds(@Nonnull Rect bounds) {
        bounds.set(mBounds);
    }

    /**
     * Return a copy of the drawable's bounds in a new Rect. This returns the
     * same values as getBounds(), but the returned object is guaranteed to not
     * be changed later by the drawable (i.e. it retains no reference to this
     * rect). If the caller already has a Rect allocated, call copyBounds(rect).
     *
     * @return A copy of the drawable's bounds
     */
    @Nonnull
    public final Rect copyBounds() {
        return mBounds.copy();
    }

    /**
     * Return the drawable's bounds Rect. Note: for efficiency, the returned
     * object may be the same object stored in the drawable (though this is not
     * guaranteed), so if a persistent copy of the bounds is needed, call
     * copyBounds(rect) instead.
     * You should also not change the object returned by this method as it may
     * be the same object stored in the drawable.
     *
     * @return The bounds of the drawable (which may change later, so caller
     * beware). DO NOT ALTER the returned object as it may change the
     * stored bounds of this drawable.
     * @see #copyBounds()
     * @see #copyBounds(Rect)
     */
    @Nonnull
    public final Rect getBounds() {
        if (mBounds == ZERO_BOUNDS_RECT) {
            mBounds = new Rect();
        }
        return mBounds;
    }

    /**
     * Return in padding the insets suggested by this Drawable for placing
     * content inside the drawable's bounds. Positive values move toward the
     * center of the Drawable (set Rect.inset).
     *
     * @return true if this drawable actually has a padding, else false. When false is returned,
     * the padding is always set to 0.
     */
    public boolean getPadding(@Nonnull Rect padding) {
        padding.set(0, 0, 0, 0);
        return false;
    }

    /**
     * Implement this interface if you want to create an animated drawable that
     * extends {@link Drawable Drawable}. Upon retrieving a drawable, use
     * {@link Drawable#setCallback(Callback)} to supply your implementation of
     * the interface to the drawable; it uses this interface to schedule and
     * execute animation changes.
     */
    public interface Callback {

        /**
         * Called when the drawable needs to be redrawn.  A view at this point
         * should invalidate itself (or at least the part of itself where the
         * drawable appears).
         *
         * @param drawable The drawable that is requesting the update.
         */
        void invalidateDrawable(@Nonnull Drawable drawable);
    }

    /**
     * Bind a {@link Callback} object to this Drawable.  Required for clients
     * that want to support animated drawables.
     *
     * @param cb The client's Callback implementation.
     * @see #getCallback()
     */
    public final void setCallback(@Nullable Callback cb) {
        mCallback = cb != null ? new WeakReference<>(cb) : null;
    }

    /**
     * Return the current {@link Callback} implementation attached to this
     * Drawable.
     *
     * @return A {@link Callback} instance or null if no callback was set.
     * @see #setCallback(Callback)
     */
    @Nullable
    public Callback getCallback() {
        return mCallback != null ? mCallback.get() : null;
    }

    /**
     * Use the current {@link Callback} implementation to have this Drawable
     * redrawn.  Does nothing if there is no Callback attached to the
     * Drawable.
     *
     * @see Callback#invalidateDrawable
     * @see #getCallback()
     * @see #setCallback(Callback)
     */
    public void invalidateSelf() {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.invalidateDrawable(this);
        }
    }


    /**
     * Returns the resolved layout direction for this Drawable.
     *
     * @return One of {@link icyllis.modernui.view.View#LAYOUT_DIRECTION_LTR},
     *         {@link icyllis.modernui.view.View#LAYOUT_DIRECTION_RTL}
     * @see #setLayoutDirection(int)
     */
    public int getLayoutDirection() {
        return mLayoutDirection;
    }

    /**
     * Set the layout direction for this drawable. Should be a resolved
     * layout direction, as the Drawable has no capacity to do the resolution on
     * its own.
     *
     * @param layoutDirection the resolved layout direction for the drawable,
     *                        either {@link icyllis.modernui.view.View#LAYOUT_DIRECTION_LTR}
     *                        or {@link icyllis.modernui.view.View#LAYOUT_DIRECTION_RTL}
     * @return {@code true} if the layout direction change has caused the
     *         appearance of the drawable to change such that it needs to be
     *         re-drawn, {@code false} otherwise
     * @see #getLayoutDirection()
     */
    public final boolean setLayoutDirection(int layoutDirection) {
        if (mLayoutDirection != layoutDirection) {
            mLayoutDirection = layoutDirection;
            return onLayoutDirectionChanged(layoutDirection);
        }
        return false;
    }

    /**
     * Called when the drawable's resolved layout direction changes.
     *
     * @param layoutDirection the new resolved layout direction
     * @return {@code true} if the layout direction change has caused the
     *         appearance of the drawable to change such that it needs to be
     *         re-drawn, {@code false} otherwise
     * @see #setLayoutDirection(int)
     */
    public boolean onLayoutDirectionChanged(int layoutDirection) {
        return false;
    }

    /**
     * Override this in your subclass to change appearance if you vary based on
     * the bounds.
     */
    protected void onBoundsChange(@Nonnull Rect bounds) {
    }

    /**
     * Returns the drawable's intrinsic width.
     * <p>
     * Intrinsic width is the width at which the drawable would like to be laid
     * out, including any inherent padding. If the drawable has no intrinsic
     * width, such as a solid color, this method returns -1.
     *
     * @return the intrinsic width, or -1 if no intrinsic width
     */
    public int getIntrinsicWidth() {
        return -1;
    }

    /**
     * Returns the drawable's intrinsic height.
     * <p>
     * Intrinsic height is the height at which the drawable would like to be
     * laid out, including any inherent padding. If the drawable has no
     * intrinsic height, such as a solid color, this method returns -1.
     *
     * @return the intrinsic height, or -1 if no intrinsic height
     */
    public int getIntrinsicHeight() {
        return -1;
    }

    /**
     * Returns the minimum width suggested by this Drawable. If a View uses this
     * Drawable as a background, it is suggested that the View use at least this
     * value for its width. (There will be some scenarios where this will not be
     * possible.) This value should INCLUDE any padding.
     *
     * @return The minimum width suggested by this Drawable. If this Drawable
     * doesn't have a suggested minimum width, 0 is returned.
     */
    public int getMinimumWidth() {
        return Math.max(getIntrinsicWidth(), 0);
    }

    /**
     * Returns the minimum height suggested by this Drawable. If a View uses this
     * Drawable as a background, it is suggested that the View use at least this
     * value for its height. (There will be some scenarios where this will not be
     * possible.) This value should INCLUDE any padding.
     *
     * @return The minimum height suggested by this Drawable. If this Drawable
     * doesn't have a suggested minimum height, 0 is returned.
     */
    public int getMinimumHeight() {
        return Math.max(getIntrinsicHeight(), 0);
    }

    /**
     * Specify an alpha value for the drawable. 0 means fully transparent, and
     * 255 means fully opaque. But not necessarily, subclasses may use alpha to
     * achieve specific effects under specific circumstances.
     */
    public void setAlpha(int alpha) {
    }

    /**
     * Gets the current alpha value for the drawable. 0 means fully transparent,
     * 255 means fully opaque. This method is implemented by Drawable subclasses and
     * the value returned is specific to how that class treats alpha. The default
     * return value is 255 if the class does not override this method to return a value
     * specific to its use of alpha.
     */
    public int getAlpha() {
        return 0xFF;
    }
}
