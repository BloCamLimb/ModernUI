package icyllis.modern.ui.master;

import icyllis.modern.api.internal.IModuleManager;
import icyllis.modern.api.module.IModernScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public final class UniversalModernScreen extends Screen {

    private IModuleManager manager = GlobalModuleManager.INSTANCE;

    public UniversalModernScreen(IModernScreen injector) {
        super(injector.getTitle());
        injector.injectModules(manager);
    }

    @Override
    protected void init() {
        manager.build(font, width, height);
    }

    @Override
    public void resize(@Nonnull Minecraft minecraft, int width, int height) {
        manager.resize(width, height);
    }

    @Override
    public void mouseMoved(double xPos, double p_212927_3_) {

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
