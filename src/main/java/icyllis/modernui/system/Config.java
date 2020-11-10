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

package icyllis.modernui.system;

import icyllis.modernui.font.ModernFontRenderer;
import icyllis.modernui.font.TrueTypeRenderer;
import icyllis.modernui.font.glyph.GlyphManager;
import icyllis.modernui.font.process.TextCacheProcessor;
import icyllis.modernui.graphics.BlurHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DownloadTerrainScreen;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Config {

    private static final Client CLIENT_CONFIG;
    private static final ForgeConfigSpec CLIENT_SPEC;

    private static final Common COMMON_CONFIG;
    private static final ForgeConfigSpec COMMON_SPEC;

    static {
        ForgeConfigSpec.Builder builder;

        builder = new ForgeConfigSpec.Builder();
        CLIENT_CONFIG = new Client(builder);
        CLIENT_SPEC = builder.build();

        builder = new ForgeConfigSpec.Builder();
        COMMON_CONFIG = new Common(builder);
        COMMON_SPEC = builder.build();
    }

    static void init() {
        FMLPaths.getOrCreateGameRelativePath(FMLPaths.CONFIGDIR.get().resolve(ModernUI.NAME_COMPACT), ModernUI.NAME_COMPACT);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, ModernUI.NAME_COMPACT + "/client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, ModernUI.NAME_COMPACT + "/common.toml");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(Config::reload);
    }

    static void reload(@Nonnull ModConfig.ModConfigEvent event) {
        ForgeConfigSpec spec = event.getConfig().getSpec();
        if (spec == CLIENT_SPEC) {
            CLIENT_CONFIG.load();
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Client config reloaded");
        } else if (spec == COMMON_SPEC) {
            COMMON_CONFIG.load();
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Common config reloaded");
        }
    }

    public static class Client {

        //public boolean keepRunningInScreen;

        //private final ForgeConfigSpec.BooleanValue keepRunningInScreenV;
        private final ForgeConfigSpec.BooleanValue blurEffect;
        private final ForgeConfigSpec.IntValue animationDuration;
        private final ForgeConfigSpec.IntValue blurRadius;
        private final ForgeConfigSpec.DoubleValue backgroundAlpha;

        private final ForgeConfigSpec.ConfigValue<List<? extends String>> blurBlacklist;

        private final ForgeConfigSpec.ConfigValue<String> preferredFont;
        private final ForgeConfigSpec.BooleanValue globalRenderer;
        private final ForgeConfigSpec.BooleanValue allowShadow;
        private final ForgeConfigSpec.BooleanValue antiAliasing;
        private final ForgeConfigSpec.BooleanValue highPrecision;
        private final ForgeConfigSpec.BooleanValue enableMipmap;
        private final ForgeConfigSpec.IntValue mipmapLevel;
        private final ForgeConfigSpec.IntValue resolutionLevel;
        private final ForgeConfigSpec.IntValue defaultFontSize;

        private Client(@Nonnull ForgeConfigSpec.Builder builder) {
            builder.comment("Screen Config")
                    .push("screen");

            /*keepRunningInScreenV = builder.comment("Keep game running no matter what screen is open. Modern UI's GUIs will never pause game.")
                    .define("keepGameRunning", true);*/
            animationDuration = builder.comment(
                    "The duration of screen background and blur radius animation in milliseconds. (0 = OFF)")
                    .defineInRange("animationDuration", 200, 0, 800);
            backgroundAlpha = builder.comment(
                    "Screen black background opacity in game, higher values will get darker.")
                    .defineInRange("backgroundAlpha", 0.4, 0, 0.8);

            blurEffect = builder.comment(
                    "Add blur effect to world renderer when opened, it is incompatible with OptiFine's FXAA shader or some mods.")
                    .define("blurEffect", true);
            blurRadius = builder.comment(
                    "The blur effect radius, higher values result in a small loss of performance.")
                    .defineInRange("blurRadius", 10, 2, 18);
            blurBlacklist = builder.comment(
                    "A list of GUI screen superclasses that won't activate blur effect when opened.")
                    .defineList("blurBlacklist", () -> {
                        List<String> list = new ArrayList<>();
                        list.add(ChatScreen.class.getName());
                        list.add(DownloadTerrainScreen.class.getName());
                        return list;
                    }, s -> true);


            builder.pop();

            builder.comment("General Config")
                    .push("general");

            builder.pop();

            builder.comment("Font Engine Config")
                    .push("font");

            globalRenderer = builder.comment(
                    "Replace the default font renderer of vanilla to that of Modern UI.")
                    .define("globalRenderer", true);
            preferredFont = builder.comment(
                    "The font with the highest priority to use, the built-in font is always the second choice.",
                    "This can be font name if you want to use fonts that installed on your PC, for instance: Microsoft YaHei",
                    "Or can be file path if you want to use external fonts, for instance: D:/Fonts/biliw.otf",
                    "Or can be resource location if you want to use fonts in resource packs, for instance: modernui:font/biliw.otf")
                    .define("preferredFont", "");
            allowShadow = builder.comment(
                    "Allow global font renderer to draw text with shadow, setting to false can improve performance.")
                    .define("allowShadow", true);
            antiAliasing = builder.comment(
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
                    .defineInRange("mipmapLevel", 4, 0, 4);
            resolutionLevel = builder.comment(
                    "The resolution level of font, higher levels would better work with high resolution monitors.",
                    "Reference: 1 (Standard, 1.5K Fullscreen), 2 (High, 2K~3K Fullscreen), 3 (Ultra, 4K Fullscreen)")
                    .defineInRange("resolutionLevel", 2, 1, 3);
            defaultFontSize = builder.comment(
                    "The default font size for texts with no size specified.")
                    .defineInRange("defaultFontSize", 16, 12, 20);

            builder.pop();

        }

        private void load() {
            //keepRunningInScreen = keepRunningInScreenV.get();

            BlurHandler.sBlurEffect = blurEffect.get();
            BlurHandler.sAnimationDuration = animationDuration.get();
            BlurHandler.sBlurRadius = blurRadius.get();
            BlurHandler.sBackgroundAlpha = backgroundAlpha.get().floatValue();
            BlurHandler.INSTANCE.loadBlacklist(blurBlacklist.get());

            TrueTypeRenderer.sGlobalRenderer = globalRenderer.get();
            GlyphManager.sPreferredFont = preferredFont.get();
            GlyphManager.sAntiAliasing = antiAliasing.get();
            GlyphManager.sHighPrecision = highPrecision.get();
            GlyphManager.sEnableMipmap = enableMipmap.get();
            GlyphManager.sMipmapLevel = mipmapLevel.get();
            GlyphManager.sResolutionLevel = resolutionLevel.get();
            TextCacheProcessor.sDefaultFontSize = defaultFontSize.get();
            ModernFontRenderer.sAllowFontShadow = allowShadow.get();
        }
    }

    private static boolean developerMode;

    public static boolean isDeveloperMode() {
        return developerMode;
    }

    public static class Common {

        private final ForgeConfigSpec.BooleanValue enableDeveloperModeV;
        //private final ForgeConfigSpec.IntValue workingDirLevelV;

        public Common(@Nonnull ForgeConfigSpec.Builder builder) {
            builder.comment("Developer Config")
                    .push("developer");

            enableDeveloperModeV = builder.comment("Whether to enable developer mode.")
                    .define("enableDeveloperMode", false);
            /*workingDirLevelV = builder.comment("The level of your working directory, determines the root directory of your project.")
                    .defineInRange("workingDirLevel", 1, 0, Integer.MAX_VALUE);*/

            builder.pop();
        }

        private void load() {
            if (enableDeveloperModeV.get()) {
                developerMode = true;
                return;
            }
            // get '/run' parent
            Path path = FMLPaths.GAMEDIR.get().getParent();
            // the root directory of your project
            File dir = path.toFile();
            String[] r = dir.list((file, name) -> name.equals("build.gradle"));
            developerMode = r != null && r.length > 0;
        }
    }
}
