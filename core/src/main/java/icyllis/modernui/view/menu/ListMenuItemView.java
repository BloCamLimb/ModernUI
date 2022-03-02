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

package icyllis.modernui.view.menu;

import icyllis.modernui.R;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.material.MaterialCheckBox;
import icyllis.modernui.material.MaterialRadioButton;
import icyllis.modernui.math.Rect;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.StateSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.*;
import icyllis.modernui.widget.ImageView.ScaleType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * The item view for each item in the ListView-based MenuViews.
 */
public class ListMenuItemView extends LinearLayout implements MenuView.ItemView, AbsListView.SelectionBoundsAdjuster {

    private static final ColorStateList TEXT_COLOR = new ColorStateList(
            new int[][]{
                    StateSet.get(StateSet.VIEW_STATE_ENABLED),
                    StateSet.WILD_CARD
            }, new int[]{
            0xFFFFFFFF,
            0xFF808080
    });

    private MenuItemImpl mItemData;

    private ImageView mIconView;
    private RadioButton mRadioButton;
    private CheckBox mCheckBox;
    private final TextView mTitleView;
    private final TextView mShortcutView;
    private final ImageView mSubMenuArrowView;
    private final LinearLayout mContent;

    private boolean mForceShowIcon;

    public ListMenuItemView() {
        setMinimumWidth(dp(196));
        setOrientation(VERTICAL);
        setDividerDrawable(new Drawable() {
            @Override
            public void draw(@Nonnull Canvas canvas) {
                Paint paint = Paint.take();
                paint.setRGBA(255, 255, 255, 32);
                canvas.drawRect(getBounds(), paint);
            }

            @Override
            public int getIntrinsicHeight() {
                return dp(1);
            }
        });
        setDividerPadding(dp(2));

        {
            mContent = new LinearLayout();
            mContent.setDuplicateParentStateEnabled(true);
            mContent.setPaddingRelative(dp(4), dp(2), dp(16), dp(2));

            // Checkbox, and/or radio button will be inserted here.

            // Icon will be inserted here.

            // The title and summary have some gap between them.
            {
                mTitleView = new TextView();
                mTitleView.setId(R.id.title);
                mTitleView.setTextSize(16);
                mTitleView.setTextColor(TEXT_COLOR);
                mTitleView.setSingleLine();
                mTitleView.setDuplicateParentStateEnabled(true);
                mTitleView.setTextAlignment(TEXT_ALIGNMENT_VIEW_START);

                var params = new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1);
                params.gravity = Gravity.CENTER_VERTICAL;
                params.setMarginStart(dp(16));
                mContent.addView(mTitleView, params);
            }

            {
                mShortcutView = new TextView();
                mShortcutView.setTextSize(14);
                mShortcutView.setTextColor(0xFFCECECE);
                mShortcutView.setSingleLine();
                mShortcutView.setDuplicateParentStateEnabled(true);
                mShortcutView.setTextAlignment(TEXT_ALIGNMENT_VIEW_START);

                var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                params.gravity = Gravity.CENTER_VERTICAL;
                params.setMarginStart(dp(16));
                mContent.addView(mShortcutView, params);
            }

            {
                mSubMenuArrowView = new ImageView();
                mSubMenuArrowView.setScaleType(ScaleType.CENTER);
                mSubMenuArrowView.setVisibility(GONE);
                mSubMenuArrowView.setImageDrawable(new SubMenuArrowDrawable());

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
    public void initialize(@Nonnull MenuItemImpl itemData, int menuType) {
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
        mIconView = new ImageView();
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
        mRadioButton = new MaterialRadioButton();
        mRadioButton.setFocusable(false);
        mRadioButton.setClickable(false);
        mRadioButton.setDuplicateParentStateEnabled(true);
        var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        mRadioButton.setLayoutParams(params);
        addContentView(mRadioButton, 0);
    }

    private void insertCheckBox() {
        mCheckBox = new MaterialCheckBox();
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
    public void adjustListItemSelectionBounds(@Nonnull Rect rect) {
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
