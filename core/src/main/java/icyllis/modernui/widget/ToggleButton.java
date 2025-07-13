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

import icyllis.modernui.R;
import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.core.Context;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.util.AttributeSet;

/**
 * A toggle button is a simple implementation of {@link CompoundButton}. This
 * does not provide a button drawable by default, and should be only used for
 * toggle buttons that have both an icon and text. You can set the icon via
 * {@link #setCompoundDrawablesRelative} or {@link #setButtonDrawable}
 * and set the text via {@link #setText(CharSequence)}. You can also
 * {@link #setTextOn(CharSequence)} and {@link #setTextOff(CharSequence)}
 * so that the text is synchronized with the checked state.
 * <p>
 * For a toggle button with only an icon, use {@link CheckableImageButton}.
 * If the icon is not needed, we recommend using {@link Switch}.
 *
 * @since 3.12
 */
public class ToggleButton extends CompoundButton {

    private CharSequence mTextOn;
    private CharSequence mTextOff;

    public ToggleButton(Context context) {
        this(context, null);
    }

    public ToggleButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.buttonStyle);
    }

    public ToggleButton(Context context, @Nullable AttributeSet attrs,
                        @Nullable @AttrRes ResourceId defStyleAttr) {
        this(context, attrs, defStyleAttr, null);
    }

    public ToggleButton(Context context, @Nullable AttributeSet attrs,
                        @Nullable @AttrRes ResourceId defStyleAttr,
                        @Nullable @StyleRes ResourceId defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);

        syncTextState();
    }

    private void syncTextState() {
        boolean checked = isChecked();
        if (checked && mTextOn != null) {
            setText(mTextOn);
        } else if (!checked && mTextOff != null) {
            setText(mTextOff);
        }
    }

    /**
     * Returns the text for when the button is in the checked state.
     *
     * @return The text.
     */
    @Nullable
    public CharSequence getTextOn() {
        return mTextOn;
    }

    /**
     * Sets the text for when the button is in the checked state.
     * If not null, it overrides the text previously set by {@link Button#setText}.
     *
     * @param textOn The text.
     */
    public void setTextOn(@Nullable CharSequence textOn) {
        mTextOn = textOn;
        if (isChecked() && mTextOn != null) {
            setText(mTextOn);
        }
    }

    /**
     * Returns the text for when the button is not in the checked state.
     *
     * @return The text.
     */
    @Nullable
    public CharSequence getTextOff() {
        return mTextOff;
    }

    /**
     * Sets the text for when the button is not in the checked state.
     * If not null, it overrides the text previously set by {@link Button#setText}.
     *
     * @param textOff The text.
     */
    public void setTextOff(@Nullable CharSequence textOff) {
        mTextOff = textOff;
        if (!isChecked() && mTextOff != null) {
            setText(mTextOff);
        }
    }
}
