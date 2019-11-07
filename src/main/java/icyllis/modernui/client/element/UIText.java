package icyllis.modernui.client.element;

import icyllis.modernui.api.content.IDrawer;
import icyllis.modernui.api.content.IText;

public class UIText implements IText, IDrawer {

    private String text;

    @Override
    public void setValue(String value) {
        this.text = value;
    }

    @Override
    public String getValue() {
        return text;
    }

    @Override
    public void draw() {

    }
}
