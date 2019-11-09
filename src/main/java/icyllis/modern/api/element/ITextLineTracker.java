package icyllis.modern.api.element;

public interface ITextLineTracker {

    default ITextLineTracker setText(String text) {
        return setText(text, false);
    }

    ITextLineTracker setText(String text, boolean shadow);

    default ITextLineTracker setPosition(int x, int y) {
        return setPosition(x, y, false);
    }

    default ITextLineTracker setPosition(int x, int y, boolean center) {
        return setPosition((float) x, (float) y, center);
    }

    default ITextLineTracker setPosition(float x, float y) {
        return setPosition(x, y, false);
    }

    ITextLineTracker setPosition(float x, float y, boolean center);

    ITextLineTracker setColor(int color);
}
