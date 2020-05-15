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

package icyllis.modernui.system;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nonnull;

public class ConfigManager {

    public static final Client CLIENT;
    private static final ForgeConfigSpec CLIENT_SPEC;

    public static final Common COMMON;
    private static final ForgeConfigSpec COMMON_SPEC;

    static {
        ForgeConfigSpec.Builder builder;

        builder = new ForgeConfigSpec.Builder();
        CLIENT = new Client(builder);
        CLIENT_SPEC = builder.build();

        builder = new ForgeConfigSpec.Builder();
        COMMON = new Common(builder);
        COMMON_SPEC = builder.build();
    }

    public static void register() {
        FMLPaths.getOrCreateGameRelativePath(FMLPaths.CONFIGDIR.get().resolve("ModernUI"), "ModernUI");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ConfigManager.CLIENT_SPEC, "ModernUI/client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigManager.COMMON_SPEC, "ModernUI/common.toml");
    }

    public static void load(ForgeConfigSpec spec) {
        if (spec == CLIENT_SPEC) {
            CLIENT.load();
        } else if (spec == COMMON_SPEC) {
            COMMON.load();
        }
    }

    public static class Client {

        public boolean keepRunningInScreen;
        public boolean blurScreenBackground;

        public String preferredFontName;
        public boolean enableGlobalFontRenderer;
        public boolean allowFontShadow;

        private final ForgeConfigSpec.BooleanValue keepRunningInScreenV;
        private final ForgeConfigSpec.BooleanValue blurScreenBackgroundV;

        private final ForgeConfigSpec.ConfigValue<String> preferredFontNameV;
        private final ForgeConfigSpec.BooleanValue enableGlobalFontRendererV;
        private final ForgeConfigSpec.BooleanValue allowFontShadowV;

        private Client(@Nonnull ForgeConfigSpec.Builder builder) {
            builder.comment("Screen Config")
                    .push("screen");

            keepRunningInScreenV = builder.comment("Keep game running no matter what screen is open. Modern UI's GUIs will never pause game.")
                    .define("keepGameRunning", true);
            blurScreenBackgroundV = builder.comment("Blur GUI background when opening a gui screen, this is incompatible with OptiFine's FXAA shader or some mods.")
                    .define("blurGuiBackground", true);

            builder.pop();

            builder.comment("Fonts Config")
                    .push("fonts");

            enableGlobalFontRendererV = builder.comment("Replace vanilla's font renderer to Modern UI's. This won't affect the font renderer which is used in Modern UI's GUIs.")
                    .define("enableGlobalRenderer", true);
            preferredFontNameV = builder.comment("The name of font to use with highest priority. The default font that included in Modern UI is always the alternative one to use.")
                    .define("preferredFontName", "");
            allowFontShadowV = builder.comment("Allow font renderer to draw text with shadow, set to false if you can't read the font clearly.")
                    .define("allowFontShadow", true);

            builder.pop();
        }

        private void load() {
            keepRunningInScreen = keepRunningInScreenV.get();
            blurScreenBackground = blurScreenBackgroundV.get();

            preferredFontName = preferredFontNameV.get();
            enableGlobalFontRenderer = enableGlobalFontRendererV.get();
            allowFontShadow = allowFontShadowV.get();
        }
    }

    public static class Common {

        private boolean enableDeveloperMode;
        private boolean enableLibOnlyMode;

        private final ForgeConfigSpec.BooleanValue enableDeveloperModeV;
        private final ForgeConfigSpec.BooleanValue enableLibOnlyModeV;

        public Common(@Nonnull ForgeConfigSpec.Builder builder) {
            builder.comment("Developer Config")
                    .push("developer");

            enableDeveloperModeV = builder.comment("For assisting developer to debug mod and edit modules in-game")
                    .define("enableDeveloperMode", false);
            enableLibOnlyModeV = builder.comment("Make Modern UI only as a library mod to work.")
                    .define("enableLibOnlyMode", false);

            builder.pop();
        }

        private void load() {
            enableDeveloperMode = enableDeveloperModeV.get();
            enableLibOnlyMode = enableLibOnlyModeV.get();
        }

        public boolean isEnableDeveloperMode() {
            return enableDeveloperMode;
        }

        public boolean isEnableLibOnlyMode() {
            return enableLibOnlyMode;
        }
    }
}
