package icyllis.modern.api.element;

import java.util.function.Supplier;

public interface IButtonST<T extends IButtonST> {

    T text(Supplier<String> text);

    T pos(float x, float y);

    T size(float w, float h);
}
