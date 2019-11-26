package icyllis.modern.ui.element;

import icyllis.modern.api.element.IElement;
import net.minecraft.client.gui.IGuiEventListener;

public abstract class UIButton implements IElement, IGuiEventListener {

    private float bx, by, x, y, w, h, a;
    private boolean mouseHovered;
    private String t;

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        mouseHovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }
}
