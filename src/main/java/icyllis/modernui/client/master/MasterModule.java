package icyllis.modernui.client.master;

import icyllis.modernui.api.internal.IElementManager;
import icyllis.modernui.api.internal.IMasterElement;
import icyllis.modernui.api.internal.IMasterModule;
import icyllis.modernui.api.module.IElementProvider;
import icyllis.modernui.api.module.IModernModule;
import icyllis.modernui.client.manager.ElementManager;

import java.util.ArrayList;
import java.util.List;

public final class MasterModule implements IMasterModule {

    private IModernModule rawModule;

    private boolean isMain = false;

    private IElementManager elementManager = new ElementManager();

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

    @Override
    public void bake() {
        rawModule.createElements(elementManager);
    }
}
