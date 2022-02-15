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

package icyllis.modernui.animation;

import icyllis.modernui.core.ArchCore;
import org.jetbrains.annotations.ApiStatus;

/**
 * Defines common utilities for working with animations.
 */
public final class AnimationUtils {

    private static class AnimationState {
        boolean animationClockLocked;
        long currentVsyncTimeMillis;
        long lastReportedTimeMillis;
    }

    private static final ThreadLocal<AnimationState> sAnimationState = ThreadLocal.withInitial(AnimationState::new);

    /**
     * Locks AnimationUtils{@link #currentAnimationTimeMillis()} to a fixed value for the current
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
     */
    @ApiStatus.Internal
    public static void lockAnimationClock(long vsyncMillis) {
        AnimationState state = sAnimationState.get();
        state.animationClockLocked = true;
        state.currentVsyncTimeMillis = vsyncMillis;
    }

    /**
     * Frees the time lock set in place by {@link #lockAnimationClock(long)}. Must be called
     * to allow the animation clock to self-update.
     */
    @ApiStatus.Internal
    public static void unlockAnimationClock() {
        sAnimationState.get().animationClockLocked = false;
    }

    /**
     * Returns the current animation time in milliseconds used to update animations.
     * This value is updated and synced when a new frame started, it's different from
     * {@link ArchCore#timeMillis()} which gives you a real current time.
     *
     * @return the current animation time in milliseconds
     */
    public static long currentAnimationTimeMillis() {
        AnimationState state = sAnimationState.get();
        if (state.animationClockLocked) {
            // It's important that time never rewinds
            return Math.max(state.currentVsyncTimeMillis,
                    state.lastReportedTimeMillis);
        }
        state.lastReportedTimeMillis = ArchCore.timeMillis();
        return state.lastReportedTimeMillis;
    }

    private AnimationUtils() {
    }
}
