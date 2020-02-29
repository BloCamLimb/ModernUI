package icyllis.modernui.gui.master;

import icyllis.modernui.api.global.IElementBuilder;
import icyllis.modernui.gui.element.IBase;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Used to build gui elements step by step
 */
public class MasterModule {

    private Consumer<IElementBuilder> builder;

    public final int id;

    MasterModule(Consumer<IElementBuilder> builder, int id) {
        this.builder = builder;
        this.id = id;
    }

    public void build() {
        builder.accept(GlobalElementBuilder.INSTANCE);
        builder = b -> {};
    }

}
