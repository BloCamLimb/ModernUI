package icyllis.modern.api.element;

import java.util.function.Consumer;

public interface IButtonBuilder extends IBaseBuilder<IButtonBuilder> {

    IButtonBuilder onMouseHoverOn(Consumer<IButtonModifier> consumer);

    IButtonBuilder onMouseHoverOff(Consumer<IButtonModifier> consumer);
}
