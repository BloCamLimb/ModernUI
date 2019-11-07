package icyllis.modernui.client.master;

import icyllis.modernui.api.internal.IMasterModule;
import icyllis.modernui.api.module.IMasterModuleSetter;
import icyllis.modernui.api.module.IModernModule;

public final class MasterModule implements IMasterModule, IMasterModuleSetter {

    private IModernModule rawModule;

    private boolean isMain = false;

    public MasterModule(IModernModule rawModule) {
        this.rawModule = rawModule;
    }

    @Override
    public void draw() {

    }

    @Override
    public void setMain() {
        this.isMain = true;
    }
}
