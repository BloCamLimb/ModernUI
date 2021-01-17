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

import icyllis.modernui.ModernUI;

import javax.annotation.Nonnull;

/**
 * An utility class that provides {@code ViewModels} for a scope.
 */
public class ViewModelProvider {

    private final IFactory factory;
    private final ViewModelStore viewModelStore;

    /**
     * Creates {@code ViewModelProvider}. This will create {@code ViewModels}
     * and retain them in a store of the given {@code ViewModelStoreOwner}.
     * A {@link NewInstanceFactory} will be used as the factory.
     *
     * @param owner a {@code ViewModelStoreOwner} whose {@link ViewModelStore} will be used to
     *              retain {@code ViewModels}
     */
    public ViewModelProvider(@Nonnull IViewModelStoreOwner owner) {
        this(owner.getViewModelStore(), NewInstanceFactory.getInstance());
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
    public ViewModelProvider(@Nonnull IViewModelStoreOwner owner, @Nonnull IFactory factory) {
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
    public ViewModelProvider(@Nonnull ViewModelStore store, @Nonnull IFactory factory) {
        this.factory = factory;
        this.viewModelStore = store;
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
    public <T extends ViewModel> T get(@Nonnull Class<T> modelClass) {
        String canonicalName = modelClass.getCanonicalName();
        if (canonicalName == null) {
            throw new IllegalArgumentException("Local and anonymous classes can not be ViewModels");
        }
        return get(canonicalName, modelClass);
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
    public <T extends ViewModel> T get(@Nonnull String key, @Nonnull Class<T> modelClass) {
        ViewModel viewModel = viewModelStore.get(key);

        if (modelClass.isInstance(viewModel)) {
            return (T) viewModel;
        } else {
            if (viewModel != null) {
                ModernUI.LOGGER.warn(ViewModel.MARKER,
                        "ViewModel {} got by key {} does not match model class {}, a new instance is overriding the previous one",
                        viewModel, key, modelClass);
            }
        }
        viewModel = factory.create(key, modelClass);
        viewModelStore.put(key, viewModel);
        return (T) viewModel;
    }

    /**
     * Implementations of {@code IFactory} interface are responsible to instantiate ViewModels.
     */
    @FunctionalInterface
    public interface IFactory {

        /**
         * Creates a new instance of the given {@code Class}.
         *
         * @param key        a key associated with the requested ViewModel
         * @param modelClass a {@code Class} whose instance is requested
         * @param <T>        The type parameter for the ViewModel.
         * @return a newly created ViewModel
         */
        @Nonnull
        <T extends ViewModel> T create(@Nonnull String key, @Nonnull Class<T> modelClass);
    }

    /**
     * Simple factory, which calls empty constructor on the give class.
     */
    public static class NewInstanceFactory implements IFactory {

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
        public <T extends ViewModel> T create(@Nonnull String key, @Nonnull Class<T> modelClass) {
            try {
                return modelClass.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
