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

package icyllis.modernui.util;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;

/**
 * Provides methods for registering or unregistering arbitrary observers in an {@link ArrayList}.
 * <p>
 * This abstract class is intended to be subclassed and specialized to maintain
 * a registry of observers of specific types and dispatch notifications to them.
 *
 * @param <T> The observer type.
 */
@ParametersAreNonnullByDefault
public abstract class Observable<T> {

    /**
     * The list of observers.  An observer can be in the list at most
     * once and will never be null.
     */
    protected final ArrayList<T> mObservers = new ArrayList<T>();

    /**
     * Adds an observer to the list. The observer cannot be null, and it must not already
     * be registered.
     *
     * @param observer the observer to register
     * @throws IllegalStateException the observer is already registered
     */
    public void registerObserver(T observer) {
        synchronized (mObservers) {
            if (mObservers.contains(observer)) {
                throw new IllegalStateException("Observer " + observer + " is already registered.");
            }
            mObservers.add(observer);
        }
    }

    /**
     * Removes a previously registered observer. The observer must not be null, and it
     * must already have been registered.
     *
     * @param observer the observer to unregister
     * @throws IllegalStateException the observer is not yet registered
     */
    public void unregisterObserver(T observer) {
        synchronized (mObservers) {
            if (!mObservers.remove(observer)) {
                throw new IllegalStateException("Observer " + observer + " was not registered.");
            }
        }
    }

    /**
     * Remove all registered observers.
     */
    public void unregisterAll() {
        synchronized (mObservers) {
            mObservers.clear();
        }
    }
}
