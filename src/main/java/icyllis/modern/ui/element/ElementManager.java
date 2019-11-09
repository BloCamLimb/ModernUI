package icyllis.modern.ui.element;

import icyllis.modern.api.basic.IDraw;
import icyllis.modern.api.basic.IElement;
import icyllis.modern.api.basic.IFontDraw;
import icyllis.modern.api.element.ITextLineTracker;
import icyllis.modern.api.internal.IElementManager;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

public class ElementManager implements IElementManager {

    private FontRenderer fontRenderer;

    private List<IElement> elements = new ArrayList<>();
    private List<IDraw> draws = new ArrayList<>();
    private List<IFontDraw> fontDraws = new ArrayList<>();

    public ElementManager(FontRenderer fontRenderer) {
        this.fontRenderer = fontRenderer;
    }

    @Override
    public void defaultBackground() {
        UIBackground u = new UIBackground();
        elements.add(u);
        draws.add(u);
    }

    @Override
    public ITextLineTracker newTextLine() {
        UITextLine u = new UITextLine();
        elements.add(u);
        fontDraws.add(u);
        return u;
    }

    @Override
    public void draw() {
        draws.forEach(IDraw::draw);
        fontDraws.forEach(e -> e.draw(fontRenderer));
    }

    @Override
    public void resize(int width, int height) {
        elements.forEach(e -> e.resize(fontRenderer, width, height));
    }
}
