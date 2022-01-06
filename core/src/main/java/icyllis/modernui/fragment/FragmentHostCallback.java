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

import icyllis.modernui.core.Handler;
import icyllis.modernui.view.View;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * Integration points with the Fragment host.
 * <p>
 * Fragments may be hosted by any object. In order to host fragments, implement
 * {@link FragmentHostCallback}, overriding the methods applicable to the host.
 * <p>
 * FragmentManager changes its behavior based on what optional interfaces your
 * FragmentHostCallback implements. This includes the following:
 * <ul>
 *     <li><strong>{@link FragmentOnAttachListener}</strong>: Removes the need to
 *     manually call {@link FragmentManager#addFragmentOnAttachListener} from your
 *     host in order to receive {@link FragmentOnAttachListener#onAttachFragment} callbacks
 *     for the {@link FragmentController#getFragmentManager()}.</li>
 *     <li><strong>{@link androidx.activity.OnBackPressedDispatcherOwner}</strong>: Removes
 *     the need to manually call
 *     {@link FragmentManager#popBackStackImmediate()} when handling the system
 *     back button.</li>
 * </ul>
 *
 * @param <E> the type of object that's currently hosting the fragments. An instance of this
 *            class must be returned by {@link #onGetHost()}.
 */
public abstract class FragmentHostCallback<E> implements FragmentContainer {

    @Nonnull
    final Handler mHandler;
    @Nonnull
    final FragmentManager mFragmentManager = new FragmentManager();

    public FragmentHostCallback(@Nonnull Handler handler) {
        mHandler = handler;
    }

    /**
     * Print internal state into the given stream.
     *
     * @param prefix Desired prefix to prepend at each line of output.
     * @param fd     The raw file descriptor that the dump is being sent to.
     * @param writer The PrintWriter to which you should dump your state. This will be closed
     *               for you after you return.
     * @param args   additional arguments to the dump request.
     */
    public void onDump(@Nonnull String prefix, @Nullable FileDescriptor fd,
                       @Nonnull PrintWriter writer, @Nullable String... args) {
    }

    /**
     * Return the object that's currently hosting the fragment.
     */
    @Nullable
    public abstract E onGetHost();

    @Nullable
    @Override
    public View onFindViewById(int id) {
        return null;
    }

    @Override
    public boolean onHasView() {
        return true;
    }
}
