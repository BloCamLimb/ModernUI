package icyllis.modern.api.internal;

import icyllis.modern.api.element.ITextLineTracker;
import icyllis.modern.api.element.ITextureTracker;

public interface IElementBuilder {

    void defaultBackground();

    ITextLineTracker textLine();

    ITextureTracker texture();
}
