package icyllis.modern.ui.master;

import icyllis.modern.api.element.IColorBuilder;
import icyllis.modern.api.element.INavigationBuilder;
import icyllis.modern.api.element.ITextBuilder;
import icyllis.modern.api.element.ITextureBuilder;
import icyllis.modern.api.global.IElementBuilder;
import icyllis.modern.ui.button.InputBox;
import icyllis.modern.ui.button.NavigationButton;
import icyllis.modern.ui.element.UIBackground;
import icyllis.modern.ui.element.UIColorRect;
import icyllis.modern.ui.element.UIText;
import icyllis.modern.ui.element.UITexture;
import net.minecraft.network.PacketBuffer;

public class GlobalElementBuilder implements IElementBuilder {

    public static final GlobalElementBuilder INSTANCE = new GlobalElementBuilder();

    private PacketBuffer extraData;

    private MasterModule receiver;
    private IMasterScreen master;

    public void setReceiver(MasterModule receiver, IMasterScreen master) {
        this.receiver = receiver;
        this.master = master;
    }

    public void setExtraData(PacketBuffer extraData) {
        this.extraData = extraData;
    }

    @Override
    public PacketBuffer getExtraData() {
        return extraData;
    }

    @Override
    public void defaultBackground() {
        UIBackground u = new UIBackground();
        receiver.addElement(u);
    }

    @Override
    public ITextBuilder text() {
        UIText u = new UIText();
        receiver.addElement(u);
        return u;
    }

    @Override
    public ITextureBuilder texture() {
        UITexture u = new UITexture();
        receiver.addElement(u);
        return u;
    }

    @Override
    public INavigationBuilder navigation() {
        NavigationButton b = new NavigationButton();
        receiver.addElement(b);
        master.addChild(b);
        return b;
    }

    @Override
    public IColorBuilder colorRect() {
        UIColorRect u = new UIColorRect();
        receiver.addElement(u);
        return u;
    }

    @Override
    public void input() {
        InputBox u = new InputBox();
        receiver.addElement(u);
        master.addChild(u);
    }
}
