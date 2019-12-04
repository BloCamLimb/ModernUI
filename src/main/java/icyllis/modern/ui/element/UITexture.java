package icyllis.modern.ui.element;

import com.mojang.blaze3d.platform.GlStateManager;
import icyllis.modern.api.element.IElement;
import icyllis.modern.api.element.ITextureBuilder;
import icyllis.modern.ui.master.DrawTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;

import java.util.function.Supplier;

public class UITexture implements ITextureBuilder, IElement {

    protected TextureManager textureManager;
    protected ResourceLocation res;
    protected Runnable color;
    protected float bx, by, x, y, u, v, w, h;

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

    public void draw(boolean hover) {
        GlStateManager.enableBlend();
        color.run();
        textureManager.bindTexture(res);
        DrawTools.blit(x, y, u, hover ? v + h : v, w, h);
    }

    @Override
    public void resize(int width, int height) {
        x = width / 2f + bx;
        y = height / 2f + by;
    }

    @Override
    public ITextureBuilder tex(ResourceLocation texture) {
        res = texture;
        return this;
    }

    @Override
    public ITextureBuilder pos(float x, float y) {
        bx = x;
        by = y;
        return this;
    }

    @Override
    public ITextureBuilder uv(float u, float v) {
        this.u = u;
        this.v = v;
        return this;
    }

    @Override
    public ITextureBuilder size(float w, float h) {
        this.w = w;
        this.h = h;
        return this;
    }

    @Override
    public ITextureBuilder color(Supplier<Integer> color) {
        this.color = () -> {
          float f = (color.get() >> 16 & 0xff) / 255.0f;
          float f1 = (color.get() >> 8 & 0xff) / 255.0f;
          float f2 = (color.get() & 0xff) / 255.0f;
          GlStateManager.color3f(f, f1, f2);
        };
        return this;
    }

}
