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

package icyllis.modernui.widget;

import icyllis.modernui.R;
import icyllis.modernui.animation.AnimationUtils;
import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.annotation.ColorInt;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.*;
import icyllis.modernui.resources.TypedArray;
import icyllis.modernui.view.View;

import javax.annotation.Nonnull;

/**
 * This class performs the graphical effect used at the edges of scrollable widgets
 * when the user scrolls beyond the content bounds in 2D space.
 *
 * <p>EdgeEffect is stateful. Custom widgets using EdgeEffect should create an
 * instance for each edge that should show the effect, feed it input data using
 * the methods {@link #onAbsorb(int)}, {@link #onPull(float)}, and {@link #onRelease()},
 * and draw the effect using {@link #draw(Canvas)} in the widget's overridden
 * {@link View#draw(Canvas)} method. If {@link #isFinished()} returns
 * false after drawing, the edge effect's animation is not yet complete and the widget
 * should schedule another drawing pass to continue the animation.</p>
 *
 * <p>When drawing, widgets should draw their main content and child views first,
 * usually by invoking <code>super.draw(canvas)</code> from an overridden <code>draw</code>
 * method. (This will invoke onDraw and dispatch drawing to child views as needed.)
 * The edge effect may then be drawn on top of the view's content using the
 * {@link #draw(Canvas)} method.</p>
 */
public class EdgeEffect {

    /**
     * The default blend mode used by {@link EdgeEffect}.
     */
    public static final BlendMode DEFAULT_BLEND_MODE = BlendMode.SRC_ATOP;

    // Time it will take the effect to fully recede in ms
    private static final int RECEDE_TIME = 600;

    // Time it will take before a pulled glow begins receding in ms
    private static final int PULL_TIME = 167;

    // Time it will take in ms for a pulled glow to decay to partial strength before release
    private static final int PULL_DECAY_TIME = 2000;

    private static final float MAX_ALPHA = 0.15f;
    private static final float GLOW_ALPHA_START = .09f;

    // Minimum velocity that will be absorbed
    private static final int MIN_VELOCITY = 100;
    // Maximum velocity, clamps at this value
    private static final int MAX_VELOCITY = 10000;

    private static final float EPSILON = 0.001f;

    private static final double ANGLE = Math.PI / 6;
    private static final float SIN = (float) Math.sin(ANGLE);
    private static final float COS = (float) Math.cos(ANGLE);
    private static final float RADIUS_FACTOR = 0.6f;

    private float mGlowAlpha;
    private float mGlowScaleY;
    private float mDistance;

    private float mGlowAlphaStart;
    private float mGlowAlphaFinish;
    private float mGlowScaleYStart;
    private float mGlowScaleYFinish;

    private long mStartTime;
    private float mDuration;

    private static final int STATE_IDLE = 0;
    private static final int STATE_PULL = 1;
    private static final int STATE_ABSORB = 2;
    private static final int STATE_RECEDE = 3;
    private static final int STATE_PULL_DECAY = 4;

    private static final float PULL_DISTANCE_ALPHA_GLOW_FACTOR = 0.8f;

    private static final int VELOCITY_GLOW_FACTOR = 6;

    private int mState = STATE_IDLE;

    private float mPullDistance;

    private float mWidth;
    private float mHeight;
    private final Paint mPaint = new Paint();
    private float mRadius;
    private float mBaseGlowScale;
    private float mDisplacement = 0.5f;
    private float mTargetDisplacement = 0.5f;

    private static final String[] STYLEABLE = {
            R.ns, R.attr.colorEdgeEffect
    };

    /**
     * Construct a new EdgeEffect with a theme appropriate for the provided context.
     */
    public EdgeEffect(Context context) {
        final TypedArray a = context.getTheme().obtainStyledAttributes(
                STYLEABLE);
        final int themeColor = a.getColor(0, 0xff666666);
        a.recycle();

        mPaint.setColor((themeColor & 0xffffff) | 0x33000000);
        mPaint.setStyle(Paint.FILL);
        mPaint.setBlendMode(DEFAULT_BLEND_MODE);
    }

    /**
     * Set the size of this edge effect in pixels.
     *
     * @param width  Effect width in pixels
     * @param height Effect height in pixels
     */
    public void setSize(int width, int height) {
        final float r = width * RADIUS_FACTOR / SIN;
        final float y = COS * r;
        final float h = r - y;
        final float or = height * RADIUS_FACTOR / SIN;
        final float oy = COS * or;
        final float oh = or - oy;

        mRadius = r;
        mBaseGlowScale = h > 0 ? Math.min(oh / h, 1.f) : 1.f;

        mWidth = width;
        mHeight = (int) Math.min(height, h);
    }

    /**
     * Reports if this EdgeEffect's animation is finished. If this method returns false
     * after a call to {@link #draw(Canvas)} the host widget should schedule another
     * drawing pass to continue the animation.
     *
     * @return true if animation is finished, false if drawing should continue on the next frame.
     */
    public boolean isFinished() {
        return mState == STATE_IDLE;
    }

    /**
     * Immediately finish the current animation.
     * After this call {@link #isFinished()} will return true.
     */
    public void finish() {
        mState = STATE_IDLE;
        mDistance = 0;
    }

    /**
     * A view should call this when content is pulled away from an edge by the user.
     * This will update the state of the current visual effect and its associated animation.
     * The host view should always {@link View#invalidate()} after this
     * and draw the results accordingly.
     *
     * <p>Views using EdgeEffect should favor {@link #onPull(float, float)} when the displacement
     * of the pull point is known.</p>
     *
     * @param deltaDistance Change in distance since the last call. Values may be 0 (no change) to
     *                      1.f (full length of the view) or negative values to express change
     *                      back toward the edge reached to initiate the effect.
     */
    public void onPull(float deltaDistance) {
        onPull(deltaDistance, 0.5f);
    }

    /**
     * A view should call this when content is pulled away from an edge by the user.
     * This will update the state of the current visual effect and its associated animation.
     * The host view should always {@link View#invalidate()} after this
     * and draw the results accordingly.
     *
     * @param deltaDistance Change in distance since the last call. Values may be 0 (no change) to
     *                      1.f (full length of the view) or negative values to express change
     *                      back toward the edge reached to initiate the effect.
     * @param displacement  The displacement from the starting side of the effect of the point
     *                      initiating the pull. In the case of touch this is the finger position.
     *                      Values may be from 0-1.
     */
    public void onPull(float deltaDistance, float displacement) {
        final long now = AnimationUtils.currentAnimationTimeMillis();
        mTargetDisplacement = displacement;
        if (mState == STATE_PULL_DECAY && now - mStartTime < mDuration) {
            return;
        }
        mState = STATE_PULL;

        mStartTime = now;
        mDuration = PULL_TIME;

        mPullDistance += deltaDistance;
        mDistance = Math.max(0f, mPullDistance);

        if (mPullDistance == 0) {
            mGlowScaleY = mGlowScaleYStart = 0;
            mGlowAlpha = mGlowAlphaStart = 0;
        } else {
            final float absDeltaDis = Math.abs(deltaDistance);
            mGlowAlpha = mGlowAlphaStart = Math.min(MAX_ALPHA,
                    mGlowAlpha + (absDeltaDis * PULL_DISTANCE_ALPHA_GLOW_FACTOR));

            final float scale = (float) (Math.max(0, 1 - 1 /
                    Math.sqrt(Math.abs(mPullDistance) * mHeight) - 0.3d) / 0.7d);

            mGlowScaleY = mGlowScaleYStart = scale;
        }

        mGlowAlphaFinish = mGlowAlpha;
        mGlowScaleYFinish = mGlowScaleY;
    }

    /**
     * A view should call this when content is pulled away from an edge by the user.
     * This will update the state of the current visual effect and its associated animation.
     * The host view should always {@link View#invalidate()} after this
     * and draw the results accordingly. This works similarly to {@link #onPull(float, float)},
     * but returns the amount of <code>deltaDistance</code> that has been consumed. If the
     * {@link #getDistance()} is currently 0 and <code>deltaDistance</code> is negative, this
     * function will return 0 and the drawn value will remain unchanged.
     * <p>
     * This method can be used to reverse the effect from a pull or absorb and partially consume
     * some of a motion:
     *
     * <pre class="prettyprint">
     *     if (deltaY < 0) {
     *         float consumed = edgeEffect.onPullDistance(deltaY / getHeight(), x / getWidth());
     *         deltaY -= consumed * getHeight();
     *         if (edgeEffect.getDistance() == 0f) edgeEffect.onRelease();
     *     }
     * </pre>
     *
     * @param deltaDistance Change in distance since the last call. Values may be 0 (no change) to
     *                      1.f (full length of the view) or negative values to express change
     *                      back toward the edge reached to initiate the effect.
     * @param displacement  The displacement from the starting side of the effect of the point
     *                      initiating the pull. In the case of touch this is the finger position.
     *                      Values may be from 0-1.
     * @return The amount of <code>deltaDistance</code> that was consumed, a number between
     * 0 and <code>deltaDistance</code>.
     */
    public float onPullDistance(float deltaDistance, float displacement) {
        float finalDistance = Math.max(0f, deltaDistance + mDistance);
        float delta = finalDistance - mDistance;
        if (delta == 0f && mDistance == 0f) {
            return 0f; // No pull, don't do anything.
        }

        if (mState != STATE_PULL && mState != STATE_PULL_DECAY) {
            // Catch the edge glow in the middle of an animation.
            mPullDistance = mDistance;
            mState = STATE_PULL;
        }
        onPull(delta, displacement);
        return delta;
    }

    /**
     * Returns the pull distance needed to be released to remove the showing effect.
     * It is determined by the {@link #onPull(float, float)} <code>deltaDistance</code> and
     * any animating values, including from {@link #onAbsorb(int)} and {@link #onRelease()}.
     * <p>
     * This can be used in conjunction with {@link #onPullDistance(float, float)} to
     * release the currently showing effect.
     *
     * @return The pull distance that must be released to remove the showing effect.
     */
    public float getDistance() {
        return mDistance;
    }

    /**
     * Call when the object is released after being pulled.
     * This will begin the "decay" phase of the effect. After calling this method
     * the host view should {@link View#invalidate()} and thereby
     * draw the results accordingly.
     */
    public void onRelease() {
        mPullDistance = 0;

        if (mState != STATE_PULL && mState != STATE_PULL_DECAY) {
            return;
        }

        mState = STATE_RECEDE;
        mGlowAlphaStart = mGlowAlpha;
        mGlowScaleYStart = mGlowScaleY;

        mGlowAlphaFinish = 0.f;
        mGlowScaleYFinish = 0.f;

        mStartTime = AnimationUtils.currentAnimationTimeMillis();
        mDuration = RECEDE_TIME;
    }

    /**
     * Call when the effect absorbs an impact at the given velocity.
     * Used when a fling reaches the scroll boundary.
     *
     * <p>When using a {@link OverScroller},
     * the method <code>getCurrVelocity</code> will provide a reasonable approximation
     * to use here.</p>
     *
     * @param velocity Velocity at impact in pixels per second.
     */
    public void onAbsorb(int velocity) {
        mState = STATE_ABSORB;
        velocity = Math.min(Math.max(MIN_VELOCITY, Math.abs(velocity)), MAX_VELOCITY);

        mStartTime = AnimationUtils.currentAnimationTimeMillis();
        mDuration = 0.15f + (velocity * 0.02f);

        // The glow depends more on the velocity, and therefore starts out
        // nearly invisible.
        mGlowAlphaStart = GLOW_ALPHA_START;
        mGlowScaleYStart = Math.max(mGlowScaleY, 0.f);

        // Growth for the size of the glow should be quadratic to properly
        // respond
        // to a user's scrolling speed. The faster the scrolling speed, the more
        // intense the effect should be for both the size and the saturation.
        final int f = velocity / 100;
        mGlowScaleYFinish = Math.min(0.025f + (velocity * f * 0.00015f) / 2, 1.f);
        // Alpha should change for the glow as well as size.
        mGlowAlphaFinish = Math.max(
                mGlowAlphaStart,
                Math.min(velocity * VELOCITY_GLOW_FACTOR * .00001f, MAX_ALPHA));
        mTargetDisplacement = 0.5f;
    }

    /**
     * Set the color of this edge effect in argb.
     *
     * @param color Color in argb
     */
    public void setColor(@ColorInt int color) {
        mPaint.setColor(color);
    }

    /**
     * Set or clear the blend mode. A blend mode defines how source pixels
     * (generated by a drawing command) are composited with the destination pixels
     * (content of the render target).
     * <p>
     * Pass null to clear any previous blend mode.
     *
     * @param blendMode May be null. The blend mode to be installed in the paint
     * @see BlendMode
     */
    public void setBlendMode(@Nullable BlendMode blendMode) {
        mPaint.setBlendMode(blendMode);
    }

    /**
     * Return the color of this edge effect in argb.
     *
     * @return The color of this edge effect in argb
     */
    @ColorInt
    public int getColor() {
        return mPaint.getColor();
    }

    /**
     * Returns the blend mode. A blend mode defines how source pixels
     * (generated by a drawing command) are composited with the destination pixels
     * (content of the render target).
     *
     * @return BlendMode
     */
    @Nullable
    public BlendMode getBlendMode() {
        return mPaint.getBlendMode();
    }

    /**
     * Draw into the provided canvas. Assumes that the canvas has been rotated
     * accordingly and the size has been set. The effect will be drawn the full
     * width of X=0 to X=width, beginning from Y=0 and extending to some factor <
     * 1.f of height.
     *
     * @param canvas Canvas to draw into
     * @return true if drawing should continue beyond this frame to continue the
     * animation
     */
    public boolean draw(@Nonnull Canvas canvas) {
        update();
        canvas.save();

        final float centerX = mWidth * 0.5f;
        final float centerY = mHeight - mRadius;

        canvas.scale(1.f, Math.min(mGlowScaleY, 1.f) * mBaseGlowScale, centerX, 0);

        final float displacement = Math.max(0, Math.min(mDisplacement, 1.f)) - 0.5f;
        float translateX = mWidth * displacement / 2;

        canvas.translate(translateX, 0);
        mPaint.setAlpha((int) (0xff * mGlowAlpha));
        canvas.drawCircle(centerX, centerY, mRadius, mPaint);
        canvas.restore();

        boolean oneLastFrame = false;
        if (mState == STATE_RECEDE && mDistance == 0) {
            mState = STATE_IDLE;
            oneLastFrame = true;
        }

        return mState != STATE_IDLE || oneLastFrame;
    }

    /**
     * Return the maximum height that the edge effect will be drawn at given the original
     * {@link #setSize(int, int) input size}.
     *
     * @return The maximum height of the edge effect
     */
    public int getMaxHeight() {
        return (int) mHeight;
    }

    private void update() {
        final long time = AnimationUtils.currentAnimationTimeMillis();
        final float t = Math.min((time - mStartTime) / mDuration, 1.f);

        final float p = TimeInterpolator.DECELERATE.getInterpolation(t);

        mGlowAlpha = mGlowAlphaStart + (mGlowAlphaFinish - mGlowAlphaStart) * p;
        mGlowScaleY = mGlowScaleYStart + (mGlowScaleYFinish - mGlowScaleYStart) * p;
        if (mState != STATE_PULL) {
            mDistance = calculateDistanceFromGlowValues(mGlowScaleY, mGlowAlpha);
        }
        mDisplacement = (mDisplacement + mTargetDisplacement) / 2;

        if (t >= 1.f - EPSILON) {
            switch (mState) {
                case STATE_ABSORB -> {
                    mState = STATE_RECEDE;
                    mStartTime = AnimationUtils.currentAnimationTimeMillis();
                    mDuration = RECEDE_TIME;
                    mGlowAlphaStart = mGlowAlpha;
                    mGlowScaleYStart = mGlowScaleY;

                    // After absorb, the glow should fade to nothing.
                    mGlowAlphaFinish = 0.f;
                    mGlowScaleYFinish = 0.f;
                }
                case STATE_PULL -> {
                    mState = STATE_PULL_DECAY;
                    mStartTime = AnimationUtils.currentAnimationTimeMillis();
                    mDuration = PULL_DECAY_TIME;
                    mGlowAlphaStart = mGlowAlpha;
                    mGlowScaleYStart = mGlowScaleY;

                    // After pull, the glow should fade to nothing.
                    mGlowAlphaFinish = 0.f;
                    mGlowScaleYFinish = 0.f;
                }
                case STATE_PULL_DECAY -> mState = STATE_RECEDE;
                case STATE_RECEDE -> mState = STATE_IDLE;
            }
        }
    }

    /**
     * @return The estimated pull distance as calculated from mGlowScaleY.
     */
    private float calculateDistanceFromGlowValues(float scale, float alpha) {
        if (scale >= 1f) {
            // It should asymptotically approach 1, but not reach there.
            // Here, we're just choosing a value that is large.
            return 1f;
        }
        if (scale > 0f) {
            float v = 1f / 0.7f / (mGlowScaleY - 1f);
            return v * v / mHeight;
        }
        return alpha / PULL_DISTANCE_ALPHA_GLOW_FACTOR;
    }
}
