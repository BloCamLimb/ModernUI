package icyllis.modernui.client.internal;

import icyllis.modernui.api.widget.IWidgetProvider;
import icyllis.modernui.api.widget.IWidgetText;
import icyllis.modernui.client.element.UIText;

public class WidgetProvider implements IWidgetProvider {

    @Override
    public IWidgetText createText() {
        return new UIText();
    }
}
