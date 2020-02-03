package icyllis.modern.ui.font;

import icyllis.modern.system.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;

public class StringRenderer {

    public static IFontRenderer STRING_RENDERER;

    static {
        STRING_RENDERER = TrueTypeRenderer.INSTANCE;
        //STRING_RENDERER = VanillaFontRenderer.INSTANCE;
    }

    public static void switchRenderer(boolean mod) {
        if (mod) {
            STRING_RENDERER = TrueTypeRenderer.INSTANCE;
        } else {
            STRING_RENDERER = VanillaFontRenderer.INSTANCE;
        }
    }
}
