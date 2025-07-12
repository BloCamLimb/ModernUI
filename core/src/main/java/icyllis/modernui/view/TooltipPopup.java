/*
 * Modern UI.
 * Copyright (C) 2023-2025 BloCamLimb. All rights reserved.
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
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *   Copyright (C) 2016 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package icyllis.modernui.view;

import icyllis.modernui.R;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.resources.TypedValue;
import icyllis.modernui.widget.TextView;
import org.jetbrains.annotations.ApiStatus;

// Modified from Android
@ApiStatus.Internal
public class TooltipPopup {

    private final Context mContext;

    private final TextView mTextView;
    private final WindowManager.LayoutParams mParams;
    private final int[] mTmpAnchorPos = new int[2];

    public TooltipPopup(Context context) {
        mContext = context;
        mTextView = new TextView(context);
        final Resources.Theme theme = context.getTheme();
        final TypedValue value = new TypedValue();
        if (theme.resolveAttribute(R.ns, R.attr.textAppearanceBodySmall, value, true))
            mTextView.setTextAppearance(value.getResourceId());
        if (theme.resolveAttribute(R.ns, R.attr.colorOnSurfaceInverse, value, true))
            mTextView.setTextColor(value.data);
        mTextView.setGravity(Gravity.CENTER_VERTICAL);
        mParams = new WindowManager.LayoutParams();
        mParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        mParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_ABOVE_SUB_PANEL;
        var background = new ShapeDrawable();
        background.setShape(ShapeDrawable.RECTANGLE);
        if (theme.resolveAttribute(R.ns, R.attr.colorSurfaceInverse, value, true))
            background.setColor(value.data);
        background.setCornerRadius(mTextView.dp(4));
        mTextView.setBackground(background);
    }

    public void show(View anchorView, int anchorX, int anchorY, boolean fromTouch,
                     CharSequence tooltipText) {
        if (isShowing()) {
            hide();
        }

        mTextView.setText(tooltipText);
        mTextView.setMaxWidth(Math.min(anchorView.getRootView().getMeasuredWidth() / 2, mTextView.dp(512)));
        mTextView.setMinHeight(mTextView.dp(24));
        mTextView.setPadding(mTextView.dp(8), mTextView.dp(4), mTextView.dp(8), mTextView.dp(4));

        computePosition(anchorView, anchorX, anchorY, fromTouch, mParams);

        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        assert wm != null;
        wm.addView(mTextView, mParams);
    }

    public void hide() {
        if (!isShowing()) {
            return;
        }

        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        assert wm != null;
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
