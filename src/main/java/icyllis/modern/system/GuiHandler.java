package icyllis.modern.system;

import icyllis.modern.api.global.IContainerFactory;
import icyllis.modern.api.global.IGuiHandler;
import icyllis.modern.api.module.IGuiScreen;
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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public enum GuiHandler implements IGuiHandler {
    INSTANCE;

    /*private final Map<ResourceLocation, IContainerFactory> CONTAINERS = new HashMap<>();
    private final Map<ResourceLocation, Function<PacketBuffer, IGuiScreen>> SCREENS = new HashMap<>();*/

    private final Map<ResourceLocation, Pair<IContainerFactory<? extends Container>, Function<PacketBuffer, IGuiScreen>>> CONTAINER_SCREENS = new HashMap<>();

    public void openContainerScreen(ResourceLocation id, int windowId, PacketBuffer extraData) {
        if (CONTAINER_SCREENS.containsKey(id)) {
            Pair<IContainerFactory<? extends Container>, Function<PacketBuffer, IGuiScreen>> pair = CONTAINER_SCREENS.get(id);
            PacketBuffer copied = new PacketBuffer(extraData.copy());
            IContainerFactory factory = pair.getKey();
            Container container = factory.create(windowId, Minecraft.getInstance().player.inventory, extraData);
            Minecraft.getInstance().player.openContainer = container;
            IGuiScreen screen = pair.getValue().apply(copied);
            Minecraft.getInstance().displayGuiScreen(new UniversalModernScreenG<>(screen, container));
        } else {
            Minecraft.getInstance().player.connection.sendPacket(new CCloseWindowPacket(windowId));
        }
    }

    @Override
    public <M extends Container> void registerContainerGui(ResourceLocation id, IContainerFactory<M> factory, Function<PacketBuffer, IGuiScreen> gui) {
        CONTAINER_SCREENS.put(id, new Pair<>(factory, gui));
    }

    @Override
    public void openGui(Supplier<IGuiScreen> supplier) {
        Minecraft.getInstance().displayGuiScreen(new UniversalModernScreen(supplier.get()));
    }

}
