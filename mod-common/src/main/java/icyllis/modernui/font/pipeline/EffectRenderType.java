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

package icyllis.modernui.font.pipeline;

import com.google.common.collect.ImmutableList;
import icyllis.modernui.system.ModernUI;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.Objects;

public class EffectRenderType extends RenderType {

    private static final EffectRenderType INSTANCE    = new EffectRenderType();
    private static final EffectRenderType SEE_THROUGH = new EffectRenderType(ModernUI.MODID + ":text_effect_see_through");

    private static final ImmutableList<RenderState> STATES;
    private static final ImmutableList<RenderState> SEE_THROUGH_STATES;

    static {
        STATES = ImmutableList.of(
                RenderState.NO_TEXTURE,
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
                RenderState.NO_TEXTURE,
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

    private EffectRenderType() {
        super(ModernUI.MODID + ":text_effect",
                DefaultVertexFormats.POSITION_COLOR_LIGHTMAP,
                GL11.GL_QUADS, 256, false, true,
                () -> STATES.forEach(RenderState::setupRenderState),
                () -> STATES.forEach(RenderState::clearRenderState));
        this.hashCode = Objects.hash(super.hashCode(), STATES);
    }

    private EffectRenderType(String t) {
        super(t,
                DefaultVertexFormats.POSITION_COLOR_LIGHTMAP,
                GL11.GL_QUADS, 256, false, true,
                () -> SEE_THROUGH_STATES.forEach(RenderState::setupRenderState),
                () -> SEE_THROUGH_STATES.forEach(RenderState::clearRenderState));
        this.hashCode = Objects.hash(super.hashCode(), SEE_THROUGH_STATES);
    }

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
