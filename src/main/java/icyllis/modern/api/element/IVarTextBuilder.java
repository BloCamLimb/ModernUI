package icyllis.modern.api.element;

import java.util.function.Supplier;

public interface IVarTextBuilder {

    IVarTextBuilder text(Supplier<String> text);

    IVarTextBuilder pos(float x, float y);

    /**
     * Text alignment: 0 = left, 0.25 = center, 0.5 = right
     * @param align see above
     */
    IVarTextBuilder align(float align);

    IVarTextBuilder color(int color);
}
