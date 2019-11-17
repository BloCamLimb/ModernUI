package icyllis.modern.core;

import icyllis.modern.api.ModernUIAPI;
import icyllis.modern.ui.font.StringRenderer;
import icyllis.modern.ui.master.GlobalAnimationManager;
import icyllis.modern.ui.test.ContainerHolder;
import icyllis.modern.ui.test.RegistryScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber
public class GlobalEventHandler {

    @SubscribeEvent
    public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
        if(!event.getPlayer().getEntityWorld().isRemote && event.getItemStack().getItem().equals(Items.DIAMOND)) {
            ModernUIAPI.INSTANCE.network().openGUI((ServerPlayerEntity) event.getPlayer(), new ContainerHolder(), new BlockPos(-155,82,-121));
        }
    }

    @SubscribeEvent
    public static void onContainerClosed(PlayerContainerEvent.Close event) {
        ModernUI.logger.info("Container closed: {}", event.getContainer());
    }

    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(Dist.CLIENT)
    public static class ClientEventHandler {

        @SubscribeEvent
        public static void onRenderTick(TickEvent.RenderTickEvent event) {
            if(event.phase == TickEvent.Phase.START) {
                GlobalAnimationManager.INSTANCE.tick(event.renderTickTime);
                StringRenderer.DEFAULT_FONT_RENDERER.init();
            }
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if(event.phase == TickEvent.Phase.START) {
                GlobalAnimationManager.INSTANCE.tick();
                Minecraft mc = Minecraft.getInstance();
                Screen gui = mc.currentScreen;
                if(gui == null || !gui.isPauseScreen())
                    if(gui == null && mc.gameSettings.keyBindDrop.isPressed()) {
                        ModernUIAPI.INSTANCE.screen().openScreen(RegistryScreens.TEST_CONTAINER_SCREEN);
                    }
            }
        }
    }
}
