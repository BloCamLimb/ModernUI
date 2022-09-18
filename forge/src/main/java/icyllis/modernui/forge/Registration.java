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

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import icyllis.modernui.ModernUI;
import icyllis.modernui.core.Core;
import icyllis.modernui.core.Handler;
import icyllis.modernui.forge.mixin.AccessOptions;
import icyllis.modernui.graphics.opengl.ShaderManager;
import icyllis.modernui.graphics.opengl.TextureManager;
import icyllis.modernui.test.TestFragment;
import icyllis.modernui.testforge.TestContainerMenu;
import icyllis.modernui.testforge.TestPauseFragment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

import static icyllis.modernui.ModernUI.*;

/**
 * This class handles mod loading events, all registry entries are only available under the development mode.
 */
@Mod.EventBusSubscriber(modid = ModernUI.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
final class Registration {

    private Registration() {
    }

    @SubscribeEvent
    static void register(@Nonnull RegisterEvent event) {
        if (ModernUIForge.sDevelopment) {
            event.register(ForgeRegistries.MENU_TYPES.getRegistryKey(), Registration::registerMenus);
            event.register(ForgeRegistries.ITEMS.getRegistryKey(), Registration::registerItems);
        }
    }

    static void registerMenus(@Nonnull RegisterEvent.RegisterHelper<MenuType<?>> helper) {
        helper.register(MuiRegistries.TEST_MENU_KEY, IForgeMenuType.create(TestContainerMenu::new));
    }

    static void registerItems(@Nonnull RegisterEvent.RegisterHelper<Item> helper) {
        Item.Properties properties = new Item.Properties().stacksTo(1);
        helper.register(MuiRegistries.PROJECT_BUILDER_ITEM_KEY, new ProjectBuilderItem(properties));
    }

    @SubscribeEvent
    static void setupCommon(@Nonnull FMLCommonSetupEvent event) {
        /*byte[] bytes = null;
        try (InputStream stream = ModernUIForge.class.getClassLoader().getResourceAsStream(
                "icyllis/modernui/forge/NetworkMessages.class")) {
            Objects.requireNonNull(stream, "Mod file is broken");
            bytes = IOUtils.toByteArray(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (InputStream stream = ModernUIForge.class.getClassLoader().getResourceAsStream(
                "icyllis/modernui/forge/NetworkMessages$C.class")) {
            Objects.requireNonNull(stream, "Mod file is broken");
            bytes = ArrayUtils.addAll(bytes, IOUtils.toByteArray(stream));
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        if (ModList.get().getModContainerById(new String(new byte[]{0x1f ^ 0x74, (0x4 << 0x1) | 0x41,
                ~-0x78, 0xd2 >> 0x1}, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT)).isPresent()) {
            event.enqueueWork(() -> LOGGER.fatal("OK"));
        }
        /*bytes = ArrayUtils.addAll(bytes, ModList.get().getModFileById(ModernUI.ID).getLicense()
                .getBytes(StandardCharsets.UTF_8));
        if (bytes == null) {
            throw new IllegalStateException();
        }*/
        if (ModernUIForge.sDevelopment) {
            NetworkMessages.sNetwork = DistExecutor.safeRunForDist(() -> NetworkMessages.Client::new,
                    () -> NetworkMessages::new);
        }

        MinecraftForge.EVENT_BUS.register(ServerHandler.INSTANCE);

        // give it a probe
        if (MuiForgeApi.isServerStarted()) {
            LOGGER.info(MARKER, "");
        }
    }

    /*@Nonnull
    private static String digest(@Nonnull byte[] in) {
        try {
            in = MessageDigest.getInstance("MD5").digest(in);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 15; i += 3) {
            int c = (in[i] & 0xFF) | (in[i + 1] & 0xFF) << 8 | (in[i + 2] & 0xFF) << 16;
            for (int k = 0; k < 4; k++) {
                final int m = c & 0x3f;
                final char t;
                if (m < 26)
                    t = (char) ('A' + m);
                else if (m < 52)
                    t = (char) ('a' + m - 26);
                else if (m < 62)
                    t = (char) ('0' + m - 52);
                else if (m == 62)
                    t = '+';
                else // m == 63
                    t = '/';
                sb.append(t);
                c >>= 6;
            }
        }
        sb.append(Integer.toHexString(in[15] & 0xFF));
        return sb.toString();
    }*/

    @Mod.EventBusSubscriber(modid = ModernUI.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    static class ModClient {

        static {
            assert (FMLEnvironment.dist.isClient());
        }

        private ModClient() {
        }

        @SubscribeEvent
        static void loadingClient(RegisterParticleProvidersEvent event) {
            // this event fired after LOAD_REGISTRIES and before COMMON_SETUP on client main thread (render thread)
            // this event fired before RegisterClientReloadListenersEvent
            UIManager.initialize();
        }

        @SubscribeEvent
        static void registerResourceListener(@Nonnull RegisterClientReloadListenersEvent event) {
            // this event fired after LOAD_REGISTRIES and before COMMON_SETUP on client main thread (render thread)
            // this event fired after ParticleFactoryRegisterEvent
            event.registerReloadListener((ResourceManagerReloadListener) manager -> {
                ShaderManager.getInstance().reload();
                TextureManager.getInstance().reload();
                Handler handler = Core.getUiHandlerAsync();
                // FML may throw ex, so it can be null
                if (handler != null) {
                    // Call in lambda, not in creating the lambda
                    handler.post(() -> UIManager.getInstance().updateLayoutDir(Config.CLIENT.mForceRtl.get()));
                }
            });

            LOGGER.debug(MARKER, "Registered resource reload listener");
        }

        @SubscribeEvent
        static void registerKeyMapping(@Nonnull RegisterKeyMappingsEvent event) {
            event.register(UIManager.OPEN_CENTER_KEY);
        }

        @SubscribeEvent
        static void setupClient(@Nonnull FMLClientSetupEvent event) {
            //SettingsManager.INSTANCE.buildAllSettings();
            //UIManager.getInstance().registerMenuScreen(Registration.TEST_MENU, menu -> new TestUI());

            event.enqueueWork(() -> {
                ModernUI.getSelectedTypeface();
                UIManager.initializeRenderer();
                if (ModernUIForge.sDevelopment) {
                    MenuScreens.register(MuiRegistries.TEST_MENU.get(), MenuScreenFactory.create(menu ->
                            menu.isDiamond() ? new TestFragment() : new TestPauseFragment()));
                }
            });

            // Always replace static variable as an insurance policy
            /*AccessOption.setGuiScale(new CycleOption("options.guiScale",
                    (options, integer) -> options.guiScale = Integer.remainderUnsigned(
                            options.guiScale + integer, (MForgeCompat.calcGuiScales() & 0xf) + 1),
                    (options, cycleOption) -> options.guiScale == 0 ?
                            ((AccessOption) cycleOption).callGenericValueLabel(new TranslatableComponent("options" +
                                    ".guiScale.auto")
                                    .append(new TextComponent(" (" + (MForgeCompat.calcGuiScales() >> 4 & 0xf) + ")")
                                    )) :
                            ((AccessOption) cycleOption).callGenericValueLabel(new TextComponent(Integer.toString
                            (options
                            .guiScale))))
            );*/

            if (Config.CLIENT.mUseNewGuiScale.get()) {
                final OptionInstance<Integer> newGuiScale = new OptionInstance<>("options.guiScale",
                        OptionInstance.noTooltip(),
                        (caption, value) -> {
                            if (value == 0) { // auto (best)
                                int best = (MuiForgeApi.calcGuiScales() >> 4 & 0xf);
                                return Options.genericValueLabel(caption,
                                        Component.translatable("options.guiScale.auto")
                                                .append(Component.literal(" (" + best + ")")));
                            } else {
                                MutableComponent valueComponent = Component.literal(value.toString());
                                if (value < (MuiForgeApi.calcGuiScales() >> 8 & 0xf)) {
                                    valueComponent.withStyle(ChatFormatting.RED);
                                }
                                return Options.genericValueLabel(caption, valueComponent);
                            }
                        },
                        new GuiScaleValueSet(), 0, value -> {
                    if (value != Minecraft.getInstance().getWindow().getGuiScale()) {
                        Minecraft.getInstance().resizeDisplay();
                    }
                });
                // no barrier
                ((AccessOptions) Minecraft.getInstance().options).setGuiScale(newGuiScale);
                if (ModernUIForge.isOptiFineLoaded()) {
                    OptiFineIntegration.setGuiScale(newGuiScale);
                    LOGGER.info(MARKER, "Replace OptiFine GUI Scale");
                }
            }

            /*Option[] settings = null;
            boolean captured = false;
            if (ModernUIForge.isOptiFineLoaded()) {
                try {
                    Field field = VideoSettingsScreen.class.getDeclaredField("videoOptions");
                    field.setAccessible(true);
                    settings = (Option[]) field.get(null);
                } catch (Exception e) {
                    LOGGER.error(ModernUI.MARKER, "Failed to be compatible with OptiFine video settings", e);
                }
            } else {
                settings = AccessVideoSettings.getOptions();
            }
            if (settings != null) {
                for (int i = 0; i < settings.length; i++) {
                    if (settings[i] != Option.GUI_SCALE) {
                        continue;
                    }
                    ProgressOption option = new ProgressOption("options.guiScale", 0, 2, 1,
                            options -> (double) options.guiScale,
                            (options, aDouble) -> {
                                if (options.guiScale != aDouble.intValue()) {
                                    options.guiScale = aDouble.intValue();
                                    Minecraft.getInstance().resizeDisplay();
                                }
                            },
                            (options, progressOption) -> options.guiScale == 0 ?
                                    ((AccessOptions) progressOption)
                                            .callGenericValueLabel(new TranslatableComponent("options.guiScale.auto")
                                                    .append(new TextComponent(" (" + (MuiForgeApi.calcGuiScales() >> 4 &
                                                    0xf) + ")"))) :
                                    ((AccessOptions) progressOption)
                                            .callGenericValueLabel(new TextComponent(Integer.toString(options
                                            .guiScale)))
                    );
                    settings[i] = EventHandler.Client.sNewGuiScale = option;
                    captured = true;
                    break;
                }
            }
            if (!captured) {
                LOGGER.error(MARKER, "Failed to capture video settings");
            }*/
        }

        static class GuiScaleValueSet implements OptionInstance.IntRangeBase,
                OptionInstance.SliderableOrCyclableValueSet<Integer> {

            static {
                assert (FMLEnvironment.dist.isClient());
            }

            @Override
            public int minInclusive() {
                return 0;
            }

            @Override
            public int maxInclusive() {
                return MuiForgeApi.calcGuiScales() & 0xf;
            }

            @Nonnull
            @Override
            public Integer fromSliderValue(double progress) {
                return Math.toIntExact(Math.round(Mth.map(progress, 0.0, 1.0, minInclusive(), maxInclusive())));
            }

            @Nonnull
            @Override
            public Optional<Integer> validateValue(@Nonnull Integer value) {
                return Optional.of(Mth.clamp(value, minInclusive(), maxInclusive()));
            }

            @Nonnull
            @Override
            public Codec<Integer> codec() {
                Function<Integer, DataResult<Integer>> function = value -> {
                    int max = maxInclusive() + 1;
                    if (value.compareTo(minInclusive()) >= 0 && value.compareTo(max) <= 0) {
                        return DataResult.success(value);
                    }
                    return DataResult.error(
                            "Value " + value + " outside of range [" + minInclusive() + ":" + max + "]", value);
                };
                return Codec.INT.flatXmap(function, function);
            }

            @Nonnull
            @Override
            public CycleButton.ValueListSupplier<Integer> valueListSupplier() {
                return CycleButton.ValueListSupplier.create(IntStream.range(minInclusive(), maxInclusive() + 1).boxed().toList());
            }

            @Override
            public boolean createCycleButton() {
                return false;
            }
        }

        @SubscribeEvent
        static void onMenuOpen(@Nonnull OpenMenuEvent event) {
            if (ModernUIForge.sDevelopment) {
                if (event.getMenu() instanceof TestContainerMenu c) {
                    if (c.isDiamond()) {
                        event.set(new TestFragment());
                    } else {
                        event.set(new TestPauseFragment());
                    }
                }
            }
        }
    }

    static class ModClientDev {

        static {
            assert (FMLEnvironment.dist.isClient());
        }

        private ModClientDev() {
        }

        @SubscribeEvent
        static void onRegistryModel(@Nonnull ModelEvent.RegisterAdditional event) {
            event.register(new ResourceLocation(ModernUI.ID, "item/project_builder_main"));
            event.register(new ResourceLocation(ModernUI.ID, "item/project_builder_cube"));
        }

        @SubscribeEvent
        static void onBakeModel(@Nonnull ModelEvent.BakingCompleted event) {
            Map<ResourceLocation, BakedModel> registry = event.getModels();
            replaceModel(registry, new ModelResourceLocation(ModernUI.ID, "project_builder", "inventory"),
                    baseModel -> new ProjectBuilderModel(baseModel, event.getModelBakery()));
        }

        private static void replaceModel(@Nonnull Map<ResourceLocation, BakedModel> modelRegistry,
                                         @Nonnull ModelResourceLocation location,
                                         @Nonnull Function<BakedModel, BakedModel> replacer) {
            modelRegistry.put(location, replacer.apply(modelRegistry.get(location)));
        }
    }
}
