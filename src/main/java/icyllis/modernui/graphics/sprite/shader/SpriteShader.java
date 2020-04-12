/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.graphics.sprite.shader;

import icyllis.modernui.graphics.shader.ShaderProgram;
import icyllis.modernui.graphics.shader.ShaderTools;
import icyllis.modernui.graphics.shader.VertexAttrib;
import icyllis.modernui.graphics.shader.uniform.UniformFloat;
import icyllis.modernui.graphics.shader.uniform.UniformMatrix4f;
import icyllis.modernui.math.Matrix4f;
import net.minecraft.client.shader.ShaderLoader;

import java.nio.FloatBuffer;

public abstract class SpriteShader extends ShaderProgram {

    private UniformMatrix4f MVMatrix;

    private UniformMatrix4f PMatrix;

    private UniformFloat alpha;

    private VertexAttrib vertexPosition;

    private VertexAttrib vertexColor;

    public SpriteShader(int program, ShaderLoader vertex, ShaderLoader fragment) {
        super(program, vertex, fragment);
    }

    public void findPointers() {
        MVMatrix = ShaderTools.getUniformMatrix4f(this, "u_MVMatrix");
        PMatrix = ShaderTools.getUniformMatrix4f(this, "u_PMatrix");
        alpha = ShaderTools.getUniformFloat(this, "u_alpha");
        vertexPosition = new VertexAttrib(VertexAttrib.Type.VEC3, 0);
        vertexColor = new VertexAttrib(VertexAttrib.Type.VEC4, 1);
    }

    public void loadMVMatrix(Matrix4f matrix4f) {
        MVMatrix.load(matrix4f);
    }

    public void loadPMatrix(Matrix4f matrix4f) {
        PMatrix.load(matrix4f);
    }

    public void loadGlobalAlpha(float f) {
        alpha.load(f);
    }

    public void loadPosition(FloatBuffer buffer) {
        vertexPosition.load(buffer);
    }

    public void loadColor(FloatBuffer buffer) {
        vertexColor.load(buffer);
    }
}
