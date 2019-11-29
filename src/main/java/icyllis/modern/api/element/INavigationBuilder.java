package icyllis.modern.api.element;

import java.util.function.Consumer;

public interface INavigationBuilder extends IButtonBuilder<INavigationBuilder> {

    /**
     * Which module should the button go
     * @param id module index
     * @return ST
     */
    INavigationBuilder to(int id);

    INavigationBuilder tex(Consumer<ITextureBuilder> consumer);
}
