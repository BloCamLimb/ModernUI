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

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CLIENT = new Client(builder);
        CLIENT_SPEC = builder.build();
    }

    public static void register() {
        FMLPaths.getOrCreateGameRelativePath(FMLPaths.CONFIGDIR.get().resolve("ModernUI"), "ModernUI");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ConfigManager.CLIENT_SPEC, "ModernUI/client.toml");
    }

    public static void load(ForgeConfigSpec spec) {
        if (spec == CLIENT_SPEC) {
            CLIENT.load();
        }
    }

    public static class Client {

        public String preferredFontName;

        private final ForgeConfigSpec.ConfigValue<String> preferredFontNameV;

        private Client(@Nonnull ForgeConfigSpec.Builder builder) {
            builder.comment("Fonts Config")
                    .push("fonts");

            preferredFontNameV = builder.comment("The name of font to use with highest priority.")
                    .define("preferredFontName", "");

            builder.pop();
        }

        private void load() {
            preferredFontName = preferredFontNameV.get();
        }
    }
}
