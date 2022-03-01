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

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import icyllis.modernui.ModernUI;
import icyllis.modernui.core.ArchCore;
import icyllis.modernui.forge.MuiForgeApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.data.loading.DatagenModLoader;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nonnull;
import java.io.IOException;

@OnlyIn(Dist.CLIENT)
public final class ModernUITextMC {

    static Config CONFIG;
    private static ForgeConfigSpec CONFIG_SPEC;

    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().register(ModernUITextMC.class);

        FMLPaths.getOrCreateGameRelativePath(FMLPaths.CONFIGDIR.get().resolve(ModernUI.NAME_CPT), ModernUI.NAME_CPT);
        ModContainer mod = ModLoadingContext.get().getActiveContainer();

        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CONFIG = new Config(builder);
        CONFIG_SPEC = builder.build();
        mod.addConfig(new ModConfig(ModConfig.Type.CLIENT, CONFIG_SPEC, mod, ModernUI.NAME_CPT + "/text.toml"));

        FMLJavaModLoadingContext.get().getModEventBus().addListener(CONFIG::reload);

        if (!DatagenModLoader.isRunningDataGen()) {
            ((ReloadableResourceManager) Minecraft.getInstance().getResourceManager())
                    .registerReloadListener(
                            (ResourceManagerReloadListener) (manager) -> TextLayoutEngine.getInstance().reload()
                    );
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    static void setupClient(@Nonnull FMLClientSetupEvent event) {
        // preload text engine, note that this event is fired after client config first load
        // so that the typeface is loaded
        Minecraft.getInstance().execute(() -> {
            ModernUI.getInstance().getSelectedTypeface();
            TextLayoutEngine.getInstance().lookupVanillaNode(ModernUI.NAME_CPT);
        });
        MuiForgeApi.addOnWindowResizeListener((width, height, newScale, oldScale) -> {
            if (ArchCore.hasRenderThread() && newScale != oldScale) {
                TextLayoutEngine.getInstance().reload();
            }
        });
        MuiForgeApi.addOnDebugDumpListener(builder -> {
            builder.print("Text Layout Entries: ");
            builder.println(TextLayoutEngine.getInstance().countEntries());
        });
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    static void onShaderReload(@Nonnull RegisterShadersEvent event) {
        ResourceProvider provider = event.getResourceManager();
        try {
            event.registerShader(new ShaderInstance(provider, TextRenderType.SHADER_RL,
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), TextRenderType::setShader);
            event.registerShader(new ShaderInstance(provider, TextRenderType.SHADER_SEE_THROUGH_RL,
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), TextRenderType::setShaderSeeThrough);
        } catch (IOException e) {
            throw new RuntimeException("Bad shaders", e);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Config {

        //final ForgeConfigSpec.BooleanValue globalRenderer;
        private final ForgeConfigSpec.BooleanValue mAllowShadow;
        private final ForgeConfigSpec.BooleanValue mFixedResolution;

        //private final ForgeConfigSpec.BooleanValue antiAliasing;
        //private final ForgeConfigSpec.BooleanValue highPrecision;
        //private final ForgeConfigSpec.BooleanValue enableMipmap;
        //private final ForgeConfigSpec.IntValue mipmapLevel;
        //private final ForgeConfigSpec.IntValue resolutionLevel;
        //private final ForgeConfigSpec.IntValue defaultFontSize;

        private Config(@Nonnull ForgeConfigSpec.Builder builder) {
            builder.comment("Text Config")
                    .push("text");

            /*globalRenderer = builder.comment(
                    "Apply Modern UI font renderer (including text layouts) to the entire game rather than only " +
                            "Modern UI itself.")
                    .define("globalRenderer", true);*/
            mAllowShadow = builder.comment("Allow text renderer to draw text with shadow, setting to false can " +
                            "improve performance a bit.")
                    .define("allowShadow", true);
            mFixedResolution = builder.comment("Fixed resolution level. When the GUI scale increases, the resolution " +
                                    "level will not increase.",
                            "In this case, gui scale should be even numbers (2, 4, 6...), based on Minecraft GUI " +
                                    "system.",
                            "If your fonts are not really bitmap fonts, then you should keep this setting false.")
                    .define("fixedResolution", false);
            /*antiAliasing = builder.comment(
                    "Enable font anti-aliasing.")
                    .define("antiAliasing", true);
            highPrecision = builder.comment(
                    "Enable high precision rendering, this is very useful especially when the font is very small.")
                    .define("highPrecision", true);
            enableMipmap = builder.comment(
                    "Enable mipmap for font textures, this makes font will not be blurred when scaling down.")
                    .define("enableMipmap", true);
            mipmapLevel = builder.comment(
                    "The mipmap level for font textures.")
                    .defineInRange("mipmapLevel", 4, 0, 4);*/
            /*resolutionLevel = builder.comment(
                    "The resolution level of font, higher levels would better work with high resolution monitors.",
                    "Reference: 1 (Standard, 1.5K Fullscreen), 2 (High, 2K~3K Fullscreen), 3 (Ultra, 4K Fullscreen)",
                    "This should match your GUI scale. Scale -> Level: [1,2] -> 1; [3,4] -> 2; [5,) -> 3")
                    .defineInRange("resolutionLevel", 2, 1, 3);*/
            /*defaultFontSize = builder.comment(
                    "The default font size for texts with no size specified. (deprecated, to be removed)")
                    .defineInRange("defaultFontSize", 16, 12, 20);*/

            builder.pop();
        }

        void reload(@Nonnull ModConfigEvent event) {
            final IConfigSpec<?> spec = event.getConfig().getSpec();
            if (spec != CONFIG_SPEC) {
                return;
            }
            ModernFontRenderer.sAllowShadow = mAllowShadow.get();
            boolean fixedResolution = mFixedResolution.get();
            if (fixedResolution != TextLayoutEngine.sFixedResolution) {
                TextLayoutEngine.sFixedResolution = fixedResolution;
                Minecraft.getInstance().submit(TextLayoutEngine.getInstance()::reload);
            }
            /*GlyphManagerForge.sPreferredFont = preferredFont.get();
            GlyphManagerForge.sAntiAliasing = antiAliasing.get();
            GlyphManagerForge.sHighPrecision = highPrecision.get();
            GlyphManagerForge.sEnableMipmap = enableMipmap.get();
            GlyphManagerForge.sMipmapLevel = mipmapLevel.get();*/
            //GlyphManager.sResolutionLevel = resolutionLevel.get();
            //TextLayoutEngine.sDefaultFontSize = defaultFontSize.get();

            ModernUI.LOGGER.debug(ModernUI.MARKER, "Text config reloaded with {}", event.getClass().getSimpleName());
        }
    }
}
