package icyllis.modern.api.internal;

import icyllis.modern.api.module.IModernModule;
import icyllis.modern.api.module.IModuleTracker;

public interface IModuleReceiver {

    IModuleTracker receiveModule(IModernModule module);
}
