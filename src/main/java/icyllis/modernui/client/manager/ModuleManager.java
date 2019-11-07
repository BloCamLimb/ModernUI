package icyllis.modernui.client.manager;

import icyllis.modernui.api.internal.IMasterModule;
import icyllis.modernui.api.internal.IModuleManager;
import icyllis.modernui.api.module.IModernModule;
import icyllis.modernui.api.module.IModuleHandler;
import icyllis.modernui.client.handler.ModuleHandler;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager implements IModuleManager {

    private List<IMasterModule> modules = new ArrayList<>();

    @Override
    public IModuleHandler injectModule(IModernModule module) {
        return ModuleHandler.INSTANCE.track(this, module);
    }

    @Override
    public void receiveModule(IMasterModule masterModule) {
        modules.add(masterModule);
    }

    @Override
    public void draw() {
        modules.forEach(IMasterModule::draw);
    }
}
