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
import org.apache.commons.lang3.tuple.Pair;

public class ModernUI_Config {

    private static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    public static String preferredFontName;

    static {
        Pair<Client, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = pair.getLeft();
        CLIENT_SPEC = pair.getRight();
    }

    public static void registerClientConfig() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ModernUI_Config.CLIENT_SPEC);
    }

    public static void loadClientConfig() {
        preferredFontName = CLIENT.preferredFontName.get();
    }

    private static class Client {

        public final ForgeConfigSpec.ConfigValue<String> preferredFontName;

        private Client(ForgeConfigSpec.Builder builder) {
            builder.comment("Modern UI Fonts Config")
                    .push("fonts");

            preferredFontName = builder.comment("The name of font to use with highest priority.")
                    .define("preferredFontName", "");
        }
    }
}
