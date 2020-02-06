package icyllis.modern.api.element;

import java.util.function.Consumer;

public interface INavigationBuilder extends IButtonBuilder<INavigationBuilder> {

    /**
     * Which module should the button is/go
     * @param id module id
     * @return builder
     */
    INavigationBuilder setTarget(int id);

    INavigationBuilder setTexture(Consumer<ITextureBuilder> consumer);

    INavigationBuilder onMouseHoverOn(Consumer<INavigationGetter> consumer);

    INavigationBuilder onMouseHoverOff(Consumer<INavigationGetter> consumer);
}
