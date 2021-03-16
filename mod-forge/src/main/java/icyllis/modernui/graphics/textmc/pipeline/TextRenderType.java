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

package icyllis.modernui.graphics.textmc.pipeline;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.opengl.GL11;

import java.util.Objects;

public class TextRenderType extends RenderType {

    /**
     * Texture id to render type map
     */
    //TODO remove some old textures depend on put order
    private static final Int2ObjectMap<TextRenderType> TYPES             = new Int2ObjectLinkedOpenHashMap<>();
    private static final Int2ObjectMap<TextRenderType> SEE_THROUGH_TYPES = new Int2ObjectLinkedOpenHashMap<>();

    /**
     * Only the texture id is different, the rest state are same
     */
    private static final ImmutableList<RenderStateShard> GENERAL_STATES;
    private static final ImmutableList<RenderStateShard> SEE_THROUGH_STATES;

    static {
        GENERAL_STATES = ImmutableList.of(
                RenderStateShard.TRANSLUCENT_TRANSPARENCY,
                RenderStateShard.NO_DIFFUSE_LIGHTING,
                RenderStateShard.FLAT_SHADE,
                RenderStateShard.DEFAULT_ALPHA,
                RenderStateShard.LEQUAL_DEPTH_TEST,
                RenderStateShard.CULL,
                RenderStateShard.LIGHTMAP,
                RenderStateShard.NO_OVERLAY,
                RenderStateShard.FOG,
                RenderStateShard.NO_LAYERING,
                RenderStateShard.MAIN_TARGET,
                RenderStateShard.DEFAULT_TEXTURING,
                RenderStateShard.COLOR_DEPTH_WRITE,
                RenderStateShard.DEFAULT_LINE
        );
        SEE_THROUGH_STATES = ImmutableList.of(
                RenderStateShard.TRANSLUCENT_TRANSPARENCY,
                RenderStateShard.NO_DIFFUSE_LIGHTING,
                RenderStateShard.FLAT_SHADE,
                RenderStateShard.DEFAULT_ALPHA,
                RenderStateShard.NO_DEPTH_TEST,
                RenderStateShard.CULL,
                RenderStateShard.LIGHTMAP,
                RenderStateShard.NO_OVERLAY,
                RenderStateShard.FOG,
                RenderStateShard.NO_LAYERING,
                RenderStateShard.MAIN_TARGET,
                RenderStateShard.DEFAULT_TEXTURING,
                RenderStateShard.COLOR_WRITE,
                RenderStateShard.DEFAULT_LINE
        );
    }

    private final int hashCode;

    /**
     * The OpenGL texture ID that contains this glyph image.
     */
    public final int textureName;

    private TextRenderType(int textureName) {
        super("modern_text",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                GL11.GL_QUADS, 256, false, true,
                () -> {
                    GENERAL_STATES.forEach(RenderStateShard::setupRenderState);
                    RenderSystem.enableTexture();
                    RenderSystem.bindTexture(textureName);
                },
                () -> GENERAL_STATES.forEach(RenderStateShard::clearRenderState));
        this.textureName = textureName;
        this.hashCode = Objects.hash(super.hashCode(), GENERAL_STATES, textureName);
    }

    private TextRenderType(int textureName, String t) {
        super(t,
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                GL11.GL_QUADS, 256, false, true,
                () -> {
                    SEE_THROUGH_STATES.forEach(RenderStateShard::setupRenderState);
                    RenderSystem.enableTexture();
                    RenderSystem.bindTexture(textureName);
                },
                () -> SEE_THROUGH_STATES.forEach(RenderStateShard::clearRenderState));
        this.textureName = textureName;
        this.hashCode = Objects.hash(super.hashCode(), SEE_THROUGH_STATES, textureName);
    }

    public static TextRenderType getOrCacheType(int textureName, boolean seeThrough) {
        if (seeThrough) {
            return SEE_THROUGH_TYPES.computeIfAbsent(textureName, n -> new TextRenderType(n, "modern_text_see_through"));
        }
        return TYPES.computeIfAbsent(textureName, TextRenderType::new);
    }

    public static void clearTextures() {
        for (Int2ObjectMap.Entry<TextRenderType> entry : TYPES.int2ObjectEntrySet()) {
            GlStateManager._deleteTexture(entry.getIntKey());
        }
        TYPES.clear();
        SEE_THROUGH_TYPES.clear();
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
