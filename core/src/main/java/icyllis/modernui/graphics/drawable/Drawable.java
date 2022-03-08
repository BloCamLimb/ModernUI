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

import icyllis.modernui.annotation.ColorInt;
import icyllis.modernui.core.ArchCore;
import icyllis.modernui.core.Handler;
import icyllis.modernui.graphics.BlendMode;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.math.Rect;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.LayoutDirection;
import icyllis.modernui.util.StateSet;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.ImageView;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Arrays;

/**
 * A Drawable is a general abstraction for "something that can be drawn."  Most
 * often you will deal with Drawable as the type of resource retrieved for
 * drawing things to the screen; the Drawable class provides a generic API for
 * dealing with an underlying visual resource that may take a variety of forms.
 * Unlike a {@link View}, a Drawable does not have any facility to
 * receive events or otherwise interact with the user.
 *
 * <p>In addition to simple drawing, Drawable provides a number of generic
 * mechanisms for its client to interact with what is being drawn:
 *
 * <ul>
 *     <li> The {@link #setBounds} method <var>must</var> be called to tell the
 *     Drawable where it is drawn and how large it should be.  All Drawables
 *     should respect the requested size, often simply by scaling their
 *     imagery.  A client can find the preferred size for some Drawables with
 *     the {@link #getIntrinsicHeight} and {@link #getIntrinsicWidth} methods.
 *
 *     <li> The {@link #getPadding} method can return from some Drawables
 *     information about how to frame content that is placed inside of them.
 *     For example, a Drawable that is intended to be the frame for a button
 *     widget would need to return padding that correctly places the label
 *     inside of itself.
 *
 *     <li> The {@link #setState} method allows the client to tell the Drawable
 *     in which state it is to be drawn, such as "focused", "selected", etc.
 *     Some drawables may modify their imagery based on the selected state.
 *
 *     <li> The {@link #setLevel} method allows the client to supply a single
 *     continuous controller that can modify the Drawable is displayed, such as
 *     a battery level or progress level.  Some drawables may modify their
 *     imagery based on the current level.
 *
 *     <li> A Drawable can perform animations by calling back to its client
 *     through the {@link Callback} interface.  All clients should support this
 *     interface (via {@link #setCallback}) so that animations will work.  A
 *     simple way to do this is through the system facilities such as
 *     {@link View#setBackground(Drawable)} and {@link ImageView}.
 * </ul>
 * <p>
 * Though usually not visible to the application, Drawables may take a variety
 * of forms:
 *
 * <ul>
 *     <li> <b>Image</b>: a bitmap, like a PNG, JPEG, TGA or BMP image.
 *     <li> <b>Vector</b>: a drawable defined as a set of points, lines, and
 *     curves along with its associated color information. This type of drawable
 *     can be scaled without loss of display quality.
 *     <li> <b>Shape</b>: contains simple drawing commands instead of a raw
 *     bitmap, allowing it to resize better in some cases.
 *     <li> <b>Layers</b>: a compound drawable, which draws multiple underlying
 *     drawables on top of each other.
 *     <li> <b>States</b>: a compound drawable that selects one of a set of
 *     drawables based on its state.
 *     <li> <b>Levels</b>: a compound drawable that selects one of a set of
 *     drawables based on its level.
 *     <li> <b>Scale</b>: a compound drawable with a single child drawable,
 *     whose overall size is modified based on the current level.
 * </ul>
 *
 * <p>
 * At a minimum, custom drawable classes must implement the abstract methods on
 * Drawable and should override the {@link Drawable#draw(Canvas)} method to
 * draw content. Significantly, consider overriding the
 * {@link Drawable#setAlpha(int)} to handle alpha correctly.
 */
public abstract class Drawable {

    private static final Rect ZERO_BOUNDS_RECT = new Rect();

    static final BlendMode DEFAULT_BLEND_MODE = BlendMode.SRC_IN;

    private int[] mStateSet = StateSet.WILD_CARD;
    private int mLevel = 0;
    private Rect mBounds = ZERO_BOUNDS_RECT;  // lazily becomes a new Rect()
    @Nullable
    private WeakReference<Callback> mCallback = null;
    private boolean mVisible = true;

    private int mLayoutDirection;

    /**
     * Draw in its bounds (set via setBounds) respecting optional effects such
     * as alpha (set via setAlpha).
     *
     * @param canvas The canvas to draw into
     */
    public abstract void draw(@Nonnull Canvas canvas);

    /**
     * Specify a bounding rectangle for the Drawable. This is where the drawable
     * will draw when its draw() method is called.
     */
    public void setBounds(int left, int top, int right, int bottom) {
        Rect oldBounds = mBounds;

        if (oldBounds == ZERO_BOUNDS_RECT) {
            oldBounds = mBounds = new Rect();
        }

        if (oldBounds.left != left || oldBounds.top != top ||
                oldBounds.right != right || oldBounds.bottom != bottom) {
            if (!oldBounds.isEmpty()) {
                // first invalidate the previous bounds
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
        return new Rect(mBounds);
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
     * Implement this interface if you want to create an animated drawable that
     * extends {@link Drawable}.
     * Upon retrieving a drawable, use
     * {@link Drawable#setCallback(Callback)}
     * to supply your implementation of the interface to the drawable; it uses
     * this interface to schedule and execute animation changes.
     */
    public interface Callback {

        /**
         * Called when the drawable needs to be redrawn.  A view at this point
         * should invalidate itself (or at least the part of itself where the
         * drawable appears).
         *
         * @param who The drawable that is requesting the update.
         */
        void invalidateDrawable(@Nonnull Drawable who);

        /**
         * A Drawable can call this to schedule the next frame of its
         * animation.  An implementation can generally simply call
         * {@link Handler#postAtTime(Runnable, Object, long)} with
         * the parameters <var>(what, who, when)</var> to perform the
         * scheduling.
         *
         * @param who  The drawable being scheduled.
         * @param what The action to execute.
         * @param when The time (in milliseconds) to run.  The timebase is
         *             {@link ArchCore#timeMillis()}
         */
        void scheduleDrawable(@Nonnull Drawable who, @Nonnull Runnable what, long when);

        /**
         * A Drawable can call this to unschedule an action previously
         * scheduled with {@link #scheduleDrawable}.  An implementation can
         * generally simply call
         * {@link Handler#removeCallbacks(Runnable, Object)} with
         * the parameters <var>(what, who)</var> to unschedule the drawable.
         *
         * @param who  The drawable being unscheduled.
         * @param what The action being unscheduled.
         */
        void unscheduleDrawable(@Nonnull Drawable who, @Nonnull Runnable what);
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
     * Use the current {@link Callback} implementation to have this Drawable
     * scheduled.  Does nothing if there is no Callback attached to the
     * Drawable.
     *
     * @param what The action being scheduled.
     * @param when The time (in milliseconds) to run.
     * @see Callback#scheduleDrawable
     */
    public void scheduleSelf(@Nonnull Runnable what, long when) {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.scheduleDrawable(this, what, when);
        }
    }

    /**
     * Use the current {@link Callback} implementation to have this Drawable
     * unscheduled.  Does nothing if there is no Callback attached to the
     * Drawable.
     *
     * @param what The runnable that you no longer want called.
     * @see Callback#unscheduleDrawable
     */
    public void unscheduleSelf(@Nonnull Runnable what) {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.unscheduleDrawable(this, what);
        }
    }

    /**
     * Returns the resolved layout direction for this Drawable.
     *
     * @return One of {@link View#LAYOUT_DIRECTION_LTR},
     * {@link View#LAYOUT_DIRECTION_RTL}
     * @see #setLayoutDirection(int)
     */
    @View.ResolvedLayoutDir
    public int getLayoutDirection() {
        return mLayoutDirection;
    }

    /**
     * Set the layout direction for this drawable. Should be a resolved
     * layout direction, as the Drawable has no capacity to do the resolution on
     * its own.
     *
     * @param layoutDirection the resolved layout direction for the drawable,
     *                        either {@link View#LAYOUT_DIRECTION_LTR}
     *                        or {@link View#LAYOUT_DIRECTION_RTL}
     * @return {@code true} if the layout direction change has caused the
     * appearance of the drawable to change such that it needs to be
     * re-drawn, {@code false} otherwise
     * @see #getLayoutDirection()
     */
    public final boolean setLayoutDirection(@View.ResolvedLayoutDir int layoutDirection) {
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
     * appearance of the drawable to change such that it needs to be
     * re-drawn, {@code false} otherwise
     * @see #setLayoutDirection(int)
     */
    protected boolean onLayoutDirectionChanged(@View.ResolvedLayoutDir int layoutDirection) {
        return false;
    }

    /**
     * Specify an alpha value for the drawable. 0 means fully transparent, and
     * 255 means fully opaque.
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

    /**
     * Specifies tint color for this drawable.
     * <p>
     * To clear the tint, pass {@code null} to
     * {@link #setTintList(ColorStateList)}.
     *
     * @param tintColor Color to use for tinting this drawable
     * @see #setTintList(ColorStateList)
     * @see #setTintBlendMode(BlendMode)
     */
    public void setTint(@ColorInt int tintColor) {
        setTintList(ColorStateList.valueOf(tintColor));
    }

    /**
     * Specifies tint color for this drawable as a color state list.
     *
     * @param tint Color state list to use for tinting this drawable, or
     *             {@code null} to clear the tint
     * @see #setTint(int)
     * @see #setTintBlendMode(BlendMode)
     */
    public void setTintList(@Nullable ColorStateList tint) {
    }

    /**
     * Specifies a tint blending mode for this drawable.
     * <p>
     * Defines how this drawable's tint color should be blended into the drawable
     * before it is drawn to screen. Default tint mode is {@link BlendMode#SRC_IN}.
     *
     * @param blendMode BlendMode to apply to the drawable
     * @see #setTint(int)
     * @see #setTintList(ColorStateList)
     */
    public void setTintBlendMode(@Nonnull BlendMode blendMode) {
    }

    /**
     * Specifies the hotspot's location within the drawable.
     *
     * @param x The X coordinate of the center of the hotspot
     * @param y The Y coordinate of the center of the hotspot
     */
    public void setHotspot(float x, float y) {
    }

    /**
     * Sets the bounds to which the hotspot is constrained, if they should be
     * different from the drawable bounds.
     *
     * @param left   position in pixels of the left bound
     * @param top    position in pixels of the top bound
     * @param right  position in pixels of the right bound
     * @param bottom position in pixels of the bottom bound
     * @see #getHotspotBounds(Rect)
     */
    public void setHotspotBounds(int left, int top, int right, int bottom) {
    }

    /**
     * Populates {@code outRect} with the hotspot bounds.
     *
     * @param outRect the rect to populate with the hotspot bounds
     * @see #setHotspotBounds(int, int, int, int)
     */
    public void getHotspotBounds(@Nonnull Rect outRect) {
        outRect.set(getBounds());
    }

    /**
     * Indicates whether this drawable will change its appearance based on
     * state. Clients can use this to determine whether it is necessary to
     * calculate their state and call setState.
     *
     * @return True if this drawable changes its appearance based on state,
     * false otherwise.
     * @see #setState(int[])
     */
    public boolean isStateful() {
        return false;
    }

    /**
     * Indicates whether this drawable has at least one state spec explicitly
     * specifying state_focused.
     *
     * <p>Note: A View uses a {@link Drawable} instance as its background and it
     * changes its appearance based on a state. On keyboard devices, it should
     * specify its state_focused to make sure the user
     * knows which view is holding the focus.</p>
     *
     * @return {@code true} if state_focused is specified
     * for this drawable.
     */
    public boolean hasFocusStateSpecified() {
        return false;
    }

    /**
     * Specify a set of states for the drawable. These are use-case specific,
     * so see the relevant documentation. As an example, the background for
     * widgets like Button understand the following states:
     * [state_focused, state_pressed].
     *
     * <p>If the new state you are supplying causes the appearance of the
     * Drawable to change, then it is responsible for calling
     * {@link #invalidateSelf} in order to have itself redrawn, <em>and</em>
     * true will be returned from this function.
     *
     * <p>Note: The Drawable holds a reference on to <var>stateSet</var>
     * until a new state array is given to it, so you must not modify this
     * array during that time.</p>
     *
     * @param stateSet The new set of states to be displayed.
     * @return Returns true if this change in state has caused the appearance
     * of the Drawable to change (hence requiring an invalidate), otherwise
     * returns false.
     */
    public boolean setState(@Nonnull final int[] stateSet) {
        if (!Arrays.equals(mStateSet, stateSet)) {
            mStateSet = stateSet;
            return onStateChange(stateSet);
        }
        return false;
    }

    /**
     * Describes the current state, as a union of primitive states, such as
     * {@link icyllis.modernui.R.attr#state_focused},
     * {@link icyllis.modernui.R.attr#state_selected}, etc.
     * Some drawables may modify their imagery based on the selected state.
     *
     * @return An array of resource Ids describing the current state.
     */
    @Nonnull
    public int[] getState() {
        return mStateSet;
    }

    /**
     * If this Drawable does transition animations between states, ask that
     * it immediately jump to the current state and skip any active animations.
     */
    public void jumpToCurrentState() {
    }

    /**
     * @return The current drawable that will be used by this drawable. For simple drawables, this
     * is just the drawable itself. For drawables that change state like
     * {@link StateListDrawable} and {@link LevelListDrawable} this will be the child drawable
     * currently in use.
     */
    @Nullable
    public Drawable getCurrent() {
        return this;
    }

    /**
     * Specify the level for the drawable.  This allows a drawable to vary its
     * imagery based on a continuous controller, for example to show progress
     * or volume level.
     *
     * <p>If the new level you are supplying causes the appearance of the
     * Drawable to change, then it is responsible for calling
     * {@link #invalidateSelf} in order to have itself redrawn, <em>and</em>
     * true will be returned from this function.
     *
     * @param level The new level, from 0 (minimum) to 10000 (maximum).
     * @return Returns true if this change in level has caused the appearance
     * of the Drawable to change (hence requiring an invalidate), otherwise
     * returns false.
     */
    public final boolean setLevel(int level) {
        if (mLevel != level) {
            mLevel = level;
            return onLevelChange(level);
        }
        return false;
    }

    /**
     * Retrieve the current level.
     *
     * @return int Current level, from 0 (minimum) to 10000 (maximum).
     */
    public final int getLevel() {
        return mLevel;
    }

    /**
     * Set whether this Drawable is visible.  This generally does not impact
     * the Drawable's behavior, but is a hint that can be used by some
     * Drawables, for example, to decide whether run animations.
     *
     * @param visible Set to true if visible, false if not.
     * @param restart You can supply true here to force the drawable to behave
     *                as if it has just become visible, even if it had last
     *                been set visible.  Used for example to force animations
     *                to restart.
     * @return true if the new visibility is different from its previous state.
     */
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = mVisible != visible;
        if (changed) {
            mVisible = visible;
            invalidateSelf();
        }
        return changed;
    }

    public final boolean isVisible() {
        return mVisible;
    }

    /**
     * Set whether this Drawable is automatically mirrored when its layout direction is RTL
     * (right-to left). See {@link LayoutDirection}.
     *
     * @param mirrored Set to true if the Drawable should be mirrored, false if not.
     */
    public void setAutoMirrored(boolean mirrored) {
    }

    /**
     * Tells if this Drawable will be automatically mirrored  when its layout direction is RTL
     * right-to-left. See {@link LayoutDirection}.
     *
     * @return boolean Returns true if this Drawable will be automatically mirrored.
     */
    public boolean isAutoMirrored() {
        return false;
    }

    /**
     * Override this in your subclass to change appearance if you recognize the
     * specified state.
     *
     * @return Returns true if the state change has caused the appearance of
     * the Drawable to change (that is, it needs to be drawn), else false
     * if it looks the same and there is no need to redraw it since its
     * last state.
     */
    protected boolean onStateChange(@Nonnull int[] state) {
        return false;
    }

    /**
     * Override this in your subclass to change appearance if you vary based
     * on level.
     *
     * @return Returns true if the level change has caused the appearance of
     * the Drawable to change (that is, it needs to be drawn), else false
     * if it looks the same and there is no need to redraw it since its
     * last level.
     */
    protected boolean onLevelChange(int level) {
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
        final int intrinsicWidth = getIntrinsicWidth();
        return Math.max(intrinsicWidth, 0);
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
        final int intrinsicHeight = getIntrinsicHeight();
        return Math.max(intrinsicHeight, 0);
    }

    /**
     * Return in padding the insets suggested by this Drawable for placing
     * content inside the drawable's bounds. Positive values move toward the
     * center of the Drawable (set {@link Rect#inset}).
     *
     * @return true if this drawable actually has a padding, else false. When false is returned,
     * the padding is always set to 0.
     */
    public boolean getPadding(@Nonnull Rect padding) {
        padding.set(0, 0, 0, 0);
        return false;
    }

    /**
     * Make this drawable mutable. This operation cannot be reversed. A mutable
     * drawable is guaranteed to not share its state with any other drawable.
     * This is especially useful when you need to modify properties of drawables
     * loaded from resources. By default, all drawables instances loaded from
     * the same resource share a common state; if you modify the state of one
     * instance, all the other instances will receive the same modification.
     * <p>
     * Calling this method on a mutable Drawable will have no effect.
     *
     * @return This drawable.
     * @see ConstantState
     * @see #getConstantState()
     */
    @Nonnull
    public Drawable mutate() {
        return this;
    }

    /**
     * Clears the mutated state, allowing this drawable to be cached and
     * mutated again.
     */
    public void clearMutated() {
        // Default implementation is no-op.
    }

    /**
     * Return a {@link ConstantState} instance that holds the shared state of this Drawable.
     *
     * @return The ConstantState associated to that Drawable.
     * @see ConstantState
     * @see Drawable#mutate()
     */
    @Nullable
    public ConstantState getConstantState() {
        return null;
    }

    /**
     * This abstract class is used by {@link Drawable}s to store shared constant state and data
     * between Drawables.
     * <p>
     * Use {@link Drawable#getConstantState()} to retrieve the ConstantState of a drawable. Calling
     * {@link Drawable#mutate()} on a Drawable should typically create a new ConstantState for that
     * Drawable.
     */
    public static abstract class ConstantState {

        /**
         * Creates a new Drawable instance from its constant state.
         *
         * @return a new drawable object based on this constant state
         */
        @Nonnull
        public abstract Drawable newDrawable();
    }
}
