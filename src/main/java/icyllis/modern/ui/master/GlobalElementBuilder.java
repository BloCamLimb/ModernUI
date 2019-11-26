package icyllis.modern.ui.master;

import icyllis.modern.api.element.INavigationST;
import icyllis.modern.api.element.ITextLineST;
import icyllis.modern.api.element.ITextureST;
import icyllis.modern.api.internal.IElementBuilder;
import icyllis.modern.ui.button.NavigationButton;
import icyllis.modern.ui.element.UIBackground;
import icyllis.modern.ui.element.UITextLine;
import icyllis.modern.ui.element.UITexture;

public class GlobalElementBuilder implements IElementBuilder {

    public static final GlobalElementBuilder INSTANCE = new GlobalElementBuilder();

    private MasterModule receiver;
    private IMasterScreen master;

    public void setReceiver(MasterModule receiver, IMasterScreen master) {
        this.receiver = receiver;
        this.master = master;
    }

    @Override
    public void defaultBackground() {
        UIBackground u = new UIBackground();
        receiver.add(u);
    }

    @Override
    public ITextLineST textLine() {
        UITextLine u = new UITextLine();
        receiver.add(u);
        return u;
    }

    @Override
    public ITextureST texture() {
        UITexture u = new UITexture();
        receiver.add(u);
        return u;
    }

    @Override
    public INavigationST navigation() {
        NavigationButton b = new NavigationButton();
        receiver.add(b);
        master.addChild(b);
        return b;
    }
}
