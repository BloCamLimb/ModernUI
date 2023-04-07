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

package icyllis.modernui.akashi.shading;

/**
 * Interface for all shaders builders.
 */
public interface ShaderBuilder {

    /**
     * Writes the specified string to one of the shaders.
     */
    void codeAppend(String str);

    /**
     * Writes a formatted string to one of the shaders using the specified format
     * string and arguments.
     *
     * @see java.util.Formatter#format(String, Object...)
     */
    void codeAppendf(String format, Object... args);

    /**
     * Similar to {@link #codeAppendf(String, Object...)}, but writes at the beginning.
     */
    void codePrependf(String format, Object... args);

    /**
     * Generates a mangled name for a helper function in the fragment shader. Will give consistent
     * results if called more than once.
     */
    String getMangledName(String baseName);
}
