package icyllis.modern.ui.test;

import icyllis.modern.api.module.IModernModule;
import icyllis.modern.api.module.IElementBuilder;

public class TestMainModule implements IModernModule {

    @Override
    public void createElements(IElementBuilder builder) {
        builder.defaultBackground();
        builder.newTextLine()
                .setText("Snownee likes to eat lemons")
                .setPosition(0, -60, true);
        builder.newTextLine()
                .setText("\u6cdb\u94f6\u6cb3\u683c\u96f7\u79d1\u6280\u6709\u9650\u516c\u53f8")
                .setPosition(0, -40, true);
    }

}
