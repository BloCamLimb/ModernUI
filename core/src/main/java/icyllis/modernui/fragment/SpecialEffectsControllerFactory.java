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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.view.ViewGroup;

/**
 * Factory for constructing instances of {@link SpecialEffectsController} on demand.
 */
@FunctionalInterface
interface SpecialEffectsControllerFactory {

    /**
     * Create a new {@link SpecialEffectsController} for the given container.
     *
     * @param container The ViewGroup the created SpecialEffectsController should control.
     * @return a new instance of SpecialEffectsController
     */
    @NonNull
    SpecialEffectsController createController(@NonNull ViewGroup container);
}
