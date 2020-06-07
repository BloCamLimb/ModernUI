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

package icyllis.modernui.graphics.shader.program;

import icyllis.modernui.graphics.shader.ShaderProgram;
import org.lwjgl.opengl.GL20;

public class RingShader extends ShaderProgram {

    public static RingShader INSTANCE = new RingShader("rect", "ring");

    public RingShader(String vert, String frag) {
        super(vert, frag);
    }

    public void setRadius(float inner, float radius) {
        GL20.glUniform2f(0, inner, radius);
    }

    public void setCenterPos(float x, float y) {
        GL20.glUniform2f(1, x, y);
    }
}
