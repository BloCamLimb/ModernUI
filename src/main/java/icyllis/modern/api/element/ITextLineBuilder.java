package icyllis.modern.api.element;

import java.util.function.Supplier;

public interface ITextLineBuilder {

    ITextLineBuilder text(Supplier<String> text);

    ITextLineBuilder pos(float x, float y);

    /**
     * Text alignment: 0 = left, 0.25 = center, 0.5 = right
     * @param align see above
     */
    ITextLineBuilder align(float align);

    ITextLineBuilder color(int color);
}
