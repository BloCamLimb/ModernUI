package icyllis.modern.ui.master;

import icyllis.modern.api.internal.IMasterManager;
import icyllis.modern.api.internal.IMasterModule;
import icyllis.modern.api.module.IModernModule;
import icyllis.modern.api.module.IModuleTracker;
import net.minecraft.client.gui.FontRenderer;

public class MasterModule implements IMasterModule, IModuleTracker {

    private IModernModule rawModule;

    private int trigger = 0;
    private boolean triggered = false;

    private IMasterManager elementManager;

    MasterModule(IModernModule rawModule) {
        this.rawModule = rawModule;
    }

    @Override
    public void build(FontRenderer fontRenderer, int width, int height) {
        elementManager = new MasterModuleManager(fontRenderer);
        rawModule.createElements(elementManager);
        resize(width, height);
        rawModule = null;
        triggered = true;
    }

    @Override
    public void draw() {
        if(triggered)
            elementManager.draw();
    }

    @Override
    public IModuleTracker setTrigger(int id) {
        trigger = id;
        return this;
    }

    @Override
    public void resize(int width, int height) {
        elementManager.resize(width, height);
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
