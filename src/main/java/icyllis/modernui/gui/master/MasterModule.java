package icyllis.modernui.gui.master;

import icyllis.modernui.api.global.IElementBuilder;
import icyllis.modernui.gui.element.Base;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Baked gui module
 */
public class MasterModule {

    private Consumer<IElementBuilder> rawModule;

    private List<Base> elements = new ArrayList<>();

    MasterModule(Consumer<IElementBuilder> rawModule) {
        this.rawModule = rawModule;
    }

    public void build(IMasterScreen master, int width, int height) {
        GlobalElementBuilder.INSTANCE.setReceiver(this, master);
        rawModule.accept(GlobalElementBuilder.INSTANCE);
        resize(width, height);
        rawModule = null;
    }

    public void draw() {
        elements.forEach(Base::draw);
    }

    public void resize(int width, int height) {
        elements.forEach(e -> e.resize(width, height));
    }

    public void addElement(Base e) {
        elements.add(e);
    }

}
