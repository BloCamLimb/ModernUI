package icyllis.modern.ui.master;

import com.mojang.blaze3d.platform.GlStateManager;
import icyllis.modern.api.global.IElementBuilder;
import icyllis.modern.api.global.IModuleFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GlobalModuleManager implements IModuleFactory {

    static final GlobalModuleManager INSTANCE = new GlobalModuleManager();

    private List<MasterModule> allModules = new ArrayList<>();

    private MasterModule currentModule;

    public void build(IMasterScreen master, int width, int height) {
        allModules.forEach(m -> m.build(master, width, height));
    }

    public void draw() {
        GlStateManager.enableBlend();
        currentModule.draw();
    }

    public void resize(int width, int height) {
        allModules.forEach(m -> m.resize(width, height));
    }

    public void clear() {
        allModules.clear();
        GlobalAnimationManager.INSTANCE.clearAll();
    }

    @Override
    public IModuleFactory add(Consumer<IElementBuilder> module) {
        MasterModule masterModule = new MasterModule(module);
        allModules.add(masterModule);
        if (allModules.size() == 1) {
            currentModule = masterModule;
        }
        return this;
    }
}
