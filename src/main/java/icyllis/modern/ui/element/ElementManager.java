package icyllis.modern.ui.element;

import icyllis.modern.api.basic.IElement;
import icyllis.modern.api.basic.IFontDraw;
import icyllis.modern.api.element.ITextLineTracker;
import icyllis.modern.api.internal.IElementManager;
import icyllis.modern.api.module.IPositionFixer;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

public class ElementManager implements IElementManager {

    private FontRenderer fontRenderer;
    private int width, height;
    private int positionFixerIndex = -1;

    private List<IElement> elements = new ArrayList<>();
    private List<IFontDraw> fontDraws = new ArrayList<>();

    public ElementManager(FontRenderer fontRenderer, int w, int h) {
        this.fontRenderer = fontRenderer;
        width = w;
        height = h;
    }

    @Override
    public ITextLineTracker newTextLine() {
        UIText u = new UIText();
        elements.add(u);
        fontDraws.add(u);
        return u;
    }

    @Override
    public void draw() {
        fontDraws.iterator().forEachRemaining(e -> e.draw(fontRenderer));
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public IPositionFixer setNext(int count, int x, int y) {
        int p = positionFixerIndex += count;
        if(p < elements.size()) {
            elements.get(p).setPosition(width / 2 + x, height / 2 + y);
        }
        return this;
    }
}
