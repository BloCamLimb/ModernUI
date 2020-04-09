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
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.master.ModernUIScreen;
import icyllis.modernui.shader.BlurHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber
public class EventsHandler {

    /*@SubscribeEvent
    public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {

    }

    @SubscribeEvent
    public static void onContainerClosed(PlayerContainerEvent.Close event) {

    }*/

    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(Dist.CLIENT)
    public static class ClientEventHandler {

        @SubscribeEvent
        public static void onRenderTick(TickEvent.RenderTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                TrueTypeRenderer.INSTANCE.init();
                GlobalModuleManager.INSTANCE.renderTick(event.renderTickTime);
            } else {
                BlurHandler.INSTANCE.tick();
            }
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                GlobalModuleManager.INSTANCE.clientTick();
            }
        }

        @SubscribeEvent
        public static void onGuiOpen(GuiOpenEvent event) {
            GlobalModuleManager.INSTANCE.resetTicks();
            boolean hasGui = event.getGui() != null && !(event.getGui() instanceof ChatScreen);
            BlurHandler.INSTANCE.blur(hasGui);
        }

        @SubscribeEvent
        public static void onGuiInit(GuiScreenEvent.InitGuiEvent event) {

        }
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
            ModernUI.LOGGER.debug(MARKER, "{} has been initialized", SettingsManager.INSTANCE.getDeclaringClass().getSimpleName()); // call constructor methods
        }

        @SubscribeEvent
        public static void onConfigChange(ModConfig.ModConfigEvent event) {
            ConfigManager.loadConfig(event.getConfig().getSpec());
        }

    }
}
