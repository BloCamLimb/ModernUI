/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.core;

import java.util.Locale;

public class ShaderUtils {

    public static String buildShaderErrorMessage(String shader, String errors) {
        StringBuilder b = new StringBuilder("""
                Shader compilation error
                ------------------------
                """);
        String[] lines = shader.split("\n");
        for (int i = 0; i < lines.length; ++i) {
            b.append(String.format(Locale.ROOT, "%4s\t%s\n", i + 1, lines[i]));
        }
        b.append("Errors:\n");
        b.append(errors);
        return b.toString();
    }
}
