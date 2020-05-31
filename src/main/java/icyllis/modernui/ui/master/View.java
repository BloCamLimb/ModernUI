/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import icyllis.modernui.ui.layout.DefaultLayout;
import icyllis.modernui.ui.layout.ILayout;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;

@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
public class View implements IDrawable, IViewRect {

    /**
     * Parent view, assigned by {@link #setParent(IViewParent)}
     */
    private IViewParent parent;

    /**
     * View layout info to determine a rect area
     */
    private ILayout layout;

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
     * Auto layout by system
     *
     * @param prev previous child view in parent or the parent
     */
    public final void layout(IViewRect prev) {
        if (layout == null) {
            layout = DefaultLayout.INSTANCE;
        }

        left = layout.getLayoutX(prev, parent);
        top = layout.getLayoutY(prev, parent);

        right = left + layout.getLayoutWidth(prev, parent);
        bottom = top + layout.getLayoutHeight(prev, parent);

        right = Math.max(right, left);
        bottom = Math.max(bottom, top);

        dispatchLayout();
        onLayout();
    }

    /**
     * Layout child views
     */
    protected void dispatchLayout() {

    }

    /**
     * Called after this view and child views layout
     */
    protected void onLayout() {

    }

    public final IViewParent getParent() {
        return parent;
    }

    /**
     * Set parent view, do not call this unless you know what you're doing
     * <p>
     * Should call immediately after this view is created to make sure
     * parent is not null when calling resize()
     *
     * @param parent parent view
     */
    public final void setParent(@Nonnull IViewParent parent) {
        this.parent = parent;
    }

    public final ILayout getLayout() {
        return layout;
    }

    public final void setLayout(@Nonnull ILayout layout) {
        this.layout = layout;
    }

    public final void selfRelayout() {
        getParent().relayoutChild(this);
    }

    public final int getId() {
        return id;
    }

    /**
     * ID should not be repeated in the same view group
     *
     * @param id view id
     */
    public final void setId(int id) {
        this.id = id;
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
        return getParent().getRelativeMX();
    }

    public double getRelativeMY() {
        return getParent().getRelativeMY();
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
