/*
 * Modern UI.
 * Copyright (C) 2022-2025 BloCamLimb. All rights reserved.
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
 *   Copyright (C) 2006 The Android Open Source Project
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

package icyllis.modernui.widget;

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Context;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/**
 * <p>This class is used to create a multiple-exclusion scope for a set of radio
 * buttons. Checking one radio button that belongs to a radio group unchecks
 * any previously checked radio button within the same group. So this group only
 * allows a single button to be checked.</p>
 *
 * <p>Initially, all of the radio buttons are unchecked. If all child views are
 * {@link RadioButton}, it is not possible to uncheck a particular radio button,
 * the radio group can be cleared via {@link #clearCheck()} to remove the checked
 * state. If at least one of the non-radio buttons must be selected, use
 * {@link #setSelectionRequired(boolean)}.</p>
 *
 * <p>The selection is identified by the unique id of the radio button as defined
 * by {@link View#setId(int)}.</p>
 *
 * <p>On version 3.12 and above, all {@link Checkable2} subclasses can be added to
 * a radio group and will behave like radio buttons, such as ToggleButton,
 * {@link CheckableImageButton}, and even {@link CheckBox}. However, it is most
 * optimal to add {@link RadioButton} to this group.</p>
 *
 * @see RadioButton
 */
// Based on Android and optimized by Modern UI
public class RadioGroup extends LinearLayout {

    private static final Marker MARKER = MarkerManager.getMarker("RadioGroup");

    // holds the checked id; the selection is empty by default
    private int mCheckedId = NO_ID;
    private boolean mSelectionRequired = false;
    // when true, mOnCheckedChangeListener discards events
    private boolean mProtectFromCheckedChange = false;
    @Nullable
    private OnCheckedChangeListener mOnCheckedChangeListener;
    // tracks children radio buttons checked state
    private final Checkable.OnCheckedChangeListener mChildOnCheckedChangeListener = (buttonView, isChecked) -> {
        // prevents from infinite recursion
        if (!mProtectFromCheckedChange) {
            int buttonId = buttonView.getId();
            if (buttonId == NO_ID) {
                ModernUI.LOGGER.error(MARKER, "Button ID is not valid: {}", buttonView);
                return;
            }
            if (isChecked && buttonId != mCheckedId) {
                setCheckedStateForView(mCheckedId, false);
                setCheckedId(buttonId);
            } else if (!isChecked && mSelectionRequired && buttonId == mCheckedId) {
                // It's the only checked item, cannot be unchecked if selection is required
                // No need to prevent infinite recursion here
                ((Checkable2) buttonView).setChecked(true);
            }
        }
    };

    public RadioGroup(Context context) {
        super(context);
        setOrientation(VERTICAL);
    }

    @Override
    protected void onViewAdded(View child) {
        super.onViewAdded(child);
        if (child instanceof Checkable2 button) {
            // generates an id if it's missing
            int buttonId = child.getId();
            if (buttonId == NO_ID) {
                buttonId = View.generateViewId();
                child.setId(buttonId);
            }
            if (button.isChecked() && buttonId != mCheckedId) {
                setCheckedStateForView(mCheckedId, false);
                setCheckedId(buttonId);
            }

            button.setInternalOnCheckedChangeListener(mChildOnCheckedChangeListener);
        }
    }

    @Override
    protected void onViewRemoved(View child) {
        super.onViewRemoved(child);
        if (child instanceof Checkable2) {
            ((Checkable2) child).setInternalOnCheckedChangeListener(null);
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
        if (checkedView instanceof Checkable2) {
            mProtectFromCheckedChange = true;
            ((Checkable2) checkedView).setChecked(checked);
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
     * Sets whether we prevent all child buttons from being deselected.
     * If a child is not {@link RadioButton}, it may be unchecked by user,
     * by setting this to true to prevent it from being unchecked.
     * This is false by default.
     */
    public void setSelectionRequired(boolean selectionRequired) {
        mSelectionRequired = selectionRequired;
    }

    /**
     * Returns whether we prevent all child buttons from being deselected.
     * This is false by default.
     *
     * @see #setSelectionRequired(boolean)
     */
    public boolean isSelectionRequired() {
        return mSelectionRequired;
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

    @NonNull
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
        void onCheckedChanged(RadioGroup group, int checkedId);
    }
}
