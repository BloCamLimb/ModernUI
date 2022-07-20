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
import com.mojang.blaze3d.platform.Window;
import icyllis.modernui.ModernUI;
import icyllis.modernui.core.Core;
import icyllis.modernui.core.Handler;
import icyllis.modernui.graphics.Color;
import icyllis.modernui.graphics.font.GLFontAtlas;
import icyllis.modernui.graphics.font.GlyphManager;
import icyllis.modernui.textmc.TextLayoutEngine;
import icyllis.modernui.view.ViewConfiguration;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.*;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static icyllis.modernui.ModernUI.*;

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
            LOGGER.debug(MARKER, "Client config reloaded with {}", event.getClass().getSimpleName());
        } else if (spec == COMMON_SPEC) {
            COMMON.reload();
            LOGGER.debug(MARKER, "Common config reloaded with {}", event.getClass().getSimpleName());
        } else if (spec == SERVER_SPEC) {
            SERVER.reload();
            LOGGER.debug(MARKER, "Server config reloaded with {}", event.getClass().getSimpleName());
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

        public static final int ANIM_DURATION_MIN = 0;
        public static final int ANIM_DURATION_MAX = 800;
        public static final int BLUR_RADIUS_MIN = 2;
        public static final int BLUR_RADIUS_MAX = 18;
        public static final float FONT_SCALE_MIN = 0.5f;
        public static final float FONT_SCALE_MAX = 2.0f;

        final ForgeConfigSpec.BooleanValue blurEffect;
        final ForgeConfigSpec.IntValue backgroundDuration;
        final ForgeConfigSpec.IntValue blurRadius;
        final ForgeConfigSpec.ConfigValue<List<? extends String>> backgroundColor;
        final ForgeConfigSpec.BooleanValue inventoryPause;
        final ForgeConfigSpec.BooleanValue tooltip;
        final ForgeConfigSpec.ConfigValue<List<? extends String>> tooltipFill;
        final ForgeConfigSpec.ConfigValue<List<? extends String>> tooltipStroke;
        final ForgeConfigSpec.IntValue tooltipDuration;
        final ForgeConfigSpec.BooleanValue ding;
        //private final ForgeConfigSpec.BooleanValue hudBars;
        final ForgeConfigSpec.BooleanValue forceRtl;
        final ForgeConfigSpec.DoubleValue fontScale;
        final ForgeConfigSpec.EnumValue<WindowMode> windowMode;

        final ForgeConfigSpec.IntValue scrollbarSize;
        final ForgeConfigSpec.IntValue touchSlop;
        final ForgeConfigSpec.IntValue minScrollbarTouchTarget;
        final ForgeConfigSpec.IntValue minimumFlingVelocity;
        final ForgeConfigSpec.IntValue maximumFlingVelocity;
        final ForgeConfigSpec.IntValue overscrollDistance;
        final ForgeConfigSpec.IntValue overflingDistance;
        final ForgeConfigSpec.DoubleValue verticalScrollFactor;
        final ForgeConfigSpec.DoubleValue horizontalScrollFactor;

        private final ForgeConfigSpec.ConfigValue<List<? extends String>> blurBlacklist;

        final ForgeConfigSpec.BooleanValue antiAliasing;
        final ForgeConfigSpec.BooleanValue fractionalMetrics;
        final ForgeConfigSpec.BooleanValue linearSampling;
        final ForgeConfigSpec.ConfigValue<List<? extends String>> fontFamily;

        final ForgeConfigSpec.BooleanValue skipGLCapsError;
        final ForgeConfigSpec.BooleanValue showGLCapsError;

        private Client(@Nonnull ForgeConfigSpec.Builder builder) {
            builder.comment("Screen Config")
                    .push("screen");

            backgroundDuration = builder.comment(
                            "The duration of GUI background color and blur radius animation in milliseconds. (0 = OFF)")
                    .defineInRange("animationDuration", 200, ANIM_DURATION_MIN, ANIM_DURATION_MAX);
            backgroundColor = builder.comment(
                            "The GUI background color in #RRGGBB or #AARRGGBB format. Default value: #66000000",
                            "Can be one to four values representing top left, top right, bottom right and bottom left" +
                                    " color.",
                            "Multiple values produce a gradient effect, whereas one value produce a solid color.",
                            "When values is less than 4, the rest of the corner color will be replaced by the last " +
                                    "value.")
                    .defineList("backgroundColor", () -> {
                        List<String> list = new ArrayList<>();
                        list.add("#66000000");
                        return list;
                    }, o -> true);

            blurEffect = builder.comment(
                            "Add blur effect to GUI background when opened, it is incompatible with OptiFine's FXAA " +
                                    "shader and some mods.")
                    .define("blurEffect", true);
            blurRadius = builder.comment(
                            "The 4-pass blur effect radius, higher values result in a small loss of performance.")
                    .defineInRange("blurRadius", 9, BLUR_RADIUS_MIN, BLUR_RADIUS_MAX);
            blurBlacklist = builder.comment(
                            "A list of GUI screen superclasses that won't activate blur effect when opened.")
                    .defineList("blurBlacklist", () -> {
                        List<String> list = new ArrayList<>();
                        list.add(ChatScreen.class.getName());
                        return list;
                    }, o -> true);
            inventoryPause = builder.comment(
                            "(Beta) Pause the game when inventory (also includes creative mode) opened.")
                    .define("inventoryPause", false);

            builder.pop();

            builder.comment("Tooltip Config")
                    .push("tooltip");

            tooltip = builder.comment(
                            "Whether to enable Modern UI tooltip style, or back to vanilla style.")
                    .define("enable", true);
            tooltipFill = builder.comment(
                            "The tooltip FILL color in #RRGGBB or #AARRGGBB format. Default: #D4000000",
                            "Can be one to four values representing top left, top right, bottom right and bottom left" +
                                    " color.",
                            "Multiple values produce a gradient effect, whereas one value produce a solid color.",
                            "When values is less than 4, the rest of the corner color will be replaced by the last " +
                                    "value.")
                    .defineList("colorFill", () -> {
                        List<String> list = new ArrayList<>();
                        list.add("#D4000000");
                        return list;
                    }, $ -> true);
            tooltipStroke = builder.comment(
                            "The tooltip STROKE color in #RRGGBB or #AARRGGBB format. Default: #F0AADCF0, #F0DAD0F4, " +
                                    "#F0FFC3F7 and #F0DAD0F4",
                            "Can be one to four values representing top left, top right, bottom right and bottom left" +
                                    " color.",
                            "Multiple values produce a gradient effect, whereas one value produce a solid color.",
                            "When values is less than 4, the rest of the corner color will be replaced by the last " +
                                    "value.")
                    .defineList("colorStroke", () -> {
                        List<String> list = new ArrayList<>();
                        list.add("#F0AADCF0");
                        list.add("#F0DAD0F4");
                        list.add("#F0FFC3F7");
                        list.add("#F0DAD0F4");
                        return list;
                    }, $ -> true);
            tooltipDuration = builder.comment(
                            "The duration of tooltip alpha animation in milliseconds. (0 = OFF)")
                    .defineInRange("animationDuration", 200, ANIM_DURATION_MIN, ANIM_DURATION_MAX);

            builder.pop();

            builder.comment("General Config")
                    .push("general");

            ding = builder.comment("Play a sound effect when the game is loaded.")
                    .define("ding", true);

            /*hudBars = builder.comment(
                    "Show additional HUD bars added by ModernUI on the bottom-left of the screen.")
                    .define("hudBars", false);*/

            windowMode = builder.comment("Control the window mode, normal mode does nothing.")
                    .defineEnum("windowMode", WindowMode.NORMAL);

            skipGLCapsError = builder.comment("UI renderer is disabled when the OpenGL capability test fails.",
                            "Sometimes the driver reports wrong values, you can enable this to ignore it.")
                    .define("skipGLCapsError", false);
            showGLCapsError = builder.comment("A dialog popup is displayed when the OpenGL capability test fails.",
                            "Set to false to not show it. This is ignored when skipGLCapsError=true")
                    .define("showGLCapsError", true);

            builder.pop();

            builder.comment("View system config, only applied to Modern UI Core.")
                    .push("view");

            forceRtl = builder.comment("Force layout direction to RTL, otherwise, the current Locale setting.")
                    .define("forceRtl", false);
            fontScale = builder.comment("The global font scale used with sp units.")
                    .defineInRange("fontScale", 1.0f, FONT_SCALE_MIN, FONT_SCALE_MAX);
            scrollbarSize = builder.comment("Default scrollbar size in dips.")
                    .defineInRange("scrollbarSize", ViewConfiguration.SCROLL_BAR_SIZE, 0, 1024);
            touchSlop = builder.comment("Distance a touch can wander before we think the user is scrolling in dips.")
                    .defineInRange("touchSlop", ViewConfiguration.TOUCH_SLOP, 0, 1024);
            minScrollbarTouchTarget = builder.comment("Minimum size of the touch target for a scrollbar in dips.")
                    .defineInRange("minScrollbarTouchTarget", ViewConfiguration.MIN_SCROLLBAR_TOUCH_TARGET, 0, 1024);
            minimumFlingVelocity = builder.comment("Minimum velocity to initiate a fling in dips per second.")
                    .defineInRange("minimumFlingVelocity", ViewConfiguration.MINIMUM_FLING_VELOCITY, 0, 32767);
            maximumFlingVelocity = builder.comment("Maximum velocity to initiate a fling in dips per second.")
                    .defineInRange("maximumFlingVelocity", ViewConfiguration.MAXIMUM_FLING_VELOCITY, 0, 32767);
            overscrollDistance = builder.comment("Max distance in dips to overscroll for edge effects.")
                    .defineInRange("overscrollDistance", ViewConfiguration.OVERSCROLL_DISTANCE, 0, 1024);
            overflingDistance = builder.comment("Max distance in dips to overfling for edge effects.")
                    .defineInRange("overflingDistance", ViewConfiguration.OVERFLING_DISTANCE, 0, 1024);
            verticalScrollFactor = builder.comment("Amount to scroll in response to a vertical scroll event, in dips " +
                            "per axis value.")
                    .defineInRange("verticalScrollFactor", ViewConfiguration.VERTICAL_SCROLL_FACTOR, 0, 1024);
            horizontalScrollFactor = builder.comment("Amount to scroll in response to a horizontal scroll event, in " +
                            "dips per axis value.")
                    .defineInRange("horizontalScrollFactor", ViewConfiguration.HORIZONTAL_SCROLL_FACTOR, 0, 1024);

            builder.pop();


            builder.comment("Font Config")
                    .push("font");

            antiAliasing = builder.comment(
                            "Control the anti-aliasing of raw glyph rendering.")
                    .define("antiAliasing", true);
            fractionalMetrics = builder.comment(
                            "Control the fractional metrics of raw glyph rendering.",
                            "Disable for rougher fonts; Enable for smoother fonts.")
                    .define("fractionalMetrics", true);
            linearSampling = builder.comment(
                            "Enable linear sampling for font atlases with mipmaps, mag filter will be always NEAREST.",
                            "If your fonts are not bitmap fonts, then you should keep this setting true.")
                    .define("linearSampling", true);
            // Segoe UI, Source Han Sans CN Medium, Noto Sans, Open Sans, San Francisco, Calibri,
            // Microsoft YaHei UI, STHeiti, SimHei, SansSerif
            fontFamily = builder.comment(
                            "A set of font families with fallbacks to determine the typeface to use.",
                            "TrueType & OpenTrue are supported. Each element can be one of the following three cases.",
                            "1) Font family root name for those installed on your PC, for instance: Segoe UI",
                            "2) File path for external fonts on your PC, for instance: /usr/shared/fonts/x.otf",
                            "3) Resource location for those loaded with resource packs, for instance: " +
                                    "modernui:font/biliw.otf",
                            "Using bitmap fonts should consider other text settings, default glyph size should be 16x.",
                            "This list is only read once when the game is loaded. A game restart is required to reload")
                    .defineList("fontFamily", () -> {
                        List<String> list = new ArrayList<>();
                        list.add("modernui:font/default.ttf");
                        list.add("Segoe UI");
                        list.add("modernui:font/biliw.otf");
                        list.add("Noto Sans");
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

        void saveOnly() {
            Util.ioPool().execute(() -> CLIENT_SPEC.save());
        }

        void saveAndReload() {
            Util.ioPool().execute(() -> {
                CLIENT_SPEC.save();
                reload();
            });
        }

        private void reload() {
            BlurHandler.sBlurEffect = blurEffect.get();
            BlurHandler.sAnimationDuration = backgroundDuration.get();
            BlurHandler.sBlurRadius = blurRadius.get();

            List<? extends String> colors = backgroundColor.get();
            int color = 0x66000000;
            for (int i = 0; i < 4; i++) {
                if (colors != null && i < colors.size()) {
                    String s = colors.get(i);
                    try {
                        color = Color.parseColor(s);
                    } catch (Exception e) {
                        LOGGER.error(MARKER, "Wrong color format for screen background, index: {}", i, e);
                    }
                }
                BlurHandler.sBackgroundColor[i] = color;
            }

            BlurHandler.INSTANCE.loadBlacklist(blurBlacklist.get());

            ModernUIForge.sInventoryScreenPausesGame = inventoryPause.get();
            TooltipRenderer.sTooltip = !ModernUIForge.hasGLCapsError() && tooltip.get();

            colors = tooltipFill.get();
            color = 0xD4000000;
            for (int i = 0; i < 4; i++) {
                if (colors != null && i < colors.size()) {
                    String s = colors.get(i);
                    try {
                        color = Color.parseColor(s);
                    } catch (Exception e) {
                        LOGGER.error(MARKER, "Wrong color format for tooltip fill, index: {}", i, e);
                    }
                }
                TooltipRenderer.sFillColor[i] = color;
            }
            colors = tooltipStroke.get();
            color = 0xF0AADCF0;
            for (int i = 0; i < 4; i++) {
                if (colors != null && i < colors.size()) {
                    String s = colors.get(i);
                    try {
                        color = Color.parseColor(s);
                    } catch (Exception e) {
                        LOGGER.error(MARKER, "Wrong color format for tooltil stroke, index: {}", i, e);
                    }
                }
                TooltipRenderer.sStrokeColor[i] = color;
            }
            TooltipRenderer.sAnimationDuration = tooltipDuration.get();

            UIManager.sPlaySoundOnLoaded = ding.get();
            WindowMode winMode = windowMode.get();
            if (winMode != WindowMode.NORMAL) {
                Minecraft.getInstance().tell(() -> {
                    Window winB3D = Minecraft.getInstance().getWindow();
                    switch (winMode) {
                        case FULLSCREEN -> {
                            if (!winB3D.isFullscreen()) {
                                winB3D.toggleFullScreen();
                            }
                        }
                        case FULLSCREEN_BORDERLESS -> {
                            if (winB3D.isFullscreen()) {
                                winB3D.toggleFullScreen();
                            }
                            GLFW.glfwRestoreWindow(winB3D.getWindow());
                            GLFW.glfwSetWindowAttrib(winB3D.getWindow(),
                                    GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
                            GLFW.glfwMaximizeWindow(winB3D.getWindow());
                        }
                        case MAXIMIZED -> {
                            if (winB3D.isFullscreen()) {
                                winB3D.toggleFullScreen();
                            }
                            GLFW.glfwSetWindowAttrib(winB3D.getWindow(),
                                    GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
                            GLFW.glfwMaximizeWindow(winB3D.getWindow());
                        }
                        case MINIMIZED -> {
                            if (winB3D.isFullscreen()) {
                                winB3D.toggleFullScreen();
                            }
                            GLFW.glfwSetWindowAttrib(winB3D.getWindow(),
                                    GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
                            GLFW.glfwIconifyWindow(winB3D.getWindow());
                        }
                        case WINDOWED -> {
                            if (winB3D.isFullscreen()) {
                                winB3D.toggleFullScreen();
                            }
                            GLFW.glfwSetWindowAttrib(winB3D.getWindow(),
                                    GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
                            GLFW.glfwRestoreWindow(winB3D.getWindow());
                        }
                        case WINDOWED_BORDERLESS -> {
                            if (winB3D.isFullscreen()) {
                                winB3D.toggleFullScreen();
                            }
                            GLFW.glfwSetWindowAttrib(winB3D.getWindow(),
                                    GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
                            GLFW.glfwRestoreWindow(winB3D.getWindow());
                        }
                    }
                });
            }

            //TestHUD.sBars = hudBars.get();
            Handler handler = Core.getUiHandlerAsync();
            if (handler != null) {
                handler.post(() -> {
                    UIManager.getInstance().updateLayoutDir();
                    ViewConfiguration.get().setFontScale(fontScale.get().floatValue());
                    ViewConfiguration.get().setScrollbarSize(scrollbarSize.get());
                    ViewConfiguration.get().setTouchSlop(touchSlop.get());
                    ViewConfiguration.get().setMinScrollbarTouchTarget(minScrollbarTouchTarget.get());
                    ViewConfiguration.get().setMinimumFlingVelocity(minimumFlingVelocity.get());
                    ViewConfiguration.get().setMaximumFlingVelocity(maximumFlingVelocity.get());
                    ViewConfiguration.get().setOverscrollDistance(overscrollDistance.get());
                    ViewConfiguration.get().setOverflingDistance(overflingDistance.get());
                    ViewConfiguration.get().setVerticalScrollFactor(verticalScrollFactor.get().floatValue());
                    ViewConfiguration.get().setHorizontalScrollFactor(horizontalScrollFactor.get().floatValue());
                });
            }

            boolean reload = false;
            if (GlyphManager.sAntiAliasing != antiAliasing.get()) {
                GlyphManager.sAntiAliasing = antiAliasing.get();
                reload = true;
            }
            if (GlyphManager.sFractionalMetrics != fractionalMetrics.get()) {
                GlyphManager.sFractionalMetrics = fractionalMetrics.get();
                reload = true;
            }
            if (GLFontAtlas.sLinearSampling != linearSampling.get()) {
                GLFontAtlas.sLinearSampling = linearSampling.get();
                reload = true;
            }
            if (reload) {
                Minecraft.getInstance().submit(() -> TextLayoutEngine.getInstance().reloadAll());
            }

            ModernUI.getSelectedTypeface();
        }

        public enum WindowMode {
            NORMAL,
            FULLSCREEN,
            FULLSCREEN_BORDERLESS,
            MAXIMIZED,
            MINIMIZED,
            WINDOWED,
            WINDOWED_BORDERLESS;

            @Nonnull
            @Override
            public String toString() {
                return I18n.get("modernui.windowMode." + name().toLowerCase(Locale.ROOT));
            }
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
