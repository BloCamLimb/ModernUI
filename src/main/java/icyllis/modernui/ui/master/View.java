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
import icyllis.modernui.ui.layout.MeasureSpec;
import icyllis.modernui.ui.test.IViewRect;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;

/**
 * View is the basic component of UI. View has its own rectangular area on screen,
 * which is also responsible for drawing and event handling.
 */
@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
public class View implements IDrawable, IViewRect {

    static final int PFLAG_MEASURED_DIMENSION_SET = 0x00000800;
    static final int PFLAG_LAYOUT_REQUIRED = 0x00002000;

    public int privateFlags;

    /**
     * Parent view, assigned by {@link #assignParent(IViewParent)}
     */
    protected IViewParent parent;

    private int id;

    /**
     * Rect area
     */
    private int left;
    private int top;
    private int right;
    private int bottom;

    /**
     * System properties
     */
    private boolean visible = true;
    private boolean listening = true;

    private int minWidth;
    private int minHeight;

    int prevWidthMeasureSpec = Integer.MIN_VALUE;
    int prevHeightMeasureSpec = Integer.MIN_VALUE;

    int measuredWidth;
    int measuredHeight;

    /**
     * Raw draw method
     *
     * @param canvas canvas to draw content
     * @param time   elapsed time from a gui open
     */
    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        if (visible) {

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
     * Assign rect area of this view and all descendants
     * <p>
     * Derived classes should not override this method.
     * Derived classes with children should override
     * onLayout(). In that method, they should
     * call layout on each of their children
     *
     * @param l left position, relative to parent
     * @param t top position, relative to parent
     * @param r right position, relative to parent
     * @param b bottom position, relative to parent
     */
    public void layout(int l, int t, int r, int b) {
        boolean changed = setFrame(l, t, r, b);

        if (changed || hasFlag(PFLAG_LAYOUT_REQUIRED)) {
            onLayout(changed, l, t, r, b);

            removeFlag(PFLAG_LAYOUT_REQUIRED);
        }
    }

    /**
     * Layout child views
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
     * Assign the rect area of this view, called from layout()
     *
     * @param left   left position, relative to parent
     * @param top    top position, relative to parent
     * @param right  right position, relative to parent
     * @param bottom bottom position, relative to parent
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

        final boolean specChanged =
                widthMeasureSpec != prevWidthMeasureSpec
                        || heightMeasureSpec != prevHeightMeasureSpec;
        final boolean isSpecExactly =
                MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.Mode.EXACTLY
                        && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.Mode.EXACTLY;
        final boolean matchesSpecSize =
                measuredWidth == MeasureSpec.getSize(widthMeasureSpec)
                        && measuredHeight == MeasureSpec.getSize(heightMeasureSpec);
        final boolean needsLayout = specChanged
                && (!isSpecExactly || !matchesSpecSize);

        if (needsLayout) {
            // remove the flag first anyway
            removeFlag(PFLAG_MEASURED_DIMENSION_SET);

            onMeasure(widthMeasureSpec, heightMeasureSpec);

            // the flag should be added in onMeasure()
            if (!hasFlag(PFLAG_MEASURED_DIMENSION_SET)) {
                throw new IllegalStateException();
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
     * Helper method to get default size
     *
     * @param size        default size
     * @param measureSpec measure specification
     * @return measured size
     */
    public static int getDefaultSize(int size, int measureSpec) {
        int result = size;

        switch (MeasureSpec.getMode(measureSpec)) {
            case UNSPECIFIED:
                break;
            case AT_MOST:
            case EXACTLY:
                result = MeasureSpec.getSize(measureSpec);
                break;
        }

        return result;
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

    public void relayoutFromParent() {
        getParent().relayoutChild(this);
    }

    public int getId() {
        return id;
    }

    /**
     * ID should not be repeated in the same view group
     *
     * @param id view id
     */
    public void setId(int id) {
        this.id = id;
    }

    // helper method
    void addFlag(int flag) {
        privateFlags |= flag;
    }

    // helper method
    void removeFlag(int flag) {
        privateFlags &= ~flag;
    }

    // helper method
    boolean hasFlag(int flag) {
        return (privateFlags & flag) != 0;
    }

    /**
     * Get view width
     *
     * @return width
     */
    @Override
    public final int getWidth() {
        return right - left;
    }

    /**
     * Get view height
     *
     * @return height
     */
    @Override
    public final int getHeight() {
        return bottom - top;
    }

    /**
     * Get view left (x1)
     *
     * @return left
     */
    @Override
    public final int getLeft() {
        return left;
    }

    /**
     * Get view top (y1)
     *
     * @return top
     */
    @Override
    public final int getTop() {
        return top;
    }

    /**
     * Get view right (x2)
     *
     * @return right
     */
    @Override
    public final int getRight() {
        return right;
    }

    /**
     * Get view bottom (y2)
     *
     * @return bottom
     */
    @Override
    public final int getBottom() {
        return bottom;
    }

    public final void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            onVisibleChanged(visible);
        }
    }

    public final void setListening(boolean listening) {
        if (this.listening != listening) {
            this.listening = listening;
            onListeningChanged(listening);
        }
    }

    public final boolean isVisible() {
        return visible;
    }

    public final boolean isListening() {
        return listening;
    }

    protected void onListeningChanged(boolean listening) {

    }

    protected void onVisibleChanged(boolean visible) {

    }

    public double getRelativeMX() {
        return getParent().getRelativeMX() + getParent().getTranslationX();
    }

    public double getRelativeMY() {
        return getParent().getRelativeMY() + getParent().getTranslationY();
    }

    public float toAbsoluteX(float rx) {
        return getParent().toAbsoluteX(rx) - getParent().getTranslationX();
    }

    public float toAbsoluteY(float ry) {
        return getParent().toAbsoluteY(ry) - getParent().getTranslationY();
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
    }

    /**
     * Check if mouse hover this view
     * This is system method and can't be override
     *
     * @param mouseX relative mouse X pos
     * @param mouseY relative mouse Y pos
     * @return return {@code true} if certain view hovered
     */
    public final boolean updateMouseHover(double mouseX, double mouseY) {
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

}
