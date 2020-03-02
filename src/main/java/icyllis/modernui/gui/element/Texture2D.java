package icyllis.modernui.gui.element;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.api.builder.ITextureBuilder;
import icyllis.modernui.gui.master.DrawTools;
import icyllis.modernui.gui.master.GlobalAnimationManager;
import icyllis.modernui.system.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Function;

public class Texture2D extends Base implements ITextureBuilder {

    protected TextureManager textureManager = Minecraft.getInstance().textureManager;

    protected ResourceLocation res;

    protected float sizeW, sizeH, u, v;

    public float tintR, tintG, tintB;

    private float scale;

    public Texture2D() {

    }

    @Override
    public void draw() {
        GlStateManager.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.pushMatrix();
        RenderSystem.color4f(tintR, tintG, tintB, alpha);
        RenderSystem.scalef(scale, scale, scale);
        textureManager.bindTexture(res);
        DrawTools.blit(renderX / scale, renderY / scale, u, v, sizeW, sizeH);
        RenderSystem.popMatrix();
    }

    @Override
    public ITextureBuilder init(Function<Integer, Float> x, Function<Integer, Float> y, float w, float h, ResourceLocation texture, float u, float v, int tintRGBA, float scale) {
        this.fakeX = x;
        this.fakeY = y;
        this.sizeW = w;
        this.sizeH = h;
        this.res = texture;
        this.u = u;
        this.v = v;
        this.alpha = (tintRGBA >> 24 & 255) / 255.0f;
        this.tintR = (tintRGBA >> 16 & 255) / 255.0f;
        this.tintG = (tintRGBA >> 8 & 255) / 255.0f;
        this.tintB = (tintRGBA & 255) / 255.0f;
        this.scale = scale;
        return this;
    }

    @Override
    public void buildToPool(Consumer<IBase> pool) {
        pool.accept(this);
    }

    @Override
    public void buildToPool(Consumer<IBase> pool, Consumer<Texture2D> consumer) {
        pool.accept(this);
        consumer.accept(this);
    }

    @Override
    public Texture2D buildForMe() {
        return this;
    }
}
