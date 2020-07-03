/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
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

import icyllis.modernui.graphics.BlurHandler;
import icyllis.modernui.graphics.font.ModernFontRenderer;
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

    public static final  Client          CLIENT;
    private static final ForgeConfigSpec CLIENT_SPEC;

    public static final  Common          COMMON;
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

    static void init() {
        FMLPaths.getOrCreateGameRelativePath(FMLPaths.CONFIGDIR.get().resolve("ModernUI"), "ModernUI");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC, "ModernUI/client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC, "ModernUI/common.toml");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(Config::reload);
    }

    static void reload(@Nonnull ModConfig.ModConfigEvent event) {
        ForgeConfigSpec spec = event.getConfig().getSpec();
        if (spec == CLIENT_SPEC) {
            CLIENT.load();
            //ModernUI.LOGGER.debug(ModernUI.MARKER, "Client config loaded");
        } else if (spec == COMMON_SPEC) {
            COMMON.load();
            //ModernUI.LOGGER.debug(ModernUI.MARKER, "Common config loaded");
        }
    }

    public static class Client {

        //public boolean keepRunningInScreen;
        public boolean blurScreenBackground;

        public String  preferredFontName;
        public boolean enableGlobalFontRenderer;

        //private final ForgeConfigSpec.BooleanValue keepRunningInScreenV;
        private final ForgeConfigSpec.BooleanValue blurScreenBackgroundV;

        private final ForgeConfigSpec.ConfigValue<List<? extends String>> blurScreenExclusionsV;

        private final ForgeConfigSpec.ConfigValue<String> preferredFontNameV;
        private final ForgeConfigSpec.BooleanValue        enableGlobalFontRendererV;
        private final ForgeConfigSpec.BooleanValue        allowFontShadowV;

        private Client(@Nonnull ForgeConfigSpec.Builder builder) {
            builder.comment("Screen Config")
                    .push("screen");

            /*keepRunningInScreenV = builder.comment("Keep game running no matter what screen is open. Modern UI's GUIs will never pause game.")
                    .define("keepGameRunning", true);*/
            blurScreenBackgroundV = builder.comment("Blur GUI background when opening a gui screen, this is incompatible with OptiFine's FXAA shader or some mods.")
                    .define("blurGuiBackground", true);

            blurScreenExclusionsV = builder.comment("A list of gui screen superclasses that won't activate blur effect when opened.")
                    .defineList("blurGuiExclusions", ArrayList::new, s -> true);

            builder.pop();

            builder.comment("Fonts Config")
                    .push("fonts");

            enableGlobalFontRendererV = builder.comment("Replace font renderer of vanilla to that of Modern UI. This won't affect the font renderer used in Modern UI's gui screens.")
                    .define("enableGlobalRenderer", true);
            preferredFontNameV = builder.comment("The font name with the highest priority to use, the default one included in Modern UI is always the alternative one to use.")
                    .define("preferredFontName", "");
            allowFontShadowV = builder.comment("Allow font renderer to draw text with shadow, set to false if you can't read the font clearly.")
                    .define("allowFontShadow", true);

            builder.pop();
        }

        private void load() {
            //keepRunningInScreen = keepRunningInScreenV.get();
            blurScreenBackground = blurScreenBackgroundV.get();

            BlurHandler.INSTANCE.loadExclusions(blurScreenExclusionsV.get());

            preferredFontName = preferredFontNameV.get();
            enableGlobalFontRenderer = enableGlobalFontRendererV.get();
            ModernFontRenderer.sAllowFontShadow = allowFontShadowV.get();
        }
    }

    private static boolean developerMode;

    public static boolean isDeveloperMode() {
        return developerMode;
    }

    public static class Common {

        //private final ForgeConfigSpec.BooleanValue enableDeveloperModeV;
        //private final ForgeConfigSpec.IntValue workingDirLevelV;

        public Common(@Nonnull ForgeConfigSpec.Builder builder) {
            builder.comment("Developer Config")
                    .push("developer");

            /*enableDeveloperModeV = builder.comment("For assisting developer to debug mod and edit modules in-game")
                    .define("enableDeveloperMode", false);
            workingDirLevelV = builder.comment("The level of your working directory, determines the root directory of your project.")
                    .defineInRange("workingDirLevel", 1, 0, Integer.MAX_VALUE);*/

            builder.pop();
        }

        private void load() {
            if (!developerMode) {
                Path path = FMLPaths.GAMEDIR.get();
                // determines the root directory of your project
                for (int i = 0; i <= 1; i++) {
                    File dir = path.toFile();
                    String[] r = dir.list((f, n) -> n.equals("build.gradle"));
                    if (r != null && r.length > 0) {
                        developerMode = true;
                        ModernUI.LOGGER.debug(ModernUI.MARKER, "Enables Developer Mode");
                        break;
                    }
                    path = path.getParent();
                }
            }
        }
    }
}