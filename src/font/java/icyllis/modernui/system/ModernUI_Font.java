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

import icyllis.modernui.api.ModernUI_API;
import icyllis.modernui.font.TrueTypeRenderer;
import icyllis.modernui.font.compat.ModernFontRenderer;
import icyllis.modernui.font.glyph.GlyphManager;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

/**
 * We allow font renderer to be a standalone mod
 */
@Mod(ModernUI_Font.MODID)
public class ModernUI_Font {

    public static final String MODID = "modernuifont";

    public static final Logger LOGGER = LogManager.getLogger(ModernUI_API.MOD_NAME_COMPACT);

    public static final  Config          CONFIG;
    private static final ForgeConfigSpec CONFIG_SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CONFIG = new Config(builder);
        CONFIG_SPEC = builder.build();
    }

    public ModernUI_Font() {
        FMLPaths.getOrCreateGameRelativePath(FMLPaths.CONFIGDIR.get().resolve(ModernUI_API.MOD_NAME_COMPACT), ModernUI_API.MOD_NAME_COMPACT);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CONFIG_SPEC, ModernUI_API.MOD_NAME_COMPACT + "/font.toml");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ModernUI_Font::reload);
    }

    static void reload(@Nonnull ModConfig.ModConfigEvent event) {
        ForgeConfigSpec spec = event.getConfig().getSpec();
        if (spec == CONFIG_SPEC) {
            CONFIG.load();
        }
    }

    public static class Config {

        private final ForgeConfigSpec.ConfigValue<String> preferredName;
        private final ForgeConfigSpec.BooleanValue        globalRenderer;
        private final ForgeConfigSpec.BooleanValue        allowShadow;
        private final ForgeConfigSpec.BooleanValue        antiAliasing;
        private final ForgeConfigSpec.BooleanValue        highPrecision;
        private final ForgeConfigSpec.BooleanValue        enableMipmap;

        private Config(@Nonnull ForgeConfigSpec.Builder builder) {
            builder.comment("General Config")
                    .push("general");

            globalRenderer = builder.comment(
                    "Replace the default font renderer of vanilla to that of Modern UI. This doesn't affect the font renderer in Modern UI.")
                    .define("globalRenderer", true);

            builder.pop();

            builder.comment("Font Config")
                    .push("font");

            preferredName = builder.comment(
                    "The font name with the highest priority to use, the built-in font is always the alternative one to use.")
                    .define("preferredName", "");
            allowShadow = builder.comment(
                    "Allow font renderer to draw text with shadow, set to false if you can't read the font clearly.")
                    .define("allowShadow", true);
            antiAliasing = builder.comment(
                    "Enable font anti-aliasing.")
                    .define("antiAliasing", true);
            highPrecision = builder.comment(
                    "Enable high precision rendering, this is very useful especially when the font size is very small.")
                    .define("highPrecision", true);
            enableMipmap = builder.comment(
                    "Enable mipmap for font textures, this makes font will not be blurred when scaling.")
                    .define("enableMipmap", true);

            builder.pop();
        }

        private void load() {
            TrueTypeRenderer.sGlobalRenderer = globalRenderer.get();
            TrueTypeRenderer.sPreferredFontName = preferredName.get();
            GlyphManager.sAntiAliasing = antiAliasing.get();
            GlyphManager.sHighPrecision = highPrecision.get();
            GlyphManager.sEnableMipmap = enableMipmap.get();
            ModernFontRenderer.sAllowFontShadow = allowShadow.get();
        }
    }
}
