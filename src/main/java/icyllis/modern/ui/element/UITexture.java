package icyllis.modern.ui.element;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modern.api.animation.IAlphaAnimation;
import icyllis.modern.api.element.ITextureBuilder;
import icyllis.modern.ui.master.DrawTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class UITexture extends UIElement<ITextureBuilder> implements ITextureBuilder {

    protected TextureManager textureManager;
    protected ResourceLocation res;
    protected float u, v;

    private float tintR, tintG, tintB;

    public UITexture() {
        textureManager = Minecraft.getInstance().textureManager;
        alpha = tintR = tintG = tintB = 1.0f;
    }

    @Override
    public void draw() {
        GlStateManager.enableBlend();
        RenderSystem.color4f(tintR, tintG, tintB, alpha);
        textureManager.bindTexture(res);
        DrawTools.blit(renderX, renderY, u, v, sizeW, sizeH);
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

}
