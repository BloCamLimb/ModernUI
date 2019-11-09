package icyllis.modern.ui.element;

import icyllis.modern.api.basic.IDraw;
import icyllis.modern.api.basic.IElement;
import icyllis.modern.ui.master.DrawTools;
import net.minecraft.client.gui.FontRenderer;

public class UIBackground implements IElement, IDraw {

    private int width, height;

    @Override
    public void draw() {
        DrawTools.fillGradient(0, 0, width, height, -1072689136, -804253680, 0);
    }

    @Override
    public void resize(FontRenderer fontRenderer, int width, int height) {
        this.width = width;
        this.height = height;
    }
}
