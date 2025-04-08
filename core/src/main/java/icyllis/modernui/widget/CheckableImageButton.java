/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.core.Context;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.util.AttributeSet;
import org.jetbrains.annotations.ApiStatus;

/**
 * An extension to {@link ImageButton} that implements {@link Checkable} interface
 * and is conditionally checkable, see {@link #setCheckable(boolean)}. The checked
 * state is toggled on click.
 * <p>
 * This class is more lightweight than {@link CompoundButton} or ToggleButton
 * in implementing icon-only toggle buttons.
 *
 * @since 3.12
 */
public class CheckableImageButton extends ImageButton implements Checkable2 {

    private boolean mChecked;
    private boolean mCheckable = true;
    private boolean mBroadcasting;

    private OnCheckedChangeListener mOnCheckedChangeListener;
    private OnCheckedChangeListener mOnCheckedChangeListenerInternal;

    public CheckableImageButton(Context context) {
        super(context);
    }

    public CheckableImageButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, null);
    }

    public CheckableImageButton(Context context, @Nullable AttributeSet attrs,
                                @Nullable @AttrRes ResourceId defStyleAttr) {
        this(context, attrs, defStyleAttr, null);
    }

    public CheckableImageButton(Context context, @Nullable AttributeSet attrs,
                                @Nullable @AttrRes ResourceId defStyleAttr,
                                @Nullable @StyleRes ResourceId defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    @Override
    public boolean performClick() {
        toggle();
        return super.performClick();
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (isCheckable() && mChecked != checked) {
            mChecked = checked;
            refreshDrawableState();

            // Avoid infinite recursions if setChecked() is called from a listener
            if (mBroadcasting) {
                return;
            }

            mBroadcasting = true;
            if (mOnCheckedChangeListenerInternal != null) {
                mOnCheckedChangeListenerInternal.onCheckedChanged(this, mChecked);
            }
            if (mOnCheckedChangeListener != null) {
                mOnCheckedChangeListener.onCheckedChanged(this, mChecked);
            }
            mBroadcasting = false;
        }
    }

    /**
     * Returns whether this image button is checkable. By default,
     * this is true.
     *
     * @see #setCheckable(boolean)
     */
    public boolean isCheckable() {
        return mCheckable;
    }

    /**
     * Sets whether this image button is checkable. By default,
     * this is true.
     *
     * @param checkable Whether this button is checkable.
     */
    public void setCheckable(boolean checkable) {
        mCheckable = checkable;
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
     * Register a callback to be invoked when the checked state of this button
     * changes. This callback is used for internal purpose only.
     *
     * @param listener the callback to call on checked state change
     * @hidden
     */
    @ApiStatus.Internal
    public void setInternalOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
        mOnCheckedChangeListenerInternal = listener;
    }

    @NonNull
    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 2);
        if (isCheckable()) {
            mergeDrawableStates(drawableState, CompoundButton.CHECKABLE_STATE_SET);
        }
        if (isChecked()) {
            mergeDrawableStates(drawableState, CompoundButton.CHECKED_STATE_SET);
        }
        return drawableState;
    }
}
