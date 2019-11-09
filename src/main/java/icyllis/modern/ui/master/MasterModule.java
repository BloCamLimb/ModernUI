package icyllis.modern.ui.master;

import icyllis.modern.api.internal.IElementManager;
import icyllis.modern.api.internal.IMasterModule;
import icyllis.modern.api.module.IModernModule;
import icyllis.modern.api.tracker.IModuleTracker;
import icyllis.modern.ui.element.ElementManager;
import net.minecraft.client.gui.FontRenderer;

public final class MasterModule implements IMasterModule, IModuleTracker {

    private IModernModule rawModule;

    private int trigger = 0;
    private boolean triggered = false;

    private IElementManager elementManager;

    public MasterModule(IModernModule rawModule) {
        this.rawModule = rawModule;
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
    public void bake(FontRenderer fontRenderer, int width, int height) {
        elementManager = new ElementManager(fontRenderer, width, height);
        rawModule.createElements(elementManager);
        rawModule.fixPositions(elementManager);
        triggered = true;
    }

    @Override
    public void resize(int width, int height) {
        elementManager.resize(width, height);
        rawModule.fixPositions(elementManager);
    }

    @Override
    public boolean trigger(int id) {
        return trigger == id;
    }

    @Override
    public boolean triggered() {
        return triggered;
    }

    @Override
    public void end() {

    }
}
