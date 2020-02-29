package icyllis.modernui.gui.element;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.master.DrawTools;
import icyllis.modernui.gui.master.GlobalAnimationManager;
import icyllis.modernui.system.ModernUI;
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

    public Texture2D(Function<Integer, Float> x, Function<Integer, Float> y, float w, float h, ResourceLocation res, float u, float v, float r, float g, float b, float a, float s) {
        this.fakeX = x;
        this.fakeY = y;
        this.sizeW = w;
        this.sizeH = h;
        this.res = res;
        this.u = u;
        this.v = v;
        this.tintR = r;
        this.tintG = g;
        this.tintB = b;
        this.alpha = a;
        this.scale = s;
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

}
