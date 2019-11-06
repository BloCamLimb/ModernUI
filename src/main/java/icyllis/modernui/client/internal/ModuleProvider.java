package icyllis.modernui.client.internal;

import icyllis.modernui.api.module.IModernModule;
import icyllis.modernui.api.module.IModuleProvider;
import icyllis.modernui.api.module.IModuleTracker;

import java.util.ArrayList;
import java.util.List;

public class ModuleProvider implements IModuleProvider {

    private List<IModuleTracker> trackers = new ArrayList<>();

    @Override
    public IModuleTracker injectModule(IModernModule module) {
        return new ModuleTracker(module);
    }
}
