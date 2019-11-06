package icyllis.modernui.client.module;

import icyllis.modernui.api.module.IModernModule;
import icyllis.modernui.api.widget.IWidgetProvider;

public class TestMainModule implements IModernModule {

    @Override
    public void createGUIElements(IWidgetProvider provider) {
        provider.createText().setValue("2");
    }
}
