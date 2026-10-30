/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
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

package icyllis.modernui.resources;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Runtime resource factory, suitable for complex color, animation, transition.
 *
 * @hidden
 */
@ApiStatus.Internal
@FunctionalInterface
public interface RuntimeResourceFactory<T> extends RuntimeFactory {

    /**
     * @param resources resources object
     * @param theme     theme object
     * @return new resource instance
     */
    @Nullable
    T create(@NonNull Resources resources, Resources.@Nullable Theme theme);
}
