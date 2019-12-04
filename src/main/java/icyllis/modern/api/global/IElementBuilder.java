package icyllis.modern.api.global;

import icyllis.modern.api.element.INavigationBuilder;
import icyllis.modern.api.element.ITextLineBuilder;
import icyllis.modern.api.element.ITextureBuilder;

public interface IElementBuilder {

    void defaultBackground();

    ITextLineBuilder textLine();

    ITextureBuilder texture();

    INavigationBuilder navigation();

    void input();
}
