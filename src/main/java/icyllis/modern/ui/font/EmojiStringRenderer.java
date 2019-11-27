package icyllis.modern.ui.font;

import icyllis.modern.system.ModernUI;
import icyllis.modern.ui.master.DrawTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class EmojiStringRenderer implements IFontRenderer {

    public static final EmojiStringRenderer INSTANCE = new EmojiStringRenderer();

    private final IFontRenderer FONT;
    private final TextureManager TEX;
    {
        FONT = StringRenderer.STRING_RENDERER;
        TEX = Minecraft.getInstance().textureManager;
    }

    private final ResourceLocation EMOJI = new ResourceLocation(ModernUI.MODID, "gui/emoji.png");
    private final float TEX_WID = 11.5f;

    private WeakHashMap<String, EmojiText> MAPS = new WeakHashMap<>();

    @Override
    public float drawString(String str, float startX, float startY, int color, int alpha, float align) {
        EmojiText entry = MAPS.get(str);
        if (entry == null) {
            entry = cache(str, startX, startY);
        }
        entry.text.forEach(t -> FONT.drawString(t.str, t.x, t.y, color, 255, 0));
        TEX.bindTexture(EMOJI);
        entry.emoji.forEach(e -> DrawTools.blit(e.x, e.y, e.u, e.v, TEX_WID, TEX_WID));
        return 0;
    }

    @Override
    public float getStringWidth(String str) {
        return 0;
    }

    @Override
    public int sizeStringToWidth(String str, float width) {
        return 0;
    }

    @Override
    public String trimStringToWidth(String str, float width, boolean reverse) {
        return null;
    }

    private static class EmojiText {

        List<Text> text;
        List<Emoji> emoji;

        EmojiText(List<Text> text, List<Emoji> emoji) {
            this.text = text;
            this.emoji = emoji;
        }
    }

    private static class Text {

        String str;
        float x, y;

        Text(String str, float x, float y) {
            this.str = str;
            this.x = x;
            this.y = y;
        }
    }

    private static class Emoji {

        float x, y, u, v;

        Emoji(float x, float y, float u, float v) {
            this.x = x;
            this.y = y;
            this.u = u;
            this.v = v;
        }
    }

    private EmojiText cache(String str, float startX, float startY) {
        int start = 0, next;
        List<Text> text = new ArrayList<>();
        List<Emoji> emoji = new ArrayList<>();
        float totalWidth = 0;
        while ((next = str.indexOf(':', start)) != -1 && next + 5 < str.length()) {
            if (str.charAt(next + 5) == ':') {
                String s2 = str.substring(next + 1, next + 5);
                try {
                    int code2 = Integer.parseInt(s2, 0x10);
                    String s3 = str.substring(start, Math.min(next, str.length()));
                    text.add(new Text(s3, startX + totalWidth, startY));
                    float wi = FONT.getStringWidth(s3);
                    totalWidth += wi;
                    Emoji e = new Emoji(startX + totalWidth, startY - 1, (code2 >> 8) * TEX_WID, (code2 & 0xff) * TEX_WID);
                    totalWidth += TEX_WID;
                    emoji.add(e);
                    start = next + 5;
                } catch (final NumberFormatException e) {
                    start = next + 1;
                }
            } else {
                start = next + 1;
            }
        }
        start++;
        if (start < str.length())
            text.add(new Text(str.substring(start), startX + totalWidth, startY));
        EmojiText ems = new EmojiText(text, emoji);
        MAPS.put(str, ems);
        return ems;
    }
}
