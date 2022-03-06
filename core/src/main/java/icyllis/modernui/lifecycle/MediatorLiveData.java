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

import icyllis.modernui.annotation.CallSuper;
import icyllis.modernui.annotation.UiThread;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * {@link LiveData} subclass which may observe other {@code LiveData} objects and react on
 * {@code OnChanged} events from them.
 * <p>
 * This class correctly propagates its active/inactive states down to source {@code LiveData}
 * objects.
 * <p>
 * Consider the following scenario: we have 2 instances of {@code LiveData}, let's name them
 * {@code liveData1} and {@code liveData2}, and we want to merge their emissions in one object:
 * {@code liveDataMerger}. Then, {@code liveData1} and {@code liveData2} will become sources for
 * the {@code MediatorLiveData liveDataMerger} and every time {@code onChanged} callback
 * is called for either of them, we set a new value in {@code liveDataMerger}.
 *
 * <pre>{@code
 * LiveData<Integer> liveData1 = ...;
 * LiveData<Integer> liveData2 = ...;
 *
 * MediatorLiveData<Integer> liveDataMerger = new MediatorLiveData<>();
 * liveDataMerger.addSource(liveData1, liveDataMerger::setValue);
 * liveDataMerger.addSource(liveData2, liveDataMerger::setValue);
 * }</pre>
 * <p>
 * Let's consider that we only want 10 values emitted by {@code liveData1}, to be
 * merged in the {@code liveDataMerger}. Then, after 10 values, we can stop listening to {@code
 * liveData1} and remove it as a source.
 * <pre>{@code
 * liveDataMerger.addSource(liveData1, new Observer<>() {
 *      private int count = 1;
 *
 *      @Override
 *      public void onChanged(@Nullable Integer s) {
 *          count++;
 *          liveDataMerger.setValue(s);
 *          if (count > 10) {
 *              liveDataMerger.removeSource(liveData1);
 *          }
 *      }
 * });
 * }</pre>
 *
 * @param <T> The type of data hold by this instance
 */
public class MediatorLiveData<T> extends MutableLiveData<T> {

    private final SafeLinkedList<LiveData<?>, Source<?>> mSources = new SafeLinkedList<>();

    public MediatorLiveData() {
    }

    /**
     * Starts to listen the given {@code source} LiveData, {@code onChanged} observer will be called
     * when {@code source} value was changed.
     * <p>
     * {@code onChanged} callback will be called only when this {@code MediatorLiveData} is active.
     * <p> If the given LiveData is already added as a source but with a different Observer,
     * {@link IllegalArgumentException} will be thrown.
     *
     * @param source    the {@code LiveData} to listen to
     * @param onChanged The observer that will receive the events
     * @param <S>       The type of data hold by {@code source} LiveData
     */
    @UiThread
    public <S> void addSource(@Nonnull LiveData<S> source, @Nonnull Observer<? super S> onChanged) {
        Source<S> e = new Source<>(source, onChanged);
        Source<?> existing = mSources.putIfAbsent(e);
        if (existing != null && existing.mObserver != onChanged) {
            throw new IllegalArgumentException(
                    "This source was already added with the different observer");
        }
        if (existing != null) {
            return;
        }
        if (hasActiveObservers()) {
            e.plug();
        }
    }

    /**
     * Stops to listen the given {@code LiveData}.
     *
     * @param toRemote {@code LiveData} to stop to listen
     * @param <S>      the type of data hold by {@code source} LiveData
     */
    @UiThread
    public <S> void removeSource(@Nonnull LiveData<S> toRemote) {
        Source<?> source = mSources.remove(toRemote);
        if (source != null) {
            source.unplug();
        }
    }

    @CallSuper
    @Override
    protected void onActive() {
        for (Source<?> source : mSources) {
            source.plug();
        }
    }

    @CallSuper
    @Override
    protected void onInactive() {
        for (Source<?> source : mSources) {
            source.unplug();
        }
    }

    private static class Source<V> implements Observer<V>, Supplier<LiveData<?>> {

        final LiveData<V> mLiveData;
        final Observer<? super V> mObserver;
        int mVersion = START_VERSION;

        Source(LiveData<V> liveData, final Observer<? super V> observer) {
            mLiveData = liveData;
            mObserver = observer;
        }

        @Override
        public LiveData<?> get() {
            return mLiveData;
        }

        void plug() {
            mLiveData.observeForever(this);
        }

        void unplug() {
            mLiveData.removeObserver(this);
        }

        @Override
        public void onChanged(@Nullable V v) {
            if (mVersion != mLiveData.getVersion()) {
                mVersion = mLiveData.getVersion();
                mObserver.onChanged(v);
            }
        }
    }
}
