/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

import icyllis.modernui.core.Context;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * <p>This class is used to create a multiple-exclusion scope for a set of radio
 * buttons. Checking one radio button that belongs to a radio group unchecks
 * any previously checked radio button within the same group.</p>
 *
 * <p>Initially, all of the radio buttons are unchecked. While it is not possible
 * to uncheck a particular radio button, the radio group can be cleared to
 * remove the checked state.</p>
 *
 * @see RadioButton
 * @see RadioGroup
 */
public class RelativeRadioGroup extends RelativeLayout {

    // holds the checked id; the selection is empty by default
    private int mCheckedId = NO_ID;
    // tracks children radio buttons checked state
    private final Checkable.OnCheckedChangeListener mChildOnCheckedChangeListener = new CheckedStateTracker();
    // when true, mOnCheckedChangeListener discards events
    private boolean mProtectFromCheckedChange = false;
    @Nullable
    private OnCheckedChangeListener mOnCheckedChangeListener;

    public RelativeRadioGroup(Context context) {
        super(context);
    }

    @Override
    protected void onViewAdded(View child) {
        super.onViewAdded(child);
        if (child instanceof RadioButton button) {
            if (button.getId() == NO_ID) {
                button.setId(generateViewId());
            }
            if (button.isChecked()) {
                setCheckedStateForView(mCheckedId, false);
                setCheckedId(button.getId());
            }

            button.setOnCheckedChangeListener(mChildOnCheckedChangeListener);
        }
    }

    @Override
    protected void onViewRemoved(View child) {
        super.onViewRemoved(child);
        if (child instanceof RadioButton) {
            ((RadioButton) child).setOnCheckedChangeListener(null);
        }
    }

    /**
     * <p>Sets the selection to the radio button whose identifier is passed in
     * parameter. Using {@link #NO_ID} as the selection identifier clears the selection;
     * such an operation is equivalent to invoking {@link #clearCheck()}.</p>
     *
     * @param id the unique id of the radio button to select in this group
     * @see #getCheckedId()
     * @see #clearCheck()
     */
    public void check(int id) {
        // don't even bother
        if (id != NO_ID && (id == mCheckedId)) {
            return;
        }
        setCheckedStateForView(mCheckedId, false);
        setCheckedStateForView(id, true);

        setCheckedId(id);
    }

    private void setCheckedId(int id) {
        mCheckedId = id;

        if (mOnCheckedChangeListener != null) {
            mOnCheckedChangeListener.onCheckedChanged(this, mCheckedId);
        }
    }

    private void setCheckedStateForView(int viewId, boolean checked) {
        if (viewId == NO_ID) {
            return;
        }
        View checkedView = findViewById(viewId);
        if (checkedView instanceof RadioButton) {
            mProtectFromCheckedChange = true;
            ((RadioButton) checkedView).setChecked(checked);
            mProtectFromCheckedChange = false;
        }
    }

    /**
     * <p>Returns the identifier of the selected radio button in this group.
     * Upon empty selection, the returned value is {@link #NO_ID}.</p>
     *
     * @return the unique id of the selected radio button in this group
     * @see #check(int)
     * @see #clearCheck()
     */
    public final int getCheckedId() {
        return mCheckedId;
    }

    /**
     * <p>Clears the selection. When the selection is cleared, no radio button
     * in this group is selected and {@link #getCheckedId()} returns
     * {@link #NO_ID}.</p>
     *
     * @see #check(int)
     * @see #getCheckedId()
     */
    public final void clearCheck() {
        check(NO_ID);
    }

    /**
     * <p>Register a callback to be invoked when the checked radio button
     * changes in this group.</p>
     *
     * @param listener the callback to call on checked state change
     */
    public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }

    @Nonnull
    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    /**
     * <p>Interface definition for a callback to be invoked when the checked
     * radio button changed in this group.</p>
     */
    @FunctionalInterface
    public interface OnCheckedChangeListener {

        /**
         * <p>Called when the checked radio button has changed. When the
         * selection is cleared, checkedId is {@link #NO_ID}.</p>
         *
         * @param group     the group in which the checked radio button has changed
         * @param checkedId the unique identifier of the newly checked radio button
         */
        void onCheckedChanged(RelativeRadioGroup group, int checkedId);
    }

    private class CheckedStateTracker implements Checkable.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(View buttonView, boolean isChecked) {
            // prevents from infinite recursion
            if (!mProtectFromCheckedChange) {
                setCheckedStateForView(mCheckedId, false);
                setCheckedId(buttonView.getId());
            }
        }
    }
}
