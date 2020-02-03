package icyllis.modern.system;

import icyllis.modern.api.global.IContainerFactory;
import icyllis.modern.api.handler.IGuiHandler;
import icyllis.modern.api.global.IModuleFactory;
import icyllis.modern.ui.master.GlobalElementBuilder;
import icyllis.modern.ui.master.UniversalModernScreen;
import icyllis.modern.ui.master.UniversalModernScreenG;
import javafx.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CCloseWindowPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public enum GuiHandler implements IGuiHandler {
    INSTANCE;

    /*private final Map<ResourceLocation, IContainerFactory> CONTAINERS = new HashMap<>();
    private final Map<ResourceLocation, Function<PacketBuffer, IGuiScreen>> SCREENS = new HashMap<>();*/

    private final Map<ResourceLocation, Pair<IContainerFactory<? extends Container>, Consumer<IModuleFactory>>> CONTAINER_SCREENS = new HashMap<>();

    @SuppressWarnings("ConstantConditions")
    public void openContainerScreen(ResourceLocation id, int windowId, PacketBuffer extraData) {
        if (CONTAINER_SCREENS.containsKey(id)) {
            Pair<IContainerFactory<? extends Container>, Consumer<IModuleFactory>> pair = CONTAINER_SCREENS.get(id);
            GlobalElementBuilder.INSTANCE.setExtraData(new PacketBuffer(extraData.copy()));
            IContainerFactory factory = pair.getKey();
            Container container = factory.create(windowId, Minecraft.getInstance().player.inventory, extraData);
            Minecraft.getInstance().player.openContainer = container;
            Minecraft.getInstance().displayGuiScreen(new UniversalModernScreenG<>(pair.getValue(), container));
        } else {
            Minecraft.getInstance().player.connection.sendPacket(new CCloseWindowPacket(windowId));
        }
    }

    @Override
    public <M extends Container> void registerContainerGui(ResourceLocation id, IContainerFactory<M> containerFactory, Consumer<IModuleFactory> moduleFactory) {
        if (CONTAINER_SCREENS.containsKey(id)) {
            ModernUI.LOGGER.error("Duplicated ID when registering container gui ({})", id.toString());
        } else {
            CONTAINER_SCREENS.put(id, new Pair<>(containerFactory, moduleFactory));
        }
    }

    @Override
    public void openGui(Consumer<IModuleFactory> factoryConsumer) {
        Minecraft.getInstance().displayGuiScreen(new UniversalModernScreen(factoryConsumer));
    }

}
