package icyllis.modern.ui.button;

import icyllis.modern.api.element.INavigationBuilder;
import icyllis.modern.api.element.ITextureBuilder;
import icyllis.modern.ui.element.UIButton;
import icyllis.modern.ui.element.UITextureButton;

import java.util.function.Consumer;

/**
 * Only way to switch module
 */
public class NavigationButton extends UIButton<INavigationBuilder> implements INavigationBuilder {

    private UITextureButton texture;

    @Override
    public void draw() {
        if (mouseHovered) {
            textLine.draw();
        }
        texture.draw(mouseHovered);
    }

    @Override
    public INavigationBuilder to(int id) {
        return this;
    }

    @Override
    public INavigationBuilder tex(Consumer<ITextureBuilder> consumer) {
        UITextureButton u = new UITextureButton();
        consumer.accept(u);
        texture = u;
        return this;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        texture.resize(width, height);
    }
}
