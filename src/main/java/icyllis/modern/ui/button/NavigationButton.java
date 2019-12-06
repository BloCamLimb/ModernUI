package icyllis.modern.ui.button;

import com.mojang.blaze3d.platform.GlStateManager;
import icyllis.modern.api.element.INavigationBuilder;
import icyllis.modern.api.element.ITextureBuilder;
import icyllis.modern.ui.element.UIButton;
import icyllis.modern.ui.element.UITexture;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL32;

import java.util.function.Consumer;

/**
 * A button to switch module
 */
public class NavigationButton extends UIButton<INavigationBuilder> implements INavigationBuilder {

    private UITexture texture;

    public NavigationButton() {

    }

    @Override
    public void draw() {
        GlStateManager.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.pushMatrix();
        if (mouseHovered) {
            textLine.draw();
        }
        texture.draw(mouseHovered);
        GlStateManager.popMatrix();
    }

    @Override
    public INavigationBuilder to(int index) {
        return this;
    }

    @Override
    public INavigationBuilder tex(Consumer<ITextureBuilder> consumer) {
        UITexture u = new UITexture();
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
