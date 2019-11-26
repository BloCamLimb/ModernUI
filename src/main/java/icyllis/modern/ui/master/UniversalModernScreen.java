package icyllis.modern.ui.master;

import icyllis.modern.api.module.IModernGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public final class UniversalModernScreen extends Screen implements IMasterScreen {

    private GlobalModuleManager manager = GlobalModuleManager.INSTANCE;

    public UniversalModernScreen(IModernGui injector) {
        super(injector.getTitle());
        injector.createModules(manager);
    }

    @Override
    protected void init() {
        manager.build(this, width, height);
    }

    @Override
    public void resize(@Nonnull Minecraft minecraft, int width, int height) {
        manager.resize(width, height);
    }

    @Override
    public void mouseMoved(double xPos, double p_212927_3_) {

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

}
