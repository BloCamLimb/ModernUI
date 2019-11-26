package icyllis.modern.api.element;

import java.util.function.Supplier;

public interface ITextLineST {

    ITextLineST text(Supplier<String> text);

    ITextLineST pos(float x, float y);

    /**
     * Text alignment: 0 = left, 0.25 = center, 0.5 = right
     * @param align see above
     * @return ST
     */
    ITextLineST align(float align);

    ITextLineST color(int color);
}
