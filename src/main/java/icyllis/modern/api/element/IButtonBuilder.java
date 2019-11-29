package icyllis.modern.api.element;

import java.util.function.Consumer;

public interface IButtonBuilder<T extends IButtonBuilder> {

    T text(Consumer<ITextLineBuilder> consumer);

    T pos(float x, float y);

    T size(float w, float h);
}
