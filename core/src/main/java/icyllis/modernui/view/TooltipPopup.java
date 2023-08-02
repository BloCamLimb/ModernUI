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

import icyllis.modernui.app.Activity;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.widget.TextView;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class TooltipPopup {

    private final Context mContext;

    private final TextView mTextView;
    private final WindowManager.LayoutParams mParams;
    private final int[] mTmpAnchorPos = new int[2];

    public TooltipPopup(Context context) {
        mContext = context;
        mTextView = new TextView(context);
        mTextView.setTextColor(0xFFFFFFFF);
        mTextView.setTextSize(14);
        mParams = new WindowManager.LayoutParams();
        mParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        mParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_ABOVE_SUB_PANEL;
        var background = new ShapeDrawable();
        background.setShape(ShapeDrawable.RECTANGLE);
        background.setColor(0xE6313131);
        background.setCornerRadius(mTextView.dp(2));
        mTextView.setBackground(background);
    }

    public void show(View anchorView, int anchorX, int anchorY, boolean fromTouch,
                     CharSequence tooltipText) {
        if (isShowing()) {
            hide();
        }

        mTextView.setText(tooltipText);
        mTextView.setMaxWidth(Math.min(anchorView.getRootView().getMeasuredWidth() / 2, mTextView.dp(512)));
        mTextView.setPadding(mTextView.dp(16), mTextView.dp(6.5f), mTextView.dp(16), mTextView.dp(6.5f));

        computePosition(anchorView, anchorX, anchorY, fromTouch, mParams);

        WindowManager wm = ((Activity) mContext).getWindowManager();
        wm.addView(mTextView, mParams);
    }

    public void hide() {
        if (!isShowing()) {
            return;
        }

        WindowManager wm = ((Activity) mContext).getWindowManager();
        wm.removeView(mTextView);
    }

    public View getContentView() {
        return mTextView;
    }

    public boolean isShowing() {
        return mTextView.getParent() != null;
    }

    private void computePosition(View anchorView, int anchorX, int anchorY, boolean fromTouch,
                                 WindowManager.LayoutParams outParams) {

        final int tooltipPreciseAnchorThreshold = mTextView.dp(96);

        final int offsetX;
        if (anchorView.getWidth() >= tooltipPreciseAnchorThreshold) {
            // Wide view. Align the tooltip horizontally to the precise X position.
            offsetX = anchorX;
        } else {
            // Otherwise anchor the tooltip to the view center.
            offsetX = anchorView.getWidth() / 2;  // Center on the view horizontally.
        }

        final int offsetBelow;
        final int offsetAbove;
        if (anchorView.getHeight() >= tooltipPreciseAnchorThreshold) {
            // Tall view. Align the tooltip vertically to the precise Y position.
            final int offsetExtra = mTextView.dp(8);
            offsetBelow = anchorY + offsetExtra;
            offsetAbove = anchorY - offsetExtra;
        } else {
            // Otherwise anchor the tooltip to the view center.
            offsetBelow = anchorView.getHeight();  // Place below the view in most cases.
            offsetAbove = 0;  // Place above the view if the tooltip does not fit below.
        }

        outParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;

        final int tooltipOffset;
        if (fromTouch) {
            tooltipOffset = mTextView.dp(16);
        } else {
            tooltipOffset = 0;
        }

        View appView = anchorView.getRootView();

        anchorView.getLocationOnScreen(mTmpAnchorPos);

        outParams.x = mTmpAnchorPos[0] + offsetX - appView.getMeasuredWidth() / 2;

        final int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        mTextView.measure(spec, spec);
        final int tooltipHeight = mTextView.getMeasuredHeight();

        final int yAbove = mTmpAnchorPos[1] + offsetAbove - tooltipOffset - tooltipHeight;
        final int yBelow = mTmpAnchorPos[1] + offsetBelow + tooltipOffset;
        if (fromTouch) {
            if (yAbove >= 0) {
                outParams.y = yAbove;
            } else {
                outParams.y = yBelow;
            }
        } else {
            if (yBelow + tooltipHeight <= appView.getMeasuredHeight()) {
                outParams.y = yBelow;
            } else {
                outParams.y = yAbove;
            }
        }
    }
}
