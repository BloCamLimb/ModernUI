package icyllis.modern.api.element;

import java.util.function.Consumer;

public interface IButtonBuilder<T extends IButtonBuilder> extends IBaseBuilder<T> {

    T text(Consumer<ITextBuilder> consumer);
}
