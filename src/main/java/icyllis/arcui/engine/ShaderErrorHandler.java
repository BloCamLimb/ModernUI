/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.engine;

import java.util.Locale;

/**
 * Abstract class to report errors when compiling shaders.
 */
@FunctionalInterface
public interface ShaderErrorHandler {

    /**
     * Used when no error handler is set. Will throw RuntimeException.
     */
    ShaderErrorHandler DEFAULT = (shader, errors) -> {
        System.err.println("Shader compilation error");
        System.err.println("------------------------");
        String[] lines = shader.split("\n");
        for (int i = 0; i < lines.length; ++i) {
            System.err.printf(Locale.ROOT, "%4s\t%s\n", i + 1, lines[i]);
        }
        System.err.println("Errors:");
        System.err.println(errors);
        throw new RuntimeException("Shader compilation failed!");
    };

    void compileError(String shader, String errors);
}
