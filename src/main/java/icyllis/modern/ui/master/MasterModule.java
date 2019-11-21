package icyllis.modern.ui.master;

import com.mojang.blaze3d.platform.GlStateManager;
import icyllis.modern.api.internal.IMasterManager;
import icyllis.modern.api.internal.IMasterModule;
import icyllis.modern.api.module.IModernModule;
import icyllis.modern.api.module.IModuleTracker;

public class MasterModule implements IMasterModule, IModuleTracker {

    private IModernModule rawModule;

    private int trigger = 0;
    private boolean triggered = false;

    private IMasterManager manager;

    MasterModule(IModernModule rawModule) {
        this.rawModule = rawModule;
    }

    @Override
    public void build(int width, int height) {
        manager = new MasterModuleManager();
        rawModule.createElements(manager);
        resize(width, height);
        rawModule = null;
        triggered = true;
    }

    @Override
    public void draw() {
        if(triggered)
            manager.draw();
    }

    @Override
    public IModuleTracker setTrigger(int id) {
        trigger = id;
        return this;
    }

    @Override
    public void resize(int width, int height) {
        manager.resize(width, height);
    }

    @Override
    public boolean trigger(int id) {
        return trigger == id;
    }

    @Override
    public boolean triggered() {
        return triggered;
    }
}
