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

package icyllis.modernui.view;

import icyllis.modernui.graphics.math.Point;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.system.ModernUI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;

/**
 * The top of a view hierarchy, implementing the needed protocol between View
 * and the system
 */
public final class ViewRootImpl implements IViewParent {

    public static final int TYPE_APPLICATION = 500;

    public static final int TYPE_OVERLAY = 2500;

    private final UIManager manager;

    private boolean hasDragOperation;

    // to schedule layout on next frame
    private boolean layoutRequested = false;

    final int type;
    View view;

    private final int[] inBounds  = new int[]{0, 0, 0, 0};
    private final int[] outBounds = new int[4];

    public ViewRootImpl(UIManager manager, int type) {
        this.manager = manager;
        this.type = type;
    }

    void install(@Nonnull View view) {
        this.view = view;
        ViewGroup.LayoutParams params = view.getLayoutParams();
        // convert layout params
        if (!(params instanceof LayoutParams)) {
            params = new LayoutParams();
            view.setLayoutParams(params);
        }
        view.assignParent(this);
        view.dispatchAttachedToWindow(this);
    }

    boolean startDragAndDrop(@Nonnull View view, @Nullable DragData data, @Nullable View.DragShadow shadow, int flags) {
        if (manager.dragEvent != null) {
            ModernUI.LOGGER.error(View.MARKER, "startDragAndDrop failed by another ongoing operation");
            return false;
        }

        Point center = new Point();
        if (shadow == null) {
            shadow = new View.DragShadow(view);
            if (view.isMouseHovered()) {
                // default strategy
                center.x = (int) manager.getViewMouseX(view);
                center.y = (int) manager.getViewMouseY(view);
            } else {
                shadow.onProvideShadowCenter(center);
            }
        } else {
            shadow.onProvideShadowCenter(center);
        }

        manager.dragEvent = new DragEvent(data);
        manager.dragShadow = shadow;
        manager.dragShadowCenter = center;

        hasDragOperation = true;

        manager.performDrag(DragEvent.ACTION_DRAG_STARTED);
        return true;
    }

    void performLayout(int widthSpec, int heightSpec, boolean forceLayout) {
        if (view == null || (!forceLayout && !layoutRequested)) {
            return;
        }
        LayoutParams lp = (LayoutParams) view.getLayoutParams();

        int childWidthMeasureSpec = ViewGroup.getChildMeasureSpec(widthSpec,
                0, lp.width);
        int childHeightMeasureSpec = ViewGroup.getChildMeasureSpec(heightSpec,
                0, lp.height);

        view.measure(childWidthMeasureSpec, childHeightMeasureSpec);

        inBounds[2] = MeasureSpec.getSize(widthSpec);
        inBounds[3] = MeasureSpec.getSize(heightSpec);

        Gravity.apply(lp.gravity, view.getMeasuredWidth(), view.getMeasuredHeight(),
                inBounds, lp.x, lp.y, outBounds);

        view.layout(outBounds[0], outBounds[1], outBounds[2], outBounds[3]);
        layoutRequested = false;
    }

    void onDraw(Canvas canvas) {
        if (view != null) {
            view.draw(canvas);
        }
    }

    boolean onCursorPosEvent(LinkedList<View> route, double x, double y) {
        if (view != null) {
            return view.onCursorPosEvent(route, x, y);
        }
        return false;
    }

    boolean onMouseEvent(MotionEvent event) {
        if (view != null) {
            final boolean handled = view.onMouseEvent(event);
            if (!handled && event.action == MotionEvent.ACTION_MOVE) {
                view.ensureMouseHoverExit();
            }
            return handled;
        }
        return false;
    }

    void ensureMouseHoverExit() {
        if (view != null) {
            view.ensureMouseHoverExit();
        }
    }

    void performDragEvent(DragEvent event) {
        if (hasDragOperation) {

        }
    }

    void tick(int ticks) {
        if (view != null) {
            view.tick(ticks);
        }
    }

    @Nullable
    @Override
    public IViewParent getParent() {
        return null;
    }

    /**
     * Request layout all views with layout mark in layout pass
     *
     * @see View#requestLayout()
     * @see View#forceLayout()
     */
    @Override
    public void requestLayout() {
        layoutRequested = true;
        manager.layoutRequested = true;
    }

    @Override
    public float getScrollX() {
        return 0;
    }

    @Override
    public float getScrollY() {
        return 0;
    }

    /**
     * Window layout params
     */
    public static class LayoutParams extends ViewGroup.LayoutParams {

        /**
         * X position for this window.  With the default gravity it is ignored.
         * When using {@link Gravity#LEFT} or {@link Gravity#RIGHT} it provides
         * an offset from the given edge.
         */
        public int x;

        /**
         * Y position for this window.  With the default gravity it is ignored.
         * When using {@link Gravity#TOP} or {@link Gravity#BOTTOM} it provides
         * an offset from the given edge.
         */
        public int y;

        /**
         * Placement of window within the screen as per {@link Gravity}.
         *
         * @see Gravity
         */
        public int gravity = Gravity.TOP_LEFT;

        public LayoutParams() {
            super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.gravity = gravity;
        }
    }
}
