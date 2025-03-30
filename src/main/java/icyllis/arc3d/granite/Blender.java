/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2025 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.granite;

import org.jspecify.annotations.Nullable;

/**
 * {@code Blender} represents a custom blend function in the pipeline. A blender
 * combines a source color (from the paint) and destination color (from the
 * canvas) into a new color.
 *
 * @see BlendMode
 */
public interface Blender {

    /**
     * Returns the blender's BlendMode in 'mode' if this Blender represents any BlendMode.
     * Returns null for other types of blends.
     */
    @Nullable
    default BlendMode asBlendMode() {
        return null;
    }
}
