package icyllis.modernui.core.subscriber;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if(event.phase == TickEvent.Phase.START) {

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
}
