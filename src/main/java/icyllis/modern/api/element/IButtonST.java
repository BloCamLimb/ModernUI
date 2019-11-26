package icyllis.modern.api.element;

import java.util.function.Consumer;

public interface IButtonST<T extends IButtonST> {

    T text(Consumer<ITextLineST> consumer);

    T pos(float x, float y);

    T size(float w, float h);
}
