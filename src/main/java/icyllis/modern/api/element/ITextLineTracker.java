package icyllis.modern.api.element;

import java.util.function.Supplier;

public interface ITextLineTracker {

    ITextLineTracker text(Supplier<String> text);

    ITextLineTracker pos(float x, float y);

    ITextLineTracker pos(float x, float y, boolean center);

    ITextLineTracker color(int color);
}
