/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.ui.master;

import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.math.Point;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.ui.layout.MeasureSpec;
import icyllis.modernui.ui.widget.ScrollController;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * View is the basic component of UI. View has its own rectangular area on screen,
 * which is also responsible for drawing and event handling.
 *
 * @since 2.0
 */
@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
public class View {

    /**
     * @see #generateViewId()
     */
    private static final AtomicInteger GENERATED_ID = new AtomicInteger(1);

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
     * |--------|--------|--------|--------|
     *                                    1  PFLAG_HOVERED
     * |--------|--------|--------|--------|
     *                        1              PFLAG_MEASURED_DIMENSION_SET
     *                       1               PFLAG_FORCE_LAYOUT
     *                      1                PFLAG_LAYOUT_REQUIRED
     * |--------|--------|--------|--------|
     */
    static final int PFLAG_HOVERED                = 1;
    static final int PFLAG_MEASURED_DIMENSION_SET = 1 << 11;
    static final int PFLAG_FORCE_LAYOUT           = 1 << 12;
    static final int PFLAG_LAYOUT_REQUIRED        = 1 << 13;

    /**
     * Private status flags for different stages or processes
     * Internal use only
     */
    int privateFlags;

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

    /*
     * View masks
     * |--------|--------|--------|--------|
     *                                   11  VISIBILITY
     *                                  1    FOCUSABLE
     *                                 1     ENABLED
     *                              11       SCROLLBARS
     * |--------|--------|--------|--------|
     *
     * |--------|--------|--------|--------|
     */
    /**
     * View flags
     * {@link #setStateFlag(int, int)}
     */
    int viewFlags;

    /**
     * Parent view of this view
     * {@link #assignParent(IViewParent)}
     */
    private IViewParent parent;

    /**
     * Internal use
     */
    ViewRootImpl viewRoot;

    /**
     * View id to identify this view in UI hierarchy
     * {@link #getId()}
     * {@link #setId(int)}
     */
    private int id = NO_ID;

    /**
     * View left on screen
     * {@link #getLeft()}
     */
    private int left;

    /**
     * View top on screen
     * {@link #getTop()}
     */
    private int top;

    /**
     * View right on screen
     * {@link #getRight()}
     */
    private int right;

    /**
     * View bottom on screen
     * {@link #getBottom()}
     */
    private int bottom;

    //private boolean listening = true;

    private int minWidth;
    private int minHeight;

    /**
     * Cached previous measure spec to avoid unnecessary measurements
     */
    private int prevWidthMeasureSpec  = Integer.MIN_VALUE;
    private int prevHeightMeasureSpec = Integer.MIN_VALUE;

    /**
     * The measurement result in onMeasure(), used to layout
     */
    private int measuredWidth;
    private int measuredHeight;

    /**
     * Scrollbars
     */
    @Nullable
    private ScrollBar horizontalScrollBar;
    @Nullable
    private ScrollBar verticalScrollBar;

    /**
     * The layout parameters associated with this view and used by the parent
     * {@link ViewGroup} to determine how this view should be laid out.
     */
    private ViewGroup.LayoutParams layoutParams;

    /**
     * Raw draw method, do not override this
     *
     * @param canvas canvas to draw content
     */
    public void draw(@Nonnull Canvas canvas) {
        if ((viewFlags & VISIBILITY_MASK) == 0) {

            onDraw(canvas);

            dispatchDraw(canvas);

            //TODO Draw scrollbars
            if (verticalScrollBar != null) {
                verticalScrollBar.draw(canvas);
            }
        }
    }

    /**
     * Draw this view if visible
     * Before you draw in the method, you have to call {@link Canvas#moveTo(View)},
     * (0, 0) will be the top left of the bounds,
     * (width, height) will be the bottom right of the bounds.
     * See {@link #getWidth()}
     * See {@link #getHeight()}
     *
     * @param canvas canvas to draw content
     */
    protected void onDraw(@Nonnull Canvas canvas) {

    }

    /**
     * Draw child views if visible
     *
     * @param canvas canvas to draw content
     */
    protected void dispatchDraw(@Nonnull Canvas canvas) {

    }

    /**
     * Called from client thread every tick on pre-tick, to update or cache something
     *
     * @param ticks elapsed ticks from a gui open, 20 tick = 1 second
     */
    protected void tick(int ticks) {

    }

    /**
     * Assign rect area of this view and all descendants
     * <p>
     * Derived classes should NOT override this method for any reason
     * Derived classes with children should override onLayout()
     * In that method, they should call layout() on each of their children
     *
     * @param left   left position, relative to game window
     * @param top    top position, relative to game window
     * @param right  right position, relative to game window
     * @param bottom bottom position, relative to game window
     */
    public void layout(int left, int top, int right, int bottom) {
        boolean changed = setFrame(left, top, right, bottom);

        if (changed || (privateFlags & PFLAG_LAYOUT_REQUIRED) != 0) {
            layoutScrollBars();

            onLayout(changed);

            privateFlags &= ~PFLAG_LAYOUT_REQUIRED;
        }

        privateFlags &= ~PFLAG_FORCE_LAYOUT;
    }

    /**
     * Layout scroll bars if enabled
     */
    private void layoutScrollBars() {
        ScrollBar scrollBar = verticalScrollBar;
        if (scrollBar != null) {
            int thickness = scrollBar.getSize();
            int r = right - scrollBar.getRightPadding();
            int l = Math.max(r - thickness, left);
            int t = top + scrollBar.getTopPadding();
            int b = bottom - scrollBar.getBottomPadding();
            scrollBar.setFrame(l, t, r, b);
        }
        scrollBar = horizontalScrollBar;
        if (scrollBar != null) {
            int thickness = scrollBar.getSize();
            int b = bottom - scrollBar.getBottomPadding();
            int t = Math.max(b - thickness, top);
            int l = left + scrollBar.getLeftPadding();
            int r = right - scrollBar.getRightPadding();
            if (isVerticalScrollBarEnabled()) {
                r -= verticalScrollBar.getWidth();
            }
            scrollBar.setFrame(l, t, r, b);
        }
    }

    /**
     * Layout child views, call from {@link #layout(int, int, int, int)}
     *
     * @param changed whether the size or position of this view was changed
     */
    protected void onLayout(boolean changed) {

    }

    /**
     * Assign the rect area of this view, called from layout()
     *
     * @param l left position, relative to game window
     * @param t top position, relative to game window
     * @param r right position, relative to game window
     * @param b bottom position, relative to game window
     * @return whether the rect area of this view was changed
     */
    protected boolean setFrame(int l, int t, int r, int b) {
        if (left != l || right != r || top != t || bottom != b) {

            int oldWidth = getWidth();
            int oldHeight = getHeight();

            left = l;
            top = t;
            right = r;
            bottom = b;

            int newWidth = getWidth();
            int newHeight = getHeight();

            boolean sizeChanged = (newWidth != oldWidth) || (newHeight != oldHeight);

            if (sizeChanged) {
                onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
            }
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
     * Called by parent to calculate the size of this view.
     * This method is used to post the onMeasure event,
     * and the measurement result is performed in {@link #onMeasure(int, int)}
     *
     * @param widthMeasureSpec  width measure specification imposed by the parent
     * @param heightMeasureSpec height measure specification imposed by the parent
     * @throws IllegalStateException measurement result is not set in
     *                               {@link #onMeasure(int, int)}
     * @see #onMeasure(int, int)
     */
    public final void measure(int widthMeasureSpec, int heightMeasureSpec) {

        boolean needsLayout = (privateFlags & PFLAG_FORCE_LAYOUT) != 0;

        if (!needsLayout) {

            boolean specChanged =
                    widthMeasureSpec != prevWidthMeasureSpec
                            || heightMeasureSpec != prevHeightMeasureSpec;
            boolean isSpecExactly =
                    MeasureSpec.getMode(widthMeasureSpec).isExactly()
                            && MeasureSpec.getMode(heightMeasureSpec).isExactly();
            boolean matchesSpecSize =
                    measuredWidth == MeasureSpec.getSize(widthMeasureSpec)
                            && measuredHeight == MeasureSpec.getSize(heightMeasureSpec);
            needsLayout = specChanged
                    && (!isSpecExactly || !matchesSpecSize);
        }

        if (needsLayout) {
            // remove the flag first anyway
            privateFlags &= ~PFLAG_MEASURED_DIMENSION_SET;

            onMeasure(widthMeasureSpec, heightMeasureSpec);

            // the flag should be added in onMeasure() by calling setMeasuredDimension()
            if ((privateFlags & PFLAG_MEASURED_DIMENSION_SET) == 0) {
                throw new IllegalStateException("Measured dimension unspecified on measure");
            }

            privateFlags |= PFLAG_LAYOUT_REQUIRED;
        }

        prevWidthMeasureSpec = widthMeasureSpec;
        prevHeightMeasureSpec = heightMeasureSpec;
    }

    /**
     * Measure this view and should be override and shouldn't call super.onMeasure()
     * You must call {@link #setMeasuredDimension(int, int)} to set measurement result
     *
     * @param widthMeasureSpec  width measure specification imposed by the parent
     *                          {@link MeasureSpec}
     * @param heightMeasureSpec height measure specification imposed by the parent
     *                          {@link MeasureSpec}
     */
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getDefaultSize(minWidth, widthMeasureSpec),
                getDefaultSize(minHeight, heightMeasureSpec));
    }

    /**
     * Set measurement result, should be called on {@link #onMeasure(int, int)}
     *
     * @param measuredWidth  measured width of this view
     * @param measuredHeight measured height of this view
     */
    protected final void setMeasuredDimension(int measuredWidth, int measuredHeight) {
        this.measuredWidth = measuredWidth;
        this.measuredHeight = measuredHeight;

        privateFlags |= PFLAG_MEASURED_DIMENSION_SET;
    }

    /**
     * Get measured width information
     * This should be used during measurement and layout calculations only.
     * Use {@link #getWidth()} to get the width of this view after layout.
     *
     * @return measured width of this view
     */
    public final int getMeasuredWidth() {
        return measuredWidth;
    }

    /**
     * Get measured height information
     * This should be used during measurement and layout calculations only.
     * Use {@link #getHeight()} to get the height of this view after layout.
     *
     * @return measured height of this view
     */
    public final int getMeasuredHeight() {
        return measuredHeight;
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
        return layoutParams;
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
        layoutParams = params;
        requestLayout();
    }

    public int getMinWidth() {
        return minWidth;
    }

    public int getMinHeight() {
        return minHeight;
    }

    /**
     * Helper method to get default size
     *
     * @param size        default size
     * @param measureSpec measure specification
     * @return measured size
     */
    public static int getDefaultSize(int size, int measureSpec) {

        switch (MeasureSpec.getMode(measureSpec)) {
            case UNSPECIFIED:
                break;
            case AT_MOST:
            case EXACTLY:
                return MeasureSpec.getSize(measureSpec);
        }

        return size;
    }

    public static int resolveSize(int size, int measureSpec) {
        int specSize = MeasureSpec.getSize(measureSpec);
        int result;
        switch (MeasureSpec.getMode(measureSpec)) {
            case AT_MOST:
                result = Math.min(specSize, size);
                break;
            case EXACTLY:
                result = specSize;
                break;
            default:
                result = size;
        }
        return result;
    }

    /**
     * Get the parent of this view
     *
     * @return parent of this view
     */
    @Nullable
    public final IViewParent getParent() {
        return parent;
    }

    /**
     * Assign parent view, this method is called by system
     * Internal method, derived classes should NOT override
     * or call this method for any reason
     *
     * @param parent parent view
     * @throws RuntimeException parent is already assigned
     */
    final void assignParent(@Nonnull IViewParent parent) {
        if (this.parent == null) {
            this.parent = parent;
        } else {
            throw new RuntimeException("Parent of view " + this + " has been assigned");
        }
    }

    /**
     * Get the ID of this view
     *
     * @return view id
     */
    public int getId() {
        return id;
    }

    /**
     * ID should not be repeated in the same view group and a positive number
     *
     * @param id view id
     */
    public void setId(int id) {
        if (id == NO_ID) {
            this.id = generateViewId();
        } else {
            this.id = id;
        }
    }

    /**
     * Generate next view identifier, multi-threaded
     *
     * @return generated id
     */
    public static int generateViewId() {
        for (; ; ) {
            int cur = GENERATED_ID.get();
            int next = cur + 1;
            if (next < 1) {
                next = 1;
            }
            if (GENERATED_ID.compareAndSet(cur, next)) {
                return cur;
            }
        }
    }

    //TODO state switching events
    void setStateFlag(int flag, int mask) {
        final int old = viewFlags;

        viewFlags = (viewFlags & ~mask) | (flag & mask);

        final int change = viewFlags ^ old;

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
        return viewFlags & VISIBILITY_MASK;
    }

    /**
     * Define whether the horizontal scrollbar should have or not.
     * The scrollbar is null by default.
     *
     * @param enabled true if the horizontal scrollbar should be enabled
     * @see #isHorizontalScrollBarEnabled()
     * @see #setHorizontalScrollBar(ScrollBar)
     */
    public final void setHorizontalScrollBarEnabled(boolean enabled) {
        if (isHorizontalScrollBarEnabled() != enabled) {
            viewFlags ^= SCROLLBARS_HORIZONTAL;
        }
    }

    /**
     * Define whether the vertical scrollbar should have or not.
     * The scrollbar is null by default.
     *
     * @param enabled true if the vertical scrollbar should be enabled
     * @see #isVerticalScrollBarEnabled()
     * @see #setVerticalScrollBar(ScrollBar)
     */
    public final void setVerticalScrollBarEnabled(boolean enabled) {
        if (isVerticalScrollBarEnabled() != enabled) {
            viewFlags ^= SCROLLBARS_VERTICAL;
        }
    }

    /**
     * Indicate whether the horizontal scrollbar should have or not.
     * The scrollbar is null by default.
     *
     * @return true if the horizontal scrollbar already created, false otherwise
     * @see #setHorizontalScrollBarEnabled(boolean)
     * @see #setHorizontalScrollBar(ScrollBar)
     */
    public final boolean isHorizontalScrollBarEnabled() {
        return (viewFlags & SCROLLBARS_HORIZONTAL) != 0;
    }

    /**
     * Indicate whether the vertical scrollbar should have or not.
     * The scrollbar is null by default.
     *
     * @return true if the vertical scrollbar already created, false otherwise
     * @see #setVerticalScrollBarEnabled(boolean)
     * @see #setVerticalScrollBar(ScrollBar)
     */
    public final boolean isVerticalScrollBarEnabled() {
        return (viewFlags & SCROLLBARS_VERTICAL) != 0;
    }

    /**
     * Set horizontal scrollbar, ensure it's nonnull if enabled
     *
     * @param scrollBar scrollbar
     * @see #setHorizontalScrollBarEnabled(boolean)
     * @see #isHorizontalScrollBarEnabled()
     */
    public final void setHorizontalScrollBar(@Nullable ScrollBar scrollBar) {
        if (horizontalScrollBar != scrollBar) {
            horizontalScrollBar = scrollBar;
        }
    }

    /**
     * Set vertical scrollbar, ensure it's nonnull if enabled
     *
     * @param scrollBar scrollbar
     * @see #setVerticalScrollBarEnabled(boolean)
     * @see #isVerticalScrollBarEnabled()
     */
    public final void setVerticalScrollBar(@Nullable ScrollBar scrollBar) {
        if (verticalScrollBar != scrollBar) {
            verticalScrollBar = scrollBar;
            if (scrollBar != null) {
                scrollBar.flags |= ScrollBar.VERTICAL;
            }
        }
    }

    /**
     * Get horizontal scrollbar if set
     *
     * @return scrollbar
     * @see #setHorizontalScrollBar(ScrollBar)
     */
    @Nullable
    public final ScrollBar getHorizontalScrollBar() {
        return horizontalScrollBar;
    }

    /**
     * Get vertical scrollbar if set
     *
     * @return scrollbar
     * @see #setVerticalScrollBar(ScrollBar)
     */
    @Nullable
    public final ScrollBar getVerticalScrollBar() {
        return verticalScrollBar;
    }

    /**
     * Get view current layout width
     *
     * @return width
     */
    public final int getWidth() {
        return right - left;
    }

    /**
     * Get view current layout height
     *
     * @return height
     */
    public final int getHeight() {
        return bottom - top;
    }

    /**
     * Get view logic left position on screen.
     * The sum of scroll amounts of all parent views are not counted.
     *
     * @return left
     */
    public final int getLeft() {
        return left;
    }

    /**
     * Get view logic top position on screen.
     * The sum of scroll amounts of all parent views are not counted.
     *
     * @return top
     */
    public final int getTop() {
        return top;
    }

    /**
     * Get view logic right position on screen.
     * The sum of scroll amounts of all parent views are not counted.
     *
     * @return right
     */
    public final int getRight() {
        return right;
    }

    /**
     * Get view logic bottom position on screen.
     * The sum of scroll amounts of all parent views are not counted.
     *
     * @return bottom
     */
    public final int getBottom() {
        return bottom;
    }

    /*public final void setListening(boolean listening) {
        if (this.listening != listening) {
            this.listening = listening;
            onListeningChanged(listening);
        }
    }

    public final boolean isListening() {
        return listening;
    }

    protected void onListeningChanged(boolean listening) {

    }

    protected void onVisibleChanged(boolean visible) {

    }*/

    void dispatchAttachedToWindow(ViewRootImpl viewRoot) {
        this.viewRoot = viewRoot;
    }

    /**
     * Request layout if layout information changed.
     * This will schedule a layout pass of the view tree.
     */
    public void requestLayout() {
        boolean requestParent = (privateFlags & PFLAG_FORCE_LAYOUT) == 0;

        privateFlags |= PFLAG_FORCE_LAYOUT;

        if (requestParent && parent != null) {
            parent.requestLayout();
        }
    }

    /**
     * Add a mark to force this view to be laid out during the next
     * layout pass.
     */
    public void forceLayout() {
        privateFlags |= PFLAG_FORCE_LAYOUT;
    }

    /**
     * Computes the coordinates of this view in its window. The argument
     * must be an array of two integers. After the method returns, the array
     * contains the x and y location in that order.
     *
     * @param location an array of two integers in which to hold the coordinates
     */
    public void getLocationInWindow(@Nonnull int[] location) {
        if (location.length < 2) {
            throw new IllegalArgumentException("Location array length must be two at least");
        }

        float x = left;
        float y = top;

        IViewParent parent = this.parent;
        while (parent != null) {
            x -= parent.getScrollX();
            y -= parent.getScrollY();
            parent = parent.getParent();
        }

        location[0] = (int) x;
        location[1] = (int) y;
    }

    /**
     * Finds the first descendant view with the given ID, the view itself if
     * the ID matches {@link #getId()}, or {@code null} if the ID is invalid
     * (< 0) or there is no matching view in the hierarchy.
     * <p>
     * <strong>Note:</strong> In most cases -- depending on compiler support --
     * the resulting view is automatically cast to the target class type. If
     * the target class type is unconstrained, an explicit cast may be
     * necessary.
     *
     * @param id the ID to search for
     * @return a view with given ID if found, or {@code null} otherwise
     */
    @Nullable
    public final <T extends View> T findViewById(int id) {
        if (id == NO_ID) {
            return null;
        }
        return findViewTraversal(id);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    <T extends View> T findViewTraversal(int id) {
        if (id == this.id) {
            return (T) this;
        }
        return null;
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
     * @param data   The data to be transferred by the drag and drop operation. It can be in any form,
     *               which can not only store the required data, but also let the view identify whether
     *               it can accept the followed DragEvent.
     * @param shadow The shadow object to draw the shadow, {@code null} will generate a default shadow.
     * @param flags  Flags, 0 for no flags.
     * @return {@code true} if operation successfully started, {@code false} means the system was
     * unable to start the operation because of another ongoing operation or some other reasons.
     */
    public final boolean startDragAndDrop(@Nullable DragData data, @Nullable DragShadow shadow, int flags) {
        if (viewRoot == null) {
            ModernUI.LOGGER.error(MARKER, "startDragAndDrop called out of a window");
            return false;
        }
        return viewRoot.startDragAndDrop(this, data, shadow, flags);
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
     * Handle primitive mouse event of this view
     *
     * @param event the event to be handled
     * @return {@code true} if the event was consumed by the view, {@code false} otherwise
     */
    public final boolean onMouseEvent(@Nonnull MouseEvent event) {
        if ((viewFlags & ENABLED_MASK) == DISABLED) {
            return false;
        }

        final double mouseX = event.x;
        final double mouseY = event.y;
        final int action = event.action;

        switch (action) {
            case MouseEvent.ACTION_MOVE:
                final boolean prevHovered = (privateFlags & PFLAG_HOVERED) != 0;
                if (mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom) {
                    if (!prevHovered) {
                        privateFlags |= PFLAG_HOVERED;
                        onMouseHoverEnter(mouseX, mouseY);
                    }
                    onMouseHoverMoved(mouseX, mouseY);
                    dispatchMouseEvent(event);
                    return true;
                } else {
                    if (prevHovered) {
                        privateFlags &= ~PFLAG_HOVERED;
                        onMouseHoverExit();
                    }
                }
                return false;
            case MouseEvent.ACTION_PRESS:
                if ((privateFlags & PFLAG_HOVERED) != 0) {
                    if (dispatchMouseEvent(event)) {
                        return true;
                    }
                    return onMousePressed(event.x, event.y, event.button);
                }
                return false;
            case MouseEvent.ACTION_SCROLL:
                if ((privateFlags & PFLAG_HOVERED) != 0) {
                    if (dispatchMouseEvent(event)) {
                        return true;
                    }
                    return onMouseScrolled(event.x, event.y, event.scrollDelta);
                }
                return false;
        }
        return false;
    }

    /**
     * Pass the mouse event down to the target view, or this view if it is the target.
     *
     * @param event the event to be dispatched
     * @return {@code true} if the event was consumed by the view, {@code false} otherwise
     */
    public boolean dispatchMouseEvent(@Nonnull MouseEvent event) {
        return false;
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

    /**
     * Internal method. Ensure rest of views of other branches to hover exit.
     */
    void ensureMouseHoverExit() {
        if ((privateFlags & PFLAG_HOVERED) != 0) {
            privateFlags &= ~PFLAG_HOVERED;
            onMouseHoverExit();
        }
    }

    /**
     * Returns true if the view is currently mouse hovered
     *
     * @return {@code true} if the view is currently mouse hovered
     */
    public final boolean isMouseHovered() {
        return (privateFlags & PFLAG_HOVERED) != 0;
    }

    /**
     * Called when mouse start to hover on this view
     *
     * @param mouseX relative mouse x pos
     * @param mouseY relative mouse y pos
     */
    protected void onMouseHoverEnter(double mouseX, double mouseY) {

    }

    /**
     * Called when mouse hovered on this view and moved
     * Also called at the same time as onMouseHoverEnter()
     *
     * @param mouseX relative mouse x pos
     * @param mouseY relative mouse y pos
     */
    protected void onMouseHoverMoved(double mouseX, double mouseY) {

    }

    /**
     * Called when mouse no longer hover on this view
     */
    protected void onMouseHoverExit() {

    }

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
     */
    protected boolean onMouseReleased(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    /**
     * Called when mouse hovered on this view and a mouse button clicked.
     *
     * @param mouseX      relative mouse x pos
     * @param mouseY      relative mouse y pos
     * @param mouseButton mouse button, for example {@link GLFW#GLFW_MOUSE_BUTTON_LEFT}
     * @return {@code true} if action performed
     */
    protected boolean onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    /**
     * Called when mouse hovered on this view and left button double clicked.
     * If no action performed in this method, onMouseClicked will be called.
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
     * @param modifiers modifier key, for example {@link GLFW#GLFW_MOD_CONTROL}
     *                  Actually you need {@link Screen#hasControlDown()},
     *                  {@link Screen#hasShiftDown()}, {@link Screen#hasAltDown()}
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
     * @param modifiers modifier key, for example {@link GLFW#GLFW_MOD_CONTROL}.
     *                  Actually you need {@link Screen#hasControlDown()},
     *                  {@link Screen#hasShiftDown()}, {@link Screen#hasAltDown()}
     * @return return {@code true} if action performed
     */
    protected boolean onKeyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /**
     * Called when a unicode character typed.
     *
     * @param codePoint char code
     * @param modifiers modifier key, for example {@link GLFW#GLFW_MOD_CONTROL}
     *                  Actually you need {@link Screen#hasControlDown()},
     *                  {@link Screen#hasShiftDown()}, {@link Screen#hasAltDown()}
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

    /**
     * This class encapsulated methods to handle events of and draw the scroll bar.
     * Scrollbar is integrated in the view it's created.
     * To control the scroll amount, use {@link ScrollController}
     *
     * @since 1.6
     */
    public class ScrollBar {

        // scrollbar masks
        private static final int DRAW_TRACK        = 1;
        private static final int DRAW_THUMB        = 1 << 1;
        private static final int ALWAYS_DRAW_TRACK = 1 << 2;
        private static final int TRACK_HOVERED     = 1 << 3;
        private static final int THUMB_HOVERED     = 1 << 4;
        private static final int VERTICAL          = 1 << 5;

        @Nullable
        private Drawable track;
        //Nonnull
        private Drawable thumb;

        public int flags;

        private float thumbOffset;
        private float scrollRange;

        private int left;
        private int top;
        private int right;
        private int bottom;

        /**
         * Alternative size if not specified in drawable
         * {@link #setAlternativeSize(int)}
         * {@link #getSize()}
         */
        private int altSize;

        /**
         * left, top, right, bottom padding
         * {@link #setPadding(int, int, int, int)}
         */
        private int padding;

        public ScrollBar() {
            //TODO config for default size
            altSize = 5;
        }

        private void draw(@Nonnull Canvas canvas) {
            /*if (!barHovered && !isDragging && brightness > 0.5f) {
                if (canvas.getDrawingTime() > startTime) {
                    float change = (startTime - canvas.getDrawingTime()) / 2000.0f;
                    brightness = Math.max(0.75f + change, 0.5f);
                }
            }
            canvas.setColor(16, 16, 16, 40);
            canvas.drawRect(getLeft(), getTop(), getRight(), getBottom());
            int br = (int) (brightness * 255.0f);
            canvas.setColor(br, br, br, 128);
            canvas.drawRect(getLeft(), barY, getRight(), barY + barLength);*/

            if ((flags & DRAW_TRACK) != 0 && track != null) {
                track.draw(canvas);
            }
            if ((flags & DRAW_THUMB) != 0) {
                // due to gui scaling, we have to do with float rather than integer
                canvas.save();
                if (isVertical()) {
                    canvas.translate(0, thumbOffset);
                } else {
                    canvas.translate(thumbOffset, 0);
                }
                thumb.draw(canvas);
                canvas.restore();
            }
        }

        /**
         * Set scroll bar parameters, should be called from scroller's listener
         *
         * @param range  scroll range, max scroll amount
         * @param offset scroll offset, current scroll amount
         * @param extent visible range
         */
        public void setParameters(float range, float offset, float extent) {
            boolean drawTrack;
            boolean drawThumb;
            boolean vertical = isVertical();
            if (extent <= 0 || range <= 0) {
                drawTrack = (flags & ALWAYS_DRAW_TRACK) != 0;
                drawThumb = false;
            } else {
                drawTrack = drawThumb = true;
            }
            if (track != null) {
                track.setBounds(left, top, right, bottom);
            }

            final int totalLength;
            final int thickness;
            if (vertical) {
                totalLength = getHeight();
                thickness = getWidth();
            } else {
                totalLength = getWidth();
                thickness = getHeight();
            }

            float preciseLength = totalLength * extent / (range + extent);
            float preciseOffset = (totalLength - preciseLength) * offset / range;

            int thumbLength = Math.round(Math.max(preciseLength, thickness << 1));
            thumbOffset = Math.min(preciseOffset, totalLength - thumbLength);
            scrollRange = range;

            if (drawThumb) {
                if (vertical) {
                    thumb.setBounds(left, top, right, top + thumbLength);
                } else {
                    thumb.setBounds(left, top, left + thumbLength, bottom);
                }
            }
            if (drawTrack) {
                flags |= DRAW_TRACK;
            } else {
                flags &= ~DRAW_TRACK;
            }
            if (drawThumb) {
                flags |= DRAW_THUMB;
            } else {
                flags &= ~DRAW_THUMB;
            }
        }

        public void setTrackDrawable(@Nullable Drawable track) {
            this.track = track;
        }

        public void setThumbDrawable(@Nonnull Drawable thumb) {
            this.thumb = thumb;
        }

        public boolean isAlwaysDrawTrack() {
            return (flags & ALWAYS_DRAW_TRACK) != 0;
        }

        /**
         * Indicates whether the vertical scrollbar track should always be drawn
         * regardless of the extent.
         */
        public void setAlwaysDrawTrack(boolean alwaysDrawTrack) {
            if (alwaysDrawTrack) {
                flags |= ALWAYS_DRAW_TRACK;
            } else {
                flags &= ~ALWAYS_DRAW_TRACK;
            }
        }

        /**
         * Set the scroll bar alternative size, if size is not specified
         * in thumb or track drawable.
         *
         * @param alternativeSize alternative scrollbar thickness
         */
        public void setAlternativeSize(int alternativeSize) {
            altSize = alternativeSize;
        }

        /**
         * Set the scroll bar padding to view frame
         *
         * @param left   left padding [0-255]
         * @param top    top padding [0-255]
         * @param right  right padding [0-255]
         * @param bottom bottom padding [0-255]
         */
        public void setPadding(int left, int top, int right, int bottom) {
            padding = left | top << 8 | right << 16 | bottom << 24;
        }

        public int getLeftPadding() {
            return padding & 0xff;
        }

        public int getTopPadding() {
            return (padding >> 8) & 0xff;
        }

        public int getRightPadding() {
            return (padding >> 16) & 0xff;
        }

        public int getBottomPadding() {
            return (padding >> 24) & 0xff;
        }

        private int getSize() {
            int s;
            if (isVertical()) {
                if (track != null) {
                    s = track.getIntrinsicWidth();
                } else {
                    s = thumb.getIntrinsicWidth();
                }
            } else {
                if (track != null) {
                    s = track.getIntrinsicHeight();
                } else {
                    s = thumb.getIntrinsicHeight();
                }
            }
            if (s <= 0) {
                return altSize;
            }
            return s;
        }

        private boolean isVertical() {
            return (flags & VERTICAL) != 0;
        }

        private int getThumbLength() {
            if (isVertical()) {
                return thumb.getHeight();
            } else {
                return thumb.getWidth();
            }
        }

        /*public void draw(float currentTime) {
            if (!barHovered && !isDragging && brightness > 0.5f) {
                if (currentTime > startTime) {
                    float change = (startTime - currentTime) / 40.0f;
                    brightness = Math.max(0.75f + change, 0.5f);
                }
            }
            if (!visible) {
                return;
            }

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.getBuffer();

            bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            bufferBuilder.pos(x, y + maxLength, 0.0D).color(16, 16, 16, 40).endVertex();
            bufferBuilder.pos(x + barThickness, y + maxLength, 0.0D).color(16, 16, 16, 40).endVertex();
            bufferBuilder.pos(x + barThickness, y, 0.0D).color(16, 16, 16, 40).endVertex();
            bufferBuilder.pos(x, y, 0.0D).color(16, 16, 16, 40).endVertex();
            tessellator.draw();

            int b = (int) (brightness * 255);

            bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            bufferBuilder.pos(x, barY + barLength, 0.0D).color(b, b, b, 128).endVertex();
            bufferBuilder.pos(x + barThickness, barY + barLength, 0.0D).color(b, b, b, 128).endVertex();
            bufferBuilder.pos(x + barThickness, barY, 0.0D).color(b, b, b, 128).endVertex();
            bufferBuilder.pos(x, barY, 0.0D).color(b, b, b, 128).endVertex();
            tessellator.draw();
        }*/

        /*private void wake() {
            brightness = 0.75f;
            startTime = UIManager.INSTANCE.getDrawingTime() + 10.0f;
        }*/

        /*public void setBarLength(float percentage) {
            this.barLength = (int) (getHeight() * percentage);
        }

        private float getMaxDragLength() {
            return getHeight() - barLength;
        }

        public void setBarOffset(float percentage) {
            barY = getMaxDragLength() * percentage + getTop();
            wake();
        }*/

        private void setFrame(int l, int t, int r, int b) {
            left = l;
            top = t;
            right = r;
            bottom = b;
        }

        private boolean updateMouseHover(double mouseX, double mouseY) {
            if (mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom) {
                flags |= TRACK_HOVERED;
                return true;
            }
            //TODO drawable states
            flags &= ~TRACK_HOVERED;
            return false;
        }

        private boolean isThumbHovered() {
            return (flags & THUMB_HOVERED) != 0;
        }

        private boolean isTrackHovered() {
            return (flags & TRACK_HOVERED) != 0;
        }

        /*@Override
        protected boolean onUpdateMouseHover(int mouseX, int mouseY) {
            boolean prev = barHovered;
            barHovered = isMouseOnBar(mouseY);
            if (prev != barHovered) {
                if (barHovered) {
                    wake();
                } else {
                    startTime = UIManager.INSTANCE.getDrawingTime() + 10.0f;
                }
            }
            return super.onUpdateMouseHover(mouseX, mouseY);
        }*/

        /*@Override
        protected void onMouseHoverMoved(double mouseX, double mouseY) {
            super.onMouseHoverMoved(mouseX, mouseY);
            if (vertical) {
                float thumbY = getTop() + thumbOffset;
                thumbHovered = mouseY >= thumbY && mouseY < thumbY + thumbLength;
            } else {
                float thumbX = getLeft() + thumbOffset;
                thumbHovered = mouseX >= thumbX && mouseX < thumbX + thumbLength;
            }
        }

        @Override
        protected void onMouseHoverExit() {
            super.onMouseHoverExit();
            thumbHovered = false;
        }*/

        private int getWidth() {
            return right - left;
        }

        private int getHeight() {
            return bottom - top;
        }

        private boolean onMouseClicked(double mouseX, double mouseY, int mouseButton) {
            /*if (thumbHovered) {
                UIManager.INSTANCE.setDragging(this);
                return true;
            }*/
            if (isTrackHovered()) {
                if (isVertical()) {
                    float start = top + thumbOffset;
                    float end = start + getThumbLength();
                    if (mouseY < start) {
                        float delta = toScrollDelta((float) (mouseY - start - 1), true);
                        onScrollBarClicked(true, Math.max(-60.0f, delta));
                        return true;
                    } else if (mouseY > end) {
                        float delta = toScrollDelta((float) (mouseY - end + 1), true);
                        onScrollBarClicked(true, Math.min(60.0f, delta));
                        return true;
                    }
                } else {
                    float start = left + thumbOffset;
                    float end = start + getThumbLength();
                    if (mouseX < start) {
                        float delta = toScrollDelta((float) (mouseX - start - 1), false);
                        onScrollBarClicked(false, Math.max(-60.0f, delta));
                        return true;
                    } else if (mouseX > end) {
                        float delta = toScrollDelta((float) (mouseX - end + 1), false);
                        onScrollBarClicked(false, Math.min(60.0f, delta));
                        return true;
                    }
                }
            }
            return false;
        }

        /*@Override
        protected boolean onMouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
            if (vertical) {
                if (mouseY >= getTop() && mouseY <= getBottom()) {
                    *//*accDelta += deltaY;
                    int i = (int) (accDelta * scale);
                    float delta = i / scale;
                    accDelta -= delta;
                    delta = toScrollDelta(delta);*//*
                    onScrollBarDragged(toScrollDelta((float) deltaY), vertical);
                    return true;
                }
            } else {
                if (mouseX >= getLeft() && mouseX <= getRight()) {
                    *//*accDelta += deltaX;
                    int i = (int) (accDelta * 2.0f);
                    float delta = i / 2.0f;
                    accDelta -= delta;
                    delta = toScrollDelta(delta);*//*
                    onScrollBarDragged(toScrollDelta((float) deltaX), vertical);
                    return true;
                }
            }
            return super.onMouseDragged(mouseX, mouseY, deltaX, deltaY);
        }*/

        /*@Override
        protected void onMouseHoverExit() {
            super.onMouseHoverExit();
            if (barHovered) {
                barHovered = false;
                startTime = UIManager.INSTANCE.getDrawingTime() + 10.0f;
            }
        }*/

        /*private boolean isMouseOnBar(double mouseY) {
            return mouseY >= barY && mouseY <= barY + barLength;
        }*/

        /*@Override
        protected boolean onMouseLeftClicked(int mouseX, int mouseY) {
            if (barHovered) {
                isDragging = true;
                UIManager.INSTANCE.setDragging(this);
            } else {
                if (mouseY < barY) {
                    float mov = transformPosToAmount((float) (barY - mouseY));
                    //controller.scrollSmoothBy(-Math.min(60f, mov));
                } else if (mouseY > barY + barLength) {
                    float mov = transformPosToAmount((float) (mouseY - barY - barLength));
                    //controller.scrollSmoothBy(Math.min(60f, mov));
                }
            }
            return true;
        }*/

        /*@Override
        protected void onStopDragging() {
            super.onStopDragging();
            if (isDragging) {
                isDragging = false;
                startTime = UIManager.INSTANCE.getDrawingTime() + 10.0f;
            }
        }*/

        /*@Override
        protected boolean onMouseDragged(int mouseX, int mouseY, double deltaX, double deltaY) {
            *//*if (barY + deltaY >= y && barY - y + deltaY <= getMaxDragLength()) {
                draggingY += deltaY;
            }
            if (mouseY == draggingY) {
                window.scrollDirect(transformPosToAmount((float) deltaY));
            }*//*
            if (mouseY >= getTop() && mouseY <= getBottom()) {
                accDragging += deltaY;
                int i = (int) (accDragging * 2.0);
                if (i != 0) {
                    //controller.scrollDirectBy(transformPosToAmount(i / 2.0f));
                    accDragging -= i / 2.0f;
                }
            }
            return true;
        }*/

        /**
         * Transform mouse position change to scroll amount change
         *
         * @param delta    relative mouse position change
         * @param vertical is vertical
         * @return scroll delta
         */
        private float toScrollDelta(float delta, boolean vertical) {
            delta *= scrollRange;
            if (vertical) {
                return delta / (getHeight() - getThumbLength());
            } else {
                return delta / (getWidth() - getThumbLength());
            }
        }
    }

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
    public interface MouseHoverListener {

        void onEvent();
    }

    /**
     * Defines mouse hover type
     */
    private enum HoverType {

        /**
         * For a View
         */
        TREE,

        /**
         * For a ViewGroup
         */
        STRICT,

        /**
         * For a ViewGroup or it's children
         */
        BOTH;

        /**
         * Returns true if mouse is hovering a View or one of its children
         *
         * @return {@code true} if it's general
         */
        public boolean isGeneral() {
            return this != STRICT;
        }

        /**
         * Returns true if mouse is hovering a View or there's no child is hovered
         *
         * @return {@code true} if it's foremost
         */
        public boolean isForemost() {
            return this != TREE;
        }
    }
}
