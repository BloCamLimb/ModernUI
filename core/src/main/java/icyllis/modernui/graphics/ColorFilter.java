/*
 * Modern UI.
 * Copyright (C) 2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics;

import org.jetbrains.annotations.ApiStatus;

/**
 * ColorFilters are optional objects in the drawing pipeline. When present in
 * a paint, they are called with the "src" colors, and return new colors, which
 * are then passed onto the next stage (either ImageFilter or Blender).
 * <p>
 * All subclasses are required to be reentrant-safe : it must be legal to share
 * the same instance between several threads.
 *
 * @since 3.11
 */
public abstract class ColorFilter {

    /**
     * Returns null if this filter is no-op.
     *
     * @hidden
     */
    @ApiStatus.Internal
    public abstract icyllis.arc3d.core.effects.ColorFilter getNativeColorFilter();
}
