package icyllis.modernui.client.internal;

import icyllis.modernui.api.module.IModernModule;
import icyllis.modernui.api.module.IModuleTracker;

public class ModuleTracker implements IModuleTracker {

    private IModernModule module;

    public ModuleTracker(IModernModule module) {
        this.module = module;
    }

    @Override
    public IModernModule getModule() {
        return module;
    }
}
