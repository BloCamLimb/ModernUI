package icyllis.modern.ui.element;

import icyllis.modern.api.element.*;
import net.minecraft.client.gui.IGuiEventListener;

import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public abstract class UIButton<T extends IButtonBuilder> implements IElement, IButtonBuilder<T>, IGuiEventListener {

    /** original xy, render xy, width height, alpha(0-1F) **/
    protected float bx, by, x, y, w, h, alpha = 1.0f;

    /** is mouse hovered on this **/
    protected boolean mouseHovered;

    /** is this visible, is cursor focused on this **/
    protected boolean visible = true, focused = false;

    /** text to show something **/
    protected UIVarText textLine = UIVarText.DEFAULT;

    @Override
    public void draw() {

    }

    @Override
    public T text(Consumer<IVarTextBuilder> consumer) {
        UIVarText u = new UIVarText();
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
        mouseHovered = isMouseInRange(mouseX, mouseY);
        if (b != mouseHovered) {
            onMouseHoverChanged();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (visible && mouseButton == 0 && mouseHovered) {
            onClick(mouseX, mouseY);
        }
        return mouseHovered;
    }

    protected void onClick(double mouseX, double mouseY) {

    }

    protected boolean isMouseInRange(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    protected void onMouseHoverChanged() {

    }

    protected void onFocusChanged(boolean focused) {

    }

    @Override
    public boolean changeFocus(boolean p_changeFocus_1_) {
        focused = !focused;
        return focused;
    }

    @Override
    public void resize(int width, int height) {
        x = width / 2f + bx;
        y = height / 2f + by;
        textLine.resize(width, height);
    }
}
