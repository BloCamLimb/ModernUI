package icyllis.modernui.api.element;

import java.util.function.Consumer;

public interface IWidgetBuilder {

    IWidgetBuilder createConstTextLine(Consumer<ITextLineBuilder> builderConsumer);

    IWidgetBuilder createHoverTextLine(Consumer<ITextLineBuilder> builderConsumer);

    IWidgetBuilder createTexture(Consumer<ITextureBuilder> builderConsumer);

    IWidgetBuilder initEventListener(Consumer<IEventListenerBuilder> builderConsumer);

    IWidgetBuilder onHoverOn(Consumer<IWidgetModifier> consumer);

    IWidgetBuilder onHoverOff(Consumer<IWidgetModifier> consumer);

    IWidgetBuilder onLeftClick(Consumer<IWidgetModifier> consumer);
}
