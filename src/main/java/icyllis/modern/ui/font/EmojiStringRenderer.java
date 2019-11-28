package icyllis.modern.ui.font;

import icyllis.modern.system.ModernUI;
import icyllis.modern.ui.master.DrawTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import java.util.*;

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
    private WeakHashMap<String, Integer> SIZES = new WeakHashMap<>();

    @Override
    public float drawString(String str, float startX, float startY, int color, int alpha, float align) {
        EmojiText entry = MAPS.get(str);
        if (entry == null) {
            entry = cache(str);
        }
        entry.text.forEach(t -> FONT.drawString(t.str, startX + t.x, startY, color, 255, 0));
        TEX.bindTexture(EMOJI);
        entry.emoji.forEach(e -> DrawTools.blit(startX + e.x, startY + e.y, e.u, e.v, TEX_WID, TEX_WID));
        return 0;
    }

    @Override
    public float getStringWidth(String str) {
        return 0;
    }

    @Override
    public int sizeStringToWidth(String str, float width) {
        if(str.isEmpty()) {
            return 0;
        }
        int r = 0;
        if(!SIZES.containsKey(str)) {
            EmojiText entry = MAPS.get(str);
            if (entry == null) {
                entry = cache(str);
            }
            float lastT = -2000;
            int lastTC = -1;
            for (Text t : entry.text) {
                if (t.x <= width) {
                    lastT = t.x;
                    lastTC++;
                } else
                    break;
            }
            float lastE = -2000;
            int lastEC = 0;
            for (Emoji e : entry.emoji) {
                if (e.x <= width) {
                    lastE = e.x;
                    lastEC++;
                } else
                    break;
            }
            String extra = "";
            if (lastT > lastE) {
                extra = FONT.trimStringToWidth(entry.text.get(lastTC).str, width - lastT, false);
                for (int i = 0; i < lastTC; i++) {
                    r += entry.text.get(i).str.length();
                }
            } else {
                for (int i = 0; i < lastTC + 1; i++) {
                    r += entry.text.get(i).str.length();
                }
                if(width - lastE < TEX_WID) {
                    lastEC--;
                }
            }
            r += extra.length();
            r += lastEC * 6;
            SIZES.put(str, r);
            return r;
        } else {
            return SIZES.get(str);
        }
    }

    @Override
    public String trimStringToWidth(String str, float width, boolean reverse) {
        int length = sizeStringToWidth(str, width);
        str = str.substring(0, Math.min(length, str.length()));
        return str;
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
        float x;

        Text(String str, float x) {
            this.str = str;
            this.x = x;
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

    private EmojiText cache(String str) {
        int start = 0, next;
        List<Text> text = new ArrayList<>();
        List<Emoji> emoji = new ArrayList<>();
        float totalWidth = 0;
        if (str.length() < 6 || str.indexOf(':') == -1) {
            text.add(new Text(str, totalWidth));
        } else {
            int lastFound = 0;
            while ((next = str.indexOf(':', start)) != -1 && next + 5 < str.length()) {
                if (str.charAt(next + 5) == ':') {
                    String s2 = str.substring(next + 1, next + 5);
                    try {
                        int code2 = Integer.parseInt(s2, 0x10);
                        String s3 = str.substring(start, Math.min(next, str.length()));
                        text.add(new Text(s3, totalWidth));
                        float wi = FONT.getStringWidth(s3);
                        totalWidth += wi;
                        Emoji e = new Emoji(totalWidth, -1, (code2 >> 8) * TEX_WID, (code2 & 0xff) * TEX_WID);
                        totalWidth += TEX_WID;
                        emoji.add(e);
                        start = next + 5;
                        lastFound = start + 1;
                    } catch (final NumberFormatException e) {
                        start = next + 1;
                    }
                } else {
                    start = next + 1;
                }
            }
            if (lastFound < str.length())
                text.add(new Text(str.substring(lastFound), totalWidth));
        }
        EmojiText ems = new EmojiText(text, emoji);
        MAPS.put(str, ems);
        return ems;
    }
}
