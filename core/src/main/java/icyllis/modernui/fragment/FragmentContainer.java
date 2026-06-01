/*
 * ModernUI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.fragment;

import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.view.View;

/**
 * Callbacks to a {@link Fragment}'s container.
 */
interface FragmentContainer {

    /**
     * Return the view with the given resource ID. May return {@code null} if the
     * view is not a child of this container.
     */
    @Nullable
    View onFindViewById(int id);

    /**
     * Return {@code true} if the container holds any view.
     */
    boolean onHasView();
}
