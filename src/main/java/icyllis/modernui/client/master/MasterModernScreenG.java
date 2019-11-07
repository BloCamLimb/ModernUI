package icyllis.modernui.client.master;

import icyllis.modernui.api.module.IModuleInjector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.inventory.container.Container;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MasterModernScreenG<T extends ContainerScreen & IModuleInjector, G extends Container> extends ContainerScreen<G> {

    private T screen;

    public MasterModernScreenG(T screen, G container) {
        super(container, Minecraft.getInstance().player.inventory, screen.getTitle());
        this.screen = screen;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {

    }
}
