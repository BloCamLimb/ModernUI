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

package icyllis.modernui.textmc;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import icyllis.modernui.ModernUI;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import javax.annotation.Nonnull;
import java.util.Objects;

public class EffectRenderType extends RenderType {

    private static final EffectRenderType INSTANCE = new EffectRenderType();
    private static final EffectRenderType SEE_THROUGH = new EffectRenderType(ModernUI.ID + ":text_effect_see_through");

    private static final ImmutableList<RenderStateShard> STATES;
    private static final ImmutableList<RenderStateShard> SEE_THROUGH_STATES;

    static {
        STATES = ImmutableList.of(
                NO_TEXTURE,
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
                NO_TEXTURE,
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
        super(ModernUI.ID + ":text_effect",
                DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
                VertexFormat.Mode.QUADS, 256, false, true,
                () -> STATES.forEach(RenderStateShard::setupRenderState),
                () -> STATES.forEach(RenderStateShard::clearRenderState));
        this.hashCode = Objects.hash(super.hashCode(), STATES);
    }

    private EffectRenderType(String t) {
        super(t,
                DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
                VertexFormat.Mode.QUADS, 256, false, true,
                () -> SEE_THROUGH_STATES.forEach(RenderStateShard::setupRenderState),
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
