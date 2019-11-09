package icyllis.modern.api.module;

import icyllis.modern.api.element.ITextLineTracker;

public interface IElementBuilder {

    void defaultBackground();

    ITextLineTracker newTextLine();
}
