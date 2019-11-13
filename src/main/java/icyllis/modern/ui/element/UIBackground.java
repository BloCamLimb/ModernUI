package icyllis.modern.ui.element;

import icyllis.modern.api.basic.IDraw;
import icyllis.modern.api.basic.IResize;
import icyllis.modern.ui.master.DrawTools;

public class UIBackground implements IResize, IDraw {

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
