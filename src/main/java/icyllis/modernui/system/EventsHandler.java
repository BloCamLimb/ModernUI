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

import icyllis.modernui.font.TrueTypeRenderer;
import icyllis.modernui.graphics.BlurHandler;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.test.ContainerTest;
import icyllis.modernui.gui.test.ModuleTest;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Items;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.network.NetworkHooks;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nonnull;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber
public class EventsHandler {

    @SubscribeEvent
    public static void rightClickItem(@Nonnull PlayerInteractEvent.RightClickItem event) {
        if (event.getSide().isServer() && event.getItemStack().getItem().equals(Items.DIAMOND)) {
            NetworkHooks.openGui((ServerPlayerEntity) event.getPlayer(), new ContainerTest.Provider());
        }
    }

    /*@SubscribeEvent
    public static void onContainerClosed(PlayerContainerEvent.Close event) {

    }*/

    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(Dist.CLIENT)
    public static class ClientEventHandler {

        @SubscribeEvent
        public static void onRenderTick(@Nonnull TickEvent.RenderTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                TrueTypeRenderer.INSTANCE.init();
                GlobalModuleManager.INSTANCE.onRenderTick(event.renderTickTime);
            } else {
                BlurHandler.INSTANCE.renderTick();
            }
        }

        @SubscribeEvent
        public static void onClientTick(@Nonnull TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                GlobalModuleManager.INSTANCE.onClientTick();
            }
        }

        @SubscribeEvent
        public static void onGuiOpen(@Nonnull GuiOpenEvent event) {
            GlobalModuleManager.INSTANCE.onOpenGui(event.getGui(), event::setCanceled);
            BlurHandler.INSTANCE.blur(event.getGui());
        }

        @SubscribeEvent
        public static void onRenderAttackIndicator(@Nonnull RenderGameOverlayEvent.Pre event) {
            if (event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS && GlobalModuleManager.INSTANCE.getModernScreen() != null) {
                event.setCanceled(true);
            }
        }

        /*@SubscribeEvent
        public static void onGuiInit(GuiScreenEvent.InitGuiEvent event) {

        }*/
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEventHandler {

        private static Marker MARKER = MarkerManager.getMarker("SETUP");

        @SubscribeEvent
        public static void setupCommon(FMLCommonSetupEvent event) {
            ModIntegration.init();
        }

        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void setupClient(FMLClientSetupEvent event) {
            SettingsManager.INSTANCE.buildAllSettings();
            GlobalModuleManager.INSTANCE.registerContainerScreen(ContainerTest.CONTAINER, c -> ModuleTest::new);
        }

        @SubscribeEvent
        public static void registerContainers(@Nonnull RegistryEvent.Register<ContainerType<?>> event) {
            event.getRegistry().register(ContainerTest.CONTAINER);
        }

        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void registerSounds(@Nonnull RegistryEvent.Register<SoundEvent> event) {
            RegistrySounds.registerSounds(event.getRegistry());
        }

        @SubscribeEvent
        public static void onConfigChange(@Nonnull ModConfig.ModConfigEvent event) {
            ConfigManager.load(event.getConfig().getSpec());
        }

        @SubscribeEvent
        public static void onLoadComplete(FMLLoadCompleteEvent event) {
            RewrittenMethods.loadCompleted = true;
        }

    }
}
