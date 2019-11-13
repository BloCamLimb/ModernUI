package icyllis.modern.ui.master;

import icyllis.modern.api.basic.IDraw;
import icyllis.modern.api.basic.IResize;
import icyllis.modern.api.element.ITextLineTracker;
import icyllis.modern.api.internal.IMasterManager;
import icyllis.modern.ui.element.UIBackground;
import icyllis.modern.ui.element.UITextLine;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

public class MasterModuleManager implements IMasterManager {

    private FontRenderer fontRenderer;

    private List<IResize> resizes = new ArrayList<>();
    private List<IDraw> draws = new ArrayList<>();

    public MasterModuleManager(FontRenderer fontRenderer) {
        this.fontRenderer = fontRenderer;
    }

    @Override
    public void defaultBackground() {
        UIBackground u = new UIBackground();
        resizes.add(u);
        draws.add(u);
    }

    @Override
    public ITextLineTracker newTextLine() {
        UITextLine u = new UITextLine(fontRenderer);
        resizes.add(u);
        draws.add(u);

        return u;
    }

    @Override
    public void draw() {
        draws.forEach(IDraw::draw);
    }

    @Override
    public void resize(int width, int height) {
        resizes.forEach(r -> r.resize(width, height));
    }
}
