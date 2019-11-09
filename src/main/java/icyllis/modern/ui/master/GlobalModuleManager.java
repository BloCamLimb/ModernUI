package icyllis.modern.ui.master;

import icyllis.modern.api.internal.IMasterModule;
import icyllis.modern.api.internal.IModuleManager;
import icyllis.modern.api.module.IModernModule;
import icyllis.modern.api.tracker.IModuleTracker;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

public class GlobalModuleManager implements IModuleManager {

    public static final GlobalModuleManager INSTANCE = new GlobalModuleManager();

    private FontRenderer fontRenderer;
    private int width, height;

    private List<IMasterModule> modules = new ArrayList<>();

    @Override
    public IModuleTracker injectModule(IModernModule module) {
        MasterModule masterModule = new MasterModule(module);
        modules.add(masterModule);
        return masterModule;
    }

    @Override
    public void build() {
        modules.stream().filter(m -> m.trigger(-1)).findFirst().ifPresent(m -> m.bake(fontRenderer, width, height));
    }

    @Override
    public void draw() {
        modules.forEach(IMasterModule::draw);
    }

    @Override
    public void init(FontRenderer fontRenderer, int width, int height) {
        this.fontRenderer = fontRenderer;
        resize(width, height);
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void clear() {
        modules.clear();
    }
}
