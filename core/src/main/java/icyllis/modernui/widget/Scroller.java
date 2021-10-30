/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

import icyllis.modernui.animation.AnimationHandler;
import icyllis.modernui.animation.TimeInterpolator;

import java.util.Objects;

/**
 * This class controls horizontal and vertical scrolling with the ability to
 * overshoot the bounds of a scrolling operation.
 */
@SuppressWarnings("unused")
public class Scroller {

    private static final int DEFAULT_DURATION = 200;

    private final SplineScroller mScrollerX = new SplineScroller();
    private final SplineScroller mScrollerY = new SplineScroller();

    private final TimeInterpolator mInterpolator;

    private final boolean mFlywheel;

    // fling mode or normal scroll mode
    private boolean mFlingMode;

    /**
     * Creates a Scroller with flywheel.
     */
    public Scroller() {
        this(null);
    }

    public Scroller(TimeInterpolator interpolator) {
        this(interpolator, true);
    }

    public Scroller(TimeInterpolator interpolator, boolean flywheel) {
        mInterpolator = Objects.requireNonNullElse(interpolator, TimeInterpolator.DECELERATE);
        mFlywheel = flywheel;
    }

    /**
     * The amount of friction applied to flings.
     *
     * @param friction A scalar dimension-less value representing the coefficient of
     *                 friction.
     */
    public final void setFriction(float friction) {
        mScrollerX.setFriction(friction);
        mScrollerY.setFriction(friction);
    }

    /**
     * Returns whether the scroller has finished scrolling.
     *
     * @return True if the scroller has finished scrolling, false otherwise.
     */
    public final boolean isFinished() {
        return mScrollerX.mFinished && mScrollerY.mFinished;
    }

    /**
     * Force the finished field to a particular value. Contrary to
     * {@link #abortAnimation()}, forcing the animation to finished
     * does NOT cause the scroller to move to the final x and y
     * position.
     *
     * @param finished The new finished value.
     */
    public final void forceFinished(boolean finished) {
        mScrollerX.mFinished = mScrollerY.mFinished = finished;
    }

    /**
     * Returns the current X offset in the scroll.
     *
     * @return The new X offset as an absolute distance from the origin.
     */
    public final int getCurrX() {
        return mScrollerX.mCurrentPosition;
    }

    /**
     * Returns the current Y offset in the scroll.
     *
     * @return The new Y offset as an absolute distance from the origin.
     */
    public final int getCurrY() {
        return mScrollerY.mCurrentPosition;
    }

    /**
     * Returns the absolute value of the current velocity.
     *
     * @return The original velocity less the deceleration, norm of the X and Y velocity vector.
     */
    public float getCurrVelocity() {
        return (float) Math.hypot(mScrollerX.mCurrVelocity, mScrollerY.mCurrVelocity);
    }

    /**
     * Returns the start X offset in the scroll.
     *
     * @return The start X offset as an absolute distance from the origin.
     */
    public final int getStartX() {
        return mScrollerX.mStart;
    }

    /**
     * Returns the start Y offset in the scroll.
     *
     * @return The start Y offset as an absolute distance from the origin.
     */
    public final int getStartY() {
        return mScrollerY.mStart;
    }

    /**
     * Returns where the scroll will end. Valid only for "fling" scrolls.
     *
     * @return The final X offset as an absolute distance from the origin.
     */
    public final int getFinalX() {
        return mScrollerX.mFinal;
    }

    /**
     * Returns where the scroll will end. Valid only for "fling" scrolls.
     *
     * @return The final Y offset as an absolute distance from the origin.
     */
    public final int getFinalY() {
        return mScrollerY.mFinal;
    }

    /**
     * Call this when you want to know the new location. If it returns true, the
     * animation is not yet finished.
     */
    public boolean computeScrollOffset() {
        if (isFinished()) {
            return false;
        }

        if (mFlingMode) {
            if (!mScrollerX.mFinished) {
                if (!mScrollerX.update()) {
                    if (mScrollerX.continueWhenFinished()) {
                        mScrollerX.finish();
                    }
                }
            }

            if (!mScrollerY.mFinished) {
                if (!mScrollerY.update()) {
                    if (mScrollerY.continueWhenFinished()) {
                        mScrollerY.finish();
                    }
                }
            }
        } else {
            long time = AnimationHandler.currentTimeMillis();
            // Any scroller can be used for time, since they were started
            // together in scroll mode. We use X here.
            final long elapsedTime = time - mScrollerX.mStartTime;

            final int duration = mScrollerX.mDuration;
            if (elapsedTime < duration) {
                final float q = mInterpolator.getInterpolation(elapsedTime / (float) duration);
                mScrollerX.updateScroll(q);
                mScrollerY.updateScroll(q);
            } else {
                abortAnimation();
            }
        }

        return true;
    }

    /**
     * Start scrolling by providing a starting point and the distance to travel.
     *
     * @param startX Starting horizontal scroll offset in pixels. Positive
     *               numbers will scroll the content to the left.
     * @param startY Starting vertical scroll offset in pixels. Positive numbers
     *               will scroll the content up.
     * @param dx     Horizontal distance to travel. Positive numbers will scroll the
     *               content to the left.
     * @param dy     Vertical distance to travel. Positive numbers will scroll the
     *               content up.
     */
    public void startScroll(int startX, int startY, int dx, int dy) {
        int duration;
        int dis = Math.max(Math.abs(dy), Math.abs(dx));
        if (dis > 160.0) {
            duration = (int) (Math.sqrt(dis / 160.0) * DEFAULT_DURATION);
        } else {
            duration = DEFAULT_DURATION;
        }
        double d = AnimationHandler.currentTimeMillis() - mScrollerX.mStartTime;
        if (d < DEFAULT_DURATION * 0.6) {
            duration *= (0.2 * d / (DEFAULT_DURATION * 0.6)) + 0.8;
        }
        startScroll(startX, startY, dx, dy, duration);
    }

    /**
     * Start scrolling by providing a starting point and the distance to travel.
     *
     * @param startX   Starting horizontal scroll offset in pixels. Positive
     *                 numbers will scroll the content to the left.
     * @param startY   Starting vertical scroll offset in pixels. Positive numbers
     *                 will scroll the content up.
     * @param dx       Horizontal distance to travel. Positive numbers will scroll the
     *                 content to the left.
     * @param dy       Vertical distance to travel. Positive numbers will scroll the
     *                 content up.
     * @param duration Duration of the scroll in milliseconds.
     */
    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        mFlingMode = false;
        mScrollerX.startScroll(startX, dx, duration);
        mScrollerY.startScroll(startY, dy, duration);
    }

    /**
     * Call this when you want to 'spring back' into a valid coordinate range.
     *
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param minX   Minimum valid X value
     * @param maxX   Maximum valid X value
     * @param minY   Minimum valid Y value
     * @param maxY   Minimum valid Y value
     * @return true if a springback was initiated, false if startX and startY were
     * already within the valid range.
     */
    public boolean springBack(int startX, int startY, int minX, int maxX, int minY, int maxY) {
        mFlingMode = true;
        // Make sure both methods are called.
        final boolean springbackX = mScrollerX.springback(startX, minX, maxX);
        final boolean springbackY = mScrollerY.springback(startY, minY, maxY);
        return springbackX || springbackY;
    }

    public void fling(int startX, int startY, int velocityX, int velocityY,
                      int minX, int maxX, int minY, int maxY) {
        fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, 0, 0);
    }

    /**
     * Start scrolling based on a fling gesture. The distance traveled will
     * depend on the initial velocity of the fling.
     *
     * @param startX    Starting point of the scroll (X)
     * @param startY    Starting point of the scroll (Y)
     * @param velocityX Initial velocity of the fling (X) measured in pixels per
     *                  second.
     * @param velocityY Initial velocity of the fling (Y) measured in pixels per
     *                  second
     * @param minX      Minimum X value. The scroller will not scroll past this point
     *                  unless overX > 0. If overfling is allowed, it will use minX as
     *                  a springback boundary.
     * @param maxX      Maximum X value. The scroller will not scroll past this point
     *                  unless overX > 0. If overfling is allowed, it will use maxX as
     *                  a springback boundary.
     * @param minY      Minimum Y value. The scroller will not scroll past this point
     *                  unless overY > 0. If overfling is allowed, it will use minY as
     *                  a springback boundary.
     * @param maxY      Maximum Y value. The scroller will not scroll past this point
     *                  unless overY > 0. If overfling is allowed, it will use maxY as
     *                  a springback boundary.
     * @param overX     Overfling range. If > 0, horizontal overfling in either
     *                  direction will be possible.
     * @param overY     Overfling range. If > 0, vertical overfling in either
     *                  direction will be possible.
     */
    public void fling(int startX, int startY, int velocityX, int velocityY,
                      int minX, int maxX, int minY, int maxY, int overX, int overY) {
        // Continue a scroll or fling in progress
        if (mFlywheel && !isFinished()) {
            float oldVelocityX = mScrollerX.mCurrVelocity;
            float oldVelocityY = mScrollerY.mCurrVelocity;
            if (Math.signum(velocityX) == Math.signum(oldVelocityX) &&
                    Math.signum(velocityY) == Math.signum(oldVelocityY)) {
                velocityX += oldVelocityX;
                velocityY += oldVelocityY;
            }
        }

        mFlingMode = true;
        mScrollerX.fling(startX, velocityX, minX, maxX, overX);
        mScrollerY.fling(startY, velocityY, minY, maxY, overY);
    }

    /**
     * Notify the scroller that we've reached a horizontal boundary.
     * Normally the information to handle this will already be known
     * when the animation is started, such as in a call to one of the
     * fling functions. However there are cases where this cannot be known
     * in advance. This function will transition the current motion and
     * animate from startX to finalX as appropriate.
     *
     * @param startX Starting/current X position
     * @param finalX Desired final X position
     * @param overX  Magnitude of overscroll allowed. This should be the maximum
     *               desired distance from finalX. Absolute value - must be positive.
     */
    public void notifyHorizontalEdgeReached(int startX, int finalX, int overX) {
        mScrollerX.notifyEdgeReached(startX, finalX, overX);
    }

    /**
     * Notify the scroller that we've reached a vertical boundary.
     * Normally the information to handle this will already be known
     * when the animation is started, such as in a call to one of the
     * fling functions. However there are cases where this cannot be known
     * in advance. This function will animate a parabolic motion from
     * startY to finalY.
     *
     * @param startY Starting/current Y position
     * @param finalY Desired final Y position
     * @param overY  Magnitude of overscroll allowed. This should be the maximum
     *               desired distance from finalY. Absolute value - must be positive.
     */
    public void notifyVerticalEdgeReached(int startY, int finalY, int overY) {
        mScrollerY.notifyEdgeReached(startY, finalY, overY);
    }

    /**
     * Returns whether the current Scroller is currently returning to a valid position.
     * Valid bounds were provided by the
     * {@link #fling(int, int, int, int, int, int, int, int, int, int)} method.
     * <p>
     * One should check this value before calling
     * {@link #startScroll(int, int, int, int)} as the interpolation currently in progress
     * to restore a valid position will then be stopped. The caller has to take into account
     * the fact that the started scroll will start from an overscrolled position.
     *
     * @return true when the current position is overscrolled and in the process of
     * interpolating back to a valid value.
     */
    public boolean isOverScrolled() {
        return ((!mScrollerX.mFinished &&
                mScrollerX.mState != SplineScroller.SPLINE) ||
                (!mScrollerY.mFinished &&
                        mScrollerY.mState != SplineScroller.SPLINE));
    }

    /**
     * Stops the animation. Contrary to {@link #forceFinished(boolean)},
     * aborting the animating causes the scroller to move to the final x and y
     * positions.
     *
     * @see #forceFinished(boolean)
     */
    public void abortAnimation() {
        mScrollerX.finish();
        mScrollerY.finish();
    }

    /**
     * Returns the time elapsed since the beginning of the scrolling.
     *
     * @return The elapsed time in milliseconds.
     */
    public int timePassed() {
        final long time = AnimationHandler.currentTimeMillis();
        final long startTime = Math.min(mScrollerX.mStartTime, mScrollerY.mStartTime);
        return (int) (time - startTime);
    }

    public boolean isScrollingInDirection(float xvel, float yvel) {
        final int dx = mScrollerX.mFinal - mScrollerX.mStart;
        final int dy = mScrollerY.mFinal - mScrollerY.mStart;
        return !isFinished() && Math.signum(xvel) == Math.signum(dx) &&
                Math.signum(yvel) == Math.signum(dy);
    }

    private static class SplineScroller {

        private static final float SCROLL_FRICTION = 0.015f;
        private static final float PHYSICAL_COEFF = 51890.202f;

        private static final float GRAVITY = 2000.0f;

        private static float getDeceleration(int velocity) {
            return velocity > 0 ? -GRAVITY : GRAVITY;
        }

        private static final float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));
        private static final float INFLEXION = 0.35f; // Tension lines cross at (INFLEXION, 1)
        private static final float START_TENSION = 0.5f;
        private static final float END_TENSION = 1.0f;
        private static final float P1 = START_TENSION * INFLEXION;
        private static final float P2 = 1.0f - END_TENSION * (1.0f - INFLEXION);

        private static final int NB_SAMPLES = 100;
        private static final float[] SPLINE_POSITION = new float[NB_SAMPLES + 1];
        private static final float[] SPLINE_TIME = new float[NB_SAMPLES + 1];

        private static final int SPLINE = 0;
        private static final int CUBIC = 1;
        private static final int BALLISTIC = 2;

        static {
            float x_min = 0.0f;
            float y_min = 0.0f;
            for (int i = 0; i < NB_SAMPLES; i++) {
                final float alpha = (float) i / NB_SAMPLES;

                float x_max = 1.0f;
                float x, tx, coef;
                while (true) {
                    x = x_min + (x_max - x_min) / 2.0f;
                    coef = 3.0f * x * (1.0f - x);
                    tx = coef * ((1.0f - x) * P1 + x * P2) + x * x * x;
                    if (Math.abs(tx - alpha) < 1E-5) break;
                    if (tx > alpha) x_max = x;
                    else x_min = x;
                }
                SPLINE_POSITION[i] = coef * ((1.0f - x) * START_TENSION + x) + x * x * x;

                float y_max = 1.0f;
                float y, dy;
                while (true) {
                    y = y_min + (y_max - y_min) / 2.0f;
                    coef = 3.0f * y * (1.0f - y);
                    dy = coef * ((1.0f - y) * START_TENSION + y) + y * y * y;
                    if (Math.abs(dy - alpha) < 1E-5) break;
                    if (dy > alpha) y_max = y;
                    else y_min = y;
                }
                SPLINE_TIME[i] = coef * ((1.0f - y) * P1 + y * P2) + y * y * y;
            }
            SPLINE_POSITION[NB_SAMPLES] = SPLINE_TIME[NB_SAMPLES] = 1.0f;
        }

        // Initial position
        private int mStart;

        // Current position
        private int mCurrentPosition;

        // Final position
        private int mFinal;

        // Initial velocity
        private int mVelocity;

        // Current velocity
        private float mCurrVelocity;

        // Constant current deceleration
        private float mDeceleration;

        // Animation starting time, in system milliseconds
        private long mStartTime;

        // Animation duration, in milliseconds
        private int mDuration;

        // Duration to complete spline component of animation
        private int mSplineDuration;

        // Distance to travel along spline animation
        private int mSplineDistance;

        // Whether the animation is currently in progress
        private boolean mFinished = true;

        // The allowed overshot distance before boundary is reached.
        private int mOver;

        // Fling friction
        private float mFlingFriction = SCROLL_FRICTION;

        // Current state of the animation.
        private int mState = SPLINE;

        private SplineScroller() {
        }

        void updateScroll(float q) {
            mCurrentPosition = mStart + Math.round(q * (mFinal - mStart));
        }

        void setFriction(float friction) {
            mFlingFriction = friction;
        }

        /*
         * Modifies mDuration to the duration it takes to get from start to newFinal using the
         * spline interpolation. The previous duration was needed to get to oldFinal.
         */
        private void adjustDuration(int start, int oldFinal, int newFinal) {
            final int oldDistance = oldFinal - start;
            final int newDistance = newFinal - start;
            final float x = Math.abs((float) newDistance / oldDistance);
            final int index = (int) (NB_SAMPLES * x);
            if (index < NB_SAMPLES) {
                final float x_inf = (float) index / NB_SAMPLES;
                final float x_sup = (float) (index + 1) / NB_SAMPLES;
                final float t_inf = SPLINE_TIME[index];
                final float t_sup = SPLINE_TIME[index + 1];
                final float timeCoef = t_inf + (x - x_inf) / (x_sup - x_inf) * (t_sup - t_inf);
                mDuration *= timeCoef;
            }
        }

        void startScroll(int start, int distance, int duration) {
            mFinished = false;

            mCurrentPosition = mStart = start;
            mFinal = start + distance;

            mStartTime = AnimationHandler.currentTimeMillis();
            mDuration = duration;

            // Unused
            mDeceleration = 0.0f;
            mVelocity = 0;
        }

        void finish() {
            mCurrentPosition = mFinal;
            mCurrVelocity = 0.0f;
            mFinished = true;
        }

        boolean springback(int start, int min, int max) {
            mFinished = true;

            mCurrentPosition = mStart = mFinal = start;
            mVelocity = 0;

            mStartTime = AnimationHandler.currentTimeMillis();
            mDuration = 0;

            if (start < min) {
                startSpringback(start, min, 0);
            } else if (start > max) {
                startSpringback(start, max, 0);
            }

            return !mFinished;
        }

        private void startSpringback(int start, int end, int velocity) {
            // mStartTime has been set
            mFinished = false;
            mState = CUBIC;
            mCurrentPosition = mStart = start;
            mFinal = end;
            final int delta = start - end;
            mDeceleration = getDeceleration(delta);
            // TODO take velocity into account
            mVelocity = -delta; // only sign is used
            mOver = Math.abs(delta);
            mDuration = (int) (1000.0 * Math.sqrt(-2.0 * delta / mDeceleration));
        }

        void fling(int start, int velocity, int min, int max, int over) {
            mOver = over;
            mFinished = false;
            mCurrVelocity = mVelocity = velocity;
            mDuration = mSplineDuration = 0;
            mStartTime = AnimationHandler.currentTimeMillis();
            mCurrentPosition = mStart = start;

            if (start > max || start < min) {
                startAfterEdge(start, min, max, velocity);
                return;
            }

            mState = SPLINE;
            double totalDistance = 0.0;

            if (velocity != 0) {
                mDuration = mSplineDuration = getSplineFlingDuration(velocity);
                totalDistance = getSplineFlingDistance(velocity);
            }

            mSplineDistance = (int) (totalDistance * Math.signum(velocity));
            mFinal = start + mSplineDistance;

            // Clamp to a valid final position
            if (mFinal < min) {
                adjustDuration(mStart, mFinal, min);
                mFinal = min;
            }

            if (mFinal > max) {
                adjustDuration(mStart, mFinal, max);
                mFinal = max;
            }
        }

        private double getSplineDeceleration(int velocity) {
            return Math.log(INFLEXION * Math.abs(velocity) / (mFlingFriction * PHYSICAL_COEFF));
        }

        private double getSplineFlingDistance(int velocity) {
            final double l = getSplineDeceleration(velocity);
            final double decelMinusOne = DECELERATION_RATE - 1.0;
            return mFlingFriction * PHYSICAL_COEFF * Math.exp(DECELERATION_RATE / decelMinusOne * l);
        }

        /* Returns the duration, expressed in milliseconds */
        private int getSplineFlingDuration(int velocity) {
            final double l = getSplineDeceleration(velocity);
            final double decelMinusOne = DECELERATION_RATE - 1.0;
            return (int) (1000.0 * Math.exp(l / decelMinusOne));
        }

        private void fitOnBounceCurve(int start, int end, int velocity) {
            // Simulate a bounce that started from edge
            final float durationToApex = -velocity / mDeceleration;
            // The float cast below is necessary to avoid integer overflow.
            final float velocitySquared = (float) velocity * velocity;
            final float distanceToApex = velocitySquared / 2.0f / Math.abs(mDeceleration);
            final float distanceToEdge = Math.abs(end - start);
            final float totalDuration = (float) Math.sqrt(
                    2.0 * (distanceToApex + distanceToEdge) / Math.abs(mDeceleration));
            mStartTime -= (int) (1000.0f * (totalDuration - durationToApex));
            mCurrentPosition = mStart = end;
            mVelocity = (int) (-mDeceleration * totalDuration);
        }

        private void startBounceAfterEdge(int start, int end, int velocity) {
            mDeceleration = getDeceleration(velocity == 0 ? start - end : velocity);
            fitOnBounceCurve(start, end, velocity);
            onEdgeReached();
        }

        private void startAfterEdge(int start, int min, int max, int velocity) {
            if (start > min && start < max) {
                mFinished = true;
                return;
            }
            final boolean positive = start > max;
            final int edge = positive ? max : min;
            final int overDistance = start - edge;
            boolean keepIncreasing = overDistance * velocity >= 0;
            if (keepIncreasing) {
                // Will result in a bounce or a to_boundary depending on velocity.
                startBounceAfterEdge(start, edge, velocity);
            } else {
                final double totalDistance = getSplineFlingDistance(velocity);
                if (totalDistance > Math.abs(overDistance)) {
                    fling(start, velocity, positive ? min : start, positive ? start : max, mOver);
                } else {
                    startSpringback(start, edge, velocity);
                }
            }
        }

        void notifyEdgeReached(int start, int end, int over) {
            // mState is used to detect successive notifications 
            if (mState == SPLINE) {
                mOver = over;
                mStartTime = AnimationHandler.currentTimeMillis();
                // We were in fling/scroll mode before: current velocity is such that distance to
                // edge is increasing. This ensures that startAfterEdge will not start a new fling.
                startAfterEdge(start, end, end, (int) mCurrVelocity);
            }
        }

        private void onEdgeReached() {
            // mStart, mVelocity and mStartTime were adjusted to their values when edge was reached.
            // The float cast below is necessary to avoid integer overflow.
            final float velocitySquared = (float) mVelocity * mVelocity;
            float distance = velocitySquared / (2.0f * Math.abs(mDeceleration));
            final float sign = Math.signum(mVelocity);

            if (distance > mOver) {
                // Default deceleration is not sufficient to slow us down before boundary
                mDeceleration = -sign * velocitySquared / (2.0f * mOver);
                distance = mOver;
            }

            mOver = (int) distance;
            mState = BALLISTIC;
            mFinal = mStart + (int) (mVelocity > 0 ? distance : -distance);
            mDuration = -(int) (1000.0f * mVelocity / mDeceleration);
        }

        // inverted return
        boolean continueWhenFinished() {
            switch (mState) {
                case SPLINE:
                    // Duration from start to null velocity
                    if (mDuration < mSplineDuration) {
                        // If the animation was clamped, we reached the edge
                        mCurrentPosition = mStart = mFinal;
                        // TODO Better compute speed when edge was reached
                        mVelocity = (int) mCurrVelocity;
                        mDeceleration = getDeceleration(mVelocity);
                        mStartTime += mDuration;
                        onEdgeReached();
                    } else {
                        // Normal stop, no need to continue
                        return true;
                    }
                    break;
                case BALLISTIC:
                    mStartTime += mDuration;
                    startSpringback(mFinal, mStart, 0);
                    break;
                case CUBIC:
                    return true;
            }

            update();
            return false;
        }

        /*
         * Update the current position and velocity for current time. Returns
         * true if update has been done and false if animation duration has been
         * reached.
         */
        boolean update() {
            final long time = AnimationHandler.currentTimeMillis();
            final long currentTime = time - mStartTime;

            if (currentTime == 0) {
                // Skip work but report that we're still going if we have a nonzero duration.
                return mDuration > 0;
            }
            if (currentTime > mDuration) {
                return false;
            }

            double distance = 0.0;
            switch (mState) {
                case SPLINE: {
                    final float t = (float) currentTime / mSplineDuration;
                    final int index = (int) (NB_SAMPLES * t);
                    float distanceCoef = 1.f;
                    float velocityCoef = 0.f;
                    if (index < NB_SAMPLES) {
                        final float t_inf = (float) index / NB_SAMPLES;
                        final float t_sup = (float) (index + 1) / NB_SAMPLES;
                        final float d_inf = SPLINE_POSITION[index];
                        final float d_sup = SPLINE_POSITION[index + 1];
                        velocityCoef = (d_sup - d_inf) / (t_sup - t_inf);
                        distanceCoef = d_inf + (t - t_inf) * velocityCoef;
                    }

                    distance = distanceCoef * mSplineDistance;
                    mCurrVelocity = velocityCoef * mSplineDistance / mSplineDuration * 1000.0f;
                    break;
                }

                case BALLISTIC: {
                    final float t = currentTime / 1000.0f;
                    mCurrVelocity = mVelocity + mDeceleration * t;
                    distance = mVelocity * t + mDeceleration * t * t / 2.0f;
                    break;
                }

                case CUBIC: {
                    final float t = (float) (currentTime) / mDuration;
                    final float t2 = t * t;
                    final float sign = Math.signum(mVelocity);
                    distance = sign * mOver * (3.0f * t2 - 2.0f * t * t2);
                    mCurrVelocity = sign * mOver * 6.0f * (-t + t2);
                    break;
                }
            }

            mCurrentPosition = mStart + (int) Math.round(distance);

            return true;
        }
    }
}
