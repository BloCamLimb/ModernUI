package icyllis.modern.ui.element;

import icyllis.modern.ui.master.DrawTools;

public final class UIBackground implements IElement {

    private int width, height;

    @Override
    public void draw() {
        DrawTools.fillGradient(0, 0, width, height, 0xc0101010, 0xd0101010, 0);
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
