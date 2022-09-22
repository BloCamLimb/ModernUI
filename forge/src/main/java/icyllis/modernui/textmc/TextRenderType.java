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
import icyllis.modernui.textmc.mixin.AccessRenderBuffers;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static icyllis.modernui.ModernUI.*;

/**
 * Fast and modern text render type.
 */
public class TextRenderType extends RenderType {

    private static final ResourceLocation
            SHADER_RL = new ResourceLocation(ModernUI.ID, "rendertype_modern_text"),
            SHADER_GLOW_RL = new ResourceLocation(ModernUI.ID, "rendertype_modern_text_glow"),
            SHADER_SEE_THROUGH_RL = new ResourceLocation(ModernUI.ID, "rendertype_modern_text_see_through");

    static final ShaderStateShard
            RENDERTYPE_MODERN_TEXT = new ShaderStateShard(TextRenderType::getShader),
            RENDERTYPE_MODERN_TEXT_GLOW = new ShaderStateShard(TextRenderType::getShaderGlow),
            RENDERTYPE_MODERN_TEXT_SEE_THROUGH = new ShaderStateShard(TextRenderType::getShaderSeeThrough);

    private static volatile ShaderInstance sShader;
    private static volatile ShaderInstance sShaderGlow;
    private static volatile ShaderInstance sShaderSeeThrough;

    /**
     * Texture id to render type map
     */
    private static final Int2ObjectMap<TextRenderType> TYPES = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<TextRenderType> GLOW_TYPES = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<TextRenderType> SEE_THROUGH_TYPES = new Int2ObjectOpenHashMap<>();
    //private static final Int2ObjectMap<TextRenderType> POLYGON_OFFSET_TYPES = new Int2ObjectOpenHashMap<>();

    /**
     * Only the texture id is different, the rest state are same
     */
    private static final ImmutableList<RenderStateShard> STATES;
    private static final ImmutableList<RenderStateShard> GLOW_STATES;
    private static final ImmutableList<RenderStateShard> SEE_THROUGH_STATES;
    //private static final ImmutableList<RenderStateShard> POLYGON_OFFSET_STATES;

    private static TextRenderType sFirstType;
    private static final BufferBuilder sFirstBufferBuilder = new BufferBuilder(131072);

    private static TextRenderType sFirstGlowType;
    private static final BufferBuilder sFirstGlowBufferBuilder = new BufferBuilder(131072);

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
        return seeThrough ? SEE_THROUGH_TYPES.computeIfAbsent(texture, TextRenderType::makeSeeThroughType)
                : TYPES.computeIfAbsent(texture, TextRenderType::makeType);
    }

    @Nonnull
    public static TextRenderType getOrCreateGlow(int texture) {
        return GLOW_TYPES.computeIfAbsent(texture, TextRenderType::makeGlowType);
    }

    @Nonnull
    private static TextRenderType makeType(int texture) {
        TextRenderType renderType = new TextRenderType("modern_text", 256, () -> {
            STATES.forEach(RenderStateShard::setupRenderState);
            RenderSystem.enableTexture();
            RenderSystem.setShaderTexture(0, texture);
        }, () -> STATES.forEach(RenderStateShard::clearRenderState));
        if (sFirstType == null) {
            assert (TYPES.isEmpty());
            sFirstType = renderType;
            ((AccessRenderBuffers) Minecraft.getInstance().renderBuffers()).getFixedBuffers()
                    .put(renderType, sFirstBufferBuilder);
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
            assert (GLOW_TYPES.isEmpty());
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
     * There may be some issues here. We want to use a general atlas for Minecraft text rendering
     * which uses deferred rendering to improve performance, but if the first glyph is a color Emoji,
     * this goes against.
     */
    @Nullable
    public static TextRenderType firstType() {
        return sFirstType;
    }

    /**
     * Deferred rendering.
     */
    @Nullable
    public static TextRenderType firstGlowType() {
        return sFirstGlowType;
    }

    public static void clear() {
        if (sFirstType != null) {
            assert (!TYPES.isEmpty());
            if (!((AccessRenderBuffers) Minecraft.getInstance().renderBuffers()).getFixedBuffers()
                    .remove(sFirstType, sFirstBufferBuilder)) {
                throw new IllegalStateException();
            }
            sFirstType = null;
        }
        if (sFirstGlowType != null) {
            assert (!GLOW_TYPES.isEmpty());
            if (!((AccessRenderBuffers) Minecraft.getInstance().renderBuffers()).getFixedBuffers()
                    .remove(sFirstGlowType, sFirstGlowBufferBuilder)) {
                throw new IllegalStateException();
            }
            sFirstGlowType = null;
        }
        TYPES.clear();
        GLOW_TYPES.clear();
        SEE_THROUGH_TYPES.clear();
        sFirstBufferBuilder.clear();
        sFirstGlowBufferBuilder.clear();
    }

    public static ShaderInstance getShader() {
        return sShader;
    }

    public static ShaderInstance getShaderGlow() {
        return sShaderGlow;
    }

    public static ShaderInstance getShaderSeeThrough() {
        return sShaderSeeThrough;
    }

    /**
     * Load Modern UI text shaders for early text rendering. These shaders are loaded only once,
     * and cannot be overridden by other resource packs or reloaded. They will not be closed
     * unless Minecraft is quited.
     */
    public static synchronized void preloadShaders() {
        if (sShader != null) {
            return;
        }
        final VanillaPackResources resources = Minecraft.getInstance().getClientPackSource().getVanillaPack();
        final ResourceProvider provider = location -> {
            // don't worry
            @SuppressWarnings("resource") final InputStream stream = ModernUITextMC.class
                    .getResourceAsStream("/assets/" + location.getNamespace() + "/" + location.getPath());
            if (stream == null) {
                return Optional.of(new Resource(resources.getName(),
                        () -> resources.getResource(PackType.CLIENT_RESOURCES, location)));
            }
            return Optional.of(new Resource(ModernUI.ID, () -> stream));
        };
        try {
            sShader = new ShaderInstance(provider, SHADER_RL,
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
            sShaderGlow = new ShaderInstance(provider, SHADER_GLOW_RL,
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
            sShaderSeeThrough = new ShaderInstance(provider, SHADER_SEE_THROUGH_RL,
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
        } catch (IOException e) {
            throw new IllegalStateException("Bad text shaders", e);
        }
        LOGGER.info(MARKER, "Preloaded modern text shaders");
    }
}
