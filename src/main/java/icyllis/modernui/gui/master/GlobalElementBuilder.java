package icyllis.modernui.gui.master;

import icyllis.modernui.api.builder.IRectangleBuilder;
import icyllis.modernui.api.builder.ITextLineBuilder;
import icyllis.modernui.api.builder.ITextureBuilder;
import icyllis.modernui.api.template.*;
import icyllis.modernui.api.global.IElementBuilder;
import icyllis.modernui.gui.element.*;
import icyllis.modernui.gui.template.ButtonT1;
import icyllis.modernui.gui.template.Background;
import icyllis.modernui.gui.template.ButtonT1B;
import icyllis.modernui.gui.template.ButtonT2;
import net.minecraft.network.PacketBuffer;

import java.util.function.Consumer;
import java.util.function.IntPredicate;

public class GlobalElementBuilder implements IElementBuilder {

    public static final GlobalElementBuilder INSTANCE = new GlobalElementBuilder();

    private GlobalModuleManager manager = GlobalModuleManager.INSTANCE;

    private PacketBuffer extraData;

    private IModernScreen master;

    public void setMaster(IModernScreen master) {
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
    public void pool(IntPredicate availability, Consumer<Consumer<IBase>> poolModifier) {
        ElementPool p = new ElementPool(availability);
        poolModifier.accept(p);
        manager.add(p);
    }

    @Override
    public IBackground defaultBackground() {
        return new Background();
    }

    @Override
    public ITextLineBuilder textLine() {
        return new TextLine();
    }

    @Override
    public IButtonT1 buttonT1() {
        ButtonT1 b = new ButtonT1();
        master.addChild(b.listener);
        return b;
    }

    @Override
    public IButtonT1B buttonT1B() {
        ButtonT1B b = new ButtonT1B();
        master.addChild(b.listener);
        return b;
    }

    @Override
    public IButtonT2 buttonT2() {
        ButtonT2 b = new ButtonT2();
        master.addChild(b.listener);
        return b;
    }

    @Override
    public ITextureBuilder texture() {
        return new Texture2D();
    }

    @Override
    public IRectangleBuilder rectangle() {
        return new Rectangle();
    }

}
