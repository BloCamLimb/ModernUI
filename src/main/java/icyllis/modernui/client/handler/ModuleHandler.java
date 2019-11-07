package icyllis.modernui.client.handler;

import icyllis.modernui.api.internal.IModuleReceiver;
import icyllis.modernui.api.module.IModernModule;
import icyllis.modernui.api.module.IModuleHandler;
import icyllis.modernui.client.master.MasterModule;

public enum ModuleHandler implements IModuleHandler {
    INSTANCE;

    private MasterModule masterModule;
    private IModuleReceiver receiver;

    public void setup() {

    }

    public IModuleHandler track(IModuleReceiver receiver, IModernModule modernModule) {
        this.masterModule = new MasterModule(modernModule);
        this.receiver = receiver;
        return this;
    }

    @Override
    public IModuleHandler setMain() {
        masterModule.setMain();
        return this;
    }

    @Override
    public void finish() {
        receiver.insertModule(masterModule);
    }
}
