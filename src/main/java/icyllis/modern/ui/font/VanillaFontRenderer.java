package icyllis.modern.ui.font;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modern.system.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.INestedGuiEventHandler;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class VanillaFontRenderer implements IFontRenderer {

    static final VanillaFontRenderer INSTANCE = new VanillaFontRenderer();
    private final FontRenderer FONT;
    {
        FONT = Minecraft.getInstance().fontRenderer;
        //FONT = Minecraft.getInstance().getFontResourceManager().getFontRenderer(new ResourceLocation(ModernUI.MODID, "unix"));
    }

    @Override
    public float drawString(String str, float startX, float startY, int color, int alpha, float align) {
        startX = startX - FONT.getStringWidth(str) * align * 2;
        return FONT.drawString(str, startX, startY, color | alpha << 24);
    }

    @Override
    public float getStringWidth(String str) {
        return FONT.getStringWidth(str);
    }

    @Override
    public int sizeStringToWidth(String str, float width) {
        return FONT.sizeStringToWidth(str, (int) width);
    }

    @Override
    public String trimStringToWidth(String str, float width, boolean reverse) {
        return FONT.trimStringToWidth(str, (int) width, reverse);
    }
}
