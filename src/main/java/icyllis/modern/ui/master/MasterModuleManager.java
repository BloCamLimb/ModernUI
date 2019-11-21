package icyllis.modern.ui.master;

import icyllis.modern.api.element.IElement;
import icyllis.modern.api.element.ITextLineTracker;
import icyllis.modern.api.element.ITextureTracker;
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
    public ITextLineTracker textLine() {
        UITextLine u = new UITextLine();
        elements.add(u);
        return u;
    }

    @Override
    public ITextureTracker texture() {
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
