package icyllis.modern.ui.element;

import icyllis.modern.api.element.IButtonST;
import icyllis.modern.api.element.IElement;
import icyllis.modern.api.element.ITextLineST;
import icyllis.modern.system.ModernUI;
import net.minecraft.client.gui.IGuiEventListener;

import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public abstract class UIButton<T extends IButtonST> implements IElement, IButtonST<T>, IGuiEventListener {

    private float bx, by, x, y, w, h, alpha;
    protected boolean mouseHovered;
    protected UITextLine textLine;

    @Override
    public void draw() {

    }

    @Override
    public T text(Consumer<ITextLineST> consumer) {
        UITextLine u = new UITextLine();
        consumer.accept(u);
        textLine = u;
        return (T) this;
    }

    @Override
    public T pos(float x, float y) {
        bx = x;
        by = y;
        return (T) this;
    }

    @Override
    public T size(float w, float h) {
        this.w = w;
        this.h = h;
        return (T) this;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        boolean b = mouseHovered;
        mouseHovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        if (b != mouseHovered) {
            onMouseHoverChanged();
        }
    }

    protected void onMouseHoverChanged() {

    }

    @Override
    public void resize(int width, int height) {
        x = width / 2f + bx;
        y = height / 2f + by;
        textLine.resize(width, height);
    }
}
