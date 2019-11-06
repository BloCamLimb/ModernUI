package icyllis.modernui.client.module;

import icyllis.modernui.api.module.IModernModule;
import icyllis.modernui.api.module.IModuleTracker;

public class TestMainModule implements IModernModule {

    private IModuleTracker tracker;

    @Override
    public void initTrack(IModuleTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public IModuleTracker getTracker() {
        return tracker;
    }
}
