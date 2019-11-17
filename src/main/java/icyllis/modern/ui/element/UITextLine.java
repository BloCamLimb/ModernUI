package icyllis.modern.ui.element;

import icyllis.modern.api.basic.IDraw;
import icyllis.modern.api.basic.IResize;
import icyllis.modern.api.element.ITextLineTracker;
import icyllis.modern.core.ModernUI;
import icyllis.modern.ui.font.StringRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.util.function.Supplier;

public class UITextLine implements ITextLineTracker, IResize, IDraw {

    private FontRenderer fontRenderer;
    private StringRenderer renderer = StringRenderer.DEFAULT_FONT_RENDERER;

    private float bx, by;
    private float x, y;
    private int color = -1;
    private boolean shadowed, centered;
    private String t;
    private Supplier<Long> tt;

    public UITextLine(FontRenderer fontRenderer) {
        this.fontRenderer = fontRenderer;
        tt = () -> Minecraft.getInstance().world.getWorld().getGameTime();
    }

    @Override
    public void draw() {
        if(shadowed)
            fontRenderer.drawStringWithShadow(t, x, y, color);
        else {
            renderer.renderString(t + tt.get(), x, y, 0xfff91020);
        }
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
    public void resize(int width, int height) {
        y = height / 2f + by;
        if(centered) {
            float wid = renderer.getStringWidth(t);
            x = (width - wid) / 2f + bx;
        } else {
            x = width / 2f + bx;
        }
    }
}
