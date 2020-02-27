package icyllis.modernui.api.template;

import icyllis.modernui.api.builder.IEventListenerInitializer;
import icyllis.modernui.api.builder.ITextLineBuilder;
import icyllis.modernui.api.builder.ITextureBuilder;

import java.util.function.Consumer;

public interface IButtonT1 {

    IButtonT1 createHoverTextLine(Consumer<ITextLineBuilder> builderConsumer);

    IButtonT1 createTexture(Consumer<ITextureBuilder> builderConsumer);

    IButtonT1 initEventListener(Consumer<IEventListenerInitializer> builderConsumer);

    IButtonT1 onLeftClick(Runnable runnable);
}
