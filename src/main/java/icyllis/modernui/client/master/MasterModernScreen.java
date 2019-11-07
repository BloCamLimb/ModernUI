package icyllis.modernui.client.master;

import icyllis.modernui.api.internal.IModuleManager;
import icyllis.modernui.api.module.IModuleInjector;
import icyllis.modernui.client.manager.ModuleManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class MasterModernScreen<T extends IModuleInjector> extends Screen {

    private IModuleManager manager = new ModuleManager();

    public MasterModernScreen(T injector) {
        super(injector.getTitle());
        injector.injectModules(this.manager);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        manager.draw();
    }
}
