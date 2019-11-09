package icyllis.modern.ui.element;

import icyllis.modern.api.basic.IFontDraw;
import icyllis.modern.api.element.ITextLine;
import icyllis.modern.api.element.ITextLineTracker;
import net.minecraft.client.gui.FontRenderer;

public class UITextLine implements ITextLine, ITextLineTracker, IFontDraw {

    private float bx, by;
    private float x, y;
    private int color = -1;
    private boolean shadowed, centered;
    private String t;

    @Override
    public void draw(FontRenderer fontRenderer) {
        if(shadowed)
            fontRenderer.drawStringWithShadow(t, x, y, color);
        else
            fontRenderer.drawString(t, x, y, color);
    }

    @Override
    public ITextLineTracker setText(String text, boolean shadow) {
        t = text;
        shadowed = shadow;
        return this;
    }

    @Override
    public ITextLineTracker setPosition(float x, float y, boolean center) {
        bx = x;
        by = y;
        centered = center;
        return this;
    }

    @Override
    public ITextLineTracker setColor(int color) {
        this.color = color;
        return this;
    }

    @Override
    public void resize(FontRenderer fontRenderer, int width, int height) {
        y = height / 2f + by;
        if(centered) {
            int wid = fontRenderer.getStringWidth(t);
            x = (width - wid) / 2f + bx;
        } else {
            x = width / 2f + bx;
        }
    }
}
