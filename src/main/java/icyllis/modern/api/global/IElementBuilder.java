package icyllis.modern.api.global;

import icyllis.modern.api.animation.IAnimationBuilder;
import icyllis.modern.api.element.*;
import net.minecraft.network.PacketBuffer;

import java.util.function.Consumer;

public interface IElementBuilder {

    PacketBuffer getExtraData();

    void defaultBackground();

    ITextBuilder text();

    ITextureBuilder texture();

    INavigationBuilder navigation();

    IColorBuilder colorRect();

    void input();
}
