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

package icyllis.modernui.graphics.shader;

import net.minecraft.client.shader.IShaderManager;
import net.minecraft.client.shader.ShaderLoader;

import javax.annotation.Nonnull;

public class ShaderProgram implements IShaderManager {

    private int program;

    private ShaderLoader vertex;

    private ShaderLoader fragment;

    public ShaderProgram(int program, ShaderLoader vertex, ShaderLoader fragment) {
        this.program = program;
        this.vertex = vertex;
        this.fragment = fragment;
    }

    @Override
    public int getProgram() {
        return program;
    }

    @Override
    public void markDirty() {

    }

    @Nonnull
    @Override
    public ShaderLoader getVertexShaderLoader() {
        return vertex;
    }

    @Nonnull
    @Override
    public ShaderLoader getFragmentShaderLoader() {
        return fragment;
    }

}
