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

package icyllis.modernui.forge;

import icyllis.modernui.fragment.Fragment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A convenient way to check instanceof MenuScreen or SimpleScreen
 */
public sealed interface MuiScreen permits MenuScreen, SimpleScreen {

    /**
     * @return the main fragment
     */
    @Nonnull
    Fragment getFragment();

    /**
     * @return a callback describes the screen properties
     */
    @Nullable
    ScreenCallback getCallback();
}
