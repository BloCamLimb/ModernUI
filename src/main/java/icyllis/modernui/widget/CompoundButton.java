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

package icyllis.modernui.widget;

import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.Canvas;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A button with two states, checked and unchecked. When the button is pressed
 * or clicked, the state changes automatically.
 */
public abstract class CompoundButton extends Button implements ICheckable {

    private boolean mChecked;
    // broadcast check state changes to listeners, prevent dead loop
    private boolean mBroadcasting;

    private Drawable mButtonDrawable;

    private OnCheckedChangeListener mOnCheckedChangeListener;

    public CompoundButton() {
        setChecked(false);
    }

    @Override
    protected void onDraw(@Nonnull Canvas canvas) {
        final Drawable buttonDrawable = mButtonDrawable;
        super.onDraw(canvas);
        if (buttonDrawable != null) {
            buttonDrawable.draw(canvas);
            /*final int scrollX = mScrollX;
            final int scrollY = mScrollY;
            if (scrollX == 0 && scrollY == 0) {
                buttonDrawable.draw(canvas);
            } else {
                canvas.translate(scrollX, scrollY);
                buttonDrawable.draw(canvas);
                canvas.translate(-scrollX, -scrollY);
            }*/
        }
    }

    @Override
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
            refreshDrawableState();

            if (mBroadcasting) {
                return;
            }
            mBroadcasting = true;

            if (mOnCheckedChangeListener != null) {
                mOnCheckedChangeListener.onCheckedChanged(this, mChecked);
            }

            mBroadcasting = false;
        }
    }

    @Override
    public final boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    /**
     * Register a callback to be invoked when the checked state of this button
     * changes.
     *
     * @param listener the callback to call on checked state change
     */
    public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }

    /**
     * Sets a drawable as the compound button image.
     *
     * @param drawable the drawable to set
     */
    public void setButtonDrawable(@Nullable Drawable drawable) {
        if (mButtonDrawable != drawable) {
            mButtonDrawable = drawable;
        }
    }

    /**
     * @return the drawable used as the compound button image
     * @see #setButtonDrawable(Drawable)
     */
    @Nullable
    public Drawable getButtonDrawable() {
        return mButtonDrawable;
    }

    /**
     * Interface definition for a callback to be invoked when the checked state
     * of a compound button changed.
     */
    @FunctionalInterface
    public interface OnCheckedChangeListener {

        /**
         * Called when the checked state of a compound button has changed.
         *
         * @param buttonView The compound button view whose state has changed.
         * @param isChecked  The new checked state of buttonView.
         */
        void onCheckedChanged(CompoundButton buttonView, boolean isChecked);
    }
}
