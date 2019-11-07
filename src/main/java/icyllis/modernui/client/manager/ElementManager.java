package icyllis.modernui.client.manager;

import icyllis.modernui.api.module.IElementProvider;
import icyllis.modernui.api.content.IText;
import icyllis.modernui.client.element.UIText;

public class ElementManager implements IElementProvider {

    @Override
    public IText newTextLine() {
        return new UIText();
    }
}
