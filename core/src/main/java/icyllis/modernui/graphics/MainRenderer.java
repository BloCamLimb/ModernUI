/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.graphics.shader.Shader;
import icyllis.modernui.graphics.shader.ShaderManager;
import icyllis.modernui.platform.Window;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

import static icyllis.modernui.graphics.GLWrapper.*;

/**
 * The main renderer for {@link Window Window}, which does
 * OpenGL calls on render thread. It will always use dedicated GPU to render.
 */
@RenderThread
public final class MainRenderer {

    public static final int MATRIX_BLOCK_BINDING = 0;

    public static final Shader POS_COLOR = new Shader();

    private final GLCanvas mCanvas = GLCanvas.getInstance();

    private final int mMatrixUBO;

    private MainRenderer() {
        mMatrixUBO = glCreateBuffers();
        glNamedBufferStorage(mMatrixUBO, 64 * 128, GL_DYNAMIC_STORAGE_BIT);
        glBindBufferBase(GL_UNIFORM_BUFFER, MATRIX_BLOCK_BINDING, mMatrixUBO);

        ShaderManager.getInstance().addListener(this::onLoadShaders);
    }

    private void onLoadShaders(@Nonnull ShaderManager manager) {
        int pos_color_vert = manager.getShard(ModernUI.get(), "pos_color.vert");
        int color_frag = manager.getShard(ModernUI.get(), "color.frag");
        manager.create(POS_COLOR, pos_color_vert, color_frag);
    }

    public void uploadMatrix(@Nonnull ByteBuffer data, int size) {
        nglNamedBufferSubData(mMatrixUBO, 0, size, MemoryUtil.memAddress(data));
    }
}
