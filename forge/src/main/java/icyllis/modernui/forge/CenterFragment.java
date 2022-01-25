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

import icyllis.modernui.animation.*;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.fragment.FragmentTransaction;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.math.MathUtil;
import icyllis.modernui.math.Rect;
import icyllis.modernui.text.FontPaint;
import icyllis.modernui.text.InputFilter;
import icyllis.modernui.text.method.ArrowKeyMovementMethod;
import icyllis.modernui.text.method.DigitsInputFilter;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

import static icyllis.modernui.view.ViewConfiguration.dp;

public class CenterFragment extends Fragment {

    public static final int BACKGROUND_COLOR = 0xc0292a2c;
    public static final int THEME_COLOR = 0xffcda398;

    @Nullable
    @Override
    public View onCreateView(@Nullable ViewGroup container, @Nullable DataSet savedInstanceState) {
        LinearLayout base = new LinearLayout();
        base.setOrientation(LinearLayout.VERTICAL);
        base.setBackground(new Background());
        {
            TextView title = new TextView();
            title.setText("Modern UI Center");
            title.setTextSize(18);
            title.setTextStyle(FontPaint.BOLD);

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            params.setMargins(0, dp(12), 0, dp(18));
            base.addView(title, params);
        }
        RelativeLayout screen = new RelativeLayout();
        {
            TextView title = new TextView();
            title.setText("Screen");
            title.setTextSize(16);
            title.setTextColor(THEME_COLOR);
            title.setId(15);

            RelativeLayout.LayoutParams params =
                    new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            params.addRule(RelativeLayout.ALIGN_PARENT_START);
            params.setMargins(dp(6), dp(6), dp(6), dp(6));
            screen.addView(title, params);

            {
                TextView view = new TextView();
                view.setText("Blur Radius");
                view.setTextSize(14);
                view.setId(16);

                params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, 15);
                params.addRule(RelativeLayout.ALIGN_START, 15);
                screen.addView(view, params);

                TextView input = new TextView();
                input.setText(Config.CLIENT.blurRadius.get().toString(), TextView.BufferType.EDITABLE);
                input.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                input.setTextSize(14);
                input.setGravity(Gravity.CENTER_VERTICAL);
                input.setFocusableInTouchMode(true);
                input.setMovementMethod(ArrowKeyMovementMethod.getInstance());
                input.setFilters(new InputFilter[]{DigitsInputFilter.getInstance((Locale) null),
                        new InputFilter.LengthFilter(2)});
                input.setOnFocusChangeListener((__, hasFocus) -> {
                    if (!hasFocus) {
                        int radius = Integer.parseInt(input.getText().toString());
                        radius = MathUtil.clamp(radius, 2, 18);
                        input.setText(Integer.toString(radius));
                        if (radius != Config.CLIENT.blurRadius.get()) {
                            Config.CLIENT.blurRadius.set(radius);
                            Config.CLIENT.saveAndReload();
                        }
                    }
                });

                params = new RelativeLayout.LayoutParams(dp(40), ViewGroup.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_BASELINE, 16);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp(6), 0, dp(6), 0);
                screen.addView(input, params);
            }

            {
                TextView view = new TextView();
                view.setText("Blur Effect");
                view.setTextSize(14);
                view.setId(18);

                params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, 16);
                params.addRule(RelativeLayout.ALIGN_START, 16);
                screen.addView(view, params);

                SwitchButton button = new SwitchButton();
                button.setCheckedColor(THEME_COLOR);
                button.setChecked(Config.CLIENT.blurEffect.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    Config.CLIENT.blurEffect.set(checked);
                    Config.CLIENT.saveAndReload();
                });
                button.setClickable(true);

                params = new RelativeLayout.LayoutParams(dp(36), dp(16));
                params.addRule(RelativeLayout.ALIGN_TOP, 18);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp(6), dp(3), dp(6), dp(3));
                screen.addView(button, params);
            }
        }
        {
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(dp(350), ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            params.setMargins(dp(12), dp(12), dp(12), dp(18));
            base.postDelayed(() -> base.addView(screen, params), 200);
        }
        RelativeLayout tooltip = new RelativeLayout();
        {
            TextView title = new TextView();
            title.setText("Tooltip");
            title.setTextSize(16);
            title.setTextColor(THEME_COLOR);
            title.setId(25);

            RelativeLayout.LayoutParams params =
                    new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            params.addRule(RelativeLayout.ALIGN_PARENT_START);
            params.setMargins(dp(6), dp(6), dp(6), dp(6));
            tooltip.addView(title, params);

            {
                TextView view = new TextView();
                view.setText("Enable");
                view.setTextSize(14);
                view.setId(26);

                params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, 25);
                params.addRule(RelativeLayout.ALIGN_START, 25);
                tooltip.addView(view, params);

                SwitchButton button = new SwitchButton();
                button.setCheckedColor(THEME_COLOR);
                button.setChecked(Config.CLIENT.tooltip.get());
                button.setOnCheckedChangeListener((__, checked) -> {
                    Config.CLIENT.tooltip.set(checked);
                    Config.CLIENT.saveAndReload();
                });
                button.setClickable(true);

                params = new RelativeLayout.LayoutParams(dp(36), dp(16));
                params.addRule(RelativeLayout.ALIGN_TOP, 26);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp(6), dp(3), dp(6), dp(3));
                tooltip.addView(button, params);
            }
        }
        {
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(dp(350), ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            params.setMargins(dp(12), dp(12), dp(12), dp(18));
            base.postDelayed(() -> base.addView(tooltip, params), 400);
        }
        RelativeLayout layout = new RelativeLayout();
        {
            TextView title = new TextView();
            title.setText("Layout");
            title.setTextSize(16);
            title.setTextColor(THEME_COLOR);
            title.setId(35);

            RelativeLayout.LayoutParams params =
                    new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            params.addRule(RelativeLayout.ALIGN_PARENT_START);
            params.setMargins(dp(6), dp(6), dp(6), dp(6));
            layout.addView(title, params);

            {
                TextView view = new TextView();
                view.setText("RTL Layout");
                view.setTextSize(14);
                view.setId(36);

                params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, 35);
                params.addRule(RelativeLayout.ALIGN_START, 35);
                layout.addView(view, params);

                SwitchButton button = new SwitchButton();
                button.setCheckedColor(THEME_COLOR);
                button.setChecked(UIManager.sInstance.getDecorView().isLayoutRtl());
                button.setOnCheckedChangeListener((__, checked) ->
                        UIManager.sInstance.getDecorView().setLayoutDirection(
                                checked ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_INHERIT));
                button.setClickable(true);

                params = new RelativeLayout.LayoutParams(dp(36), dp(16));
                params.addRule(RelativeLayout.ALIGN_TOP, 36);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(dp(6), dp(3), dp(6), dp(3));
                layout.addView(button, params);
            }
        }
        {
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(dp(350), ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            params.setMargins(dp(12), dp(12), dp(12), dp(18));
            base.postDelayed(() -> base.addView(layout, params), 600);
        }

        base.setDividerDrawable(new Drawable() {
            @Override
            public void draw(@Nonnull Canvas canvas) {
                Paint paint = Paint.take();
                paint.setColor(0xc0606060);
                canvas.drawRect(getBounds(), paint);
            }

            @Override
            public int getIntrinsicHeight() {
                return dp(2);
            }
        });
        base.setDividerPadding(dp(200));
        base.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE | LinearLayout.SHOW_DIVIDER_END);

        base.setLayoutTransition(new LayoutTransition());

        FrameLayout.LayoutParams baseParams = new FrameLayout.LayoutParams(dp(720), dp(450));
        baseParams.gravity = Gravity.CENTER;
        base.setLayoutParams(baseParams);
        return base;
    }

    @Nullable
    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        if (enter && transit == FragmentTransaction.TRANSIT_FRAGMENT_OPEN) {
            Keyframe keyframe = Keyframe.ofFloat(0, 0.75f);
            Keyframe keyframe1 = Keyframe.ofFloat(1, 1);
            keyframe1.setInterpolator(TimeInterpolator.OVERSHOOT);
            PropertyValuesHolder<View, Float, Float> scaleX = PropertyValuesHolder.ofKeyframe(View.SCALE_X,
                    keyframe, keyframe1);
            PropertyValuesHolder<View, Float, Float> scaleY = PropertyValuesHolder.ofKeyframe(View.SCALE_Y,
                    keyframe.copy(), keyframe1.copy());
            PropertyValuesHolder<View, Float, Float> alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0, 1);
            Animator animator = ObjectAnimator.ofPropertyValuesHolder(null, scaleX, scaleY, alpha);
            animator.setDuration(400);
            animator.setInterpolator(TimeInterpolator.LINEAR);
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
            paint.setColor(THEME_COLOR);
            paint.setStrokeWidth(mStrokeWidth);
            canvas.drawRoundRect(bounds.left + inner, bounds.top + inner, bounds.right - inner,
                    bounds.bottom - inner, mRadius, paint);
        }

        @Override
        public boolean getPadding(@Nonnull Rect padding) {
            int inner = (int) Math.ceil(mStrokeWidth * 0.5f);
            padding.set(inner, inner, inner, inner);
            return true;
        }
    }
}
