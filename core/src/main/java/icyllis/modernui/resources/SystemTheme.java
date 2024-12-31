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
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.drawable.RippleDrawable;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.SparseArray;
import icyllis.modernui.util.StateSet;
import icyllis.modernui.widget.Button;
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

    public static final ColorStateList TEXT_COLOR_SECONDARY;

    static {
        int[][] stateSet = {
                new int[]{-R.attr.state_enabled},
                new int[]{R.attr.state_hovered},
                StateSet.WILD_CARD
        };
        int[] colors = {
                COLOR_FOREGROUND_DISABLED,
                COLOR_FOREGROUND,
                COLOR_FOREGROUND_NORMAL
        };
        TEXT_COLOR_SECONDARY = new ColorStateList(stateSet, colors);
    }

    public static final ColorStateList COLOR_CONTROL_NORMAL = TEXT_COLOR_SECONDARY;

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

    static {
        setToMaterialDark();
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
        //// lineHeight 20sp here???
        tv.setHintTextColor(textColorHint);
        tv.setLinkTextColor(textColorLink);
        // textColor would be overridden
    }

    public void applyTextButtonStyle(Button btn) {
        btn.setMinHeight(btn.dp(40));
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

        return t;
    }
}
