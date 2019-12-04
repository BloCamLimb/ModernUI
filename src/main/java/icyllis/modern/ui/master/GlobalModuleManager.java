package icyllis.modern.ui.master;

import icyllis.modern.api.global.IModuleList;
import icyllis.modern.api.module.IGuiModule;

import java.util.ArrayList;
import java.util.List;

public class GlobalModuleManager implements IModuleList {

    static final GlobalModuleManager INSTANCE = new GlobalModuleManager();

    private List<MasterModule> modules = new ArrayList<>();
    private MasterModule currentModule;

    @Override
    public void add(IGuiModule module) {
        MasterModule masterModule = new MasterModule(module);
        modules.add(masterModule);
        if (modules.size() == 1) {
            currentModule = masterModule;
        }
    }

    public void build(IMasterScreen master, int width, int height) {
        modules.forEach(m -> m.build(master, width, height));
    }

    public void draw() {
       currentModule.draw();
    }

    public void resize(int width, int height) {
        modules.forEach(m -> m.resize(width, height));
    }

    public void clear() {
        modules.clear();
    }
}
