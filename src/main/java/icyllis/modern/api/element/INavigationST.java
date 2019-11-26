package icyllis.modern.api.element;

import java.util.function.Consumer;

public interface INavigationST extends IButtonST<INavigationST> {

    /**
     * Which module should the button go
     * @param id module index
     * @return ST
     */
    INavigationST to(int id);

    INavigationST tex(Consumer<ITextureST> consumer);
}
