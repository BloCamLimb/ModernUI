package icyllis.modern.api.internal;

import icyllis.modern.api.element.INavigationST;
import icyllis.modern.api.element.ITextLineST;
import icyllis.modern.api.element.ITextureST;

public interface IElementBuilder {

    void defaultBackground();

    ITextLineST textLine();

    ITextureST texture();

    INavigationST navigation();
}
