package icyllis.modernui.api.global;

import icyllis.modernui.api.builder.IRectangleBuilder;
import icyllis.modernui.api.builder.ITextLineBuilder;
import icyllis.modernui.api.builder.ITextureBuilder;
import icyllis.modernui.api.template.*;
import net.minecraft.network.PacketBuffer;

public interface IElementBuilder {

    PacketBuffer getExtraData();

    IRectangleBuilder rectangle();

    ITextureBuilder texture();

    ITextLineBuilder textLine();

    IButtonT1 buttonT1();

    IBackground defaultBackground();
}
