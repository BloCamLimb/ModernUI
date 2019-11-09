package icyllis.modern.ui.test;

import icyllis.modern.api.module.IPositionFixer;
import icyllis.modern.api.module.IModernModule;
import icyllis.modern.api.module.IElementBuilder;

public class TestMainModule implements IModernModule {

    @Override
    public void createElements(IElementBuilder builder) {
        builder.newTextLine()
                .setValue("Hi, here")
                .end();
    }

    @Override
    public void fixPositions(IPositionFixer fixer) {
        fixer.setNext(-20, -60);
    }
}
