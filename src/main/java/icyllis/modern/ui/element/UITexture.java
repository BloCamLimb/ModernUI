package icyllis.modern.ui.element;

import com.mojang.blaze3d.platform.GlStateManager;
import icyllis.modern.api.animation.IAlphaAnimation;
import icyllis.modern.api.element.ITextureAnimator;
import icyllis.modern.api.element.ITextureBuilder;
import icyllis.modern.ui.animation.AlphaAnimation;
import icyllis.modern.ui.master.DrawTools;
import icyllis.modern.ui.master.GlobalAnimationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class UITexture implements ITextureBuilder, ITextureAnimator, IElement {

    protected TextureManager textureManager;
    protected ResourceLocation res;
    protected float bx, by, x, y, u, v, w, h;

    protected Supplier<Integer> color;
    protected Supplier<Float> alpha;

    public UITexture() {
        textureManager = Minecraft.getInstance().textureManager;
        color = () -> 0xffffff;
        alpha = () -> 1.0f;
    }

    @Override
    public void draw() {
        runColor();
        textureManager.bindTexture(res);
        DrawTools.blit(x, y, u, v, w, h);
    }

    public void draw(boolean hover) {
        runColor();
        textureManager.bindTexture(res);
        DrawTools.blit(x, y, u, hover ? v + h : v, w, h);
    }

    private void runColor() {
        int color = this.color.get();
        float f = (color >> 16 & 0xff) / 255.0f;
        float f1 = (color >> 8 & 0xff) / 255.0f;
        float f2 = (color & 0xff) / 255.0f;
        GlStateManager.color4f(f, f1, f2, alpha.get());
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
        this.color = color;
        return this;
    }

    @Override
    public ITextureAnimator toAnimated() {
        return this;
    }

    @Override
    public ITextureAnimator alpha(Consumer<IAlphaAnimation> a) {
        AlphaAnimation i = GlobalAnimationManager.INSTANCE.newAlpha(alpha.get());
        a.accept(i);
        alpha = i;
        return this;
    }

}
