package icyllis.modern.ui.element;

import com.mojang.blaze3d.platform.GlStateManager;
import icyllis.modern.api.element.IElement;
import icyllis.modern.api.element.ITextureTracker;
import icyllis.modern.ui.master.DrawTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import java.util.function.Supplier;

public class UITexture implements ITextureTracker, IElement {

    private TextureManager textureManager;
    private ResourceLocation res;
    private Runnable color;
    private float bx, by, x, y, u, v, w, h;

    public UITexture() {
        textureManager = Minecraft.getInstance().textureManager;
        color = () -> {};
    }

    @Override
    public void draw() {
        GlStateManager.enableBlend();
        color.run();
        textureManager.bindTexture(res);
        DrawTools.blit(x, y, u, v, w, h);
    }

    @Override
    public void resize(int width, int height) {
        x = width / 2f + bx;
        y = height / 2f + by;
    }

    @Override
    public ITextureTracker tex(ResourceLocation texture) {
        res = texture;
        return this;
    }

    @Override
    public ITextureTracker pos(float x, float y) {
        bx = x;
        by = y;
        return this;
    }

    @Override
    public ITextureTracker uv(float x, float y) {
        u = x;
        v = y;
        return this;
    }

    @Override
    public ITextureTracker size(float x, float y) {
        w = x;
        h = y;
        return this;
    }

    @Override
    public ITextureTracker color(Supplier<Integer> color) {
        this.color = () -> {
          float f = (color.get() >> 16 & 0xff) / 255.0f;
          float f1 = (color.get() >> 8 & 0xff) / 255.0f;
          float f2 = (color.get() & 0xff) / 255.0f;
          GlStateManager.color3f(f, f1, f2);
        };
        return this;
    }

}
