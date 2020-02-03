package icyllis.modern.api.global;

import icyllis.modern.api.element.IConstTextBuilder;
import icyllis.modern.api.element.INavigationBuilder;
import icyllis.modern.api.element.IVarTextBuilder;
import icyllis.modern.api.element.ITextureBuilder;
import net.minecraft.network.PacketBuffer;

public interface IElementBuilder {

    PacketBuffer getExtraData();

    void defaultBackground();

    IVarTextBuilder varText();

    IConstTextBuilder constText();

    ITextureBuilder texture();

    INavigationBuilder navigation();

    void input();
}
