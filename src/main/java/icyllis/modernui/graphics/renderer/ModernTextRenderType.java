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

package icyllis.modernui.graphics.renderer;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.Objects;

public class ModernTextRenderType extends RenderType {

    public static final RenderType MODERN_TEXT;

    static {
        ImmutableList<RenderState> renderStates;
        renderStates = ImmutableList.of(
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
        MODERN_TEXT = new ModernTextRenderType(renderStates);
    }

    private final int hashCode;

    private ModernTextRenderType(ImmutableList<RenderState> list) {
        super("modern_text",
                DefaultVertexFormats.POSITION_COLOR_TEX_LIGHTMAP,
                GL11.GL_QUADS, 256, false, true,
                () -> list.forEach(RenderState::setupRenderState),
                () -> list.forEach(RenderState::clearRenderState));
        this.hashCode = Objects.hash(super.hashCode(), list, 1);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

}
