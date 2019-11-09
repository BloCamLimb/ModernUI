package icyllis.modern.api.internal;

import icyllis.modern.api.module.IElementBuilder;

public interface IMasterManager extends IElementBuilder {

    void draw();

    void resize(int width, int height);
}
