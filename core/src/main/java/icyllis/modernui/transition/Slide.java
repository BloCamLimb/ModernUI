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

package icyllis.modernui.transition;


import icyllis.modernui.animation.Animator;
import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import org.intellij.lang.annotations.MagicConstant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This transition tracks changes to the visibility of target views in the
 * start and end scenes and moves views in or out from one of the edges of the
 * scene. Visibility is determined by both the
 * {@link View#setVisibility(int)} state of the view as well as whether it
 * is parented in the current view hierarchy. Disappearing Views are
 * limited as described in {@link Visibility#onDisappear(ViewGroup,
 * TransitionValues, int, TransitionValues, int)}.
 */
public class Slide extends Visibility {

    private static final String PROPNAME_SCREEN_POSITION = "modernui:slide:screenPosition";
    private CalculateSlide mSlideCalculator = sCalculateBottom;
    private int mSlideEdge = Gravity.BOTTOM;

    @Retention(RetentionPolicy.SOURCE)
    @MagicConstant(intValues = {Gravity.LEFT, Gravity.TOP, Gravity.RIGHT, Gravity.BOTTOM, Gravity.START, Gravity.END})
    public @interface GravityFlag {
    }

    private interface CalculateSlide {

        /**
         * Returns the translation value for view when it goes out of the scene
         */
        float getGoneX(@Nonnull ViewGroup sceneRoot, @Nonnull View view);

        /**
         * Returns the translation value for view when it goes out of the scene
         */
        float getGoneY(@Nonnull ViewGroup sceneRoot, @Nonnull View view);
    }

    private abstract static class CalculateSlideHorizontal implements CalculateSlide {

        @Override
        public float getGoneY(@Nonnull ViewGroup sceneRoot, @Nonnull View view) {
            return view.getTranslationY();
        }
    }

    private abstract static class CalculateSlideVertical implements CalculateSlide {

        @Override
        public float getGoneX(@Nonnull ViewGroup sceneRoot, @Nonnull View view) {
            return view.getTranslationX();
        }
    }

    private static final CalculateSlide sCalculateLeft = new CalculateSlideHorizontal() {

        @Override
        public float getGoneX(@Nonnull ViewGroup sceneRoot, @Nonnull View view) {
            return view.getTranslationX() - sceneRoot.getWidth();
        }
    };

    private static final CalculateSlide sCalculateStart = new CalculateSlideHorizontal() {

        @Override
        public float getGoneX(@Nonnull ViewGroup sceneRoot, @Nonnull View view) {
            final boolean isRtl = sceneRoot.isLayoutRtl();
            final float x;
            if (isRtl) {
                x = view.getTranslationX() + sceneRoot.getWidth();
            } else {
                x = view.getTranslationX() - sceneRoot.getWidth();
            }
            return x;
        }
    };

    private static final CalculateSlide sCalculateTop = new CalculateSlideVertical() {

        @Override
        public float getGoneY(@Nonnull ViewGroup sceneRoot, @Nonnull View view) {
            return view.getTranslationY() - sceneRoot.getHeight();
        }
    };

    private static final CalculateSlide sCalculateRight = new CalculateSlideHorizontal() {

        @Override
        public float getGoneX(@Nonnull ViewGroup sceneRoot, @Nonnull View view) {
            return view.getTranslationX() + sceneRoot.getWidth();
        }
    };

    private static final CalculateSlide sCalculateEnd = new CalculateSlideHorizontal() {

        @Override
        public float getGoneX(@Nonnull ViewGroup sceneRoot, @Nonnull View view) {
            final boolean isRtl = sceneRoot.isLayoutRtl();
            final float x;
            if (isRtl) {
                x = view.getTranslationX() - sceneRoot.getWidth();
            } else {
                x = view.getTranslationX() + sceneRoot.getWidth();
            }
            return x;
        }
    };

    private static final CalculateSlide sCalculateBottom = new CalculateSlideVertical() {

        @Override
        public float getGoneY(@Nonnull ViewGroup sceneRoot, @Nonnull View view) {
            return view.getTranslationY() + sceneRoot.getHeight();
        }
    };

    /**
     * Constructor using the default {@link Gravity#BOTTOM}
     * slide edge direction.
     */
    public Slide() {
        setSlideEdge(Gravity.BOTTOM);
    }

    /**
     * Constructor using the provided slide edge direction.
     */
    public Slide(@GravityFlag int slideEdge) {
        setSlideEdge(slideEdge);
    }

    private void captureValues(TransitionValues transitionValues) {
        View view = transitionValues.view;
        int[] position = new int[2];
        view.getLocationInWindow(position);
        transitionValues.values.put(PROPNAME_SCREEN_POSITION, position);
    }

    @Override
    public void captureStartValues(@Nonnull TransitionValues transitionValues) {
        super.captureStartValues(transitionValues);
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(@Nonnull TransitionValues transitionValues) {
        super.captureEndValues(transitionValues);
        captureValues(transitionValues);
    }

    /**
     * Change the edge that Views appear and disappear from.
     *
     * @param slideEdge The edge of the scene to use for Views appearing and disappearing. One of
     *                  {@link Gravity#LEFT}, {@link  Gravity#TOP},
     *                  {@link Gravity#RIGHT}, {@link Gravity#BOTTOM},
     *                  {@link Gravity#START}, {@link Gravity#END}.
     */
    public void setSlideEdge(@GravityFlag int slideEdge) {
        switch (slideEdge) {
            case Gravity.LEFT -> mSlideCalculator = sCalculateLeft;
            case Gravity.TOP -> mSlideCalculator = sCalculateTop;
            case Gravity.RIGHT -> mSlideCalculator = sCalculateRight;
            case Gravity.BOTTOM -> mSlideCalculator = sCalculateBottom;
            case Gravity.START -> mSlideCalculator = sCalculateStart;
            case Gravity.END -> mSlideCalculator = sCalculateEnd;
            default -> throw new IllegalArgumentException("Invalid slide direction");
        }
        mSlideEdge = slideEdge;
        SidePropagation propagation = new SidePropagation();
        propagation.setSide(slideEdge);
        setPropagation(propagation);
    }

    /**
     * Returns the edge that Views appear and disappear from.
     *
     * @return the edge of the scene to use for Views appearing and disappearing. One of
     * {@link Gravity#LEFT}, {@link  Gravity#TOP},
     * {@link Gravity#RIGHT}, {@link Gravity#BOTTOM},
     * {@link Gravity#START}, {@link Gravity#END}.
     */
    @GravityFlag
    public int getSlideEdge() {
        return mSlideEdge;
    }

    @Nullable
    @Override
    public Animator onAppear(ViewGroup sceneRoot, View view,
                             TransitionValues startValues, TransitionValues endValues) {
        if (endValues == null) {
            return null;
        }
        int[] position = (int[]) endValues.values.get(PROPNAME_SCREEN_POSITION);
        float endX = view.getTranslationX();
        float endY = view.getTranslationY();
        float startX = mSlideCalculator.getGoneX(sceneRoot, view);
        float startY = mSlideCalculator.getGoneY(sceneRoot, view);
        return TranslationAnimationCreator
                .createAnimation(view, endValues, position[0], position[1],
                        startX, startY, endX, endY, TimeInterpolator.DECELERATE, this);
    }

    @Nullable
    @Override
    public Animator onDisappear(ViewGroup sceneRoot, View view,
                                TransitionValues startValues, TransitionValues endValues) {
        if (startValues == null) {
            return null;
        }
        int[] position = (int[]) startValues.values.get(PROPNAME_SCREEN_POSITION);
        float startX = view.getTranslationX();
        float startY = view.getTranslationY();
        float endX = mSlideCalculator.getGoneX(sceneRoot, view);
        float endY = mSlideCalculator.getGoneY(sceneRoot, view);
        return TranslationAnimationCreator
                .createAnimation(view, startValues, position[0], position[1],
                        startX, startY, endX, endY, TimeInterpolator.ACCELERATE, this);
    }
}
