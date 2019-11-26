package icyllis.modern.api.element;

import java.util.function.Supplier;

public interface ITextLineST {

    ITextLineST text(Supplier<String> text);

    ITextLineST pos(float x, float y);

    ITextLineST pos(float x, float y, boolean center);

    ITextLineST color(int color);
}
