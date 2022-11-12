/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.textmc;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.graphics.opengl.GLTexture;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

import static icyllis.modernui.graphics.opengl.GLCore.*;

@RenderThread
public class EffectRenderType extends RenderType {

    private static final GLTexture WHITE = new GLTexture(GL_TEXTURE_2D);

    private static final ImmutableList<RenderStateShard> STATES;
    private static final ImmutableList<RenderStateShard> SEE_THROUGH_STATES;

    private static final EffectRenderType TYPE;
    private static final EffectRenderType SEE_THROUGH_TYPE;

    static {
        WHITE.allocate2DCompat(GL_R8, 2, 2, 0);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer pixels = stack.bytes((byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff);
            WHITE.uploadCompat(0, 0, 0, 2, 2, 0, 0, 0, 1,
                    GL_RED, GL_UNSIGNED_BYTE, MemoryUtil.memAddress(pixels));
        }
        WHITE.setSwizzleCompat(GL_ONE, GL_ONE, GL_ONE, GL_ONE);
        STATES = ImmutableList.of(
                TextRenderType.RENDERTYPE_MODERN_TEXT,
                TRANSLUCENT_TRANSPARENCY,
                LEQUAL_DEPTH_TEST,
                CULL,
                LIGHTMAP,
                NO_OVERLAY,
                NO_LAYERING,
                MAIN_TARGET,
                DEFAULT_TEXTURING,
                COLOR_DEPTH_WRITE,
                DEFAULT_LINE
        );
        SEE_THROUGH_STATES = ImmutableList.of(
                TextRenderType.RENDERTYPE_MODERN_TEXT_SEE_THROUGH,
                TRANSLUCENT_TRANSPARENCY,
                NO_DEPTH_TEST,
                CULL,
                LIGHTMAP,
                NO_OVERLAY,
                NO_LAYERING,
                MAIN_TARGET,
                DEFAULT_TEXTURING,
                COLOR_WRITE,
                DEFAULT_LINE
        );
        TYPE = new EffectRenderType("modern_text_effect", 256, () -> {
            STATES.forEach(RenderStateShard::setupRenderState);
            RenderSystem.enableTexture();
            RenderSystem.setShaderTexture(0, WHITE.get());
        }, () -> STATES.forEach(RenderStateShard::clearRenderState));
        SEE_THROUGH_TYPE = new EffectRenderType("modern_text_effect_see_through", 256, () -> {
            SEE_THROUGH_STATES.forEach(RenderStateShard::setupRenderState);
            RenderSystem.enableTexture();
            RenderSystem.setShaderTexture(0, WHITE.get());
        }, () -> SEE_THROUGH_STATES.forEach(RenderStateShard::clearRenderState));
    }

    private EffectRenderType(String name, int bufferSize, Runnable setupState, Runnable clearState) {
        super(name, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS,
                bufferSize, false, true, setupState, clearState);
    }

    @Nonnull
    public static EffectRenderType getRenderType(boolean seeThrough) {
        return seeThrough ? SEE_THROUGH_TYPE : TYPE;
    }

    @Nonnull
    public static EffectRenderType getRenderType(Font.DisplayMode mode) {
        throw new IllegalStateException();
    }
}
