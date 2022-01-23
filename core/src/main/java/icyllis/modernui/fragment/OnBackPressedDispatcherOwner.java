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

import javax.annotation.Nonnull;

/**
 * A class that has an {@link OnBackPressedDispatcher} that allows you to register a
 * {@link OnBackPressedCallback} for handling the system back button.
 * <p>
 * It is expected that classes that implement this interface route the system back button
 * to the dispatcher
 *
 * @see OnBackPressedDispatcher
 */
public interface OnBackPressedDispatcherOwner extends LifecycleOwner {

    /**
     * Retrieve the {@link OnBackPressedDispatcher} that should handle the system back button.
     *
     * @return The {@link OnBackPressedDispatcher}.
     */
    @Nonnull
    OnBackPressedDispatcher getOnBackPressedDispatcher();
}
