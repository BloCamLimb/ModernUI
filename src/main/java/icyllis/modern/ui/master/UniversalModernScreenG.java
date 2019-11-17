package icyllis.modern.ui.master;

import icyllis.modern.api.module.IModernScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class UniversalModernScreenG<G extends Container> extends ContainerScreen<G> {

    public UniversalModernScreenG(IModernScreen injector, G container, ITextComponent name) {
        super(container, Minecraft.getInstance().player.inventory, name);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {

    }
}
