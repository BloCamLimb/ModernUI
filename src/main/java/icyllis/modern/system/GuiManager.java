package icyllis.modern.system;

import icyllis.modern.api.global.IContainerFactory;
import icyllis.modern.api.global.IGuiManager;
import icyllis.modern.api.module.IModernGui;
import icyllis.modern.ui.master.UniversalModernScreen;
import icyllis.modern.ui.master.UniversalModernScreenG;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CCloseWindowPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public enum GuiManager implements IGuiManager {
    INSTANCE;

    private final Map<ResourceLocation, IContainerFactory> CONTAINERS = new HashMap<>();
    private final Map<ResourceLocation, Supplier<IModernGui>> SCREENS = new HashMap<>();

    public void openContainerScreen(ResourceLocation id, int windowId, PacketBuffer extraData) {
        if (SCREENS.containsKey(id) && CONTAINERS.containsKey(id)) {
            IModernGui screen = SCREENS.get(id).get();
            IContainerFactory factory = CONTAINERS.get(id);
            PacketBuffer copied = new PacketBuffer(extraData.copy());
            Container container = factory.create(windowId, Minecraft.getInstance().player.inventory, extraData);
            screen.updateData(copied);
            Minecraft.getInstance().player.openContainer = container;
            Minecraft.getInstance().displayGuiScreen(new UniversalModernScreenG<>(screen, container));
        } else {
            Minecraft.getInstance().player.connection.sendPacket(new CCloseWindowPacket(windowId));
        }
    }

    @Override
    public <M extends Container> void registerContainerGui(ResourceLocation id, IContainerFactory<M> factory, Supplier<IModernGui> screen) {
        SCREENS.put(id, screen);
        CONTAINERS.put(id, factory);
    }

    @Override
    public void openGui(Supplier<IModernGui> supplier) {
        Minecraft.getInstance().displayGuiScreen(new UniversalModernScreen(supplier.get()));
    }

}
