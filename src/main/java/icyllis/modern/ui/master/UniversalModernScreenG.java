package icyllis.modern.ui.master;

import icyllis.modern.api.module.IGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.inventory.container.Container;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public final class UniversalModernScreenG<G extends Container> extends ContainerScreen<G> implements IMasterScreen {

    private GlobalModuleManager manager = GlobalModuleManager.INSTANCE;

    public UniversalModernScreenG(IGuiScreen injector, G container) {
        super(container, Minecraft.getInstance().player.inventory, injector.getTitle());
        injector.addModules(manager);
    }

    @Override
    protected void init() {
        super.init();
        manager.build(this, width, height);
    }

    @Override
    public void resize(@Nonnull Minecraft minecraft, int width, int height) {
        //super.resize(minecraft, width, height);
        manager.resize(width, height);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {

    }

    @Override
    public void addChild(IGuiEventListener eventListener) {
        children.add(eventListener);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        manager.draw();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        manager.clear();
    }

    @Override
    public void mouseMoved(double p_212927_1_, double p_212927_3_) {
        children.forEach(e -> e.mouseMoved(p_212927_1_, p_212927_3_));
    }
}
