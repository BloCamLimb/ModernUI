package icyllis.modern.ui.master;

import icyllis.modern.api.internal.IModuleManager;
import icyllis.modern.api.module.IModernScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class UniversalModernScreen extends Screen {

    private IModuleManager manager = GlobalModuleManager.INSTANCE;

    public UniversalModernScreen(IModernScreen injector) {
        super(injector.getTitle());
        injector.injectModules(manager);
    }

    @Override
    protected void init() {
        manager.init(font, width, height);
        manager.build();
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
