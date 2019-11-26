package icyllis.modern.ui.element;

import com.mojang.blaze3d.platform.GlStateManager;
import icyllis.modern.ui.master.DrawTools;

public class UITextureButton extends UITexture {

    public void draw(boolean hover) {
        GlStateManager.enableBlend();
        color.run();
        textureManager.bindTexture(res);
        DrawTools.blit(x, y, u, hover ? v + h : v, w, h);
    }
}
