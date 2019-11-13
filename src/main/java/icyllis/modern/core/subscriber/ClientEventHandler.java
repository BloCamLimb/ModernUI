package icyllis.modern.core.subscriber;

import icyllis.modern.ui.master.GlobalAnimationManager;
import icyllis.modern.ui.master.GlobalModuleManager;
import icyllis.modern.ui.master.UniversalModernScreen;
import icyllis.modern.ui.test.TestScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.FMLNetworkConstants;

@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if(event.phase == TickEvent.Phase.START) {
            GlobalAnimationManager.INSTANCE.tick(event.renderTickTime);
        }
    }

    @SubscribeEvent
    public static void onGuiOpen(GuiOpenEvent event) {
        /*if(event.getGui() instanceof IModuleInjector) {
            if(event.getGui() instanceof IHasContainer) {
                event.setGui(new MasterModernScreenG<>((ContainerScreen & IModuleInjector) event.getGui(), ((ContainerScreen) event.getGui()).getContainer()));
            } else {
                event.setGui(new MasterModernScreen<>((Screen & IModuleInjector) event.getGui()));
            }
        }*/
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if(event.phase == TickEvent.Phase.START) {
            GlobalAnimationManager.INSTANCE.tick();
            Minecraft mc = Minecraft.getInstance();
            Screen gui = mc.currentScreen;
            if(gui == null || !gui.isPauseScreen())
                if(gui == null && mc.gameSettings.keyBindDrop.isPressed()) {
                    mc.displayGuiScreen(new UniversalModernScreen(new TestScreen()));
                }
        }
    }
}
