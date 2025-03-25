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
import icyllis.modernui.graphics.drawable.ColorDrawable;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.drawable.LayerDrawable;
import icyllis.modernui.graphics.drawable.RippleDrawable;
import icyllis.modernui.graphics.drawable.ScaleDrawable;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.material.drawable.RadioButtonDrawable;
import icyllis.modernui.material.drawable.CircularIndeterminateDrawable;
import icyllis.modernui.material.drawable.LinearIndeterminateDrawable;
import icyllis.modernui.material.drawable.SeekbarThumbDrawable;
import icyllis.modernui.material.drawable.SwitchThumbDrawable;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.SparseArray;
import icyllis.modernui.util.StateSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.widget.Spinner;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.concurrent.GuardedBy;
import java.util.Objects;
import java.util.function.Function;

/**
 * @hidden
 */
@ApiStatus.Internal
public class SystemTheme {

    public static final int COLOR_FOREGROUND = 0xFFFFFFFF;
    public static final int COLOR_FOREGROUND_NORMAL = 0xFFB0B0B0;
    public static final int COLOR_FOREGROUND_DISABLED = 0xFF3F3F3F;

    public static final float DISABLED_ALPHA = 0.3f;
    public static final float PRIMARY_CONTENT_ALPHA = 1;
    public static final float SECONDARY_CONTENT_ALPHA = 0.7f;

    public static final int COLOR_CONTROL_ACTIVATED = 0xffcda398;

    private static final float material_emphasis_disabled = 0.38f;
    private static final float material_emphasis_disabled_background = 0.12f;

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
    public boolean isDark;

    @GuardedBy("self")
    static final Object2ObjectOpenHashMap<Resources.ThemeKey, ThemedCache> gCache = new Object2ObjectOpenHashMap<>();

    static class ThemedCache {

        final int
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

        ThemedCache(Resources.Theme theme) {
            final TypedValue value = new TypedValue();
            theme.getAttribute(R.ns, R.attr.colorPrimary, value);
            colorPrimary = value.data;
            theme.getAttribute(R.ns, R.attr.colorOnPrimary, value);
            colorOnPrimary = value.data;
            theme.getAttribute(R.ns, R.attr.colorPrimaryInverse, value);
            colorPrimaryInverse = value.data;
            theme.getAttribute(R.ns, R.attr.colorPrimaryContainer, value);
            colorPrimaryContainer = value.data;
            theme.getAttribute(R.ns, R.attr.colorOnPrimaryContainer, value);
            colorOnPrimaryContainer = value.data;
            theme.getAttribute(R.ns, R.attr.colorPrimaryFixed, value);
            colorPrimaryFixed = value.data;
            theme.getAttribute(R.ns, R.attr.colorPrimaryFixedDim, value);
            colorPrimaryFixedDim = value.data;
            theme.getAttribute(R.ns, R.attr.colorOnPrimaryFixed, value);
            colorOnPrimaryFixed = value.data;
            theme.getAttribute(R.ns, R.attr.colorOnPrimaryFixedVariant, value);
            colorOnPrimaryFixedVariant = value.data;
            theme.getAttribute(R.ns, R.attr.colorSecondary, value);
            colorSecondary = value.data;
            theme.getAttribute(R.ns, R.attr.colorOnSecondary, value);
            colorOnSecondary = value.data;
            theme.getAttribute(R.ns, R.attr.colorSecondaryContainer, value);
            colorSecondaryContainer = value.data;
            theme.getAttribute(R.ns, R.attr.colorOnSecondaryContainer, value);
            colorOnSecondaryContainer = value.data;
            theme.getAttribute(R.ns, R.attr.colorSecondaryFixed, value);
            colorSecondaryFixed = value.data;
            theme.getAttribute(R.ns, R.attr.colorSecondaryFixedDim, value);
            colorSecondaryFixedDim = value.data;
            theme.getAttribute(R.ns, R.attr.colorOnSecondaryFixed, value);
            colorOnSecondaryFixed = value.data;
            theme.getAttribute(R.ns, R.attr.colorOnSecondaryFixedVariant, value);
            colorOnSecondaryFixedVariant = value.data;
            theme.getAttribute(R.ns, R.attr.colorTertiary, value);
            colorTertiary = value.data;
            theme.getAttribute(R.ns, R.attr.colorOnTertiary, value);
            colorOnTertiary = value.data;
            theme.getAttribute(R.ns, R.attr.colorTertiaryContainer, value);
            colorTertiaryContainer = value.data;
            theme.getAttribute(R.ns, R.attr.colorOnTertiaryContainer, value);
            colorOnTertiaryContainer = value.data;
            theme.getAttribute(R.ns, R.attr.colorTertiaryFixed, value);
            colorTertiaryFixed = value.data;
            theme.getAttribute(R.ns, R.attr.colorTertiaryFixedDim, value);
            colorTertiaryFixedDim = value.data;
            theme.getAttribute(R.ns, R.attr.colorOnTertiaryFixed, value);
            colorOnTertiaryFixed = value.data;
            theme.getAttribute(R.ns, R.attr.colorOnTertiaryFixedVariant, value);
            colorOnTertiaryFixedVariant = value.data;
            theme.getAttribute(R.ns, R.attr.colorBackground, value);
            colorBackground = value.data;
            theme.getAttribute(R.ns, R.attr.colorOnBackground, value);
            colorOnBackground = value.data;
            theme.getAttribute(R.ns, R.attr.colorSurface, value);
            colorSurface = value.data;
            theme.getAttribute(R.ns, R.attr.colorOnSurface, value);
            colorOnSurface = value.data;
            theme.getAttribute(R.ns, R.attr.colorSurfaceVariant, value);
            colorSurfaceVariant = value.data;
            theme.getAttribute(R.ns, R.attr.colorOnSurfaceVariant, value);
            colorOnSurfaceVariant = value.data;
            theme.getAttribute(R.ns, R.attr.colorSurfaceInverse, value);
            colorSurfaceInverse = value.data;
            theme.getAttribute(R.ns, R.attr.colorOnSurfaceInverse, value);
            colorOnSurfaceInverse = value.data;
            theme.getAttribute(R.ns, R.attr.colorSurfaceBright, value);
            colorSurfaceBright = value.data;
            theme.getAttribute(R.ns, R.attr.colorSurfaceDim, value);
            colorSurfaceDim = value.data;
            theme.getAttribute(R.ns, R.attr.colorSurfaceContainer, value);
            colorSurfaceContainer = value.data;
            theme.getAttribute(R.ns, R.attr.colorSurfaceContainerLow, value);
            colorSurfaceContainerLow = value.data;
            theme.getAttribute(R.ns, R.attr.colorSurfaceContainerHigh, value);
            colorSurfaceContainerHigh = value.data;
            theme.getAttribute(R.ns, R.attr.colorSurfaceContainerLowest, value);
            colorSurfaceContainerLowest = value.data;
            theme.getAttribute(R.ns, R.attr.colorSurfaceContainerHighest, value);
            colorSurfaceContainerHighest = value.data;
            theme.getAttribute(R.ns, R.attr.colorOutline, value);
            colorOutline = value.data;
            theme.getAttribute(R.ns, R.attr.colorOutlineVariant, value);
            colorOutlineVariant = value.data;
            theme.getAttribute(R.ns, R.attr.colorError, value);
            colorError = value.data;
            theme.getAttribute(R.ns, R.attr.colorOnError, value);
            colorOnError = value.data;
            theme.getAttribute(R.ns, R.attr.colorErrorContainer, value);
            colorErrorContainer = value.data;
            theme.getAttribute(R.ns, R.attr.colorOnErrorContainer, value);
            colorOnErrorContainer = value.data;
        }

        private SparseArray<ColorStateList> button_foreground_color_selector;
        private ColorStateList button_foreground_color_selector(int colorOnContainer) {
            if (button_foreground_color_selector == null) {
                button_foreground_color_selector = new SparseArray<>();
            }
            var csl = button_foreground_color_selector.get(colorOnContainer);
            if (csl != null) {
                return csl;
            }
            csl = new ColorStateList(
                    new int[][]{
                            StateSet.get(StateSet.VIEW_STATE_ENABLED),
                            StateSet.WILD_CARD
                    },
                    new int[]{
                            colorOnContainer,
                            modulateColor(colorOnSurface, material_emphasis_disabled)
                    }
            );
            button_foreground_color_selector.put(colorOnContainer, csl);
            return csl;
        }

        private SparseArray<ColorStateList> button_background_color_selector;
        private ColorStateList button_background_color_selector(int colorContainer) {
            if (button_background_color_selector == null) {
                button_background_color_selector = new SparseArray<>();
            }
            var csl = button_background_color_selector.get(colorContainer);
            if (csl != null) {
                return csl;
            }
            csl = new ColorStateList(
                    new int[][]{
                            StateSet.get(StateSet.VIEW_STATE_ENABLED),
                            StateSet.WILD_CARD
                    },
                    new int[]{
                            colorContainer,
                            modulateColor(colorOnSurface, material_emphasis_disabled_background)
                    }
            );
            button_background_color_selector.put(colorContainer, csl);
            return csl;
        }

        private SparseArray<ColorStateList> button_ripple_color_selector;
        private ColorStateList button_ripple_color_selector(int colorOnContainer) {
            if (button_ripple_color_selector == null) {
                button_ripple_color_selector = new SparseArray<>();
            }
            var csl = button_ripple_color_selector.get(colorOnContainer);
            if (csl != null) {
                return csl;
            }
            csl = new ColorStateList(
                    new int[][]{
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
                    }
            );
            button_ripple_color_selector.put(colorOnContainer, csl);
            return csl;
        }

        private ColorStateList button_outline_color_selector;
        private ColorStateList button_outline_color_selector() {
            if (button_outline_color_selector != null) {
                return button_outline_color_selector;
            }
            button_outline_color_selector = new ColorStateList(
                    new int[][]{
                            new int[]{-R.attr.state_enabled},
                            StateSet.WILD_CARD
                    },
                    new int[]{
                            modulateColor(colorOnSurface, 0.12f),
                            colorOutline
                    }
            );
            return button_outline_color_selector;
        }

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

        private ColorStateList radio_button_tint;
        private ColorStateList radio_button_tint() {
            if (radio_button_tint != null) {
                return radio_button_tint;
            }
            radio_button_tint = new ColorStateList(
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
            return radio_button_tint;
        }

        // reused for checkbox
        private ColorStateList radio_button_ripple_tint;
        private ColorStateList radio_button_ripple_tint() {
            if (radio_button_ripple_tint != null) {
                return radio_button_ripple_tint;
            }
            radio_button_ripple_tint = new ColorStateList(
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
            return radio_button_ripple_tint;
        }

        private ColorStateList switch_track_tint;
        private ColorStateList switch_track_tint() {
            if (switch_track_tint != null) {
                return switch_track_tint;
            }
            switch_track_tint = new ColorStateList(
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
            return switch_track_tint;
        }

        private ColorStateList switch_track_decoration_tint;
        private ColorStateList switch_track_decoration_tint() {
            if (switch_track_decoration_tint != null) {
                return switch_track_decoration_tint;
            }
            switch_track_decoration_tint = new ColorStateList(
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
            return switch_track_decoration_tint;
        }

        private ColorStateList switch_thumb_tint;
        private ColorStateList switch_thumb_tint() {
            if (switch_thumb_tint != null) {
                return switch_thumb_tint;
            }
            switch_thumb_tint = new ColorStateList(
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
            return switch_thumb_tint;
        }

        // reused for progress bar, seek bar
        private ColorStateList slider_track_color_active;
        private ColorStateList slider_track_color_active() {
            if (slider_track_color_active != null) {
                return slider_track_color_active;
            }
            slider_track_color_active = new ColorStateList(
                    new int[][]{
                            StateSet.get(StateSet.VIEW_STATE_ENABLED),
                            StateSet.WILD_CARD
                    },
                    new int[]{
                            colorPrimary,
                            modulateColor(colorOnSurface, 0.38f)
                    }
            );
            return slider_track_color_active;
        }

        // reused for progress bar, seek bar
        private ColorStateList slider_track_color_inactive;
        private ColorStateList slider_track_color_inactive() {
            if (slider_track_color_inactive != null) {
                return slider_track_color_inactive;
            }
            slider_track_color_inactive = new ColorStateList(
                    new int[][]{
                            StateSet.get(StateSet.VIEW_STATE_ENABLED),
                            StateSet.WILD_CARD
                    },
                    new int[]{
                            colorSecondaryContainer,
                            modulateColor(colorOnSurface, 0.12f)
                    }
            );
            return slider_track_color_inactive;
        }

        // added by Modern UI
        private ColorStateList secondary_progress_tint;
        private ColorStateList secondary_progress_tint() {
            if (secondary_progress_tint != null) {
                return secondary_progress_tint;
            }
            secondary_progress_tint = new ColorStateList(
                    new int[][]{
                            StateSet.get(StateSet.VIEW_STATE_ENABLED),
                            StateSet.WILD_CARD
                    },
                    new int[]{
                            colorPrimaryContainer,
                            Color.TRANSPARENT
                    }
            );
            return secondary_progress_tint;
        }
    }

    static <T> T fromCache(Resources.Theme theme, Function<ThemedCache, T> fn) {
        synchronized (gCache) {
            ThemedCache cache = gCache.get(theme.getKey());
            if (cache == null) {
                cache = new ThemedCache(theme);
                gCache.put(theme.getKey(), cache);
            }
            return fn.apply(cache);
        }
    }

    static int dp(float value, Resources resources) {
        final float f = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DP,
                value, resources.getDisplayMetrics());
        final int res = (int) (f >= 0 ? f + 0.5f : f - 0.5f);
        if (res != 0) return res;
        if (value == 0) return 0;
        if (value > 0) return 1;
        return -1;
    }

    public static void addToResources(SystemResourcesBuilder b) {
        addStylesToResources(b);
        {
            SystemTheme t = createMaterial(true);
            var style = b.newStyle(R.style.Theme_Material3_Dark.entry(), "");

            addColorsToTheme(t, style);

            style.addReference(R.attr.textAppearanceDisplayLarge, R.style.TextAppearance_Material3_DisplayLarge);
            style.addReference(R.attr.textAppearanceDisplayMedium, R.style.TextAppearance_Material3_DisplayMedium);
            style.addReference(R.attr.textAppearanceDisplaySmall, R.style.TextAppearance_Material3_DisplaySmall);
            style.addReference(R.attr.textAppearanceHeadlineLarge, R.style.TextAppearance_Material3_HeadlineLarge);
            style.addReference(R.attr.textAppearanceHeadlineMedium, R.style.TextAppearance_Material3_HeadlineMedium);
            style.addReference(R.attr.textAppearanceHeadlineSmall, R.style.TextAppearance_Material3_HeadlineSmall);
            style.addReference(R.attr.textAppearanceTitleLarge, R.style.TextAppearance_Material3_TitleLarge);
            style.addReference(R.attr.textAppearanceTitleMedium, R.style.TextAppearance_Material3_TitleMedium);
            style.addReference(R.attr.textAppearanceTitleSmall, R.style.TextAppearance_Material3_TitleSmall);
            style.addReference(R.attr.textAppearanceBodyLarge, R.style.TextAppearance_Material3_BodyLarge);
            style.addReference(R.attr.textAppearanceBodyMedium, R.style.TextAppearance_Material3_BodyMedium);
            style.addReference(R.attr.textAppearanceBodySmall, R.style.TextAppearance_Material3_BodySmall);
            style.addReference(R.attr.textAppearanceLabelLarge, R.style.TextAppearance_Material3_LabelLarge);
            style.addReference(R.attr.textAppearanceLabelMedium, R.style.TextAppearance_Material3_LabelMedium);
            style.addReference(R.attr.textAppearanceLabelSmall, R.style.TextAppearance_Material3_LabelSmall);

            style.addAttribute(R.attr.colorEdgeEffect, R.attr.colorPrimary);

            style.addAttribute(R.attr.colorControlNormal, R.attr.textColorSecondary);
            style.addAttribute(R.attr.colorControlActivated, R.attr.colorAccent);
            style.addColor(R.attr.colorControlHighlight, 0x33ffffff);
            style.addColor(R.attr.colorButtonNormal, 0xff5a595b);

            style.addColor(R.attr.colorAccent, 0xFFD9E2FF);

            style.addReference(R.attr.textAppearance, R.style.TextAppearance);
            style.addReference(R.attr.buttonStyle, R.style.Widget_Material3_Button);
            style.addReference(R.attr.radioButtonStyle, R.style.Widget_Material3_CompoundButton_RadioButton);
            style.addReference(R.attr.radioButtonStyleMenuItem, R.style.Widget_Material3_CompoundButton_RadioButton_MenuItem);
            style.addReference(R.attr.switchStyle, R.style.Widget_Material3_CompoundButton_Switch);
            style.addReference(R.attr.progressBarStyle, R.style.Widget_Material3_ProgressBar);
            style.addReference(R.attr.progressBarStyleSmall, R.style.Widget_Material3_ProgressBar_Small);
            style.addReference(R.attr.progressBarStyleHorizontal, R.style.Widget_Material3_ProgressBar_Horizontal);
            style.addReference(R.attr.progressBarStyleVertical, R.style.Widget_Material3_ProgressBar_Vertical);
            style.addReference(R.attr.seekBarStyle, R.style.Widget_Material3_SeekBar);
            style.addReference(R.attr.popupMenuStyle, R.style.Widget_Material3_PopupMenu);
            style.addReference(R.attr.contextPopupMenuStyle, R.style.Widget_Material3_PopupMenu_ContextMenu);
            style.addReference(R.attr.listPopupWindowStyle, R.style.Widget_Material3_PopupMenu_ListPopupWindow);
        }
    }

    private static void addColorsToTheme(SystemTheme t, SystemResourcesBuilder.Style style) {

        style.addColor(R.attr.colorPrimary, t.colorPrimary);
        style.addColor(R.attr.colorOnPrimary, t.colorOnPrimary);
        style.addColor(R.attr.colorPrimaryInverse, t.colorPrimaryInverse);
        style.addColor(R.attr.colorPrimaryContainer, t.colorPrimaryContainer);
        style.addColor(R.attr.colorOnPrimaryContainer, t.colorOnPrimaryContainer);
        style.addColor(R.attr.colorPrimaryFixed, t.colorPrimaryFixed);
        style.addColor(R.attr.colorPrimaryFixedDim, t.colorPrimaryFixedDim);
        style.addColor(R.attr.colorOnPrimaryFixed, t.colorOnPrimaryFixed);
        style.addColor(R.attr.colorOnPrimaryFixedVariant, t.colorOnPrimaryFixedVariant);
        style.addColor(R.attr.colorSecondary, t.colorSecondary);
        style.addColor(R.attr.colorOnSecondary, t.colorOnSecondary);
        style.addColor(R.attr.colorSecondaryContainer, t.colorSecondaryContainer);
        style.addColor(R.attr.colorOnSecondaryContainer, t.colorOnSecondaryContainer);
        style.addColor(R.attr.colorSecondaryFixed, t.colorSecondaryFixed);
        style.addColor(R.attr.colorSecondaryFixedDim, t.colorSecondaryFixedDim);
        style.addColor(R.attr.colorOnSecondaryFixed, t.colorOnSecondaryFixed);
        style.addColor(R.attr.colorOnSecondaryFixedVariant, t.colorOnSecondaryFixedVariant);
        style.addColor(R.attr.colorTertiary, t.colorTertiary);
        style.addColor(R.attr.colorOnTertiary, t.colorOnTertiary);
        style.addColor(R.attr.colorTertiaryContainer, t.colorTertiaryContainer);
        style.addColor(R.attr.colorOnTertiaryContainer, t.colorOnTertiaryContainer);
        style.addColor(R.attr.colorTertiaryFixed, t.colorTertiaryFixed);
        style.addColor(R.attr.colorTertiaryFixedDim, t.colorTertiaryFixedDim);
        style.addColor(R.attr.colorOnTertiaryFixed, t.colorOnTertiaryFixed);
        style.addColor(R.attr.colorOnTertiaryFixedVariant, t.colorOnTertiaryFixedVariant);
        style.addColor(R.attr.colorBackground, t.colorBackground);
        style.addColor(R.attr.colorOnBackground, t.colorOnBackground);
        style.addColor(R.attr.colorSurface, t.colorSurface);
        style.addColor(R.attr.colorOnSurface, t.colorOnSurface);
        style.addColor(R.attr.colorSurfaceVariant, t.colorSurfaceVariant);
        style.addColor(R.attr.colorOnSurfaceVariant, t.colorOnSurfaceVariant);
        style.addColor(R.attr.colorSurfaceInverse, t.colorSurfaceInverse);
        style.addColor(R.attr.colorOnSurfaceInverse, t.colorOnSurfaceInverse);
        style.addColor(R.attr.colorSurfaceBright, t.colorSurfaceBright);
        style.addColor(R.attr.colorSurfaceDim, t.colorSurfaceDim);
        style.addColor(R.attr.colorSurfaceContainer, t.colorSurfaceContainer);
        style.addColor(R.attr.colorSurfaceContainerLow, t.colorSurfaceContainerLow);
        style.addColor(R.attr.colorSurfaceContainerHigh, t.colorSurfaceContainerHigh);
        style.addColor(R.attr.colorSurfaceContainerLowest, t.colorSurfaceContainerLowest);
        style.addColor(R.attr.colorSurfaceContainerHighest, t.colorSurfaceContainerHighest);
        style.addColor(R.attr.colorOutline, t.colorOutline);
        style.addColor(R.attr.colorOutlineVariant, t.colorOutlineVariant);
        style.addColor(R.attr.colorError, t.colorError);
        style.addColor(R.attr.colorOnError, t.colorOnError);
        style.addColor(R.attr.colorErrorContainer, t.colorErrorContainer);
        style.addColor(R.attr.colorOnErrorContainer, t.colorOnErrorContainer);

        style.addColor(R.attr.textColorPrimary, (resources, theme) -> t.textColorPrimary);
        style.addColor(R.attr.textColorPrimaryInverse, (resources, theme) -> t.textColorPrimaryInverse);
        style.addColor(R.attr.textColorSecondary, (resources, theme) -> t.textColorSecondary);
        style.addColor(R.attr.textColorSecondaryInverse, (resources, theme) -> t.textColorSecondaryInverse);
        style.addColor(R.attr.textColorPrimaryDisableOnly, (resources, theme) -> t.textColorPrimaryDisableOnly);
        style.addColor(R.attr.textColorHighlight, t.textColorHighlight);
        style.addColor(R.attr.textColorHint, (resources, theme) -> t.textColorHint);
        style.addColor(R.attr.textColorLink, (resources, theme) -> t.textColorLink);
    }

    private static void addStylesToResources(SystemResourcesBuilder b) {
        {
            var style = b.newStyle(R.style.TextAppearance.entry(), "");
            style.addAttribute(R.attr.textColor, R.attr.textColorPrimary);
            style.addAttribute(R.attr.textColorHint, R.attr.textColorHint);
            style.addAttribute(R.attr.textColorHighlight, R.attr.textColorHighlight);
            style.addAttribute(R.attr.textColorLink, R.attr.textColorLink);
            style.addDimension(R.attr.textSize, 16, TypedValue.COMPLEX_UNIT_SP);
        }
        // Headline
        {
            var style = b.newStyle(R.style.TextAppearance_Material3_DisplayLarge.entry(),
                    R.style.TextAppearance.entry());
            style.addAttribute(R.attr.textColor, R.attr.textColorSecondary);
            style.addDimension(R.attr.textSize, 57, TypedValue.COMPLEX_UNIT_SP);
        }
        {
            var style = b.newStyle(R.style.TextAppearance_Material3_DisplayMedium.entry(),
                    R.style.TextAppearance.entry());
            style.addAttribute(R.attr.textColor, R.attr.textColorSecondary);
            style.addDimension(R.attr.textSize, 45, TypedValue.COMPLEX_UNIT_SP);
        }
        {
            var style = b.newStyle(R.style.TextAppearance_Material3_DisplaySmall.entry(),
                    R.style.TextAppearance.entry());
            style.addAttribute(R.attr.textColor, R.attr.textColorSecondary);
            style.addDimension(R.attr.textSize, 36, TypedValue.COMPLEX_UNIT_SP);
        }
        // Headline
        {
            var style = b.newStyle(R.style.TextAppearance_Material3_HeadlineLarge.entry(),
                    R.style.TextAppearance.entry());
            style.addAttribute(R.attr.textColor, R.attr.textColorSecondary);
            style.addDimension(R.attr.textSize, 32, TypedValue.COMPLEX_UNIT_SP);
        }
        {
            var style = b.newStyle(R.style.TextAppearance_Material3_HeadlineMedium.entry(),
                    R.style.TextAppearance.entry());
            style.addAttribute(R.attr.textColor, R.attr.textColorPrimary);
            style.addDimension(R.attr.textSize, 28, TypedValue.COMPLEX_UNIT_SP);
        }
        {
            var style = b.newStyle(R.style.TextAppearance_Material3_HeadlineSmall.entry(),
                    R.style.TextAppearance.entry());
            style.addAttribute(R.attr.textColor, R.attr.textColorPrimary);
            style.addDimension(R.attr.textSize, 24, TypedValue.COMPLEX_UNIT_SP);
        }
        // Title
        {
            var style = b.newStyle(R.style.TextAppearance_Material3_TitleLarge.entry(), R.style.TextAppearance.entry());
            style.addAttribute(R.attr.textColor, R.attr.textColorPrimary);
            style.addDimension(R.attr.textSize, 22, TypedValue.COMPLEX_UNIT_SP);
        }
        {
            var style = b.newStyle(R.style.TextAppearance_Material3_TitleMedium.entry(),
                    R.style.TextAppearance.entry());
            style.addAttribute(R.attr.textColor, R.attr.textColorPrimary);
            style.addDimension(R.attr.textSize, 16, TypedValue.COMPLEX_UNIT_SP);
            style.addInteger(R.attr.textFontWeight, 500);
        }
        {
            var style = b.newStyle(R.style.TextAppearance_Material3_TitleSmall.entry(), R.style.TextAppearance.entry());
            style.addAttribute(R.attr.textColor, R.attr.textColorPrimary);
            style.addDimension(R.attr.textSize, 14, TypedValue.COMPLEX_UNIT_SP);
            style.addInteger(R.attr.textFontWeight, 500);
        }
        // Body
        {
            var style = b.newStyle(R.style.TextAppearance_Material3_BodyLarge.entry(), R.style.TextAppearance.entry());
            style.addAttribute(R.attr.textColor, R.attr.textColorPrimary);
            style.addDimension(R.attr.textSize, 16, TypedValue.COMPLEX_UNIT_SP);
        }
        {
            var style = b.newStyle(R.style.TextAppearance_Material3_BodyMedium.entry(), R.style.TextAppearance.entry());
            style.addAttribute(R.attr.textColor, R.attr.textColorPrimary);
            style.addDimension(R.attr.textSize, 14, TypedValue.COMPLEX_UNIT_SP);
        }
        {
            var style = b.newStyle(R.style.TextAppearance_Material3_BodySmall.entry(), R.style.TextAppearance.entry());
            style.addAttribute(R.attr.textColor, R.attr.textColorSecondary);
            style.addDimension(R.attr.textSize, 12, TypedValue.COMPLEX_UNIT_SP);
        }
        // Label
        {
            var style = b.newStyle(R.style.TextAppearance_Material3_LabelLarge.entry(), R.style.TextAppearance.entry());
            style.addAttribute(R.attr.textColor, R.attr.textColorPrimary);
            style.addDimension(R.attr.textSize, 14, TypedValue.COMPLEX_UNIT_SP);
            style.addInteger(R.attr.textFontWeight, 500);
        }
        {
            var style = b.newStyle(R.style.TextAppearance_Material3_LabelMedium.entry(),
                    R.style.TextAppearance.entry());
            style.addAttribute(R.attr.textColor, R.attr.textColorSecondary);
            style.addDimension(R.attr.textSize, 12, TypedValue.COMPLEX_UNIT_SP);
            style.addInteger(R.attr.textFontWeight, 500);
        }
        {
            var style = b.newStyle(R.style.TextAppearance_Material3_LabelSmall.entry(), R.style.TextAppearance.entry());
            style.addAttribute(R.attr.textColor, R.attr.textColorSecondary);
            style.addDimension(R.attr.textSize, 11, TypedValue.COMPLEX_UNIT_SP);
            style.addInteger(R.attr.textFontWeight, 500);
        }
        // Buttons
        {
            var style = b.newStyle(R.style.Widget_Material3_Button.entry(), "");
            style.addDimension(R.attr.minHeight, 32, TypedValue.COMPLEX_UNIT_DP);
            style.addDimension(R.attr.minWidth, 80, TypedValue.COMPLEX_UNIT_DP);
            style.addBoolean(R.attr.focusable, true);
            style.addBoolean(R.attr.clickable, true);
            style.addFlags(R.attr.gravity, Gravity.CENTER);
            style.addDimension(R.attr.maxWidth, 320, TypedValue.COMPLEX_UNIT_DP);
            style.addDimension(R.attr.paddingLeft, 20, TypedValue.COMPLEX_UNIT_DP);
            style.addDimension(R.attr.paddingRight, 20, TypedValue.COMPLEX_UNIT_DP);
            style.addDimension(R.attr.paddingTop, 2, TypedValue.COMPLEX_UNIT_DP);
            style.addDimension(R.attr.paddingBottom, 2, TypedValue.COMPLEX_UNIT_DP);
            style.addAttribute(R.attr.textAppearance, R.attr.textAppearanceLabelLarge);
            style.addColor(R.attr.textColor, (resources, theme) ->
                    fromCache(theme, cache -> cache.button_foreground_color_selector(cache.colorOnPrimary)));
            style.addDrawable(R.attr.background, (resources, theme) -> {
                var backgroundTint = fromCache(theme,
                        cache -> cache.button_background_color_selector(cache.colorPrimary));
                var rippleColor = fromCache(theme,
                        cache -> cache.button_ripple_color_selector(cache.colorOnPrimary));

                var background = new ShapeDrawable();
                background.setCornerRadius(1000);
                background.setColor(backgroundTint);
                return new RippleDrawable(rippleColor, background, null);
            });
        }
        {
            var style = b.newStyle(R.style.Widget_Material3_Button_TonalButton.entry(),
                    R.style.Widget_Material3_Button.entry());
            style.addColor(R.attr.textColor, (resources, theme) ->
                    fromCache(theme, cache -> cache.button_foreground_color_selector(cache.colorOnSecondaryContainer)));
            style.addDrawable(R.attr.background, (resources, theme) -> {
                var backgroundTint = fromCache(theme,
                        cache -> cache.button_background_color_selector(cache.colorSecondaryContainer));
                var rippleColor = fromCache(theme,
                        cache -> cache.button_ripple_color_selector(cache.colorOnSecondaryContainer));

                var background = new ShapeDrawable();
                background.setCornerRadius(1000);
                background.setColor(backgroundTint);
                return new RippleDrawable(rippleColor, background, null);
            });
        }
        {
            var style = b.newStyle(R.style.Widget_Material3_Button_TextButton.entry(),
                    R.style.Widget_Material3_Button.entry());
            style.addDimension(R.attr.paddingLeft, 8, TypedValue.COMPLEX_UNIT_DP);
            style.addDimension(R.attr.paddingRight, 8, TypedValue.COMPLEX_UNIT_DP);
            style.addColor(R.attr.textColor, (resources, theme) ->
                    fromCache(theme, cache -> cache.text_button_foreground_color_selector(cache.colorPrimary)));
            style.addDrawable(R.attr.background, (resources, theme) -> {
                var backgroundTint = fromCache(theme, cache -> cache.text_button_background_color_selector(0));
                var rippleColor = fromCache(theme,
                        cache -> cache.text_button_ripple_color_selector(cache.colorPrimary));

                var background = new ShapeDrawable();
                background.setCornerRadius(1000);
                background.setColor(backgroundTint);
                return new RippleDrawable(rippleColor, background, null);
            });
        }
        {
            var style = b.newStyle(R.style.Widget_Material3_Button_OutlinedButton.entry(),
                    R.style.Widget_Material3_Button.entry());
            style.addColor(R.attr.textColor, (resources, theme) ->
                    fromCache(theme, cache -> cache.text_button_foreground_color_selector(cache.colorPrimary)));
            style.addDrawable(R.attr.background, (resources, theme) -> {
                var backgroundTint = fromCache(theme, cache -> cache.text_button_background_color_selector(0));
                var rippleColor = fromCache(theme,
                        cache -> cache.text_button_ripple_color_selector(cache.colorPrimary));
                var strokeWidth = dp(1, resources);
                var strokeColor = fromCache(theme, ThemedCache::button_outline_color_selector);

                var background = new ShapeDrawable();
                background.setCornerRadius(1000);
                background.setColor(backgroundTint);
                background.setStroke(strokeWidth, strokeColor);
                return new RippleDrawable(rippleColor, background, null);
            });
        }
        // Compound buttons
        {
            var style = b.newStyle(R.style.Widget_CompoundButton.entry(), "");
            style.addBoolean(R.attr.focusable, true);
            style.addBoolean(R.attr.clickable, true);
            style.addFlags(R.attr.gravity, Gravity.CENTER_VERTICAL | Gravity.START);
            style.addAttribute(R.attr.textColor, R.attr.textColorPrimaryDisableOnly);
        }
        {
            var style = b.newStyle(R.style.Widget_Material3_CompoundButton_RadioButton.entry(),
                    R.style.Widget_CompoundButton.entry());
            style.addAttribute(R.attr.textAppearance, R.attr.textAppearanceBodyMedium);
            style.addDrawable(R.attr.button, (resources, theme) -> {
                var buttonTint = fromCache(theme, ThemedCache::radio_button_tint);
                var button = new RadioButtonDrawable(resources, true, true, true);
                button.setTintList(buttonTint);
                return button;
            });
            style.addDrawable(R.attr.background, (resources, theme) -> {
                var rippleTint = fromCache(theme, ThemedCache::radio_button_ripple_tint);
                var ripple = new RippleDrawable(rippleTint, null, null);
                var radius = dp(16, resources);
                ripple.setRadius(radius);
                return ripple;
            });
        }
        {
            var style = b.newStyle(R.style.Widget_Material3_CompoundButton_RadioButton_MenuItem.entry(),
                    R.style.Widget_CompoundButton.entry());
            style.addAttribute(R.attr.textAppearance, R.attr.textAppearanceBodyMedium);
            style.addDrawable(R.attr.button, (resources, theme) -> {
                var buttonTint = fromCache(theme, ThemedCache::radio_button_tint);
                var button = new RadioButtonDrawable(resources, false, false, false);
                button.setTintList(buttonTint);
                return button;
            });
        }
        {
            var style = b.newStyle(R.style.Widget_Material3_CompoundButton_Switch.entry(),
                    R.style.Widget_CompoundButton.entry());
            style.addAttribute(R.attr.textAppearance, R.attr.textAppearanceBodyMedium);
            style.addDimension(R.attr.switchMinWidth, 52, TypedValue.COMPLEX_UNIT_DP);
            style.addDimension(R.attr.switchPadding, 16, TypedValue.COMPLEX_UNIT_DP);
            style.addDrawable(R.attr.track, (resources, theme) -> {
                var tint = fromCache(theme, ThemedCache::switch_track_tint);
                var decorationTint = fromCache(theme, ThemedCache::switch_track_decoration_tint);
                var track = new ShapeDrawable();
                track.setShape(ShapeDrawable.RECTANGLE);
                track.setSize(dp(52, resources), dp(32, resources));
                track.setCornerRadius(dp(16, resources));
                track.setColor(tint);
                track.setStroke(dp(2, resources), decorationTint);
                return track;
            });
            style.addDrawable(R.attr.thumb, (resources, theme) -> {
                var tint = fromCache(theme, ThemedCache::switch_thumb_tint);
                var thumb = new SwitchThumbDrawable(resources, true, true);
                thumb.setTintList(tint);
                return thumb;
            });
        }
        // Progress bars
        {
            var style = b.newStyle(R.style.Widget_Material3_ProgressBar.entry(), "");
            style.addDimension(R.attr.minWidth, 48, TypedValue.COMPLEX_UNIT_DP);
            style.addDimension(R.attr.minHeight, 48, TypedValue.COMPLEX_UNIT_DP);
            style.addDrawable(R.attr.indeterminateDrawable, (resources, theme) -> {
                var drawable = new CircularIndeterminateDrawable(resources, 40, 4, 4);
                // there is no disabled color in indeterminate mode
                drawable.setIndicatorColor(fromCache(theme, cache -> cache.colorPrimary));
                drawable.setTrackColor(fromCache(theme, cache -> cache.colorSecondaryContainer));
                return drawable;
            });
            style.addDrawable(R.attr.progressDrawable, (resources, theme) -> circular_progress_drawable(resources, theme, 40, 4, 4));
        }
        {
            var style = b.newStyle(R.style.Widget_Material3_ProgressBar_Small.entry(), "");
            style.addDimension(R.attr.minWidth, 36, TypedValue.COMPLEX_UNIT_DP);
            style.addDimension(R.attr.minHeight, 36, TypedValue.COMPLEX_UNIT_DP);
            style.addDrawable(R.attr.indeterminateDrawable, (resources, theme) -> {
                var drawable = new CircularIndeterminateDrawable(resources, 28, 4, 3);
                // there is no disabled color in indeterminate mode
                drawable.setIndicatorColor(fromCache(theme, cache -> cache.colorPrimary));
                drawable.setTrackColor(fromCache(theme, cache -> cache.colorSecondaryContainer));
                return drawable;
            });
            style.addDrawable(R.attr.progressDrawable, (resources, theme) -> circular_progress_drawable(resources, theme, 28, 4, 3));
        }
        {
            var style = b.newStyle(R.style.Widget_Material3_ProgressBar_ExtraSmall.entry(), "");
            style.addDimension(R.attr.minWidth, 24, TypedValue.COMPLEX_UNIT_DP);
            style.addDimension(R.attr.minHeight, 24, TypedValue.COMPLEX_UNIT_DP);
            style.addDrawable(R.attr.indeterminateDrawable, (resources, theme) -> {
                var drawable = new CircularIndeterminateDrawable(resources, 20, 2, 2.5f);
                // there is no disabled color in indeterminate mode
                drawable.setIndicatorColor(fromCache(theme, cache -> cache.colorPrimary));
                drawable.setTrackColor(fromCache(theme, cache -> cache.colorSecondaryContainer));
                return drawable;
            });
            style.addDrawable(R.attr.progressDrawable, (resources, theme) -> circular_progress_drawable(resources, theme, 20, 2, 2.5f));
        }
        {
            var style = b.newStyle(R.style.Widget_Material3_ProgressBar_Horizontal.entry(), "");
            style.addDrawable(R.attr.indeterminateDrawable, (resources, theme) -> {
                var drawable = new LinearIndeterminateDrawable(resources, false);
                // there is no disabled color in indeterminate mode
                drawable.setIndicatorColor(fromCache(theme, cache -> cache.colorPrimary));
                drawable.setTrackColor(fromCache(theme, cache -> cache.colorSecondaryContainer));
                return drawable;
            });
            style.addDrawable(R.attr.progressDrawable, (resources, theme) -> linear_progress_drawable(resources, theme, false));
            style.addBoolean(R.attr.mirrorForRtl, true);
        }
        {
            var style = b.newStyle(R.style.Widget_Material3_ProgressBar_Vertical.entry(), "");
            style.addDrawable(R.attr.indeterminateDrawable, (resources, theme) -> {
                var drawable = new LinearIndeterminateDrawable(resources, true);
                // there is no disabled color in indeterminate mode
                drawable.setIndicatorColor(fromCache(theme, cache -> cache.colorPrimary));
                drawable.setTrackColor(fromCache(theme, cache -> cache.colorSecondaryContainer));
                return drawable;
            });
            style.addDrawable(R.attr.progressDrawable, (resources, theme) -> linear_progress_drawable(resources, theme, true));
        }
        // Seek bars
        {
            var style = b.newStyle(R.style.Widget_Material3_SeekBar.entry(), "");
            style.addDrawable(R.attr.progressDrawable, (resources, theme) -> {
                var background = new ShapeDrawable();
                background.setShape(ShapeDrawable.HLINE);
                background.setSize(-1, dp(10, resources));
                background.setCornerRadius(dp(5, resources));
                background.setColor(fromCache(theme, ThemedCache::slider_track_color_inactive));
                var progress = new ShapeDrawable();
                progress.setShape(ShapeDrawable.HLINE);
                progress.setSize(-1, dp(10, resources));
                progress.setCornerRadius(dp(5, resources));
                progress.setColor(fromCache(theme, ThemedCache::slider_track_color_active));
                var scaledProgress = new ScaleDrawable(progress, Gravity.LEFT, 1.0f, -1.0f);
                var track = new LayerDrawable(background, scaledProgress);
                track.setId(0, R.id.background);
                track.setId(1, R.id.progress);
                track.setLayerGravity(0, Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL);
                track.setLayerGravity(1, Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL);
                return track;
            });
            style.addBoolean(R.attr.splitTrack, true);
            style.addBoolean(R.attr.mirrorForRtl, true);
            style.addBoolean(R.attr.focusable, true);
            style.addDrawable(R.attr.thumb, (resources, theme) -> {
                var thumb = new SeekbarThumbDrawable(resources);
                thumb.setTintList(fromCache(theme, ThemedCache::slider_track_color_active));
                return thumb;
            });
            style.addDimension(R.attr.paddingLeft, 16, TypedValue.COMPLEX_UNIT_DP);
            style.addDimension(R.attr.paddingRight, 16, TypedValue.COMPLEX_UNIT_DP);
        }
        {
            var style = b.newStyle(R.style.Widget_Material3_SeekBar_Discrete.entry(), R.style.Widget_Material3_SeekBar.entry());
            style.addDrawable(R.attr.tickMark, (resources, theme) -> {
                var tick = new ShapeDrawable();
                tick.setShape(ShapeDrawable.CIRCLE);
                int size = dp(4, resources);
                tick.setSize(size, size);
                tick.setColor(fromCache(theme, ThemedCache::slider_track_color_inactive));
                tick.setUseLevelForShape(false);
                return tick;
            });
        }
        // Popup menus
        {
            var style = b.newStyle(R.style.Widget_Material3_PopupMenu.entry(), "");
            style.addDimension(R.attr.popupElevation, 3, TypedValue.COMPLEX_UNIT_DP);
            style.addDrawable(R.attr.popupBackground, (resources, theme) -> {
                var popupBackground = new ShapeDrawable();
                popupBackground.setShape(ShapeDrawable.RECTANGLE);
                popupBackground.setCornerRadius(dp(4, resources));
                int color = fromCache(theme, cache -> cache.colorSurfaceContainer);
                popupBackground.setColor(color);
                int pad = dp(2, resources);
                popupBackground.setPadding(pad, pad, pad, pad);
                return popupBackground;
            });
        }
        {
            var style = b.newStyle(R.style.Widget_Material3_PopupMenu_ContextMenu.entry(),
                    R.style.Widget_Material3_PopupMenu.entry());
            style.addBoolean(R.attr.overlapAnchor, true);
        }
        {
            var style = b.newStyle(R.style.Widget_Material3_PopupMenu_ListPopupWindow.entry(),
                    R.style.Widget_Material3_PopupMenu.entry());
            // see spinner
        }
    }

    private static Drawable circular_progress_drawable(Resources resources, Resources.Theme theme,
                                                       int size, int inset, float thickness) {
        var background = new ShapeDrawable();
        background.setShape(ShapeDrawable.RING);
        background.setInnerRadius(dp((size - inset - inset) * 0.5f, resources));
        background.setThickness(dp(thickness, resources));
        background.setColor(fromCache(theme, ThemedCache::slider_track_color_inactive));
        background.setUseLevelForShape(false);
        // there is no secondary progress in circular shape
        var progress = new ShapeDrawable();
        progress.setShape(ShapeDrawable.RING);
        progress.setInnerRadius(dp((size - inset - inset) * 0.5f, resources));
        progress.setThickness(dp(thickness, resources));
        progress.setCornerRadius(1);
        progress.setColor(fromCache(theme, ThemedCache::slider_track_color_active));
        var track = new LayerDrawable(background, progress);
        track.setId(0, R.id.background);
        track.setId(1, R.id.progress);
        track.setLayerGravity(0, Gravity.CENTER);
        track.setLayerGravity(1, Gravity.CENTER);
        return track;
    }

    private static Drawable linear_progress_drawable(Resources resources, Resources.Theme theme,
                                                     boolean vertical) {
        LayerDrawable track;
        if (vertical) {
            var background = new ShapeDrawable();
            background.setShape(ShapeDrawable.VLINE);
            background.setSize(dp(4, resources), -1);
            background.setCornerRadius(1);
            background.setStroke(dp(4, resources), fromCache(theme, ThemedCache::slider_track_color_inactive));
            var secondaryProgress = new ShapeDrawable();
            secondaryProgress.setShape(ShapeDrawable.VLINE);
            secondaryProgress.setSize(dp(4, resources), -1);
            secondaryProgress.setCornerRadius(1);
            secondaryProgress.setStroke(dp(4, resources), fromCache(theme, ThemedCache::secondary_progress_tint));
            var progress = new ShapeDrawable();
            progress.setShape(ShapeDrawable.VLINE);
            progress.setSize(dp(4, resources), -1);
            progress.setCornerRadius(1);
            progress.setStroke(dp(4, resources), fromCache(theme, ThemedCache::slider_track_color_active));
            var scaledSecondaryProgress = new ScaleDrawable(secondaryProgress, Gravity.BOTTOM, 1.0f, -1.0f);
            var scaledProgress = new ScaleDrawable(progress, Gravity.BOTTOM, 1.0f, -1.0f);
            track = new LayerDrawable(background, scaledSecondaryProgress, scaledProgress);
            track.setLayerGravity(0, Gravity.CENTER_HORIZONTAL | Gravity.FILL_VERTICAL);
            track.setLayerGravity(1, Gravity.CENTER_HORIZONTAL | Gravity.FILL_VERTICAL);
            track.setLayerGravity(2, Gravity.CENTER_HORIZONTAL | Gravity.FILL_VERTICAL);
        } else {
            var background = new ShapeDrawable();
            background.setShape(ShapeDrawable.HLINE);
            background.setSize(-1, dp(4, resources));
            background.setCornerRadius(1);
            background.setStroke(dp(4, resources), fromCache(theme, ThemedCache::slider_track_color_inactive));
            var secondaryProgress = new ShapeDrawable();
            secondaryProgress.setShape(ShapeDrawable.HLINE);
            secondaryProgress.setSize(-1, dp(4, resources));
            secondaryProgress.setCornerRadius(1);
            secondaryProgress.setStroke(dp(4, resources), fromCache(theme, ThemedCache::secondary_progress_tint));
            var progress = new ShapeDrawable();
            progress.setShape(ShapeDrawable.HLINE);
            progress.setSize(-1, dp(4, resources));
            progress.setCornerRadius(1);
            progress.setStroke(dp(4, resources), fromCache(theme, ThemedCache::slider_track_color_active));
            var scaledSecondaryProgress = new ScaleDrawable(secondaryProgress, Gravity.LEFT, 1.0f, -1.0f);
            var scaledProgress = new ScaleDrawable(progress, Gravity.LEFT, 1.0f, -1.0f);
            track = new LayerDrawable(background, scaledSecondaryProgress, scaledProgress);
            track.setLayerGravity(0, Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL);
            track.setLayerGravity(1, Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL);
            track.setLayerGravity(2, Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL);
        }
        track.setId(0, R.id.background);
        track.setId(1, R.id.secondaryProgress);
        track.setId(2, R.id.progress);
        return track;
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

    public void applySpinnerStyle(Spinner spinner) {
        //TODO background (arrow indicator)
        var listSelector = new RippleDrawable(colorControlHighlight(), null, new ColorDrawable(~0));
        spinner.setDropDownSelector(listSelector);
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
