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

package icyllis.modernui.widget;

import icyllis.modernui.R;
import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.core.Context;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.util.AttributeSet;
import org.intellij.lang.annotations.MagicConstant;

/**
 * <p>
 * A checkbox is a specific type of tri-states button that can be either
 * checked, unchecked, or indeterminate. The indeterminate state can only
 * be set programmatically.
 * </p>
 *
 * <p>See the <a href="https://developer.android.com/guide/topics/ui/controls/checkbox">Checkboxes</a>
 * guide.</p>
 */
public class CheckBox extends CompoundButton {

    /**
     * The unchecked state of the checkbox. A checkbox is unchecked by default.
     *
     * @see #setCheckedState(int)
     * @see #getCheckedState()
     */
    public static final int STATE_UNCHECKED = 0;

    /**
     * The checked state of the checkbox.
     *
     * @see #setCheckedState(int)
     * @see #getCheckedState()
     */
    public static final int STATE_CHECKED = 1;

    /**
     * The indeterminate state of the checkbox.
     *
     * @see #setCheckedState(int)
     * @see #getCheckedState()
     */
    public static final int STATE_INDETERMINATE = 2;

    private static final int[] INDETERMINATE_STATE_SET = {R.attr.state_indeterminate};

    @Nullable
    private OnCheckedStateChangeListener mOnCheckedStateChangeListener;

    private int mCheckedState;

    /**
     * Callback interface invoked when one of three independent checkbox states change.
     *
     * @see #setCheckedState(int)
     */
    @FunctionalInterface
    public interface OnCheckedStateChangeListener {

        /**
         * Called when the checked/indeterminate/unchecked state of a checkbox changes.
         *
         * @param checkBox the {@link CheckBox}
         * @param state    the new state of the checkbox
         */
        void onCheckedStateChanged(@NonNull CheckBox checkBox, @MagicConstant(intValues = {STATE_UNCHECKED,
                STATE_CHECKED, STATE_INDETERMINATE}) int state);
    }

    @AttrRes
    private static final ResourceId DEF_STYLE_ATTR =
            ResourceId.attr(R.ns, R.attr.checkboxStyle);

    public CheckBox(Context context) {
        this(context, null);
    }

    public CheckBox(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, DEF_STYLE_ATTR);
    }

    public CheckBox(Context context, @Nullable AttributeSet attrs,
                       @Nullable @AttrRes ResourceId defStyleAttr) {
        this(context, attrs, defStyleAttr, null);
    }

    public CheckBox(Context context, @Nullable AttributeSet attrs,
                       @Nullable @AttrRes ResourceId defStyleAttr,
                       @Nullable @StyleRes ResourceId defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void setChecked(boolean checked) {
        setCheckedState(checked ? STATE_CHECKED : STATE_UNCHECKED);
    }

    /**
     * Sets the CheckedState of the checkbox.
     *
     * @param checkedState the checked, unchecked, or indeterminate state to be set
     * @see #getCheckedState()
     */
    public void setCheckedState(@MagicConstant(intValues = {STATE_UNCHECKED,
            STATE_CHECKED, STATE_INDETERMINATE}) int checkedState) {
        if (mCheckedState != checkedState) {
            mCheckedState = checkedState;
            mChecked = mCheckedState == STATE_CHECKED;
            refreshDrawableState();

            // Avoid infinite recursions if setCheckedState is called from a listener.
            if (mBroadcasting) {
                return;
            }

            mBroadcasting = true;
            if (mOnCheckedStateChangeListener != null) {
                mOnCheckedStateChangeListener.onCheckedStateChanged(this, mCheckedState);
            }
            if (mCheckedState != STATE_INDETERMINATE && mOnCheckedChangeListener != null) {
                mOnCheckedChangeListener.onCheckedChanged(/* buttonView= */ this, mChecked);
            }

            mBroadcasting = false;
        }
    }

    /**
     * Returns the current checkbox state.
     *
     * @see #setCheckedState(int)
     */
    @MagicConstant(intValues = {STATE_UNCHECKED, STATE_CHECKED, STATE_INDETERMINATE})
    public int getCheckedState() {
        return mCheckedState;
    }

    /**
     * Register a {@link OnCheckedStateChangeListener} that will be invoked when the checkbox state
     * changes.
     *
     * @param listener the callback to call on checked state change
     */
    public void setOnCheckedStateChangeListener(@Nullable OnCheckedStateChangeListener listener) {
        mOnCheckedStateChangeListener = listener;
    }

    @NonNull
    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableStates = super.onCreateDrawableState(extraSpace + 1);

        if (getCheckedState() == STATE_INDETERMINATE) {
            mergeDrawableStates(drawableStates, INDETERMINATE_STATE_SET);
        }

        return drawableStates;
    }
}
