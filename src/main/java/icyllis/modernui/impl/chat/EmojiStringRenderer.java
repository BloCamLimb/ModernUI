/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.impl.chat;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.graphics.font.TextAlign;
import icyllis.modernui.graphics.font.IFontRenderer;
import icyllis.modernui.graphics.font.TrueTypeRenderer;
import icyllis.modernui.graphics.math.Color3i;
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.ui.test.DrawTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.*;

//TODO
public class EmojiStringRenderer implements IFontRenderer {

    public static final EmojiStringRenderer INSTANCE = new EmojiStringRenderer();

    private final IFontRenderer FONT;
    private final TextureManager TEX;
    {
        FONT = TrueTypeRenderer.INSTANCE;
        TEX = Minecraft.getInstance().textureManager;
    }

    public static final ResourceLocation EMOJI = new ResourceLocation(ModernUI.MODID, "textures/gui/emoji.png");
    private final float TEX_WID = 11.5f;

    private SizeKey lookKey = new SizeKey();

    private WeakHashMap<String, EmojiText> MAPS = new WeakHashMap<>();

    @Override
    public float drawString(String str, float startX, float startY, int r, int g, int b, int a, TextAlign align) {
        EmojiText entry = MAPS.get(str);
        if (entry == null) {
            entry = cache(str);
        }
        entry.text.forEach(t -> FONT.drawString(t.str, startX + t.x, startY, r, g, b, a, align));
        RenderSystem.color3f(0.867f, 0.867f, 0.867f);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        TEX.bindTexture(EMOJI);
        entry.emoji.forEach(e -> DrawTools.blit(startX + e.x, startY - 1, e.u, e.v, TEX_WID, TEX_WID));
        return 0; // unsupported
    }

    @Override
    public float getStringWidth(String str) {
        EmojiText entry = MAPS.get(str);
        if (entry == null) {
            entry = cache(str);
        }
        float lastT = entry.text.size() > 0 ? entry.text.get(entry.text.size() - 1).x : 0;
        float lastE = entry.emoji.size() > 0 ? entry.emoji.get(entry.emoji.size() - 1).x : -1;
        if(lastT > lastE) {
            return lastT + FONT.getStringWidth(entry.text.get(entry.text.size() - 1).str);
        } else {
            return lastE + 11.5f;
        }
    }

    @Override
    public int sizeStringToWidth(String str, float width) {
        if(str.isEmpty()) {
            return 0;
        }
        int r = 0;
        EmojiText entry = MAPS.get(str);
        if (entry == null) {
            entry = cache(str);
        }
        float lastT = 0;
        int lastTC = -1;
        for (Text t : entry.text) {
            if (t.x <= width) {
                lastT = t.x;
                lastTC++;
            } else
                break;
        }
        float lastE = -1;
        int lastEC = 0;
        for (Emoji e : entry.emoji) {
            if (e.x <= width) {
                lastE = e.x;
                lastEC++;
            } else
                break;
        }
        int extra = 0;
        if (lastT > lastE) {
            extra = FONT.sizeStringToWidth(entry.text.get(lastTC).str, width - Math.max(lastT, lastEC * 11.5f));
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
        r += extra;
        r += lastEC * 6;
        return r;
    }

    @Override
    public String trimStringToWidth(String str, float width, boolean reverse) {
        int length = sizeStringToWidth(str, width);
        str = str.substring(0, length);
        return str;
    }

    private static class SizeKey {

        String str = "";
        int w = 0;

        @Override
        public int hashCode() {
            int code = 0, length = str.length();

            for (int index = 0; index < length; index++) {
                char c = str.charAt(index);
                code = (code * 31) + c;
            }
            code = (code * 31) + w;
            return code;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if(obj instanceof SizeKey) {
                SizeKey key = (SizeKey) obj;
                String other = key.str;
                int length = str.length();

                if (length != other.length()) {
                    return false;
                }
                if (key.w != w) {
                    return false;
                }

                for (int index = 0; index < length; index++) {
                    char c1 = str.charAt(index);
                    char c2 = other.charAt(index);

                    if (c1 != c2) {
                        return false;
                    }

                }
                return true;
            }

            return false;
        }

        @Override
        public String toString() {
            return str;
        }
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

    public static class Emoji {

        float x, u, v;

        Emoji(float x, float u, float v) {
            this.x = x;
            this.u = u;
            this.v = v;
        }
    }

    private EmojiText cache(String str) {
        int start = 0, next;
        List<Text> text = new ArrayList<>();
        List<Emoji> emoji = new ArrayList<>();
        float totalWidth = 0;
        if (str.length() < 6 || str.indexOf('\u256a') == -1) {
            text.add(new Text(str, totalWidth));
        } else {
            int lastFound = 0;
            while ((next = str.indexOf('\u256a', start)) != -1 && next + 5 < str.length()) {
                if (str.charAt(next + 5) == '\u256a') {
                    String s2 = str.substring(next + 1, next + 5);
                    try {
                        int code2 = Integer.parseInt(s2, 0x10);
                        int code3 = code2 >> 8;
                        int code4 = code2 & 0xff;
                        String s3 = str.substring(start, Math.min(next, str.length()));
                        text.add(new Text(s3, totalWidth));
                        float wi = FONT.getStringWidth(s3);
                        totalWidth += wi;
                        Emoji e = new Emoji(totalWidth, code3 * TEX_WID, code4 * TEX_WID);
                        totalWidth += TEX_WID;
                        emoji.add(e);
                        lastFound = start = next + 6;
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
