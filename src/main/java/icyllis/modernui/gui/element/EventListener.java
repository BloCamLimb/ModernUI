package icyllis.modernui.gui.element;

import icyllis.modernui.api.builder.IEventListenerInitializer;
import icyllis.modernui.system.ModernUI;
import net.minecraft.client.gui.IGuiEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An advanced element that can be interacted with mouse and keyboard and trigger something
 * Generally used in widget with other element
 */
public class EventListener<T> implements IEventListenerInitializer, IGuiEventListener {

    private final T host;

    protected Function<Integer, Float> fakeX, fakeY;

    /** whether mouse hovered on this **/
    protected boolean mouseHovered = false;

    /** whether cursor focused on this **/
    protected boolean available = true, focused = false;

    /** 0 = rect, 1 = circle, 2 = sector **/
    protected int shape;

    protected float x, y, width, height, radius;

    protected double clockwise, flare;

    protected List<iEvent<T>> events = new ArrayList<>();

    public EventListener(T widget) {
        this.host = widget;
    }

    @Override
    public IEventListenerInitializer setPos(Function<Integer, Float> x, Function<Integer, Float> y) {
        fakeX = x;
        fakeY = y;
        return this;
    }

    @Override
    public IEventListenerInitializer setRectShape(float width, float height) {
        shape = 0;
        this.width = width;
        this.height = height;
        return this;
    }

    @Override
    public IEventListenerInitializer setCircleShape(float radius) {
        shape = 1;
        this.radius = radius;
        return this;
    }

    @Override
    public IEventListenerInitializer setSectorShape(float radius, float clockwise, float flare) {
        shape = 2;
        this.radius = radius;
        this.clockwise = Math.tan(clockwise);
        this.flare = Math.tan(flare);
        return this;
    }

    public void resize(int width, int height) {
        float x = fakeX.apply(width);
        float y = fakeY.apply(height);
        this.x = x;
        this.y = y;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        boolean previous = mouseHovered;
        this.mouseHovered = isMouseInShape(mouseX, mouseY);
        if (previous != mouseHovered) {
            onHoveredChanged();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (available && mouseButton == 0 && mouseHovered) {
            onLeftClick();
            return true;
        }
        return false;
    }

    protected boolean isMouseInShape(double mouseX, double mouseY) {
        switch (shape) {
            case 0:
                return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
            case 1:
                return Math.sqrt((mouseX - x) * (mouseX - x) + (mouseY - y) * (mouseY - y)) <= radius;
            case 2:
                boolean inRadius = Math.sqrt((mouseX - x) * (mouseX - x) + (mouseY - y) * (mouseY - y)) <= radius;
                if (inRadius) {
                    double angle = (mouseX - x) / (y - mouseY);
                    return angle >= clockwise && angle <= clockwise + flare;
                } else {
                    return false;
                }
        }
        return false;
    }

    @Override
    public boolean changeFocus(boolean p_changeFocus_1_) {
        focused = !focused;
        return focused;
    }

    protected void onHoveredChanged() {
        if (mouseHovered) {
            events.stream().filter(e -> e.id() == iEvent.MOUSE_HOVER_ON).forEach(a -> a.run(host));
        } else {
            events.stream().filter(e -> e.id() == iEvent.MOUSE_HOVER_OFF).forEach(a -> a.run(host));
        }
    }

    protected void onLeftClick() {
        events.stream().filter(e -> e.id == iEvent.LEFT_CLICK).forEach(a -> a.run(host));
    }

    public void addHoverOn(Consumer<T> consumer) {
        events.add(new iEvent<>(iEvent.MOUSE_HOVER_ON, consumer));
    }

    public void addHoverOff(Consumer<T> consumer) {
        events.add(new iEvent<>(iEvent.MOUSE_HOVER_OFF, consumer));
    }

    public void addLeftClick(Consumer<T> consumer) {
        events.add(new iEvent<>(iEvent.LEFT_CLICK, consumer));
    }

    private static final class iEvent<T> {

        public static int MOUSE_HOVER_ON = 1;
        public static int MOUSE_HOVER_OFF = 2;
        public static int LEFT_CLICK = 3;
        public static int RIGHT_CLICK = 4;

        private final int id;

        private final Consumer<T> consumer;

        public iEvent(int id, Consumer<T> consumer) {
            this.id = id;
            this.consumer = consumer;
        }

        public int id() {
            return id;
        }

        public void run(T t) {
            consumer.accept(t);
        }
    }
}
