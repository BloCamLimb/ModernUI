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

package icyllis.modernui.forge;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static icyllis.modernui.forge.ModernUIForge.*;

@ApiStatus.Internal
final class Config {

    static final Common COMMON;
    private static final ForgeConfigSpec COMMON_SPEC;

    static {
        ForgeConfigSpec.Builder builder;

        builder = new ForgeConfigSpec.Builder();
        COMMON = new Common(builder);
        COMMON_SPEC = builder.build();
    }

    static void init() {
        FMLPaths.getOrCreateGameRelativePath(FMLPaths.CONFIGDIR.get().resolve(NAME_CPT), NAME_CPT);
        ModContainer mod = ModLoadingContext.get().getActiveContainer();
        mod.addConfig(new C(ModConfig.Type.COMMON, COMMON_SPEC, mod, "common")); // dedicated server only
        FMLJavaModLoadingContext.get().getModEventBus().addListener(Config::reload);
    }

    static void reload(@Nonnull ModConfigEvent event) {
        final IConfigSpec<?> spec = event.getConfig().getSpec();
        if (spec == COMMON_SPEC) {
            COMMON.reload();
            LOGGER.debug(MARKER, "Common config reloaded with {}", event.getClass().getSimpleName());
        }
    }

    private static class C extends ModConfig {

        public C(Type type, ForgeConfigSpec spec, ModContainer container, String name) {
            super(type, spec, container, NAME_CPT + "/" + name + ".toml");
        }
    }

    // common config exists on physical client and physical server once game loaded
    // they are independent and do not sync with each other
    public static class Common {

        final ForgeConfigSpec.BooleanValue autoShutdown;
        final ForgeConfigSpec.ConfigValue<List<? extends String>> shutdownTimes;

        private Common(@Nonnull ForgeConfigSpec.Builder builder) {
            builder.comment("Auto Shutdown Config")
                    .push("autoShutdown");

            autoShutdown = builder.comment(
                            "Enable auto-shutdown for server.")
                    .define("enable", false);
            shutdownTimes = builder.comment(
                            "The time points of when server will auto-shutdown. Format: HH:mm.")
                    .defineList("times", () -> {
                        List<String> list = new ArrayList<>();
                        list.add("04:00");
                        list.add("16:00");
                        return list;
                    }, s -> true);

            builder.pop();
        }

        private void reload() {
            ServerHandler.INSTANCE.determineShutdownTime();
        }
    }
}
