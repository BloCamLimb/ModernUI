package icyllis.modern.api.internal;

import icyllis.modern.api.module.IElementBuilder;
import icyllis.modern.api.module.IPositionFixer;

public interface IElementManager extends IElementBuilder, IPositionFixer {

    void draw();

    void resize(int width, int height);
}
