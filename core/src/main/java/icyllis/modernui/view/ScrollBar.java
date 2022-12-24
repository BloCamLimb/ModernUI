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

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.Rect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class ScrollBar extends Drawable implements Drawable.Callback {

    private Drawable mVerticalTrack;
    private Drawable mVerticalThumb;

    private Drawable mHorizontalTrack;
    private Drawable mHorizontalThumb;

    private int mRange;
    private int mOffset;
    private int mExtent;

    private boolean mVertical;
    private boolean mBoundsChanged;
    private boolean mRangeChanged;

    private boolean mAlwaysDrawHorizontalTrack;
    private boolean mAlwaysDrawVerticalTrack;

    private int mAlpha = 255;
    private boolean mHasSetAlpha;

    ScrollBar() {
    }

    /**
     * Indicate whether the horizontal scrollbar track should always be drawn
     * regardless of the extent. Defaults to false.
     *
     * @param alwaysDrawTrack Whether the track should always be drawn
     * @see #getAlwaysDrawHorizontalTrack()
     */
    public void setAlwaysDrawHorizontalTrack(boolean alwaysDrawTrack) {
        mAlwaysDrawHorizontalTrack = alwaysDrawTrack;
    }

    /**
     * Indicate whether the vertical scrollbar track should always be drawn
     * regardless of the extent. Defaults to false.
     *
     * @param alwaysDrawTrack Whether the track should always be drawn
     * @see #getAlwaysDrawVerticalTrack()
     */
    public void setAlwaysDrawVerticalTrack(boolean alwaysDrawTrack) {
        mAlwaysDrawVerticalTrack = alwaysDrawTrack;
    }

    /**
     * @return whether the vertical scrollbar track should always be drawn
     * regardless of the extent.
     * @see #setAlwaysDrawVerticalTrack(boolean)
     */
    public boolean getAlwaysDrawVerticalTrack() {
        return mAlwaysDrawVerticalTrack;
    }

    /**
     * @return whether the horizontal scrollbar track should always be drawn
     * regardless of the extent.
     * @see #setAlwaysDrawHorizontalTrack(boolean)
     */
    public boolean getAlwaysDrawHorizontalTrack() {
        return mAlwaysDrawHorizontalTrack;
    }

    public void setParameters(int range, int offset, int extent, boolean vertical) {
        if (mVertical != vertical) {
            mVertical = vertical;

            mBoundsChanged = true;
        }

        if (mRange != range || mOffset != offset || mExtent != extent) {
            mRange = range;
            mOffset = offset;
            mExtent = extent;

            mRangeChanged = true;
        }
    }

    @Override
    protected void onBoundsChange(@Nonnull Rect bounds) {
        super.onBoundsChange(bounds);
        mBoundsChanged = true;
    }

    @Override
    public void draw(@Nonnull Canvas canvas) {
        final Rect r = getBounds();
        if (canvas.quickReject(r.left, r.top, r.right, r.bottom)) {
            return;
        }

        final boolean vertical = mVertical;
        final int extent = mExtent;
        final int range = mRange;

        boolean drawTrack = true;
        boolean drawThumb = true;
        if (extent <= 0 || range <= extent) {
            drawTrack = vertical ? mAlwaysDrawVerticalTrack : mAlwaysDrawHorizontalTrack;
            drawThumb = false;
        }

        if (drawTrack) {
            drawTrack(canvas, r, vertical);
        }

        if (drawThumb) {
            final int barLength = vertical ? r.height() : r.width();
            final int thickness = vertical ? r.width() : r.height();
            final int thumbLength =
                    Math.max(Math.round((float) barLength * extent / range), thickness * 2);
            final int thumbOffset =
                    Math.min(Math.round((float) (barLength - thumbLength) * mOffset / (range - extent)),
                            barLength - thumbLength);

            drawThumb(canvas, r, thumbOffset, thumbLength, vertical);
        }
    }

    private void drawTrack(Canvas canvas, Rect bounds, boolean vertical) {
        final Drawable track;
        if (vertical) {
            track = mVerticalTrack;
        } else {
            track = mHorizontalTrack;
        }

        if (track != null) {
            if (mBoundsChanged) {
                track.setBounds(bounds);
            }
            track.draw(canvas);
        }
    }

    private void drawThumb(Canvas canvas, Rect bounds, int offset, int length, boolean vertical) {
        final boolean changed = mRangeChanged || mBoundsChanged;
        if (vertical) {
            if (mVerticalThumb != null) {
                final Drawable thumb = mVerticalThumb;
                if (changed) {
                    thumb.setBounds(bounds.left, bounds.top + offset,
                            bounds.right, bounds.top + offset + length);
                }

                thumb.draw(canvas);
            }
        } else {
            if (mHorizontalThumb != null) {
                final Drawable thumb = mHorizontalThumb;
                if (changed) {
                    thumb.setBounds(bounds.left + offset, bounds.top,
                            bounds.left + offset + length, bounds.bottom);
                }

                thumb.draw(canvas);
            }
        }
    }

    public int getSize(boolean vertical) {
        if (vertical) {
            return mVerticalTrack != null ? mVerticalTrack.getIntrinsicWidth() :
                    mVerticalThumb != null ? mVerticalThumb.getIntrinsicWidth() : 0;
        } else {
            return mHorizontalTrack != null ? mHorizontalTrack.getIntrinsicHeight() :
                    mHorizontalThumb != null ? mHorizontalThumb.getIntrinsicHeight() : 0;
        }
    }

    @Nullable
    public Drawable getVerticalTrackDrawable() {
        return mVerticalTrack;
    }

    @Nullable
    public Drawable getVerticalThumbDrawable() {
        return mVerticalThumb;
    }

    @Nullable
    public Drawable getHorizontalTrackDrawable() {
        return mHorizontalTrack;
    }

    @Nullable
    public Drawable getHorizontalThumbDrawable() {
        return mHorizontalThumb;
    }

    public void setVerticalThumbDrawable(@Nullable Drawable thumb) {
        if (mVerticalThumb != null) {
            mVerticalThumb.setCallback(null);
        }

        propagateCurrentState(thumb);
        mVerticalThumb = thumb;
    }

    public void setVerticalTrackDrawable(@Nullable Drawable track) {
        if (mVerticalTrack != null) {
            mVerticalTrack.setCallback(null);
        }

        propagateCurrentState(track);
        mVerticalTrack = track;
    }

    public void setHorizontalThumbDrawable(@Nullable Drawable thumb) {
        if (mHorizontalThumb != null) {
            mHorizontalThumb.setCallback(null);
        }

        propagateCurrentState(thumb);
        mHorizontalThumb = thumb;
    }

    public void setHorizontalTrackDrawable(@Nullable Drawable track) {
        if (mHorizontalTrack != null) {
            mHorizontalTrack.setCallback(null);
        }

        propagateCurrentState(track);
        mHorizontalTrack = track;
    }

    private void propagateCurrentState(@Nullable Drawable d) {
        if (d != null) {
            d.setCallback(this);

            if (mHasSetAlpha) {
                d.setAlpha(mAlpha);
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mAlpha = alpha;
        mHasSetAlpha = true;

        if (mVerticalTrack != null) {
            mVerticalTrack.setAlpha(alpha);
        }
        if (mVerticalThumb != null) {
            mVerticalThumb.setAlpha(alpha);
        }
        if (mHorizontalTrack != null) {
            mHorizontalTrack.setAlpha(alpha);
        }
        if (mHorizontalThumb != null) {
            mHorizontalThumb.setAlpha(alpha);
        }
    }

    @Override
    public int getAlpha() {
        return mAlpha;
    }

    @Override
    public void invalidateDrawable(@Nonnull Drawable drawable) {
        invalidateSelf();
    }

    @Override
    public void scheduleDrawable(@Nonnull Drawable who, @Nonnull Runnable what, long when) {
        scheduleSelf(what, when);
    }

    @Override
    public void unscheduleDrawable(@Nonnull Drawable who, @Nonnull Runnable what) {
        unscheduleSelf(what);
    }
}
