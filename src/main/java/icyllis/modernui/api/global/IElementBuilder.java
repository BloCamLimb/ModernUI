package icyllis.modernui.api.global;

import icyllis.modernui.api.element.*;
import net.minecraft.network.PacketBuffer;

public interface IElementBuilder {

    PacketBuffer getExtraData();

    IRectangleBuilder rectangle();

    ITextureBuilder texture();

    ITextLineBuilder textLine();

    IWidgetBuilder widget();

    void defaultBackground();
}
