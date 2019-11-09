package icyllis.modern.api.module;

import icyllis.modern.api.tracker.IModuleTracker;

public interface IModuleInjector {

    IModuleTracker injectModule(IModernModule module);
}
