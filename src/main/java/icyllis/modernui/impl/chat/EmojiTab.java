/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
import icyllis.modernui.graphics.math.TextAlign;
import icyllis.modernui.graphics.font.TrueTypeRenderer;
import icyllis.modernui.gui.math.Color3i;
import icyllis.modernui.system.StorageManager;
import icyllis.modernui.system.ConstantsLibrary;
import icyllis.modernui.gui.test.DrawTools;
import javafx.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.renderer.texture.TextureManager;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class EmojiTab implements IGuiEventListener {

    private int y, screenWidth, screenHeight, showMode = 0;
    private int hisY1, hisX2 = 70, hisY2;
    private int selY1, selX2 = 161, selPage = 0;
    private boolean isDoubleLined = false;
    private Pair<String, Integer> hoverEmoji;
    private ChatInputBox inputBox;

    private List<Pair<String, Integer>> cachedEmoji;

    private final TextureManager TEX = Minecraft.getInstance().textureManager;

    EmojiTab(ChatInputBox inputBox) {
        this.inputBox = inputBox;
        cachedEmoji = StorageManager.findEmoji("");
    }

    public void draw(int mouseX, int mouseY) {
        RenderSystem.disableAlphaTest();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        TEX.bindTexture(ConstantsLibrary.ICONS);
        if(showMode == 0) {
            RenderSystem.color3f(0.6f, 0.6f, 0.6f);
        }
        DrawTools.blit(5, y - 10, 0, 0, 8, 8);

        if(showMode == 1) {
            DrawTools.fillRectWithColor(2, hisY1, hisX2, hisY2, 0x80000000);
            GlStateManager.disableBlend();
            GlStateManager.enableBlend();
            TEX.bindTexture(EmojiStringRenderer.EMOJI);
            boolean anyFound = false;
            CYCLE:
            for(int y = 0; y < 3; y++) {
                for(int x = 0; x < 5; x++) {
                    int index = y * 5 + x;
                    if(index >= StorageManager.getEmojiHistory().size()) {
                        break CYCLE;
                    }
                    int rx = 4 + x * 13;
                    int ry = hisY1 + 2 + y * 13;
                    anyFound = isAnyFound(mouseX, mouseY, anyFound, rx, ry, StorageManager.getEmojiHistory().get(index));
                }
            }
            if(!anyFound) {
                hoverEmoji = null;
            }
        } else if (showMode == 2) {
            DrawTools.fillRectWithColor(2, selY1, selX2, hisY2, 0x80000000);
            GlStateManager.disableBlend();
            GlStateManager.enableBlend();
            TEX.bindTexture(EmojiStringRenderer.EMOJI);
            boolean anyFound = false;
            CYCLE:
            for(int y = 0; y < 6; y++) {
                for(int x = 0; x < 12; x++) {
                    int index = selPage * 72 + y * 12 + x;
                    if(index >= cachedEmoji.size()) {
                        break CYCLE;
                    }
                    int rx = 4 + x * 13;
                    int ry = this.selY1 + 2 + y * 13;
                    anyFound = isAnyFound(mouseX, mouseY, anyFound, rx, ry, cachedEmoji.get(index));
                }
            }
            if(!anyFound) {
                hoverEmoji = null;
            }
        }


        /*for(int x = 0; x < 2; x++) {
            for(int y = 0; y < 2; y++) {
                int index = x * 5 + y;
                *//*if(index >= cachedEmoji.size()) {
                    break;
                }
                int code = cachedEmoji.get(index);
                int code3 = code >> 8;
                int code4 = code & 0xff;*//*
                DrawTools.blit(20 + x * 12, 20 + y * 12, x * 11.5f, y * 11.5f, 11.5f, 11.5f);
            }
        }*/
        RenderSystem.enableAlphaTest();
    }

    private boolean isAnyFound(int mouseX, int mouseY, boolean anyFound, int rx, int ry, Pair<String, Integer> emoji) {
        int code = emoji.getValue();
        String name = emoji.getKey();
        if(!anyFound && mouseX > rx && mouseX < rx + 12 && mouseY > ry && mouseY < ry + 12) {
            GlStateManager.disableBlend();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            DrawTools.fillRectWithColor(rx, ry, rx + 11.5f, ry + 11.5f, 0x40d0d0d0);
            TrueTypeRenderer.INSTANCE.drawString(name, 18, y - 11, Color3i.GRAY_224, 1.0f, TextAlign.LEFT);
            GlStateManager.disableBlend();
            GlStateManager.enableBlend();
            hoverEmoji = emoji;
            anyFound = true;
            TEX.bindTexture(EmojiStringRenderer.EMOJI);
        }
        int code3 = code >> 8;
        int code4 = code & 0xff;
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.color3f(0.867f, 0.867f, 0.867f);
        DrawTools.blit(rx, ry, code3 * 11.5f, code4 * 11.5f, 11.5f, 11.5f);
        return anyFound;
    }

    public void resize(int width, int height) {
        y = isDoubleLined ? height - 26 : height - 14;
        hisY1 = y - 54;
        hisY2 = y - 12;
        selY1 = y - 93;
        screenHeight = height;
        screenWidth = width;
    }

    void setDoubleLine(boolean b) {
        isDoubleLined = b;
        resize(screenWidth, screenHeight);
    }

    @Override
    public void mouseMoved(double xPos, double yPos) {
        if(showMode == 1) {
            if(xPos < 2 || xPos > hisX2 || yPos > hisY2 || yPos < hisY1) {
                showMode = 0;
            }
        }
        if(showMode != 2 && xPos >= 5 && xPos <= 12 && yPos >= y - 12 && yPos <= y - 2) {
            showMode = 1;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if(mouseButton == 0) {
            if(showMode == 1) {
                if(hoverEmoji != null) {
                    inputBox.writeText("\u256a" + Integer.toHexString(hoverEmoji.getValue() | 0x10000).substring(1) + "\u256a");
                    StorageManager.addToEmojiHistory(hoverEmoji);
                    return true;
                }
            }
            if(mouseX >= 5 && mouseX <= 12 && mouseY >= y - 12 && mouseY <= y - 2) {
                showMode = 2;
                selPage = 0;
                return true;
            }
            if(showMode == 2) {
                if(hoverEmoji != null) {
                    inputBox.writeText("\u256a" + Integer.toHexString(hoverEmoji.getValue() | 0x10000).substring(1) + "\u256a");
                    StorageManager.addToEmojiHistory(hoverEmoji);
                    return true;
                }
                if(mouseX < 2 || mouseX > selX2 || mouseY > hisY2 || mouseY < selY1) {
                    showMode = 0;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        if(showMode == 2 && mouseX >= 2 && mouseX <= selX2 && mouseY <= hisY2 && mouseY >= selY1) {
            if (scroll == -1) {
                if ((selPage + 1) * 72 > cachedEmoji.size()) {
                    return false;
                }
                ++selPage;
                return true;
            } else if (scroll == 1) {
                selPage = Math.max(0, --selPage);
                return true;
            }
        }
        return false;
    }
}
