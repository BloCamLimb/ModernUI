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

import icyllis.modernui.shader.blur.BlurHandler;
import icyllis.modernui.font.TrueTypeRenderer;
import icyllis.modernui.gui.master.GlobalModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiOpenEvent;
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

    @SubscribeEvent
    public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
        /*if(!event.getPlayer().getEntityWorld().isRemote && event.getItemStack().getItem().equals(Items.DIAMOND)) {
            ModernUI_API.INSTANCE.getNetworkManager().openGUI((ServerPlayerEntity) event.getPlayer(), new ContainerProvider(), new BlockPos(-155,82,-121));
        }*/
    }

    @SubscribeEvent
    public static void onContainerClosed(PlayerContainerEvent.Close event) {
        //ModernUI.logger.info("Container closed: {}", event.getContainer());
    }

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

        @SuppressWarnings("UnclearExpression")
        @SubscribeEvent
        public static void onGuiOpen(GuiOpenEvent event) {
            boolean hasGui = event.getGui() != null;
            boolean current = Minecraft.getInstance().currentScreen != null;
            if (hasGui != current)
                GlobalModuleManager.INSTANCE.resetTicks();
            BlurHandler.INSTANCE.blur(hasGui);
            //ModernUI.LOGGER.debug("Open GUI {}", hasGui ? event.getGui().getClass().getSimpleName() : "null");
        }

        @SubscribeEvent
        public static void onConfigLoad(ModConfig.Loading event) {
            if (event.getConfig().getSpec().equals(ModernUI_Config.CLIENT_SPEC)) {
                ModernUI_Config.loadClientConfig();
            }
        }
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModSetupHandler {

        private static Marker MARKER = MarkerManager.getMarker("SETUP");

        @SubscribeEvent
        public static void setupCommon(FMLCommonSetupEvent event) {
            ModIntegration.init();
        }

        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void setupClient(FMLClientSetupEvent event) {
            //HistoryRecorder.gEmojiPair();
            ModernUI.LOGGER.debug(MARKER, "{} has been initialized", SettingsManager.INSTANCE.getDeclaringClass().getSimpleName()); // call constructor methods
        }

    }
}
