package icyllis.modern.ui.element;

import icyllis.modern.api.element.IElement;
import icyllis.modern.api.element.ITextLineST;
import icyllis.modern.ui.font.TrueTypeRenderer;

import java.util.function.Supplier;

public class UITextLine implements ITextLineST, IElement {

    private TrueTypeRenderer renderer = TrueTypeRenderer.DEFAULT_FONT_RENDERER;

    private float bx, by;
    private float x, y;
    private int color = 0xffffff;
    private int alpha = 0xff;
    private float align;
    private Supplier<String> text;

    @Override
    public void draw() {
        renderer.drawString(text.get(), x, y, color, alpha, align);
    }

    @Override
    public ITextLineST text(Supplier<String> text) {
        this.text = text;
        return this;
    }

    @Override
    public ITextLineST pos(float x, float y) {
        bx = x;
        by = y;
        return this;
    }

    @Override
    public ITextLineST align(float align) {
        this.align = align;
        return this;
    }

    @Override
    public ITextLineST color(int color) {
        this.color = color;
        return this;
    }

    @Override
    public void resize(int width, int height) {
        x = width / 2f + bx;
        y = height / 2f + by;
    }
}
