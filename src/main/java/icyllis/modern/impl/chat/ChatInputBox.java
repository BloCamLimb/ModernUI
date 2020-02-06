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

package icyllis.modern.impl.chat;

import icyllis.modern.api.element.IButtonBuilder;
import icyllis.modern.ui.element.UIButton;
import icyllis.modern.ui.font.EmojiStringRenderer;
import icyllis.modern.ui.font.IFontRenderer;
import icyllis.modern.ui.master.DrawTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Double lined, intended for chat bar
 */
public final class ChatInputBox extends UIButton<IButtonBuilder> {

    private final IFontRenderer renderer = EmojiStringRenderer.INSTANCE;

    /** 0-20 cycle, for cursor splash **/
    private int timer = 0;

    /** character pos **/
    private int cursor = 0, selector = 0;

    /** cache cursor render x **/
    private float cursorX, selectorX;

    /** cache first line string length **/
    private int firstLength = 0;

    /** cache if text has two lines, for rendering **/
    private boolean isDoubleLined = false;

    /** full text **/
    private String text = "\u256a000e\u256a";

    /** for blacklist **/
    private Predicate<String> filter = s -> true;

    private Consumer<Boolean> onLineChanged = b -> {};

    private boolean shiftDown = false;

    ChatInputBox() {
        selectorX = cursorX = 4;
        renderX = 4f;
        focused = true;
    }

    @Override
    public void draw() {
        if(isDoubleLined) {
            DrawTools.fillRectWithColor(renderX - 2, renderY - 12, renderX + sizeW + 2, renderY + 12, 0x80000000);
            if (cursor != selector) {
                int left = Math.min(this.cursor, this.selector);
                int right = Math.max(this.cursor, this.selector);
                float le = Math.min(this.cursorX, this.selectorX);
                float ri = Math.max(this.cursorX, this.selectorX);
                if(left > firstLength) {
                    DrawTools.fillRectWithColor(le, renderY + 0.5f, ri, renderY + 11.5f, 0x8097def0);
                } else {
                    if(right > firstLength) {
                        if(cursor < selector) {
                            DrawTools.fillRectWithColor(cursorX, renderY - 11.5f, renderX + sizeW, renderY - 0.5f, 0x8097def0);
                            DrawTools.fillRectWithColor(renderX, renderY + 0.5f, selectorX, renderY + 11.5f, 0x8097def0);
                        } else {
                            DrawTools.fillRectWithColor(selectorX, renderY - 11.5f, renderX + sizeW, renderY - 0.5f, 0x8097def0);
                            DrawTools.fillRectWithColor(renderX, renderY + 0.5f, cursorX, renderY + 11.5f, 0x8097def0);
                        }
                    } else {
                        DrawTools.fillRectWithColor(le, renderY - 11.5f, ri, renderY - 0.5f, 0x8097def0);
                    }
                }
            }
            renderer.drawString(text.substring(0, firstLength), renderX, renderY - 10.5f, 0xdddddd, 0xff, 0);
            renderer.drawString(text.substring(firstLength), renderX, renderY + 1.5f, 0xdddddd, 0xff, 0);
        } else {
            DrawTools.fillRectWithColor(renderX - 2, renderY, renderX + sizeW + 2, renderY + 12, 0x80000000);
            if (cursor != selector) {
                float le = Math.min(this.cursorX, this.selectorX);
                float ri = Math.max(this.cursorX, this.selectorX);
                DrawTools.fillRectWithColor(le, renderY + 0.5f, ri, renderY + 11.5f, 0x8097def0);
            }
            renderer.drawString(text, renderX, renderY + 1.5f, 0xdddddd, 0xff, 0);
        }

        if (timer < 10) {
            if(isDoubleLined) {
                DrawTools.fillRectWithColor(cursorX, cursor > firstLength ? renderY + 0.5f : renderY - 11.5f, cursorX + 0.5f, cursor > firstLength ? renderY + 11.5f : renderY - 0.5f, 0xffdddddd);
            } else {
                DrawTools.fillRectWithColor(cursorX, renderY + 0.5f, cursorX + 0.5f, renderY + 11.5f, 0xffdddddd);
            }
        }

    }

    void tick() {
        timer++;
        timer %= 20;
    }

    @Override
    public void resize(int width, int height) {
        renderY = height - 14;
        sizeW = width - 8;
        String r = text;
        text = "";
        writeText(r);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        // override superclass
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if(mouseButton == 0 && isMouseInRange(mouseX, mouseY)) {
            float dx = (float) (mouseX - renderX) + 1.0f;
            double dy = mouseY - renderY;
            if(isDoubleLined) {
                if(dy > 0 && dy < 12) {
                    setCursorSafety(firstLength + renderer.sizeStringToWidth(text.substring(firstLength), dx));
                    setSelector();
                } else if(dy < 0 && dy > -12) {
                    setCursorSafety(renderer.sizeStringToWidth(text, dx));
                    setSelector();
                }
            } else {
                if(dy > 0 && dy < 12) {
                    setCursorSafety(renderer.sizeStringToWidth(text, dx));
                    setSelector();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean isMouseInRange(double mouseX, double mouseY) {
        return mouseX >= renderX && mouseX <= renderX + sizeW && mouseY >= renderY + (isDoubleLined ? -12 : 0) && mouseY <= renderY + 12;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifier) {
        this.shiftDown = Screen.hasShiftDown();
        if (Screen.isSelectAll(key)) {
            this.setCursorSafety(0);
            this.setSelectorToEnd();
            return true;
        } else if (Screen.isCopy(key)) {
            Minecraft.getInstance().keyboardListener.setClipboardString(getSelectedText());
            return true;
        } else if (Screen.isPaste(key)) {
            this.writeText(Minecraft.getInstance().keyboardListener.getClipboardString());
            return true;
        } else if (Screen.isCut(key)) {
            Minecraft.getInstance().keyboardListener.setClipboardString(getSelectedText());
            this.writeText("");
            return true;
        } else {
            switch (key) {
                case GLFW.GLFW_KEY_BACKSPACE:
                    this.shiftDown = false;
                    this.delete(false);
                    this.shiftDown = Screen.hasShiftDown();
                    return true;
                case GLFW.GLFW_KEY_DELETE:
                    this.shiftDown = false;
                    this.delete(true);
                    this.shiftDown = Screen.hasShiftDown();
                    return true;
                case GLFW.GLFW_KEY_RIGHT:
                    if(Screen.hasControlDown()) {
                        this.setCursorSafety(text.length());
                    } else {
                        this.moveCursor(true);
                    }
                    return true;
                case GLFW.GLFW_KEY_LEFT:
                    if(Screen.hasControlDown()) {
                        this.setCursorSafety(0);
                    } else {
                        this.moveCursor(false);
                    }
                    return true;
                case GLFW.GLFW_KEY_DOWN:
                    if(isDoubleLined && cursor <= firstLength) {
                        setCursorSafety(renderer.sizeStringToWidth(text, renderer.getStringWidth(text.substring(0, cursor)) + sizeW));
                    }
                    return true;
                case GLFW.GLFW_KEY_UP:
                    if(isDoubleLined & cursor > firstLength) {
                        setCursorSafety(renderer.sizeStringToWidth(text, renderer.getStringWidth(text.substring(firstLength, cursor)) + 1.0f));
                    }
                    return true;
                case GLFW.GLFW_KEY_HOME:
                    this.setCursorSafety(0);
                    return true;
                case GLFW.GLFW_KEY_END:
                    this.setCursorSafety(text.length());
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char c, int i) {
        if (SharedConstants.isAllowedCharacter(c)) {
            writeText(Character.toString(c));
            return true;
        }
        return false;
    }

    public void setLineChanged(Consumer<Boolean> s) {
        onLineChanged = s;
    }

    public void writeText(String textToWrite) {
        String result = "";
        String toWrite = SharedConstants.filterAllowedCharacters(textToWrite);

        int left = Math.min(this.cursor, this.selector);
        int right = Math.max(this.cursor, this.selector);

        if (!this.text.isEmpty())
            result = result + this.text.substring(0, left);

        result = result + toWrite;

        if (!this.text.isEmpty())
            result = result + this.text.substring(right);

        if (this.filter.test(result)) {
            int line1s = renderer.sizeStringToWidth(result, sizeW - 0.5f);
            if(result.length() > line1s) {
                this.firstLength = line1s;
                float line1width = renderer.getStringWidth(result.substring(0, line1s));
                this.text = renderer.trimStringToWidth(result, line1width + sizeW, false);
                this.isDoubleLined = true;
                onLineChanged.accept(true);
            } else {
                this.firstLength = line1s;
                this.text = result;
                this.isDoubleLined = false;
                onLineChanged.accept(false);
            }
            this.setCursorSafety(left + toWrite.length()); // trimmed, but don't worry, it's safe
            this.setSelector(); // needed with shift pressed
        }
    }

    private void setSelectorToEnd() {
        selector = text.length();
        if(isDoubleLined) {
            selectorX = renderX + renderer.getStringWidth(text.substring(firstLength));
        } else {
            selectorX = renderX + renderer.getStringWidth(text);
        }
    }

    private void setCursorSafety(int pos) {
        this.cursor = MathHelper.clamp(pos, 0, this.text.length());
        if (cursor > 0) {
            cursorX = renderX + (cursor > firstLength
                    ? renderer.getStringWidth(text.substring(firstLength, cursor))
                    : renderer.getStringWidth(text.substring(0, cursor)));
        } else {
            cursorX = renderX;
        }
        if(!shiftDown) {
            this.selector = cursor;
            this.selectorX = cursorX;
        }
    }

    /**
     * Needed when no matter if pressed shift
     */
    private void setSelector() {
        this.selector = cursor;
        this.selectorX = cursorX;
    }

    private void moveCursor(boolean reverse) {
        if(reverse) {
            if(text.length() >= cursor + 6 && text.codePointAt(cursor) == '\u256a') {
                setCursorSafety(cursor + 6);
            } else {
                setCursorSafety(cursor + 1);
            }
        } else {
            if(cursor >= 6 && text.length() >= 6 && text.codePointAt(cursor - 1) == '\u256a') {
                setCursorSafety(cursor - 6);
            } else {
                setCursorSafety(cursor - 1);
            }
        }
        timer = 0;
    }

    private String getSelectedText() {
        int left = Math.min(this.cursor, this.selector);
        int right = Math.max(this.cursor, this.selector);
        return text.substring(left, right);
    }

    private void delete(boolean reverse) {
        if(Screen.hasControlDown()) {
            deleteWords(reverse);
        } else {
            deleteFromCursor(reverse);
        }
    }

    private void deleteWords(boolean reverse) {
        if (this.text.isEmpty()) {
            return;
        }
        String result = "";
        if (reverse) {
            result = text.substring(0, cursor);
            if(filter.test(result)) {
                this.text = result;
                if (isDoubleLined && text.length() <= firstLength) {
                    this.firstLength = text.length();
                    this.isDoubleLined = false;
                    onLineChanged.accept(false);
                }
            }
        } else {
            if (cursor < text.length()) {
                result = text.substring(cursor);
            }
            if(filter.test(result)) {
                this.text = result;
                if(isDoubleLined && text.length() <= firstLength) {
                    this.firstLength = text.length();
                    this.isDoubleLined = false;
                    onLineChanged.accept(false);
                }
                setCursorSafety(text.length() - cursor);
            }
        }

    }

    private void deleteFromCursor(boolean reverse) {
        if (this.text.isEmpty()) {
            return;
        }
        if(cursor != selector) {
            writeText("");
        } else {
            int left = reverse ? cursor : cursor - 1;
            int right = reverse ? cursor + 1 : cursor;
            int multi = 1;
            String result = "";
            if(left >= 0) {
                if(!reverse && cursor >= 6 && text.length() >= 6 && text.codePointAt(left) == '\u256a') {
                    result = text.substring(0, left - 5);
                    multi = 6;
                } else {
                    result = text.substring(0, left);
                }
            }
            if(right < text.length()) {
                if(reverse && text.length() >= cursor + 6 && text.codePointAt(cursor) == '\u256a') {
                    result = text.substring(right + 5);
                    multi = 6;
                } else {
                    result = result + text.substring(right);
                }
            }
            if(filter.test(result)) {
                this.text = result;
                if(isDoubleLined && text.length() <= firstLength) {
                    this.firstLength = text.length();
                    this.isDoubleLined = false;
                    onLineChanged.accept(false);
                }
                if(!reverse) {
                    setCursorSafety(cursor - multi);
                }
            }
        }
    }
}
