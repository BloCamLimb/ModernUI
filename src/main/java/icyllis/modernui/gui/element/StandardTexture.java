package icyllis.modernui.gui.element;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.master.DrawTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.function.Function;

public class StandardTexture extends Element {

    protected TextureManager textureManager = Minecraft.getInstance().textureManager;

    protected ResourceLocation res;

    protected float u, v;

    public float sizeW, sizeH;

    public float tintR, tintG, tintB, opacity;

    public float scale;

    public StandardTexture(Function<Integer, Float> x, Function<Integer, Float> y, float w, float h, ResourceLocation texture, float u, float v, int tintRGBA, float scale) {
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
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.pushMatrix();
        RenderSystem.color4f(tintR, tintG, tintB, opacity);
        RenderSystem.scalef(scale, scale, scale);
        textureManager.bindTexture(res);
        DrawTools.blit(x / scale, y / scale, u, v, sizeW, sizeH);
        RenderSystem.popMatrix();
    }
}
