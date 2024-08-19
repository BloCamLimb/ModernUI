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

/**
 * Shaders specify the source color(s) for what is being drawn. If a paint
 * has no shader, then the paint's color is used. If the paint has a
 * shader, then the shader's color(s) are use instead, but they are
 * modulated by the paint's alpha. This makes it easy to create a shader
 * once (e.g. bitmap tiling or gradient) and then change its transparency
 * w/o having to modify the original shader... only the paint's alpha needs
 * to be modified.
 */
public abstract class Shader {
}
