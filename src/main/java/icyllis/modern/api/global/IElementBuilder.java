package icyllis.modern.api.global;

import icyllis.modern.api.element.IConstTextBuilder;
import icyllis.modern.api.element.INavigationBuilder;
import icyllis.modern.api.element.IVarTextBuilder;
import icyllis.modern.api.element.ITextureBuilder;

public interface IElementBuilder {

    void defaultBackground();

    IVarTextBuilder varText();

    IConstTextBuilder constText();

    ITextureBuilder texture();

    INavigationBuilder navigation();

    void input();
}
