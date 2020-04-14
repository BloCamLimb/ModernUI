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

package icyllis.modernui.graphics.shader.uniform;

import icyllis.modernui.graphics.shader.ShaderUniform;
import icyllis.modernui.gui.math.Matrix4f;
import org.lwjgl.opengl.GL20;

import javax.annotation.Nonnull;

public class UniformMatrix4f extends ShaderUniform<Matrix4f> {

    public UniformMatrix4f(int location) {
        super(location);
    }

    @Override
    public void load(@Nonnull Matrix4f data) {
        if (location != -1) {
            GL20.glUniform4fv(location, data.getData());
        }
    }
}
