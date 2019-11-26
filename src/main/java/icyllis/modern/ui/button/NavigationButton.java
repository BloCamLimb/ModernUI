package icyllis.modern.ui.button;

import icyllis.modern.api.element.INavigationST;
import icyllis.modern.api.element.ITextureST;
import icyllis.modern.ui.element.UIButton;
import icyllis.modern.ui.element.UITexture;
import icyllis.modern.ui.element.UITextureButton;

import java.util.function.Consumer;

/**
 * Only way to switch module
 */
public class NavigationButton extends UIButton<INavigationST> implements INavigationST {

    private UITextureButton texture;

    @Override
    public void draw() {
        if (mouseHovered) {
            textLine.draw();
        }
        texture.draw(mouseHovered);
    }

    @Override
    public INavigationST to(int id) {
        return this;
    }

    @Override
    public INavigationST tex(Consumer<ITextureST> consumer) {
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

    @Override
    protected void onMouseHoverChanged() {

    }
}
