/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.IEventBus;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.function.Consumer;

/**
 * The Lifecycle of {@link icyllis.modernui.fragment.Fragment}
 */
public class Lifecycle {

    /**
     * The provider that owns this Lifecycle.
     * Only WeakReference on LifecycleOwner is kept, so if somebody leaks Lifecycle, they won't leak
     * the whole Fragment / Activity. However, to leak Lifecycle object isn't great idea neither,
     * because it keeps strong references on all other listeners, so you'll leak all of them as
     * well.
     */
    private final WeakReference<ILifecycleOwner> mLifecycleOwner;

    private final IEventBus mLifecycleEventBus = BusBuilder.builder()
            .setTrackPhases(false)
            .markerType(LifecycleEvent.class)
            .build();

    /**
     * Current state
     */
    private State mState = State.INITIALIZED;

    /**
     * Creates a new Lifecycle for the given provider.
     * <p>
     * You should usually create this inside your LifecycleOwner class's constructor and hold
     * onto the same instance.
     *
     * @param provider The owner LifecycleOwner
     */
    public Lifecycle(@Nonnull ILifecycleOwner provider) {
        mLifecycleOwner = new WeakReference<>(provider);
    }

    /**
     * Add a listener what will be called when LifecycleOwner changes state.
     *
     * @param listener the listener to call
     * @param <T>      event type
     */
    public <T extends LifecycleEvent> void addListener(Consumer<T> listener) {
        mLifecycleEventBus.addListener(listener);
    }

    public void registerObject(Object object) {
        mLifecycleEventBus.register(object);
    }

    public void unregisterObject(Object object) {
        mLifecycleEventBus.unregister(object);
    }

    /**
     * Moves the Lifecycle to the given state and dispatches necessary events to the listeners
     *
     * @param state new state
     */
    public void setCurrentState(@Nonnull State state) {
        moveToState(state);
    }

    /**
     * Sets the current state and notifies the listeners
     * <p>
     * Note that if the {@code currentState} is the same state as the last call to this method,
     * calling this method has no effect.
     *
     * @param type The event type that was received
     */
    public void handleLifecycleEvent(@Nonnull LifecycleEvent.Type type) {
        State next = getStateAfter(type);
        moveToState(next);
    }

    private void moveToState(@Nonnull State state) {
        if (mState == state) {
            return;
        }
        ILifecycleOwner lifecycleOwner = mLifecycleOwner.get();
        if (lifecycleOwner == null) {
            throw new IllegalStateException("LifecycleOwner of this Lifecycle is already garbage collected. " +
                    "It is too late to change lifecycle state.");
        }
        int diff;
        while ((diff = state.compareTo(mState)) != 0) {
            if (diff < 0)
                mLifecycleEventBus.post(downEvent(lifecycleOwner, mState));
            else
                mLifecycleEventBus.post(upEvent(lifecycleOwner, mState));
        }
    }

    @Nonnull
    private static State getStateAfter(@Nonnull LifecycleEvent.Type event) {
        switch (event) {
            case ON_CREATE:
            case ON_STOP:
                return State.CREATED;
            case ON_START:
            case ON_PAUSE:
                return State.STARTED;
            case ON_RESUME:
                return State.RESUMED;
            case ON_DESTROY:
                return State.DESTROYED;
        }
        throw new IllegalArgumentException("Unexpected event value " + event);
    }

    @Nonnull
    private LifecycleEvent downEvent(ILifecycleOwner owner, @Nonnull State state) {
        switch (state) {
            case INITIALIZED:
            case DESTROYED:
                throw new IllegalArgumentException();
            case CREATED:
                mState = State.DESTROYED;
                return new LifecycleEvent.Destroy(owner);
            case STARTED:
                mState = State.CREATED;
                return new LifecycleEvent.Stop(owner);
            case RESUMED:
                mState = State.STARTED;
                return new LifecycleEvent.Pause(owner);
        }
        throw new IllegalArgumentException("Unexpected state value " + state);
    }

    @Nonnull
    private LifecycleEvent upEvent(ILifecycleOwner owner, @Nonnull State state) {
        switch (state) {
            case INITIALIZED:
            case DESTROYED:
                mState = State.CREATED;
                return new LifecycleEvent.Create(owner);
            case CREATED:
                mState = State.STARTED;
                return new LifecycleEvent.Start(owner);
            case STARTED:
                mState = State.RESUMED;
                return new LifecycleEvent.Resume(owner);
            case RESUMED:
                throw new IllegalArgumentException();
        }
        throw new IllegalArgumentException("Unexpected state value " + state);
    }

    /**
     * Returns the current state of the Lifecycle.
     *
     * @return The current state of the Lifecycle.
     */
    @Nonnull
    public State getCurrentState() {
        return mState;
    }

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
         * @return true if this state is greater or equal to the given {@code state}
         */
        public boolean isAtLeast(@Nonnull State state) {
            return compareTo(state) >= 0;
        }
    }
}
