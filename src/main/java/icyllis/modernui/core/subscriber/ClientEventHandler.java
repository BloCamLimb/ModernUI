package icyllis.modernui.core.subscriber;

import icyllis.modernui.api.module.IModernScreen;
import icyllis.modernui.client.screen.MasterModernScreenG;
import icyllis.modernui.client.screen.MasterModernScreen;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
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
        if(event.getGui() instanceof IModernScreen) {
            if(event.getGui() instanceof IHasContainer) {
                event.setGui(new MasterModernScreenG<>((ContainerScreen & IModernScreen) event.getGui(), ((ContainerScreen) event.getGui()).getContainer()));
            } else {
                event.setGui(new MasterModernScreen<>((Screen & IModernScreen) event.getGui()));
            }
        }
    }
}
