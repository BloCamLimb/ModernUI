package icyllis.modern.ui.element;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
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

public class UITexture extends UIElement<ITextureBuilder> implements ITextureBuilder, ITextureAnimator {

    protected TextureManager textureManager;
    protected ResourceLocation res;
    protected float u, v;

    protected Supplier<Integer> color;
    protected Supplier<Float> alpha;

    public UITexture() {
        textureManager = Minecraft.getInstance().textureManager;
        color = () -> 0xffffff;
        alpha = () -> 1.0f;
    }

    @Override
    public void draw() {
        GlStateManager.enableBlend();
        runColor();
        textureManager.bindTexture(res);
        float x = renderX.get(), y = renderY.get(), w = sizeW.get(), h = sizeH.get();
        DrawTools.blit(x, y, u, v, w, h);
    }

    public void draw(boolean hover) {
        GlStateManager.enableBlend();
        runColor();
        textureManager.bindTexture(res);
        float x = renderX.get(), y = renderY.get(), w = sizeW.get(), h = sizeH.get();
        DrawTools.blit(x, y, u, hover ? v + h : v, w, h);
    }

    private void runColor() {
        int color = this.color.get();
        float f = (color >> 16 & 0xff) / 255.0f;
        float f1 = (color >> 8 & 0xff) / 255.0f;
        float f2 = (color & 0xff) / 255.0f;
        RenderSystem.color4f(f, f1, f2, alpha.get());
    }

    @Override
    public ITextureBuilder tex(ResourceLocation texture) {
        res = texture;
        return this;
    }

    @Override
    public ITextureBuilder uv(float u, float v) {
        this.u = u;
        this.v = v;
        return this;
    }

    @Override
    public ITextureBuilder color(Supplier<Integer> color) {
        this.color = color;
        return this;
    }

    @Override
    public ITextureAnimator animated() {
        return this;
    }

    @Override
    public ITextureAnimator alpha(Consumer<IAlphaAnimation> a) {

        return this;
    }

}
