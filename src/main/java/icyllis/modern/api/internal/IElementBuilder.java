package icyllis.modern.api.internal;

import icyllis.modern.api.element.ITextLineTracker;

public interface IElementBuilder {

    void defaultBackground();

    ITextLineTracker newTextLine();
}
