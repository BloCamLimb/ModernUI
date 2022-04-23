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
import icyllis.modernui.mixin.AccessOption;
import icyllis.modernui.mixin.AccessVideoSettings;
import icyllis.modernui.platform.RenderCore;
import icyllis.modernui.screen.UIManager;
import icyllis.modernui.test.TestMenu;
import icyllis.modernui.textmc.TextLayoutEngine;
import net.minecraft.client.*;
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
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static icyllis.modernui.ModernUI.LOGGER;

/**
 * This class handles mod loading events
 */
@Mod.EventBusSubscriber(modid = ModernUI.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
final class Registration {

    private Registration() {
    }

    @SubscribeEvent
    static void registerMenus(@Nonnull RegistryEvent.Register<MenuType<?>> event) {
        if (ModernUIForge.sDevelopment) {
            event.getRegistry().register(IForgeContainerType.create(TestMenu::new)
                    .setRegistryName("test"));
        }
    }

    @SubscribeEvent
    static void registerItems(@Nonnull RegistryEvent.Register<Item> event) {
        if (ModernUIForge.sDevelopment) {
            Item.Properties properties = new Item.Properties().stacksTo(1);
            event.getRegistry().register(new Item(properties)
                    .setRegistryName("project_builder"));
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    static void loadingClient(ParticleFactoryRegisterEvent event) {
        // this event fired after LOAD_REGISTRIES and before COMMON_SETUP on render thread
        // we use this because we want a ResourceReloadListener after language data reloaded
        RenderCore.initialize();
        UIManager.initialize();
    }

    @SubscribeEvent
    static void setupCommon(@Nonnull FMLCommonSetupEvent event) {
        /*byte[] protocol = null;
        try (InputStream stream = ModernUI.class.getClassLoader().getResourceAsStream(
                NetworkMessages.class.getName().replace('.', '/') + ".class")) {
            Objects.requireNonNull(stream, "Mod file is broken");
            protocol = IOUtils.toByteArray(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (InputStream stream = ModernUI.class.getClassLoader().getResourceAsStream(
                NetworkMessages.class.getName().replace('.', '/') + "$C.class")) {
            Objects.requireNonNull(stream, "Mod file is broken");
            protocol = ArrayUtils.addAll(protocol, IOUtils.toByteArray(stream));
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        if (ModList.get().getModContainerById(new String(new byte[]{0x1f ^ 0x74, (0x4 << 0x1) | 0x41,
                ~-0x78, 0xd2 >> 0x1}, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT)).isPresent()) {
            event.enqueueWork(() -> LOGGER.fatal("OK"));
        }
        /*protocol = ArrayUtils.addAll(protocol, ModList.get().getModFileById(ModernUI.ID).getTrustData()
                .map(s -> s.getBytes(StandardCharsets.UTF_8)).orElse(null));*/

        NetworkMessages.sNetwork = new NetworkHandler(ModernUI.ID, "main_network", () -> NetworkMessages::handle,
                NetworkMessages::handle, "270", true);

        MinecraftForge.EVENT_BUS.register(ServerHandler.INSTANCE);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    static void setupClient(@Nonnull FMLClientSetupEvent event) {
        //SettingsManager.INSTANCE.buildAllSettings();
        //UIManager.getInstance().registerMenuScreen(Registration.TEST_MENU, menu -> new TestUI());
        Minecraft.getInstance().execute(() -> TextLayoutEngine.getInstance().lookupVanillaNode(ModernUI.NAME_CPT));

        AccessOption.setGuiScale(new CycleOption("options.guiScale",
                (options, integer) -> options.guiScale = Integer.remainderUnsigned(
                        options.guiScale + integer, (MForgeCompat.calcGuiScales() & 0xf) + 1),
                (options, cycleOption) -> options.guiScale == 0 ?
                        ((AccessOption) cycleOption).callGenericValueLabel(new TranslatableComponent("options" +
                                ".guiScale.auto")
                                .append(new TextComponent(" (" + (MForgeCompat.calcGuiScales() >> 4 & 0xf) + ")"))) :
                        ((AccessOption) cycleOption).callGenericValueLabel(new TextComponent(Integer.toString(options.guiScale))))
        );

        Option[] settings = null;
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
                                                    .append(new TextComponent(" (" + (MForgeCompat.calcGuiScales() >> 4 & 0xf) + ")"))) :
                                    ((AccessOption) progressOption)
                                            .callGenericValueLabel(new TextComponent(Integer.toString(options.guiScale)))
                    );
                    settings[i] = EventHandler.Client.sNewGuiScale = option;
                    break;
                }
            }
        } else {
            LOGGER.error(ModernUI.MARKER, "Failed to capture video settings");
        }
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
