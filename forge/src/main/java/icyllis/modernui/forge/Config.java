/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import icyllis.modernui.ModernUI;
import icyllis.modernui.text.FontAtlas;
import icyllis.modernui.text.GlyphManager;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@ApiStatus.Internal
final class Config {

    static Client CLIENT;
    private static ForgeConfigSpec CLIENT_SPEC;

    static final Common COMMON;
    private static final ForgeConfigSpec COMMON_SPEC;

    static final Server SERVER;
    private static final ForgeConfigSpec SERVER_SPEC;

    static {
        ForgeConfigSpec.Builder builder;

        if (FMLEnvironment.dist.isClient()) {
            builder = new ForgeConfigSpec.Builder();
            CLIENT = new Client(builder);
            CLIENT_SPEC = builder.build();
        }

        builder = new ForgeConfigSpec.Builder();
        COMMON = new Common(builder);
        COMMON_SPEC = builder.build();

        builder = new ForgeConfigSpec.Builder();
        SERVER = new Server(builder);
        SERVER_SPEC = builder.build();
    }

    static void init() {
        FMLPaths.getOrCreateGameRelativePath(FMLPaths.CONFIGDIR.get().resolve(ModernUI.NAME_CPT), ModernUI.NAME_CPT);
        ModContainer mod = ModLoadingContext.get().getActiveContainer();
        if (FMLEnvironment.dist.isClient()) {
            mod.addConfig(new C(ModConfig.Type.CLIENT, CLIENT_SPEC, mod, "client")); // client only
            mod.addConfig(new C(ModConfig.Type.COMMON, COMMON_SPEC, mod, "common")); // client only, but server logic
            mod.addConfig(new C(ModConfig.Type.SERVER, SERVER_SPEC, mod, "server")); // sync to client (local)
        } else {
            mod.addConfig(new C(ModConfig.Type.COMMON, COMMON_SPEC, mod, "common")); // dedicated server only
            mod.addConfig(new C(ModConfig.Type.SERVER, SERVER_SPEC, mod, "server")); // sync to client (network)
        }
        FMLJavaModLoadingContext.get().getModEventBus().addListener(Config::reload);
    }

    static void reload(@Nonnull ModConfigEvent event) {
        final IConfigSpec<?> spec = event.getConfig().getSpec();
        if (spec == CLIENT_SPEC) {
            /*try {
                ((com.electronwill.nightconfig.core.Config) ObfuscationReflectionHelper.findField(ForgeConfigSpec
                .class, "childConfig").get(CLIENT_SPEC)).set(Lists.newArrayList("tooltip", "frameColor"), "0xE8B4DF");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            CLIENT_SPEC.save();*/
            CLIENT.reload();
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Client config reloaded with {}", event.getClass().getSimpleName());
        } else if (spec == COMMON_SPEC) {
            COMMON.reload();
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Common config reloaded with {}", event.getClass().getSimpleName());
        } else if (spec == SERVER_SPEC) {
            SERVER.reload();
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Server config reloaded with {}", event.getClass().getSimpleName());
        }
    }

    private static class C extends ModConfig {

        private static final Toml _TOML = new Toml();

        public C(Type type, ForgeConfigSpec spec, ModContainer container, String name) {
            super(type, spec, container, ModernUI.NAME_CPT + "/" + name + ".toml");
        }

        @Override
        public ConfigFileTypeHandler getHandler() {
            return _TOML;
        }
    }

    private static class Toml extends ConfigFileTypeHandler {

        private Toml() {
        }

        // reroute it to the global config directory
        // see ServerLifecycleHooks, ModConfig.Type.SERVER
        private static Path reroute(@Nonnull Path configBasePath) {
            //noinspection SpellCheckingInspection
            if (configBasePath.endsWith("serverconfig")) {
                return FMLPaths.CONFIGDIR.get();
            }
            return configBasePath;
        }

        @Override
        public Function<ModConfig, CommentedFileConfig> reader(Path configBasePath) {
            return super.reader(reroute(configBasePath));
        }

        @Override
        public void unload(Path configBasePath, ModConfig config) {
            super.unload(reroute(configBasePath), config);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Client {

        final ForgeConfigSpec.BooleanValue blurEffect;
        final ForgeConfigSpec.IntValue animationDuration;
        final ForgeConfigSpec.IntValue blurRadius;
        final ForgeConfigSpec.ConfigValue<List<? extends String>> backgroundColor;
        final ForgeConfigSpec.BooleanValue tooltip;
        final ForgeConfigSpec.ConfigValue<List<? extends String>> tooltipFill;
        final ForgeConfigSpec.ConfigValue<List<? extends String>> tooltipStroke;
        final ForgeConfigSpec.BooleanValue ding;
        //private final ForgeConfigSpec.BooleanValue hudBars;

        private final ForgeConfigSpec.ConfigValue<List<? extends String>> blurBlacklist;

        private final ForgeConfigSpec.BooleanValue bitmapLike;
        private final ForgeConfigSpec.BooleanValue linearSampling;
        final ForgeConfigSpec.ConfigValue<List<? extends String>> fontFamily;

        private Client(@Nonnull ForgeConfigSpec.Builder builder) {
            builder.comment("Screen Config")
                    .push("screen");

            animationDuration = builder.comment(
                            "The duration of GUI background color and blur radius animation in milliseconds. (0 = OFF)")
                    .defineInRange("animationDuration", 200, 0, 800);
            backgroundColor = builder.comment(
                            "The GUI background color in 0xAARRGGBB format. Default value: 0x66000000",
                            "Can be one to four values representing top left, top right, bottom left and bottom right" +
                                    " color.",
                            "Multiple values produce a gradient effect, whereas one value produce a solid color.",
                            "When values is less than 4, the rest of the corner color will be replaced by the last " +
                                    "value.")
                    .defineList("backgroundColor", () -> {
                        List<String> list = new ArrayList<>();
                        list.add("0x66000000");
                        return list;
                    }, o -> true);

            blurEffect = builder.comment(
                            "Add blur effect to GUI background when opened, it is incompatible with OptiFine's FXAA " +
                                    "shader and some mods.")
                    .define("blurEffect", true);
            blurRadius = builder.comment(
                            "The 4-pass blur effect radius, higher values result in a small loss of performance.")
                    .defineInRange("blurRadius", 9, 2, 18);
            blurBlacklist = builder.comment(
                            "A list of GUI screen superclasses that won't activate blur effect when opened.")
                    .defineList("blurBlacklist", () -> {
                        List<String> list = new ArrayList<>();
                        list.add(ChatScreen.class.getName());
                        return list;
                    }, o -> true);

            builder.pop();

            builder.comment("Tooltip Config")
                    .push("tooltip");

            tooltip = builder.comment(
                            "Whether to enable Modern UI tooltip style, or back to vanilla style.")
                    .define("enable", true);
            tooltipFill = builder.comment(
                            "The tooltip FILL color in 0xAARRGGBB format. Default: 0xD4000000",
                            "Can be one to four values representing top left, top right, bottom left and bottom right" +
                                    " color.",
                            "Multiple values produce a gradient effect, whereas one value produce a solid color.",
                            "When values is less than 4, the rest of the corner color will be replaced by the last " +
                                    "value.")
                    .defineList("colorFill", () -> {
                        List<String> list = new ArrayList<>();
                        list.add("0xD4000000");
                        return list;
                    }, $ -> true);
            tooltipStroke = builder.comment(
                            "The tooltip STROKE color in 0xAARRGGBB format. Default: 0xF0AADCF0, 0xF0DAD0F4, ~ and " +
                                    "0xF0FFC3F7",
                            "Can be one to four values representing top left, top right, bottom left and bottom right" +
                                    " color.",
                            "Multiple values produce a gradient effect, whereas one value produce a solid color.",
                            "When values is less than 4, the rest of the corner color will be replaced by the last " +
                                    "value.")
                    .defineList("colorStroke", () -> {
                        List<String> list = new ArrayList<>();
                        list.add("0xF0AADCF0");
                        list.add("0xF0DAD0F4");
                        list.add("0xF0DAD0F4");
                        list.add("0xF0FFC3F7");
                        return list;
                    }, $ -> true);

            builder.pop();

            builder.comment("General Config")
                    .push("general");

            ding = builder.comment("Play a sound effect when the game is loaded.")
                    .define("ding", true);

            /*hudBars = builder.comment(
                    "Show additional HUD bars added by ModernUI on the bottom-left of the screen.")
                    .define("hudBars", false);*/

            builder.pop();


            builder.comment("Font Config")
                    .push("font");

            bitmapLike = builder.comment(
                            "Bitmap-like mode, anti-aliasing and high precision for glyph layouts are OFF.",
                            "A game restart is required to reload the setting properly.")
                    .define("bitmapLike", false);
            linearSampling = builder.comment(
                            "Bilinear sampling font textures with mipmaps, magnification sampling will be always " +
                                    "NEAREST.",
                            "If your fonts are not really bitmap fonts, then you should keep this setting true.",
                            "A game restart is required to reload the setting properly.")
                    .define("linearSampling", true);
            fontFamily = builder.comment(
                            "A set of font families with precedence relationships to determine the typeface to use.",
                            "TrueType and OpenTrue are supported. Each list element can be one of the following three" +
                                    " cases.",
                            "1) Font family name for those installed on your PC, for instance: Segoe UI",
                            "2) File path for external fonts on your PC, for instance: /usr/shared/fonts/x.otf",
                            "3) Resource location for those loaded with resource packs, for instance: " +
                                    "modernui:font/biliw.otf",
                            "Using pixelated (bitmap) fonts should consider other settings, and glyph size should be " +
                                    "16x.",
                            "This list is only read once when the game is loaded. A game restart is required to reload")
                    .defineList("fontFamily", () -> {
                        List<String> list = new ArrayList<>();
                        list.add("Segoe UI");
                        list.add("modernui:font/biliw.otf");
                        list.add("Noto Sans");
                        list.add("Open Sans");
                        list.add("San Francisco");
                        list.add("Calibri");
                        list.add("Microsoft YaHei UI");
                        list.add("STHeiti");
                        list.add("SimHei");
                        list.add("SansSerif");
                        return list;
                    }, s -> true);

            builder.pop();
        }

        void saveAndReload() {
            Util.ioPool().execute(() -> {
                CLIENT_SPEC.save();
                reload();
            });
        }

        private void reload() {
            BlurHandler.sBlurEffect = blurEffect.get();
            BlurHandler.sAnimationDuration = animationDuration.get();
            BlurHandler.sBlurRadius = blurRadius.get();

            List<? extends String> colors = backgroundColor.get();
            int color = 0x66000000;
            for (int i = 0; i < BlurHandler.sBackgroundColor.length; i++) {
                if (colors != null && i < colors.size()) {
                    String s = colors.get(i);
                    try {
                        color = Integer.parseUnsignedInt(s.substring(2), 16);
                    } catch (Exception e) {
                        ModernUI.LOGGER.error(ModernUI.MARKER,
                                "Wrong color format for setting background color: {}", s);
                    }
                }
                BlurHandler.sBackgroundColor[i] = color;
            }

            BlurHandler.INSTANCE.loadBlacklist(blurBlacklist.get());

            TooltipRenderer.sTooltip = tooltip.get();

            colors = tooltipFill.get();
            color = 0xD4000000;
            for (int i = 0; i < TooltipRenderer.sFillColor.length; i++) {
                if (colors != null && i < colors.size()) {
                    String s = colors.get(i);
                    try {
                        color = Integer.parseUnsignedInt(s.substring(2), 16);
                    } catch (Exception e) {
                        ModernUI.LOGGER.error(ModernUI.MARKER,
                                "Wrong color format for setting tooltip fill color: {}", s);
                    }
                }
                TooltipRenderer.sFillColor[i] = color;
            }
            colors = tooltipStroke.get();
            color = 0xF0AADCF0;
            for (int i = 0; i < TooltipRenderer.sStrokeColor.length; i++) {
                if (colors != null && i < colors.size()) {
                    String s = colors.get(i);
                    try {
                        color = Integer.parseUnsignedInt(s.substring(2), 16);
                    } catch (Exception e) {
                        ModernUI.LOGGER.error(ModernUI.MARKER,
                                "Wrong color format for setting tooltip stroke color: {}", s);
                    }
                }
                TooltipRenderer.sStrokeColor[i] = color;
            }

            UIManager.sPlaySoundOnLoaded = ding.get();
            //TestHUD.sBars = hudBars.get();

            GlyphManager.sBitmapLike = bitmapLike.get();
            FontAtlas.sLinearSampling = linearSampling.get();

            ModernUI.getInstance().getSelectedTypeface();
        }
    }

    // common config exists on physical client and physical server once game loaded
    // they are independent and do not sync with each other
    public static class Common {

        private final ForgeConfigSpec.BooleanValue developerMode;
        final ForgeConfigSpec.IntValue oneTimeEvents;

        final ForgeConfigSpec.BooleanValue autoShutdown;

        final ForgeConfigSpec.ConfigValue<List<? extends String>> shutdownTimes;

        private Common(@Nonnull ForgeConfigSpec.Builder builder) {
            builder.comment("Developer Config")
                    .push("developer");

            developerMode = builder.comment("Whether to enable developer mode.")
                    .define("enableDeveloperMode", false);
            oneTimeEvents = builder
                    .defineInRange("oneTimeEvents", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);

            builder.pop();

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
            ModernUIForge.sDeveloperMode = developerMode.get();
            ServerHandler.INSTANCE.determineShutdownTime();
        }
    }

    // server config is available when integrated server or dedicated server started
    // if on dedicated server, all config data will sync to remote client via network
    public static class Server {

        private Server(@Nonnull ForgeConfigSpec.Builder builder) {

        }

        private void reload() {

        }
    }
}
