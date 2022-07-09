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
import icyllis.modernui.core.NativeImage;
import icyllis.modernui.graphics.opengl.GLCore;
import icyllis.modernui.graphics.opengl.GLTexture;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.Objects;

import static org.lwjgl.opengl.GL11C.GL_ONE;
import static org.lwjgl.opengl.GL11C.GL_RED;

@RenderThread
public class EffectRenderType extends RenderType {

    private static final GLTexture WHITE = new GLTexture(GLCore.GL_TEXTURE_2D);

    private static final EffectRenderType INSTANCE = new EffectRenderType();
    private static final EffectRenderType SEE_THROUGH = new EffectRenderType("modern_text_effect_see_through");

    private static final ImmutableList<RenderStateShard> STATES;
    private static final ImmutableList<RenderStateShard> SEE_THROUGH_STATES;

    static {
        WHITE.allocate2DCompat(NativeImage.Format.RED.internalGlFormat, 2, 2, 0);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer pixels = stack.malloc(4);
            for (int i = 0; i < 4; i++) {
                pixels.put((byte) 0xff);
            }
            WHITE.uploadCompat(0, 0, 0, 2, 2, 0, 0, 0, 1,
                    NativeImage.Format.RED.glFormat, GLCore.GL_UNSIGNED_BYTE, MemoryUtil.memAddress(pixels.flip()));
        }
        WHITE.setSwizzleCompat(GL_ONE, GL_ONE, GL_ONE, GL_RED);
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
    }

    private final int hashCode;

    private EffectRenderType() {
        super("modern_text_effect",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS, 256, false, true,
                () -> {
                    STATES.forEach(RenderStateShard::setupRenderState);
                    RenderSystem.enableTexture();
                    RenderSystem.setShaderTexture(0, WHITE.get());
                },
                () -> STATES.forEach(RenderStateShard::clearRenderState));
        this.hashCode = Objects.hash(super.hashCode(), STATES);
    }

    private EffectRenderType(String t) {
        super(t,
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS, 256, false, true,
                () -> {
                    SEE_THROUGH_STATES.forEach(RenderStateShard::setupRenderState);
                    RenderSystem.enableTexture();
                    RenderSystem.setShaderTexture(0, WHITE.get());
                },
                () -> SEE_THROUGH_STATES.forEach(RenderStateShard::clearRenderState));
        this.hashCode = Objects.hash(super.hashCode(), SEE_THROUGH_STATES);
    }

    @Nonnull
    public static EffectRenderType getRenderType(boolean seeThrough) {
        return seeThrough ? SEE_THROUGH : INSTANCE;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * Singleton, the constructor is private
     */
    @Override
    public boolean equals(Object o) {
        return this == o;
    }
}
