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

package icyllis.modernui.view.menu;

import icyllis.modernui.R;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.material.MaterialCheckBox;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.resources.TypedValue;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.AbsListView;
import icyllis.modernui.widget.CheckBox;
import icyllis.modernui.widget.CompoundButton;
import icyllis.modernui.widget.ImageView;
import icyllis.modernui.widget.ImageView.ScaleType;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.RadioButton;
import icyllis.modernui.widget.TextView;
import org.jetbrains.annotations.ApiStatus;

import static icyllis.modernui.view.ViewGroup.LayoutParams.*;

/**
 * The item view for each item in the ListView-based MenuViews.
 */
// Modified from Android
@ApiStatus.Internal
public class ListMenuItemView extends LinearLayout
        implements MenuView.ItemView, AbsListView.SelectionBoundsAdjuster {

    private MenuItemImpl mItemData;

    private ImageView mIconView;
    private RadioButton mRadioButton;
    private CheckBox mCheckBox;
    private final TextView mTitleView;
    private final TextView mShortcutView;
    private final ImageView mSubMenuArrowView;
    private final LinearLayout mContent;

    private boolean mForceShowIcon;

    public ListMenuItemView(Context context) {
        super(context);
        setMinimumWidth(dp(196));
        setOrientation(VERTICAL);
        var divider = new ShapeDrawable();
        divider.setShape(ShapeDrawable.HLINE);
        divider.setSize(-1, dp(1));
        final TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(R.ns, R.attr.colorOutlineVariant, value, true))
            divider.setColor(value.data);
        setDividerDrawable(divider);
        setDividerPadding(dp(2));

        {
            mContent = new LinearLayout(getContext());
            mContent.setDuplicateParentStateEnabled(true);
            mContent.setPaddingRelative(dp(4), dp(2), dp(16), dp(2));

            // Checkbox, and/or radio button will be inserted here.

            // Icon will be inserted here.

            // The title and summary have some gap between them.
            {
                mTitleView = new TextView(getContext());
                mTitleView.setId(R.id.title);
                mTitleView.setTextAppearance(ResourceId.attr(R.ns, R.attr.textAppearanceBodyMedium));
                mTitleView.setSingleLine();
                mTitleView.setDuplicateParentStateEnabled(true);
                mTitleView.setTextAlignment(TEXT_ALIGNMENT_VIEW_START);

                var params = new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1);
                params.gravity = Gravity.CENTER_VERTICAL;
                params.setMarginStart(dp(16));
                mContent.addView(mTitleView, params);
            }

            {
                mShortcutView = new TextView(getContext());
                mShortcutView.setTextAppearance(ResourceId.attr(R.ns, R.attr.textAppearanceBodySmall));
                mShortcutView.setSingleLine();
                mShortcutView.setDuplicateParentStateEnabled(true);
                mShortcutView.setTextAlignment(TEXT_ALIGNMENT_VIEW_START);

                var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                params.gravity = Gravity.CENTER_VERTICAL;
                params.setMarginStart(dp(16));
                mContent.addView(mShortcutView, params);
            }

            {
                mSubMenuArrowView = new ImageView(getContext());
                mSubMenuArrowView.setScaleType(ScaleType.CENTER);
                mSubMenuArrowView.setVisibility(GONE);
                mSubMenuArrowView.setImageDrawable(new SubMenuArrowDrawable(context));
                if (context.getTheme().resolveAttribute(R.ns, R.attr.textColorSecondary, value, true))
                    mSubMenuArrowView.setImageTintList(context.getResources().loadColorStateList(value,
                            context.getTheme()));

                var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                params.gravity = Gravity.CENTER;
                params.setMarginStart(dp(8));
                mContent.addView(mSubMenuArrowView, params);
            }

            addView(mContent, MATCH_PARENT, WRAP_CONTENT);
        }

        setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
    }

    @Override
    public void initialize(@NonNull MenuItemImpl itemData, int menuType) {
        mItemData = itemData;

        setVisibility(itemData.isVisible() ? View.VISIBLE : View.GONE);

        setTitle(itemData.getTitleForItemView(this));
        setCheckable(itemData.isCheckable());
        setShortcut(itemData.shouldShowShortcut(), itemData.getShortcut());
        setIcon(itemData.getIcon());
        setEnabled(itemData.isEnabled());
        setSubMenuArrowVisible(itemData.hasSubMenu());
    }

    private void addContentView(View v) {
        addContentView(v, -1);
    }

    private void addContentView(View v, int index) {
        if (mContent != null) {
            mContent.addView(v, index);
        } else {
            addView(v, index);
        }
    }

    public void setForceShowIcon(boolean forceShow) {
        mForceShowIcon = forceShow;
    }

    @Override
    public void setTitle(@Nullable CharSequence title) {
        if (title != null) {
            mTitleView.setText(title);
            if (mTitleView.getVisibility() != VISIBLE) {
                mTitleView.setVisibility(VISIBLE);
            }
        } else {
            if (mTitleView.getVisibility() != GONE) {
                mTitleView.setVisibility(GONE);
            }
        }
    }

    @Override
    public MenuItemImpl getItemData() {
        return mItemData;
    }

    @Override
    public void setCheckable(boolean checkable) {
        if (!checkable && mRadioButton == null && mCheckBox == null) {
            return;
        }

        // Depending on whether its exclusive check or not, the checkbox or
        // radio button will be the one in use (and the other will be otherCompoundButton)
        final CompoundButton compoundButton;
        final CompoundButton otherCompoundButton;

        if (mItemData.isExclusiveCheckable()) {
            if (mRadioButton == null) {
                insertRadioButton();
            }
            compoundButton = mRadioButton;
            otherCompoundButton = mCheckBox;
        } else {
            if (mCheckBox == null) {
                insertCheckBox();
            }
            compoundButton = mCheckBox;
            otherCompoundButton = mRadioButton;
        }

        if (checkable) {
            compoundButton.setChecked(mItemData.isChecked());

            if (compoundButton.getVisibility() != VISIBLE) {
                compoundButton.setVisibility(VISIBLE);
            }

            // Make sure the other compound button isn't visible
            if (otherCompoundButton != null && otherCompoundButton.getVisibility() != GONE) {
                otherCompoundButton.setVisibility(GONE);
            }
        } else {
            if (mCheckBox != null) {
                mCheckBox.setVisibility(GONE);
            }
            if (mRadioButton != null) {
                mRadioButton.setVisibility(GONE);
            }
        }
    }

    @Override
    public void setChecked(boolean checked) {
        CompoundButton compoundButton;

        if (mItemData.isExclusiveCheckable()) {
            if (mRadioButton == null) {
                insertRadioButton();
            }
            compoundButton = mRadioButton;
        } else {
            if (mCheckBox == null) {
                insertCheckBox();
            }
            compoundButton = mCheckBox;
        }

        compoundButton.setChecked(checked);
    }

    private void setSubMenuArrowVisible(boolean hasSubmenu) {
        if (mSubMenuArrowView != null) {
            mSubMenuArrowView.setVisibility(hasSubmenu ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void setShortcut(boolean showShortcut, char shortcutKey) {
        final int newVisibility = (showShortcut && mItemData.shouldShowShortcut())
                ? VISIBLE : GONE;

        if (newVisibility == VISIBLE) {
            mShortcutView.setText(mItemData.getShortcutLabel());
        }

        if (mShortcutView.getVisibility() != newVisibility) {
            mShortcutView.setVisibility(newVisibility);
        }
    }

    @Override
    public void setIcon(Drawable icon) {
        final boolean showIcon = mItemData.shouldShowIcon() || mForceShowIcon;
        if (!showIcon) {
            return;
        }

        if (mIconView == null && icon == null && !mForceShowIcon) {
            return;
        }

        if (mIconView == null) {
            insertIconView();
        }

        if (icon != null || mForceShowIcon) {
            mIconView.setImageDrawable(icon);

            if (mIconView.getVisibility() != VISIBLE) {
                mIconView.setVisibility(VISIBLE);
            }
        } else {
            mIconView.setVisibility(GONE);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mIconView != null && mForceShowIcon) {
            // Enforce minimum icon spacing
            ViewGroup.LayoutParams lp = getLayoutParams();
            LayoutParams iconLp = (LayoutParams) mIconView.getLayoutParams();
            if (lp.height > 0 && iconLp.width <= 0) {
                iconLp.width = lp.height;
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void insertIconView() {
        mIconView = new ImageView(getContext());
        mIconView.setScaleType(ScaleType.CENTER_INSIDE);
        mIconView.setDuplicateParentStateEnabled(true);
        var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        params.setMarginsRelative(dp(8), dp(8), dp(-8), dp(8));
        mIconView.setLayoutParams(params);
        if (mRadioButton != null || mCheckBox != null) {
            addContentView(mIconView, 1);
        } else {
            addContentView(mIconView, 0);
        }
    }

    private void insertRadioButton() {
        mRadioButton = new RadioButton(getContext(), null, ResourceId.attr(R.ns, R.attr.radioButtonStyleMenuItem));
        mRadioButton.setFocusable(false);
        mRadioButton.setClickable(false);
        mRadioButton.setDuplicateParentStateEnabled(true);
        var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        mRadioButton.setLayoutParams(params);
        addContentView(mRadioButton, 0);
    }

    private void insertCheckBox() {
        //TODO
        mCheckBox = new MaterialCheckBox(getContext());
        mCheckBox.setFocusable(false);
        mCheckBox.setClickable(false);
        mCheckBox.setDuplicateParentStateEnabled(true);
        var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        mCheckBox.setLayoutParams(params);
        addContentView(mCheckBox, 0);
    }

    @Override
    public boolean prefersCondensedTitle() {
        return false;
    }

    @Override
    public boolean showsIcon() {
        return mForceShowIcon;
    }

    /**
     * Enable or disable group dividers for this view.
     */
    public void setGroupDividerEnabled(boolean groupDividerEnabled) {
        // If mHasListDivider is true, disabling the groupDivider.
        // Otherwise, checking enabling it according to groupDividerEnabled flag.
        if ((getShowDividers() == SHOW_DIVIDER_NONE) == groupDividerEnabled) {
            if (groupDividerEnabled) {
                setShowDividers(SHOW_DIVIDER_BEGINNING);
                setPadding(0, dp(2), 0, 0);
            } else {
                setShowDividers(SHOW_DIVIDER_NONE);
                setPadding(0, 0, 0, 0);
            }
        }
    }

    @Override
    public void adjustListItemSelectionBounds(@NonNull Rect rect) {
        rect.inset(dp(4), dp(2));
        if (getShowDividers() != SHOW_DIVIDER_NONE) {
            // groupDivider is a part of MenuItemListView.
            // If ListMenuItem with divider enabled is hovered/clicked, divider also gets selected.
            // Clipping the selector bounds from the top divider portion when divider is enabled,
            // so that divider does not get selected on hover or click.
            rect.top += dp(1 + 4);
        }
    }
}
