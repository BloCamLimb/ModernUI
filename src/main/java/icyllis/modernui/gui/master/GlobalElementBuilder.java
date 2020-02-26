package icyllis.modernui.gui.master;

import icyllis.modernui.api.element.IRectangleBuilder;
import icyllis.modernui.api.element.ITextLineBuilder;
import icyllis.modernui.api.element.ITextureBuilder;
import icyllis.modernui.api.element.IWidgetBuilder;
import icyllis.modernui.api.global.IElementBuilder;
import icyllis.modernui.gui.element.Rectangle;
import icyllis.modernui.gui.element.Texture2D;
import icyllis.modernui.gui.element.Widget;
import icyllis.modernui.gui.template.InputBox;
import icyllis.modernui.gui.template.EBackground;
import icyllis.modernui.gui.element.TextLine;
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
        EBackground u = new EBackground();
        receiver.addElement(u);
    }

    @Override
    public ITextLineBuilder textLine() {
        TextLine u = new TextLine();
        receiver.addElement(u);
        return u;
    }

    @Override
    public IWidgetBuilder widget() {
        Widget w = new Widget();
        receiver.addElement(w);
        master.addChild(w.listener);
        return w;
    }

    @Override
    public ITextureBuilder texture() {
        Texture2D u = new Texture2D();
        receiver.addElement(u);
        return u;
    }

    @Override
    public IRectangleBuilder rectangle() {
        Rectangle u = new Rectangle();
        receiver.addElement(u);
        return u;
    }

}
