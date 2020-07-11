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

package icyllis.modernui.font.compat;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.Map;
import java.util.Objects;

public class TextRenderType extends RenderType {

    private static final Map<Integer, RenderType> TYPES = new Int2ObjectArrayMap<>();

    private static final ImmutableList<RenderState> GENERAL_STATES;

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
                RenderState.DEFAULT_LINE);
    }

    private final int hashCode;

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
        this.hashCode = Objects.hash(super.hashCode(), GENERAL_STATES, textureName);
    }

    public static RenderType getOrCacheType(int textureName) {
        return TYPES.computeIfAbsent(textureName, TextRenderType::new);
    }

    public static boolean removeCachedType(int textureName) {
        return TYPES.remove(textureName) != null;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }
}
