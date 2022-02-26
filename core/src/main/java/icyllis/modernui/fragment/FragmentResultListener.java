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

import icyllis.modernui.lifecycle.LifecycleOwner;
import icyllis.modernui.util.DataSet;

import javax.annotation.Nonnull;

/**
 * Listener for handling fragment results.
 * <p>
 * This object should be passed to
 * {@link FragmentManager#setFragmentResultListener(String, LifecycleOwner, FragmentResultListener)}
 * and it will listen for results with the same key that are passed into
 * {@link FragmentManager#setFragmentResult(String, DataSet)}.
 *
 * @see FragmentResultOwner#setFragmentResultListener
 */
public interface FragmentResultListener {

    /**
     * Callback used to handle results passed between fragments.
     *
     * @param requestKey key used to store the result
     * @param result     result passed to the callback
     */
    void onFragmentResult(@Nonnull String requestKey, @Nonnull DataSet result);
}
