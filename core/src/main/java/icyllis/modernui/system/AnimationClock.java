/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.system;

import org.jetbrains.annotations.ApiStatus;

/**
 * Information about the current animation clock, the value is reported by
 * {@link icyllis.modernui.core.Choreographer}.
 *
 * @since 3.14
 */
public final class AnimationClock {

    private static final ThreadLocal<AnimationClock> sAnimationState
            = ThreadLocal.withInitial(AnimationClock::new);

    private boolean animationClockLocked;
    private long currentVsyncTimeMillis;
    private long lastReportedTimeMillis;

    private AnimationClock() {
    }

    /**
     * Locks AnimationClock{@link #currentAnimationTimeMillis()} to a fixed value for the current
     * thread.
     * <p>
     * Must be followed by a call to {@link #unlockAnimationClock()} to allow time to
     * progress. Failing to do this will result in stuck animations, scrolls, and flings.
     * <p>
     * Note that time is not allowed to "rewind" and must perpetually flow forward. So the
     * lock may fail if the time is in the past from a previously returned value, however
     * time will be frozen for the duration of the lock. The clock is a thread-local, so
     * ensure that this method, {@link #unlockAnimationClock()}, and
     * {@link #currentAnimationTimeMillis()} are all called on the same thread.
     *
     * @hidden
     */
    @ApiStatus.Internal
    public static void lockAnimationClock(long vsyncMillis) {
        var state = sAnimationState.get();
        state.animationClockLocked = true;
        state.currentVsyncTimeMillis = vsyncMillis;
    }

    /**
     * Frees the time lock set in place by {@link #lockAnimationClock(long)}. Must be called
     * to allow the animation clock to self-update.
     *
     * @hidden
     */
    @ApiStatus.Internal
    public static void unlockAnimationClock() {
        sAnimationState.get().animationClockLocked = false;
    }

    /**
     * Returns the current animation time in milliseconds used to update animations.
     * This value is updated and synced when a new frame started, it's different from
     * {@link SystemClock#uptimeMillis()} which gives you a real current time.
     *
     * @return the current animation time in milliseconds
     */
    public static long currentAnimationTimeMillis() {
        var state = sAnimationState.get();
        if (state.animationClockLocked) {
            // It's important that time never rewinds
            return Math.max(state.currentVsyncTimeMillis,
                    state.lastReportedTimeMillis);
        }
        state.lastReportedTimeMillis = SystemClock.uptimeMillis();
        return state.lastReportedTimeMillis;
    }
}
