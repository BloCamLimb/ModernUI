package icyllis.modern.ui.font;

public class StringRenderer {

    public static IFontRenderer STRING_RENDERER;
    static {
        STRING_RENDERER = TrueTypeRenderer.INSTANCE;
    }

    public static void switchRenderer(boolean mod) {
        if (mod) {
            STRING_RENDERER = TrueTypeRenderer.INSTANCE;
        } else {
            STRING_RENDERER = VanillaFontRenderer.INSTANCE;
        }
    }
}
