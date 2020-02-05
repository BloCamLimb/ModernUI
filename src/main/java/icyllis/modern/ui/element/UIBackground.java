package icyllis.modern.ui.element;

import icyllis.modern.ui.master.DrawTools;

/**
 * Commonly used
 */
public final class UIBackground extends UIElement {

    private int width, height;

    @Override
    public void draw() {
        DrawTools.fillRectWithColor(0, 0, width, height, 0x75000000);
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
