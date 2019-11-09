package icyllis.modern.ui.element;

import icyllis.modern.api.basic.IFontDraw;
import icyllis.modern.api.element.ITextLine;
import icyllis.modern.api.element.ITextLineTracker;
import icyllis.modern.core.ModernUI;
import net.minecraft.client.gui.FontRenderer;

public class UIText implements ITextLine, IFontDraw {

    private int x, y;
    private String text;

    @Override
    public ITextLineTracker setValue(String value) {
        this.text = value;
        return this;
    }

    @Override
    public String getValue() {
        return text;
    }

    @Override
    public void draw(FontRenderer fontRenderer) {
        fontRenderer.drawString(text, x, y, 0xffffffff);
    }

    @Override
    public void end() {

    }

    @Override
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        ModernUI.logger.info(ModernUI.marker, "Position Set [x={},y={}]", x, y);
    }
}
