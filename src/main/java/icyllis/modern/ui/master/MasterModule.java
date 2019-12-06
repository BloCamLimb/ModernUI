package icyllis.modern.ui.master;

import icyllis.modern.ui.element.IElement;
import icyllis.modern.api.module.IGuiModule;

import java.util.ArrayList;
import java.util.List;

public class MasterModule {

    private IGuiModule raw;

    private List<IElement> elements = new ArrayList<>();

    MasterModule(IGuiModule raw) {
        this.raw = raw;
    }

    public void build(IMasterScreen master, int width, int height) {
        GlobalElementBuilder.INSTANCE.setReceiver(this, master);
        raw.createElements(GlobalElementBuilder.INSTANCE);
        resize(width, height);
        raw = null;
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
