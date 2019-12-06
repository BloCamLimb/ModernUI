package icyllis.modern.ui.master;

import icyllis.modern.api.element.IConstTextBuilder;
import icyllis.modern.api.element.INavigationBuilder;
import icyllis.modern.api.element.IVarTextBuilder;
import icyllis.modern.api.element.ITextureBuilder;
import icyllis.modern.api.global.IElementBuilder;
import icyllis.modern.ui.button.InputBox;
import icyllis.modern.ui.button.NavigationButton;
import icyllis.modern.ui.element.UIBackground;
import icyllis.modern.ui.element.UIConstText;
import icyllis.modern.ui.element.UIVarText;
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
    public IVarTextBuilder varText() {
        UIVarText u = new UIVarText();
        receiver.add(u);
        return u;
    }

    @Override
    public IConstTextBuilder constText() {
        UIConstText u = new UIConstText();
        receiver.add(u);
        return u;
    }

    @Override
    public ITextureBuilder texture() {
        UITexture u = new UITexture();
        receiver.add(u);
        return u;
    }

    @Override
    public INavigationBuilder navigation() {
        NavigationButton b = new NavigationButton();
        receiver.add(b);
        master.addChild(b);
        return b;
    }

    @Override
    public void input() {
        InputBox u = new InputBox();
        receiver.add(u);
        master.addChild(u);
    }
}
