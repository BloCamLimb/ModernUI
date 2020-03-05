package icyllis.modernui.gui.master;

import icyllis.modernui.api.element.IElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

/**
 * A.K.A Module
 */
public class ElementPool implements IntPredicate, Consumer<IElement> {

    private IntPredicate availability;

    private Consumer<Consumer<IElement>> builder;

    private List<IElement> elements = new ArrayList<>();

    public ElementPool(IntPredicate availability, Consumer<Consumer<IElement>> builder) {
        this.availability = availability;
        this.builder = builder;
    }

    public void draw(float currentTime) {
        elements.forEach(iElement -> iElement.draw(currentTime));
    }

    public void resize(int width, int height) {
        elements.forEach(i -> i.resize(width, height));
    }

    public void tick(int ticks) {
        elements.forEach(iElement -> iElement.tick(ticks));
    }

    public void build() {
        if (elements.isEmpty()) {
            builder.accept(this);
        }
    }

    public void clear() {
        elements.clear();
    }

    @Override
    public void accept(IElement iBase) {
        elements.add(iBase);
    }

    @Override
    public boolean test(int value) {
        return availability.test(value);
    }
}
