package icyllis.modern.ui.master;

import icyllis.modern.api.element.IElement;
import icyllis.modern.api.element.ITextLineST;
import icyllis.modern.api.element.ITextureST;
import icyllis.modern.api.internal.IMasterManager;
import icyllis.modern.ui.element.UIBackground;
import icyllis.modern.ui.element.UITextLine;
import icyllis.modern.ui.element.UITexture;

import java.util.ArrayList;
import java.util.List;

public class MasterModuleManager implements IMasterManager {

    private List<IElement> elements = new ArrayList<>();

    public MasterModuleManager() {

    }

    @Override
    public void defaultBackground() {
        UIBackground u = new UIBackground();
        elements.add(u);
    }

    @Override
    public ITextLineST textLine() {
        UITextLine u = new UITextLine();
        elements.add(u);
        return u;
    }

    @Override
    public ITextureST texture() {
        UITexture u = new UITexture();
        elements.add(u);
        return u;
    }

    @Override
    public void draw() {
        elements.forEach(IElement::draw);
    }

    @Override
    public void resize(int width, int height) {
        elements.forEach(r -> r.resize(width, height));
    }
}
