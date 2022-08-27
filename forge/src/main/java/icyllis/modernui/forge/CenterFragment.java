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
import icyllis.modernui.core.Core;
import icyllis.modernui.forge.Config.Client;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.fragment.FragmentTransaction;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.drawable.StateListDrawable;
import icyllis.modernui.graphics.font.GlyphManager;
import icyllis.modernui.material.MaterialDrawable;
import icyllis.modernui.math.FMath;
import icyllis.modernui.math.Rect;
import icyllis.modernui.text.InputFilter;
import icyllis.modernui.text.TextPaint;
import icyllis.modernui.text.method.DigitsInputFilter;
import icyllis.modernui.textmc.ModernUITextMC;
import icyllis.modernui.textmc.TextLayoutEngine;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.util.StateSet;
import icyllis.modernui.view.*;
import icyllis.modernui.view.View.OnLayoutChangeListener;
import icyllis.modernui.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

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

        var base = new LinearLayout();
        base.setId(R.id.content);
        base.setOrientation(LinearLayout.VERTICAL);
        base.setBackground(new Background());
        {
            var title = new TextView();
            title.setId(R.id.title);
            title.setText(I18n.get("modernui.center.title"));
            title.setTextSize(22);
            title.setTextStyle(TextPaint.BOLD);

            var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            params.setMargins(0, dp(12), 0, dp(12));
            base.addView(title, params);
        }

        {
            var content = new LinearLayout();
            content.setOrientation(LinearLayout.HORIZONTAL);

            boolean firstDown = Math.random() < 0.5;

            {
                ScrollView left = new ScrollView();
                left.addView(createLeftPanel(), MATCH_PARENT, WRAP_CONTENT);
                var params = new LinearLayout.LayoutParams(0, MATCH_PARENT, 1);
                params.setMargins(dp6, dp6, dp6, dp6);
                content.addView(left, params);

                ObjectAnimator animator = ObjectAnimator.ofFloat(left, View.TRANSLATION_Y,
                        dp(firstDown ? -200 : 200), 0);
                animator.setInterpolator(TimeInterpolator.DECELERATE_CUBIC);
                left.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                                               int oldTop, int oldRight, int oldBottom) {
                        animator.start();
                        v.removeOnLayoutChangeListener(this);
                    }
                });
                left.setEdgeEffectColor(THEME_COLOR);
            }

            {
                ScrollView right = new ScrollView();
                right.addView(createRightPanel(), MATCH_PARENT, WRAP_CONTENT);
                var params = new LinearLayout.LayoutParams(0, MATCH_PARENT, 1);
                params.setMargins(dp6, dp6, dp6, dp6);
                content.addView(right, params);

                ObjectAnimator animator = ObjectAnimator.ofFloat(right, View.TRANSLATION_Y,
                        dp(firstDown ? 200 : -200), 0);
                animator.setInterpolator(TimeInterpolator.DECELERATE_CUBIC);
                right.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                                               int oldTop, int oldRight, int oldBottom) {
                        animator.start();
                        v.removeOnLayoutChangeListener(this);
                    }
                });
                right.setEdgeEffectColor(THEME_COLOR);
            }

            var params = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
            base.addView(content, params);
        }

        var params = new FrameLayout.LayoutParams(dp(720), dp(450));
        params.gravity = Gravity.CENTER;
        base.setLayoutParams(params);
        return base;
    }

    @Nonnull
    private LinearLayout createCategory(String titleKey) {
        var layout = new LinearLayout();
        layout.setOrientation(LinearLayout.VERTICAL);

        final int dp6 = dp(6);
        final int dp12 = dp(12);
        {
            var title = new TextView();
            title.setId(R.id.title);
            title.setText(I18n.get(titleKey));
            title.setTextSize(16);
            title.setTextColor(THEME_COLOR);

            var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            params.gravity = Gravity.START;
            params.setMargins(dp6, dp6, dp6, dp6);
            layout.addView(title, params);
        }

        var params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        params.setMargins(dp12, dp12, dp12, dp(18));
        layout.setLayoutParams(params);

        return layout;
    }

    @Nonnull
    private LinearLayout createInputOption(String titleKey) {
        var layout = new LinearLayout();
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setHorizontalGravity(Gravity.START);

        final int dp3 = dp(3);
        final int dp6 = dp(6);
        {
            var title = new TextView();
            title.setText(I18n.get(titleKey));
            title.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            title.setTextSize(14);

            var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 1);
            params.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
            layout.addView(title, params);
        }
        {
            var input = new EditText();
            input.setId(R.id.input);
            input.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            input.setTextSize(14);
            input.setPadding(dp3, 0, dp3, 0);

            StateListDrawable background = new StateListDrawable();
            background.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), new InputBackground());
            background.setEnterFadeDuration(300);
            background.setExitFadeDuration(300);
            input.setBackground(background);

            var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            params.gravity = Gravity.CENTER_VERTICAL;
            layout.addView(input, params);
        }

        var params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        params.setMargins(dp6, 0, dp6, 0);
        layout.setLayoutParams(params);

        return layout;
    }

    @Nonnull
    private LinearLayout createButtonOption(String titleKey) {
        var layout = new LinearLayout();
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setHorizontalGravity(Gravity.START);

        final int dp3 = dp(3);
        final int dp6 = dp(6);
        {
            var title = new TextView();
            title.setText(I18n.get(titleKey));
            title.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            title.setTextSize(14);

            var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 1);
            params.gravity = Gravity.CENTER_VERTICAL;
            layout.addView(title, params);
        }
        {
            var button = new SwitchButton();
            button.setId(R.id.button1);
            button.setCheckedColor(THEME_COLOR);

            var params = new LinearLayout.LayoutParams(dp(36), dp(16));
            params.gravity = Gravity.CENTER_VERTICAL;
            params.setMargins(0, dp3, 0, dp3);
            layout.addView(button, params);
        }

        var params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        params.setMargins(dp6, 0, dp6, 0);
        layout.setLayoutParams(params);

        return layout;
    }

    // Minecraft
    @Nonnull
    private View createLeftPanel() {
        var panel = new LinearLayout();
        panel.setOrientation(LinearLayout.VERTICAL);

        {
            // Screen
            var category = createCategory("modernui.center.category.screen");
            {
                var option = createInputOption("modernui.center.screen.backgroundDuration");
                var input = option.<EditText>requireViewById(R.id.input);
                input.setText(Config.CLIENT.backgroundDuration.get().toString());
                input.setFilters(DigitsInputFilter.getInstance(input.getTextLocale()), new InputFilter.LengthFilter(3));
                input.setOnFocusChangeListener((view, hasFocus) -> {
                    if (!hasFocus) {
                        EditText v = (EditText) view;
                        int value = FMath.clamp(Integer.parseInt(v.getText().toString()),
                                Client.ANIM_DURATION_MIN, Client.ANIM_DURATION_MAX);
                        v.setText(Integer.toString(value));
                        if (value != Config.CLIENT.backgroundDuration.get()) {
                            Config.CLIENT.backgroundDuration.set(value);
                            Config.CLIENT.saveAndReload();
                        }
                    }
                });
                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.screen.blurEffect");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked(Config.CLIENT.blurEffect.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    Config.CLIENT.blurEffect.set(checked);
                    Config.CLIENT.saveAndReload();
                });
                category.addView(option);
            }
            {
                var option = createInputOption("modernui.center.screen.blurRadius");
                var input = option.<EditText>requireViewById(R.id.input);
                input.setText(Config.CLIENT.blurRadius.get().toString());
                input.setFilters(DigitsInputFilter.getInstance(input.getTextLocale()), new InputFilter.LengthFilter(2));
                input.setOnFocusChangeListener((view, hasFocus) -> {
                    if (!hasFocus) {
                        EditText v = (EditText) view;
                        int value = FMath.clamp(Integer.parseInt(v.getText().toString()),
                                Client.BLUR_RADIUS_MIN, Client.BLUR_RADIUS_MAX);
                        v.setText(Integer.toString(value));
                        if (value != Config.CLIENT.blurRadius.get()) {
                            Config.CLIENT.blurRadius.set(value);
                            Config.CLIENT.saveAndReload();
                        }
                    }
                });
                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.screen.inventoryPause");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked(Config.CLIENT.inventoryPause.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    Config.CLIENT.inventoryPause.set(checked);
                    Config.CLIENT.saveAndReload();
                });
                category.addView(option);
            }
            {
                var option = new LinearLayout();
                option.setOrientation(LinearLayout.HORIZONTAL);
                option.setHorizontalGravity(Gravity.START);

                final int dp6 = dp(6);
                {
                    var title = new TextView();
                    title.setText(I18n.get("modernui.center.screen.windowMode"));
                    title.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                    title.setTextSize(14);

                    var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 1);
                    params.gravity = Gravity.CENTER_VERTICAL;
                    option.addView(title, params);
                }
                {
                    var spinner = new Spinner();
                    spinner.setGravity(Gravity.END);
                    spinner.setAdapter(new ArrayAdapter<>(Client.WindowMode.values()));
                    spinner.setSelection(Config.CLIENT.windowMode.get().ordinal());
                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            Client.WindowMode windowMode = Client.WindowMode.values()[position];
                            if (Config.CLIENT.windowMode.get() != windowMode) {
                                Config.CLIENT.windowMode.set(windowMode);
                                Config.CLIENT.saveAndReload();
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });

                    var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                    params.gravity = Gravity.CENTER_VERTICAL;
                    option.addView(spinner, params);
                }

                var params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                params.gravity = Gravity.CENTER;
                params.setMargins(dp6, 0, dp6, 0);
                option.setLayoutParams(params);

                category.addView(option);
            }
            panel.addView(category);
        }

        {
            var category = createCategory("modernui.center.category.extension");
            {
                var option = createButtonOption("modernui.center.extension.ding");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked(Config.CLIENT.ding.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    Config.CLIENT.ding.set(checked);
                    Config.CLIENT.saveAndReload();
                });
                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.extension.tooltip");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked(Config.CLIENT.tooltip.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    Config.CLIENT.tooltip.set(checked);
                    Config.CLIENT.saveAndReload();
                });
                category.addView(option);
            }
            {
                var option = createInputOption("modernui.center.extension.tooltipDuration");
                var input = option.<EditText>requireViewById(R.id.input);
                input.setText(Config.CLIENT.tooltipDuration.get().toString());
                input.setFilters(DigitsInputFilter.getInstance(input.getTextLocale()), new InputFilter.LengthFilter(3));
                input.setOnFocusChangeListener((view, hasFocus) -> {
                    if (!hasFocus) {
                        EditText v = (EditText) view;
                        int value = FMath.clamp(Integer.parseInt(v.getText().toString()),
                                Client.ANIM_DURATION_MIN, Client.ANIM_DURATION_MAX);
                        v.setText(Integer.toString(value));
                        if (value != Config.CLIENT.tooltipDuration.get()) {
                            Config.CLIENT.tooltipDuration.set(value);
                            Config.CLIENT.saveAndReload();
                        }
                    }
                });
                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.extension.smoothScrolling");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked((ModernUIForge.getBootstrapLevel() & ModernUIForge.BOOTSTRAP_DISABLE_SMOOTH_SCROLLING) == 0);
                button.setOnCheckedChangeListener((__, checked) -> {
                    int level = ModernUIForge.getBootstrapLevel();
                    if (checked) {
                        level &= ~ModernUIForge.BOOTSTRAP_DISABLE_SMOOTH_SCROLLING;
                    } else {
                        level |= ModernUIForge.BOOTSTRAP_DISABLE_SMOOTH_SCROLLING;
                    }
                    ModernUIForge.setBootstrapLevel(level);
                    Toast.makeText(I18n.get("gui.modernui.restart_to_work"), Toast.LENGTH_SHORT)
                            .show();
                });
                category.addView(option);
            }
            panel.addView(category);
        }

        {
            // Text Engine
            var category = createCategory("modernui.center.category.text");
            {
                var option = createButtonOption("modernui.center.text.textEngine");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked((ModernUIForge.getBootstrapLevel() & ModernUIForge.BOOTSTRAP_DISABLE_TEXT_ENGINE) == 0);
                button.setOnCheckedChangeListener((__, checked) -> {
                    int level = ModernUIForge.getBootstrapLevel();
                    if (checked) {
                        level &= ~ModernUIForge.BOOTSTRAP_DISABLE_TEXT_ENGINE;
                    } else {
                        level |= ModernUIForge.BOOTSTRAP_DISABLE_TEXT_ENGINE;
                    }
                    ModernUIForge.setBootstrapLevel(level);
                    Toast.makeText(I18n.get("gui.modernui.restart_to_work"), Toast.LENGTH_SHORT)
                            .show();
                });
                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.text.colorEmoji");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked(ModernUITextMC.CONFIG.mColorEmoji.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    ModernUITextMC.CONFIG.mColorEmoji.set(checked);
                    ModernUITextMC.CONFIG.saveAndReload();
                });
                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.text.bitmapRepl");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked(ModernUITextMC.CONFIG.mBitmapReplacement.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    ModernUITextMC.CONFIG.mBitmapReplacement.set(checked);
                    ModernUITextMC.CONFIG.saveOnly();
                    Toast.makeText(I18n.get("gui.modernui.restart_to_work"), Toast.LENGTH_SHORT)
                            .show();
                });
                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.text.emojiShortcodes");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked(ModernUITextMC.CONFIG.mEmojiShortcodes.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    ModernUITextMC.CONFIG.mEmojiShortcodes.set(checked);
                    ModernUITextMC.CONFIG.saveAndReload();
                });
                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.text.allowShadow");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked(ModernUITextMC.CONFIG.mAllowShadow.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    ModernUITextMC.CONFIG.mAllowShadow.set(checked);
                    ModernUITextMC.CONFIG.saveAndReload();
                });
                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.text.fixedResolution");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked(ModernUITextMC.CONFIG.mFixedResolution.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    ModernUITextMC.CONFIG.mFixedResolution.set(checked);
                    ModernUITextMC.CONFIG.saveAndReload();
                });
                category.addView(option);
            }
            {
                var option = createInputOption("modernui.center.text.baseFontSize");
                var input = option.<EditText>requireViewById(R.id.input);
                input.setText(ModernUITextMC.CONFIG.mBaseFontSize.get().toString());
                input.setFilters(DigitsInputFilter.getInstance(input.getTextLocale(), false, true),
                        new InputFilter.LengthFilter(5));
                input.setOnFocusChangeListener((view, hasFocus) -> {
                    if (!hasFocus) {
                        EditText v = (EditText) view;
                        float value = FMath.clamp(Float.parseFloat(v.getText().toString()),
                                ModernUITextMC.Config.BASE_FONT_SIZE_MIN, ModernUITextMC.Config.BASE_FONT_SIZE_MAX);
                        v.setText(Float.toString(value));
                        if (value != ModernUITextMC.CONFIG.mBaseFontSize.get()) {
                            ModernUITextMC.CONFIG.mBaseFontSize.set((double) value);
                            ModernUITextMC.CONFIG.saveAndReload();
                        }
                    }
                });
                category.addView(option);
            }
            {
                var option = createInputOption("modernui.center.text.baselineShift");
                var input = option.<EditText>requireViewById(R.id.input);
                input.setText(ModernUITextMC.CONFIG.mBaselineShift.get().toString());
                input.setFilters(DigitsInputFilter.getInstance(input.getTextLocale(), false, true),
                        new InputFilter.LengthFilter(5));
                input.setOnFocusChangeListener((view, hasFocus) -> {
                    if (!hasFocus) {
                        EditText v = (EditText) view;
                        float value = FMath.clamp(Float.parseFloat(v.getText().toString()),
                                ModernUITextMC.Config.BASELINE_MIN, ModernUITextMC.Config.BASELINE_MAX);
                        v.setText(Float.toString(value));
                        if (value != ModernUITextMC.CONFIG.mBaselineShift.get()) {
                            ModernUITextMC.CONFIG.mBaselineShift.set((double) value);
                            ModernUITextMC.CONFIG.saveAndReload();
                        }
                    }
                });
                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.text.superSampling");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked(ModernUITextMC.CONFIG.mSuperSampling.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    ModernUITextMC.CONFIG.mSuperSampling.set(checked);
                    ModernUITextMC.CONFIG.saveAndReload();
                });
                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.text.alignPixels");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked(ModernUITextMC.CONFIG.mAlignPixels.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    ModernUITextMC.CONFIG.mAlignPixels.set(checked);
                    ModernUITextMC.CONFIG.saveAndReload();
                });
                category.addView(option);
            }
            {
                var option = new LinearLayout();
                option.setOrientation(LinearLayout.HORIZONTAL);
                option.setHorizontalGravity(Gravity.START);

                final int dp6 = dp(6);
                {
                    var title = new TextView();
                    title.setText(I18n.get("modernui.center.text.bidiHeuristicAlgo"));
                    title.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                    title.setTextSize(14);

                    var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 1);
                    params.gravity = Gravity.CENTER_VERTICAL;
                    option.addView(title, params);
                }
                {
                    var spinner = new Spinner();
                    spinner.setGravity(Gravity.END);
                    spinner.setAdapter(new ArrayAdapter<>(TEXT_DIRS));
                    spinner.setSelection(TextLayoutEngine.sTextDirection - 1);
                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            int value = position + 1;
                            if (ModernUITextMC.CONFIG.mTextDirection.get() != value) {
                                ModernUITextMC.CONFIG.mTextDirection.set(value);
                                ModernUITextMC.CONFIG.saveAndReload();
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });

                    var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                    params.gravity = Gravity.CENTER_VERTICAL;
                    option.addView(spinner, params);
                }

                var params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                params.gravity = Gravity.CENTER;
                params.setMargins(dp6, 0, dp6, 0);
                option.setLayoutParams(params);

                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.text.textShaping");
                option.<SwitchButton>requireViewById(R.id.button1).setChecked(true);
                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.text.fixSurrogate");
                option.<SwitchButton>requireViewById(R.id.button1).setChecked(true);
                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.text.fastDigitRepl");
                option.<SwitchButton>requireViewById(R.id.button1).setChecked(true);
                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.text.fastStreamingAlgo");
                option.<SwitchButton>requireViewById(R.id.button1).setChecked(true);
                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.text.graphemeAlgo");
                option.<SwitchButton>requireViewById(R.id.button1).setChecked(true);
                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.text.lineBreakingAlgo");
                option.<SwitchButton>requireViewById(R.id.button1).setChecked(true);
                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.text.substringAlgo");
                option.<SwitchButton>requireViewById(R.id.button1).setChecked(true);
                category.addView(option);
            }
            {
                var option = createInputOption("modernui.center.text.cacheLifespan");
                var input = option.<EditText>requireViewById(R.id.input);
                input.setText(ModernUITextMC.CONFIG.mCacheLifespan.get().toString());
                input.setFilters(DigitsInputFilter.getInstance(input.getTextLocale()), new InputFilter.LengthFilter(2));
                input.setOnFocusChangeListener((view, hasFocus) -> {
                    if (!hasFocus) {
                        EditText v = (EditText) view;
                        int value = FMath.clamp(Integer.parseInt(v.getText().toString()),
                                ModernUITextMC.Config.LIFESPAN_MIN, ModernUITextMC.Config.LIFESPAN_MAX);
                        v.setText(Integer.toString(value));
                        if (value != ModernUITextMC.CONFIG.mCacheLifespan.get()) {
                            ModernUITextMC.CONFIG.mCacheLifespan.set(value);
                            ModernUITextMC.CONFIG.saveAndReload();
                        }
                    }
                });
                category.addView(option);
            }
            {
                var option = createInputOption("modernui.center.text.rehashThreshold");
                var input = option.<EditText>requireViewById(R.id.input);
                input.setText(ModernUITextMC.CONFIG.mRehashThreshold.get().toString());
                input.setFilters(DigitsInputFilter.getInstance(input.getTextLocale()), new InputFilter.LengthFilter(4));
                input.setOnFocusChangeListener((view, hasFocus) -> {
                    if (!hasFocus) {
                        EditText v = (EditText) view;
                        int value = FMath.clamp(Integer.parseInt(v.getText().toString()),
                                ModernUITextMC.Config.REHASH_MIN, ModernUITextMC.Config.REHASH_MAX);
                        v.setText(Integer.toString(value));
                        if (value != ModernUITextMC.CONFIG.mRehashThreshold.get()) {
                            ModernUITextMC.CONFIG.mRehashThreshold.set(value);
                            ModernUITextMC.CONFIG.saveAndReload();
                        }
                    }
                });
                category.addView(option);
            }
            panel.addView(category);
        }

        panel.setDividerDrawable(new Divider());
        panel.setDividerPadding(dp(8));
        panel.setShowDividers(LinearLayout.SHOW_DIVIDER_BEGINNING | LinearLayout.SHOW_DIVIDER_MIDDLE | LinearLayout.SHOW_DIVIDER_END);

        return panel;
    }

    @Nonnull
    private View createRightPanel() {
        final int dp6 = dp(6);

        var panel = new LinearLayout();
        panel.setOrientation(LinearLayout.VERTICAL);

        {
            var category = createCategory("modernui.center.category.system");
            {
                var option = createButtonOption("modernui.center.system.forceRtlLayout");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked(Config.CLIENT.forceRtl.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    Config.CLIENT.forceRtl.set(checked);
                    Config.CLIENT.saveAndReload();
                });
                category.addView(option);
            }
            {
                var option = createInputOption("modernui.center.system.globalFontScale");
                var input = option.<EditText>requireViewById(R.id.input);
                input.setText(Config.CLIENT.fontScale.get().toString());
                input.setFilters(DigitsInputFilter.getInstance(input.getTextLocale(), false, true),
                        new InputFilter.LengthFilter(4));
                input.setOnFocusChangeListener((view, hasFocus) -> {
                    if (!hasFocus) {
                        EditText v = (EditText) view;
                        double value = Math.max(Math.min(Double.parseDouble(v.getText().toString()),
                                Client.FONT_SCALE_MAX), Client.FONT_SCALE_MIN);
                        v.setText(Double.toString(value));
                        if (value != Config.CLIENT.fontScale.get()) {
                            Config.CLIENT.fontScale.set(value);
                            Config.CLIENT.saveAndReload();
                        }
                    }
                });
                category.addView(option);
            }
            {
                var option = createInputOption("modernui.center.system.globalAnimationScale");
                var input = option.<EditText>requireViewById(R.id.input);
                input.setText(Float.toString(ValueAnimator.sDurationScale));
                input.setFilters(DigitsInputFilter.getInstance(input.getTextLocale(), false, true),
                        new InputFilter.LengthFilter(4));
                input.setOnFocusChangeListener((view, hasFocus) -> {
                    if (!hasFocus) {
                        EditText v = (EditText) view;
                        double scale = Math.max(Math.min(Double.parseDouble(v.getText().toString()), 10), 0.1);
                        v.setText(Double.toString(scale));
                        if (scale != ValueAnimator.sDurationScale) {
                            ValueAnimator.sDurationScale = (float) scale;
                        }
                    }
                });
                category.addView(option);
            }
            panel.addView(category);
        }

        {
            var category = createCategory("modernui.center.category.font");
            {
                var option = new LinearLayout();
                option.setOrientation(LinearLayout.HORIZONTAL);
                option.setHorizontalGravity(Gravity.START);

                final int dp3 = dp(3);
                {
                    var title = new TextView();
                    title.setText(I18n.get("modernui.center.font.fontFamily"));
                    title.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                    title.setTextSize(14);
                    title.setMinWidth(dp(60));

                    var params = new LinearLayout.LayoutParams(0, WRAP_CONTENT, 2);
                    params.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
                    option.addView(title, params);
                }
                {
                    var input = new EditText();
                    input.setId(R.id.input);
                    input.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                    input.setTextSize(14);
                    input.setPadding(dp3, 0, dp3, 0);

                    input.setText(String.join("\n", Config.CLIENT.fontFamily.get()));
                    input.setOnFocusChangeListener((view, hasFocus) -> {
                        if (!hasFocus) {
                            EditText v = (EditText) view;
                            ArrayList<String> list = new ArrayList<>();
                            for (String s : v.getText().toString().split("\n")) {
                                if (!s.isBlank()) {
                                    list.add(s);
                                }
                            }
                            v.setText(String.join("\n", list));
                            if (!Config.CLIENT.fontFamily.get().equals(list)) {
                                Config.CLIENT.fontFamily.set(list);
                                Config.CLIENT.saveOnly();
                                Toast.makeText(I18n.get("gui.modernui.restart_to_work"), Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                    });

                    StateListDrawable background = new StateListDrawable();
                    background.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), new InputBackground());
                    background.setEnterFadeDuration(300);
                    background.setExitFadeDuration(300);
                    input.setBackground(background);

                    var params = new LinearLayout.LayoutParams(0, WRAP_CONTENT, 5);
                    params.gravity = Gravity.CENTER_VERTICAL;
                    option.addView(input, params);
                }

                var params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                params.gravity = Gravity.CENTER;
                params.setMargins(dp6, 0, dp6, 0);
                option.setLayoutParams(params);

                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.font.antiAliasing");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked(Config.CLIENT.antiAliasing.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    Config.CLIENT.antiAliasing.set(checked);
                    Config.CLIENT.saveAndReload();
                });
                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.font.fractionalMetrics");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked(Config.CLIENT.fractionalMetrics.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    Config.CLIENT.fractionalMetrics.set(checked);
                    Config.CLIENT.saveAndReload();
                });
                category.addView(option);
            }
            {
                var option = createButtonOption("modernui.center.font.linearSampling");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked(Config.CLIENT.linearSampling.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    Config.CLIENT.linearSampling.set(checked);
                    Config.CLIENT.saveAndReload();
                });
                category.addView(option);
            }
            panel.addView(category);
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
            panel.addView(group, params);
        }

        if (ModernUIForge.isDeveloperMode()) {
            var category = createCategory("Debug");
            {
                var option = createInputOption("Gamma");
                var input = option.<EditText>requireViewById(R.id.input);
                input.setText(Minecraft.getInstance().options.gamma().get().toString());
                input.setFilters(DigitsInputFilter.getInstance(input.getTextLocale(), false, true),
                        new InputFilter.LengthFilter(6));
                input.setOnFocusChangeListener((view, hasFocus) -> {
                    if (!hasFocus) {
                        EditText v = (EditText) view;
                        double gamma = Double.parseDouble(v.getText().toString());
                        v.setText(Double.toString(gamma));
                        // no sync, but safe
                        Minecraft.getInstance().options.gamma().set(gamma);
                    }
                });
                category.addView(option);
            }
            {
                var option = createButtonOption("Take Screenshot (Y)");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked(false);
                button.setOnCheckedChangeListener((__, checked) ->
                        Core.postOnRenderThread(() -> UIManager.getInstance().takeScreenshot()));
                category.addView(option);
            }
            {
                var option = createButtonOption("Debug Dump (P)");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked(false);
                button.setOnCheckedChangeListener((__, checked) ->
                        Core.postOnMainThread(() -> UIManager.getInstance().dump()));
                category.addView(option);
            }
            {
                var option = createButtonOption("Debug Glyph Manager (G)");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked(false);
                button.setOnCheckedChangeListener((__, checked) ->
                        Core.postOnMainThread(() -> GlyphManager.getInstance().debug()));
                category.addView(option);
            }
            {
                var option = createButtonOption("GC (F)");
                var button = option.<SwitchButton>requireViewById(R.id.button1);
                button.setChecked(false);
                button.setOnCheckedChangeListener((__, checked) -> System.gc());
                category.addView(option);
            }
            panel.addView(category);
        }

        panel.setDividerDrawable(new Divider());
        panel.setDividerPadding(dp(8));
        panel.setShowDividers(LinearLayout.SHOW_DIVIDER_BEGINNING | LinearLayout.SHOW_DIVIDER_MIDDLE | LinearLayout.SHOW_DIVIDER_END);

        return panel;
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
            PropertyValuesHolder scaleX = PropertyValuesHolder.ofKeyframe(View.SCALE_X, kfStart, kfEnd);
            PropertyValuesHolder scaleY = PropertyValuesHolder.ofKeyframe(View.SCALE_Y, kfStart.copy(), kfEnd.copy());
            kfStart = Keyframe.ofFloat(0, 0);
            kfEnd = Keyframe.ofFloat(1, 1);
            kfEnd.setInterpolator(TimeInterpolator.DECELERATE_CUBIC);
            PropertyValuesHolder alpha = PropertyValuesHolder.ofKeyframe(View.ALPHA, kfStart, kfEnd);
            final Animator animator = ObjectAnimator.ofPropertyValuesHolder(null, scaleX, scaleY, alpha);
            animator.setDuration(400);
            // we use keyframe-specified interpolators
            animator.setInterpolator(null);
            return animator;
        }
        return super.onCreateAnimator(transit, enter, nextAnim);
    }

    private static final TextDir[] TEXT_DIRS = {
            new TextDir(View.TEXT_DIRECTION_FIRST_STRONG, "FirstStrong"),
            new TextDir(View.TEXT_DIRECTION_ANY_RTL, "AnyRTL-LTR"),
            new TextDir(View.TEXT_DIRECTION_LTR, "LTR"),
            new TextDir(View.TEXT_DIRECTION_RTL, "RTL"),
            new TextDir(View.TEXT_DIRECTION_LOCALE, "Locale"),
            new TextDir(View.TEXT_DIRECTION_FIRST_STRONG_LTR, "FirstStrong-LTR"),
            new TextDir(View.TEXT_DIRECTION_FIRST_STRONG_RTL, "FirstStrong-RTL"),
    };

    private record TextDir(int key, String text) {

        @Override
        public String toString() {
            return text;
        }
    }

    private static class Background extends Drawable {

        private final float mRadius;
        private final float mStrokeWidth;

        private Background() {
            mRadius = dp(8);
            mStrokeWidth = dp(4);
        }

        @Override
        public void draw(@Nonnull Canvas canvas) {
            Paint paint = Paint.take();
            Rect bounds = getBounds();
            paint.setStyle(Paint.FILL);
            paint.setColor(BACKGROUND_COLOR);
            float inner = mStrokeWidth * 0.5f;
            canvas.drawRoundRect(bounds.left + inner, bounds.top + inner, bounds.right - inner,
                    bounds.bottom - inner, mRadius, paint);
            paint.setStyle(Paint.STROKE);
            paint.setColors(THEME_COLOR, THEME_COLOR_2, THEME_COLOR, THEME_COLOR_2);
            paint.setStrokeWidth(mStrokeWidth);
            paint.setSmoothRadius(inner);
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
