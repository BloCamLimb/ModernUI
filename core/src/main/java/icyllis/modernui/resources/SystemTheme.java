/*
 * Modern UI.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.resources;

import icyllis.modernui.R;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Color;
import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.ColorDrawable;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.drawable.LayerDrawable;
import icyllis.modernui.graphics.drawable.RippleDrawable;
import icyllis.modernui.graphics.drawable.ScaleDrawable;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.material.drawable.ButtonRadioDrawable;
import icyllis.modernui.material.drawable.SeekbarThumbDrawable;
import icyllis.modernui.material.drawable.SwitchThumbDrawable;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.SparseArray;
import icyllis.modernui.util.StateSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.RadioButton;
import icyllis.modernui.widget.SeekBar;
import icyllis.modernui.widget.Spinner;
import icyllis.modernui.widget.Switch;
import icyllis.modernui.widget.TextView;

import java.util.Objects;

/**
 * Temp use.
 */
public class SystemTheme {

    public static final int COLOR_FOREGROUND = 0xFFFFFFFF;
    public static final int COLOR_FOREGROUND_NORMAL = 0xFFB0B0B0;
    public static final int COLOR_FOREGROUND_DISABLED = 0xFF3F3F3F;

    public static final float DISABLED_ALPHA = 0.3f;
    public static final float PRIMARY_CONTENT_ALPHA = 1;
    public static final float SECONDARY_CONTENT_ALPHA = 0.7f;

    public static final int COLOR_CONTROL_ACTIVATED = 0xffcda398;

    public static int modulateColor(int baseColor, float alphaMod) {
        if (alphaMod == 1.0f) {
            return baseColor;
        }

        final int baseAlpha = Color.alpha(baseColor);
        final int alpha = MathUtil.clamp((int) (baseAlpha * alphaMod + 0.5f), 0, 255);

        return (baseColor & 0xFFFFFF) | (alpha << 24);
    }

    private static volatile SystemTheme currentTheme;

    @NonNull
    public static SystemTheme currentTheme() {
        return currentTheme;
    }

    public static void setCurrentTheme(@NonNull SystemTheme t) {
        currentTheme = Objects.requireNonNull(t);
    }

    public static void setToMaterialDark() {
        setCurrentTheme(createMaterial(true));
    }

    public static void setToMaterialLight() {
        setCurrentTheme(createMaterial(false));
    }

    public static void setToDefaultDark() {
        setCurrentTheme(createDefault(true, 2));
    }

    public static void setToDefaultLight() {
        setCurrentTheme(createDefault(false, 0));
    }

    static {
        setToDefaultDark();
    }

    public int
            colorPrimary,
            colorOnPrimary,
            colorPrimaryInverse,
            colorPrimaryContainer,
            colorOnPrimaryContainer,
            colorPrimaryFixed,
            colorPrimaryFixedDim,
            colorOnPrimaryFixed,
            colorOnPrimaryFixedVariant,
            colorSecondary,
            colorOnSecondary,
            colorSecondaryContainer,
            colorOnSecondaryContainer,
            colorSecondaryFixed,
            colorSecondaryFixedDim,
            colorOnSecondaryFixed,
            colorOnSecondaryFixedVariant,
            colorTertiary,
            colorOnTertiary,
            colorTertiaryContainer,
            colorOnTertiaryContainer,
            colorTertiaryFixed,
            colorTertiaryFixedDim,
            colorOnTertiaryFixed,
            colorOnTertiaryFixedVariant,
            colorBackground,
            colorOnBackground,
            colorSurface,
            colorOnSurface,
            colorSurfaceVariant,
            colorOnSurfaceVariant,
            colorSurfaceInverse,
            colorOnSurfaceInverse,
            colorSurfaceBright,
            colorSurfaceDim,
            colorSurfaceContainer,
            colorSurfaceContainerLow,
            colorSurfaceContainerHigh,
            colorSurfaceContainerLowest,
            colorSurfaceContainerHighest,
            colorOutline,
            colorOutlineVariant,
            colorError,
            colorOnError,
            colorErrorContainer,
            colorOnErrorContainer;
    public ColorStateList
            textColorPrimary,
            textColorPrimaryInverse,
            textColorSecondary,
            textColorSecondaryInverse,
            textColorTertiary,
            textColorTertiaryInverse,
            textColorPrimaryDisableOnly,
            textColorPrimaryInverseDisableOnly,
            textColorHint,
            textColorHintInverse;
    public int
            textColorHighlight,
            textColorHighlightInverse;
    public ColorStateList
            textColorLink,
            textColorLinkInverse,
            textColorAlertDialogListItem;
    public boolean isDark;

    private SparseArray<ColorStateList> text_button_foreground_color_selector;
    private ColorStateList text_button_foreground_color_selector(int colorOnContainer) {
        if (text_button_foreground_color_selector == null) {
            text_button_foreground_color_selector = new SparseArray<>();
        }
        var csl = text_button_foreground_color_selector.get(colorOnContainer);
        if (csl != null) {
            return csl;
        }
        csl = new ColorStateList(
                new int[][]{
                        new int[]{-R.attr.state_enabled},
                        new int[]{-R.attr.state_checkable},
                        new int[]{R.attr.state_checked},
                        StateSet.WILD_CARD
                },
                new int[]{
                        modulateColor(colorOnSurface, 0.38f),
                        colorOnContainer,
                        colorOnSecondaryContainer,
                        colorOnSurface
                }
        );
        text_button_foreground_color_selector.put(colorOnContainer, csl);
        return csl;
    }

    private SparseArray<ColorStateList> text_button_background_color_selector;
    private ColorStateList text_button_background_color_selector(int colorContainer) {
        if (text_button_background_color_selector == null) {
            text_button_background_color_selector = new SparseArray<>();
        }
        var csl = text_button_background_color_selector.get(colorContainer);
        if (csl != null) {
            return csl;
        }
        csl = new ColorStateList(
                new int[][]{
                        new int[]{R.attr.state_enabled, R.attr.state_checked},
                        StateSet.WILD_CARD
                },
                new int[]{
                        colorSecondaryContainer,
                        colorContainer
                }
        );
        text_button_background_color_selector.put(colorContainer, csl);
        return csl;
    }

    private SparseArray<ColorStateList> text_button_ripple_color_selector;
    private ColorStateList text_button_ripple_color_selector(int colorOnContainer) {
        if (text_button_ripple_color_selector == null) {
            text_button_ripple_color_selector = new SparseArray<>();
        }
        var csl = text_button_ripple_color_selector.get(colorOnContainer);
        if (csl != null) {
            return csl;
        }
        csl = new ColorStateList(
                new int[][]{
                    new int[]{-R.attr.state_checkable, R.attr.state_pressed},
                    new int[]{-R.attr.state_checkable, R.attr.state_focused},
                    new int[]{-R.attr.state_checkable, R.attr.state_hovered},
                    new int[]{-R.attr.state_checkable},

                    new int[]{R.attr.state_checked, R.attr.state_pressed},
                    new int[]{R.attr.state_checked, R.attr.state_focused},
                    new int[]{R.attr.state_checked, R.attr.state_hovered},
                    new int[]{R.attr.state_checked},

                    new int[]{R.attr.state_pressed},
                    new int[]{R.attr.state_focused},
                    new int[]{R.attr.state_hovered},
                    StateSet.WILD_CARD
                },
                new int[]{
                        modulateColor(colorOnContainer, 0.1f),
                        modulateColor(colorOnContainer, 0.102f),
                        modulateColor(colorOnContainer, 0.08f),
                        modulateColor(colorOnContainer, 0.064f),

                        modulateColor(colorOnSurface, 0.1f),
                        modulateColor(colorOnSecondaryContainer, 0.102f),
                        modulateColor(colorOnSecondaryContainer, 0.08f),
                        modulateColor(colorOnSecondaryContainer, 0.064f),

                        modulateColor(colorOnSecondaryContainer, 0.1f),
                        modulateColor(colorOnSurface, 0.102f),
                        modulateColor(colorOnSurface, 0.08f),
                        modulateColor(colorOnSurface, 0.064f),
                }
        );
        text_button_ripple_color_selector.put(colorOnContainer, csl);
        return csl;
    }

    private Drawable createButtonBackground() {
        return null;
    }

    public void applyTextAppearanceLabelLarge(TextView tv) {
        tv.setTextStyle(Paint.BOLD);
        tv.setTextSize(14);
        tv.setTextColor(textColorPrimary);
        tv.setHintTextColor(textColorHint);
        tv.setHighlightColor(textColorHighlight);
        tv.setLinkTextColor(textColorLink);
    }

    public void applyTextAppearanceBodyLarge(TextView tv) {
        tv.setTextStyle(Paint.NORMAL);
        tv.setTextSize(16);
        tv.setTextColor(textColorPrimary);
        tv.setHintTextColor(textColorHint);
        tv.setHighlightColor(textColorHighlight);
        tv.setLinkTextColor(textColorLink);
    }

    public void applyTextAppearanceBodyMedium(TextView tv) {
        tv.setTextStyle(Paint.NORMAL);
        tv.setTextSize(14);
        tv.setTextColor(textColorPrimary);
        tv.setHintTextColor(textColorHint);
        tv.setHighlightColor(textColorHighlight);
        tv.setLinkTextColor(textColorLink);
    }

    public void applyTextAppearanceBodySmall(TextView tv) {
        tv.setTextStyle(Paint.NORMAL);
        tv.setTextSize(12);
        tv.setTextColor(textColorSecondary);
        tv.setHintTextColor(textColorHint);
        tv.setHighlightColor(textColorHighlight);
        tv.setLinkTextColor(textColorLink);
    }

    public void applyTextButtonStyle(Button btn) {
        btn.setMinHeight(btn.dp(32));
        btn.setMinWidth(btn.dp(80));
        btn.setMaxWidth(btn.dp(320));
        btn.setPadding(btn.dp(12), btn.dp(6), btn.dp(12), btn.dp(6));
        applyTextAppearanceLabelLarge(btn);

        btn.setTextColor(text_button_foreground_color_selector(colorPrimary));
        var backgroundTint = text_button_background_color_selector(0);
        var rippleColor = text_button_ripple_color_selector(colorPrimary);

        var background = new ShapeDrawable();
        background.setCornerRadius(btn.dp(20));
        background.setTintList(backgroundTint);
        var rippleDrawable = new RippleDrawable(rippleColor, background, null);
        btn.setBackground(rippleDrawable);
    }

    private ColorStateList radioButtonTint;
    private ColorStateList radioButtonTint() {
        if (radioButtonTint != null) {
            return radioButtonTint;
        }
        radioButtonTint = new ColorStateList(
                new int[][]{
                        new int[]{-R.attr.state_enabled},
                        new int[]{R.attr.state_checked},
                        StateSet.WILD_CARD
                },
                new int[]{
                        modulateColor(colorOnSurface, 0.38f),
                        colorPrimary,
                        colorOnSurfaceVariant
                }
        );
        return radioButtonTint;
    }

    private ColorStateList colorControlHighlight;
    public ColorStateList colorControlHighlight() {
        if (colorControlHighlight != null) {
            return colorControlHighlight;
        }
        colorControlHighlight = new ColorStateList(
                new int[][]{
                        new int[]{R.attr.state_enabled, R.attr.state_checked},
                        new int[]{R.attr.state_activated},
                        StateSet.WILD_CARD
                },
                new int[]{
                        modulateColor(colorSecondary, 0.2f),
                        modulateColor(colorSecondary, 0.2f),
                        isDark ? 0x33ffffff : 0x1f000000
                }
        );
        return colorControlHighlight;
    }

    // reused for checkbox
    private ColorStateList radioButtonRippleTint;
    private ColorStateList radioButtonRippleTint() {
        if (radioButtonRippleTint != null) {
            return radioButtonRippleTint;
        }
        radioButtonRippleTint = new ColorStateList(
            new int[][]{
                    new int[]{R.attr.state_checked, R.attr.state_pressed},
                    new int[]{R.attr.state_checked, R.attr.state_focused},
                    new int[]{R.attr.state_checked, R.attr.state_hovered},
                    new int[]{R.attr.state_checked},

                    new int[]{R.attr.state_pressed},
                    new int[]{R.attr.state_focused},
                    new int[]{R.attr.state_hovered},
                    StateSet.WILD_CARD
            },
                new int[]{
                        modulateColor(colorOnSurface, 0.1f),
                        modulateColor(colorPrimary, 0.102f),
                        modulateColor(colorPrimary, 0.08f),
                        modulateColor(colorPrimary, 0.064f),

                        modulateColor(colorPrimary, 0.1f),
                        modulateColor(colorOnSurface, 0.102f),
                        modulateColor(colorOnSurface, 0.08f),
                        modulateColor(colorOnSurface, 0.064f)
                }
        );
        return radioButtonRippleTint;
    }

    /**
     * Widget.Material3.CompoundButton.RadioButton
     */
    public void applyRadioButtonStyle(RadioButton btn) {
        applyRadioButtonStyle(btn, true, true);
    }

    /**
     * Widget.Material3.CompoundButton.RadioButton
     */
    public void applyRadioButtonStyle(RadioButton btn, boolean animated, boolean withRipple) {
        applyTextAppearanceBodyMedium(btn);
        var button = new ButtonRadioDrawable(btn, animated, withRipple);
        btn.setButtonDrawable(button);
        btn.setButtonTintList(radioButtonTint());
        if (withRipple) {
            var ripple = new RippleDrawable(radioButtonRippleTint(), null, null);
            ripple.setRadius(button.getIntrinsicWidth() / 2); // 16dp
            btn.setBackground(ripple);
        }
    }

    private ColorStateList switchTrackTint;
    private ColorStateList switchTrackTint() {
        if (switchTrackTint != null) {
            return switchTrackTint;
        }
        switchTrackTint = new ColorStateList(
                new int[][]{
                        new int[]{-R.attr.state_enabled, -R.attr.state_checked},
                        new int[]{-R.attr.state_enabled, R.attr.state_checked},
                        new int[]{R.attr.state_checked},
                        StateSet.WILD_CARD
                },
                new int[]{
                        modulateColor(colorSurfaceContainerHighest, 0.12f),
                        modulateColor(colorOnSurface, 0.12f),
                        colorPrimary,
                        colorSurfaceContainerHighest
                }
        );
        return switchTrackTint;
    }

    private ColorStateList switchTrackDecorationTint;
    private ColorStateList switchTrackDecorationTint() {
        if (switchTrackDecorationTint != null) {
            return switchTrackDecorationTint;
        }
        switchTrackDecorationTint = new ColorStateList(
                new int[][]{
                        new int[]{R.attr.state_checked},
                        new int[]{-R.attr.state_enabled},
                        StateSet.WILD_CARD
                },
                new int[]{
                        Color.TRANSPARENT,
                        modulateColor(colorOnSurface, 0.12f),
                        colorOutline
                }
        );
        return switchTrackDecorationTint;
    }

    private ColorStateList switchThumbTint;
    private ColorStateList switchThumbTint() {
        if (switchThumbTint != null) {
            return switchThumbTint;
        }
        switchThumbTint = new ColorStateList(
                new int[][]{
                        new int[]{-R.attr.state_enabled, -R.attr.state_checked},
                        new int[]{-R.attr.state_enabled, R.attr.state_checked},
                        new int[]{R.attr.state_checked, R.attr.state_pressed},
                        new int[]{R.attr.state_checked, R.attr.state_hovered},
                        new int[]{R.attr.state_checked, R.attr.state_focused},
                        new int[]{R.attr.state_checked},
                        new int[]{R.attr.state_pressed},
                        new int[]{R.attr.state_hovered},
                        new int[]{R.attr.state_focused},
                        StateSet.WILD_CARD
                },
                new int[]{
                        modulateColor(colorOnSurface, 0.38f),
                        modulateColor(colorSurface, 1f),
                        colorPrimaryContainer,
                        colorPrimaryContainer,
                        colorPrimaryContainer,
                        colorOnPrimary,
                        colorOnSurfaceVariant,
                        colorOnSurfaceVariant,
                        colorOnSurfaceVariant,
                        colorOutline
                }
        );
        return switchThumbTint;
    }

    /**
     * Widget.Material3.CompoundButton.MaterialSwitch
     */
    public void applySwitchStyle(Switch btn) {
        applySwitchStyle(btn, true, true);
    }

    public void applySwitchStyle(Switch btn, boolean animated, boolean usePressState) {
        applyTextAppearanceBodyMedium(btn);
        var track = new ShapeDrawable();
        track.setShape(ShapeDrawable.RECTANGLE);
        track.setSize(btn.dp(52), btn.dp(32));
        track.setCornerRadius(btn.dp(16));
        track.setColor(switchTrackTint());
        track.setStroke(btn.dp(2), switchTrackDecorationTint());
        btn.setTrackDrawable(track);
        btn.setSwitchMinWidth(track.getIntrinsicWidth());
        btn.setSwitchPadding(btn.dp(16));
        var thumb = new SwitchThumbDrawable(btn, animated, usePressState);
        btn.setThumbDrawable(thumb);
        btn.setThumbTintList(switchThumbTint());
    }

    public void applySpinnerStyle(Spinner spinner) {
        //TODO background (arrow indicator)
        var listSelector = new RippleDrawable(colorControlHighlight(), null, new ColorDrawable(~0));
        spinner.setDropDownSelector(listSelector);
        var popupBackground = new ShapeDrawable();
        popupBackground.setShape(ShapeDrawable.RECTANGLE);
        popupBackground.setCornerRadius(spinner.dp(4));
        popupBackground.setColor(colorBackground);
        int dp2 = spinner.dp(2);
        popupBackground.setPadding(dp2, dp2, dp2, dp2);
        spinner.setPopupBackgroundDrawable(popupBackground);
    }

    private ColorStateList sliderTrackColorActive;
    private ColorStateList sliderTrackColorActive() {
        if (sliderTrackColorActive != null) {
            return sliderTrackColorActive;
        }
        sliderTrackColorActive = new ColorStateList(
                new int[][]{
                        StateSet.get(StateSet.VIEW_STATE_ENABLED),
                        StateSet.WILD_CARD
                },
                new int[]{
                        colorPrimary,
                        modulateColor(colorOnSurface, 0.38f)
                }
        );
        return sliderTrackColorActive;
    }

    private ColorStateList sliderTrackColorInactive;
    private ColorStateList sliderTrackColorInactive() {
        if (sliderTrackColorInactive != null) {
            return sliderTrackColorInactive;
        }
        sliderTrackColorInactive = new ColorStateList(
                new int[][]{
                        StateSet.get(StateSet.VIEW_STATE_ENABLED),
                        StateSet.WILD_CARD
                },
                new int[]{
                        colorSecondaryContainer,
                        modulateColor(colorOnSurface, 0.12f)
                }
        );
        return sliderTrackColorInactive;
    }

    public void applySeekBarStyle(SeekBar seekBar) {
        applySeekBarStyle(seekBar, false);
    }

    public void applySeekBarStyle(SeekBar seekBar, boolean discrete) {
        seekBar.setClickable(true);
        var background = new ShapeDrawable();
        background.setShape(ShapeDrawable.HLINE);
        background.setSize(-1, seekBar.dp(10));
        background.setCornerRadius(seekBar.dp(5));
        background.setColor(sliderTrackColorInactive());
        var progress = new ShapeDrawable();
        progress.setShape(ShapeDrawable.HLINE);
        progress.setSize(-1, seekBar.dp(10));
        progress.setCornerRadius(seekBar.dp(5));
        progress.setColor(sliderTrackColorActive());
        var scaledProgress = new ScaleDrawable(progress, Gravity.LEFT, 1.0f, -1.0f);
        var track = new LayerDrawable(background, scaledProgress);
        track.setId(0, R.id.background);
        track.setId(1, R.id.progress);
        track.setLayerGravity(0, Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL);
        track.setLayerGravity(1, Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL);
        seekBar.setProgressDrawable(track);
        seekBar.setSplitTrack(true);
        var thumb = new SeekbarThumbDrawable(seekBar);
        seekBar.setThumb(thumb);
        seekBar.setThumbTintList(sliderTrackColorActive());
        seekBar.setPadding(seekBar.dp(16), 0, seekBar.dp(16), 0);
        if (discrete) {
            var tick = new ShapeDrawable();
            tick.setShape(ShapeDrawable.CIRCLE);
            tick.setSize(seekBar.dp(4), seekBar.dp(4));
            tick.setColor(sliderTrackColorInactive());
            tick.setUseLevelForShape(false);
            seekBar.setTickMark(tick);
        }
    }

    public static SystemTheme createDefault(boolean isDark, int subclass) {
        SystemTheme t = new SystemTheme();
        int colorTextPrimaryInverse;
        int colorTextSecondaryAndTertiaryInverse;
        int colorTextSecondaryAndTertiaryInverseDisabled;
        int colorTextPrimaryInverseDisableOnly;
        int colorTextHintInverse;
        if (subclass == 0) {
            if (isDark) {
                t.colorSurface = 0xFF1A110F;
                t.colorSurfaceDim = 0xFF1A110F;
                t.colorSurfaceBright = 0xFF423734;
                t.colorSurfaceContainerLowest = 0xFF140C0A;
                t.colorSurfaceContainerLow = 0xFF231917;
                t.colorSurfaceContainer = 0xFF271D1B;
                t.colorSurfaceContainerHigh = 0xFF322825;
                t.colorSurfaceContainerHighest = 0xFF3D3230;
                t.colorOnSurface = 0xFFF1DFDA;
                t.colorSurfaceVariant = 0xFF53433F;
                t.colorOnSurfaceVariant = 0xFFD8C2BC;
                t.colorSurfaceInverse = 0xFFF1DFDA;
                t.colorOnSurfaceInverse = 0xFF392E2B;
                t.colorOutline = 0xFFA08C87;
                t.colorOutlineVariant = 0xFF53433F;
                t.colorPrimary = 0xFFFFB5A1;
                t.colorOnPrimary = 0xFF561F10;
                t.colorPrimaryContainer = 0xFF723523;
                t.colorOnPrimaryContainer = 0xFFFFDBD1;
                t.colorPrimaryInverse = 0xFF8F4B39;
                t.colorSecondary = 0xFFE7BDB2;
                t.colorOnSecondary = 0xFF442A23;
                t.colorSecondaryContainer = 0xFF5D4038;
                t.colorOnSecondaryContainer = 0xFFFFDBD1;
                t.colorTertiary = 0xFFD9C58D;
                t.colorOnTertiary = 0xFF3B2F05;
                t.colorTertiaryContainer = 0xFF534619;
                t.colorOnTertiaryContainer = 0xFFF6E1A6;
                t.colorError = 0xFFFFB4AB;
                t.colorOnError = 0xFF690005;
                t.colorErrorContainer = 0xFF93000A;
                t.colorOnErrorContainer = 0xFFFFDAD6;
                colorTextPrimaryInverse = 0xFF231917;
                colorTextSecondaryAndTertiaryInverse = 0xFF53433F;
                colorTextSecondaryAndTertiaryInverseDisabled = 0xFF231917;
                colorTextPrimaryInverseDisableOnly = 0xFF231917;
                colorTextHintInverse = 0xFF231917;
            } else {
                t.colorSurface = 0xFFFFF8F6;
                t.colorSurfaceDim = 0xFFE8D6D2;
                t.colorSurfaceBright = 0xFFFFF8F6;
                t.colorSurfaceContainerLowest = 0xFFFFFFFF;
                t.colorSurfaceContainerLow = 0xFFFFF1ED;
                t.colorSurfaceContainer = 0xFFFCEAE5;
                t.colorSurfaceContainerHigh = 0xFFF7E4E0;
                t.colorSurfaceContainerHighest = 0xFFF1DFDA;
                t.colorOnSurface = 0xFF231917;
                t.colorSurfaceVariant = 0xFFF5DED8;
                t.colorOnSurfaceVariant = 0xFF53433F;
                t.colorSurfaceInverse = 0xFF392E2B;
                t.colorOnSurfaceInverse = 0xFFFFEDE8;
                t.colorOutline = 0xFF85736E;
                t.colorOutlineVariant = 0xFFD8C2BC;
                t.colorPrimary = 0xFF8F4B39;
                t.colorOnPrimary = 0xFFFFFFFF;
                t.colorPrimaryContainer = 0xFFFFDBD1;
                t.colorOnPrimaryContainer = 0xFF723523;
                t.colorPrimaryInverse = 0xFFFFB5A1;
                t.colorSecondary = 0xFF77574E;
                t.colorOnSecondary = 0xFFFFFFFF;
                t.colorSecondaryContainer = 0xFFFFDBD1;
                t.colorOnSecondaryContainer = 0xFF5D4038;
                t.colorTertiary = 0xFF6C5D2F;
                t.colorOnTertiary = 0xFFFFFFFF;
                t.colorTertiaryContainer = 0xFFF6E1A6;
                t.colorOnTertiaryContainer = 0xFF534619;
                t.colorError = 0xFFBA1A1A;
                t.colorOnError = 0xFFFFFFFF;
                t.colorErrorContainer = 0xFFFFDAD6;
                t.colorOnErrorContainer = 0xFF93000A;
                colorTextPrimaryInverse = 0xFFF1DFDA;
                colorTextSecondaryAndTertiaryInverse = 0xFFD8C2BC;
                colorTextSecondaryAndTertiaryInverseDisabled = 0xFFF1DFDA;
                colorTextPrimaryInverseDisableOnly = 0xFFF1DFDA;
                colorTextHintInverse = 0xFFF1DFDA;
            }
            t.colorPrimaryFixed = 0xFFFFDBD1;
            t.colorPrimaryFixedDim = 0xFFFFB5A1;
            t.colorOnPrimaryFixed = 0xFF3A0B01;
            t.colorOnPrimaryFixedVariant = 0xFF723523;
            t.colorSecondaryFixed = 0xFFFFDBD1;
            t.colorSecondaryFixedDim = 0xFFE7BDB2;
            t.colorOnSecondaryFixed = 0xFF2C150F;
            t.colorOnSecondaryFixedVariant = 0xFF5D4038;
            t.colorTertiaryFixed = 0xFFF6E1A6;
            t.colorTertiaryFixedDim = 0xFFD9C58D;
            t.colorOnTertiaryFixed = 0xFF231B00;
            t.colorOnTertiaryFixedVariant = 0xFF534619;
        } else if (subclass == 1) {
            if (isDark) {
                t.colorSurface = 0xFF161312;
                t.colorSurfaceDim = 0xFF161312;
                t.colorSurfaceBright = 0xFF3D3837;
                t.colorSurfaceContainerLowest = 0xFF110D0D;
                t.colorSurfaceContainerLow = 0xFF1E1B1A;
                t.colorSurfaceContainer = 0xFF221F1E;
                t.colorSurfaceContainerHigh = 0xFF2D2928;
                t.colorSurfaceContainerHighest = 0xFF383433;
                t.colorOnSurface = 0xFFE9E1DF;
                t.colorSurfaceVariant = 0xFF504441;
                t.colorOnSurfaceVariant = 0xFFD4C3BE;
                t.colorSurfaceInverse = 0xFFE9E1DF;
                t.colorOnSurfaceInverse = 0xFF342F2E;
                t.colorOutline = 0xFF9D8D8A;
                t.colorOutlineVariant = 0xFF504441;
                t.colorPrimary = 0xFFEABEB2;
                t.colorOnPrimary = 0xFF452922;
                t.colorPrimaryContainer = 0xFFCDA398;
                t.colorOnPrimaryContainer = 0xFF573931;
                t.colorPrimaryInverse = 0xFF78564D;
                t.colorSecondary = 0xFFD9C1BC;
                t.colorOnSecondary = 0xFF3C2D29;
                t.colorSecondaryContainer = 0xFF564541;
                t.colorOnSecondaryContainer = 0xFFCAB4AE;
                t.colorTertiary = 0xFFD6C6A0;
                t.colorOnTertiary = 0xFF392F14;
                t.colorTertiaryContainer = 0xFFBAAB86;
                t.colorOnTertiaryContainer = 0xFF4A3F22;
                t.colorError = 0xFFFFB4AB;
                t.colorOnError = 0xFF690005;
                t.colorErrorContainer = 0xFF93000A;
                t.colorOnErrorContainer = 0xFFFFDAD6;
                colorTextPrimaryInverse = 0xFF1E1B1A;
                colorTextSecondaryAndTertiaryInverse = 0xFF504441;
                colorTextPrimaryInverseDisableOnly = 0xFF1E1B1A;
                colorTextSecondaryAndTertiaryInverseDisabled = 0xFF1E1B1A;
                colorTextHintInverse = 0xFF1E1B1A;
            } else {
                t.colorSurface = 0xFFFFF8F6;
                t.colorSurfaceDim = 0xFFE0D8D6;
                t.colorSurfaceBright = 0xFFFFF8F6;
                t.colorSurfaceContainerLowest = 0xFFFFFFFF;
                t.colorSurfaceContainerLow = 0xFFFBF2F0;
                t.colorSurfaceContainer = 0xFFF5ECEA;
                t.colorSurfaceContainerHigh = 0xFFEFE6E4;
                t.colorSurfaceContainerHighest = 0xFFE9E1DF;
                t.colorOnSurface = 0xFF1E1B1A;
                t.colorSurfaceVariant = 0xFFF1DFDA;
                t.colorOnSurfaceVariant = 0xFF504441;
                t.colorSurfaceInverse = 0xFF342F2E;
                t.colorOnSurfaceInverse = 0xFFF8EFED;
                t.colorOutline = 0xFF827470;
                t.colorOutlineVariant = 0xFFD4C3BE;
                t.colorPrimary = 0xFF78564D;
                t.colorOnPrimary = 0xFFFFFFFF;
                t.colorPrimaryContainer = 0xFFCDA398;
                t.colorOnPrimaryContainer = 0xFF573931;
                t.colorPrimaryInverse = 0xFFE9BDB1;
                t.colorSecondary = 0xFF6C5A56;
                t.colorOnSecondary = 0xFFFFFFFF;
                t.colorSecondaryContainer = 0xFFF6DDD7;
                t.colorOnSecondaryContainer = 0xFF73605C;
                t.colorTertiary = 0xFF695D3E;
                t.colorOnTertiary = 0xFFFFFFFF;
                t.colorTertiaryContainer = 0xFFBAAB86;
                t.colorOnTertiaryContainer = 0xFF4A3F22;
                t.colorError = 0xFFBA1A1A;
                t.colorOnError = 0xFFFFFFFF;
                t.colorErrorContainer = 0xFFFFDAD6;
                t.colorOnErrorContainer = 0xFF93000A;
                colorTextPrimaryInverse = 0xFFE9E1DF;
                colorTextSecondaryAndTertiaryInverse = 0xFFD4C3BE;
                colorTextPrimaryInverseDisableOnly = 0xFFE9E1DF;
                colorTextSecondaryAndTertiaryInverseDisabled = 0xFFE9E1DF;
                colorTextHintInverse = 0xFFE9E1DF;
            }
            t.colorPrimaryFixed = 0xFFFFDBD1;
            t.colorPrimaryFixedDim = 0xFFE9BDB1;
            t.colorOnPrimaryFixed = 0xFF2D150E;
            t.colorOnPrimaryFixedVariant = 0xFF5E3F37;
            t.colorSecondaryFixed = 0xFFF6DDD7;
            t.colorSecondaryFixedDim = 0xFFD9C1BC;
            t.colorOnSecondaryFixed = 0xFF251915;
            t.colorOnSecondaryFixedVariant = 0xFF53433F;
            t.colorTertiaryFixed = 0xFFF2E1B9;
            t.colorTertiaryFixedDim = 0xFFD5C59F;
            t.colorOnTertiaryFixed = 0xFF231B03;
            t.colorOnTertiaryFixedVariant = 0xFF504628;
        } else if (subclass == 2) {
            if (isDark) {
                t.colorSurface = 0xFF161312;
                t.colorSurfaceDim = 0xFF161312;
                t.colorSurfaceBright = 0xFF3C3837;
                t.colorSurfaceContainerLowest = 0xFF100E0D;
                t.colorSurfaceContainerLow = 0xFF1E1B1A;
                t.colorSurfaceContainer = 0xFF221F1E;
                t.colorSurfaceContainerHigh = 0xFF2D2928;
                t.colorSurfaceContainerHighest = 0xFF383433;
                t.colorOnSurface = 0xFFE9E1DF;
                t.colorSurfaceVariant = 0xFF4A4645;
                t.colorOnSurfaceVariant = 0xFFCCC5C3;
                t.colorSurfaceInverse = 0xFFE9E1DF;
                t.colorOnSurfaceInverse = 0xFF332F2F;
                t.colorOutline = 0xFF968F8E;
                t.colorOutlineVariant = 0xFF4A4645;
                t.colorPrimary = 0xFFE0BFB7;
                t.colorOnPrimary = 0xFF402C26;
                t.colorPrimaryContainer = 0xFF58413B;
                t.colorOnPrimaryContainer = 0xFFFDDBD3;
                t.colorPrimaryInverse = 0xFF715952;
                t.colorSecondary = 0xFFD8C2BC;
                t.colorOnSecondary = 0xFF3B2D2A;
                t.colorSecondaryContainer = 0xFF53433F;
                t.colorOnSecondaryContainer = 0xFFF5DED8;
                t.colorTertiary = 0xFFE7BDB2;
                t.colorOnTertiary = 0xFF442A23;
                t.colorTertiaryContainer = 0xFF5D4038;
                t.colorOnTertiaryContainer = 0xFFFFDBD1;
                t.colorError = 0xFFFFB4AB;
                t.colorOnError = 0xFF690005;
                t.colorErrorContainer = 0xFF93000A;
                t.colorOnErrorContainer = 0xFFFFDAD6;
                colorTextPrimaryInverse = 0xFF1E1B1A;
                colorTextSecondaryAndTertiaryInverse = 0xFF4A4645;
                colorTextPrimaryInverseDisableOnly = 0xFF1E1B1A;
                colorTextSecondaryAndTertiaryInverseDisabled = 0xFF1E1B1A;
                colorTextHintInverse = 0xFF1E1B1A;
            } else {
                t.colorSurface = 0xFFFFF8F6;
                t.colorSurfaceDim = 0xFFE0D8D6;
                t.colorSurfaceBright = 0xFFFFF8F6;
                t.colorSurfaceContainerLowest = 0xFFFFFFFF;
                t.colorSurfaceContainerLow = 0xFFFAF2F0;
                t.colorSurfaceContainer = 0xFFF4ECEA;
                t.colorSurfaceContainerHigh = 0xFFEFE6E4;
                t.colorSurfaceContainerHighest = 0xFFE9E1DF;
                t.colorOnSurface = 0xFF1E1B1A;
                t.colorSurfaceVariant = 0xFFE9E1DF;
                t.colorOnSurfaceVariant = 0xFF4A4645;
                t.colorSurfaceInverse = 0xFF332F2F;
                t.colorOnSurfaceInverse = 0xFFF7EFED;
                t.colorOutline = 0xFF7C7674;
                t.colorOutlineVariant = 0xFFCCC5C3;
                t.colorPrimary = 0xFF715952;
                t.colorOnPrimary = 0xFFFFFFFF;
                t.colorPrimaryContainer = 0xFFFDDBD3;
                t.colorOnPrimaryContainer = 0xFF58413B;
                t.colorPrimaryInverse = 0xFFE0BFB7;
                t.colorSecondary = 0xFF6C5B56;
                t.colorOnSecondary = 0xFFFFFFFF;
                t.colorSecondaryContainer = 0xFFF5DED8;
                t.colorOnSecondaryContainer = 0xFF53433F;
                t.colorTertiary = 0xFF77574E;
                t.colorOnTertiary = 0xFFFFFFFF;
                t.colorTertiaryContainer = 0xFFFFDBD1;
                t.colorOnTertiaryContainer = 0xFF5D4038;
                t.colorError = 0xFFBA1A1A;
                t.colorOnError = 0xFFFFFFFF;
                t.colorErrorContainer = 0xFFFFDAD6;
                t.colorOnErrorContainer = 0xFF93000A;
                colorTextPrimaryInverse = 0xFFE9E1DF;
                colorTextSecondaryAndTertiaryInverse = 0xFFCCC5C3;
                colorTextPrimaryInverseDisableOnly = 0xFFE9E1DF;
                colorTextSecondaryAndTertiaryInverseDisabled = 0xFFE9E1DF;
                colorTextHintInverse = 0xFFE9E1DF;
            }
            t.colorPrimaryFixed = 0xFFFDDBD3;
            t.colorPrimaryFixedDim = 0xFFE0BFB7;
            t.colorOnPrimaryFixed = 0xFF291712;
            t.colorOnPrimaryFixedVariant = 0xFF58413B;
            t.colorSecondaryFixed = 0xFFF5DED8;
            t.colorSecondaryFixedDim = 0xFFD8C2BC;
            t.colorOnSecondaryFixed = 0xFF251915;
            t.colorOnSecondaryFixedVariant = 0xFF53433F;
            t.colorTertiaryFixed = 0xFFFFDBD1;
            t.colorTertiaryFixedDim = 0xFFE7BDB2;
            t.colorOnTertiaryFixed = 0xFF2C150F;
            t.colorOnTertiaryFixedVariant = 0xFF5D4038;
        } else {
            if (isDark) {
                t.colorSurface = 0xFF131313;
                t.colorSurfaceDim = 0xFF131313;
                t.colorSurfaceBright = 0xFF393939;
                t.colorSurfaceContainerLowest = 0xFF0E0E0E;
                t.colorSurfaceContainerLow = 0xFF1B1B1B;
                t.colorSurfaceContainer = 0xFF1F1F1F;
                t.colorSurfaceContainerHigh = 0xFF2A2A2A;
                t.colorSurfaceContainerHighest = 0xFF353535;
                t.colorOnSurface = 0xFFE2E2E2;
                t.colorSurfaceVariant = 0xFF474747;
                t.colorOnSurfaceVariant = 0xFFC6C6C6;
                t.colorSurfaceInverse = 0xFFE2E2E2;
                t.colorOnSurfaceInverse = 0xFF303030;
                t.colorOutline = 0xFF919191;
                t.colorOutlineVariant = 0xFF474747;
                t.colorPrimary = 0xFFFFFFFF;
                t.colorOnPrimary = 0xFF1B1B1B;
                t.colorPrimaryContainer = 0xFFD4D4D4;
                t.colorOnPrimaryContainer = 0xFF000000;
                t.colorPrimaryInverse = 0xFF5E5E5E;
                t.colorSecondary = 0xFFC6C6C6;
                t.colorOnSecondary = 0xFF1B1B1B;
                t.colorSecondaryContainer = 0xFF474747;
                t.colorOnSecondaryContainer = 0xFFE2E2E2;
                t.colorTertiary = 0xFFE2E2E2;
                t.colorOnTertiary = 0xFF1B1B1B;
                t.colorTertiaryContainer = 0xFF919191;
                t.colorOnTertiaryContainer = 0xFF000000;
                t.colorError = 0xFFFFB4AB;
                t.colorOnError = 0xFF690005;
                t.colorErrorContainer = 0xFF93000A;
                t.colorOnErrorContainer = 0xFFFFDAD6;
                colorTextPrimaryInverse = 0xFF1B1B1B;
                colorTextSecondaryAndTertiaryInverse = 0xFF474747;
                colorTextPrimaryInverseDisableOnly = 0xFF1B1B1B;
                colorTextSecondaryAndTertiaryInverseDisabled = 0xFF1B1B1B;
                colorTextHintInverse = 0xFF1B1B1B;
            } else {
                t.colorSurface = 0xFFF9F9F9;
                t.colorSurfaceDim = 0xFFDADADA;
                t.colorSurfaceBright = 0xFFF9F9F9;
                t.colorSurfaceContainerLowest = 0xFFFFFFFF;
                t.colorSurfaceContainerLow = 0xFFF3F3F3;
                t.colorSurfaceContainer = 0xFFEEEEEE;
                t.colorSurfaceContainerHigh = 0xFFE8E8E8;
                t.colorSurfaceContainerHighest = 0xFFE2E2E2;
                t.colorOnSurface = 0xFF1B1B1B;
                t.colorSurfaceVariant = 0xFFE2E2E2;
                t.colorOnSurfaceVariant = 0xFF474747;
                t.colorSurfaceInverse = 0xFF303030;
                t.colorOnSurfaceInverse = 0xFFF1F1F1;
                t.colorOutline = 0xFF777777;
                t.colorOutlineVariant = 0xFFC6C6C6;
                t.colorPrimary = 0xFF000000;
                t.colorOnPrimary = 0xFFE2E2E2;
                t.colorPrimaryContainer = 0xFF3B3B3B;
                t.colorOnPrimaryContainer = 0xFFFFFFFF;
                t.colorPrimaryInverse = 0xFFC6C6C6;
                t.colorSecondary = 0xFF5E5E5E;
                t.colorOnSecondary = 0xFFFFFFFF;
                t.colorSecondaryContainer = 0xFFD4D4D4;
                t.colorOnSecondaryContainer = 0xFF1B1B1B;
                t.colorTertiary = 0xFF3B3B3B;
                t.colorOnTertiary = 0xFFE2E2E2;
                t.colorTertiaryContainer = 0xFF747474;
                t.colorOnTertiaryContainer = 0xFFFFFFFF;
                t.colorError = 0xFFBA1A1A;
                t.colorOnError = 0xFFFFFFFF;
                t.colorErrorContainer = 0xFFFFDAD6;
                t.colorOnErrorContainer = 0xFF410002;
                colorTextPrimaryInverse = 0xFFE2E2E2;
                colorTextSecondaryAndTertiaryInverse = 0xFFC6C6C6;
                colorTextPrimaryInverseDisableOnly = 0xFFE2E2E2;
                colorTextSecondaryAndTertiaryInverseDisabled = 0xFFE2E2E2;
                colorTextHintInverse = 0xFFE2E2E2;
            }
            t.colorPrimaryFixed = 0xFF5E5E5E;
            t.colorPrimaryFixedDim = 0xFF474747;
            t.colorOnPrimaryFixed = 0xFFFFFFFF;
            t.colorOnPrimaryFixedVariant = 0xFFE2E2E2;
            t.colorSecondaryFixed = 0xFFC6C6C6;
            t.colorSecondaryFixedDim = 0xFFABABAB;
            t.colorOnSecondaryFixed = 0xFF1B1B1B;
            t.colorOnSecondaryFixedVariant = 0xFF3B3B3B;
            t.colorTertiaryFixed = 0xFF5E5E5E;
            t.colorTertiaryFixedDim = 0xFF474747;
            t.colorOnTertiaryFixed = 0xFFFFFFFF;
            t.colorOnTertiaryFixedVariant = 0xFFE2E2E2;
        }

        t.colorBackground = t.colorSurface;
        t.colorOnBackground = t.colorOnSurface;

        var textStateSpec = new int[][]{
                new int[]{-R.attr.state_enabled},
                StateSet.WILD_CARD
        };
        t.textColorPrimary = new ColorStateList(
                textStateSpec,
                new int[]{
                        modulateColor(t.colorOnSurface, 0.38f),
                        t.colorOnSurface
                }
        );
        t.textColorPrimaryInverse = new ColorStateList(
                textStateSpec,
                new int[]{
                        modulateColor(colorTextPrimaryInverse, 0.38f),
                        colorTextPrimaryInverse
                }
        );
        t.textColorSecondary = new ColorStateList(
                textStateSpec,
                new int[]{
                        modulateColor(t.colorOnSurface, 0.38f),
                        t.colorOnSurfaceVariant
                }
        );
        t.textColorSecondaryInverse = new ColorStateList(
                textStateSpec,
                new int[]{
                        modulateColor(colorTextSecondaryAndTertiaryInverseDisabled, 0.38f),
                        colorTextSecondaryAndTertiaryInverse
                }
        );
        t.textColorPrimaryDisableOnly = new ColorStateList(
                textStateSpec,
                new int[]{
                        modulateColor(t.colorOnBackground, 0.6f),
                        t.colorOnBackground
                }
        );
        t.textColorPrimaryInverseDisableOnly = new ColorStateList(
                textStateSpec,
                new int[]{
                        modulateColor(colorTextPrimaryInverseDisableOnly, 0.6f),
                        colorTextPrimaryInverseDisableOnly
                }
        );
        var hintStateSpec = new int[][]{
                new int[]{R.attr.state_enabled, R.attr.state_pressed},
                StateSet.WILD_CARD
        };
        t.textColorHint = new ColorStateList(
                hintStateSpec,
                new int[]{
                        modulateColor(t.colorOnBackground, 0.6f),
                        modulateColor(t.colorOnBackground, 0.38f)
                }
        );
        t.textColorHintInverse = new ColorStateList(
                hintStateSpec,
                new int[]{
                        modulateColor(colorTextHintInverse, 0.87f),
                        modulateColor(colorTextHintInverse, 0.6f)
                }
        );
        t.textColorTertiary = t.textColorSecondary;
        t.textColorTertiaryInverse = t.textColorSecondaryInverse;

        t.textColorHighlight = modulateColor(t.colorPrimary, 0.6f);
        t.textColorHighlightInverse = modulateColor(t.colorPrimaryInverse, 0.6f);

        t.textColorLink = ColorStateList.valueOf(t.colorPrimary);
        t.textColorLinkInverse = ColorStateList.valueOf(t.colorPrimaryInverse);

        t.textColorAlertDialogListItem = t.textColorPrimary;

        t.isDark = isDark;

        return t;
    }

    // Base.V14.Theme.Material3.Dark
    // Base.V14.Theme.Material3.Light
    public static SystemTheme createMaterial(boolean isDark) {
        SystemTheme t = new SystemTheme();
        //@formatter:off
        final int palette_black = 0xff000000;
        final int palette_error0 = 0xff000000;
        final int palette_error10 = 0xff410e0b;
        final int palette_error100 = 0xffffffff;
        final int palette_error20 = 0xff601410;
        final int palette_error30 = 0xff8c1d18;
        final int palette_error40 = 0xffb3261e;
        final int palette_error50 = 0xffdc362e;
        final int palette_error60 = 0xffe46962;
        final int palette_error70 = 0xffec928e;
        final int palette_error80 = 0xfff2b8b5;
        final int palette_error90 = 0xfff9dedc;
        final int palette_error95 = 0xfffceeee;
        final int palette_error99 = 0xfffffbf9;
        final int palette_neutral0 = 0xff000000;
        final int palette_neutral10 = 0xff1d1b20;
        final int palette_neutral100 = 0xffffffff;
        final int palette_neutral12 = 0xff211f26;
        final int palette_neutral17 = 0xff2b2930;
        final int palette_neutral20 = 0xff322f35;
        final int palette_neutral22 = 0xff36343b;
        final int palette_neutral24 = 0xff3b383e;
        final int palette_neutral30 = 0xff48464c;
        final int palette_neutral4 = 0xff0f0d13;
        final int palette_neutral40 = 0xff605d64;
        final int palette_neutral50 = 0xff79767d;
        final int palette_neutral6 = 0xff141218;
        final int palette_neutral60 = 0xff938f96;
        final int palette_neutral70 = 0xffaea9b1;
        final int palette_neutral80 = 0xffcac5cd;
        final int palette_neutral87 = 0xffded8e1;
        final int palette_neutral90 = 0xffe6e0e9;
        final int palette_neutral92 = 0xffece6f0;
        final int palette_neutral94 = 0xfff3edf7;
        final int palette_neutral95 = 0xfff5eff7;
        final int palette_neutral96 = 0xfff7f2fa;
        final int palette_neutral98 = 0xfffef7ff;
        final int palette_neutral99 = 0xfffffbff;
        final int palette_neutral_variant0 = 0xff000000;
        final int palette_neutral_variant10 = 0xff1d1a22;
        final int palette_neutral_variant100 = 0xffffffff;
        final int palette_neutral_variant20 = 0xff322f37;
        final int palette_neutral_variant30 = 0xff49454f;
        final int palette_neutral_variant40 = 0xff605d66;
        final int palette_neutral_variant50 = 0xff79747e;
        final int palette_neutral_variant60 = 0xff938f99;
        final int palette_neutral_variant70 = 0xffaea9b4;
        final int palette_neutral_variant80 = 0xffcac4d0;
        final int palette_neutral_variant90 = 0xffe7e0ec;
        final int palette_neutral_variant95 = 0xfff5eefa;
        final int palette_neutral_variant99 = 0xfffffbfe;
        final int palette_primary0 = 0xff000000;
        final int palette_primary10 = 0xff21005d;
        final int palette_primary100 = 0xffffffff;
        final int palette_primary20 = 0xff381e72;
        final int palette_primary30 = 0xff4f378b;
        final int palette_primary40 = 0xff6750a4;
        final int palette_primary50 = 0xff7f67be;
        final int palette_primary60 = 0xff9a82db;
        final int palette_primary70 = 0xffb69df8;
        final int palette_primary80 = 0xffd0bcff;
        final int palette_primary90 = 0xffeaddff;
        final int palette_primary95 = 0xfff6edff;
        final int palette_primary99 = 0xfffffbfe;
        final int palette_secondary0 = 0xff000000;
        final int palette_secondary10 = 0xff1d192b;
        final int palette_secondary100 = 0xffffffff;
        final int palette_secondary20 = 0xff332d41;
        final int palette_secondary30 = 0xff4a4458;
        final int palette_secondary40 = 0xff625b71;
        final int palette_secondary50 = 0xff7a7289;
        final int palette_secondary60 = 0xff958da5;
        final int palette_secondary70 = 0xffb0a7c0;
        final int palette_secondary80 = 0xffccc2dc;
        final int palette_secondary90 = 0xffe8def8;
        final int palette_secondary95 = 0xfff6edff;
        final int palette_secondary99 = 0xfffffbfe;
        final int palette_tertiary0 = 0xff000000;
        final int palette_tertiary10 = 0xff31111d;
        final int palette_tertiary100 = 0xffffffff;
        final int palette_tertiary20 = 0xff492532;
        final int palette_tertiary30 = 0xff633b48;
        final int palette_tertiary40 = 0xff7d5260;
        final int palette_tertiary50 = 0xff986977;
        final int palette_tertiary60 = 0xffb58392;
        final int palette_tertiary70 = 0xffd29dac;
        final int palette_tertiary80 = 0xffefb8c8;
        final int palette_tertiary90 = 0xffffd8e4;
        final int palette_tertiary95 = 0xffffecf1;
        final int palette_tertiary99 = 0xfffffbfa;
        final int palette_white = 0xffffffff;
        //@formatter:on
        if (isDark) {
            t.colorPrimary = palette_primary80;
            t.colorOnPrimary = palette_primary20;
            t.colorPrimaryInverse = palette_primary40;
            t.colorPrimaryContainer = palette_primary30;
            t.colorOnPrimaryContainer = palette_primary90;

            t.colorSecondary = palette_secondary80;
            t.colorOnSecondary = palette_secondary20;
            t.colorSecondaryContainer = palette_secondary30;
            t.colorOnSecondaryContainer = palette_secondary90;

            t.colorTertiary = palette_tertiary80;
            t.colorOnTertiary = palette_tertiary20;
            t.colorTertiaryContainer = palette_tertiary30;
            t.colorOnTertiaryContainer = palette_tertiary90;
        } else {
            t.colorPrimary = palette_primary40;
            t.colorOnPrimary = palette_primary100;
            t.colorPrimaryInverse = palette_primary80;
            t.colorPrimaryContainer = palette_primary90;
            t.colorOnPrimaryContainer = palette_primary10;

            t.colorSecondary = palette_secondary40;
            t.colorOnSecondary = palette_secondary100;
            t.colorSecondaryContainer = palette_secondary90;
            t.colorOnSecondaryContainer = palette_secondary10;

            t.colorTertiary = palette_tertiary40;
            t.colorOnTertiary = palette_tertiary100;
            t.colorTertiaryContainer = palette_tertiary90;
            t.colorOnTertiaryContainer = palette_tertiary10;
        }
        t.colorPrimaryFixed = palette_primary90;
        t.colorPrimaryFixedDim = palette_primary80;
        t.colorOnPrimaryFixed = palette_primary10;
        t.colorOnPrimaryFixedVariant = palette_primary30;

        t.colorSecondaryFixed = palette_secondary90;
        t.colorSecondaryFixedDim = palette_secondary80;
        t.colorOnSecondaryFixed = palette_secondary10;
        t.colorOnSecondaryFixedVariant = palette_secondary30;

        t.colorTertiaryFixed = palette_tertiary90;
        t.colorTertiaryFixedDim = palette_tertiary80;
        t.colorOnTertiaryFixed = palette_tertiary10;
        t.colorOnTertiaryFixedVariant = palette_tertiary30;
        if (isDark) {
            t.colorSurface = palette_neutral6;
            t.colorOnSurface = palette_neutral90;
            t.colorSurfaceVariant = palette_neutral_variant30;
            t.colorOnSurfaceVariant = palette_neutral_variant80;
            t.colorSurfaceInverse = palette_neutral90;
            t.colorOnSurfaceInverse = palette_neutral20;
            t.colorSurfaceBright = palette_neutral24;
            t.colorSurfaceDim = palette_neutral6;

            t.colorSurfaceContainer = palette_neutral12;
            t.colorSurfaceContainerLow = palette_neutral10;
            t.colorSurfaceContainerHigh = palette_neutral17;
            t.colorSurfaceContainerLowest = palette_neutral4;
            t.colorSurfaceContainerHighest = palette_neutral22;

            t.colorOutline = palette_neutral_variant60;
            t.colorOutlineVariant = palette_neutral_variant30;
            t.colorError = palette_error80;
            t.colorOnError = palette_error20;
            t.colorErrorContainer = palette_error30;
            t.colorOnErrorContainer = palette_error90;
        } else {
            t.colorSurface = palette_neutral98;
            t.colorOnSurface = palette_neutral10;
            t.colorSurfaceVariant = palette_neutral_variant90;
            t.colorOnSurfaceVariant = palette_neutral_variant30;
            t.colorSurfaceInverse = palette_neutral20;
            t.colorOnSurfaceInverse = palette_neutral95;
            t.colorSurfaceBright = palette_neutral98;
            t.colorSurfaceDim = palette_neutral87;

            t.colorSurfaceContainer = palette_neutral94;
            t.colorSurfaceContainerLow = palette_neutral96;
            t.colorSurfaceContainerHigh = palette_neutral92;
            t.colorSurfaceContainerLowest = palette_neutral100;
            t.colorSurfaceContainerHighest = palette_neutral90;

            t.colorOutline = palette_neutral_variant50;
            t.colorOutlineVariant = palette_neutral_variant80;
            t.colorError = palette_error40;
            t.colorOnError = palette_error100;
            t.colorErrorContainer = palette_error90;
            t.colorOnErrorContainer = palette_error10;
        }
        t.colorBackground = t.colorSurface;
        t.colorOnBackground = t.colorOnSurface;

        var textStateSpec = new int[][]{
                new int[]{-R.attr.state_enabled},
                StateSet.WILD_CARD
        };
        var colorPrimaryText = new ColorStateList(
                textStateSpec,
                new int[]{
                        modulateColor(palette_neutral10, 0.38f),
                        palette_neutral10
                }
        );
        var darkColorPrimaryText = new ColorStateList(
                textStateSpec,
                new int[]{
                        modulateColor(palette_neutral90, 0.38f),
                        palette_neutral90
                }
        );
        var colorSecondaryText = new ColorStateList(
                textStateSpec,
                new int[]{
                        modulateColor(palette_neutral10, 0.38f),
                        palette_neutral_variant30
                }
        );
        var darkColorSecondaryText = new ColorStateList(
                textStateSpec,
                new int[]{
                        modulateColor(palette_neutral90, 0.38f),
                        palette_neutral_variant80
                }
        );
        var textDisableOnly = new ColorStateList(
                textStateSpec,
                new int[]{
                        modulateColor(palette_neutral10, 0.6f),
                        palette_neutral10
                }
        );
        var darkTextDisableOnly = new ColorStateList(
                textStateSpec,
                new int[]{
                        modulateColor(palette_neutral90, 0.6f),
                        palette_neutral90
                }
        );
        var hintStateSpec = new int[][]{
                new int[]{R.attr.state_enabled, R.attr.state_pressed},
                StateSet.WILD_CARD
        };
        var hintForeground = new ColorStateList(
                hintStateSpec,
                new int[]{
                        modulateColor(palette_neutral10, 0.6f),
                        modulateColor(palette_neutral10, 0.38f)
                }
        );
        var darkHintForeground = new ColorStateList(
                hintStateSpec,
                new int[]{
                        modulateColor(palette_neutral90, 0.87f),
                        modulateColor(palette_neutral90, 0.6f)
                }
        );
        if (isDark) {
            t.textColorPrimary = darkColorPrimaryText;
            t.textColorPrimaryInverse = colorPrimaryText;
            t.textColorSecondary = darkColorSecondaryText;
            t.textColorSecondaryInverse = colorSecondaryText;
            t.textColorPrimaryDisableOnly = darkTextDisableOnly;
            t.textColorPrimaryInverseDisableOnly = textDisableOnly;
            t.textColorHint = darkHintForeground;
            t.textColorHintInverse = hintForeground;
        } else {
            t.textColorPrimary = colorPrimaryText;
            t.textColorPrimaryInverse = darkColorPrimaryText;
            t.textColorSecondary = colorSecondaryText;
            t.textColorSecondaryInverse = darkColorSecondaryText;
            t.textColorPrimaryDisableOnly = textDisableOnly;
            t.textColorPrimaryInverseDisableOnly = darkTextDisableOnly;
            t.textColorHint = hintForeground;
            t.textColorHintInverse = darkHintForeground;
        }
        t.textColorTertiary = t.textColorSecondary;
        t.textColorTertiaryInverse = t.textColorSecondaryInverse;

        t.textColorHighlight = modulateColor(t.colorPrimary, 0.6f);
        t.textColorHighlightInverse = modulateColor(t.colorPrimaryInverse, 0.6f);

        t.textColorLink = ColorStateList.valueOf(t.colorPrimary);
        t.textColorLinkInverse = ColorStateList.valueOf(t.colorPrimaryInverse);

        t.textColorAlertDialogListItem = t.textColorPrimary;

        t.isDark = isDark;

        return t;
    }
}
