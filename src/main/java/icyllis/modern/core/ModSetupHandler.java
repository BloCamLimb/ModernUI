package icyllis.modern.core;

import icyllis.modern.api.ModernUIAPI;
import icyllis.modern.ui.test.RegistryScreens;
import icyllis.modern.ui.test.TestContainer;
import icyllis.modern.ui.test.TestScreen;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModSetupHandler {

    private static final Marker MARKER = MarkerManager.getMarker("SETUP");

    @SubscribeEvent
    public static void setupCommon(FMLCommonSetupEvent event) {

    }

    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientSetupHandler {

        @SubscribeEvent
        public static void setupClient(FMLClientSetupEvent event) {

        }

        @SubscribeEvent
        public static void registerContainers(RegistryEvent.Register<ContainerType<?>> event) {
            ModernUIAPI.INSTANCE.screen().registerContainerScreen(RegistryScreens.TEST_CONTAINER, TestContainer::new, TestScreen::new);
        }
    }

}
