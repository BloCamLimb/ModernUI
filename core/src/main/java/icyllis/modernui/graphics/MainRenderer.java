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
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static icyllis.modernui.graphics.GLWrapper.*;

/**
 * The main renderer for {@link Window Window}, which does
 * OpenGL calls on render thread. It will always use dedicated GPU to render.
 */
@RenderThread
public final class MainRenderer {

    public static final int MATRIX_BLOCK_BINDING = 0;

    public static final Shader POS_COLOR = new Shader();

    private final int mMatrixUBO;

    private final List<RenderNode> mRenderNodes = new ArrayList<>();

    private int mPosColorVAO;

    private Int2ObjectMap<Runnable> mFormatSetup = new Int2ObjectArrayMap<>();

    private MainRenderer() {
        mMatrixUBO = glCreateBuffers();
        glNamedBufferStorage(mMatrixUBO, 64 * 128, GL_DYNAMIC_STORAGE_BIT);
        glBindBufferBase(GL_UNIFORM_BUFFER, MATRIX_BLOCK_BINDING, mMatrixUBO);

        mPosColorVAO = glCreateVertexArrays();
        glVertexArrayAttribFormat(mPosColorVAO, 0, 2, GL_FLOAT, false, 0);
        glVertexArrayAttribFormat(mPosColorVAO, 1, 4, GL_UNSIGNED_BYTE, true, 8);
        glVertexArrayAttribBinding(mPosColorVAO, 0, 0);
        glVertexArrayAttribBinding(mPosColorVAO, 1, 0);

        glVertexArrayAttribFormat(mPosColorVAO, 2, 4, GL_FLOAT, false, 0);
        glVertexArrayAttribFormat(mPosColorVAO, 3, 4, GL_FLOAT, false, 16);
        glVertexArrayAttribFormat(mPosColorVAO, 4, 4, GL_FLOAT, false, 32);
        glVertexArrayAttribFormat(mPosColorVAO, 5, 4, GL_FLOAT, false, 48);
        glVertexArrayAttribBinding(mPosColorVAO, 2, 1);
        glVertexArrayAttribBinding(mPosColorVAO, 3, 1);
        glVertexArrayAttribBinding(mPosColorVAO, 4, 1);
        glVertexArrayAttribBinding(mPosColorVAO, 5, 1);
        glVertexArrayBindingDivisor(mPosColorVAO, 1, 1);

        mFormatSetup.put(RenderNode.RECT, () -> {
            glBindVertexArray(mPosColorVAO);
            POS_COLOR.use();
        });

        ShaderManager.getInstance().addListener(this::onLoadShaders);
    }

    public void render() {
        for (var node : mRenderNodes) {
            Int2LongMap state = node.update();
            for (var entry : state.int2LongEntrySet()) {
                int format = entry.getIntKey();
                mFormatSetup.get(format).run();
            }
        }
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
