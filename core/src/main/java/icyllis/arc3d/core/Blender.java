/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.core;

import icyllis.modernui.annotation.Nullable;

/**
 * {@code Blender} represents a custom blend function in the pipeline. A blender
 * combines a source color (from the paint) and destination color (from the
 * draw buffer) into a new color.
 *
 * @see BlendMode
 * @since 3.7
 */
public interface Blender {

    /**
     * Returns the blender's BlendMode in `mode` if this Blender represents any BlendMode.
     * Returns null for other types of blends.
     * <p>
     * Note that this method is equivalent to <code>(o instanceof BlendMode)</code> check.
     */
    @Nullable
    default BlendMode asBlendMode() {
        return null;
    }
}
