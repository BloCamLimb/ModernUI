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

package icyllis.modernui.view;

import icyllis.modernui.util.Pools;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;

/**
 * Helper for tracking the velocity of touch events, for implementing
 * flinging and other such gestures.
 * <p>
 * Use {@link #obtain()} to retrieve a new instance of the class when you are going
 * to begin tracking.  Put the motion events you receive into it with
 * {@link #addMovement(MotionEvent)}.  When you want to determine the velocity call
 * {@link #computeCurrentVelocity(int)} and then call {@link #getXVelocity()}
 * and {@link #getYVelocity()} to retrieve the velocity.
 */
@ApiStatus.Internal
public final class VelocityTracker {

    private static final Pools.Pool<VelocityTracker> sPool = Pools.newSynchronizedPool(2);

    private static final long ASSUME_POINTER_STOPPED_TIME = 40000000; // 40ms

    /**
     * Velocity Tracker Strategy: Invalid.
     *
     * @hide
     */
    public static final int VELOCITY_TRACKER_STRATEGY_DEFAULT = -1;

    /**
     * Velocity Tracker Strategy: Impulse.
     * Physical model of pushing an object.  Quality: VERY GOOD.
     * Works with duplicate coordinates, unclean finger liftoff.
     *
     * @hide
     */
    public static final int VELOCITY_TRACKER_STRATEGY_IMPULSE = 0;

    /**
     * Velocity Tracker Strategy: LSQ1.
     * 1st order least squares.  Quality: POOR.
     * Frequently under-fits the touch data especially when the finger accelerates
     * or changes direction.  Often underestimates velocity.  The direction
     * is overly influenced by historical touch points.
     *
     * @hide
     */
    public static final int VELOCITY_TRACKER_STRATEGY_LSQ1 = 1;

    /**
     * Velocity Tracker Strategy: LSQ2.
     * 2nd order least squares.  Quality: VERY GOOD.
     * Pretty much ideal, but can be confused by certain kinds of touch data,
     * particularly if the panel has a tendency to generate delayed,
     * duplicate or jittery touch coordinates when the finger is released.
     *
     * @hide
     */
    public static final int VELOCITY_TRACKER_STRATEGY_LSQ2 = 2;

    /**
     * Velocity Tracker Strategy: LSQ3.
     * 3rd order least squares.  Quality: UNUSABLE.
     * Frequently over-fits the touch data yielding wildly divergent estimates
     * of the velocity when the finger is released.
     *
     * @hide
     */
    public static final int VELOCITY_TRACKER_STRATEGY_LSQ3 = 3;

    /**
     * Velocity Tracker Strategy: WLSQ2_DELTA.
     * 2nd order weighted least squares, delta weighting.  Quality: EXPERIMENTAL
     *
     * @hide
     */
    public static final int VELOCITY_TRACKER_STRATEGY_WLSQ2_DELTA = 4;

    /**
     * Velocity Tracker Strategy: WLSQ2_CENTRAL.
     * 2nd order weighted least squares, central weighting.  Quality: EXPERIMENTAL
     *
     * @hide
     */
    public static final int VELOCITY_TRACKER_STRATEGY_WLSQ2_CENTRAL = 5;

    /**
     * Velocity Tracker Strategy: WLSQ2_RECENT.
     * 2nd order weighted least squares, recent weighting.  Quality: EXPERIMENTAL
     *
     * @hide
     */
    public static final int VELOCITY_TRACKER_STRATEGY_WLSQ2_RECENT = 6;

    private final int mStrategyId;

    private long mLastEventTime;
    private float mVelocityX;
    private float mVelocityY;
    private final Strategy mStrategy;
    private final Estimator mEstimator = new Estimator();

    private VelocityTracker(int strategy) {
        mStrategyId = strategy;
        if (strategy == VELOCITY_TRACKER_STRATEGY_DEFAULT) {
            strategy = VELOCITY_TRACKER_STRATEGY_LSQ2;
        }
        mStrategy = switch (strategy) {
            case VELOCITY_TRACKER_STRATEGY_IMPULSE -> new ImpulseStrategy();
            case VELOCITY_TRACKER_STRATEGY_LSQ1 -> new LeastSquaresStrategy(1);
            case VELOCITY_TRACKER_STRATEGY_LSQ2 -> new LeastSquaresStrategy(2);
            case VELOCITY_TRACKER_STRATEGY_LSQ3 -> new LeastSquaresStrategy(3);
            case VELOCITY_TRACKER_STRATEGY_WLSQ2_DELTA -> new LeastSquaresStrategy(2,
                    LeastSquaresStrategy.Weighting.WEIGHTING_DELTA);
            case VELOCITY_TRACKER_STRATEGY_WLSQ2_CENTRAL -> new LeastSquaresStrategy(2,
                    LeastSquaresStrategy.Weighting.WEIGHTING_CENTRAL);
            case VELOCITY_TRACKER_STRATEGY_WLSQ2_RECENT -> new LeastSquaresStrategy(2,
                    LeastSquaresStrategy.Weighting.WEIGHTING_RECENT);
            default -> throw new IllegalStateException("Unexpected value: " + strategy);
        };
    }

    /**
     * Retrieve a new VelocityTracker object to watch the velocity of a
     * motion.  Be sure to call {@link #recycle} when done.  You should
     * generally only maintain an active object while tracking a movement,
     * so that the VelocityTracker can be re-used elsewhere.
     *
     * @return Returns a new VelocityTracker.
     */
    @Nonnull
    public static VelocityTracker obtain() {
        VelocityTracker instance = sPool.acquire();
        return (instance != null) ? instance : new VelocityTracker(VELOCITY_TRACKER_STRATEGY_DEFAULT);
    }

    /**
     * Obtains a velocity tracker with the specified strategy.
     * For testing and comparison purposes only.
     *
     * @param strategy The strategy id, VELOCITY_TRACKER_STRATEGY_DEFAULT to use the default.
     * @return The velocity tracker.
     * @hide
     */
    @Nonnull
    public static VelocityTracker obtain(int strategy) {
        return new VelocityTracker(strategy);
    }

    /**
     * Return a VelocityTracker object back to be re-used by others.  You must
     * not touch the object after calling this function.
     */
    public void recycle() {
        if (mStrategyId == VELOCITY_TRACKER_STRATEGY_DEFAULT) {
            clear();
            sPool.release(this);
        }
    }

    /**
     * Return strategy id of VelocityTracker object.
     *
     * @return The velocity tracker strategy id.
     * @hide
     */
    public int getStrategyId() {
        return mStrategyId;
    }

    /**
     * Reset the velocity tracker back to its initial state.
     */
    public void clear() {
        mVelocityX = 0;
        mVelocityY = 0;
        mStrategy.clear();
        mEstimator.clear();
    }

    /**
     * Add a user's movement to the tracker.  You should call this for the
     * initial {@link MotionEvent#ACTION_DOWN}, the following
     * {@link MotionEvent#ACTION_MOVE} events that you receive, and the
     * final {@link MotionEvent#ACTION_UP}.  You can, however, call this
     * for whichever events you desire.
     *
     * @param event The MotionEvent you received and would like to track.
     */
    public void addMovement(@Nonnull MotionEvent event) {
        int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_HOVER_ENTER:
                // Clear all pointers on down before adding the new movement.
                clear();
                break;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_HOVER_MOVE:
                break;
            default:
                // Ignore all other actions because they do not convey any new information about
                // pointer movement.  We also want to preserve the last known velocity of the pointers.
                // Note that ACTION_UP and ACTION_POINTER_UP always report the last known position
                // of the pointers that went up.  ACTION_POINTER_UP does include the new position of
                // pointers that remained down, but we will also receive an ACTION_MOVE with this
                // information if any of them actually moved.  Since we don't know how many pointers
                // will be going up at once it makes sense to just wait for the following ACTION_MOVE
                // before adding the movement.
                return;
        }

        long eventTime = event.getEventTimeNano();

        if (eventTime - mLastEventTime >= ASSUME_POINTER_STOPPED_TIME) {
            // We have not received any movements for too long.  Assume that all pointers
            // have stopped.
            mStrategy.clear();
        }
        mLastEventTime = eventTime;

        mStrategy.addMovement(eventTime, event.getX(), event.getY());
    }

    /**
     * Equivalent to invoking {@link #computeCurrentVelocity(int, float)} with a maximum
     * velocity of Float.MAX_VALUE.
     *
     * @see #computeCurrentVelocity(int, float)
     */
    public boolean computeCurrentVelocity(int units) {
        if (mStrategy.getEstimator(mEstimator) && mEstimator.degree >= 1) {
            float vx = mEstimator.xCoeff[1];
            float vy = mEstimator.yCoeff[1];
            mVelocityX = vx * units / 1000;
            mVelocityY = vy * units / 1000;
            return true;
        } else {
            mVelocityX = 0;
            mVelocityY = 0;
            return false;
        }
    }

    /**
     * Compute the current velocity based on the points that have been
     * collected.  Only call this when you actually want to retrieve velocity
     * information, as it is relatively expensive.  You can then retrieve
     * the velocity with {@link #getXVelocity()} and
     * {@link #getYVelocity()}.
     *
     * @param units       The units you would like the velocity in.  A value of 1
     *                    provides pixels per millisecond, 1000 provides pixels per second, etc.
     * @param maxVelocity The maximum velocity that can be computed by this method.
     *                    This value must be declared in the same unit as the units parameter. This value
     *                    must be positive.
     */
    public boolean computeCurrentVelocity(int units, float maxVelocity) {
        if (mStrategy.getEstimator(mEstimator) && mEstimator.degree >= 1) {
            float vx = mEstimator.xCoeff[1];
            float vy = mEstimator.yCoeff[1];
            mVelocityX = Math.max(Math.min(vx * units / 1000, maxVelocity), -maxVelocity);
            mVelocityY = Math.max(Math.min(vy * units / 1000, maxVelocity), -maxVelocity);
            return true;
        } else {
            mVelocityX = 0;
            mVelocityY = 0;
            return false;
        }
    }

    /**
     * Retrieve the last computed X velocity.  You must first call
     * {@link #computeCurrentVelocity(int)} before calling this function.
     *
     * @return The previously computed X velocity.
     */
    public float getXVelocity() {
        return mVelocityX;
    }

    /**
     * Retrieve the last computed Y velocity.  You must first call
     * {@link #computeCurrentVelocity(int)} before calling this function.
     *
     * @return The previously computed Y velocity.
     */
    public float getYVelocity() {
        return mVelocityY;
    }

    /**
     * Get an estimator for the movements of a pointer using past movements of the
     * pointer to predict future movements.
     * <p>
     * It is not necessary to call {@link #computeCurrentVelocity(int)} before calling
     * this method.
     *
     * @param outEstimator The estimator to populate.
     * @return True if an estimator was obtained, false if there is no information
     * available about the pointer.
     */
    @ApiStatus.Internal
    public boolean getEstimator(@Nonnull Estimator outEstimator) {
        return mStrategy.getEstimator(outEstimator);
    }

    /**
     * Implements a particular velocity tracker algorithm.
     */
    public interface Strategy {

        void clear();

        void addMovement(long eventTime, float x, float y);

        boolean getEstimator(@Nonnull Estimator outEstimator);
    }

    static abstract class CommonStrategy implements Strategy {

        // Sample horizon.
        // We don't use too much history by default since we want to react to quick
        // changes in direction.
        static final int HORIZON = 100000000; // 100 ms

        // Number of samples to keep.
        static final int HISTORY_SIZE = 20;

        int mIndex;
        final long[] mEventTime = new long[HISTORY_SIZE];
        final float[] mX = new float[HISTORY_SIZE];
        final float[] mY = new float[HISTORY_SIZE];

        final float[] mTmpX = new float[HISTORY_SIZE];
        final float[] mTmpY = new float[HISTORY_SIZE];

        CommonStrategy() {
            clear();
        }

        @Override
        public void clear() {
            mIndex = 0;
        }

        @Override
        public void addMovement(long eventTime, float x, float y) {
            if (mEventTime[mIndex] != eventTime) {
                // When ACTION_POINTER_DOWN happens, we will first receive ACTION_MOVE with the coordinates
                // of the existing pointers, and then ACTION_POINTER_DOWN with the coordinates that include
                // the new pointer. If the eventtimes for both events are identical, just update the data
                // for this time.
                // We only compare against the last value, as it is likely that addMovement is called
                // in chronological order as events occur.
                mIndex++;
            }
            if (mIndex == HISTORY_SIZE) {
                mIndex = 0;
            }

            mEventTime[mIndex] = eventTime;
            mX[mIndex] = x;
            mY[mIndex] = y;
        }
    }

    public static class ImpulseStrategy extends CommonStrategy {

        final long[] mTmpTime = new long[HISTORY_SIZE];

        public ImpulseStrategy() {
        }

        /**
         * Calculate the total impulse provided to the screen and the resulting velocity.
         * <p>
         * The touchscreen is modeled as a physical object.
         * Initial condition is discussed below, but for now suppose that v(t=0) = 0
         * <p>
         * The kinetic energy of the object at the release is E=0.5*m*v^2
         * Then vfinal = sqrt(2E/m). The goal is to calculate E.
         * <p>
         * The kinetic energy at the release is equal to the total work done on the object by the finger.
         * The total work W is the sum of all dW along the path.
         * <p>
         * dW = F*dx, where dx is the piece of path traveled.
         * Force is change of momentum over time, F = dp/dt = m dv/dt.
         * Then substituting:
         * dW = m (dv/dt) * dx = m * v * dv
         * <p>
         * Summing along the path, we get:
         * W = sum(dW) = sum(m * v * dv) = m * sum(v * dv)
         * Since the mass stays constant, the equation for final velocity is:
         * vfinal = sqrt(2*sum(v * dv))
         * <p>
         * Here,
         * dv : change of velocity = (v[i+1]-v[i])
         * dx : change of distance = (x[i+1]-x[i])
         * dt : change of time = (t[i+1]-t[i])
         * v : instantaneous velocity = dx/dt
         * <p>
         * The final formula is:
         * vfinal = sqrt(2) * sqrt(sum((v[i]-v[i-1])*|v[i]|)) for all i
         * The absolute value is needed to properly account for the sign. If the velocity over a
         * particular segment descreases, then this indicates braking, which means that negative
         * work was done. So for two positive, but decreasing, velocities, this contribution would be
         * negative and will cause a smaller final velocity.
         * <p>
         * Initial condition
         * There are two ways to deal with initial condition:
         * 1) Assume that v(0) = 0, which would mean that the screen is initially at rest.
         * This is not entirely accurate. We are only taking the past X ms of touch data, where X is
         * currently equal to 100. However, a touch event that created a fling probably lasted for longer
         * than that, which would mean that the user has already been interacting with the touchscreen
         * and it has probably already been moving.
         * 2) Assume that the touchscreen has already been moving at a certain velocity, calculate this
         * initial velocity and the equivalent energy, and start with this initial energy.
         * Consider an example where we have the following data, consisting of 3 points:
         * time: t0, t1, t2
         * x   : x0, x1, x2
         * v   : 0 , v1, v2
         * Here is what will happen in each of these scenarios:
         * 1) By directly applying the formula above with the v(0) = 0 boundary condition, we will get
         * vfinal = sqrt(2*(|v1|*(v1-v0) + |v2|*(v2-v1))). This can be simplified since v0=0
         * vfinal = sqrt(2*(|v1|*v1 + |v2|*(v2-v1))) = sqrt(2*(v1^2 + |v2|*(v2 - v1)))
         * since velocity is a real number
         * 2) If we treat the screen as already moving, then it must already have an energy (per mass)
         * equal to 1/2*v1^2. Then the initial energy should be 1/2*v1*2, and only the second segment
         * will contribute to the total kinetic energy (since we can effectively consider that v0=v1).
         * This will give the following expression for the final velocity:
         * vfinal = sqrt(2*(1/2*v1^2 + |v2|*(v2-v1)))
         * This analysis can be generalized to an arbitrary number of samples.
         * <p>
         * <p>
         * Comparing the two equations above, we see that the only mathematical difference
         * is the factor of 1/2 in front of the first velocity term.
         * This boundary condition would allow for the "proper" calculation of the case when all of the
         * samples are equally spaced in time and distance, which should suggest a constant velocity.
         * <p>
         * Note that approach 2) is sensitive to the proper ordering of the data in time, since
         * the boundary condition must be applied to the oldest sample to be accurate.
         */
        static float kineticEnergyToVelocity(float work) {
            return (float) ((work < 0 ? -1.0 : 1.0) * Math.sqrt(Math.abs(work)) * 1.41421356237);
        }

        static float calculateImpulseVelocity(long[] t, float[] v, int count) {
            // The input should be in reversed time order (most recent sample at index i=0)
            // t[i] is in nanoseconds, but due to FP arithmetic, convert to seconds inside this function

            if (count < 2) {
                return 0; // if 0 or 1 points, velocity is zero
            }
            assert t[1] <= t[0];
            if (count == 2) { // if 2 points, basic linear calculation
                if (t[1] == t[0]) {
                    return 0;
                }
                return (v[1] - v[0]) / (1E-9F * (t[1] - t[0]));
            }
            // Guaranteed to have at least 3 points here
            float work = 0;
            for (int i = count - 1; i > 0; i--) { // start with the oldest sample and go forward in time
                if (t[i] == t[i - 1]) {
                    continue;
                }
                float vPrev = kineticEnergyToVelocity(work); // v[i-1]
                float vCurr = (v[i] - v[i - 1]) / (1E-9F * (t[i] - t[i - 1])); // v[i]
                work += (vCurr - vPrev) * Math.abs(vCurr);
                if (i == count - 1) {
                    work *= 0.5; // initial condition, case 2) above
                }
            }
            return kineticEnergyToVelocity(work);
        }

        @Override
        public boolean getEstimator(@Nonnull Estimator outEstimator) {
            outEstimator.clear();

            // Iterate over movement samples in reverse time order and collect samples.
            int m = 0; // number of points that will be used for fitting
            int index = mIndex;
            final long newestTime = mEventTime[mIndex];
            do {
                final long time = mEventTime[index];

                long age = newestTime - time;
                if (age > HORIZON) {
                    break;
                }

                mTmpX[m] = mX[index];
                mTmpY[m] = mY[index];
                mTmpTime[m] = time;
                index = (index == 0 ? HISTORY_SIZE : index) - 1;
            } while (++m < HISTORY_SIZE);

            if (m == 0) {
                return false; // no data
            }
            outEstimator.xCoeff[0] = 0;
            outEstimator.yCoeff[0] = 0;
            outEstimator.xCoeff[1] = calculateImpulseVelocity(mTmpTime, mTmpX, m);
            outEstimator.yCoeff[1] = calculateImpulseVelocity(mTmpTime, mTmpY, m);
            outEstimator.xCoeff[2] = 0;
            outEstimator.yCoeff[2] = 0;
            outEstimator.time = newestTime;
            outEstimator.degree = 2; // similar results to 2nd degree fit
            outEstimator.confidence = 1;
            return true;
        }
    }

    public static class LeastSquaresStrategy extends CommonStrategy {

        public enum Weighting {
            // No weights applied.  All data points are equally reliable.
            WEIGHTING_NONE,

            // Weight by time delta.  Data points clustered together are weighted less.
            WEIGHTING_DELTA,

            // Weight such that points within a certain horizon are weighed more than those
            // outside of that horizon.
            WEIGHTING_CENTRAL,

            // Weight such that points older than a certain amount are weighed less.
            WEIGHTING_RECENT
        }

        final int mDegree;
        final Weighting mWeighting;

        final float[] mTmpW = new float[HISTORY_SIZE];
        final float[] mTmpTime = new float[HISTORY_SIZE];

        final float[] mTmpXCoeff = new float[3];
        final float[] mTmpYCoeff = new float[3];

        public LeastSquaresStrategy(int degree) {
            this(degree, Weighting.WEIGHTING_NONE);
        }

        public LeastSquaresStrategy(int degree, Weighting weighting) {
            mDegree = degree;
            mWeighting = weighting;
        }

        static float vectorDot(float[] a, float[] b, int m) {
            float r = 0;
            for (int i = 0; i < m; i++) {
                r += a[i] * b[i];
            }
            return r;
        }

        static float vectorNorm(float[] a, int m) {
            float r = 0;
            for (int i = 0; i < m; i++) {
                float t = a[i];
                r += t * t;
            }
            return (float) Math.sqrt(r);
        }

        /**
         * Solves a linear least squares problem to obtain a N degree polynomial that fits
         * the specified input data as nearly as possible.
         * <p>
         * Returns true if a solution is found, false otherwise.
         * <p>
         * The input consists of two vectors of data points X and Y with indices 0..m-1
         * along with a weight vector W of the same size.
         * <p>
         * The output is a vector B with indices 0..n that describes a polynomial
         * that fits the data, such the sum of W[i] * W[i] * abs(Y[i] - (B[0] + B[1] X[i]
         * + B[2] X[i]^2 ... B[n] X[i]^n)) for all i between 0 and m-1 is minimized.
         * <p>
         * Accordingly, the weight vector W should be initialized by the caller with the
         * reciprocal square root of the variance of the error in each input data point.
         * In other words, an ideal choice for W would be W[i] = 1 / var(Y[i]) = 1 / stddev(Y[i]).
         * The weights express the relative importance of each data point.  If the weights are
         * all 1, then the data points are considered to be of equal importance when fitting
         * the polynomial.  It is a good idea to choose weights that diminish the importance
         * of data points that may have higher than usual error margins.
         * <p>
         * Errors among data points are assumed to be independent.  W is represented here
         * as a vector although in the literature it is typically taken to be a diagonal matrix.
         * <p>
         * That is to say, the function that generated the input data can be approximated
         * by v(t) ~= B[0] + B[1] t + B[2] t^2 + ... + B[n] t^n.
         * <p>
         * The coefficient of determination (R^2) is also returned to describe the goodness
         * of fit of the model for the given data.  It is a value between 0 and 1, where 1
         * indicates perfect correspondence.
         * <p>
         * This function first expands the X vector to a m by n matrix A such that
         * A[i][0] = 1, A[i][1] = X[i], A[i][2] = X[i]^2, ..., A[i][n] = X[i]^n, then
         * multiplies it by w[i]./
         * <p>
         * Then it calculates the QR decomposition of A yielding an m by m orthonormal matrix Q
         * and an m by n upper triangular matrix R.  Because R is upper triangular (lower
         * part is all zeroes), we can simplify the decomposition into an m by n matrix
         * Q1 and a n by n matrix R1 such that A = Q1 R1.
         * <p>
         * Finally we solve the system of linear equations given by R1 B = (Qtranspose W Y)
         * to find B.
         * <p>
         * For efficiency, we lay out A and Q column-wise in memory because we frequently
         * operate on the column vectors.  Conversely, we lay out R row-wise.
         * <p>
         * http://en.wikipedia.org/wiki/Numerical_methods_for_linear_least_squares
         * http://en.wikipedia.org/wiki/Gram-Schmidt
         */
        @ApiStatus.Experimental
        static float solveLeastSquares(float[] t, float[] v, float[] w, int m, int n, float[] outB) {
            // Expand the X vector to a matrix A, pre-multiplied by the weights.
            float[][] a = new float[n][m]; // column-major order
            for (int h = 0; h < m; h++) {
                a[0][h] = w[h];
                for (int i = 1; i < n; i++) {
                    a[i][h] = a[i - 1][h] * t[h];
                }
            }

            // Apply the Gram-Schmidt process to A to obtain its QR decomposition.
            float[][] q = new float[n][m]; // orthonormal basis, column-major order
            float[][] r = new float[n][n]; // upper triangular matrix, row-major order
            for (int j = 0; j < n; j++) {
                System.arraycopy(a[j], 0, q[j], 0, m);
                for (int i = 0; i < j; i++) {
                    float dot = vectorDot(q[j], q[i], m);
                    for (int h = 0; h < m; h++) {
                        q[j][h] -= dot * q[i][h];
                    }
                }

                float norm = vectorNorm(q[j], m);
                if (norm < 0.000001f) {
                    // vectors are linearly dependent or zero so no solution
                    return Float.NaN;
                }

                float invNorm = 1.0f / norm;
                for (int h = 0; h < m; h++) {
                    q[j][h] *= invNorm;
                }
                for (int i = 0; i < n; i++) {
                    r[j][i] = i < j ? 0 : vectorDot(q[j], a[i], m);
                }
            }

            // Solve R B = Qt W Y to find B.  This is easy because R is upper triangular.
            // We just work from bottom-right to top-left calculating B's coefficients.
            float[] wy = new float[m];
            for (int h = 0; h < m; h++) {
                wy[h] = v[h] * w[h];
            }
            for (int i = n; i != 0; ) {
                i--;
                outB[i] = vectorDot(q[i], wy, m);
                for (int j = n - 1; j > i; j--) {
                    outB[i] -= r[i][j] * outB[j];
                }
                outB[i] /= r[i][i];
            }

            // Calculate the coefficient of determination as 1 - (SSerr / SStot) where
            // SSerr is the residual sum of squares (variance of the error),
            // and SStot is the total sum of squares (variance of the data) where each
            // has been weighted.
            float ymean = 0;
            for (int h = 0; h < m; h++) {
                ymean += v[h];
            }
            ymean /= m;

            float sserr = 0;
            float sstot = 0;
            for (int h = 0; h < m; h++) {
                float err = v[h] - outB[0];
                float term = 1;
                for (int i = 1; i < n; i++) {
                    term *= t[h];
                    err -= term * outB[i];
                }
                sserr += w[h] * w[h] * err * err;
                float var = v[h] - ymean;
                sstot += w[h] * w[h] * var * var;
            }
            return sstot > 0.000001f ? 1.0f - (sserr / sstot) : 1;
        }

        /**
         * Optimized unweighted second-order least squares fit. About 2x speed improvement compared to
         * the default implementation
         */
        static boolean solveUnweightedLeastSquaresDeg2(float[] t, float[] v, int count, float[] out) {
            // Solving y = a*x^2 + b*x + c
            float sxi = 0, sxiyi = 0, syi = 0, sxi2 = 0, sxi3 = 0, sxi2yi = 0, sxi4 = 0;

            for (int i = 0; i < count; i++) {
                float xi = t[i];
                float yi = v[i];
                float xi2 = xi * xi;
                float xi3 = xi2 * xi;
                float xi4 = xi3 * xi;
                float xiyi = xi * yi;
                float xi2yi = xi2 * yi;

                sxi += xi;
                sxi2 += xi2;
                sxiyi += xiyi;
                sxi2yi += xi2yi;
                syi += yi;
                sxi3 += xi3;
                sxi4 += xi4;
            }

            float Sxx = sxi2 - sxi * sxi / count;
            float Sxy = sxiyi - sxi * syi / count;
            float Sxx2 = sxi3 - sxi * sxi2 / count;
            float Sx2y = sxi2yi - sxi2 * syi / count;
            float Sx2x2 = sxi4 - sxi2 * sxi2 / count;

            float denominator = Sxx * Sx2x2 - Sxx2 * Sxx2;
            if (denominator == 0) {
                return false;
            }
            // Compute a
            float numerator = Sx2y * Sxx - Sxy * Sxx2;
            float a = numerator / denominator;

            // Compute b
            numerator = Sxy * Sx2x2 - Sx2y * Sxx2;
            float b = numerator / denominator;

            // Compute c
            float c = syi / count - b * sxi / count - a * sxi2 / count;

            out[0] = c;
            out[1] = b;
            out[2] = a;
            return true;
        }

        @Override
        public boolean getEstimator(@Nonnull Estimator outEstimator) {
            outEstimator.clear();

            // Iterate over movement samples in reverse time order and collect samples.
            int m = 0; // number of points that will be used for fitting
            int index = mIndex;
            final long newestTime = mEventTime[mIndex];
            do {
                final long time = mEventTime[index];

                long age = newestTime - time;
                if (age > HORIZON) {
                    break;
                }

                mTmpX[m] = mX[index];
                mTmpY[m] = mY[index];
                mTmpW[m] = chooseWeight(index);
                mTmpTime[m] = -age * 0.000000001f;
                index = (index == 0 ? HISTORY_SIZE : index) - 1;
            } while (++m < HISTORY_SIZE);

            if (m == 0) {
                return false; // no data
            }

            // Calculate a least squares polynomial fit.
            int degree = mDegree;
            if (degree > m - 1) {
                degree = m - 1;
            }

            if (degree == 2 && mWeighting == Weighting.WEIGHTING_NONE) {
                // Optimize unweighted, quadratic polynomial fit
                boolean xCoeff = solveUnweightedLeastSquaresDeg2(mTmpTime, mTmpX, m, mTmpXCoeff);
                boolean yCoeff = solveUnweightedLeastSquaresDeg2(mTmpTime, mTmpY, m, mTmpYCoeff);
                if (xCoeff && yCoeff) {
                    outEstimator.time = newestTime;
                    outEstimator.degree = 2;
                    outEstimator.confidence = 1;
                    for (int i = 0; i <= outEstimator.degree; i++) {
                        outEstimator.xCoeff[i] = mTmpXCoeff[i];
                        outEstimator.yCoeff[i] = mTmpYCoeff[i];
                    }
                    return true;
                }
            } else if (degree >= 1) {
                // General case for a Nth degree polynomial fit
                float xDet, yDet;
                int n = degree + 1;
                xDet = solveLeastSquares(mTmpTime, mTmpX, mTmpW, m, n, outEstimator.xCoeff);
                yDet = solveLeastSquares(mTmpTime, mTmpY, mTmpW, m, n, outEstimator.yCoeff);
                if (!Float.isNaN(xDet) && !Float.isNaN(yDet)) {
                    outEstimator.time = newestTime;
                    outEstimator.degree = degree;
                    outEstimator.confidence = xDet * yDet;
                    return true;
                }
            }

            // No velocity data available for this pointer, but we do have its current position.
            outEstimator.xCoeff[0] = mTmpX[0];
            outEstimator.yCoeff[0] = mTmpY[0];
            outEstimator.time = newestTime;
            outEstimator.degree = 0;
            outEstimator.confidence = 1;
            return true;
        }

        private float chooseWeight(int index) {
            return switch (mWeighting) {
                case WEIGHTING_DELTA -> {
                    // Weight points based on how much time elapsed between them and the next
                    // point so that points that "cover" a shorter time span are weighed less.
                    //   delta  0ms: 0.5
                    //   delta 10ms: 1.0
                    if (index == mIndex) {
                        yield 1.0f;
                    }
                    int nextIndex = (index + 1) % HISTORY_SIZE;
                    float deltaMillis = (mEventTime[nextIndex] - mEventTime[index]) * 0.000001f;
                    if (deltaMillis < 0) {
                        yield 0.5f;
                    }
                    if (deltaMillis < 10) {
                        yield 0.5f + deltaMillis * 0.05f;
                    }
                    yield 1.0f;
                }
                case WEIGHTING_CENTRAL -> {
                    // Weight points based on their age, weighing very recent and very old points less.
                    //   age  0ms: 0.5
                    //   age 10ms: 1.0
                    //   age 50ms: 1.0
                    //   age 60ms: 0.5
                    float ageMillis = (mEventTime[mIndex] - mEventTime[index]) * 0.000001f;
                    if (ageMillis < 0) {
                        yield 0.5f;
                    }
                    if (ageMillis < 10) {
                        yield 0.5f + ageMillis * 0.05f;
                    }
                    if (ageMillis < 50) {
                        yield 1.0f;
                    }
                    if (ageMillis < 60) {
                        yield 0.5f + (60 - ageMillis) * 0.05f;
                    }
                    yield 0.5f;
                }
                case WEIGHTING_RECENT -> {
                    // Weight points based on their age, weighing older points less.
                    //   age   0ms: 1.0
                    //   age  50ms: 1.0
                    //   age 100ms: 0.5
                    float ageMillis = (mEventTime[mIndex] - mEventTime[index]) * 0.000001f;
                    if (ageMillis < 50) {
                        yield 1.0f;
                    }
                    if (ageMillis < 100) {
                        yield 0.5f + (100 - ageMillis) * 0.01f;
                    }
                    yield 0.5f;
                }
                default -> 1.0f;
            };
        }
    }

    /**
     * An estimator for the movements of a pointer based on a polynomial model.
     * <p>
     * The last recorded position of the pointer is at time zero seconds.
     * Past estimated positions are at negative times and future estimated positions
     * are at positive times.
     * <p>
     * First coefficient is position (in pixels), second is velocity (in pixels per second),
     * third is acceleration (in pixels per second squared).
     */
    @ApiStatus.Internal
    public static final class Estimator {

        private static final int MAX_DEGREE = 4;

        /**
         * Estimator time base, in nanoseconds.
         */
        public long time;

        /**
         * Polynomial coefficients describing motion in X.
         */
        public final float[] xCoeff = new float[MAX_DEGREE + 1];

        /**
         * Polynomial coefficients describing motion in Y.
         */
        public final float[] yCoeff = new float[MAX_DEGREE + 1];

        /**
         * Polynomial degree, or zero if only position information is available.
         */
        public int degree;

        /**
         * Confidence (coefficient of determination), between 0 (no fit) and 1 (perfect fit).
         */
        public float confidence;

        /**
         * Gets an estimate of the X position of the pointer at the specified time point.
         *
         * @param time The time point in seconds, 0 is the last recorded time.
         * @return The estimated X coordinate.
         */
        public float estimateX(float time) {
            return estimate(time, xCoeff);
        }

        /**
         * Gets an estimate of the Y position of the pointer at the specified time point.
         *
         * @param time The time point in seconds, 0 is the last recorded time.
         * @return The estimated Y coordinate.
         */
        public float estimateY(float time) {
            return estimate(time, yCoeff);
        }

        /**
         * Gets the X coefficient with the specified index.
         *
         * @param index The index of the coefficient to return.
         * @return The X coefficient, or 0 if the index is greater than the degree.
         */
        public float getXCoeff(int index) {
            return index <= degree ? xCoeff[index] : 0;
        }

        /**
         * Gets the Y coefficient with the specified index.
         *
         * @param index The index of the coefficient to return.
         * @return The Y coefficient, or 0 if the index is greater than the degree.
         */
        public float getYCoeff(int index) {
            return index <= degree ? yCoeff[index] : 0;
        }

        public void clear() {
            time = 0;
            degree = 0;
            confidence = 0;
            for (int i = 0; i <= MAX_DEGREE; i++) {
                xCoeff[i] = 0;
                yCoeff[i] = 0;
            }
        }

        private float estimate(float time, float[] c) {
            float a = 0;
            float scale = 1;
            for (int i = 0; i <= degree; i++) {
                a += c[i] * scale;
                scale *= time;
            }
            return a;
        }
    }
}
