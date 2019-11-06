package icyllis.modernui.core.subscriber;

import icyllis.modernui.client.handler.AnimationHandler;
import icyllis.modernui.client.handler.ModuleHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetupHandler {

    @SubscribeEvent
    public static void setupClient(FMLClientSetupEvent event) {
        AnimationHandler.INSTANCE.setup();
        ModuleHandler.INSTANCE.setup();
    }
}
