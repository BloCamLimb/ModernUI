/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.graphics.shader;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

public class VertexAttrib {

    private final Type type;

    private final int location;

    public VertexAttrib(Type type, int location) {
        this.type = type;
        this.location = location;
    }

    public void load(FloatBuffer buffer) {
        if (location != -1) {
            GL20.glEnableVertexAttribArray(location);
            GL20.glVertexAttribPointer(location, type.count, type.type, false, type.step, buffer);
        }
    }

    public enum Type {
        VEC2(2, 8, GL11.GL_FLOAT), // float is 4 bytes, so vec2 is 2 * 4 = 8, means every 8 bytes a vec2 (in general)
        VEC3(3, 12, GL11.GL_FLOAT),
        VEC4(4, 16, GL11.GL_FLOAT),
        FLOAT(1, 4, GL11.GL_FLOAT);

        private final int count;

        private final int step;

        private final int type;

        Type(int count, int step, int type) {
            this.count = count;
            this.step = step;
            this.type = type;
        }
    }
}
