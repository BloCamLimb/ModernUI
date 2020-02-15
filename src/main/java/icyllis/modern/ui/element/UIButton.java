package icyllis.modern.ui.element;

import icyllis.modern.api.element.*;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.math.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Button is an advanced element that can be interacted with mouse and keyboard and trigger something
 * Generally used in widget with other element
 */
public class UIButton extends UIElement<IButtonBuilder> implements IButtonBuilder, IButtonModifier, IGuiEventListener {

    /** whether mouse hovered on this **/
    protected boolean mouseHovered;

    /** whether cursor focused on this **/
    protected boolean visible = true, focused = false;

    protected List<Event<IButtonModifier>> events = new ArrayList<>();

    @Override
    public void draw() {
        // NOTHING TO RENDER
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        boolean b = mouseHovered;
        this.mouseHovered = isMouseInRange(mouseX, mouseY);
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
        return mouseX >= renderX && mouseX <= renderX + sizeW && mouseY >= renderY && mouseY <= renderY + sizeH;
    }

    protected void onMouseHoverChanged() {
        if (mouseHovered) {
            events.stream().filter(e -> e.getId() == Event.MOUSE_HOVER_ON).forEach(a -> a.run(this));
        } else {
            events.stream().filter(e -> e.getId() == Event.MOUSE_HOVER_OFF).forEach(a -> a.run(this));
        }
    }

    protected void onFocusChanged() {

    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    public boolean changeFocus(boolean p_changeFocus_1_) {
        focused = !focused;
        return focused;
    }

    @Override
    public IButtonBuilder onMouseHoverOn(Consumer<IButtonModifier> consumer) {
        events.add(new Event<>(Event.MOUSE_HOVER_ON, consumer));
        return this;
    }

    @Override
    public IButtonBuilder onMouseHoverOff(Consumer<IButtonModifier> consumer) {
        events.add(new Event<>(Event.MOUSE_HOVER_OFF, consumer));
        return this;
    }

    public static final class Event<T extends IBaseModifier> {

        public static int MOUSE_HOVER_ON = 1;
        public static int MOUSE_HOVER_OFF = 2;

        private final int id;

        private final Consumer<T> consumer;

        public Event(int id, Consumer<T> consumer) {
            this.id = id;
            this.consumer = consumer;
        }

        public int getId() {
            return id;
        }

        public void run(T t) {
            consumer.accept(t);
        }
    }
}
