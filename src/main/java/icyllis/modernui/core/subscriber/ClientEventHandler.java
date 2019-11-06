package icyllis.modernui.core.subscriber;

import icyllis.modernui.api.module.IModernScreen;
import icyllis.modernui.client.screen.ModernContainerScreen;
import icyllis.modernui.client.screen.ModernScreen;
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
                event.setGui(new ModernContainerScreen<>((ContainerScreen & IModernScreen) event.getGui(), ((ContainerScreen) event.getGui()).getContainer()));
            } else {
                event.setGui(new ModernScreen<>((Screen & IModernScreen) event.getGui()));
            }
        }
    }
}
