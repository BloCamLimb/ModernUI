package icyllis.modern.ui.master;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

public class DrawTools {

    public static void fill(float x, float y, float w, float h, int color) {
        float f3 = (float)(color >> 24 & 255) / 255.0F;
        float f = (float)(color >> 16 & 255) / 255.0F;
        float f1 = (float)(color >> 8 & 255) / 255.0F;
        float f2 = (float)(color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture();
        GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color4f(f, f1, f2, f3);
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
        bufferbuilder.pos(x, h, 0.0D).endVertex();
        bufferbuilder.pos(w, h, 0.0D).endVertex();
        bufferbuilder.pos(w, y, 0.0D).endVertex();
        bufferbuilder.pos(x, y, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture();
        GlStateManager.disableBlend();
    }

    public static void fillGradient(float x, float y, float width, float height, int startColor, int endColor, float zLevel) {
        float f = (float)(startColor >> 24 & 255) / 255.0F;
        float f1 = (float)(startColor >> 16 & 255) / 255.0F;
        float f2 = (float)(startColor >> 8 & 255) / 255.0F;
        float f3 = (float)(startColor & 255) / 255.0F;
        float f4 = (float)(endColor >> 24 & 255) / 255.0F;
        float f5 = (float)(endColor >> 16 & 255) / 255.0F;
        float f6 = (float)(endColor >> 8 & 255) / 255.0F;
        float f7 = (float)(endColor & 255) / 255.0F;
        GlStateManager.disableTexture();
        GlStateManager.enableBlend();
        GlStateManager.disableAlphaTest();
        GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(width, y, zLevel).color(f1, f2, f3, f).endVertex();
        bufferbuilder.pos(x, y, zLevel).color(f1, f2, f3, f).endVertex();
        bufferbuilder.pos(x, height, zLevel).color(f5, f6, f7, f4).endVertex();
        bufferbuilder.pos(width, height, zLevel).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlphaTest();
        GlStateManager.enableTexture();
    }

    public static void blit(float x, float y, float u, float v, float width, float height) {
        blitToScale(x, y, 0, u, v, width, height);
    }

    public static void blitWithZ(float x, float y, float z, float u, float v, float width, float height) {
        blitToScale(x, y, z, u, v, width, height);
    }

    private static void blitToScale(float x, float y, float z, float textureX, float textureY, float width, float height) {
        blitRender(x, x + width, y, y + height, z, textureX / 256.0, (textureX + width) / 256.0, textureY / 256.0, (textureY + height) / 256.0);
    }

    private static void blitRender(double x1, double x2, double y1, double y2, double z, double textureX1, double textureX2, double textureY1, double textureY2) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x1, y2, z).tex(textureX1, textureY2).endVertex();
        bufferbuilder.pos(x2, y2, z).tex(textureX2, textureY2).endVertex();
        bufferbuilder.pos(x2, y1, z).tex(textureX2, textureY1).endVertex();
        bufferbuilder.pos(x1, y1, z).tex(textureX1, textureY1).endVertex();
        tessellator.draw();
    }
}
