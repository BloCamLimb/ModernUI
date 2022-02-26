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

package icyllis.modernui.fragment;

import icyllis.modernui.lifecycle.Lifecycle;
import icyllis.modernui.lifecycle.LifecycleOwner;
import icyllis.modernui.util.DataSet;

import javax.annotation.Nonnull;

/**
 * A class that manages passing data between fragments.
 */
public interface FragmentResultOwner {

    /**
     * Sets the given result for the requestKey. This result will be delivered to a
     * {@link FragmentResultListener} that is called given to
     * {@link #setFragmentResultListener(String, LifecycleOwner, FragmentResultListener)} with
     * the same requestKey. If no {@link FragmentResultListener} with the same key is set or the
     * Lifecycle associated with the listener is not at least
     * {@link Lifecycle.State#STARTED}, the result is stored until one becomes
     * available, or {@link #clearFragmentResult(String)} is called with the same requestKey.
     *
     * @param requestKey key used to identify the result
     * @param result     the result to be passed to another fragment
     */
    void setFragmentResult(@Nonnull String requestKey, @Nonnull DataSet result);

    /**
     * Clears the stored result for the given requestKey.
     * <p>
     * This clears any result that was previously set via
     * {@link #setFragmentResult(String, DataSet)} that hasn't yet been delivered to a
     * {@link FragmentResultListener}.
     *
     * @param requestKey key used to identify the result
     */
    void clearFragmentResult(@Nonnull String requestKey);

    /**
     * Sets the {@link FragmentResultListener} for a given requestKey. Once the given
     * {@link LifecycleOwner} is at least in the {@link Lifecycle.State#STARTED}
     * state, any results set by {@link #setFragmentResult(String, DataSet)} using the same
     * requestKey will be delivered to the
     * {@link FragmentResultListener#onFragmentResult(String, DataSet) callback}. The callback will
     * remain active until the LifecycleOwner reaches the
     * {@link Lifecycle.State#DESTROYED} state or
     * {@link #clearFragmentResultListener(String)} is called with the same requestKey.
     *
     * @param requestKey     requestKey used to identify the result
     * @param lifecycleOwner lifecycleOwner for handling the result
     * @param listener       listener for result changes
     */
    void setFragmentResultListener(@Nonnull String requestKey,
                                   @Nonnull LifecycleOwner lifecycleOwner,
                                   @Nonnull FragmentResultListener listener);

    /**
     * Clears the stored {@link FragmentResultListener} for the given requestKey.
     * <p>
     * This clears any {@link FragmentResultListener} that was previously set via
     * {@link #setFragmentResultListener(String, LifecycleOwner, FragmentResultListener)}.
     *
     * @param requestKey key used to identify the result
     */
    void clearFragmentResultListener(@Nonnull String requestKey);
}
