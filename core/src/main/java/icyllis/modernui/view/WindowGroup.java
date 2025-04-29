/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

import icyllis.modernui.animation.LayoutTransition;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.Rect;
import org.jetbrains.annotations.ApiStatus;

/**
 * The root view of window view hierarchy, allowing for sub windows (panels),
 * such as menu popups, tooltips and toasts.
 *
 * @hidden
 */
//TODO this class needs to be refactored on top of ViewRoot
@ApiStatus.Internal
public final class WindowGroup extends ViewGroup implements WindowManager {

    public WindowGroup(@NonNull Context context) {
        super(context);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        //setLayoutTransition(new LayoutTransition());
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
        if (mFocused != null) {
            var attrs = (WindowManager.LayoutParams) mFocused.getLayoutParams();
            if (attrs.isModal()) {
                return dispatchTransformedTouchEvent(ev, mFocused, false);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected boolean dispatchHoverEvent(@NonNull MotionEvent event) {
        if (mFocused != null) {
            var attrs = (WindowManager.LayoutParams) mFocused.getLayoutParams();
            if (attrs.isModal()) {
                return dispatchTransformedGenericPointerEvent(event, mFocused);
            }
        }
        return super.dispatchHoverEvent(event);
    }

    @Override
    protected boolean dispatchGenericPointerEvent(@NonNull MotionEvent event) {
        if (mFocused != null) {
            var attrs = (WindowManager.LayoutParams) mFocused.getLayoutParams();
            if (attrs.isModal()) {
                return dispatchTransformedGenericPointerEvent(event, mFocused);
            }
        }
        return super.dispatchGenericPointerEvent(event);
    }

    @Override
    boolean dispatchTooltipHoverEvent(@NonNull MotionEvent event) {
        if (mFocused != null) {
            var attrs = (WindowManager.LayoutParams) mFocused.getLayoutParams();
            if (attrs.isModal()) {
                return dispatchTransformedTooltipHoverEvent(event, mFocused);
            }
        }
        return super.dispatchTooltipHoverEvent(event);
    }

    @Override
    public PointerIcon onResolvePointerIcon(@NonNull MotionEvent event) {
        if (mFocused != null) {
            var attrs = (WindowManager.LayoutParams) mFocused.getLayoutParams();
            if (attrs.isModal()) {
                return dispatchResolvePointerIcon(event, mFocused);
            }
        }
        return super.onResolvePointerIcon(event);
    }

    @Override
    public void addView(@NonNull View child, int index, @NonNull ViewGroup.LayoutParams params) {
        if (!(params instanceof WindowManager.LayoutParams lhs)) {
            throw new IllegalArgumentException("Params must be WindowManager.LayoutParams");
        }
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            var rhs = (WindowManager.LayoutParams) getChildAt(i).getLayoutParams();
            if (lhs.type < rhs.type) {
                index = i;
                break;
            }
        }
        super.addView(child, index, params);
    }

    @Override
    protected void onViewAdded(View child) {
        super.onViewAdded(child);
        var attrs = (WindowManager.LayoutParams) child.getLayoutParams();
        if ((attrs.flags & WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) == 0) {
            requestChildFocus(child, child);
        }
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        return super.onRequestFocusInDescendants(View.FOCUS_BACKWARD, previouslyFocusedRect);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {

                var attrs = (WindowManager.LayoutParams) child.getLayoutParams();

                int childWidthSpec = getChildMeasureSpec(widthMeasureSpec, 0, attrs.width);
                int childHeightSpec = getChildMeasureSpec(heightMeasureSpec, 0, attrs.height);

                child.measure(childWidthSpec, childHeightSpec);
            }
        }

        int windowWidth = MeasureSpec.getSize(widthMeasureSpec);
        int windowHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(windowWidth, windowHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int count = getChildCount();
        final Rect outParentFrame = new Rect(left, top, right, bottom);
        final Rect outFrame = new Rect();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {

                var attrs = (WindowManager.LayoutParams) child.getLayoutParams();

                final int pw = outParentFrame.width();
                final int ph = outParentFrame.height();

                final int w = child.getMeasuredWidth();
                final int h = child.getMeasuredHeight();

                Gravity.apply(attrs.gravity, w, h, outParentFrame,
                        (int) (attrs.x + attrs.horizontalMargin * pw),
                        (int) (attrs.y + attrs.verticalMargin * ph), outFrame);

                // Now make sure the window fits in the overall display frame.
                Gravity.applyDisplay(attrs.gravity, outParentFrame, outFrame);
                Rect surfaceInsets = attrs.surfaceInsets;

                //TODO temp solution
                child.layout(outFrame.left - surfaceInsets.left, outFrame.top - surfaceInsets.top,
                        outFrame.right + surfaceInsets.right, outFrame.bottom + surfaceInsets.bottom);
            }
        }
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        if (getDescendantFocusability() == FOCUS_BLOCK_DESCENDANTS) {
            return;
        }
        mFocused = null;
        super.unFocus(focused);
        mFocused = child;
        if (mParent != null) {
            mParent.requestChildFocus(this, focused);
        }
    }

    @Override
    public void clearChildFocus(View child) {
        super.clearChildFocus(child);
        final int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            View c = getChildAt(i);
            if (c.hasFocus()) {
                mFocused = c;
                break;
            }
        }
    }

    @Override
    public void clearFocus() {
    }
}
