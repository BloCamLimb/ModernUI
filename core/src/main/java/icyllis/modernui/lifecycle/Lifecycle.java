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

package icyllis.modernui.lifecycle;

import icyllis.modernui.annotation.UiThread;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Defines an object that has a Lifecycle. {@link icyllis.modernui.fragment.Fragment}
 * class implement {@link LifecycleOwner} interface which has the
 * {@link LifecycleOwner#getLifecycle()} method to access the Lifecycle. You can also
 * implement {@link LifecycleOwner} in your own classes.
 * <p>
 * {@link Event#ON_CREATE}, {@link Event#ON_START}, {@link Event#ON_RESUME} events in this class
 * are dispatched <b>after</b> the {@link LifecycleOwner}'s related method returns.
 * {@link Event#ON_PAUSE}, {@link Event#ON_STOP}, {@link Event#ON_DESTROY} events in this class
 * are dispatched <b>before</b> the {@link LifecycleOwner}'s related method is called.
 * This gives you certain guarantees on which state the owner is in.
 * <p>
 * Observe lifecycle events with {@link LifecycleObserver}.
 * <pre>
 * class TestObserver implements LifecycleObserver {
 *     &#64;Override
 *     public void onCreate(&#64;Nonnull LifecycleOwner owner) {
 *         // your code
 *     }
 * }
 * </pre>
 */
public abstract class Lifecycle {

    /**
     * Adds a LifecycleObserver that will be notified when the LifecycleOwner changes
     * state.
     * <p>
     * The given observer will be brought to the current state of the LifecycleOwner.
     * For example, if the LifecycleOwner is in {@link State#STARTED} state, the given observer
     * will receive {@link Event#ON_CREATE}, {@link Event#ON_START} events.
     *
     * @param observer The observer to notify.
     */
    @UiThread
    public abstract void addObserver(@Nonnull LifecycleObserver observer);

    /**
     * Removes the given observer from the observers list.
     * <p>
     * If this method is called while a state change is being dispatched,
     * <ul>
     * <li>If the given observer has not yet received that event, it will not receive it.
     * <li>If the given observer has more than 1 method that observes the currently dispatched
     * event and at least one of them received the event, all of them will receive the event and
     * the removal will happen afterwards.
     * </ul>
     *
     * @param observer The observer to be removed.
     */
    @UiThread
    public abstract void removeObserver(@Nonnull LifecycleObserver observer);

    /**
     * Returns the current state of the Lifecycle.
     *
     * @return The current state of the Lifecycle.
     */
    @UiThread
    @Nonnull
    public abstract State getCurrentState();

    public enum Event {
        /**
         * Constant for onCreate event of the {@link LifecycleOwner}.
         */
        ON_CREATE,
        /**
         * Constant for onStart event of the {@link LifecycleOwner}.
         */
        ON_START,
        /**
         * Constant for onResume event of the {@link LifecycleOwner}.
         */
        ON_RESUME,
        /**
         * Constant for onPause event of the {@link LifecycleOwner}.
         */
        ON_PAUSE,
        /**
         * Constant for onStop event of the {@link LifecycleOwner}.
         */
        ON_STOP,
        /**
         * Constant for onDestroy event of the {@link LifecycleOwner}.
         */
        ON_DESTROY;

        /**
         * Returns the {@link Lifecycle.Event} that will be reported by a {@link Lifecycle}
         * leaving the specified {@link Lifecycle.State} to a lower state, or {@code null}
         * if there is no valid event that can move down from the given state.
         *
         * @param state the higher state that the returned event will transition down from
         * @return the event moving down the lifecycle phases from state
         */
        @Nullable
        public static Event downFrom(@Nonnull State state) {
            return switch (state) {
                case CREATED -> ON_DESTROY;
                case STARTED -> ON_STOP;
                case RESUMED -> ON_PAUSE;
                default -> null;
            };
        }

        /**
         * Returns the {@link Lifecycle.Event} that will be reported by a {@link Lifecycle}
         * entering the specified {@link Lifecycle.State} from a higher state, or {@code null}
         * if there is no valid event that can move down to the given state.
         *
         * @param state the lower state that the returned event will transition down to
         * @return the event moving down the lifecycle phases to state
         */
        @Nullable
        public static Event downTo(@Nonnull State state) {
            return switch (state) {
                case DESTROYED -> ON_DESTROY;
                case CREATED -> ON_STOP;
                case STARTED -> ON_PAUSE;
                default -> null;
            };
        }

        /**
         * Returns the {@link Lifecycle.Event} that will be reported by a {@link Lifecycle}
         * leaving the specified {@link Lifecycle.State} to a higher state, or {@code null}
         * if there is no valid event that can move up from the given state.
         *
         * @param state the lower state that the returned event will transition up from
         * @return the event moving up the lifecycle phases from state
         */
        @Nullable
        public static Event upFrom(@Nonnull State state) {
            return switch (state) {
                case INITIALIZED -> ON_CREATE;
                case CREATED -> ON_START;
                case STARTED -> ON_RESUME;
                default -> null;
            };
        }

        /**
         * Returns the {@link Lifecycle.Event} that will be reported by a {@link Lifecycle}
         * entering the specified {@link Lifecycle.State} from a lower state, or {@code null}
         * if there is no valid event that can move up to the given state.
         *
         * @param state the higher state that the returned event will transition up to
         * @return the event moving up the lifecycle phases to state
         */
        @Nullable
        public static Event upTo(@Nonnull State state) {
            return switch (state) {
                case CREATED -> ON_CREATE;
                case STARTED -> ON_START;
                case RESUMED -> ON_RESUME;
                default -> null;
            };
        }

        /**
         * Returns the new {@link Lifecycle.State} of a {@link Lifecycle} that just reported
         * this {@link Lifecycle.Event}.
         *
         * @return the state that will result from this event
         */
        @Nonnull
        public State getTargetState() {
            return switch (this) {
                case ON_CREATE, ON_STOP -> State.CREATED;
                case ON_START, ON_PAUSE -> State.STARTED;
                case ON_RESUME -> State.RESUMED;
                case ON_DESTROY -> State.DESTROYED;
            };
        }
    }

    /**
     * Lifecycle states. You can consider the states as the nodes in a graph and
     * {@link Event}s as the edges between these nodes.
     */
    public enum State {
        /**
         * Destroyed state for a LifecycleOwner. After this event, this Lifecycle will not dispatch
         * any more events.
         */
        DESTROYED,

        /**
         * Initialized state for a LifecycleOwner.
         */
        INITIALIZED,

        /**
         * Created state for a LifecycleOwner.
         */
        CREATED,

        /**
         * Started state for a LifecycleOwner.
         */
        STARTED,

        /**
         * Resumed state for a LifecycleOwner.
         */
        RESUMED;

        /**
         * Compares if this State is greater or equal to the given {@code state}.
         *
         * @param state State to compare with
         * @return true if this State is greater or equal to the given {@code state}
         */
        public boolean isAtLeast(@Nonnull State state) {
            return compareTo(state) >= 0;
        }
    }
}
