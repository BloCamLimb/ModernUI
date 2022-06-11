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

package icyllis.modernui.forge;

import icyllis.modernui.R;
import icyllis.modernui.animation.*;
import icyllis.modernui.forge.Config.Client;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.fragment.FragmentTransaction;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.drawable.StateListDrawable;
import icyllis.modernui.material.MaterialDrawable;
import icyllis.modernui.math.FMath;
import icyllis.modernui.math.Rect;
import icyllis.modernui.text.InputFilter;
import icyllis.modernui.text.TextPaint;
import icyllis.modernui.text.method.DigitsInputFilter;
import icyllis.modernui.textmc.ModernUITextMC;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.util.StateSet;
import icyllis.modernui.view.*;
import icyllis.modernui.view.View.OnLayoutChangeListener;
import icyllis.modernui.widget.*;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static icyllis.modernui.view.View.dp;
import static icyllis.modernui.view.ViewGroup.LayoutParams.*;

public class CenterFragment extends Fragment {

    public static final int BACKGROUND_COLOR = 0xc0292a2c;
    public static final int THEME_COLOR = 0xffcda398;
    public static final int THEME_COLOR_2 = 0xffcd98a3;

    @Nullable
    @Override
    public View onCreateView(@Nullable ViewGroup container, @Nullable DataSet savedInstanceState) {
        final int dp6 = dp(6);

        var content = new LinearLayout();
        content.setId(R.id.content);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackground(new Background());
        {
            var title = new TextView();
            title.setId(R.id.title);
            title.setText(I18n.get("gui.modernui.center.title"));
            title.setTextSize(22);
            title.setTextStyle(TextPaint.BOLD);

            var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            params.setMargins(0, dp(12), 0, dp(12));
            content.addView(title, params);
        }

        {
            var panels = new LinearLayout();
            panels.setOrientation(LinearLayout.HORIZONTAL);

            boolean firstDown = Math.random() < 0.5;

            {
                ScrollView primary = new ScrollView();
                primary.addView(createPrimaryPanel(), MATCH_PARENT, WRAP_CONTENT);
                var params = new LinearLayout.LayoutParams(0, MATCH_PARENT, 1);
                params.setMargins(dp6, dp6, dp6, dp6);
                panels.addView(primary, params);

                ObjectAnimator animator = ObjectAnimator.ofFloat(primary, View.TRANSLATION_Y,
                        dp(firstDown ? -200 : 200), 0);
                animator.setInterpolator(TimeInterpolator.DECELERATE_CUBIC);
                primary.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                                               int oldTop, int oldRight, int oldBottom) {
                        animator.start();
                        v.removeOnLayoutChangeListener(this);
                    }
                });
                primary.setEdgeEffectColor(THEME_COLOR);
            }

            {
                ScrollView secondary = new ScrollView();
                secondary.addView(createSecondaryPanel(), MATCH_PARENT, WRAP_CONTENT);
                var params = new LinearLayout.LayoutParams(0, MATCH_PARENT, 1);
                params.setMargins(dp6, dp6, dp6, dp6);
                panels.addView(secondary, params);

                ObjectAnimator animator = ObjectAnimator.ofFloat(secondary, View.TRANSLATION_Y,
                        dp(firstDown ? 200 : -200), 0);
                animator.setInterpolator(TimeInterpolator.DECELERATE_CUBIC);
                secondary.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                                               int oldTop, int oldRight, int oldBottom) {
                        animator.start();
                        v.removeOnLayoutChangeListener(this);
                    }
                });
                secondary.setEdgeEffectColor(THEME_COLOR);
            }

            var params = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
            content.addView(panels, params);
        }

        var params = new FrameLayout.LayoutParams(dp(720), dp(450));
        params.gravity = Gravity.CENTER;
        content.setLayoutParams(params);
        return content;
    }

    // extension settings for Minecraft
    @Nonnull
    private View createPrimaryPanel() {
        final int dp6 = dp(6);

        var base = new LinearLayout();
        base.setOrientation(LinearLayout.VERTICAL);
        {
            var screen = new RelativeLayout();

            {
                var title = new TextView();
                title.setId(R.id.title);
                title.setText(I18n.get("gui.modernui.center.category.screen"));
                title.setTextSize(16);
                title.setTextColor(THEME_COLOR);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                params.addRule(RelativeLayout.ALIGN_PARENT_START);
                params.setMargins(dp6, dp6, dp6, dp6);
                screen.addView(title, params);
            }

            {
                var view = new TextView();
                view.setId(12);
                view.setText(I18n.get("gui.modernui.center.screen.backgroundDuration"));
                view.setTextSize(14);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, R.id.title);
                params.addRule(RelativeLayout.ALIGN_START, R.id.title);
                screen.addView(view, params);

                var input = new EditText();
                input.setText(Config.CLIENT.backgroundDuration.get().toString());
                input.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                input.setTextSize(14);
                input.setFilters(DigitsInputFilter.getInstance(input.getTextLocale()), new InputFilter.LengthFilter(3));
                input.setPadding(dp(4), 0, dp(4), 0);
                input.setOnFocusChangeListener((__, hasFocus) -> {
                    if (!hasFocus) {
                        int radius = Integer.parseInt(input.getText().toString());
                        radius = FMath.clamp(radius, Client.ANIM_DURATION_MIN, Client.ANIM_DURATION_MAX);
                        input.setText(Integer.toString(radius));
                        if (radius != Config.CLIENT.backgroundDuration.get()) {
                            Config.CLIENT.backgroundDuration.set(radius);
                            Config.CLIENT.saveAndReload();
                        }
                    }
                });
                StateListDrawable drawable = new StateListDrawable();
                drawable.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), new InputBackground());
                drawable.setEnterFadeDuration(300);
                drawable.setExitFadeDuration(300);
                input.setBackground(drawable);

                params = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_BASELINE, 12);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp6, dp6, dp6, dp6);
                screen.addView(input, params);
            }

            {
                var view = new TextView();
                view.setId(16);
                view.setText(I18n.get("gui.modernui.center.screen.blurEffect"));
                view.setTextSize(14);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, 12);
                params.addRule(RelativeLayout.ALIGN_START, 12);
                screen.addView(view, params);

                var button = new SwitchButton();
                button.setCheckedColor(THEME_COLOR);
                button.setChecked(Config.CLIENT.blurEffect.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    Config.CLIENT.blurEffect.set(checked);
                    Config.CLIENT.saveAndReload();
                });

                params = new RelativeLayout.LayoutParams(dp(36), dp(16));
                params.addRule(RelativeLayout.ALIGN_TOP, 16);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp6, dp(3), dp6, dp(3));
                screen.addView(button, params);
            }

            {
                var view = new TextView();
                view.setId(18);
                view.setText(I18n.get("gui.modernui.center.screen.blurRadius"));
                view.setTextSize(14);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, 16);
                params.addRule(RelativeLayout.ALIGN_START, 16);
                screen.addView(view, params);

                var input = new EditText();
                input.setText(Config.CLIENT.blurRadius.get().toString());
                input.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                input.setTextSize(14);
                input.setFilters(DigitsInputFilter.getInstance(input.getTextLocale()), new InputFilter.LengthFilter(2));
                input.setPadding(dp(4), 0, dp(4), 0);
                input.setOnFocusChangeListener((__, hasFocus) -> {
                    if (!hasFocus) {
                        int radius = Integer.parseInt(input.getText().toString());
                        radius = FMath.clamp(radius, Client.BLUR_RADIUS_MIN, Client.BLUR_RADIUS_MAX);
                        input.setText(Integer.toString(radius));
                        if (radius != Config.CLIENT.blurRadius.get()) {
                            Config.CLIENT.blurRadius.set(radius);
                            Config.CLIENT.saveAndReload();
                        }
                    }
                });
                StateListDrawable drawable = new StateListDrawable();
                drawable.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), new InputBackground());
                drawable.setEnterFadeDuration(300);
                drawable.setExitFadeDuration(300);
                input.setBackground(drawable);

                params = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_BASELINE, 18);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp6, dp6, dp6, dp6);
                screen.addView(input, params);
            }

            {
                var view = new TextView();
                view.setId(20);
                view.setText(I18n.get("gui.modernui.center.screen.inventoryPause"));
                view.setTextSize(14);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, 18);
                params.addRule(RelativeLayout.ALIGN_START, 18);
                screen.addView(view, params);

                var button = new SwitchButton();
                button.setCheckedColor(THEME_COLOR);
                button.setChecked(Config.CLIENT.inventoryPause.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    Config.CLIENT.inventoryPause.set(checked);
                    Config.CLIENT.saveAndReload();
                });

                params = new RelativeLayout.LayoutParams(dp(36), dp(16));
                params.addRule(RelativeLayout.ALIGN_TOP, 20);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp6, dp(3), dp6, dp(3));
                screen.addView(button, params);
            }

            var params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            params.setMargins(dp(12), dp(12), dp(12), dp(18));
            base.addView(screen, params);
        }

        {
            var extension = new RelativeLayout();

            {
                var title = new TextView();
                title.setId(R.id.title);
                title.setText(I18n.get("gui.modernui.center.category.extension"));
                title.setTextSize(16);
                title.setTextColor(THEME_COLOR);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                params.addRule(RelativeLayout.ALIGN_PARENT_START);
                params.setMargins(dp6, dp6, dp6, dp6);
                extension.addView(title, params);
            }

            {
                var view = new TextView();
                view.setId(32);
                view.setText(I18n.get("gui.modernui.center.extension.ding"));
                view.setTextSize(14);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, R.id.title);
                params.addRule(RelativeLayout.ALIGN_START, R.id.title);
                extension.addView(view, params);

                var button = new SwitchButton();
                button.setCheckedColor(THEME_COLOR);
                button.setChecked(Config.CLIENT.ding.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    Config.CLIENT.ding.set(checked);
                    Config.CLIENT.saveAndReload();
                });

                params = new RelativeLayout.LayoutParams(dp(36), dp(16));
                params.addRule(RelativeLayout.ALIGN_TOP, 32);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp6, dp(3), dp6, dp(3));
                extension.addView(button, params);
            }

            {
                var view = new TextView();
                view.setId(34);
                view.setText(I18n.get("gui.modernui.center.extension.tooltip"));
                view.setTextSize(14);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, 32);
                params.addRule(RelativeLayout.ALIGN_START, 32);
                extension.addView(view, params);

                var button = new SwitchButton();
                button.setCheckedColor(THEME_COLOR);
                button.setChecked(Config.CLIENT.tooltip.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    Config.CLIENT.tooltip.set(checked);
                    Config.CLIENT.saveAndReload();
                });

                params = new RelativeLayout.LayoutParams(dp(36), dp(16));
                params.addRule(RelativeLayout.ALIGN_TOP, 34);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp6, dp(3), dp6, dp(3));
                extension.addView(button, params);
            }

            {
                var view = new TextView();
                view.setId(35);
                view.setText(I18n.get("gui.modernui.center.extension.tooltipDuration"));
                view.setTextSize(14);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, 34);
                params.addRule(RelativeLayout.ALIGN_START, 34);
                extension.addView(view, params);

                var input = new EditText();
                input.setText(Config.CLIENT.tooltipDuration.get().toString());
                input.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                input.setTextSize(14);
                input.setFilters(DigitsInputFilter.getInstance(input.getTextLocale()), new InputFilter.LengthFilter(3));
                input.setPadding(dp(4), 0, dp(4), 0);
                input.setOnFocusChangeListener((__, hasFocus) -> {
                    if (!hasFocus) {
                        int radius = Integer.parseInt(input.getText().toString());
                        radius = FMath.clamp(radius, Client.ANIM_DURATION_MIN, Client.ANIM_DURATION_MAX);
                        input.setText(Integer.toString(radius));
                        if (radius != Config.CLIENT.tooltipDuration.get()) {
                            Config.CLIENT.tooltipDuration.set(radius);
                            Config.CLIENT.saveAndReload();
                        }
                    }
                });
                StateListDrawable drawable = new StateListDrawable();
                drawable.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), new InputBackground());
                drawable.setEnterFadeDuration(300);
                drawable.setExitFadeDuration(300);
                input.setBackground(drawable);

                params = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_BASELINE, 35);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp6, dp6, dp6, dp6);
                extension.addView(input, params);
            }

            {
                var view = new TextView();
                view.setId(36);
                view.setText(I18n.get("gui.modernui.center.extension.textEngine"));
                view.setTextSize(14);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, 35);
                params.addRule(RelativeLayout.ALIGN_START, 35);
                extension.addView(view, params);

                var button = new SwitchButton();
                button.setCheckedColor(THEME_COLOR);
                button.setChecked((ModernUIForge.getBootstrapLevel() & ModernUIForge.BOOTSTRAP_TEXT_ENGINE) == 0);
                button.setOnCheckedChangeListener((__, checked) -> {
                    int level = ModernUIForge.getBootstrapLevel();
                    if (checked) {
                        level &= ~ModernUIForge.BOOTSTRAP_TEXT_ENGINE;
                    } else {
                        level |= ModernUIForge.BOOTSTRAP_TEXT_ENGINE;
                    }
                    ModernUIForge.setBootstrapLevel(level);
                    Toast.makeText("Restart the game to take effect", Toast.LENGTH_SHORT)
                            .show();
                });

                params = new RelativeLayout.LayoutParams(dp(36), dp(16));
                params.addRule(RelativeLayout.ALIGN_TOP, 36);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp6, dp(3), dp6, dp(3));
                extension.addView(button, params);
            }

            {
                var view = new TextView();
                view.setId(38);
                view.setText(I18n.get("gui.modernui.center.extension.smoothScrolling"));
                view.setTextSize(14);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, 36);
                params.addRule(RelativeLayout.ALIGN_START, 36);
                extension.addView(view, params);

                var button = new SwitchButton();
                button.setCheckedColor(THEME_COLOR);
                button.setChecked((ModernUIForge.getBootstrapLevel() & ModernUIForge.BOOTSTRAP_SMOOTH_SCROLLING) == 0);
                button.setOnCheckedChangeListener((__, checked) -> {
                    int level = ModernUIForge.getBootstrapLevel();
                    if (checked) {
                        level &= ~ModernUIForge.BOOTSTRAP_SMOOTH_SCROLLING;
                    } else {
                        level |= ModernUIForge.BOOTSTRAP_SMOOTH_SCROLLING;
                    }
                    ModernUIForge.setBootstrapLevel(level);
                    Toast.makeText("Restart the game to take effect", Toast.LENGTH_SHORT)
                            .show();
                });

                params = new RelativeLayout.LayoutParams(dp(36), dp(16));
                params.addRule(RelativeLayout.ALIGN_TOP, 38);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp6, dp(3), dp6, dp(3));
                extension.addView(button, params);
            }

            var params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            params.setMargins(dp(12), dp(12), dp(12), dp(18));
            base.addView(extension, params);
        }

        {
            var font = new RelativeLayout();

            {
                var title = new TextView();
                title.setId(R.id.title);
                title.setText(I18n.get("gui.modernui.center.category.text"));
                title.setTextSize(16);
                title.setTextColor(THEME_COLOR);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                params.addRule(RelativeLayout.ALIGN_PARENT_START);
                params.setMargins(dp6, dp6, dp6, dp6);
                font.addView(title, params);
            }

            {
                var view = new TextView();
                view.setId(40);
                view.setText("Font family");
                view.setTextSize(14);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, R.id.title);
                params.addRule(RelativeLayout.ALIGN_START, R.id.title);
                font.addView(view, params);
            }

            {
                var view = new TextView();
                view.setId(42);
                view.setText("Bitmap-like");
                view.setTextSize(14);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, 40);
                params.addRule(RelativeLayout.ALIGN_START, 40);
                font.addView(view, params);

                var button = new SwitchButton();
                button.setCheckedColor(THEME_COLOR);
                button.setChecked(Config.CLIENT.bitmapLike.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    Config.CLIENT.bitmapLike.set(checked);
                    Config.CLIENT.saveAndReload();
                    Toast.makeText("Restart the game to work properly", Toast.LENGTH_SHORT)
                            .show();
                });

                params = new RelativeLayout.LayoutParams(dp(36), dp(16));
                params.addRule(RelativeLayout.ALIGN_TOP, 42);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp6, dp(3), dp6, dp(3));
                font.addView(button, params);
            }

            {
                var view = new TextView();
                view.setId(44);
                view.setText("Linear sampling");
                view.setTextSize(14);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, 42);
                params.addRule(RelativeLayout.ALIGN_START, 42);
                font.addView(view, params);

                var button = new SwitchButton();
                button.setCheckedColor(THEME_COLOR);
                button.setChecked(Config.CLIENT.linearSampling.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    Config.CLIENT.linearSampling.set(checked);
                    Config.CLIENT.saveAndReload();
                    Toast.makeText("Restart the game to work properly", Toast.LENGTH_SHORT)
                            .show();
                });

                params = new RelativeLayout.LayoutParams(dp(36), dp(16));
                params.addRule(RelativeLayout.ALIGN_TOP, 44);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp6, dp(3), dp6, dp(3));
                font.addView(button, params);
            }

            {
                var view = new TextView();
                view.setId(46);
                view.setText("Allow shadow");
                view.setTextSize(14);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, 44);
                params.addRule(RelativeLayout.ALIGN_START, 44);
                font.addView(view, params);

                var button = new SwitchButton();
                button.setCheckedColor(THEME_COLOR);
                button.setChecked(ModernUITextMC.CONFIG.mAllowShadow.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    ModernUITextMC.CONFIG.mAllowShadow.set(checked);
                    ModernUITextMC.CONFIG.saveAndReload();
                });

                params = new RelativeLayout.LayoutParams(dp(36), dp(16));
                params.addRule(RelativeLayout.ALIGN_TOP, 46);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp6, dp(3), dp6, dp(3));
                font.addView(button, params);
            }

            {
                var view = new TextView();
                view.setId(48);
                view.setText("Fixed resolution");
                view.setTextSize(14);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, 46);
                params.addRule(RelativeLayout.ALIGN_START, 46);
                font.addView(view, params);

                var button = new SwitchButton();
                button.setCheckedColor(THEME_COLOR);
                button.setChecked(ModernUITextMC.CONFIG.mFixedResolution.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    ModernUITextMC.CONFIG.mFixedResolution.set(checked);
                    ModernUITextMC.CONFIG.saveAndReload();
                });

                params = new RelativeLayout.LayoutParams(dp(36), dp(16));
                params.addRule(RelativeLayout.ALIGN_TOP, 48);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp6, dp(3), dp6, dp(3));
                font.addView(button, params);
            }

            var params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            params.setMargins(dp(12), dp(12), dp(12), dp(18));
            base.addView(font, params);
        }

        base.setDividerDrawable(new Divider());
        base.setDividerPadding(dp(8));
        base.setShowDividers(LinearLayout.SHOW_DIVIDER_BEGINNING | LinearLayout.SHOW_DIVIDER_MIDDLE | LinearLayout.SHOW_DIVIDER_END);

        return base;
    }

    @Nonnull
    private View createSecondaryPanel() {
        final int dp6 = dp(6);

        var base = new LinearLayout();
        base.setOrientation(LinearLayout.VERTICAL);

        {
            var system = new RelativeLayout();

            {
                var title = new TextView();
                title.setId(R.id.title);
                title.setText(I18n.get("gui.modernui.center.category.system"));
                title.setTextSize(16);
                title.setTextColor(THEME_COLOR);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                params.addRule(RelativeLayout.ALIGN_PARENT_START);
                params.setMargins(dp6, dp6, dp6, dp6);
                system.addView(title, params);
            }

            {
                var view = new TextView();
                view.setId(16);
                view.setText(I18n.get("gui.modernui.center.system.forceRtlLayout"));
                view.setTextSize(14);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, R.id.title);
                params.addRule(RelativeLayout.ALIGN_START, R.id.title);
                system.addView(view, params);

                var button = new SwitchButton();
                button.setCheckedColor(THEME_COLOR);
                button.setChecked(Config.CLIENT.forceRtl.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    Config.CLIENT.forceRtl.set(checked);
                    Config.CLIENT.saveAndReload();
                });

                params = new RelativeLayout.LayoutParams(dp(36), dp(16));
                params.addRule(RelativeLayout.ALIGN_TOP, 16);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp6, dp(3), dp6, dp(3));
                system.addView(button, params);
            }

            {
                var view = new TextView();
                view.setId(18);
                view.setText(I18n.get("gui.modernui.center.system.globalFontScale"));
                view.setTextSize(14);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, 16);
                params.addRule(RelativeLayout.ALIGN_START, 16);
                system.addView(view, params);

                var input = new EditText();
                input.setText(Config.CLIENT.fontScale.get().toString());
                input.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                input.setTextSize(14);
                input.setFilters(DigitsInputFilter.getInstance(input.getTextLocale(), false, true),
                        new InputFilter.LengthFilter(4));
                input.setPadding(dp(4), 0, dp(4), 0);
                input.setOnFocusChangeListener((__, hasFocus) -> {
                    if (!hasFocus) {
                        double radius = Double.parseDouble(input.getText().toString());
                        radius = Math.max(Math.min(radius, Client.FONT_SCALE_MAX), Client.FONT_SCALE_MIN);
                        input.setText(Double.toString(radius));
                        if (radius != Config.CLIENT.fontScale.get()) {
                            Config.CLIENT.fontScale.set(radius);
                            Config.CLIENT.saveAndReload();
                        }
                    }
                });
                StateListDrawable drawable = new StateListDrawable();
                drawable.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), new InputBackground());
                drawable.setEnterFadeDuration(300);
                drawable.setExitFadeDuration(300);
                input.setBackground(drawable);

                params = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_BASELINE, 18);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp6, dp6, dp6, dp6);
                system.addView(input, params);
            }

            {
                var view = new TextView();
                view.setId(20);
                view.setText(I18n.get("gui.modernui.center.system.globalAnimationScale"));
                view.setTextSize(14);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, 18);
                params.addRule(RelativeLayout.ALIGN_START, 18);
                system.addView(view, params);

                var input = new EditText();
                input.setText(Float.toString(ValueAnimator.sDurationScale));
                input.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                input.setTextSize(14);
                input.setFilters(DigitsInputFilter.getInstance(input.getTextLocale(), false, true),
                        new InputFilter.LengthFilter(4));
                input.setPadding(dp(4), 0, dp(4), 0);
                input.setOnFocusChangeListener((__, hasFocus) -> {
                    if (!hasFocus) {
                        double scale = Double.parseDouble(input.getText().toString());
                        scale = Math.max(Math.min(scale, 10), 0.1);
                        input.setText(Double.toString(scale));
                        if (scale != ValueAnimator.sDurationScale) {
                            ValueAnimator.sDurationScale = (float) scale;
                        }
                    }
                });
                StateListDrawable drawable = new StateListDrawable();
                drawable.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), new InputBackground());
                drawable.setEnterFadeDuration(300);
                drawable.setExitFadeDuration(300);
                input.setBackground(drawable);

                params = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_BASELINE, 20);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp6, dp6, dp6, dp6);
                system.addView(input, params);
            }

            var params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            params.setMargins(dp(12), dp(12), dp(12), dp(18));
            base.addView(system, params);
        }

        {
            var group = new RelativeLayout();

            {
                var title = new TextView();
                title.setId(18);
                title.setText("View");
                title.setTextSize(16);
                title.setTextColor(THEME_COLOR);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                params.addRule(RelativeLayout.ALIGN_PARENT_START);
                params.setMargins(dp6, dp6, dp6, dp6);
                group.addView(title, params);
            }

            addSystemSetting(20, "Scrollbar size", group, 1024, Config.CLIENT.scrollbarSize);
            addSystemSetting(22, "Touch slop", group, 1024, Config.CLIENT.touchSlop);
            addSystemSetting(24, "Min scrollbar touch target", group, 1024, Config.CLIENT.minScrollbarTouchTarget);
            addSystemSetting(26, "Minimum fling velocity", group, 32767, Config.CLIENT.minimumFlingVelocity);
            addSystemSetting(28, "Maximum fling velocity", group, 32767, Config.CLIENT.maximumFlingVelocity);
            addSystemSetting(30, "Overscroll distance", group, 1024, Config.CLIENT.overscrollDistance);
            addSystemSetting(32, "Overfling distance", group, 1024, Config.CLIENT.overflingDistance);

            {
                var view = new TextView();
                view.setId(34);
                view.setText("Vertical scroll factor");
                view.setTextSize(14);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, 32);
                params.addRule(RelativeLayout.ALIGN_START, 32);
                group.addView(view, params);

                var input = new EditText();
                input.setText(Config.CLIENT.verticalScrollFactor.get().toString());
                input.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                input.setTextSize(14);
                input.setFilters(DigitsInputFilter.getInstance(input.getTextLocale(), false, true),
                        new InputFilter.LengthFilter(6));
                input.setPadding(dp(4), 0, dp(4), 0);
                input.setOnFocusChangeListener((__, hasFocus) -> {
                    if (!hasFocus) {
                        double radius = Double.parseDouble(input.getText().toString());
                        radius = Math.max(Math.min(radius, 1024), 0);
                        input.setText(Double.toString(radius));
                        if (radius != Config.CLIENT.verticalScrollFactor.get()) {
                            Config.CLIENT.verticalScrollFactor.set(radius);
                            Config.CLIENT.saveAndReload();
                        }
                    }
                });
                StateListDrawable drawable = new StateListDrawable();
                drawable.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), new InputBackground());
                drawable.setEnterFadeDuration(300);
                drawable.setExitFadeDuration(300);
                input.setBackground(drawable);

                params = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_BASELINE, 34);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp6, dp6, dp6, dp6);
                group.addView(input, params);
            }

            {
                var view = new TextView();
                view.setId(36);
                view.setText("Horizontal scroll factor");
                view.setTextSize(14);

                var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, 34);
                params.addRule(RelativeLayout.ALIGN_START, 34);
                group.addView(view, params);

                var input = new EditText();
                input.setText(Config.CLIENT.horizontalScrollFactor.get().toString());
                input.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                input.setTextSize(14);
                input.setFilters(DigitsInputFilter.getInstance(input.getTextLocale(), false, true),
                        new InputFilter.LengthFilter(6));
                input.setPadding(dp(4), 0, dp(4), 0);
                input.setOnFocusChangeListener((__, hasFocus) -> {
                    if (!hasFocus) {
                        double radius = Double.parseDouble(input.getText().toString());
                        radius = Math.max(Math.min(radius, 1024), 0);
                        input.setText(Double.toString(radius));
                        if (radius != Config.CLIENT.horizontalScrollFactor.get()) {
                            Config.CLIENT.horizontalScrollFactor.set(radius);
                            Config.CLIENT.saveAndReload();
                        }
                    }
                });
                StateListDrawable drawable = new StateListDrawable();
                drawable.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), new InputBackground());
                drawable.setEnterFadeDuration(300);
                drawable.setExitFadeDuration(300);
                input.setBackground(drawable);

                params = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_BASELINE, 36);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp6, dp6, dp6, dp6);
                group.addView(input, params);
            }

            var params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            params.setMargins(dp(12), dp(12), dp(12), dp(18));
            base.addView(group, params);
        }

        base.setDividerDrawable(new Divider());
        base.setDividerPadding(dp(8));
        base.setShowDividers(LinearLayout.SHOW_DIVIDER_BEGINNING | LinearLayout.SHOW_DIVIDER_MIDDLE | LinearLayout.SHOW_DIVIDER_END);

        return base;
    }

    private void addSystemSetting(int id, String title, @Nonnull ViewGroup container, int max,
                                  @Nonnull IntValue config) {
        var view = new TextView();
        view.setId(id);
        view.setText(title);
        view.setTextSize(14);

        var params = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, id - 2);
        params.addRule(RelativeLayout.ALIGN_START, id - 2);
        container.addView(view, params);

        var input = new EditText();
        input.setText(config.get().toString());
        input.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        input.setTextSize(14);
        input.setFilters(DigitsInputFilter.getInstance(input.getTextLocale()), new InputFilter.LengthFilter(5));
        input.setPadding(dp(4), 0, dp(4), 0);
        input.setOnFocusChangeListener((__, hasFocus) -> {
            if (!hasFocus) {
                int val = Integer.parseInt(input.getText().toString());
                val = FMath.clamp(val, 0, max);
                input.setText(Integer.toString(val));
                if (val != config.get()) {
                    config.set(val);
                    Config.CLIENT.saveAndReload();
                }
            }
        });
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), new InputBackground());
        drawable.setEnterFadeDuration(300);
        drawable.setExitFadeDuration(300);
        input.setBackground(drawable);

        params = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_BASELINE, id);
        params.addRule(RelativeLayout.ALIGN_PARENT_END);
        params.setMargins(dp(6), dp(6), dp(6), dp(6));
        container.addView(input, params);
    }

    @Nullable
    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        if (enter && transit == FragmentTransaction.TRANSIT_FRAGMENT_OPEN) {
            Keyframe kfStart = Keyframe.ofFloat(0, 0.75f);
            Keyframe kfEnd = Keyframe.ofFloat(1, 1);
            kfEnd.setInterpolator(TimeInterpolator.OVERSHOOT);
            PropertyValuesHolder scaleX = PropertyValuesHolder.ofKeyframe(View.SCALE_X,
                    kfStart, kfEnd);
            PropertyValuesHolder scaleY = PropertyValuesHolder.ofKeyframe(View.SCALE_Y,
                    kfStart.copy(), kfEnd.copy());
            kfStart = Keyframe.ofFloat(0, 0);
            kfEnd = Keyframe.ofFloat(1, 1);
            kfEnd.setInterpolator(TimeInterpolator.DECELERATE_CUBIC);
            PropertyValuesHolder alpha = PropertyValuesHolder.ofKeyframe(View.ALPHA, kfStart, kfEnd);
            Animator animator = ObjectAnimator.ofPropertyValuesHolder(null, scaleX, scaleY, alpha);
            animator.setDuration(400);
            // we use keyframe-specified interpolators
            animator.setInterpolator(null);
            return animator;
        }
        return super.onCreateAnimator(transit, enter, nextAnim);
    }

    private static class Background extends Drawable {

        private final float mRadius, mStrokeWidth;

        private Background() {
            mRadius = dp(16);
            mStrokeWidth = dp(4);
        }

        @Override
        public void draw(@Nonnull Canvas canvas) {
            Paint paint = Paint.take();
            paint.setColor(BACKGROUND_COLOR);
            Rect bounds = getBounds();
            float inner = mStrokeWidth * 0.5f;
            canvas.drawRoundRect(bounds.left + inner, bounds.top + inner, bounds.right - inner,
                    bounds.bottom - inner, mRadius, paint);
            paint.setStyle(Paint.STROKE);
            paint.setColors(THEME_COLOR, THEME_COLOR_2, THEME_COLOR, THEME_COLOR_2);
            paint.setStrokeWidth(mStrokeWidth);
            canvas.drawRoundRect(bounds.left + inner, bounds.top + inner, bounds.right - inner,
                    bounds.bottom - inner, mRadius, paint);
            paint.drop();
        }

        @Override
        public boolean getPadding(@Nonnull Rect padding) {
            int inner = (int) Math.ceil(mStrokeWidth * 0.5f);
            padding.set(inner, inner, inner, inner);
            return true;
        }
    }

    private static class Divider extends Drawable {

        private final int mSize;

        public Divider() {
            mSize = dp(2);
        }

        @Override
        public void draw(@Nonnull Canvas canvas) {
            Paint paint = Paint.get();
            paint.setColor(0xc0606060);
            canvas.drawRect(getBounds(), paint);
        }

        @Override
        public int getIntrinsicHeight() {
            return mSize;
        }
    }

    private static class InputBackground extends Drawable {

        private int mAlpha = 255;

        @Override
        public void draw(@Nonnull Canvas canvas) {
            Paint paint = Paint.get();
            paint.setColor(0x80a0a0a0);
            paint.setAlpha(MaterialDrawable.modulateAlpha(paint.getAlpha(), mAlpha));
            if (paint.getAlpha() != 0) {
                canvas.drawRect(getBounds(), paint);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            if (mAlpha != alpha) {
                mAlpha = alpha;
                invalidateSelf();
            }
        }

        @Override
        public int getAlpha() {
            return mAlpha;
        }
    }
}
