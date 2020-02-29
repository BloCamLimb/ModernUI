package icyllis.modernui.system;

import icyllis.modernui.api.ModernUI_API;
import icyllis.modernui.api.handler.IGuiHandler;
import icyllis.modernui.impl.chat.GuiChat;
import icyllis.modernui.gui.blur.BlurHandler;
import icyllis.modernui.gui.font.TrueTypeRenderer;
import icyllis.modernui.gui.master.GlobalAnimationManager;
import icyllis.modernui.gui.test.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber
public class EventsHandler {

    @SubscribeEvent
    public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
        if(!event.getPlayer().getEntityWorld().isRemote && event.getItemStack().getItem().equals(Items.DIAMOND)) {
            ModernUI_API.INSTANCE.getNetworkHandler().openGUI((ServerPlayerEntity) event.getPlayer(), new ContainerProvider(), new BlockPos(-155,82,-121));
        }
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
                GlobalAnimationManager.INSTANCE.renderTick(event.renderTickTime);
                TrueTypeRenderer.INSTANCE.init();
            } else {
                BlurHandler.INSTANCE.tick();
            }
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                GlobalAnimationManager.INSTANCE.clientTick();
                if (Minecraft.getInstance().gameSettings.keyBindDrop.isPressed()) {
                    Minecraft.getInstance().displayGuiScreen(new GuiChat());
                }
            }
        }

        @SuppressWarnings("UnclearExpression")
        @SubscribeEvent
        public static void onGuiOpen(GuiOpenEvent event) {
            boolean hasGui = event.getGui() != null;
            boolean current = Minecraft.getInstance().currentScreen != null;
            if (hasGui != current)
                GlobalAnimationManager.INSTANCE.resetTimer();
            BlurHandler.INSTANCE.blur(hasGui);
            //ModernUI.LOGGER.debug("Open GUI {}", hasGui ? event.getGui().getClass().getSimpleName() : "null");
        }
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModSetupHandler {

        private static Marker MARKER = MarkerManager.getMarker("SETUP");

        @SubscribeEvent
        public static void setupCommon(FMLCommonSetupEvent event) {
            ModernUI.LOGGER.info(MARKER, "{} has been initialized", ModernUI_API.INSTANCE.getDeclaringClass().getSimpleName());
        }

        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void setupClient(FMLClientSetupEvent event) {
            IGuiHandler guiHandler = ModernUI_API.INSTANCE.getGuiHandler();
            guiHandler.registerContainerGui(UILibs.TEST_CONTAINER_SCREEN, ContainerTest::new, l -> l.add(new ModuleTest()::create));
            HistoryRecorder.gEmojiPair();
        }

    }
}