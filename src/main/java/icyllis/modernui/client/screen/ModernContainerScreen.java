package icyllis.modernui.client.screen;

import icyllis.modernui.api.module.IModernScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.inventory.container.Container;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModernContainerScreen<T extends ContainerScreen & IModernScreen, G extends Container> extends ContainerScreen<G> {

    private T screen;

    public ModernContainerScreen(T screen, G container) {
        super(container, Minecraft.getInstance().player.inventory, screen.getTitle());
        this.screen = screen;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {

    }
}
