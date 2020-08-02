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

package icyllis.modernui.font.node;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.Map;
import java.util.Objects;

public class TextRenderType extends RenderType {

    /**
     * Texture id to render type map
     */
    //TODO remove some old textures depend on put order
    private static final Map<Integer, TextRenderType> TYPES             = new Int2ObjectLinkedOpenHashMap<>();
    private static final Map<Integer, TextRenderType> SEE_THROUGH_TYPES = new Int2ObjectLinkedOpenHashMap<>();

    /**
     * Only the texture id is different, the rest state are same
     */
    private static final ImmutableList<RenderState> GENERAL_STATES;
    private static final ImmutableList<RenderState> SEE_THROUGH_STATES;

    static {
        GENERAL_STATES = ImmutableList.of(
                RenderState.TRANSLUCENT_TRANSPARENCY,
                RenderState.DIFFUSE_LIGHTING_DISABLED,
                RenderState.SHADE_DISABLED,
                RenderState.DEFAULT_ALPHA,
                RenderState.DEPTH_LEQUAL,
                RenderState.CULL_ENABLED,
                RenderState.LIGHTMAP_ENABLED,
                RenderState.OVERLAY_DISABLED,
                RenderState.FOG,
                RenderState.NO_LAYERING,
                RenderState.MAIN_TARGET,
                RenderState.DEFAULT_TEXTURING,
                RenderState.COLOR_DEPTH_WRITE,
                RenderState.DEFAULT_LINE
        );
        SEE_THROUGH_STATES = ImmutableList.of(
                RenderState.TRANSLUCENT_TRANSPARENCY,
                RenderState.DIFFUSE_LIGHTING_DISABLED,
                RenderState.SHADE_DISABLED,
                RenderState.DEFAULT_ALPHA,
                RenderState.DEPTH_ALWAYS,
                RenderState.CULL_ENABLED,
                RenderState.LIGHTMAP_ENABLED,
                RenderState.OVERLAY_DISABLED,
                RenderState.FOG,
                RenderState.NO_LAYERING,
                RenderState.MAIN_TARGET,
                RenderState.DEFAULT_TEXTURING,
                RenderState.COLOR_WRITE,
                RenderState.DEFAULT_LINE
        );
    }

    private final int hashCode;

    /**
     * The OpenGL texture ID that contains this glyph image.
     */
    public final int textureName;

    private TextRenderType(int textureName) {
        super("modern_text",
                DefaultVertexFormats.POSITION_COLOR_TEX_LIGHTMAP,
                GL11.GL_QUADS, 256, false, true,
                () -> {
                    GENERAL_STATES.forEach(RenderState::setupRenderState);
                    RenderSystem.enableTexture();
                    RenderSystem.bindTexture(textureName);
                },
                () -> GENERAL_STATES.forEach(RenderState::clearRenderState));
        this.textureName = textureName;
        this.hashCode = Objects.hash(super.hashCode(), GENERAL_STATES, textureName);
    }

    //TODO see through type bug
    private TextRenderType(int textureName, String t) {
        super(t,
                DefaultVertexFormats.POSITION_COLOR_TEX_LIGHTMAP,
                GL11.GL_QUADS, 256, false, true,
                () -> {
                    SEE_THROUGH_STATES.forEach(RenderState::setupRenderState);
                    RenderSystem.enableTexture();
                    RenderSystem.bindTexture(textureName);
                },
                () -> SEE_THROUGH_STATES.forEach(RenderState::clearRenderState));
        this.textureName = textureName;
        this.hashCode = Objects.hash(super.hashCode(), SEE_THROUGH_STATES, textureName);
    }

    public static TextRenderType getOrCacheType(int textureName, boolean seeThrough) {
        if (seeThrough) {
            return SEE_THROUGH_TYPES.computeIfAbsent(textureName, n -> new TextRenderType(n, "modern_text_see_through"));
        }
        return TYPES.computeIfAbsent(textureName, TextRenderType::new);
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
