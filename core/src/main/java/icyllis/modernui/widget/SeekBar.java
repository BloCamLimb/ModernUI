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

package icyllis.modernui.widget;

import icyllis.modernui.R;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.drawable.*;
import icyllis.modernui.resources.SystemTheme;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.StateSet;
import icyllis.modernui.view.Gravity;

public class SeekBar extends AbsSeekBar {

    /**
     * A callback that notifies clients when the progress level has been
     * changed. This includes changes that were initiated by the user through a
     * touch gesture or arrow key/trackball as well as changes that were initiated
     * programmatically.
     */
    public interface OnSeekBarChangeListener {

        /**
         * Notification that the progress level has changed. Clients can use the fromUser parameter
         * to distinguish user-initiated changes from those that occurred programmatically.
         *
         * @param seekBar  The SeekBar whose progress has changed
         * @param progress The current progress level. This will be in the range min..max where min
         *                 and max were set by {@link ProgressBar#setMin(int)} and
         *                 {@link ProgressBar#setMax(int)}, respectively. (The default values for
         *                 min is 0 and max is 100.)
         * @param fromUser True if the progress change was initiated by the user.
         */
        default void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        }

        /**
         * Notification that the user has started a touch gesture. Clients may want to use this
         * to disable advancing the seekbar.
         *
         * @param seekBar The SeekBar in which the touch gesture began
         */
        default void onStartTrackingTouch(SeekBar seekBar) {
        }

        /**
         * Notification that the user has finished a touch gesture. Clients may want to use this
         * to re-enable advancing the seekbar.
         *
         * @param seekBar The SeekBar in which the touch gesture began
         */
        default void onStopTrackingTouch(SeekBar seekBar) {
        }
    }

    private OnSeekBarChangeListener mOnSeekBarChangeListener;

    public SeekBar(Context context) {
        super(context);

        setFocusable(true);

        if (getClass() == SeekBar.class) {
            {
                final Drawable track;
                {
                    var shape = new ShapeDrawable();
                    shape.setShape(ShapeDrawable.HLINE);
                    shape.setStroke(dp(2), SystemTheme.COLOR_CONTROL_ACTIVATED);
                    shape.setSize(-1, dp(2));
                    shape.setCornerRadius(1);
                    track = new ScaleDrawable(shape, Gravity.LEFT, 1, -1);
                }
                final Drawable secondaryTrack;
                {
                    var shape = new ShapeDrawable();
                    shape.setShape(ShapeDrawable.HLINE);
                    shape.setStroke(dp(2), SystemTheme.COLOR_FOREGROUND_DISABLED);
                    shape.setSize(-1, dp(2));
                    shape.setCornerRadius(1);
                    secondaryTrack = shape;
                }
                var progress = new LayerDrawable(secondaryTrack, track);
                progress.setId(0, R.id.secondaryProgress);
                progress.setId(1, R.id.progress);
                setProgressDrawable(progress);
            }
            {
                var thumb = new ShapeDrawable();
                thumb.setShape(ShapeDrawable.CIRCLE);
                thumb.setSize(dp(12), dp(12));
                thumb.setColor(SystemTheme.COLOR_CONTROL_ACTIVATED);
                int[][] stateSet = {
                        StateSet.get(StateSet.VIEW_STATE_PRESSED),
                        StateSet.get(StateSet.VIEW_STATE_HOVERED),
                        StateSet.WILD_CARD
                };
                int[] colors = {
                        0x80DDDDDD,
                        0x80DDDDDD,
                        0xFF808080
                };
                thumb.setStroke(dp(1.5f), new ColorStateList(stateSet, colors));
                thumb.setUseLevelForShape(false);
                setThumb(thumb);
            }
            setPadding(dp(16), 0, dp(16), 0);
        }
    }

    @Override
    void onProgressRefresh(float scale, boolean fromUser, int progress) {
        super.onProgressRefresh(scale, fromUser, progress);

        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onProgressChanged(this, progress, fromUser);
        }
    }

    /**
     * Sets a listener to receive notifications of changes to the SeekBar's progress level. Also
     * provides notifications of when the user starts and stops a touch gesture within the SeekBar.
     *
     * @param l The seek bar notification listener
     * @see SeekBar.OnSeekBarChangeListener
     */
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        mOnSeekBarChangeListener = l;
    }

    @Override
    void onStartTrackingTouch() {
        super.onStartTrackingTouch();
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onStartTrackingTouch(this);
        }
    }

    @Override
    void onStopTrackingTouch() {
        super.onStopTrackingTouch();
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onStopTrackingTouch(this);
        }
    }
}
