package icyllis.modernui.gui.master;

import icyllis.modernui.api.global.IModuleFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.inventory.container.Container;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class UniversalModernScreenG<G extends Container> extends ContainerScreen<G> implements IMasterScreen {

    private GlobalModuleManager manager = GlobalModuleManager.INSTANCE;

    @SuppressWarnings("ConstantConditions")
    public UniversalModernScreenG(Consumer<IModuleFactory> factory, G container) {
        super(container, Minecraft.getInstance().player.inventory, UniversalModernScreen.EMPTY_TITLE);
        factory.accept(manager);
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
    public void addEventListener(IGuiEventListener eventListener) {
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
