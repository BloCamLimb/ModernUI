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

package icyllis.modernui.textmc;

import icyllis.modernui.ModernUI;
import icyllis.modernui.core.Core;
import icyllis.modernui.forge.MuiForgeApi;
import icyllis.modernui.graphics.font.GlyphManager;
import icyllis.modernui.text.TextUtils;
import icyllis.modernui.view.View;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.ParallelDispatchEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nonnull;
import java.util.regex.Matcher;

import static icyllis.modernui.ModernUI.*;

/**
 * Modern UI Text MC can bootstrap independently.
 */
@OnlyIn(Dist.CLIENT)
public final class ModernUITextMC {

    public static Config CONFIG;
    private static ForgeConfigSpec CONFIG_SPEC;

    private ModernUITextMC() {
    }

    @OnlyIn(Dist.CLIENT)
    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().register(ModernUITextMC.class);
    }

    @OnlyIn(Dist.CLIENT)
    public static void initConfig() {
        FMLPaths.getOrCreateGameRelativePath(FMLPaths.CONFIGDIR.get().resolve(ModernUI.NAME_CPT), ModernUI.NAME_CPT);
        ModContainer mod = ModLoadingContext.get().getActiveContainer();

        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CONFIG = new Config(builder);
        CONFIG_SPEC = builder.build();
        mod.addConfig(new ModConfig(ModConfig.Type.CLIENT, CONFIG_SPEC, mod, ModernUI.NAME_CPT + "/text.toml"));

        FMLJavaModLoadingContext.get().getModEventBus().addListener(CONFIG::onReload);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    static void registerResourceListener(@Nonnull RegisterClientReloadListenersEvent event) {
        // language may reload, cause TranslatableComponent changed, so clear layout cache
        event.registerReloadListener((ResourceManagerReloadListener) manager -> TextLayoutEngine.getInstance().reloadResources());
        LOGGER.debug(MARKER, "Registered language reload listener");
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    static void setupClient(@Nonnull FMLClientSetupEvent event) {
        // preload text engine, note that this event is fired after client config first load
        // so that the typeface config is valid
        Minecraft.getInstance().execute(() -> {
            ModernUI.getSelectedTypeface();
            TextLayoutEngine.getInstance().lookupVanillaNode(ModernUI.NAME_CPT);
        });
        MuiForgeApi.addOnWindowResizeListener((width, height, newScale, oldScale) -> {
            if (Core.hasRenderThread() && newScale != oldScale) {
                TextLayoutEngine.getInstance().reload();
            }
        });
        MuiForgeApi.addOnDebugDumpListener(pw -> {
            pw.print("TextLayoutEngine: ");
            pw.print("CacheCount=" + TextLayoutEngine.getInstance().getCacheCount());
            int memorySize = TextLayoutEngine.getInstance().getCacheMemorySize();
            pw.print(", CacheSize=" + TextUtils.binaryCompact(memorySize) + " (" + memorySize + " bytes)");
            memorySize = TextLayoutEngine.getInstance().getEmojiAtlasMemorySize();
            pw.println(", EmojiAtlasSize=" + TextUtils.binaryCompact(memorySize) + " (" + memorySize + " bytes)");
            GlyphManager.getInstance().dumpInfo(pw);
        });
        {
            int[] codePoints = {0x1f469, 0x1f3fc, 0x200d, 0x2764, 0xfe0f, 0x200d, 0x1f48b, 0x200d, 0x1f469, 0x1f3fd};
            CharSequenceBuilder builder = new CharSequenceBuilder();
            for (int cp : codePoints) {
                builder.addCodePoint(cp);
            }
            String string = new String(codePoints, 0, codePoints.length);
            if (builder.hashCode() != string.hashCode() || builder.hashCode() != builder.toString().hashCode()) {
                throw new RuntimeException("String.hashCode() is not identical to the specs");
            }
        }
        MinecraftForge.EVENT_BUS.register(EventHandler.class);
        LOGGER.info(MARKER, "Loaded modern text engine");
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    static void onParallelDispatch(@Nonnull ParallelDispatchEvent event) {
        // since Forge EVENT_BUS is not started yet, we should manually maintain that
        // in case of some mods render texts before entering main menu
        event.enqueueWork(() -> TextLayoutEngine.getInstance().cleanup());
    }

    /*@OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    static void registerShaders(@Nonnull RegisterShadersEvent event) {
        ResourceProvider provider = event.getResourceManager();
        try {
            event.registerShader(new ShaderInstance(provider, TextRenderType.SHADER_RL,
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), TextRenderType::setShader);
            event.registerShader(new ShaderInstance(provider, TextRenderType.SHADER_SEE_THROUGH_RL,
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), TextRenderType::setShaderSeeThrough);
        } catch (IOException e) {
            throw new RuntimeException("Bad shaders", e);
        }
    }*/

    static class EventHandler {

        @SubscribeEvent
        static void onClientChat(@Nonnull ClientChatEvent event) {
            final String msg = event.getMessage();
            if (CONFIG.mEmojiShortcodes.get() && !msg.startsWith("/")) {
                final TextLayoutEngine engine = TextLayoutEngine.getInstance();
                final Matcher matcher = TextLayoutEngine.EMOJI_SHORTCODE_PATTERN.matcher(msg);

                StringBuilder builder = null;
                int lastEnd = 0;
                while (matcher.find()) {
                    if (builder == null) {
                        builder = new StringBuilder();
                    }
                    int st = matcher.start();
                    int en = matcher.end();
                    String emoji = null;
                    if (en - st > 2) {
                        emoji = engine.lookupEmojiShortcode(msg.substring(st + 1, en - 1));
                    }
                    if (emoji != null) {
                        builder.append(msg, lastEnd, st);
                        builder.append(emoji);
                    } else {
                        builder.append(msg, lastEnd, en);
                    }
                    lastEnd = en;
                }
                if (builder != null) {
                    builder.append(msg, lastEnd, msg.length());
                    event.setMessage(builder.toString());
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Config {

        public static final int BASE_FONT_SIZE_MIN = 6;
        public static final int BASE_FONT_SIZE_MAX = 10;
        public static final int BASELINE_MIN = 4;
        public static final int BASELINE_MAX = 10;
        public static final int LIFESPAN_MIN = 2;
        public static final int LIFESPAN_MAX = 60;
        public static final int REHASH_MIN = 0;
        public static final int REHASH_MAX = 2000;

        //final ForgeConfigSpec.BooleanValue globalRenderer;
        public final ForgeConfigSpec.BooleanValue mAllowShadow;
        public final ForgeConfigSpec.BooleanValue mFixedResolution;
        public final ForgeConfigSpec.IntValue mBaseFontSize;
        public final ForgeConfigSpec.IntValue mBaselineShift;
        public final ForgeConfigSpec.BooleanValue mSuperSampling;
        public final ForgeConfigSpec.BooleanValue mAlignPixels;
        public final ForgeConfigSpec.IntValue mCacheLifespan;
        public final ForgeConfigSpec.IntValue mRehashThreshold;
        public final ForgeConfigSpec.IntValue mTextDirection;
        public final ForgeConfigSpec.BooleanValue mColorEmoji;
        public final ForgeConfigSpec.BooleanValue mBitmapReplacement;
        public final ForgeConfigSpec.BooleanValue mEmojiShortcodes;

        //private final ForgeConfigSpec.BooleanValue antiAliasing;
        //private final ForgeConfigSpec.BooleanValue highPrecision;
        //private final ForgeConfigSpec.BooleanValue enableMipmap;
        //private final ForgeConfigSpec.IntValue mipmapLevel;
        //private final ForgeConfigSpec.IntValue resolutionLevel;
        //private final ForgeConfigSpec.IntValue defaultFontSize;

        private Config(@Nonnull ForgeConfigSpec.Builder builder) {
            builder.comment("Text Config")
                    .push("text");

            /*globalRenderer = builder.comment(
                    "Apply Modern UI font renderer (including text layouts) to the entire game rather than only " +
                            "Modern UI itself.")
                    .define("globalRenderer", true);*/
            mAllowShadow = builder.comment(
                            "Allow text renderer to drop shadow, setting to false can improve performance.")
                    .define("allowShadow", true);
            mFixedResolution = builder.comment(
                            "Fix resolution level at 2. When the GUI scale increases, the resolution level remains.",
                            "Then GUI scale should be even numbers (2, 4, 6...), based on Minecraft GUI system.",
                            "If your fonts are not bitmap fonts, then you should keep this setting false.")
                    .define("fixedResolution", false);
            mBaseFontSize = builder.comment(
                            "Control base font size, in GUI scaled pixels. The default and vanilla value is 8.",
                            "For bitmap fonts, 8 represents a glyph size of 8x or 16x if fixed resolution.")
                    .defineInRange("baseFontSize", 8, BASE_FONT_SIZE_MIN, BASE_FONT_SIZE_MAX);
            mBaselineShift = builder.comment(
                            "Control vertical baseline for vanilla text layout, in GUI scaled pixels.",
                            "For smaller font, 6 is recommended. The default value is 7.")
                    .defineInRange("baselineShift", 7, BASELINE_MIN, BASELINE_MAX);
            mSuperSampling = builder.comment(
                            "Super sampling can make the text more sharper with large font size or in the 3D world.",
                            "But perhaps it makes the path edge too blurry and difficult to read.")
                    .define("superSampling", false);
            mAlignPixels = builder.comment(
                            "Enable to make each glyph pixel-aligned in text layout in screen space.",
                            "Text rendering may be better with bitmap fonts or fixed resolution or linear sampling.")
                    .define("alignPixels", false);
            mCacheLifespan = builder.comment(
                            "Set the recycle time of layout cache in seconds, using least recently used algorithm.")
                    .defineInRange("cacheLifespan", 12, LIFESPAN_MIN, LIFESPAN_MAX);
            mRehashThreshold = builder.comment("Set the rehash threshold of layout cache")
                    .defineInRange("rehashThreshold", 100, REHASH_MIN, REHASH_MAX);
            mTextDirection = builder.comment(
                            "Control bidirectional text heuristic algorithm.")
                    .defineInRange("textDirection", View.TEXT_DIRECTION_FIRST_STRONG,
                            View.TEXT_DIRECTION_FIRST_STRONG, View.TEXT_DIRECTION_FIRST_STRONG_RTL);
            mColorEmoji = builder.comment(
                            "Whether to render colored emoji or just grayscale emoji.")
                    .define("colorEmoji", true);
            mBitmapReplacement = builder.comment(
                            "Whether to use bitmap replacement for non-Emoji character sequences. Restart is required.")
                    .define("bitmapReplacement", false);
            mEmojiShortcodes = builder.comment(
                            "Allow to use Slack or Discord shortcodes to replace Emoji character sequences in chat.")
                    .define("emojiShortcodes", true);
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

        public void saveOnly() {
            Util.ioPool().execute(() -> CONFIG_SPEC.save());
        }

        public void saveAndReload() {
            Util.ioPool().execute(() -> {
                CONFIG_SPEC.save();
                reload();
            });
        }

        void onReload(@Nonnull ModConfigEvent event) {
            final IConfigSpec<?> spec = event.getConfig().getSpec();
            if (spec != CONFIG_SPEC) {
                return;
            }
            reload();
            LOGGER.debug(MARKER, "Text config reloaded with {}", event.getClass().getSimpleName());
        }

        void reload() {
            boolean reload = false;
            ModernTextRenderer.sAllowShadow = mAllowShadow.get();
            if (TextLayoutEngine.sFixedResolution != mFixedResolution.get()) {
                TextLayoutEngine.sFixedResolution = mFixedResolution.get();
                reload = true;
            }
            if (TextLayoutProcessor.sBaseFontSize != mBaseFontSize.get()) {
                TextLayoutProcessor.sBaseFontSize = mBaseFontSize.get();
                reload = true;
            }
            TextRenderNode.sBaselineOffset = mBaselineShift.get().floatValue();
            if (TextLayoutEngine.sSuperSampling != mSuperSampling.get()) {
                TextLayoutEngine.sSuperSampling = mSuperSampling.get();
                reload = true;
            }
            if (TextLayoutProcessor.sAlignPixels != mAlignPixels.get()) {
                TextLayoutProcessor.sAlignPixels = mAlignPixels.get();
                reload = true;
            }
            TextLayoutEngine.sCacheLifespan = mCacheLifespan.get();
            TextLayoutEngine.sRehashThreshold = mRehashThreshold.get();
            if (TextLayoutEngine.sTextDirection != mTextDirection.get()) {
                TextLayoutEngine.sTextDirection = mTextDirection.get();
                reload = true;
            }
            if (TextLayoutProcessor.sColorEmoji != mColorEmoji.get()) {
                TextLayoutProcessor.sColorEmoji = mColorEmoji.get();
                reload = true;
            }
            if (reload) {
                Minecraft.getInstance().submit(() -> TextLayoutEngine.getInstance().reload());
            }
            /*GlyphManagerForge.sPreferredFont = preferredFont.get();
            GlyphManagerForge.sAntiAliasing = antiAliasing.get();
            GlyphManagerForge.sHighPrecision = highPrecision.get();
            GlyphManagerForge.sEnableMipmap = enableMipmap.get();
            GlyphManagerForge.sMipmapLevel = mipmapLevel.get();*/
            //GlyphManager.sResolutionLevel = resolutionLevel.get();
            //TextLayoutEngine.sDefaultFontSize = defaultFontSize.get();
        }
    }
}
