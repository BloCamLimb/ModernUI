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
import com.mojang.blaze3d.vertex.*;
import icyllis.modernui.ModernUI;
import icyllis.modernui.forge.ModernUIForge;
import icyllis.modernui.graphics.font.GLFontAtlas;
import icyllis.modernui.graphics.opengl.GLCore;
import icyllis.modernui.textmc.mixin.AccessRenderBuffers;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Optional;

import static icyllis.modernui.ModernUI.*;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.*;
import static org.lwjgl.opengl.GL33C.glSamplerParameteri;

/**
 * Fast and modern text render type.
 */
public class TextRenderType extends RenderType {

    private static volatile ShaderInstance sShader;
    private static volatile ShaderInstance sShaderDF;
    private static volatile ShaderInstance sShaderGlow;
    private static volatile ShaderInstance sShaderSeeThrough;

    static final ShaderStateShard
            RENDERTYPE_MODERN_TEXT = new ShaderStateShard(TextRenderType::getShader),
            RENDERTYPE_MODERN_TEXT_DF = new ShaderStateShard(TextRenderType::getShaderDF),
            RENDERTYPE_MODERN_TEXT_GLOW = new ShaderStateShard(TextRenderType::getShaderGlow),
            RENDERTYPE_MODERN_TEXT_SEE_THROUGH = new ShaderStateShard(TextRenderType::getShaderSeeThrough);

    /**
     * Only the texture id is different, the rest state are same
     */
    private static final ImmutableList<RenderStateShard> STATES;
    private static final ImmutableList<RenderStateShard> DF_STATES;
    private static final ImmutableList<RenderStateShard> GLOW_STATES;
    private static final ImmutableList<RenderStateShard> SEE_THROUGH_STATES;
    //private static final ImmutableList<RenderStateShard> POLYGON_OFFSET_STATES;

    /**
     * Texture id to render type map
     */
    private static final Int2ObjectMap<TextRenderType> sTypes = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<TextRenderType> sDfTypes = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<TextRenderType> sGlowTypes = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<TextRenderType> sSeeThroughTypes = new Int2ObjectOpenHashMap<>();
    //private static final Int2ObjectMap<TextRenderType> sPolygonOffsetTypes = new Int2ObjectOpenHashMap<>();

    private static TextRenderType sFirstDfType;
    private static final BufferBuilder sFirstDfBufferBuilder = new BufferBuilder(131072);

    private static TextRenderType sFirstGlowType;
    private static final BufferBuilder sFirstGlowBufferBuilder = new BufferBuilder(131072);

    /**
     * Dynamic value controlling whether to use distance field at current stage.
     * Distance field only benefits in 3D world, it looks bad in 2D UI.
     *
     * @see icyllis.modernui.textmc.mixin.MixinGameRenderer
     */
    public static boolean sUseDistanceField;
    // DF requires MAG linear sampling
    private static int sLinearFontSampler;

    static {
        STATES = ImmutableList.of(
                RENDERTYPE_MODERN_TEXT,
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
        DF_STATES = ImmutableList.of(
                RENDERTYPE_MODERN_TEXT_DF,
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
        GLOW_STATES = ImmutableList.of(
                RENDERTYPE_MODERN_TEXT_GLOW,
                LIGHTNING_TRANSPARENCY,
                LEQUAL_DEPTH_TEST,
                CULL,
                LIGHTMAP,
                NO_OVERLAY,
                NO_LAYERING,
                MAIN_TARGET,
                DEFAULT_TEXTURING,
                COLOR_WRITE, // no depth write, translucent objects will be clipped
                DEFAULT_LINE
        );
        SEE_THROUGH_STATES = ImmutableList.of(
                RENDERTYPE_MODERN_TEXT_SEE_THROUGH,
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
        /*POLYGON_OFFSET_STATES = ImmutableList.of(
                RENDERTYPE_MODERN_TEXT,
                TRANSLUCENT_TRANSPARENCY,
                LEQUAL_DEPTH_TEST,
                CULL,
                LIGHTMAP,
                NO_OVERLAY,
                POLYGON_OFFSET_LAYERING,
                MAIN_TARGET,
                DEFAULT_TEXTURING,
                COLOR_DEPTH_WRITE,
                DEFAULT_LINE
        );*/
    }

    private TextRenderType(String name, int bufferSize, Runnable setupState, Runnable clearState) {
        super(name, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS,
                bufferSize, false, true, setupState, clearState);
    }

    @Nonnull
    public static TextRenderType getOrCreate(int texture, boolean seeThrough) {
        return seeThrough ? sSeeThroughTypes.computeIfAbsent(texture, TextRenderType::makeSeeThroughType) :
                sUseDistanceField ? sDfTypes.computeIfAbsent(texture, TextRenderType::makeDfType) :
                        sTypes.computeIfAbsent(texture, TextRenderType::makeType);
    }

    @Nonnull
    public static TextRenderType getOrCreateGlow(int texture) {
        return sGlowTypes.computeIfAbsent(texture, TextRenderType::makeGlowType);
    }

    @Nonnull
    private static TextRenderType makeType(int texture) {
        return new TextRenderType("modern_text", 256, () -> {
            STATES.forEach(RenderStateShard::setupRenderState);
            RenderSystem.enableTexture();
            RenderSystem.setShaderTexture(0, texture);
        }, () -> STATES.forEach(RenderStateShard::clearRenderState));
    }

    @Nonnull
    private static TextRenderType makeDfType(int texture) {
        TextRenderType renderType = new TextRenderType("modern_text_df", 256, () -> {
            DF_STATES.forEach(RenderStateShard::setupRenderState);
            RenderSystem.enableTexture();
            RenderSystem.setShaderTexture(0, texture);
            GLCore.glBindSampler(0, sLinearFontSampler);
        }, () -> {
            DF_STATES.forEach(RenderStateShard::clearRenderState);
            GLCore.glBindSampler(0, 0);
        });
        if (sFirstDfType == null) {
            assert (sDfTypes.isEmpty());
            sFirstDfType = renderType;
            ((AccessRenderBuffers) Minecraft.getInstance().renderBuffers()).getFixedBuffers()
                    .put(renderType, sFirstDfBufferBuilder);
        }
        return renderType;
    }

    @Nonnull
    private static TextRenderType makeGlowType(int texture) {
        TextRenderType renderType = new TextRenderType("modern_text_glow", 256, () -> {
            GLOW_STATES.forEach(RenderStateShard::setupRenderState);
            RenderSystem.enableTexture();
            RenderSystem.setShaderTexture(0, texture);
        }, () -> GLOW_STATES.forEach(RenderStateShard::clearRenderState));
        if (sFirstGlowType == null) {
            assert (sGlowTypes.isEmpty());
            sFirstGlowType = renderType;
            ((AccessRenderBuffers) Minecraft.getInstance().renderBuffers()).getFixedBuffers()
                    .put(renderType, sFirstGlowBufferBuilder);
        }
        return renderType;
    }

    @Nonnull
    private static TextRenderType makeSeeThroughType(int texture) {
        return new TextRenderType("modern_text_see_through", 256, () -> {
            SEE_THROUGH_STATES.forEach(RenderStateShard::setupRenderState);
            RenderSystem.enableTexture();
            RenderSystem.setShaderTexture(0, texture);
        }, () -> SEE_THROUGH_STATES.forEach(RenderStateShard::clearRenderState));
    }

    @Nonnull
    public static TextRenderType getOrCreate(int texture, Font.DisplayMode mode) {
        throw new IllegalStateException();
    }

    /**
     * Deferred rendering.
     * <p>
     * We use a general atlas for deferred rendering to improve performance.
     */
    @Nullable
    public static TextRenderType firstDfType() {
        return sFirstDfType;
    }

    /**
     * Similarly, but for glowing signs.
     *
     * @see #firstDfType()
     */
    @Nullable
    public static TextRenderType firstGlowType() {
        return sFirstGlowType;
    }

    public static void clear() {
        if (sFirstDfType != null) {
            assert (!sDfTypes.isEmpty());
            var access = (AccessRenderBuffers) Minecraft.getInstance().renderBuffers();
            if (!access.getFixedBuffers().remove(sFirstDfType, sFirstDfBufferBuilder)) {
                throw new IllegalStateException();
            }
            sFirstDfType = null;
        }
        if (sFirstGlowType != null) {
            assert (!sGlowTypes.isEmpty());
            var access = (AccessRenderBuffers) Minecraft.getInstance().renderBuffers();
            if (!access.getFixedBuffers().remove(sFirstGlowType, sFirstGlowBufferBuilder)) {
                throw new IllegalStateException();
            }
            sFirstGlowType = null;
        }
        sTypes.clear();
        sGlowTypes.clear();
        sSeeThroughTypes.clear();
        sFirstDfBufferBuilder.clear();
        sFirstGlowBufferBuilder.clear();
    }

    public static ShaderInstance getShader() {
        return sShader;
    }

    public static ShaderInstance getShaderDF() {
        return sShaderDF;
    }

    public static ShaderInstance getShaderGlow() {
        return sShaderGlow;
    }

    public static ShaderInstance getShaderSeeThrough() {
        return sShaderSeeThrough;
    }

    /**
     * Preload Modern UI text shaders for early text rendering. These shaders are loaded only once
     * and cannot be overridden by other resource packs or reloaded.
     */
    public static synchronized void preloadShaders() {
        if (sShader != null) {
            return;
        }
        final var fallback = Minecraft.getInstance().getClientPackSource().getVanillaPack().asProvider();
        final var provider = (ResourceProvider) location -> {
            // don't worry, ShaderInstance ctor will close it
            @SuppressWarnings("resource") final var stream = ModernUITextMC.class
                    .getResourceAsStream("/assets/" + location.getNamespace() + "/" + location.getPath());
            if (stream == null) {
                // fallback to vanilla
                return fallback.getResource(location);
            }
            return Optional.of(new Resource(ModernUI.ID, () -> stream));
        };
        try {
            sShader = new ShaderInstance(provider,
                    ModernUIForge.location("rendertype_modern_text"),
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
            sShaderDF = new ShaderInstance(provider,
                    ModernUIForge.location("rendertype_modern_text_df"),
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
            sShaderGlow = new ShaderInstance(provider,
                    ModernUIForge.location("rendertype_modern_text_glow"),
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
            sShaderSeeThrough = new ShaderInstance(provider,
                    ModernUIForge.location("rendertype_modern_text_see_through"),
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
            sLinearFontSampler = GLCore.glGenSamplers();
            glSamplerParameteri(sLinearFontSampler, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glSamplerParameteri(sLinearFontSampler, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glSamplerParameteri(sLinearFontSampler, GL_TEXTURE_MIN_LOD, 0);
            glSamplerParameteri(sLinearFontSampler, GL_TEXTURE_MAX_LOD, GLFontAtlas.MIPMAP_LEVEL);
        } catch (IOException e) {
            throw new IllegalStateException("Bad text shaders", e);
        }
        LOGGER.info(MARKER, "Preloaded modern text shaders");
    }
}
