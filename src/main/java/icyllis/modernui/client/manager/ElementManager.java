package icyllis.modernui.client.manager;

import icyllis.modernui.api.content.IText;
import icyllis.modernui.api.internal.IElementManager;
import icyllis.modernui.client.element.UIText;

public class ElementManager implements IElementManager {

    @Override
    public IText newTextLine() {
        return new UIText();
    }
}
