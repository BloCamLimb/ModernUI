package icyllis.modern.ui.test;

import icyllis.modern.api.module.IModernModule;
import icyllis.modern.api.module.IElementBuilder;

public class TestMainModule implements IModernModule {

    @Override
    public void createElements(IElementBuilder builder) {
        builder.defaultBackground();
        builder.newTextLine()
                .setText("Hello everyone, this is direwolf20")
                .setPosition(0, -60, true);
        builder.newTextLine()
                .setText("\u83ab\u6495\u0070\u0073\u672c\u5f53\u4e0a\u624b")
                .setPosition(0, -40, true);
    }

}
