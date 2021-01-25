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
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.math.Point;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The top of a view hierarchy, implementing the needed protocol between View
 * and the WindowManager
 */
public final class ViewRootImpl implements ViewParent {

    private final UIManager master;

    private boolean hasDragOperation;

    private View mView;

    /*private final int[] inBounds  = new int[]{0, 0, 0, 0};
    private final int[] outBounds = new int[4];*/

    public ViewRootImpl(UIManager manager) {
        master = manager;
    }

    void setView(@Nonnull View view) {
        if (mView == null) {
            mView = view;
            /*ViewGroup.LayoutParams params = view.getLayoutParams();
            // convert layout params
            if (!(params instanceof LayoutParams)) {
                params = new LayoutParams();
                view.setLayoutParams(params);
            }*/
            view.assignParent(this);
            view.dispatchAttachedToWindow(this);
        }
    }

    boolean startDragAndDrop(@Nonnull View view, @Nullable DragData data, @Nullable View.DragShadow shadow, int flags) {
        if (master.dragEvent != null) {
            ModernUI.LOGGER.error(View.MARKER, "startDragAndDrop failed by another ongoing operation");
            return false;
        }

        Point center = new Point();
        if (shadow == null) {
            shadow = new View.DragShadow(view);
            if (view.isHovered()) {
                // default strategy
                center.x = (int) master.getViewMouseX(view);
                center.y = (int) master.getViewMouseY(view);
            } else {
                shadow.onProvideShadowCenter(center);
            }
        } else {
            shadow.onProvideShadowCenter(center);
        }

        master.dragEvent = new DragEvent(data);
        master.dragShadow = shadow;
        master.dragShadowCenter = center;

        hasDragOperation = true;

        master.performDrag(DragEvent.ACTION_DRAG_STARTED);
        return true;
    }

    void performLayout(int widthSpec, int heightSpec) {
        if (mView == null) {
            return;
        }

        mView.measure(widthSpec, heightSpec);

        /*inBounds[2] = MeasureSpec.getSize(widthSpec);
        inBounds[3] = MeasureSpec.getSize(heightSpec);

        Gravity.apply(lp.gravity, mView.getMeasuredWidth(), mView.getMeasuredHeight(),
                inBounds, lp.x, lp.y, outBounds);*/

        mView.layout(0, 0, mView.getMeasuredWidth(), mView.getMeasuredHeight());
    }

    void onDraw(Canvas canvas) {
        if (mView != null) {
            mView.draw(canvas);
        }
    }

    boolean onInputEvent(InputEvent event) {
        if (mView != null) {
            if (event instanceof KeyEvent) {
                return processKeyEvent((KeyEvent) event);
            } else {
                return processPointerEvent((MotionEvent) event);
            }
        }
        return false;
    }

    private boolean processKeyEvent(KeyEvent event) {
        return false;
    }

    private boolean processPointerEvent(MotionEvent event) {
        return mView.dispatchPointerEvent(event);
    }

    /*boolean onCursorPosEvent(LinkedList<View> route, double x, double y) {
        if (mView != null) {
            return mView.onCursorPosEvent(route, x, y);
        }
        return false;
    }

    boolean onMouseEvent(MotionEvent event) {
        if (mView != null) {
            final boolean handled = mView.onGenericMotionEvent(event);
            if (!handled && event.getAction() == MotionEvent.ACTION_MOVE) {
                mView.ensureMouseHoverExit();
            }
            return handled;
        }
        return false;
    }

    void ensureMouseHoverExit() {
        if (mView != null) {
            mView.ensureMouseHoverExit();
        }
    }*/

    void performDragEvent(DragEvent event) {
        if (hasDragOperation) {

        }
    }

    void tick(int ticks) {
        if (mView != null) {
            mView.tick(ticks);
        }
    }

    @Nullable
    @Override
    public ViewParent getParent() {
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
        master.mLayoutRequested = true;
    }

    @Override
    public float getScrollX() {
        return 0;
    }

    @Override
    public float getScrollY() {
        return 0;
    }

    @Override
    public void childDrawableStateChanged(View child) {

    }

    /*@Deprecated
    public static class LayoutParams extends ViewGroup.LayoutParams {

        *//*
     * X position for this window.  With the default gravity it is ignored.
     * When using {@link Gravity#LEFT} or {@link Gravity#RIGHT} it provides
     * an offset from the given edge.
     *//*
        public int x;

        *//*
     * Y position for this window.  With the default gravity it is ignored.
     * When using {@link Gravity#TOP} or {@link Gravity#BOTTOM} it provides
     * an offset from the given edge.
     *//*
        public int y;

        *//*
     * Placement of window within the screen as per {@link Gravity}.
     *
     * @see Gravity
     *//*
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
    }*/
}
