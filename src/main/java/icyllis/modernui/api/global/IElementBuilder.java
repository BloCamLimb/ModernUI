package icyllis.modernui.api.global;

import icyllis.modernui.api.builder.IRectangleBuilder;
import icyllis.modernui.api.builder.ITextLineBuilder;
import icyllis.modernui.api.builder.ITextureBuilder;
import icyllis.modernui.api.template.*;
import icyllis.modernui.gui.element.IBase;
import net.minecraft.network.PacketBuffer;

import java.util.function.Consumer;
import java.util.function.IntPredicate;

public interface IElementBuilder {

    PacketBuffer getExtraData();

    void pool(IntPredicate availability, Consumer<Consumer<IBase>> poolModifier);

    IRectangleBuilder rectangle();

    ITextureBuilder texture();

    ITextLineBuilder textLine();

    IButtonT1 buttonT1();

    IButtonT1B buttonT1B();

    IButtonT2 buttonT2();

    IBackground defaultBackground();
}
