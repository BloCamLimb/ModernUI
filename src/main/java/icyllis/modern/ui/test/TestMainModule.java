package icyllis.modern.ui.test;

import icyllis.modern.api.module.IModernModule;
import icyllis.modern.api.internal.IElementBuilder;

public class TestMainModule implements IModernModule {

    @Override
    public void createElements(IElementBuilder builder) {
        builder.defaultBackground();
        builder.newTextLine()
                .setText("Snownee likes to eat lemons")
                .setPosition(0, -60, true);
        builder.newTextLine()
                .setText("Failed to check the order, please try again.")
                .setPosition(0, -20, true);
        builder.newTextLine()
                .setText("\u68c0\u5e8f\u5931\u8d25, \u8bf7\u91cd\u8bd5")
                //.setText("\u6cdb\u94f6\u6cb3\u683c\u96f7\u79d1\u6280\u6709\u9650\u516c\u53f8 \u83ab\u6495\u6253\u7535\u52a8 \u5929\u7136\u7761\u89c9")
                .setPosition(0, -40, true);
    }

}
