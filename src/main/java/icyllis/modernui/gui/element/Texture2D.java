package icyllis.modernui.gui.element;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.api.element.IRectangleBuilder;
import icyllis.modernui.api.element.ITextureBuilder;
import icyllis.modernui.gui.master.DrawTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import java.util.function.Function;

public class Texture2D extends Base implements ITextureBuilder {

    protected TextureManager textureManager;
    protected ResourceLocation res;

    protected Function<Integer, Float> GWtBW, GWtBH;

    protected float sizeW, sizeH, u, v;

    private float tintR, tintG, tintB;

    private float scale = 1.0f;

    public Texture2D() {
        textureManager = Minecraft.getInstance().textureManager;
        tintR = tintG = tintB = 1.0f;
    }

    @Override
    public void draw() {
        GlStateManager.enableBlend();
        RenderSystem.pushMatrix();
        RenderSystem.color4f(tintR, tintG, tintB, alpha);
        RenderSystem.scalef(scale, scale, scale);
        textureManager.bindTexture(res);
        DrawTools.blit(renderX / scale, renderY / scale, u, v, sizeW, sizeH);
        RenderSystem.popMatrix();
    }

    @Override
    public ITextureBuilder setPos(Function<Integer, Float> x, Function<Integer, Float> y) {
        GWtBX = x;
        GWtBY = y;
        return this;
    }

    @Override
    public ITextureBuilder setPos(float x, float y) {
        GWtBX = w -> w / 2f + x;
        GWtBY = h -> h / 2f + y;
        return this;
    }

    @Override
    public ITextureBuilder setAlpha(float a) {
        alpha = a;
        return this;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        float w = GWtBW.apply(width);
        float h = GWtBH.apply(height);
        sizeW = w;
        sizeH = h;
    }

    @Override
    public ITextureBuilder setSize(float w, float h) {
        GWtBW = g -> w;
        GWtBH = g -> h;
        return this;
    }

    @Override
    public ITextureBuilder setSize(Function<Integer, Float> w, Function<Integer, Float> h) {
        GWtBW = w;
        GWtBH = h;
        return this;
    }

    @Override
    public ITextureBuilder setTexture(ResourceLocation texture) {
        res = texture;
        return this;
    }

    @Override
    public ITextureBuilder setUV(float u, float v) {
        this.u = u;
        this.v = v;
        return this;
    }

    @Override
    public ITextureBuilder setTint(int rgb) {
        float r = (rgb >> 16 & 255) / 255.0f;
        float g = (rgb >> 8 & 255) / 255.0f;
        float b = (rgb & 255) / 255.0f;
        tintR = r;
        tintG = g;
        tintB = b;
        return this;
    }

    @Override
    public ITextureBuilder setTint(float r, float g, float b) {
        tintR = r;
        tintG = g;
        tintB = b;
        return this;
    }

    @Override
    public ITextureBuilder setScale(float scale) {
        this.scale = scale;
        return this;
    }

}
