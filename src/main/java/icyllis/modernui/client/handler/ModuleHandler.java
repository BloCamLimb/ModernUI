package icyllis.modernui.client.handler;

import icyllis.modernui.api.internal.IMasterModule;
import icyllis.modernui.api.internal.IModuleReceiver;
import icyllis.modernui.api.module.IModernModule;
import icyllis.modernui.api.module.IModuleHandler;
import icyllis.modernui.client.master.MasterModule;

public enum ModuleHandler implements IModuleHandler {
    INSTANCE;

    private IMasterModule masterModule;

    private IModuleReceiver receiver;

    public void setup() {

    }

    @Override
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
        receiver.receiveModule(masterModule);
        this.masterModule = null;
        this.receiver = null;
    }
}
