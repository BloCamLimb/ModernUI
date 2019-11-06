package icyllis.modernui.core.subscriber;

import icyllis.modernui.core.network.NetworkHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonSetupHandler {

    @SubscribeEvent
    public static void setupCommon(FMLCommonSetupEvent event) {
        NetworkHandler.INSTANCE.setup();
    }
}
