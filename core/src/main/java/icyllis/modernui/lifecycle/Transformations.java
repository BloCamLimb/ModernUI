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

package icyllis.modernui.lifecycle;

import icyllis.modernui.annotation.UiThread;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Function;

/**
 * Transformation methods for {@link LiveData}.
 * <p>
 * These methods permit functional composition and delegation of {@link LiveData} instances. The
 * transformations are calculated lazily, and will run only when the returned {@link LiveData} is
 * observed. Lifecycle behavior is propagated from the input {@code source} {@link LiveData} to the
 * returned one.
 */
public final class Transformations {

    private Transformations() {
    }

    /**
     * Returns a {@code LiveData} mapped from the input {@code source} {@code LiveData} by applying
     * {@code mapFunction} to each value set on {@code source}.
     * <p>
     * This method is analogous to {@link io.reactivex.rxjava3.core.Observable#map}.
     * <p>
     * {@code transform} will be executed on the UI thread.
     * <p>
     * Here is an example mapping a simple {@code User} struct in a {@code LiveData} to a
     * {@code LiveData} containing their full name as a {@code String}.
     *
     * <pre>{@code
     * LiveData<User> userLiveData = ...;
     * LiveData<String> userFullNameLiveData =
     *     Transformations.map(
     *         userLiveData,
     *         user -> user.firstName + user.lastName);
     * }</pre>
     *
     * @param source      the {@code LiveData} to map from
     * @param mapFunction a function to apply to each value set on {@code source} in order to set
     *                    it
     *                    on the output {@code LiveData}
     * @param <X>         the generic type parameter of {@code source}
     * @param <Y>         the generic type parameter of the returned {@code LiveData}
     * @return a LiveData mapped from {@code source} to type {@code <Y>} by applying
     * {@code mapFunction} to each value set.
     */
    @UiThread
    @Nonnull
    public static <X, Y> LiveData<Y> map(@Nonnull LiveData<X> source,
                                         @Nonnull final Function<? super X, ? extends Y> mapFunction) {
        final MediatorLiveData<Y> result = new MediatorLiveData<>();
        result.addSource(source, x -> result.setValue(mapFunction.apply(x)));
        return result;
    }

    /**
     * Returns a {@code LiveData} mapped from the input {@code source} {@code LiveData} by applying
     * {@code switchMapFunction} to each value set on {@code source}.
     * <p>
     * The returned {@code LiveData} delegates to the most recent {@code LiveData} created by
     * calling {@code switchMapFunction} with the most recent value set to {@code source}, without
     * changing the reference. In this way, {@code switchMapFunction} can change the 'backing'
     * {@code LiveData} transparently to any observer registered to the {@code LiveData} returned
     * by {@code switchMap()}.
     * <p>
     * Note that when the backing {@code LiveData} is switched, no further values from the older
     * {@code LiveData} will be set to the output {@code LiveData}. In this way, the method is
     * analogous to {@link io.reactivex.rxjava3.core.Observable#switchMap}.
     * <p>
     * {@code switchMapFunction} will be executed on the UI thread.
     * <p>
     * Here is an example class that holds a typed-in name of a user
     * {@code String} (such as from an {@code EditText}) in a {@link MutableLiveData} and
     * returns a {@code LiveData} containing a List of {@code User} objects for users that have
     * that name. It populates that {@code LiveData} by re-querying a repository-pattern object
     * each time the typed name changes.
     * <p>
     * This {@code ViewModel} would permit the observing UI to update "live" as the user ID text
     * changes.
     *
     * <pre>{@code
     * class UserViewModel extends ViewModel {
     *     MutableLiveData<String> nameQueryLiveData = ...
     *
     *     LiveData<List<String>> getUsersWithNameLiveData() {
     *         return Transformations.switchMap(
     *             nameQueryLiveData, myDataSource::getUsersWithNameLiveData);
     *     }
     *
     *     void setNameQuery(String name) {
     *         this.nameQueryLiveData.setValue(name);
     *     }
     * }
     * }</pre>
     *
     * @param source            the {@code LiveData} to map from
     * @param switchMapFunction a function to apply to each value set on {@code source} to create a
     *                          new delegate {@code LiveData} for the returned one
     * @param <X>               the generic type parameter of {@code source}
     * @param <Y>               the generic type parameter of the returned {@code LiveData}
     * @return a LiveData mapped from {@code source} to type {@code <Y>} by delegating
     * to the LiveData returned by applying {@code switchMapFunction} to each
     * value set
     */
    @UiThread
    @Nonnull
    public static <X, Y> LiveData<Y> switchMap(@Nonnull LiveData<X> source,
                                               @Nonnull final Function<? super X, ? extends LiveData<? extends Y>> switchMapFunction) {
        final MediatorLiveData<Y> result = new MediatorLiveData<>();
        result.addSource(source, new Observer<>() {
            private LiveData<? extends Y> mSource;

            @Override
            public void onChanged(@Nullable X x) {
                LiveData<? extends Y> newLiveData = switchMapFunction.apply(x);
                if (mSource == newLiveData) {
                    return;
                }
                if (mSource != null) {
                    result.removeSource(mSource);
                }
                mSource = newLiveData;
                if (mSource != null) {
                    result.addSource(mSource, result::setValue);
                }
            }
        });
        return result;
    }

    /**
     * Creates a new {@link LiveData} object that does not emit a value until the source LiveData
     * value has been changed.  The value is considered changed if {@link Object#equals(Object)}
     * yields {@code false}.
     *
     * @param source the input {@link LiveData}
     * @param <X>    the generic type parameter of {@code source}
     * @return a new {@link LiveData} of type {@code X}
     */
    @UiThread
    @Nonnull
    public static <X> LiveData<X> distinctUntilChanged(@Nonnull LiveData<X> source) {
        final MediatorLiveData<X> result = new MediatorLiveData<>();
        result.addSource(source, new Observer<>() {
            private boolean mFirstChanged;

            @Override
            public void onChanged(X currentValue) {
                final X previousValue = result.getValue();
                if (!mFirstChanged || !Objects.equals(previousValue, currentValue)) {
                    mFirstChanged = true;
                    result.setValue(currentValue);
                }
            }
        });
        return result;
    }
}
