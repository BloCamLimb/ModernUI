/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.shader.program;

import icyllis.modernui.graphics.shader.ShaderProgram;
import org.lwjgl.opengl.GL20;

public class RoundedFrameShader extends ShaderProgram {

    public static RoundedFrameShader INSTANCE = new RoundedFrameShader("rect", "rounded_rect_frame");

    public RoundedFrameShader(String vert, String frag) {
        super(vert, frag);
    }

    public void setRadius(float radius) {
        GL20.glUniform1f(0, radius);
    }

    public void setInnerRect(float left, float top, float right, float bottom) {
        GL20.glUniform4f(1, left, top, right, bottom);
    }
}
