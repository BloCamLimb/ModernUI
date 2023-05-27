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

package icyllis.modernui.fragment;

import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.util.DataSet;

import javax.annotation.Nonnull;

/**
 * Listener for receiving a callback immediately following {@link Fragment#onAttach(icyllis.modernui.core.Context)}.
 * This can be used to perform any additional setup / provide any dependencies that the Fragment
 * may need prior to child fragments being attached or the Fragment going through
 * {@link Fragment#onCreate(DataSet)}.
 *
 * @see FragmentManager#addFragmentOnAttachListener(FragmentOnAttachListener)
 */
@FunctionalInterface
public interface FragmentOnAttachListener {

    /**
     * Called after the fragment has been attached to its host. This is called
     * immediately after {@link Fragment#onAttach(icyllis.modernui.core.Context)} and before
     * {@link Fragment#onAttach(icyllis.modernui.core.Context)} has been called on any child fragments.
     *
     * @param fragmentManager FragmentManager the fragment is now attached to. This will
     *                        be the same FragmentManager that is returned by
     *                        {@link Fragment#getParentFragmentManager()}.
     * @param fragment        Fragment that just received a callback to {@link Fragment#onAttach(icyllis.modernui.core.Context)}
     */
    @UiThread
    void onAttachFragment(@Nonnull FragmentManager fragmentManager, @Nonnull Fragment fragment);
}
