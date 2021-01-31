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

import icyllis.modernui.ModernUI;
import icyllis.modernui.font.ModernFontRenderer;
import icyllis.modernui.forge.mixin.AccessOption;
import icyllis.modernui.forge.mixin.AccessVideoSettingsScreen;
import icyllis.modernui.forge.network.NetworkHandler;
import icyllis.modernui.mcimpl.MuiRegistries;
import icyllis.modernui.mcimpl.TestMenu;
import icyllis.modernui.plugin.IMuiPlugin;
import icyllis.modernui.plugin.MuiPlugin;
import icyllis.modernui.view.UIManager;
import net.minecraft.client.CycleOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Option;
import net.minecraft.client.ProgressOption;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.fml.unsafe.UnsafeHacks;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.Type;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * This class handles mod loading events
 */
@Mod.EventBusSubscriber(modid = ModernUI.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
final class Registration {

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    static void registerSounds(@Nonnull RegistryEvent.Register<SoundEvent> event) {
        final IForgeRegistry<SoundEvent> registry = event.getRegistry();

        MuiRegistries.BUTTON_CLICK_1 = registerSound(registry, "button1");
        MuiRegistries.BUTTON_CLICK_2 = registerSound(registry, "button2");
    }

    @SubscribeEvent
    static void registerMenus(@Nonnull RegistryEvent.Register<MenuType<?>> event) {
        final IForgeRegistry<MenuType<?>> registry = event.getRegistry();
        MuiRegistries.TEST_MENU = registerMenu(registry, TestMenu::new, "test");
    }

    @SubscribeEvent
    static void registerItems(@Nonnull RegistryEvent.Register<Item> event) {
        if (ModernUIForge.production) {
            Item.Properties properties = new Item.Properties().stacksTo(1).setISTER(() -> ProjectBuilderRenderer::new);
            event.getRegistry().register(MuiRegistries.PROJECT_BUILDER_ITEM = new Item(properties).setRegistryName("project_builder"));
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    static void loadingClient(ParticleFactoryRegisterEvent event) {
        // this event fired after LOAD_REGISTRIES and before COMMON_SETUP on client main thread
        // we use this because we want a ResourceReloadListener after language data reloaded
    }

    @SubscribeEvent
    static void setupCommon(@Nonnull FMLCommonSetupEvent event) {
        Map<String, IMuiPlugin> plugins = new HashMap<>();
        Type target = Type.getType(MuiPlugin.class);
        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData data : scanData.getAnnotations()) {
                if (data.getAnnotationType().equals(target)) {
                    try {
                        plugins.putIfAbsent((String) data.getAnnotationData().get("namespace"),
                                UnsafeHacks.newInstance(Class.forName(data.getMemberName()).asSubclass(IMuiPlugin.class)));
                    } catch (Throwable throwable) {
                        ModernUI.LOGGER.error(ModernUI.MARKER, "Failed to load plugin: {}", data.getMemberName(), throwable);
                    }
                }
            }
        }

        byte[] protocol = null;
        try (InputStream stream = ModernUI.class.getClassLoader().getResourceAsStream(
                NetMessages.class.getName().replace('.', '/') + ".class")) {
            if (stream != null) {
                protocol = IOUtils.toByteArray(stream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        protocol = ArrayUtils.addAll(protocol, ModList.get().getModFileById(ModernUI.ID).getTrustData()
                .map(s -> s.getBytes(StandardCharsets.UTF_8)).orElse(null));
        final boolean optional;
        if (plugins.isEmpty()) {
            optional = true;
        } else {
            optional = false;
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Found Modern UI plugins: {}", plugins.keySet());
        }

        NetMessages.network = new NetworkHandler(ModernUI.ID, "main_network", () -> NetMessages::handle,
                NetMessages::handle, protocol == null ? null : DigestUtils.md5Hex(protocol), optional);

        //

        plugins.clear();

        MinecraftForge.EVENT_BUS.register(ServerHandler.INSTANCE);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    static void setupClient(@Nonnull FMLClientSetupEvent event) {
        //SettingsManager.INSTANCE.buildAllSettings();
        //UIManager.getInstance().registerMenuScreen(Registration.TEST_MENU, menu -> new TestUI());

        ModernUIForge.EVENT_BUS.register(EventHandler.Internal.class);
        event.getMinecraftSupplier().get().submitAsync(() -> {
            UIManager.initialize();
            ModernFontRenderer.change(Config.CLIENT.globalRenderer.get(), Config.CLIENT.allowShadow.get());
        });

        AccessOption.setGuiScale(new CycleOption("options.guiScale",
                (options, integer) -> options.guiScale = Integer.remainderUnsigned(
                        options.guiScale + integer, (MuiHooks.C.calcGuiScales() & 0xf) + 1),
                (options, cycleOption) -> options.guiScale == 0 ?
                        ((AccessOption) cycleOption).callGenericValueLabel(new TranslatableComponent("options.guiScale.auto")
                                .append(new TextComponent(" (" + (MuiHooks.C.calcGuiScales() >> 4 & 0xf) + ")"))) :
                        ((AccessOption) cycleOption).callGenericValueLabel(new TextComponent(Integer.toString(options.guiScale))))
        );

        Option[] settings = null;
        if (ModernUIForge.isOptiFineLoaded()) {
            try {
                Field field = VideoSettingsScreen.class.getDeclaredField("videoOptions");
                field.setAccessible(true);
                settings = (Option[]) field.get(null);
            } catch (Exception e) {
                ModernUI.LOGGER.error(ModernUI.MARKER, "Failed to be compatible with OptiFine video settings", e);
            }
        } else {
            settings = AccessVideoSettingsScreen.getOptions();
        }
        if (settings != null)
            for (int i = 0; i < settings.length; i++) {
                if (settings[i] == Option.GUI_SCALE) {
                    ProgressOption option = new ProgressOption("options.guiScale", 0, 2, 1,
                            options -> (double) options.guiScale,
                            (options, aDouble) -> {
                                if (options.guiScale != aDouble.intValue()) {
                                    options.guiScale = aDouble.intValue();
                                    Minecraft.getInstance().resizeDisplay();
                                }
                            },
                            (options, progressOption) -> options.guiScale == 0 ?
                                    ((AccessOption) progressOption)
                                            .callGenericValueLabel(new TranslatableComponent("options.guiScale.auto")
                                                    .append(new TextComponent(" (" + (MuiHooks.C.calcGuiScales() >> 4 & 0xf) + ")"))) :
                                    ((AccessOption) progressOption)
                                            .callGenericValueLabel(new TextComponent(Integer.toString(options.guiScale)))
                    );
                    settings[i] = EventHandler.Client.NEW_GUI_SCALE = option;
                    break;
                }
            }
        else ModernUI.LOGGER.error(ModernUI.MARKER, "Failed to capture video settings");
    }

    @Nonnull
    @OnlyIn(Dist.CLIENT)
    public static SoundEvent registerSound(@Nonnull IForgeRegistry<SoundEvent> registry, String name) {
        ResourceLocation soundID = new ResourceLocation(ModLoadingContext.get().getActiveNamespace(), name);
        SoundEvent event = new SoundEvent(soundID).setRegistryName(soundID);
        registry.register(event);
        return event;
    }

    @Nonnull
    public static <T extends AbstractContainerMenu> MenuType<T> registerMenu(
            @Nonnull IForgeRegistry<MenuType<?>> registry, IContainerFactory<T> factory, String name) {
        MenuType<T> type = IForgeContainerType.create(factory);
        type.setRegistryName(name);
        registry.register(type);
        return type;
    }
}
