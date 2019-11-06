package icyllis.modernui.api.module;

import icyllis.modernui.api.module.IModernModule;
import icyllis.modernui.api.module.IModuleTracker;

public interface IModuleProvider {

    IModuleTracker injectModule(IModernModule module);
}
