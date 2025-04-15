/*
 * Modern UI.
 * Copyright (C) 2024 BloCamLimb. All rights reserved.
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
 *   Copyright (C) 2014 The Android Open Source Project
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

package icyllis.modernui.graphics.drawable;

import icyllis.arc3d.core.Color;
import icyllis.modernui.R;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.*;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.util.ColorStateList;

import java.util.Arrays;
import java.util.Objects;

/**
 * Drawable that shows a ripple effect in response to state changes. The
 * anchoring position of the ripple for a given state may be specified by
 * calling {@link #setHotspot(float, float)} with the corresponding state
 * attribute identifier.
 * <p>
 * A touch feedback drawable may contain multiple child layers, including a
 * special mask layer that is not drawn to the screen. A single layer may be
 * set as the mask from XML by specifying its {@code android:id} value as
 * {@link icyllis.modernui.R.id#mask}. At runtime, a single layer may be set as the
 * mask using {@code setId(..., R.id.mask)} or an existing mask layer
 * may be replaced using {@code setDrawableByLayerId(R.id.mask, ...)}.
 * <p>
 * If a mask layer is set, the ripple effect will be masked against that layer
 * before it is drawn over the composite of the remaining child layers.
 * <p>
 * If no mask layer is set, the ripple effect is masked against the composite
 * of the child layers.
 * <p>
 * If no child layers or mask is specified and the ripple is set as a View
 * background, the ripple will be drawn atop the first available parent
 * background within the View's hierarchy. In this case, the drawing region
 * may extend outside of the Drawable bounds.
 */
// Modified from Android
    //TODO Use analytical methods instead of textures to implement masks
public class RippleDrawable extends LayerDrawable {

    /**
     * Radius value that specifies the ripple radius should be computed based
     * on the size of the ripple's container.
     */
    public static final int RADIUS_AUTO = -1;

    /**
     * Ripple style where a solid circle is drawn. This is also the default style
     * @see #setRippleStyle(int)
     */
    public static final int STYLE_SOLID = 0;

    /** The maximum number of ripples supported. */
    private static final int MAX_RIPPLES = 10;
    private static final int DEFAULT_EFFECT_COLOR = 0x8dffffff;

    private final Rect mTempRect = new Rect();

    /** Current ripple effect bounds, used to constrain ripple effects. */
    private final Rect mHotspotBounds = new Rect();

    /** Current drawing bounds, used to compute dirty region. */
    private final Rect mDrawingBounds = new Rect();

    /** Current dirty bounds, union of current and previous drawing bounds. */
    private final Rect mDirtyBounds = new Rect();

    /** Mirrors mLayerState with some extra information. */
    private RippleState mState;

    /** The masking layer, e.g. the layer with id R.id.mask. */
    private Drawable mMask;

    /** The current background. May be actively animating or pending entry. */
    private RippleBackground mBackground;

    private BlendModeColorFilter mMaskColorFilter;
    private BlendModeColorFilter mFocusColorFilter;
    private boolean mHasValidMask;

    /** The current ripple. May be actively animating or pending entry. */
    private RippleForeground mRipple;

    /** Whether we expect to draw a ripple when visible. */
    private boolean mRippleActive;

    // Hotspot coordinates that are awaiting activation.
    private float mPendingX;
    private float mPendingY;
    private boolean mHasPending;

    /**
     * Lazily-created array of actively animating ripples. Inactive ripples are
     * pruned during draw(). The locations of these will not change.
     */
    private RippleForeground[] mExitingRipples;
    private int mExitingRipplesCount = 0;

    /** Paint used to control appearance of ripples. */
    private Paint mRipplePaint;

    /** Target density of the display into which ripples are drawn. */
    private int mDensity;

    /** Whether bounds are being overridden. */
    private boolean mOverrideBounds;

    /**
     * Creates a new ripple drawable with the specified ripple color and
     * optional content and mask drawables.
     *
     * @param color The ripple color
     * @param content The content drawable, may be {@code null}
     * @param mask The mask drawable, may be {@code null}
     */
    public RippleDrawable(@NonNull ColorStateList color, @Nullable Drawable content,
                          @Nullable Drawable mask) {
        this(new RippleState(null, null, null), null);

        if (content != null) {
            addLayer(content, null, 0, 0, 0, 0, 0);
        }

        if (mask != null) {
            addLayer(mask, null, R.id.mask, 0, 0, 0, 0);
        }

        setColor(color);
        ensurePadding();
        refreshPadding();
        updateLocalState();
    }

    @Override
    public void jumpToCurrentState() {
        super.jumpToCurrentState();

        if (mRipple != null) {
            mRipple.end();
        }

        if (mBackground != null) {
            mBackground.end();
        }

        cancelExitingRipples();
    }

    private void cancelExitingRipples() {
        final int count = mExitingRipplesCount;
        final RippleForeground[] ripples = mExitingRipples;
        for (int i = 0; i < count; i++) {
            ripples[i].end();
        }

        if (ripples != null) {
            Arrays.fill(ripples, 0, count, null);
        }
        mExitingRipplesCount = 0;
        // Always draw an additional "clean" frame after canceling animations.
        invalidateSelf(false);
    }

    @Override
    protected boolean onStateChange(@NonNull int[] stateSet) {
        final boolean changed = super.onStateChange(stateSet);

        boolean enabled = false;
        boolean pressed = false;
        boolean focused = false;
        boolean hovered = false;
        boolean windowFocused = false;

        for (int state : stateSet) {
            if (state == R.attr.state_enabled) {
                enabled = true;
            } else if (state == R.attr.state_focused) {
                focused = true;
            } else if (state == R.attr.state_pressed) {
                pressed = true;
            } else if (state == R.attr.state_hovered) {
                hovered = true;
            } else if (state == R.attr.state_window_focused) {
                windowFocused = true;
            }
        }
        setRippleActive(enabled && pressed);
        setBackgroundActive(hovered, focused, pressed, windowFocused);

        return changed;
    }

    private void setRippleActive(boolean active) {
        if (mRippleActive != active) {
            mRippleActive = active;
            if (mState.mRippleStyle == STYLE_SOLID) {
                if (active) {
                    tryRippleEnter();
                } else {
                    tryRippleExit();
                }
            }
        }
    }

    public void setBackgroundActive(boolean hovered, boolean focused, boolean pressed,
                                    boolean windowFocused) {
        if (mState.mRippleStyle == STYLE_SOLID) {
            if (mBackground == null && (hovered || focused)) {
                mBackground = new RippleBackground(this, mHotspotBounds);
                mBackground.setup(mState.mMaxRadius, mDensity);
            }
            if (mBackground != null) {
                mBackground.setState(focused, hovered, pressed);
            }
        }
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        super.onBoundsChange(bounds);

        if (!mOverrideBounds) {
            mHotspotBounds.set(bounds);
            onHotspotBoundsChanged();
        }

        final int count = mExitingRipplesCount;
        final RippleForeground[] ripples = mExitingRipples;
        for (int i = 0; i < count; i++) {
            ripples[i].onBoundsChange();
        }

        if (mBackground != null) {
            mBackground.onBoundsChange();
        }

        if (mRipple != null) {
            mRipple.onBoundsChange();
        }
        invalidateSelf();
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        final boolean changed = super.setVisible(visible, restart);

        if (!visible) {
            clearHotspots();
        } else if (changed) {
            // If we just became visible, ensure the background and ripple
            // visibilities are consistent with their internal states.
            if (mRippleActive) {
                if (mState.mRippleStyle == STYLE_SOLID) {
                    tryRippleEnter();
                } else {
                    invalidateSelf();
                }
            }

            // Skip animations, just show the correct final states.
            jumpToCurrentState();
        }

        return changed;
    }

    private boolean isBounded() {
        return getNumberOfLayers() > 0;
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    public boolean hasFocusStateSpecified() {
        return true;
    }

    /**
     * Sets the ripple color.
     *
     * @param color Ripple color as a color state list.
     */
    public void setColor(@NonNull ColorStateList color) {
        mState.mColor = Objects.requireNonNull(color, "color cannot be null");
        invalidateSelf(false);
    }
    /**
     * Sets the ripple effect color.
     *
     * @param color Ripple color as a color state list.
     *
     */
    public void setEffectColor(@NonNull ColorStateList color) {
        mState.mEffectColor = Objects.requireNonNull(color, "color cannot be null");
        invalidateSelf(false);
    }

    /**
     * @return The ripple effect color as a color state list.
     */
    public @NonNull ColorStateList getEffectColor() {
        return mState.mEffectColor;
    }

    /**
     * Sets the radius in pixels of the fully expanded ripple.
     *
     * @param radius ripple radius in pixels, or {@link #RADIUS_AUTO} to
     *               compute the radius based on the container size
     */
    public void setRadius(int radius) {
        mState.mMaxRadius = radius;
        invalidateSelf(false);
    }

    /**
     * @return the radius in pixels of the fully expanded ripple if an explicit
     *         radius has been set, or {@link #RADIUS_AUTO} if the radius is
     *         computed based on the container size
     */
    public int getRadius() {
        return mState.mMaxRadius;
    }

    @Override
    public boolean setDrawableByLayerId(int id, Drawable drawable) {
        if (super.setDrawableByLayerId(id, drawable)) {
            if (id == R.id.mask) {
                mMask = drawable;
                mHasValidMask = false;
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean canApplyTheme() {
        return (mState != null && mState.canApplyTheme()) || super.canApplyTheme();
    }

    @Override
    public void setHotspot(float x, float y) {
        mPendingX = x;
        mPendingY = y;
        if (mRipple == null || mBackground == null) {
            mHasPending = true;
        }

        if (mRipple != null) {
            mRipple.move(x, y);
        }
    }
    /**
     * Attempts to start an enter animation for the active hotspot. Fails if
     * there are too many animating ripples.
     */
    private void tryRippleEnter() {
        if (mExitingRipplesCount >= MAX_RIPPLES) {
            // This should never happen unless the user is tapping like a maniac
            // or there is a bug that's preventing ripples from being removed.
            return;
        }

        if (mRipple == null) {
            final float x;
            final float y;
            if (mHasPending) {
                mHasPending = false;
                x = mPendingX;
                y = mPendingY;
            } else {
                x = mHotspotBounds.exactCenterX();
                y = mHotspotBounds.exactCenterY();
            }

            mRipple = new RippleForeground(this, mHotspotBounds, x, y);
        }

        mRipple.setup(mState.mMaxRadius, mDensity);
        mRipple.enter();
    }

    /**
     * Attempts to start an exit animation for the active hotspot. Fails if
     * there is no active hotspot.
     */
    private void tryRippleExit() {
        if (mRipple != null) {
            if (mExitingRipples == null) {
                mExitingRipples = new RippleForeground[MAX_RIPPLES];
            }
            mExitingRipples[mExitingRipplesCount++] = mRipple;
            mRipple.exit();
            mRipple = null;
        }
    }

    /**
     * Cancels and removes the active ripple, all exiting ripples, and the
     * background. Nothing will be drawn after this method is called.
     */
    private void clearHotspots() {
        if (mRipple != null) {
            mRipple.end();
            mRipple = null;
            mRippleActive = false;
        }

        if (mBackground != null) {
            mBackground.setState(false, false, false);
        }

        cancelExitingRipples();
    }

    @Override
    public void setHotspotBounds(int left, int top, int right, int bottom) {
        mOverrideBounds = true;
        mHotspotBounds.set(left, top, right, bottom);

        onHotspotBoundsChanged();
    }

    @Override
    public void getHotspotBounds(Rect outRect) {
        outRect.set(mHotspotBounds);
    }

    /**
     * Notifies all the animating ripples that the hotspot bounds have changed and modify sessions.
     */
    private void onHotspotBoundsChanged() {
        final int count = mExitingRipplesCount;
        final RippleForeground[] ripples = mExitingRipples;
        for (int i = 0; i < count; i++) {
            ripples[i].onHotspotBoundsChanged();
        }

        if (mRipple != null) {
            mRipple.onHotspotBoundsChanged();
        }

        if (mBackground != null) {
            mBackground.onHotspotBoundsChanged();
        }
    }

    /**
     * Populates <code>outline</code> with the first available layer outline,
     * excluding the mask layer.
     *
     * @param outline Outline in which to place the first available layer outline
     */
    @Override
    public void getOutline(@NonNull Outline outline) {
        final LayerState state = mLayerState;
        final ChildDrawable[] children = state.mChildren;
        final int N = state.mNumChildren;
        for (int i = 0; i < N; i++) {
            if (children[i].mId != R.id.mask) {
                children[i].mDrawable.getOutline(outline);
                if (!outline.isEmpty()) return;
            }
        }
    }

    /**
     * Optimized for drawing ripples with a mask layer and optional content.
     */
    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mState.mRippleStyle == STYLE_SOLID) {
            drawSolid(canvas);
        }
    }

    private void drawSolid(Canvas canvas) {
        pruneRipples();

        // Clip to the dirty bounds, which will be the drawable bounds if we
        // have a mask or content and the ripple bounds if we're projecting.
        final Rect bounds = getDirtyBounds();
        final int saveCount = canvas.save();
        if (isBounded()) {
            canvas.clipRect(bounds);
        }

        drawContent(canvas);
        drawBackgroundAndRipples(canvas);

        canvas.restoreToCount(saveCount);
    }

    private void pruneRipples() {
        int remaining = 0;

        // Move remaining entries into pruned spaces.
        final RippleForeground[] ripples = mExitingRipples;
        final int count = mExitingRipplesCount;
        for (int i = 0; i < count; i++) {
            if (!ripples[i].hasFinishedExit()) {
                ripples[remaining++] = ripples[i];
            }
        }

        // Null out the remaining entries.
        for (int i = remaining; i < count; i++) {
            ripples[i] = null;
        }

        mExitingRipplesCount = remaining;
    }

    private void drawContent(Canvas canvas) {
        // Draw everything except the mask.
        final ChildDrawable[] array = mLayerState.mChildren;
        final int count = mLayerState.mNumChildren;
        for (int i = 0; i < count; i++) {
            if (array[i].mId != R.id.mask) {
                array[i].mDrawable.draw(canvas);
            }
        }
    }

    private void drawBackgroundAndRipples(Canvas canvas) {
        final RippleForeground active = mRipple;
        final RippleBackground background = mBackground;
        final int count = mExitingRipplesCount;
        if (active == null && count <= 0 && (background == null || !background.isVisible())) {
            // Move along, nothing to draw here.
            return;
        }

        final float x = mHotspotBounds.exactCenterX();
        final float y = mHotspotBounds.exactCenterY();
        canvas.translate(x, y);

        final Paint p = updateRipplePaint();

        if (background != null && background.isVisible()) {
            background.draw(canvas, p);
        }

        if (count > 0) {
            final RippleForeground[] ripples = mExitingRipples;
            for (int i = 0; i < count; i++) {
                ripples[i].draw(canvas, p);
            }
        }

        if (active != null) {
            active.draw(canvas, p);
        }

        canvas.translate(-x, -y);
    }

    Paint updateRipplePaint() {
        if (mRipplePaint == null) {
            mRipplePaint = new Paint();
            mRipplePaint.setAntiAlias(true);
            mRipplePaint.setStyle(Paint.Style.FILL);
        }

        final float x = mHotspotBounds.exactCenterX();
        final float y = mHotspotBounds.exactCenterY();

        // Grab the color for the current state and cut the alpha channel in
        // half so that the ripple and background together yield full alpha.
        final int color = mState.mColor.getColorForState(getState(), Color.BLACK);
        final Paint p = mRipplePaint;

        /*if (mMaskColorFilter != null) {
            // The ripple timing depends on the paint's alpha value, so we need
            // to push just the alpha channel into the paint and let the filter
            // handle the full-alpha color.
            int maskColor = mState.mRippleStyle == STYLE_PATTERNED ? color : color | 0xFF000000;
            if (mMaskColorFilter.getColor() != maskColor) {
                mMaskColorFilter = new PorterDuffColorFilter(maskColor, mMaskColorFilter.getMode());
                mFocusColorFilter = new PorterDuffColorFilter(color | 0xFF000000,
                        mFocusColorFilter.getMode());
            }
            p.setColor(color & 0xFF000000);
            p.setColorFilter(mMaskColorFilter);
            p.setShader(mMaskShader);
        } else {*/
            p.setColor(color);
            p.setColorFilter(null);
            p.setShader(null);
        //}

        return p;
    }

    public Rect getDirtyBounds() {
        if (!isBounded()) {
            final Rect drawingBounds = mDrawingBounds;
            final Rect dirtyBounds = mDirtyBounds;
            dirtyBounds.set(drawingBounds);
            drawingBounds.setEmpty();

            final int cX = (int) mHotspotBounds.exactCenterX();
            final int cY = (int) mHotspotBounds.exactCenterY();
            final Rect rippleBounds = mTempRect;

            final RippleForeground[] activeRipples = mExitingRipples;
            final int N = mExitingRipplesCount;
            for (int i = 0; i < N; i++) {
                activeRipples[i].getBounds(rippleBounds);
                rippleBounds.offset(cX, cY);
                drawingBounds.union(rippleBounds);
            }

            final RippleBackground background = mBackground;
            if (background != null) {
                background.getBounds(rippleBounds);
                rippleBounds.offset(cX, cY);
                drawingBounds.union(rippleBounds);
            }

            dirtyBounds.union(drawingBounds);
            dirtyBounds.union(getBounds());
            return dirtyBounds;
        } else {
            return getBounds();
        }
    }

    @Override
    public void invalidateSelf() {
        invalidateSelf(true);
    }

    void invalidateSelf(boolean invalidateMask) {
        super.invalidateSelf();

        if (invalidateMask) {
            // Force the mask to update on the next draw().
            mHasValidMask = false;
        }

    }

    @Override
    public ConstantState getConstantState() {
        return mState;
    }

    @Override
    public Drawable mutate() {
        super.mutate();

        // LayerDrawable creates a new state using createConstantState, so
        // this should always be a safe cast.
        mState = (RippleState) mLayerState;

        // The locally cached drawable may have changed.
        mMask = findDrawableByLayerId(R.id.mask);

        return this;
    }

    @Override
    RippleState createConstantState(LayerState state, Resources res) {
        return new RippleState(state, this, res);
    }

    static class RippleState extends LayerState {
        int[] mTouchThemeAttrs;
        ColorStateList mColor = ColorStateList.valueOf(Color.MAGENTA);
        ColorStateList mEffectColor = ColorStateList.valueOf(DEFAULT_EFFECT_COLOR);
        int mMaxRadius = RADIUS_AUTO;
        int mRippleStyle = STYLE_SOLID;

        public RippleState(LayerState orig, RippleDrawable owner, Resources res) {
            super(orig, owner, res);

            if (orig instanceof RippleState origs) {
                mTouchThemeAttrs = origs.mTouchThemeAttrs;
                mColor = origs.mColor;
                mMaxRadius = origs.mMaxRadius;
                mRippleStyle = origs.mRippleStyle;
                mEffectColor = origs.mEffectColor;

                if (origs.mDensity != mDensity) {
                    applyDensityScaling(orig.mDensity, mDensity);
                }
            }
        }

        @Override
        protected void onDensityChanged(int sourceDensity, int targetDensity) {
            super.onDensityChanged(sourceDensity, targetDensity);

            applyDensityScaling(sourceDensity, targetDensity);
        }

        private void applyDensityScaling(int sourceDensity, int targetDensity) {
            if (mMaxRadius != RADIUS_AUTO) {
                mMaxRadius = Drawable.scaleFromDensity(
                        mMaxRadius, sourceDensity, targetDensity, true);
            }
        }

        @Override
        public boolean canApplyTheme() {
            return mTouchThemeAttrs != null
                    //|| (mColor != null && mColor.canApplyTheme())
                    || super.canApplyTheme();
        }

        @Override
        public Drawable newDrawable() {
            return new RippleDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new RippleDrawable(this, res);
        }

        @Override
        public int getChangingConfigurations() {
            return super.getChangingConfigurations()
                    ;//| (mColor != null ? mColor.getChangingConfigurations() : 0);
        }
    }

    private RippleDrawable(RippleState state, Resources res) {
        mState = new RippleState(state, this, res);
        mLayerState = mState;
        mDensity = Drawable.resolveDensity(res, mState.mDensity);

        if (mState.mNumChildren > 0) {
            ensurePadding();
            refreshPadding();
        }

        updateLocalState();
    }

    private void updateLocalState() {
        // Initialize from constant state.
        mMask = findDrawableByLayerId(R.id.mask);
    }
}
