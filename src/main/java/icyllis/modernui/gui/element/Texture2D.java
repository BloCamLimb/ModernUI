package icyllis.modernui.gui.element;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.master.DrawTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import java.util.function.Function;

public class Texture2D extends Base {

    protected TextureManager textureManager = Minecraft.getInstance().textureManager;

    protected ResourceLocation res;

    protected float sizeW, sizeH, u, v;

    public float tintR, tintG, tintB;

    private float scale;

    public Texture2D(Function<Integer, Float> x, Function<Integer, Float> y, float w, float h, ResourceLocation texture, float u, float v, int tintRGBA, float scale) {
        super(x, y);
        this.sizeW = w;
        this.sizeH = h;
        this.res = texture;
        this.u = u;
        this.v = v;
        this.opacity = (tintRGBA >> 24 & 255) / 255.0f;
        this.tintR = (tintRGBA >> 16 & 255) / 255.0f;
        this.tintG = (tintRGBA >> 8 & 255) / 255.0f;
        this.tintB = (tintRGBA & 255) / 255.0f;
        this.scale = scale;
    }

    @Override
    public void draw(float currentTime) {
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.pushMatrix();
        RenderSystem.color4f(tintR, tintG, tintB, opacity);
        RenderSystem.scalef(scale, scale, scale);
        textureManager.bindTexture(res);
        DrawTools.blit(x / scale, y / scale, u, v, sizeW, sizeH);
        RenderSystem.popMatrix();
    }
}
