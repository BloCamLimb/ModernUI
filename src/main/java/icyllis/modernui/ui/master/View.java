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

package icyllis.modernui.ui.master;

import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.ui.layout.MeasureSpec;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * View is the basic component of UI. View has its own rectangular area on screen,
 * which is also responsible for drawing and event handling.
 */
@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
public class View {

    private static final AtomicInteger GENERATED_ID = new AtomicInteger(1);

    /**
     * Used to mark a View that has no ID.
     */
    public static final int NO_ID = -1;

    /**
     * Private flags
     */
    private static final int PFLAG_MEASURED_DIMENSION_SET = 1 << 11;
    private static final int PFLAG_LAYOUT_REQUIRED = 1 << 13;

    /**
     * Private boolean state flags
     * {@link #addFlag(int)}
     * {@link #removeFlag(int)}
     * {@link #hasFlag(int)}
     */
    private int privateFlags;

    /**
     * View flag masks
     */
    private static final int VISIBILITY_MASK = 0x3;

    /**
     * View multi-state flags
     * {@link #setFlag(int, int)}
     */
    private int viewFlags;

    /**
     * Parent view of this view
     * {@link #assignParent(IViewParent)}
     */
    private IViewParent parent;

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

    private boolean listening = true;

    private int minWidth;
    private int minHeight;

    /**
     * Cached previous measure spec to avoid unnecessary measurements
     */
    private int prevWidthMeasureSpec = Integer.MIN_VALUE;
    private int prevHeightMeasureSpec = Integer.MIN_VALUE;

    /**
     * The measurement result in onMeasure(), used to layout
     */
    private int measuredWidth;
    private int measuredHeight;

    /**
     * Raw draw method
     *
     * @param canvas canvas to draw content
     * @param time   elapsed time from a gui open
     */
    protected void draw(@Nonnull Canvas canvas, float time) {
        if ((viewFlags & VISIBILITY_MASK) == 0) {

            onDraw(canvas, time);

            dispatchDraw(canvas, time);
        }
    }

    /**
     * Draw this view if visible
     *
     * @param canvas canvas to draw content
     * @param time   elapsed time from a gui open
     */
    protected void onDraw(@Nonnull Canvas canvas, float time) {

    }

    /**
     * Dispatch events to child views
     *
     * @param canvas canvas to draw content
     * @param time   elapsed time from a gui open
     */
    protected void dispatchDraw(@Nonnull Canvas canvas, float time) {

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

        if (changed || hasFlag(PFLAG_LAYOUT_REQUIRED)) {
            onLayout(changed, left, top, right, bottom);

            removeFlag(PFLAG_LAYOUT_REQUIRED);
        }
    }

    /**
     * Layout child views
     *
     * @param changed whether the size or position of this view was changed
     * @param left    left position, relative to game window
     * @param top     top position, relative to game window
     * @param right   right position, relative to game window
     * @param bottom  bottom position, relative to game window
     */
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

    }

    /**
     * Assign the rect area of this view, called from layout()
     *
     * @param left   left position, relative to game window
     * @param top    top position, relative to game window
     * @param right  right position, relative to game window
     * @param bottom bottom position, relative to game window
     * @return whether the rect area of this view was changed
     */
    protected boolean setFrame(int left, int top, int right, int bottom) {
        if (this.left != left || this.right != right || this.top != top || this.bottom != bottom) {

            int oldWidth = this.right - this.left;
            int oldHeight = this.bottom - this.top;
            int newWidth = right - left;
            int newHeight = bottom - top;

            boolean sizeChanged = (newWidth != oldWidth) || (newHeight != oldHeight);

            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;

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
     * @see #onMeasure(int, int)
     */
    public final void measure(int widthMeasureSpec, int heightMeasureSpec) {

        boolean specChanged =
                widthMeasureSpec != prevWidthMeasureSpec
                        || heightMeasureSpec != prevHeightMeasureSpec;
        boolean isSpecExactly =
                MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.Mode.EXACTLY
                        && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.Mode.EXACTLY;
        boolean matchesSpecSize =
                measuredWidth == MeasureSpec.getSize(widthMeasureSpec)
                        && measuredHeight == MeasureSpec.getSize(heightMeasureSpec);
        boolean needsLayout = specChanged
                && (!isSpecExactly || !matchesSpecSize);

        if (needsLayout) {
            // remove the flag first anyway
            removeFlag(PFLAG_MEASURED_DIMENSION_SET);

            onMeasure(widthMeasureSpec, heightMeasureSpec);

            // the flag should be added in onMeasure()
            if (!hasFlag(PFLAG_MEASURED_DIMENSION_SET)) {
                ModernUI.LOGGER.fatal(UIManager.MARKER, "measured dimension unspecified on measure");
                setMeasuredDimension(getDefaultSize(minWidth, widthMeasureSpec),
                        getDefaultSize(minHeight, heightMeasureSpec));
            }

            addFlag(PFLAG_LAYOUT_REQUIRED);
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
     * Set measurement result, should be called in {@link #onMeasure(int, int)}
     *
     * @param measuredWidth  measured width of this view
     * @param measuredHeight measured height of this view
     */
    protected final void setMeasuredDimension(int measuredWidth, int measuredHeight) {
        this.measuredWidth = measuredWidth;
        this.measuredHeight = measuredHeight;

        addFlag(PFLAG_MEASURED_DIMENSION_SET);
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

    /**
     * Get the parent of this view
     *
     * @return parent of this view
     */
    public final IViewParent getParent() {
        return parent;
    }

    /**
     * Assign parent view, do not call this unless you know what you're doing
     * <p>
     * Should call immediately after this view is created to make sure
     * parent is not null when calling resize()
     *
     * @param parent parent view
     */
    void assignParent(@Nonnull IViewParent parent) {
        if (this.parent == null) {
            this.parent = parent;
        } else {
            throw new RuntimeException("parent of view " + this + " has been assigned");
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
            int cid = GENERATED_ID.get();
            int nid = cid + 1;
            if (nid < 1) {
                nid = 1;
            }
            if (GENERATED_ID.compareAndSet(cid, nid)) {
                return cid;
            }
        }
    }

    private void addFlag(int flag) {
        privateFlags |= flag;
    }

    private void removeFlag(int flag) {
        privateFlags &= ~flag;
    }

    private boolean hasFlag(int flag) {
        return (privateFlags & flag) != 0;
    }

    //TODO state switching events
    private void setFlag(int flag, int mask) {
        final int old = viewFlags;

        viewFlags = (viewFlags & ~mask) | (flag & mask);

        final int change = viewFlags ^ old;

    }

    /**
     * Set visibility of this view
     * See {@link Visibility}
     *
     * @param visibility visibility to set
     */
    public void setVisibility(@Nonnull Visibility visibility) {
        setFlag(visibility.ordinal(), VISIBILITY_MASK);
    }

    /**
     * Get visibility of this view.
     * See {@link Visibility}
     *
     * @return visibility
     */
    public Visibility getVisibility() {
        return Visibility.values()[viewFlags & VISIBILITY_MASK];
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

    public final void setListening(boolean listening) {
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

    }

    /*public double getRelativeMX() {
        return getParent().getRelativeMX() + getParent().getScrollX();
    }

    public double getRelativeMY() {
        return getParent().getRelativeMY() + getParent().getScrollY();
    }

    public float toAbsoluteX(float rx) {
        return getParent().toAbsoluteX(rx) - getParent().getScrollX();
    }

    public float toAbsoluteY(float ry) {
        return getParent().toAbsoluteY(ry) - getParent().getScrollY();
    }

    public float getAbsoluteLeft() {
        return toAbsoluteX(left);
    }

    public float getAbsoluteTop() {
        return toAbsoluteY(top);
    }

    public float getAbsoluteRight() {
        return toAbsoluteX(right);
    }

    public float getAbsoluteBottom() {
        return toAbsoluteY(bottom);
    }*/

    /**
     * Check if mouse hover this view
     * This is system method and can't be override
     *
     * @param mouseX relative mouse X pos
     * @param mouseY relative mouse Y pos
     * @return return {@code true} if certain view hovered
     */
    final boolean updateMouseHover(double mouseX, double mouseY) {
        if (isInView(mouseX, mouseY)) {
            if (dispatchMouseHover(mouseX, mouseY)) {
                return true;
            }
            UIManager.INSTANCE.setHoveredView(this);
            return true;
        }
        return false;
    }

    // helper
    private boolean isInView(double mx, double my) {
        return mx >= left && mx <= right && my >= top && my <= bottom;
    }

    /**
     * Dispatch events to child views if present
     *
     * @param mouseX relative mouse X pos
     * @param mouseY relative mouse Y pos
     * @return return {@code true} if certain child view hovered
     */
    protected boolean dispatchMouseHover(double mouseX, double mouseY) {
        return false;
    }

    /**
     * Called when mouse start to hover on this view
     */
    protected void onMouseHoverEnter() {

    }

    /**
     * Called when mouse no longer hover on this view
     */
    protected void onMouseHoverExit() {

    }

    /**
     * Called when mouse hover and left button clicked
     *
     * @param mouseX relative mouse X pos
     * @param mouseY relative mouse Y pos
     * @return return {@code true} if action performed
     */
    protected boolean onMouseLeftClicked(double mouseX, double mouseY) {
        return false;
    }

    /**
     * Called when mouse hover and left button released
     *
     * @param mouseX relative mouse X pos
     * @param mouseY relative mouse Y pos
     * @return return {@code true} if action performed
     */
    protected boolean onMouseLeftReleased(double mouseX, double mouseY) {
        return false;
    }

    /**
     * Called when mouse hover and left button double clicked
     *
     * @param mouseX relative mouse X pos
     * @param mouseY relative mouse Y pos
     * @return return {@code true} if action performed
     */
    protected boolean onMouseDoubleClicked(double mouseX, double mouseY) {
        return false;
    }

    /**
     * Called when mouse hover and right button clicked
     *
     * @param mouseX relative mouse X pos
     * @param mouseY relative mouse Y pos
     * @return return {@code true} if action performed
     */
    protected boolean onMouseRightClicked(double mouseX, double mouseY) {
        return false;
    }

    /**
     * Called when mouse hover and right button released
     *
     * @param mouseX relative mouse X pos
     * @param mouseY relative mouse Y pos
     * @return return {@code true} if action performed
     */
    protected boolean onMouseRightReleased(double mouseX, double mouseY) {
        return false;
    }

    /**
     * Called when mouse hover and mouse scrolled
     *
     * @param mouseX relative mouse X pos
     * @param mouseY relative mouse Y pos
     * @param amount scroll amount
     * @return return {@code true} if action performed
     */
    protected boolean onMouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }

    /**
     * Call when this view start to be listened as a draggable
     */
    protected void onStartDragging() {

    }

    /**
     * Call when this view is no longer listened as a draggable
     */
    protected void onStopDragging() {

    }

    /**
     * Called when mouse moved
     *
     * @param mouseX relative mouse X pos
     * @param mouseY relative mouse X pos
     * @param deltaX mouse x change
     * @param deltaY mouse y change
     * @return return {@code true} if action performed
     */
    protected boolean onMouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        return false;
    }

    /**
     * Call when this view start to be listened as a keyboard listener
     */
    protected void onStartKeyboard() {

    }

    /**
     * Call when this view is no longer listened as a keyboard listener
     */
    protected void onStopKeyboard() {

    }

    /**
     * Called when a key pressed
     *
     * @param keyCode   see {@link GLFW}
     * @param scanCode  keyboard scan code
     * @param modifiers modifier key, see {@link GLFW}
     * @return return {@code true} if action performed
     */
    protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /**
     * Called when a key released
     *
     * @param keyCode   see {@link GLFW}
     * @param scanCode  keyboard scan code
     * @param modifiers modifier key, see {@link GLFW}
     * @return return {@code true} if action performed
     */
    protected boolean onKeyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /**
     * Called when a unicode character typed
     *
     * @param codePoint chat code
     * @param modifiers modifier key, see {@link GLFW}
     * @return return {@code true} if action performed
     */
    protected boolean onCharTyped(char codePoint, int modifiers) {
        return false;
    }

    /**
     * View visibility.
     * {@link #setVisibility(Visibility)}
     * {@link #getVisibility()}
     */
    public enum Visibility {

        /**
         * This view is visible, as view's default value
         */
        VISIBLE,

        /**
         * This view is invisible, but it still takes up space for layout.
         */
        INVISIBLE,

        /**
         * This view is invisible, and it doesn't take any space for layout.
         */
        GONE
    }

}
