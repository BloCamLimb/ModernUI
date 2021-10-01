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
import icyllis.modernui.screen.BlurHandler;
import icyllis.modernui.screen.UIManager;
import icyllis.modernui.test.TestHUD;
import icyllis.modernui.text.FontAtlas;
import icyllis.modernui.text.GlyphManager;
import icyllis.modernui.textmc.ModernFontRenderer;
import icyllis.modernui.textmc.TextLayoutEngine;
import net.minecraft.client.Minecraft;
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
public final class Config {

    static final Client CLIENT;
    private static final ForgeConfigSpec CLIENT_SPEC;

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
        } else {
            CLIENT = null;
            CLIENT_SPEC = null;
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
            mod.addConfig(new Cfg(Cfg.Type.CLIENT, CLIENT_SPEC, mod, "client"));
            mod.addConfig(new Cfg(Cfg.Type.COMMON, COMMON_SPEC, mod, "common"));
            mod.addConfig(new Cfg(Cfg.Type.SERVER, SERVER_SPEC, mod, "server"));
        } else {
            mod.addConfig(new Cfg(Cfg.Type.COMMON, COMMON_SPEC, mod, "server")); // include dedicated server only
            mod.addConfig(new Cfg(Cfg.Type.SERVER, SERVER_SPEC, mod, "shared"));
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
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Client config reloaded with {}", event.getClass().getName());
        } else if (spec == COMMON_SPEC) {
            COMMON.reload();
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Common config reloaded with {}", event.getClass().getName());
        } else if (spec == SERVER_SPEC) {
            SERVER.reload();
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Server config reloaded with {}", event.getClass().getName());
        }
    }

    private static class Cfg extends ModConfig {

        private static final Toml _TOML = new Toml();

        public Cfg(Type type, ForgeConfigSpec spec, ModContainer container, String name) {
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

        private final ForgeConfigSpec.BooleanValue blurEffect;
        private final ForgeConfigSpec.IntValue animationDuration;
        private final ForgeConfigSpec.IntValue blurRadius;
        private final ForgeConfigSpec.ConfigValue<List<? extends String>> backgroundColor;
        private final ForgeConfigSpec.BooleanValue tooltip;
        private final ForgeConfigSpec.ConfigValue<String> tooltipColor;
        private final ForgeConfigSpec.BooleanValue ding;
        //private final ForgeConfigSpec.BooleanValue hudBars;

        private final ForgeConfigSpec.ConfigValue<List<? extends String>> blurBlacklist;

        //final ForgeConfigSpec.BooleanValue globalRenderer;
        private final ForgeConfigSpec.BooleanValue allowShadow;
        private final ForgeConfigSpec.BooleanValue bitmapLike;
        private final ForgeConfigSpec.BooleanValue fixedResolution;
        private final ForgeConfigSpec.BooleanValue linearSampling;
        //private final ForgeConfigSpec.BooleanValue antiAliasing;
        //private final ForgeConfigSpec.BooleanValue highPrecision;
        //private final ForgeConfigSpec.BooleanValue enableMipmap;
        //private final ForgeConfigSpec.IntValue mipmapLevel;
        //private final ForgeConfigSpec.IntValue resolutionLevel;
        //private final ForgeConfigSpec.IntValue defaultFontSize;
        final ForgeConfigSpec.ConfigValue<List<? extends String>> fontFamily;

        private Client(@Nonnull ForgeConfigSpec.Builder builder) {
            builder.comment("Screen Config")
                    .push("screen");

            animationDuration = builder.comment(
                    "The duration of GUI background color and blur radius animation in milliseconds. (0 = OFF)")
                    .defineInRange("animationDuration", 200, 0, 800);
            backgroundColor = builder.comment(
                    "The GUI background color in world in 0xAARRGGBB format.",
                    "Can be one to four values representing top left, top right, bottom right, bottom left color.",
                    "Multiple values produce a gradient effect, whereas one value has only one color.",
                    "When values is less than 4, the rest of the corner color will be replaced by the last value.")
                    .defineList("backgroundColor", () -> {
                        List<String> list = new ArrayList<>();
                        list.add("0x66000000");
                        return list;
                    }, s -> true);

            blurEffect = builder.comment(
                    "Add blur effect to world renderer when opened, it is incompatible with OptiFine's FXAA shader or" +
                            " some mods.")
                    .define("blurEffect", true);
            blurRadius = builder.comment(
                    "The blur effect radius, higher values result in a small loss of performance.")
                    .defineInRange("blurRadius", 10, 2, 18);
            blurBlacklist = builder.comment(
                    "A list of GUI screen superclasses that won't activate blur effect when opened.")
                    .defineList("blurBlacklist", () -> {
                        List<String> list = new ArrayList<>();
                        list.add(ChatScreen.class.getName());
                        return list;
                    }, s -> true);

            builder.pop();

            builder.comment("Tooltip Config")
                    .push("tooltip");

            tooltip = builder.comment(
                    "Enable Modern UI's tooltip style.")
                    .define("enable", true);

            tooltipColor = builder.comment(
                    "The tooltip frame color. Format: 0xRRGGBB. Default value: 0xAADCF0")
                    .define("frameColor", "0xAADCF0");

            builder.pop();

            builder.comment("General Config")
                    .push("general");

            ding = builder.comment(
                    "Play a sound effect when the game is loaded.")
                    .define("ding", true);

            /*hudBars = builder.comment(
                    "Show additional HUD bars added by ModernUI on the bottom-left of the screen.")
                    .define("hudBars", false);*/

            builder.pop();

            builder.comment("Font Config")
                    .push("font");

            /*globalRenderer = builder.comment(
                    "Apply Modern UI font renderer (including text layouts) to the entire game rather than only " +
                            "Modern UI itself.")
                    .define("globalRenderer", true);*/
            allowShadow = builder.comment(
                    "Allow font renderer to draw text with shadow, setting to false can improve performance a bit.")
                    .define("allowShadow", true);
            bitmapLike = builder.comment(
                    "Bitmap-like mode, anti-aliasing and high precision for glyph layouts are OFF.",
                    "A game restart is required to reload the setting properly.")
                    .define("bitmapLike", false);
            fixedResolution = builder.comment(
                    "Fixed resolution level. When the GUI scale increases, the resolution level will not increase.",
                    "In this case, gui scale should be even numbers (2, 4, 6...), based on Minecraft GUI system.",
                    "If your fonts are not really bitmap fonts, then you should keep this setting false.")
                    .define("fixedResolution", false);
            linearSampling = builder.comment(
                    "Bilinear sampling font textures with mipmaps, magnification sampling will be always NEAREST.",
                    "If your fonts are not really bitmap fonts, then you should keep this setting true.",
                    "A game restart is required to reload the setting properly.")
                    .define("linearSampling", true);
            fontFamily = builder.comment(
                    "A set of font families with precedence relationships to determine the typeface to use.",
                    "TrueType and OpenTrue are supported. Each list element can be one of the following three cases.",
                    "1) Font family name for those installed on your PC, for instance: Segoe UI",
                    "2) File path for external fonts on your PC, for instance: /usr/shared/fonts/x.otf",
                    "3) Resource location for those loaded with resource packs, for instance: modernui:font/biliw.otf",
                    "Using pixelated (bitmap) fonts should consider other settings, and glyph size should be 16x.",
                    "This list is only read once when the game is loaded. A game restart is required to reload")
                    .defineList("fontFamily", () -> {
                        List<String> list = new ArrayList<>();
                        list.add("modernui:font/biliw.otf");
                        list.add("Segoe UI");
                        list.add("SansSerif");
                        return list;
                    }, s -> true);
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

        private void reload() {
            BlurHandler.sBlurEffect = blurEffect.get();
            BlurHandler.sAnimationDuration = animationDuration.get();
            BlurHandler.sBlurRadius = blurRadius.get();

            List<? extends String> colors = backgroundColor.get();
            int color = 0x66000000;
            for (int i = 0; i < 4; i++) {
                if (colors != null && i < colors.size()) {
                    try {
                        color = Integer.valueOf(colors.get(i).substring(2), 16);
                    } catch (Exception e) {
                        ModernUI.LOGGER.error(ModernUI.MARKER, "Wrong color format for setting background color: {}",
                                tooltipColor, e);
                    }
                }
                BlurHandler.sBackgroundColor[i] = color;
            }

            BlurHandler.INSTANCE.loadBlacklist(blurBlacklist.get());

            TestHUD.sTooltip = tooltip.get();
            String tooltipColor = this.tooltipColor.get();
            try {
                int i = Integer.valueOf(tooltipColor.substring(2), 16);
                TestHUD.sTooltipR = i >> 16 & 0xff;
                TestHUD.sTooltipG = i >> 8 & 0xff;
                TestHUD.sTooltipB = i & 0xff;
            } catch (Exception e) {
                ModernUI.LOGGER.error(ModernUI.MARKER, "Wrong color format for setting tooltip color: {}",
                        tooltipColor, e);
            }
            UIManager.sPlaySoundOnLoaded = ding.get();
            //TestHUD.sBars = hudBars.get();

            ModernFontRenderer.sAllowShadow = allowShadow.get();
            GlyphManager.sBitmapLike = bitmapLike.get();
            boolean fixed = fixedResolution.get();
            if (fixed != TextLayoutEngine.sFixedResolution) {
                TextLayoutEngine.sFixedResolution = fixed;
                Minecraft.getInstance().submit(TextLayoutEngine.getInstance()::reload);
            }
            FontAtlas.sLinearSampling = linearSampling.get();
            /*GlyphManagerForge.sPreferredFont = preferredFont.get();
            GlyphManagerForge.sAntiAliasing = antiAliasing.get();
            GlyphManagerForge.sHighPrecision = highPrecision.get();
            GlyphManagerForge.sEnableMipmap = enableMipmap.get();
            GlyphManagerForge.sMipmapLevel = mipmapLevel.get();*/
            //GlyphManager.sResolutionLevel = resolutionLevel.get();
            //TextLayoutEngine.sDefaultFontSize = defaultFontSize.get();
            ModernUI.get().getPreferredTypeface();
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
