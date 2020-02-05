package icyllis.modern.ui.master;

import icyllis.modern.api.global.IElementBuilder;
import icyllis.modern.ui.element.UIElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Baked gui module
 */
public class MasterModule {

    private Consumer<IElementBuilder> rawModule;

    private List<UIElement> elements = new ArrayList<>();

    MasterModule(Consumer<IElementBuilder> rawModule) {
        this.rawModule = rawModule;
    }

    public void build(IMasterScreen master, int width, int height) {
        GlobalElementBuilder.INSTANCE.setReceiver(this, master);
        rawModule.accept(GlobalElementBuilder.INSTANCE);
        resize(width, height);
        GlobalAnimationManager.INSTANCE.buildAnimations();
        rawModule = null;
    }

    public void draw() {
        elements.forEach(UIElement::draw);
    }

    public void resize(int width, int height) {
        elements.forEach(e -> e.resize(width, height));
    }

    public void addElement(UIElement e) {
        elements.add(e);
    }

}
