/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
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

import icyllis.modernui.ui.example.ContainerTest;
import icyllis.modernui.ui.example.TestFragment;
import icyllis.modernui.ui.master.UIManager;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nonnull;

/**
 * All references are final, read-only
 */
public enum RegistryLibrary {
    INSTANCE;

    /** Sounds **/
    public static SoundEvent BUTTON_CLICK_1 = null;
    public static SoundEvent BUTTON_CLICK_2 = null;

    /** Containers **/
    public static ContainerType<ContainerTest> TEST_CONTAINER = null;

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    void registerSounds(@Nonnull RegistryEvent.Register<SoundEvent> event) {
        IForgeRegistry<SoundEvent> registry = event.getRegistry();

        BUTTON_CLICK_1 = registerSound(registry, "button1");
        BUTTON_CLICK_2 = registerSound(registry, "button2");
    }

    @SubscribeEvent
    void registerContainers(@Nonnull RegistryEvent.Register<ContainerType<?>> event) {
        IForgeRegistry<ContainerType<?>> registry = event.getRegistry();

        TEST_CONTAINER = registerContainer(registry, ContainerTest::new, "test");
    }

    @SubscribeEvent
    void setupCommon(@Nonnull FMLCommonSetupEvent event) {
        NetworkManager.INSTANCE.registerMessages();
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    void setupClient(@Nonnull FMLClientSetupEvent event) {
        SettingsManager.INSTANCE.buildAllSettings();
        UIManager.INSTANCE.registerContainerScreen(RegistryLibrary.TEST_CONTAINER, c -> new TestFragment());
    }

    @Nonnull
    @OnlyIn(Dist.CLIENT)
    public static SoundEvent registerSound(@Nonnull IForgeRegistry<SoundEvent> registry, String name) {
        ResourceLocation soundID = new ResourceLocation(ModernUI.MODID, name);
        SoundEvent event = new SoundEvent(soundID).setRegistryName(soundID);
        registry.register(event);
        return event;
    }

    @Nonnull
    public static <T extends Container> ContainerType<T> registerContainer(@Nonnull IForgeRegistry<ContainerType<?>> registry, IContainerFactory<T> factory, String name) {
        ContainerType<T> type = IForgeContainerType.create(factory);
        type.setRegistryName(name);
        registry.register(type);
        return type;
    }
}
