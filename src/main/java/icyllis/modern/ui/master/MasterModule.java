package icyllis.modern.ui.master;

import icyllis.modern.api.global.IElementBuilder;
import icyllis.modern.ui.element.IElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Baked gui module
 */
public class MasterModule {

    private Consumer<IElementBuilder> rawModule;

    private List<IElement> elements = new ArrayList<>();

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
        elements.forEach(IElement::draw);
    }

    public void resize(int width, int height) {
        elements.forEach(e -> e.resize(width, height));
    }

    public void add(IElement e) {
        elements.add(e);
    }

}
