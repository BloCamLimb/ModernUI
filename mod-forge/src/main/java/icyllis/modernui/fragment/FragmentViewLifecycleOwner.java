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

import icyllis.modernui.lifecycle.ILifecycleOwner;
import icyllis.modernui.lifecycle.Lifecycle;

import javax.annotation.Nonnull;

class FragmentViewLifecycleOwner implements ILifecycleOwner {

    private Lifecycle mLifecycle;

    void initialize() {
        if (mLifecycle == null) {
            mLifecycle = new Lifecycle(this);
        }
    }

    boolean isInitialized() {
        return mLifecycle != null;
    }

    @Nonnull
    @Override
    public Lifecycle getLifecycle() {
        initialize();
        return mLifecycle;
    }

    void handleLifecycleEvent(Lifecycle.Event event) {
        mLifecycle.handleLifecycleEvent(event);
    }
}
