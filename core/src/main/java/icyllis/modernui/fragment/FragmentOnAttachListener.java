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

import javax.annotation.Nonnull;

public interface FragmentOnAttachListener {

    /**
     * Called after the fragment has been attached to its host. This is called
     * immediately after {@link Fragment#onAttach(Context)} and before
     * {@link Fragment#onAttach(Context)} has been called on any child fragments.
     *
     * @param fragmentManager FragmentManager the fragment is now attached to. This will
     *                        be the same FragmentManager that is returned by
     *                        {@link Fragment#getParentFragmentManager()}.
     * @param fragment Fragment that just received a callback to {@link Fragment#onAttach(Context)}
     */
    @UiThread
    void onAttachFragment(@Nonnull FragmentManager fragmentManager, @Nonnull Fragment fragment);
}
