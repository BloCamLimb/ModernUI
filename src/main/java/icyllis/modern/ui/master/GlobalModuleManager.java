package icyllis.modern.ui.master;

import icyllis.modern.api.internal.IMasterModule;
import icyllis.modern.api.internal.IModuleManager;
import icyllis.modern.api.module.IModernModule;
import icyllis.modern.api.module.IModuleTracker;
import icyllis.modern.core.ModernUI;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

public class GlobalModuleManager implements IModuleManager {

    static final GlobalModuleManager INSTANCE = new GlobalModuleManager();

    private List<IMasterModule> modules = new ArrayList<>();

    @Override
    public IModuleTracker injectModule(IModernModule module) {
        MasterModule masterModule = new MasterModule(module);
        modules.add(masterModule);
        return masterModule;
    }

    @Override
    public void build(FontRenderer fontRenderer, int width, int height) {
        modules.forEach(m -> m.bake(fontRenderer, width, height));
    }

    @Override
    public void draw() {
        modules.forEach(IMasterModule::draw);
    }

    @Override
    public void resize(int width, int height) {
        modules.forEach(m -> m.resize(width, height));
    }

    @Override
    public void clear() {
        modules.clear();
    }
}
