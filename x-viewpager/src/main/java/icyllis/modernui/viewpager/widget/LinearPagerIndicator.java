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

package icyllis.modernui.viewpager.widget;

import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.*;
import icyllis.modernui.util.DataSetObserver;
import icyllis.modernui.view.View;

public class LinearPagerIndicator extends View {

    private float mLineWidth = 6;

    private ViewPager mPager;
    private PagerAdapter mAdapter;
    private int mPageCount;

    private final RectF mLineRect = new RectF();

    private class PageListener implements DataSetObserver,
            ViewPager.OnPageChangeListener,
            ViewPager.OnAdapterChangeListener {

        @Override
        public void onChanged() {
            if (mAdapter != null) {
                mPageCount = mAdapter.getCount();
            } else {
                mPageCount = 0;
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (mPageCount == 0)
                return;
            float segLength = (getWidth() - mLineWidth) / mPageCount;
            float left = position * segLength;
            float right = left + segLength + mLineWidth;

            float v = (getHeight() - mLineWidth) * 0.5f;

            mLineRect.set(
                    left + segLength * TimeInterpolator.DECELERATE.getInterpolation(positionOffset),
                    v,
                    right + segLength * TimeInterpolator.ACCELERATE.getInterpolation(positionOffset),
                    v + mLineWidth);
            invalidate();
        }

        @Override
        public void onAdapterChanged(@NonNull ViewPager viewPager,
                                     @Nullable PagerAdapter oldAdapter,
                                     @Nullable PagerAdapter newAdapter) {
            updateAdapter(oldAdapter, newAdapter);
        }
    }

    private final PageListener mPageListener = new PageListener();

    private int mLineColor = 0xF0AADCF0;

    public LinearPagerIndicator(Context context) {
        super(context);
    }

    public void setLineWidth(float lineWidth) {
        mLineWidth = lineWidth;
    }

    public float getLineWidth() {
        return mLineWidth;
    }

    public void setLineColor(int lineColor) {
        mLineColor = lineColor;
    }

    public int getLineColor() {
        return mLineColor;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        var paint = Paint.obtain();
        paint.setColor(mLineColor);
        canvas.drawRoundRect(mLineRect, mLineWidth / 2, paint);
        paint.recycle();
    }

    public void setPager(@Nullable ViewPager pager) {
        if (pager != null) {
            updateAdapter(mAdapter, pager.getAdapter());
            pager.setInternalPageChangeListener(mPageListener);
            pager.addOnAdapterChangeListener(mPageListener);
            mPager = pager;
        } else if (mPager != null) {
            updateAdapter(mAdapter, null);
            mPager.setInternalPageChangeListener(null);
            mPager.removeOnAdapterChangeListener(mPageListener);
            mPager = null;
        }
    }

    private void updateAdapter(@Nullable PagerAdapter oldAdapter,
                               @Nullable PagerAdapter newAdapter) {
        if (oldAdapter != null) {
            oldAdapter.unregisterDataSetObserver(mPageListener);
            mAdapter = null;
            mPageCount = 0;
        }
        if (newAdapter != null) {
            newAdapter.registerDataSetObserver(mPageListener);
            mAdapter = newAdapter;
            mPageCount = newAdapter.getCount();
        }
        invalidate();
    }
}
