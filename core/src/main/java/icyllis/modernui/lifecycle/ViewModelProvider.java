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
import icyllis.modernui.util.Log;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/**
 * A utility class that provides {@code ViewModels} for a scope.
 * <p>
 * Default {@code ViewModelProvider} for a {@code Fragment} can be obtained
 * by passing it to {@link ViewModelProvider#ViewModelProvider(ViewModelStoreOwner)}.
 */
public class ViewModelProvider {

    private static final String DEFAULT_KEY = "ViewModelProvider.DefaultKey";

    /**
     * Implementations of {@code Factory} interface are responsible to instantiate ViewModels.
     */
    @FunctionalInterface
    public interface Factory {

        /**
         * Creates a new instance of the given {@code Class}.
         * <p>
         *
         * @param modelClass a {@code Class} whose instance is requested
         * @param <T>        The type parameter for the ViewModel.
         * @return a newly created ViewModel
         */
        @Nonnull
        <T extends ViewModel> T create(@Nonnull Class<T> modelClass);
    }

    static class OnRequeryFactory {

        void onRequery(@Nonnull ViewModel viewModel) {
        }
    }

    /**
     * Implementations of {@code Factory} interface are responsible to instantiate ViewModels.
     * <p>
     * This is more advanced version of {@link Factory} that receives a key specified for requested
     * {@link ViewModel}.
     */
    static abstract class KeyedFactory extends OnRequeryFactory implements Factory {

        /**
         * Creates a new instance of the given {@code Class}.
         *
         * @param key        a key associated with the requested ViewModel
         * @param modelClass a {@code Class} whose instance is requested
         * @param <T>        The type parameter for the ViewModel.
         * @return a newly created ViewModel
         */
        @Nonnull
        public abstract <T extends ViewModel> T create(@Nonnull String key,
                                                       @Nonnull Class<T> modelClass);

        @Nonnull
        @Override
        public final <T extends ViewModel> T create(@Nonnull Class<T> modelClass) {
            throw new UnsupportedOperationException("create(String, Class<?>) must be called on "
                    + "implementations of KeyedFactory");
        }
    }

    private final Factory mFactory;
    private final ViewModelStore mViewModelStore;

    /**
     * Creates {@code ViewModelProvider}. This will create {@code ViewModels}
     * and retain them in a store of the given {@code ViewModelStoreOwner}.
     * <p>
     * This method will use the
     * {@link ViewModelStoreOwner#getDefaultViewModelProviderFactory() default factory}
     * if it returns not null. Otherwise, a {@link NewInstanceFactory} will be used.
     */
    public ViewModelProvider(@Nonnull ViewModelStoreOwner owner) {
        this(owner.getViewModelStore(), Objects.requireNonNullElseGet(
                owner.getDefaultViewModelProviderFactory(), NewInstanceFactory::getInstance));
    }

    /**
     * Creates {@code ViewModelProvider}, which will create {@code ViewModels} via the given
     * {@code Factory} and retain them in a store of the given {@code ViewModelStoreOwner}.
     *
     * @param owner   a {@code ViewModelStoreOwner} whose {@link ViewModelStore} will be used to
     *                retain {@code ViewModels}
     * @param factory a {@code Factory} which will be used to instantiate
     *                new {@code ViewModels}
     */
    public ViewModelProvider(@Nonnull ViewModelStoreOwner owner, @Nonnull Factory factory) {
        this(owner.getViewModelStore(), factory);
    }

    /**
     * Creates {@code ViewModelProvider}, which will create {@code ViewModels} via the given
     * {@code Factory} and retain them in the given {@code store}.
     *
     * @param store   {@code ViewModelStore} where ViewModels will be stored.
     * @param factory factory a {@code Factory} which will be used to instantiate
     *                new {@code ViewModels}
     */
    public ViewModelProvider(@Nonnull ViewModelStore store, @Nonnull Factory factory) {
        mFactory = factory;
        mViewModelStore = store;
    }

    /**
     * Returns an existing ViewModel or creates a new one in the scope (usually, a fragment or
     * an activity), associated with this {@code ViewModelProvider}.
     * <p>
     * The created ViewModel is associated with the given scope and will be retained
     * as long as the scope is alive (e.g. if it is an activity, until it is
     * finished or process is killed).
     *
     * @param modelClass The class of the ViewModel to create an instance of it if it is not
     *                   present.
     * @param <T>        The type parameter for the ViewModel.
     * @return A ViewModel that is an instance of the given type {@code T}.
     */
    @Nonnull
    @UiThread
    public <T extends ViewModel> T get(@Nonnull Class<T> modelClass) {
        String canonicalName = modelClass.getCanonicalName();
        if (canonicalName == null) {
            throw new IllegalArgumentException("Local and anonymous classes can not be ViewModels");
        }
        return get(DEFAULT_KEY + ":" + canonicalName, modelClass);
    }

    /**
     * Returns an existing ViewModel or creates a new one in the scope (usually, a fragment or
     * an activity), associated with this {@code ViewModelProvider}.
     * <p>
     * The created ViewModel is associated with the given scope and will be retained
     * as long as the scope is alive (e.g. if it is an activity, until it is
     * finished or process is killed).
     *
     * @param key        The key to use to identify the ViewModel.
     * @param modelClass The class of the ViewModel to create an instance of it if it is not
     *                   present.
     * @param <T>        The type parameter for the ViewModel.
     * @return A ViewModel that is an instance of the given type {@code T}.
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    @UiThread
    public <T extends ViewModel> T get(@Nonnull String key, @Nonnull Class<T> modelClass) {
        ViewModel viewModel = mViewModelStore.get(key);

        if (modelClass.isInstance(viewModel)) {
            if (mFactory instanceof OnRequeryFactory) {
                ((OnRequeryFactory) mFactory).onRequery(viewModel);
            }
            return (T) viewModel;
        } else {
            if (viewModel != null) {
                Log.LOGGER.warn(ViewModel.MARKER, "Mismatched model class {} with an existing instance {}",
                        modelClass, viewModel);
            }
        }
        if (mFactory instanceof KeyedFactory) {
            viewModel = ((KeyedFactory) mFactory).create(key, modelClass);
        } else {
            viewModel = mFactory.create(modelClass);
        }
        mViewModelStore.put(key, viewModel);
        return (T) viewModel;
    }

    /**
     * Simple factory, which calls empty constructor on the give class.
     */
    public static class NewInstanceFactory implements Factory {

        private static NewInstanceFactory sInstance;

        /**
         * Retrieve a singleton instance of NewInstanceFactory.
         *
         * @return A valid {@link NewInstanceFactory}
         */
        @Nonnull
        static NewInstanceFactory getInstance() {
            if (sInstance == null) {
                sInstance = new NewInstanceFactory();
            }
            return sInstance;
        }

        @Nonnull
        @Override
        public <T extends ViewModel> T create(@Nonnull Class<T> modelClass) {
            try {
                return modelClass.getDeclaredConstructor().newInstance();
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                throw new RuntimeException("Cannot create an instance of " + modelClass, e);
            }
        }
    }
}
