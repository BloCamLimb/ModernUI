package icyllis.modern.ui.element;

import icyllis.modern.api.element.*;
import net.minecraft.client.gui.IGuiEventListener;

import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public abstract class UIButton<T extends IButtonBuilder> extends UIElement<T> implements IButtonBuilder<T>, IGuiEventListener {

    /** is mouse hovered on this **/
    protected boolean mouseHovered;

    /** is this visible, is cursor focused on this **/
    protected boolean visible = true, focused = false;

    /** text to show something **/
    protected UIText textLine = new UIText();

    @Override
    public void draw() {

    }

    @Override
    public T text(Consumer<ITextBuilder> consumer) {
        UIText u = new UIText();
        consumer.accept(u);
        textLine = u;
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
        return mouseX >= renderX.get() && mouseX <= renderY.get() + sizeW.get() && mouseY >= renderY.get() && mouseY <= renderY.get() + sizeH.get();
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
}
