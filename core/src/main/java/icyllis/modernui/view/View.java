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

package icyllis.modernui.view;

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.CallSuper;
import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.math.Matrix4;
import icyllis.modernui.math.Point;
import icyllis.modernui.math.PointF;
import icyllis.modernui.math.Transformation;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

/**
 * View is the basic component of UI. View has its own rectangular area on screen,
 * which is also responsible for drawing and event handling.
 * <p>
 * Measure ...
 * <p>
 * Layout ...
 * <p>
 * Attaching ...
 * <p>
 * Hierarchy ...
 * <p>
 * Drawing Contents ...
 * <p>
 * Text Input & Drawing ...
 * <p>
 * Scrolling ...
 * <p>
 * Event Handling ... Native Events & Derivative Events
 * <p>
 * Listeners ...
 * <p>
 * Custom View ...
 *
 * @since 2.0
 */
@UiThread
@SuppressWarnings("unused")
public class View implements Drawable.Callback {

    /**
     * Log marker.
     */
    public static final Marker MARKER = MarkerManager.getMarker("View");

    /**
     * Used to mark a View that has no ID.
     */
    public static final int NO_ID = -1;

    /*
     * Private masks
     *
     * |--------|--------|--------|--------|
     *                             1         PFLAG_SKIP_DRAW
     * |--------|--------|--------|--------|
     *                         1             PFLAG_DRAWABLE_STATE_DIRTY
     *                        1              PFLAG_MEASURED_DIMENSION_SET
     *                       1               PFLAG_FORCE_LAYOUT
     *                      1                PFLAG_LAYOUT_REQUIRED
     * |--------|--------|--------|--------|
     *       1                               PFLAG_CANCEL_NEXT_UP_EVENT
     *     1                                 PFLAG_HOVERED
     * |--------|--------|--------|--------|
     */
    static final int PFLAG_SKIP_DRAW = 0x00000080;
    static final int PFLAG_DRAWABLE_STATE_DIRTY = 0x00000400;
    static final int PFLAG_MEASURED_DIMENSION_SET = 0x00000800;
    static final int PFLAG_FORCE_LAYOUT = 0x00001000;
    static final int PFLAG_LAYOUT_REQUIRED = 0x00002000;

    /**
     * Indicates whether the view is temporarily detached.
     */
    static final int PFLAG_CANCEL_NEXT_UP_EVENT = 0x04000000;

    /**
     * Indicates that the view has received HOVER_ENTER.  Cleared on HOVER_EXIT.
     */
    private static final int PFLAG_HOVERED = 0x10000000;

    private static final int PFLAG2_BACKGROUND_SIZE_CHANGED = 0x00000001;

    // private flags
    int mPrivateFlags;
    int mPrivateFlags2;

    /**
     * View visibility.
     * {@link #setVisibility(int)}
     * {@link #getVisibility()}
     * <p>
     * This view is visible, as view's default value
     */
    public static final int VISIBLE = 0x00000000;

    /**
     * This view is invisible, but it still takes up space for layout.
     */
    public static final int INVISIBLE = 0x00000001;

    /**
     * This view is invisible, and it doesn't take any space for layout.
     */
    public static final int GONE = 0x00000002;

    /**
     * View visibility mask
     */
    static final int VISIBILITY_MASK = 0x00000003;

    /**
     * This view can receive keyboard events
     */
    static final int FOCUSABLE = 0x00000004;

    /**
     * This view is enabled. Interpretation varies by subclass.
     * Use with ENABLED_MASK when calling setFlags.
     */
    static final int ENABLED = 0x00000000;

    /**
     * This view is disabled. Interpretation varies by subclass.
     * Use with ENABLED_MASK when calling setFlags.
     */
    static final int DISABLED = 0x00000008;

    /**
     * Mask indicating bits used for indicating whether this view is enabled
     */
    static final int ENABLED_MASK = 0x00000008;

    /**
     * This view doesn't show scrollbars.
     */
    static final int SCROLLBARS_NONE = 0x00000000;

    /**
     * This view shows horizontal scrollbar.
     */
    static final int SCROLLBARS_HORIZONTAL = 0x00000020;

    /**
     * This view shows vertical scrollbar.
     */
    static final int SCROLLBARS_VERTICAL = 0x00000040;

    /**
     * Mask for use with setFlags indicating bits used for indicating which
     * scrollbars are enabled.
     */
    static final int SCROLLBARS_MASK = 0x00000060;

    /**
     * Indicates this view can be clicked. When clickable, a View reacts
     * to clicks by notifying the OnClickListener.
     */
    static final int CLICKABLE = 0x00004000;

    /*
     * View masks
     * |--------|--------|--------|--------|
     *                                   11  VISIBILITY
     *                                  1    FOCUSABLE
     *                                 1     ENABLED
     *                              11       SCROLLBARS
     * |--------|--------|--------|--------|
     *                     1                 CLICKABLE
     * |--------|--------|--------|--------|
     */
    /**
     * The view flags hold various views states.
     * <p>
     * {@link #setStateFlag(int, int)}
     */
    int mViewFlags;

    /**
     * Parent view of this view
     * {@link #assignParent(ViewParent)}
     */
    private ViewParent mParent;

    /**
     * Internal use
     */
    AttachInfo mAttachInfo;

    /**
     * View id to identify this view in the hierarchy.
     * <p>
     * {@link #getId()}
     * {@link #setId(int)}
     */
    int mId = NO_ID;

    /**
     * The distance in pixels from the left edge of this view's parent
     * to the left edge of this view.
     */
    int mLeft;
    /**
     * The distance in pixels from the left edge of this view's parent
     * to the right edge of this view.
     */
    int mRight;
    /**
     * The distance in pixels from the top edge of this view's parent
     * to the top edge of this view.
     */
    int mTop;
    /**
     * The distance in pixels from the top edge of this view's parent
     * to the bottom edge of this view.
     */
    int mBottom;

    /**
     * The offset, in pixels, by which the content of this view is scrolled
     * horizontally.
     */
    protected int mScrollX;

    /**
     * The offset, in pixels, by which the content of this view is scrolled
     * vertically.
     */
    protected int mScrollY;

    /**
     * The transform matrix for the View. This transform is calculated internally
     * based on the translation, rotation, and scale properties. This matrix
     * affects the view both logically (such as for events) and visually.
     */
    private Transformation mTransformation;

    /**
     * The transition matrix for the View only used for transition animations. This
     * is a separate object because it does not change the properties of the view,
     * only visual effects are affected. This can be null if there is no animation.
     */
    @Nullable
    private Matrix4 mTransitionMatrix;

    /**
     * The opacity of the view as manipulated by the Fade transition. This is a
     * property only used by transitions, which is composited with the other alpha
     * values to calculate the final visual alpha value (offscreen rendering).
     * <p>
     * Note that a View has no alpha property, because alpha only affects visual
     * effects, and will have an impact on performance.
     */
    private float mTransitionAlpha = 1f;

    private int mMinWidth;
    private int mMinHeight;

    /**
     * Cached previous measure spec to avoid unnecessary measurements
     */
    private int mPrevWidthMeasureSpec = Integer.MIN_VALUE;
    private int mPrevHeightMeasureSpec = Integer.MIN_VALUE;

    /**
     * The measurement result in onMeasure(), used to layout
     */
    private int mMeasuredWidth;
    private int mMeasuredHeight;

    private Drawable mBackground;

    private ScrollCache mScrollCache;

    private int[] mDrawableState = null;

    /**
     * The layout parameters associated with this view and used by the parent
     * {@link ViewGroup} to determine how this view should be laid out.
     */
    private ViewGroup.LayoutParams mLayoutParams;

    /**
     * This method is called by ViewGroup.drawChild() to have each child view draw itself.
     */
    final void draw(@Nonnull Canvas canvas, @Nonnull ViewGroup group, boolean clip) {
        final boolean identity = hasIdentityMatrix();
        if (clip && identity &&
                canvas.quickReject(mLeft, mTop, mRight, mBottom)) {
            // quick rejected
            return;
        }

        float alpha = mTransitionAlpha;
        if (alpha <= 0) {
            // completely transparent
            return;
        }

        computeScroll();
        int sx = mScrollX;
        int sy = mScrollY;

        int saveCount = canvas.save();
        canvas.translate(mLeft, mTop);

        if (mTransitionMatrix != null) {
            canvas.multiply(mTransitionMatrix);
        }
        if (!identity) {
            canvas.multiply(getMatrix());
        }

        // true if clip region is not empty, or quick rejected
        boolean hasSpace = true;
        if (clip) {
            hasSpace = canvas.clipRect(0, 0, getWidth(), getHeight());
        }

        canvas.translate(-sx, -sy);

        if (hasSpace) {
            //TODO stacked offscreen rendering
            /*if (alpha < 1) {

            }*/

            if ((mPrivateFlags & PFLAG_SKIP_DRAW) == PFLAG_SKIP_DRAW) {
                dispatchDraw(canvas);
            } else {
                draw(canvas);
            }
        }
        canvas.restoreToCount(saveCount);
    }

    /**
     * Raw method that directly draws this view and its background, foreground,
     * overlay and all children to the given canvas. When implementing a view,
     * override {@link #onDraw(Canvas)} instead of this.
     * <p>
     * This is not the entry point for the view system to draw.
     *
     * @param canvas the canvas to draw content
     */
    @CallSuper
    public void draw(@Nonnull Canvas canvas) {
        drawBackground(canvas);

        onDraw(canvas);

        dispatchDraw(canvas);

        onDrawForeground(canvas);
    }

    private void drawBackground(@Nonnull Canvas canvas) {
        final Drawable background = mBackground;
        if (background == null) {
            return;
        }
        if ((mPrivateFlags2 & PFLAG2_BACKGROUND_SIZE_CHANGED) != 0) {
            background.setBounds(0, 0, getWidth(), getHeight());
            mPrivateFlags2 &= ~PFLAG2_BACKGROUND_SIZE_CHANGED;
        }

        final int scrollX = mScrollX;
        final int scrollY = mScrollY;
        if ((scrollX | scrollY) == 0) {
            background.draw(canvas);
        } else {
            canvas.save();
            canvas.translate(scrollX, scrollY);
            background.draw(canvas);
            canvas.restore();
        }
    }

    /**
     * Draw the content of this view, implement this to do your drawing.
     * <p>
     * Note that (0, 0) will be the top left of the bounds, and (width, height)
     * will be the bottom right of the bounds.
     *
     * @param canvas the canvas to draw content
     */
    protected void onDraw(@Nonnull Canvas canvas) {
    }

    /**
     * Draw the child views.
     *
     * @param canvas the canvas to draw content
     */
    protected void dispatchDraw(@Nonnull Canvas canvas) {
    }

    /**
     * Draw any foreground content for this view.
     * <p>
     * Foreground content may consist of scroll bars, a {@link #setForeground foreground}
     * drawable or other view-specific decorations. The foreground is drawn on top of the
     * primary view content.
     *
     * @param canvas the canvas to draw content
     */
    //TODO foreground
    public void onDrawForeground(@Nonnull Canvas canvas) {

    }

    /**
     * <p>Request the drawing of the horizontal and the vertical scrollbar. The
     * scrollbars are painted only if they have been awakened first.</p>
     *
     * @param canvas the canvas on which to draw the scrollbars
     * @see #awakenScrollBars(int)
     */
    protected final void onDrawScrollBars(@Nonnull Canvas canvas) {
        final ScrollCache cache = mScrollCache;
        if (cache == null || cache.mState == ScrollCache.OFF) {
            return;
        }
        boolean invalidate = false;
    }

    /**
     * Called from client thread every tick on pre-tick, to update or cache something
     *
     * @param ticks elapsed ticks from a gui open, 20 tick = 1 second
     */
    @Deprecated
    protected void tick(int ticks) {

    }

    /**
     * Specifies the rectangle area of this view and all its descendants.
     * <p>
     * Derived classes should NOT override this method for any reason.
     * Derived classes with children should override {@link #onLayout(boolean, int, int, int, int)}.
     * In that method, they should call layout() on each of their children
     *
     * @param left   left position, relative to parent
     * @param top    top position, relative to parent
     * @param right  right position, relative to parent
     * @param bottom bottom position, relative to parent
     */
    public void layout(int left, int top, int right, int bottom) {
        boolean changed = setFrame(left, top, right, bottom);

        if (changed || (mPrivateFlags & PFLAG_LAYOUT_REQUIRED) != 0) {
            onLayout(changed, left, top, right, bottom);

            mPrivateFlags &= ~PFLAG_LAYOUT_REQUIRED;
        }

        mPrivateFlags &= ~PFLAG_FORCE_LAYOUT;
    }

    /**
     * Layout child views, call from {@link #layout(int, int, int, int)}
     *
     * @param changed whether the size or position of this view was changed
     * @param left    left position, relative to parent
     * @param top     top position, relative to parent
     * @param right   right position, relative to parent
     * @param bottom  bottom position, relative to parent
     */
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    }

    /**
     * Assign the rectangle area of this view, called from layout()
     *
     * @param left   left position, relative to parent
     * @param top    top position, relative to parent
     * @param right  right position, relative to parent
     * @param bottom bottom position, relative to parent
     * @return whether the area of this view was changed
     */
    protected boolean setFrame(int left, int top, int right, int bottom) {
        if (mLeft != left || mRight != right || mTop != top || mBottom != bottom) {
            int oldWidth = mRight - mLeft;
            int oldHeight = mBottom - mTop;
            int newWidth = right - left;
            int newHeight = bottom - top;
            boolean sizeChanged = (newWidth != oldWidth) || (newHeight != oldHeight);

            mLeft = left;
            mTop = top;
            mRight = right;
            mBottom = bottom;

            if (sizeChanged) {
                onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
            }

            invalidate();
            mPrivateFlags2 |= PFLAG2_BACKGROUND_SIZE_CHANGED;
            return true;
        }
        return false;
    }

    /**
     * Called when width or height changed
     *
     * @param width      new width
     * @param height     new height
     * @param prevWidth  previous width
     * @param prevHeight previous height
     */
    protected void onSizeChanged(int width, int height, int prevWidth, int prevHeight) {

    }

    /**
     * This is called to find out how big a view should be. The parent
     * supplies constraint information in the width and height parameters.
     * <p>
     * This method is used to post the onMeasure() event, and the actual
     * measurement work is performed in {@link #onMeasure(int, int)}.
     *
     * @param widthMeasureSpec  width measure specification imposed by the parent
     * @param heightMeasureSpec height measure specification imposed by the parent
     * @throws IllegalStateException measured dimension is not set in
     *                               {@link #onMeasure(int, int)}
     * @see #onMeasure(int, int)
     */
    public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean needsLayout = (mPrivateFlags & PFLAG_FORCE_LAYOUT) != 0;

        if (!needsLayout) {
            boolean specChanged =
                    widthMeasureSpec != mPrevWidthMeasureSpec
                            || heightMeasureSpec != mPrevHeightMeasureSpec;
            boolean isSpecExactly =
                    MeasureSpec.getMode(widthMeasureSpec).isExactly()
                            && MeasureSpec.getMode(heightMeasureSpec).isExactly();
            boolean matchesSpecSize =
                    getMeasuredWidth() == MeasureSpec.getSize(widthMeasureSpec)
                            && getMeasuredHeight() == MeasureSpec.getSize(heightMeasureSpec);
            needsLayout = specChanged
                    && (!isSpecExactly || !matchesSpecSize);
        }

        if (needsLayout) {
            // remove the flag first anyway
            mPrivateFlags &= ~PFLAG_MEASURED_DIMENSION_SET;

            // measure ourselves, this should set the measured dimension flag back
            onMeasure(widthMeasureSpec, heightMeasureSpec);

            // the flag should be added in onMeasure() by calling setMeasuredDimension()
            if ((mPrivateFlags & PFLAG_MEASURED_DIMENSION_SET) == 0) {
                throw new IllegalStateException(getClass().getName() +
                        "#onMeasure() did not set the measured dimension" +
                        "by calling setMeasuredDimension()");
            }

            mPrivateFlags |= PFLAG_LAYOUT_REQUIRED;
        }

        mPrevWidthMeasureSpec = widthMeasureSpec;
        mPrevHeightMeasureSpec = heightMeasureSpec;
    }

    /**
     * Measure the view and its content to determine the measured width and the
     * measured height. This method is invoked by {@link #measure(int, int)} and
     * should be overridden by subclasses to provide accurate and efficient
     * measurement of their contents.
     * <p>
     * <strong>CONTRACT:</strong> When overriding this method, you
     * <em>must</em> call {@link #setMeasuredDimension(int, int)} to store the
     * measured width and height of this view. Failure to do so will trigger an
     * <code>IllegalStateException</code>, thrown by {@link #measure(int, int)}.
     * Calling super.onMeasure() is a valid use.
     * <p>
     * The base class implementation of measure defaults to the background size,
     * unless a larger size is allowed by the MeasureSpec. Subclasses should
     * override the base one to provide better measurements of their content.
     *
     * @param widthMeasureSpec  width measure specification imposed by the parent
     *                          {@link MeasureSpec}
     * @param heightMeasureSpec height measure specification imposed by the parent
     *                          {@link MeasureSpec}
     */
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getDefaultSize(mMinWidth, widthMeasureSpec),
                getDefaultSize(mMinHeight, heightMeasureSpec));
    }

    /**
     * Set the measured dimension, must be called by {@link #onMeasure(int, int)}
     *
     * @param measuredWidth  the measured width of this view
     * @param measuredHeight the measured height of this view
     */
    protected final void setMeasuredDimension(int measuredWidth, int measuredHeight) {
        mMeasuredWidth = measuredWidth;
        mMeasuredHeight = measuredHeight;

        mPrivateFlags |= PFLAG_MEASURED_DIMENSION_SET;
    }

    /**
     * Get measured width information
     * This should be used during measurement and layout calculations only.
     * Use {@link #getWidth()} to get the width of this view after layout.
     *
     * @return measured width of this view
     */
    public final int getMeasuredWidth() {
        return mMeasuredWidth;
    }

    /**
     * Get measured height information
     * This should be used during measurement and layout calculations only.
     * Use {@link #getHeight()} to get the height of this view after layout.
     *
     * @return measured height of this view
     */
    public final int getMeasuredHeight() {
        return mMeasuredHeight;
    }

    /**
     * Get the LayoutParams associated with this view. All views should have
     * layout parameters. These supply parameters to the <i>parent</i> of this
     * view specifying how it should be arranged. There are many subclasses of
     * ViewGroup.LayoutParams, and these correspond to the different subclasses
     * of ViewGroup that are responsible for arranging their children.
     * <p>
     * This method may return null if this View is not attached to a parent
     * ViewGroup or {@link #setLayoutParams(ViewGroup.LayoutParams)}
     * was not invoked successfully. When a View is attached to a parent
     * ViewGroup, this method must not return null.
     *
     * @return the LayoutParams associated with this view, or null if no
     * parameters have been set yet
     */
    public ViewGroup.LayoutParams getLayoutParams() {
        return mLayoutParams;
    }

    /**
     * Set the layout parameters associated with this view. These supply
     * parameters to the <i>parent</i> of this view specifying how it should be
     * arranged. There are many subclasses of ViewGroup.LayoutParams, and these
     * correspond to the different subclasses of ViewGroup that are responsible
     * for arranging their children.
     *
     * @param params layout parameters for this view
     */
    public void setLayoutParams(@Nonnull ViewGroup.LayoutParams params) {
        mLayoutParams = params;
        requestLayout();
    }

    /**
     * Returns the minimum width of the view.
     *
     * @return the minimum width the view will try to be, in pixels
     * @see #setMinimumWidth(int)
     */
    public int getMinimumWidth() {
        return mMinWidth;
    }

    /**
     * Sets the minimum width of the view. It is not guaranteed the view will
     * be able to achieve this minimum width (for example, if its parent layout
     * constrains it with less available width).
     *
     * @param minWidth The minimum width the view will try to be, in pixels
     * @see #getMinimumWidth()
     */
    public void setMinimumWidth(int minWidth) {
        mMinWidth = minWidth;
        requestLayout();
    }

    /**
     * Returns the minimum height of the view.
     *
     * @return the minimum height the view will try to be, in pixels
     * @see #setMinimumHeight(int)
     */
    public int getMinimumHeight() {
        return mMinHeight;
    }

    /**
     * Sets the minimum height of the view. It is not guaranteed the view will
     * be able to achieve this minimum height (for example, if its parent layout
     * constrains it with less available height).
     *
     * @param minHeight The minimum height the view will try to be, in pixels
     * @see #getMinimumHeight()
     */
    public void setMinimumHeight(int minHeight) {
        mMinHeight = minHeight;
        requestLayout();
    }

    /**
     * Utility to return a default size. Uses the supplied size if the
     * MeasureSpec imposed no constraints. Will get larger if allowed
     * by the MeasureSpec.
     *
     * @param size        default size for this view
     * @param measureSpec measure spec imposed by the parent
     * @return the measured size of this view
     */
    public static int getDefaultSize(int size, int measureSpec) {
        switch (MeasureSpec.getMode(measureSpec)) {
            case EXACTLY:
            case AT_MOST:
                return MeasureSpec.getSize(measureSpec);
        }
        return size;
    }

    public static int resolveSize(int size, int measureSpec) {
        int specSize = MeasureSpec.getSize(measureSpec);
        switch (MeasureSpec.getMode(measureSpec)) {
            case AT_MOST:
                return Math.min(specSize, size);
            case EXACTLY:
                return specSize;
            default:
                return size;
        }
    }

    /**
     * Gets the parent of this view. Note the parent is not necessarily a View.
     *
     * @return the parent of this view
     */
    @Nullable
    public final ViewParent getParent() {
        return mParent;
    }

    /**
     * Sets the parent view during layout.
     *
     * @param parent the parent of this view
     */
    final void assignParent(@Nullable ViewParent parent) {
        if (mParent == null) {
            mParent = parent;
        } else if (parent == null) {
            mParent = null;
        } else {
            throw new IllegalStateException("The parent of view " + this + " has been assigned");
        }
    }

    /**
     * Returns this view's identifier.
     *
     * @return a positive integer used to identify the view or {@link #NO_ID}
     * if the view has no ID
     * @see #setId(int)
     * @see #findViewById(int)
     */
    public int getId() {
        return mId;
    }

    /**
     * Sets the identifier for this view. The identifier does not have to be
     * unique in this view's hierarchy. The identifier should be a positive
     * integer.
     *
     * @param id a number used to identify the view
     * @see #NO_ID
     * @see #getId()
     * @see #findViewById(int)
     */
    public void setId(int id) {
        mId = id;
    }

    //TODO state switching events
    void setStateFlag(int flag, int mask) {
        final int old = mViewFlags;

        mViewFlags = (mViewFlags & ~mask) | (flag & mask);

        final int change = mViewFlags ^ old;
    }

    /**
     * Set visibility of this view
     * See {@link #VISIBLE} or {@link #INVISIBLE} or {@link #GONE}
     *
     * @param visibility visibility to set
     */
    public void setVisibility(int visibility) {
        setStateFlag(visibility, VISIBILITY_MASK);
    }

    /**
     * Get visibility of this view.
     * See {@link #VISIBLE} or {@link #INVISIBLE} or {@link #GONE}
     *
     * @return visibility
     */
    public int getVisibility() {
        return mViewFlags & VISIBILITY_MASK;
    }

    /**
     * Returns the enabled status for this view. The interpretation of the
     * enabled state varies by subclass.
     *
     * @return True if this view is enabled, false otherwise.
     */
    public boolean isEnabled() {
        return (mViewFlags & ENABLED_MASK) == ENABLED;
    }

    /**
     * Set the enabled state of this view. The interpretation of the enabled
     * state varies by subclass.
     *
     * @param enabled True if this view is enabled, false otherwise.
     */
    public void setEnabled(boolean enabled) {
        if (enabled == isEnabled()) return;

        setFlags(enabled ? ENABLED : DISABLED, ENABLED_MASK);

        /*
         * The View most likely has to change its appearance, so refresh
         * the drawable state.
         */
        refreshDrawableState();

        // Invalidate too, since the default behavior for views is to be
        // be drawn at 50% alpha rather than to change the drawable.
        //invalidate(true);

        if (!enabled) {
            //cancelPendingInputEvents();
        }
    }

    /**
     * Indicates whether this view reacts to click events or not.
     *
     * @return true if the view is clickable, false otherwise
     * @see #setClickable(boolean)
     */
    public boolean isClickable() {
        return (mViewFlags & CLICKABLE) == CLICKABLE;
    }

    /**
     * Enables or disables click events for this view. When a view
     * is clickable it will change its state to "pressed" on every click.
     * Subclasses should set the view clickable to visually react to
     * user's clicks.
     *
     * @param clickable true to make the view clickable, false otherwise
     * @see #isClickable()
     */
    public void setClickable(boolean clickable) {
        setFlags(clickable ? CLICKABLE : 0, CLICKABLE);
    }

    void setFlags(int flags, int mask) {
        int old = mViewFlags;
        mViewFlags = (mViewFlags & ~mask) | (flags & mask);
    }

    /**
     * Invalidate the whole view hierarchy. All views will be redrawn
     * in the future.
     */
    public final void invalidate() {
        if (mAttachInfo != null) {
            mAttachInfo.mViewRootImpl.invalidate();
        }
    }

    /**
     * Invalidates the specified Drawable.
     *
     * @param drawable the drawable to invalidate
     */
    @Override
    public void invalidateDrawable(@Nonnull Drawable drawable) {
        if (verifyDrawable(drawable)) {
            invalidate();
        }
    }

    /**
     * Define whether the horizontal scrollbar should have or not.
     *
     * @param enabled true if the horizontal scrollbar should be enabled
     * @see #isHorizontalScrollBarEnabled()
     */
    public void setHorizontalScrollBarEnabled(boolean enabled) {
        if (isHorizontalScrollBarEnabled() != enabled) {
            mViewFlags ^= SCROLLBARS_HORIZONTAL;
        }
    }

    /**
     * Define whether the vertical scrollbar should have or not.
     *
     * @param enabled true if the vertical scrollbar should be enabled
     * @see #isVerticalScrollBarEnabled()
     */
    public void setVerticalScrollBarEnabled(boolean enabled) {
        if (isVerticalScrollBarEnabled() != enabled) {
            mViewFlags ^= SCROLLBARS_VERTICAL;
        }
    }

    /**
     * Indicate whether the horizontal scrollbar should have or not.
     *
     * @return true if the horizontal scrollbar already created, false otherwise
     * @see #setHorizontalScrollBarEnabled(boolean)
     */
    public boolean isHorizontalScrollBarEnabled() {
        return (mViewFlags & SCROLLBARS_HORIZONTAL) != 0;
    }

    /**
     * Indicate whether the vertical scrollbar should have or not.
     *
     * @return true if the vertical scrollbar already created, false otherwise
     * @see #setVerticalScrollBarEnabled(boolean)
     */
    public boolean isVerticalScrollBarEnabled() {
        return (mViewFlags & SCROLLBARS_VERTICAL) != 0;
    }

    private void initScrollCache() {
        if (mScrollCache == null) {
            mScrollCache = new ScrollCache();
        }
    }

    private void initializeScrollBarDrawable() {
        initScrollCache();

        if (mScrollCache.mScrollBar == null) {
            mScrollCache.mScrollBar = new ScrollBar();
            mScrollCache.mScrollBar.setCallback(this);
        }
    }

    /**
     * Defines the vertical scrollbar thumb drawable
     *
     * @see #isVerticalScrollBarEnabled()
     * @see #setVerticalScrollBarEnabled(boolean)
     */
    public void setVerticalScrollbarThumbDrawable(@Nullable Drawable drawable) {
        initializeScrollBarDrawable();
        mScrollCache.mScrollBar.setVerticalThumbDrawable(drawable);
    }

    /**
     * Defines the vertical scrollbar track drawable
     *
     * @see #isVerticalScrollBarEnabled()
     * @see #setVerticalScrollBarEnabled(boolean)
     */
    public void setVerticalScrollbarTrackDrawable(@Nullable Drawable drawable) {
        initializeScrollBarDrawable();
        mScrollCache.mScrollBar.setVerticalTrackDrawable(drawable);
    }

    /**
     * Defines the horizontal thumb drawable
     *
     * @see #isHorizontalScrollBarEnabled()
     * @see #setHorizontalScrollBarEnabled(boolean)
     */
    public void setHorizontalScrollbarThumbDrawable(@Nullable Drawable drawable) {
        initializeScrollBarDrawable();
        mScrollCache.mScrollBar.setHorizontalThumbDrawable(drawable);
    }

    /**
     * Defines the horizontal track drawable
     *
     * @see #isHorizontalScrollBarEnabled()
     * @see #setHorizontalScrollBarEnabled(boolean)
     */
    public void setHorizontalScrollbarTrackDrawable(@Nullable Drawable drawable) {
        initializeScrollBarDrawable();
        mScrollCache.mScrollBar.setHorizontalTrackDrawable(drawable);
    }

    /**
     * Returns the currently configured Drawable for the thumb of the vertical scroll bar if it
     * exists, null otherwise.
     *
     * @see #isVerticalScrollBarEnabled()
     * @see #setVerticalScrollBarEnabled(boolean)
     */
    @Nullable
    public Drawable getVerticalScrollbarThumbDrawable() {
        return mScrollCache != null && mScrollCache.mScrollBar != null
                ? mScrollCache.mScrollBar.getVerticalThumbDrawable() : null;
    }

    /**
     * Returns the currently configured Drawable for the track of the vertical scroll bar if it
     * exists, null otherwise.
     *
     * @see #isVerticalScrollBarEnabled()
     * @see #setVerticalScrollBarEnabled(boolean)
     */
    @Nullable
    public Drawable getVerticalScrollbarTrackDrawable() {
        return mScrollCache != null && mScrollCache.mScrollBar != null
                ? mScrollCache.mScrollBar.getVerticalTrackDrawable() : null;
    }

    /**
     * Returns the currently configured Drawable for the thumb of the horizontal scroll bar if it
     * exists, null otherwise.
     *
     * @see #isHorizontalScrollBarEnabled()
     * @see #setHorizontalScrollBarEnabled(boolean)
     */
    @Nullable
    public Drawable getHorizontalScrollbarThumbDrawable() {
        return mScrollCache != null && mScrollCache.mScrollBar != null
                ? mScrollCache.mScrollBar.getHorizontalThumbDrawable() : null;
    }

    /**
     * Returns the currently configured Drawable for the track of the horizontal scroll bar if it
     * exists, null otherwise.
     *
     * @see #isHorizontalScrollBarEnabled()
     * @see #setHorizontalScrollBarEnabled(boolean)
     */
    @Nullable
    public Drawable getHorizontalScrollbarTrackDrawable() {
        return mScrollCache != null && mScrollCache.mScrollBar != null ?
                mScrollCache.mScrollBar.getHorizontalTrackDrawable() : null;
    }

    /**
     * Return the scrolled left position of this view. This is the left edge of
     * the displayed part of your view. You do not need to draw any pixels
     * farther left, since those are outside of the frame of your view on
     * screen.
     *
     * @return The left edge of the displayed part of your view, in pixels.
     */
    public final int getScrollX() {
        return mScrollX;
    }

    /**
     * Return the scrolled top position of this view. This is the top edge of
     * the displayed part of your view. You do not need to draw any pixels above
     * it, since those are outside of the frame of your view on screen.
     *
     * @return The top edge of the displayed part of your view, in pixels.
     */
    public final int getScrollY() {
        return mScrollY;
    }

    /**
     * Get the width of the view.
     *
     * @return the width in pixels
     */
    public final int getWidth() {
        return mRight - mLeft;
    }

    /**
     * Get the height of the view.
     *
     * @return the height in pixels
     */
    public final int getHeight() {
        return mBottom - mTop;
    }

    /**
     * Get view logic left position on screen.
     * The sum of scroll amounts of all parent views are not counted.
     *
     * @return left
     */
    public final int getLeft() {
        return mLeft;
    }

    /**
     * Get view logic top position on screen.
     * The sum of scroll amounts of all parent views are not counted.
     *
     * @return top
     */
    public final int getTop() {
        return mTop;
    }

    /**
     * Get view logic right position on screen.
     * The sum of scroll amounts of all parent views are not counted.
     *
     * @return right
     */
    public final int getRight() {
        return mRight;
    }

    /**
     * Get view logic bottom position on screen.
     * The sum of scroll amounts of all parent views are not counted.
     *
     * @return bottom
     */
    public final int getBottom() {
        return mBottom;
    }

    /**
     * The transform matrix of this view, which is calculated based on the current
     * rotation, scale, and pivot properties. This will affect this view logically
     * (such as for events) and visually.
     * <p>
     * Note that the matrix should be only used as read-only (like transforming
     * coordinates). In that case, you should call {@link #hasIdentityMatrix()}
     * first to check if the operation can be skipped, because this method will
     * create the matrix if not available. However, if you want to change the
     * transform matrix, you should use methods such as {@link #setTranslationX(float)}.
     *
     * @return the current transform matrix for the view
     * @see #hasIdentityMatrix()
     * @see #getRotation()
     * @see #getScaleX()
     * @see #getScaleY()
     * @see #getPivotX()
     * @see #getPivotY()
     */
    @Nonnull
    public final Matrix4 getMatrix() {
        ensureTransformation();
        return mTransformation.getMatrix();
    }

    /**
     * Returns true if the transform matrix is the identity matrix.
     *
     * @return true if the transform matrix is the identity matrix, false otherwise.
     * @see #getMatrix()
     */
    public final boolean hasIdentityMatrix() {
        if (mTransformation == null) {
            return true;
        }
        return mTransformation.getMatrix().isIdentity();
    }

    private void ensureTransformation() {
        if (mTransformation == null) {
            mTransformation = new Transformation();
        }
    }

    /**
     * Sets the horizontal location of this view relative to its {@link #getLeft() left} position.
     * This effectively positions the object post-layout, in addition to wherever the object's
     * layout placed it. This is not used for transition animation.
     *
     * @param translationX The horizontal position of this view relative to its left position,
     *                     in pixels.
     */
    public void setTranslationX(float translationX) {
        ensureTransformation();
        mTransformation.setTranslationX(translationX);
    }

    /**
     * Changes the transition matrix on the view. This is only used in the transition animation
     * framework, {@link Transition}. When the animation finishes, the matrix
     * should be cleared by calling this method with <code>null</code> as the matrix parameter.
     * <p>
     * Note that this matrix only affects the visual effect of this view, you should never
     * call this method. You should use methods such as {@link #setTranslationX(float)}} instead
     * to change the transformation logically.
     *
     * @param matrix the matrix, null indicates that the matrix should be cleared.
     * @see #getTransitionMatrix()
     */
    public final void setTransitionMatrix(@Nullable Matrix4 matrix) {
        mTransitionMatrix = matrix;
    }

    /**
     * Changes the transition matrix on the view. This is only used in the transition animation
     * framework, {@link Transition}. Returns <code>null</code> when there is no
     * transformation provided by {@link #setTransitionMatrix(Matrix4)}.
     * <p>
     * Note that this matrix only affects the visual effect of this view, you should never
     * call this method. You should use methods such as {@link #setTranslationX(float)}} instead
     * to change the transformation logically.
     *
     * @return the matrix, null indicates that the matrix should be cleared.
     * @see #setTransitionMatrix(Matrix4)
     */
    @Nullable
    public final Matrix4 getTransitionMatrix() {
        return mTransitionMatrix;
    }

    void dispatchAttachedToWindow(AttachInfo info) {
        mAttachInfo = info;
    }

    /**
     * Request layout if layout information changed.
     * This will schedule a layout pass of the view tree.
     */
    public void requestLayout() {
        boolean requestParent = (mPrivateFlags & PFLAG_FORCE_LAYOUT) == 0;

        mPrivateFlags |= PFLAG_FORCE_LAYOUT;

        if (requestParent && mParent != null) {
            mParent.requestLayout();
        }
    }

    /**
     * Add a mark to force this view to be laid out during the next
     * layout pass.
     */
    public void forceLayout() {
        mPrivateFlags |= PFLAG_FORCE_LAYOUT;
    }

    /**
     * Call this to force a view to update its drawable state. This will cause
     * drawableStateChanged to be called on this view. Views that are interested
     * in the new state should call getDrawableState.
     *
     * @see #drawableStateChanged
     * @see #getDrawableState
     */
    public void refreshDrawableState() {
        mPrivateFlags |= PFLAG_DRAWABLE_STATE_DIRTY;
        drawableStateChanged();

        ViewParent parent = mParent;
        if (parent != null) {
            parent.childDrawableStateChanged(this);
        }
    }

    /**
     * If your view subclass is displaying its own Drawable objects, it should
     * override this function and return true for any Drawable it is
     * displaying.  This allows animations for those drawables to be
     * scheduled.
     *
     * <p>Be sure to call through to the super class when overriding this
     * function.
     *
     * @param drawable The Drawable to verify.  Return true if it is one you are
     *                 displaying, else return the result of calling through to the
     *                 super class.
     * @return boolean If true than the Drawable is being displayed in the
     * view; else false and it is not allowed to animate.
     * @see #drawableStateChanged()
     */
    @CallSuper
    protected boolean verifyDrawable(@Nonnull Drawable drawable) {
        // Avoid verifying the scroll bar drawable so that we don't end up in
        // an invalidation loop. This effectively prevents the scroll bar
        // drawable from triggering invalidations and scheduling runnables.
        return drawable == mBackground;
    }

    /**
     * This function is called whenever the state of the view changes in such
     * a way that it impacts the state of drawables being shown.
     * <p>
     * If the View has a StateListAnimator, it will also be called to run necessary state
     * change animations.
     * <p>
     * Be sure to call through to the superclass when overriding this function.
     *
     * @see Drawable#setState(int[])
     */
    protected void drawableStateChanged() {

    }

    /**
     * Return an array of resource IDs of the drawable states representing the
     * current state of the view.
     *
     * @return The current drawable state
     * @see Drawable#setState(int[])
     * @see #drawableStateChanged()
     * @see #onCreateDrawableState(int)
     */
    public final int[] getDrawableState() {
        if (mDrawableState == null || (mPrivateFlags & PFLAG_DRAWABLE_STATE_DIRTY) != 0) {
            mDrawableState = onCreateDrawableState(0);
            mPrivateFlags &= ~PFLAG_DRAWABLE_STATE_DIRTY;
        }
        return mDrawableState;
    }

    /**
     * Generate the new {@link Drawable} state for
     * this view. This is called by the view
     * system when the cached Drawable state is determined to be invalid.  To
     * retrieve the current state, you should use {@link #getDrawableState}.
     *
     * @param extraSpace if non-zero, this is the number of extra entries you
     *                   would like in the returned array in which you can place your own
     *                   states.
     * @return an array holding the current {@link Drawable} state of
     * the view.
     * @see #mergeDrawableStates(int[], int[])
     */
    protected int[] onCreateDrawableState(int extraSpace) {
        return new int[0];
    }

    /**
     * Set the background to a given Drawable, or remove the background. If the
     * background has padding, this View's padding is set to the background's
     * padding. However, when a background is removed, this View's padding isn't
     * touched.
     *
     * @param background The Drawable to use as the background, or null to remove the
     *                   background
     */
    public void setBackground(@Nullable Drawable background) {
        if (background == mBackground) {
            return;
        }
        mBackground = background;
        if (background != null) {
            background.setCallback(this);
        }
    }

    /**
     * Finds the topmost view in the current view hierarchy.
     *
     * @return the topmost view containing this view
     */
    public final View getRootView() {
        if (mAttachInfo != null) {
            View v = mAttachInfo.mRootView;
            if (v != null) {
                return v;
            }
        }

        View v = this;

        while (v.mParent instanceof View) {
            v = (View) v.mParent;
        }

        return v;
    }

    /**
     * Computes the coordinates of this view in its window.
     *
     * @param out the point in which to hold the coordinates
     */
    public void getLocationInWindow(@Nonnull Point out) {
        if (mAttachInfo == null) {
            out.set(0, 0);
            return;
        }

        PointF p = mAttachInfo.mTmpPointF;
        p.set(0, 0);

        if (!hasIdentityMatrix()) {
            getMatrix().transform(p);
        }

        p.offset(mLeft, mTop);

        ViewParent parent = mParent;
        while (parent instanceof View) {
            View view = (View) parent;
            p.offset(-view.mScrollX, -view.mScrollY);

            if (!view.hasIdentityMatrix()) {
                view.getMatrix().transform(p);
            }

            p.offset(view.mLeft, view.mTop);
            parent = view.mParent;
        }

        p.round(out);
    }

    /**
     * Finds the first descendant view with the given ID, the view itself if
     * the ID matches {@link #getId()}, or {@code null} if the ID is invalid
     * (< 0) or there is no matching view in the hierarchy.
     *
     * @param id the id to search for
     * @return a view with given id if found, or {@code null} otherwise
     */
    @Nullable
    public final <T extends View> T findViewById(int id) {
        if (id == NO_ID) {
            return null;
        }
        return findViewTraversal(id);
    }

    /**
     * Finds the first descendant view with the given ID, the view itself if the ID
     * matches {@link #getId()}, or results in an error.
     *
     * @param id the ID to search for
     * @return a view with given ID
     * @throws IllegalArgumentException the ID is invalid or there is no matching view
     *                                  in the hierarchy
     * @see View#findViewById(int)
     */
    @Nonnull
    public final <T extends View> T getViewById(int id) {
        T view = findViewById(id);
        if (view == null) {
            throw new IllegalArgumentException("ID does not reference a View inside this View");
        }
        return view;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    <T extends View> T findViewTraversal(int id) {
        if (id == mId) {
            return (T) this;
        }
        return null;
    }

    /*boolean onCursorPosEvent(LinkedList<View> route, double x, double y) {
        if ((mViewFlags & ENABLED_MASK) == DISABLED) {
            return false;
        }
        if (x >= mLeft && x < mRight && y >= mTop && y < mBottom) {
            route.add(this);
            return true;
        }
        return false;
    }*/

    /**
     * Called by a parent to request that a child update its values for mScrollX
     * and mScrollY if necessary. This will typically be done if the child is
     * animating a scroll using a Scroller.
     */
    public void computeScroll() {
    }

    /**
     * Starts a drag and drop operation. This method passes a {@link DragShadow} object to
     * the window system. The system calls {@link DragShadow#onDrawShadow(Canvas)}
     * to draw the drag shadow itself at proper level.
     * <p>
     * Once the system has the drag shadow, it begins the drag and drop operation by sending
     * drag events to all the View objects in all windows that are currently visible. It does
     * this either by calling the View object's drag listener (an implementation of
     * {@link View.OnDragListener#onDrag(View, DragEvent)} or by calling the
     * View object's {@link #onDragEvent(DragEvent)} method.
     * Both are passed a {@link DragEvent} object that has a
     * {@link DragEvent#getAction()} value of {@link DragEvent#ACTION_DRAG_STARTED}.
     *
     * @param localState The data to be transferred by the drag and drop operation. It can be in any form,
     *                   which can not only store the required data, but also let the view identify whether
     *                   it can accept the followed DragEvent.
     * @param shadow     The shadow object to draw the shadow, {@code null} will generate a default shadow.
     * @param flags      Flags, 0 for no flags.
     * @return {@code true} if operation successfully started, {@code false} means the system was
     * unable to start the operation because of another ongoing operation or some other reasons.
     */
    public final boolean startDragAndDrop(@Nullable Object localState, @Nullable DragShadow shadow, int flags) {
        if (mAttachInfo == null) {
            ModernUI.LOGGER.error(MARKER, "startDragAndDrop called out of a window");
            return false;
        }
        return mAttachInfo.mViewRootImpl.startDragAndDrop(this, localState, shadow, flags);
    }

    /**
     * Handle primitive drag event of this view
     *
     * @param event the event to be handled
     * @return {@code true} if the event was consumed by the view, {@code false} otherwise
     */
    //TODO
    public boolean onDragEvent(DragEvent event) {
        return true;
    }

    /**
     * Dispatch a pointer event.
     * <p>
     * Dispatches touch related pointer events to {@link #onTouchEvent(MotionEvent)} and all
     * other events to {@link #onGenericMotionEvent(MotionEvent)}.  This separation of concerns
     * reinforces the invariant that {@link #onTouchEvent(MotionEvent)} is really about touches
     * and should not be expected to handle other pointing device features.
     * </p>
     *
     * @param event the motion event to be dispatched.
     * @return true if the event was handled by the view, false otherwise.
     */
    public final boolean dispatchPointerEvent(MotionEvent event) {
        if (event.isTouchEvent()) {
            return dispatchTouchEvent(event);
        } else {
            return dispatchGenericMotionEvent(event);
        }
    }

    /**
     * Dispatch a generic motion event.
     * <p>
     * Generic motion events with source class pointer are delivered to the view under the
     * pointer.  All other generic motion events are delivered to the focused view.
     * Hover events are handled specially and are delivered to {@link #onHoverEvent(MotionEvent)}.
     * <p>
     * Generally, this method should not be overridden.
     *
     * @param event the event to be dispatched
     * @return {@code true} if the event was consumed by the view, {@code false} otherwise
     */
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        final int action = event.getAction();
        if (action == MotionEvent.ACTION_HOVER_ENTER
                || action == MotionEvent.ACTION_HOVER_MOVE
                || action == MotionEvent.ACTION_HOVER_EXIT) {
            if (dispatchHoverEvent(event)) {
                return true;
            }
        }
        if (dispatchGenericPointerEvent(event)) {
            return true;
        }
        return dispatchGenericMotionEventInternal(event);
    }

    private boolean dispatchGenericMotionEventInternal(MotionEvent event) {
        if (onGenericMotionEvent(event)) {
            return true;
        }
        if ((mViewFlags & ENABLED_MASK) == DISABLED) {
            return false;
        }
        final int action = event.getAction();
        return false;
    }

    /**
     * Dispatch a hover event.
     * <p>
     * Do not call this method directly.
     * Call {@link #dispatchGenericMotionEvent(MotionEvent)} instead.
     * </p>
     *
     * @param event The motion event to be dispatched.
     * @return True if the event was handled by the view, false otherwise.
     */
    protected boolean dispatchHoverEvent(MotionEvent event) {
        return onHoverEvent(event);
    }

    /**
     * Dispatch a generic motion event to the view under the first pointer.
     * <p>
     * Do not call this method directly.
     * Call {@link #dispatchGenericMotionEvent(MotionEvent)} instead.
     * </p>
     *
     * @param event The motion event to be dispatched.
     * @return True if the event was handled by the view, false otherwise.
     */
    protected boolean dispatchGenericPointerEvent(MotionEvent event) {
        return false;
    }

    /**
     * Implement this method to handle generic motion events.
     * <p>
     * Implementations of this method should check if this view ENABLED and CLICKABLE.
     *
     * @param event the generic motion event being processed.
     * @return {@code true} if the event was consumed by the view, {@code false} otherwise
     */
    public boolean onGenericMotionEvent(MotionEvent event) {
        /*final double mouseX = event.getX();
        final double mouseY = event.getY();
        final int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                final boolean prevHovered = (mPrivateFlags & PFLAG_HOVERED) != 0;
                if (mouseX >= mLeft && mouseX < mRight && mouseY >= mTop && mouseY < mBottom) {
                    if (!prevHovered) {
                        mPrivateFlags |= PFLAG_HOVERED;
                        onMouseHoverEnter(mouseX, mouseY);
                    }
                    onMouseHoverMoved(mouseX, mouseY);
                    dispatchGenericMotionEvent(event);
                    return true;
                } else {
                    if (prevHovered) {
                        mPrivateFlags &= ~PFLAG_HOVERED;
                        onMouseHoverExit();
                    }

                }
                return false;
            case MotionEvent.ACTION_PRESS:
                if ((mPrivateFlags & PFLAG_HOVERED) != 0) {
                    if (dispatchGenericMotionEvent(event)) {
                        return true;
                    }
                    event.pressMap.putIfAbsent(event.mActionButton, this);
                    return onMousePressed(mouseX, mouseY, event.mActionButton);
                }
                return false;
            case MotionEvent.ACTION_RELEASE:
                if ((mPrivateFlags & PFLAG_HOVERED) != 0) {
                    if (dispatchGenericMotionEvent(event)) {
                        return true;
                    }
                    boolean handled;
                    handled = onMouseReleased(mouseX, mouseY, event.mActionButton);
                    if (event.pressMap.remove(event.mActionButton) == this) {
                        handled = onMouseClicked(mouseX, mouseY, event.mActionButton);
                        event.clicked = this;
                    }
                    return handled;
                }
                return false;
            case MotionEvent.ACTION_DOUBLE_CLICK:
                if ((mPrivateFlags & PFLAG_HOVERED) != 0) {
                    *//*if (dispatchMouseEvent(event)) {
                        return true;
                    }*//*
                    return onMouseDoubleClicked(mouseX, mouseY);
                }
                return false;
            case MotionEvent.ACTION_SCROLL:
                if ((mPrivateFlags & PFLAG_HOVERED) != 0) {
                    if (dispatchGenericMotionEvent(event)) {
                        return true;
                    }
                    return onMouseScrolled(mouseX, mouseY, event.scrollDelta);
                }
                return false;
        }*/
        return false;
    }

    /**
     * Implement this method to handle hover events.
     * <p>
     * This method is called whenever a pointer is hovering into, over, or out of the
     * bounds of a view and the view is not currently being touched.
     * Hover events are represented as pointer events with action
     * {@link MotionEvent#ACTION_HOVER_ENTER}, {@link MotionEvent#ACTION_HOVER_MOVE},
     * or {@link MotionEvent#ACTION_HOVER_EXIT}.
     * </p>
     * <ul>
     * <li>The view receives a hover event with action {@link MotionEvent#ACTION_HOVER_ENTER}
     * when the pointer enters the bounds of the view.</li>
     * <li>The view receives a hover event with action {@link MotionEvent#ACTION_HOVER_MOVE}
     * when the pointer has already entered the bounds of the view and has moved.</li>
     * <li>The view receives a hover event with action {@link MotionEvent#ACTION_HOVER_EXIT}
     * when the pointer has exited the bounds of the view or when the pointer is
     * about to go down due to a button click, tap, or similar user action that
     * causes the view to be touched.</li>
     * </ul>
     * <p>
     * The view should implement this method to return true to indicate that it is
     * handling the hover event, such as by changing its drawable state.
     * </p><p>
     * The default implementation calls {@link #setHovered} to update the hovered state
     * of the view when a hover enter or hover exit event is received, if the view
     * is enabled and is clickable.  The default implementation also sends hover
     * accessibility events.
     * </p>
     *
     * @param event The motion event that describes the hover.
     * @return True if the view handled the hover event.
     * @see #isHovered
     * @see #setHovered
     * @see #onHoverChanged
     */
    public boolean onHoverEvent(MotionEvent event) {
        final int action = event.getAction();
        // If we consider ourself hoverable, or if we we're already hovered,
        // handle changing state in response to ENTER and EXIT events.
        if (isHoverable() || isHovered()) {
            switch (action) {
                case MotionEvent.ACTION_HOVER_ENTER:
                    setHovered(true);
                    break;
                case MotionEvent.ACTION_HOVER_EXIT:
                    setHovered(false);
                    break;
            }

            // Dispatch the event to onGenericMotionEvent before returning true.
            // This is to provide compatibility with existing applications that
            // handled HOVER_MOVE events in onGenericMotionEvent and that would
            // break because of the new default handling for hoverable views
            // in onHoverEvent.
            // Note that onGenericMotionEvent will be called by default when
            // onHoverEvent returns false (refer to dispatchGenericMotionEvent).
            dispatchGenericMotionEventInternal(event);
            // The event was already handled by calling setHovered(), so always
            // return true.
            return true;
        }
        return false;
    }

    /**
     * Returns true if the view should handle {@link #onHoverEvent}
     * by calling {@link #setHovered} to change its hovered state.
     *
     * @return True if the view is hoverable.
     */
    private boolean isHoverable() {
        final int viewFlags = mViewFlags;
        if ((viewFlags & ENABLED_MASK) == DISABLED) {
            return false;
        }
        return (viewFlags & CLICKABLE) == CLICKABLE;
    }

    /**
     * Returns true if the view is currently hovered.
     *
     * @return True if the view is currently hovered.
     * @see #setHovered
     * @see #onHoverChanged
     */
    public boolean isHovered() {
        return (mPrivateFlags & PFLAG_HOVERED) != 0;
    }

    /**
     * Sets whether the view is currently hovered.
     * <p>
     * Calling this method also changes the drawable state of the view.  This
     * enables the view to react to hover by using different drawable resources
     * to change its appearance.
     * </p><p>
     * The {@link #onHoverChanged} method is called when the hovered state changes.
     * </p>
     *
     * @param hovered True if the view is hovered.
     * @see #isHovered
     * @see #onHoverChanged
     */
    public void setHovered(boolean hovered) {
        if (hovered) {
            if ((mPrivateFlags & PFLAG_HOVERED) == 0) {
                mPrivateFlags |= PFLAG_HOVERED;
                refreshDrawableState();
                onHoverChanged(true);
            }
        } else {
            if ((mPrivateFlags & PFLAG_HOVERED) != 0) {
                mPrivateFlags &= ~PFLAG_HOVERED;
                refreshDrawableState();
                onHoverChanged(false);
            }
        }
    }

    /**
     * Implement this method to handle hover state changes.
     * <p>
     * This method is called whenever the hover state changes as a result of a
     * call to {@link #setHovered}.
     * </p>
     *
     * @param hovered The current hover state, as returned by {@link #isHovered}.
     * @see #isHovered
     * @see #setHovered
     */
    public void onHoverChanged(boolean hovered) {

    }

    /*
     * Internal method. Check if mouse hover this view.
     *
     * @param mouseX relative mouse X pos
     * @param mouseY relative mouse Y pos
     * @return return {@code true} if certain view hovered
     */
    /*@Deprecated
    final boolean updateMouseHover(double mouseX, double mouseY) {
        final boolean prevHovered = (privateFlags & PFLAG_HOVERED) != 0;
        if (mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom) {
            if (handleScrollBarsHover(mouseX, mouseY)) {
                UIManager.getInstance().setHovered(this);
                return true;
            }
            if (!prevHovered) {
                privateFlags |= PFLAG_HOVERED;
                onMouseHoverEnter(mouseX, mouseY);
            }
            onMouseHoverMoved(mouseX, mouseY);
            if (!dispatchUpdateMouseHover(mouseX, mouseY)) {
                UIManager.getInstance().setHovered(this);
            }
            return true;
        } else {
            if (prevHovered) {
                privateFlags &= ~PFLAG_HOVERED;
                onMouseHoverExit();
            }
        }
        return false;
    }

    private boolean handleScrollBarsHover(double mouseX, double mouseY) {
        ScrollBar scrollBar = verticalScrollBar;
        if (scrollBar != null && scrollBar.updateMouseHover(mouseX, mouseY)) {
            return true;
        }
        scrollBar = horizontalScrollBar;
        return scrollBar != null && scrollBar.updateMouseHover(mouseX, mouseY);
    }*/

    /*
     * Internal method. Dispatch events to child views if present,
     * check if mouse hovered a child view.
     *
     * @param mouseX relative mouse X pos
     * @param mouseY relative mouse Y pos
     * @return return {@code true} if certain child view hovered
     */
    /*boolean dispatchUpdateMouseHover(double mouseX, double mouseY) {
        return false;
    }*/

    /*void ensureMouseHoverExit() {
        if ((mPrivateFlags & PFLAG_HOVERED) != 0) {
            mPrivateFlags &= ~PFLAG_HOVERED;
            onMouseHoverExit();
        }
    }*/

    /**
     * Returns whether this view can receive pointer events.
     *
     * @return {@code true} if this view can receive pointer events.
     */
    protected boolean canReceivePointerEvents() {
        return (mViewFlags & VISIBILITY_MASK) == VISIBLE;
    }

    /**
     * Pass the touch screen motion event down to the target view, or this view if
     * it is the target.
     *
     * @param event The motion event to be dispatched.
     * @return {@code true} if the event was handled by the view, {@code false} otherwise
     */
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean handled = false;

        final int actionMasked = event.getActionMasked();

        if (onTouchEvent(event)) {
            handled = true;
        }

        return handled;
    }

    /**
     * Implement this method to handle touch screen motion events.
     *
     * @param event the touch event
     * @return {@code true} if the event was handled by the view, {@code false} otherwise
     */
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    boolean isOnScrollbarThumb(float x, float y) {
        return false;
    }

    /*
     * Returns true if the view is currently mouse hovered
     *
     * @return {@code true} if the view is currently mouse hovered
     */
    /*public final boolean isMouseHovered() {
        return (mPrivateFlags & PFLAG_HOVERED) != 0;
    }*/

    /*
     * Called when mouse start to hover on this view
     *
     * @param mouseX relative mouse x pos
     * @param mouseY relative mouse y pos
     */
    /*protected void onMouseHoverEnter(double mouseX, double mouseY) {

    }*/

    /*
     * Called when mouse hovered on this view and moved
     * Also called at the same time as onMouseHoverEnter()
     *
     * @param mouseX relative mouse x pos
     * @param mouseY relative mouse y pos
     */
    /*protected void onMouseHoverMoved(double mouseX, double mouseY) {

    }*/

    /*
     * Called when mouse no longer hover on this view
     */
    /*protected void onMouseHoverExit() {

    }*/

    /*
     * Internal method. Called when mouse hovered on this view and a mouse button clicked.
     *
     * @param mouseX      relative mouse x pos
     * @param mouseY      relative mouse y pos
     * @param mouseButton mouse button, for example {@link GLFW#GLFW_MOUSE_BUTTON_LEFT}
     * @return {@code true} if action performed
     */
    /*final boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        ScrollBar scrollBar = verticalScrollBar;
        if (scrollBar != null && scrollBar.onMouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        scrollBar = horizontalScrollBar;
        if (scrollBar != null && scrollBar.onMouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        return onMouseClicked(mouseX, mouseY, mouseButton);
    }*/

    /**
     * Determines whether the given point, in local coordinates is inside the view.
     */
    final boolean pointInView(float localX, float localY) {
        return pointInView(localX, localY, 0);
    }

    /**
     * Utility method to determine whether the given point, in local coordinates,
     * is inside the view, where the area of the view is expanded by the slop factor.
     * This method is called while processing touch-move events to determine if the event
     * is still within the view.
     */
    boolean pointInView(float localX, float localY, float slop) {
        return localX >= -slop && localY >= -slop && localX < ((mRight - mLeft) + slop) &&
                localY < ((mBottom - mTop) + slop);
    }

    /**
     * Called when mouse hovered on this view and a mouse button pressed.
     *
     * @param mouseX      relative mouse x pos
     * @param mouseY      relative mouse y pos
     * @param mouseButton mouse button, for example {@link GLFW#GLFW_MOUSE_BUTTON_LEFT}
     * @return {@code true} if action performed
     * @see GLFW
     */
    protected boolean onMousePressed(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    /**
     * Called when mouse hovered on this view and a mouse button released.
     *
     * @param mouseX      relative mouse x pos
     * @param mouseY      relative mouse y pos
     * @param mouseButton mouse button, for example {@link GLFW#GLFW_MOUSE_BUTTON_LEFT}
     * @return {@code true} if action performed
     * @see GLFW
     */
    protected boolean onMouseReleased(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    /**
     * Called when mouse pressed and released on this view with the mouse button.
     * Called after onMouseReleased no matter whether it's consumed or not.
     *
     * @param mouseX      relative mouse x pos
     * @param mouseY      relative mouse y pos
     * @param mouseButton mouse button, for example {@link GLFW#GLFW_MOUSE_BUTTON_LEFT}
     * @return {@code true} if action performed
     * @see GLFW
     */
    protected boolean onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    /**
     * Called when mouse hovered on this view and left button double clicked.
     * If no action performed in this method, onMousePressed will be called.
     *
     * @param mouseX relative mouse x pos
     * @param mouseY relative mouse y pos
     * @return {@code true} if action performed
     */
    protected boolean onMouseDoubleClicked(double mouseX, double mouseY) {
        return false;
    }

    /**
     * Called when mouse hovered on this view and mouse scrolled.
     *
     * @param mouseX relative mouse x pos
     * @param mouseY relative mouse y pos
     * @param amount scroll amount
     * @return return {@code true} if action performed
     */
    protected boolean onMouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }

    /**
     * Call when this view start to be listened as a draggable.
     */
    protected void onStartDragging() {

    }

    /**
     * Call when this view is no longer listened as a draggable.
     */
    protected void onStopDragging() {

    }

    /**
     * Called when mouse moved and dragged.
     *
     * @param mouseX relative mouse x pos
     * @param mouseY relative mouse y pos
     * @param deltaX mouse x pos change
     * @param deltaY mouse y pos change
     * @return {@code true} if action performed
     */
    protected boolean onMouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        return false;
    }

    /**
     * Call when this view start to be listened as a keyboard listener.
     */
    protected void onStartKeyboard() {

    }

    /**
     * Call when this view is no longer listened as a keyboard listener.
     */
    protected void onStopKeyboard() {

    }

    /**
     * Called when a key pressed.
     *
     * @param keyCode   see {@link GLFW}, for example {@link GLFW#GLFW_KEY_W}
     * @param scanCode  keyboard scan code, seldom used
     * @param modifiers modifier key
     * @return return {@code true} if action performed
     */
    protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /**
     * Called when a key released.
     *
     * @param keyCode   see {@link GLFW}, for example {@link GLFW#GLFW_KEY_W}
     * @param scanCode  keyboard scan code, seldom used
     * @param modifiers modifier key
     * @return return {@code true} if action performed
     */
    protected boolean onKeyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /**
     * Called when a unicode character typed.
     *
     * @param codePoint char code
     * @param modifiers modifier key
     * @return return {@code true} if action performed
     */
    protected boolean onCharTyped(char codePoint, int modifiers) {
        return false;
    }

    /**
     * Called when click on scrollbar track and not on the thumb
     * and there was an scroll amount change
     *
     * @param vertical    {@code true} if scrollbar is vertical, horizontal otherwise
     * @param scrollDelta scroll amount change calculated by scrollbar
     */
    protected void onScrollBarClicked(boolean vertical, float scrollDelta) {

    }

    /**
     * Called when drag the scroll thumb and there was an scroll
     * amount change
     *
     * @param vertical    {@code true} if scrollbar is vertical, horizontal otherwise
     * @param scrollDelta scroll amount change calculated by scrollbar
     */
    protected void onScrollBarDragged(boolean vertical, float scrollDelta) {

    }

    /*
     * Layout scroll bars if enabled
     */
    /*private void layoutScrollBars() {
        ScrollBar scrollBar = verticalScrollBar;
        if (scrollBar != null) {
            int thickness = scrollBar.getSize();
            int r = mRight - scrollBar.getRightPadding();
            int l = Math.max(r - thickness, mLeft);
            int t = mTop + scrollBar.getTopPadding();
            int b = mBottom - scrollBar.getBottomPadding();
            scrollBar.setFrame(l, t, r, b);
        }
        scrollBar = horizontalScrollBar;
        if (scrollBar != null) {
            int thickness = scrollBar.getSize();
            int b = mBottom - scrollBar.getBottomPadding();
            int t = Math.max(b - thickness, mTop);
            int l = mLeft + scrollBar.getLeftPadding();
            int r = mRight - scrollBar.getRightPadding();
            if (isVerticalScrollBarEnabled()) {
                r -= verticalScrollBar.getWidth();
            }
            scrollBar.setFrame(l, t, r, b);
        }
    }*/

    /**
     * Creates an image that the system displays during the drag and drop operation.
     */
    public static class DragShadow {

        private final WeakReference<View> viewRef;

        public DragShadow(View view) {
            viewRef = new WeakReference<>(view);
        }

        /**
         * Construct a shadow builder object with no associated View. This
         * constructor variant is only useful when the {@link #onProvideShadowCenter(Point)}}
         * and {@link #onDrawShadow(Canvas)} methods are also overridden in order
         * to supply the drag shadow's dimensions and appearance without
         * reference to any View object.
         */
        public DragShadow() {
            viewRef = new WeakReference<>(null);
        }

        @Nullable
        public final View getView() {
            return viewRef.get();
        }

        /**
         * Called when the view is not mouse hovered or shadow is nonnull, to determine
         * where the mouse cursor position is in the shadow.
         *
         * @param outShadowCenter the center point in the shadow
         */
        public void onProvideShadowCenter(@Nonnull Point outShadowCenter) {

        }

        /**
         * Draw the shadow.
         *
         * @param canvas canvas to draw content
         */
        public void onDrawShadow(@Nonnull Canvas canvas) {
            View view = viewRef.get();
            if (view != null) {
                view.onDraw(canvas);
            } else {
                ModernUI.LOGGER.error(MARKER, "No view found on draw shadow");
            }
        }
    }

    @FunctionalInterface
    public interface OnHoverListener {

        void onHover(View v, MotionEvent event);
    }
}
