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

package icyllis.modernui.gui.widget;

import icyllis.modernui.font.FontTools;
import icyllis.modernui.gui.math.Align3H;
import icyllis.modernui.gui.master.*;
import icyllis.modernui.gui.math.Align9D;
import icyllis.modernui.gui.math.Color3f;
import icyllis.modernui.gui.math.Locator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Single line text input box
 */
//FIXME some bugs?
public class TextField extends Widget implements IKeyboardListener {

    // previous text, new text, filtered text
    private BiFunction<String, String, String> filter = (s, t) -> t;

    private String text = "";

    private int maxStringLength = 32;

    private int lineScrollOffset;
    private int cursorPosition;
    private int selectionEnd;

    private boolean shiftDown = false;

    private float leftMargin = 2;
    private float rightMargin = 2;

    protected int timer = 0;
    protected boolean editing = false;

    @Nullable
    private Decoration decoration;

    @Nullable
    private Consumer<TextField> listener;

    protected boolean runtimeUpdate;

    private Runnable enterOperation = () -> getHost().setKeyboardListener(null);

    public TextField(IHost host, Builder builder) {
        super(host, builder);
    }

    public void setDecoration(@Nonnull Function<TextField, Decoration> function) {
        if (decoration == null) {
            this.decoration = function.apply(this);
            width -= decoration.getHeaderLength();
            width -= decoration.getTrailerLength();
        }
    }

    public void setListener(@Nonnull Consumer<TextField> listener, boolean runtime) {
        this.listener = listener;
        runtimeUpdate = runtime;
    }

    public void setFilter(BiFunction<String, String, String> filter) {
        this.filter = filter;
    }

    // experimental
    private void setMargin(float left, float right) {
        this.leftMargin = left;
        this.rightMargin = right;
    }

    public void setMaxStringLength(int length) {
        this.maxStringLength = length;
        if (this.text.length() > length) {
            this.text = this.text.substring(0, length);
            this.onTextChanged();
        }
    }

    public void setEnterOperation(Runnable enterOperation) {
        this.enterOperation = enterOperation;
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        if (decoration != null) {
            decoration.draw(canvas, time);
        }

        int ds = this.cursorPosition - this.lineScrollOffset;
        int de = this.selectionEnd - this.lineScrollOffset;
        String s = FontTools.trimStringToWidth(this.text.substring(this.lineScrollOffset), getVisibleWidth(), false);
        boolean b = ds >= 0 && ds <= s.length();

        float lx = x1 + leftMargin;
        float ty = y1 + (height - 8) / 2;
        float cx = lx;
        if (de > s.length()) {
            de = s.length();
        }

        canvas.setTextAlign(Align3H.LEFT);

        if (!s.isEmpty()) {
            String s1 = b ? s.substring(0, ds) : s;
            canvas.setRGBA(0.88f, 0.88f, 0.88f, 1);
            canvas.drawText(s1, lx, ty);
            float c = FontTools.getStringWidth(s1);
            cx += c;
        }

        float kx = cx;
        if (!b) {
            kx = ds > 0 ? x2 - rightMargin : lx;
        }

        // draw selection box
        if (de != ds) {
            float l1 = lx + FontTools.getStringWidth(s.substring(0, de));
            canvas.setColor(Color3f.BLUE_C, 0.5f);
            canvas.drawRect(kx, ty - 1, l1, ty + 10);
        }

        canvas.setRGBA(0.88f, 0.88f, 0.88f, 1);

        if (!s.isEmpty() && b && ds < s.length()) {
            canvas.drawText(s.substring(ds), cx, ty);
        }

        // draw cursor
        if (editing && timer < 10) {
            canvas.drawRect(kx, ty - 1, kx + 0.5f, ty + 10);
        }
    }

    @Override
    public void tick(int ticks) {
        if (editing) {
            timer++;
            timer %= 20;
        }
    }

    /**
     * Sets the text of the text box, and moves the cursor to the end.
     */
    public void setText(String textIn) {
        String result = filter.apply(text, textIn);
        if (!result.equals(text)) {
            if (result.length() > this.maxStringLength) {
                this.text = result.substring(0, this.maxStringLength);
            } else {
                this.text = result;
            }

            this.setCursorToEnd();
            this.setSelectionPos(this.cursorPosition);
            this.onTextChanged();
        }
    }

    private void onTextChanged() {
        onTextChanged(false);
    }

    protected void onTextChanged(boolean force) {
        if (listener != null && (runtimeUpdate || force)) {
            listener.accept(this);
        }
    }

    /**
     * Returns the contents of the text box
     */
    public String getText() {
        return text;
    }

    public int getMaxStringLength() {
        return maxStringLength;
    }

    public float getLeftMargin() {
        return leftMargin;
    }

    public float getRightMargin() {
        return rightMargin;
    }

    @Nullable
    public Decoration getDecoration() {
        return decoration;
    }

    public void writeText(String textToWrite) {
        String result = "";
        String toWrite = SharedConstants.filterAllowedCharacters(textToWrite);
        int i = Math.min(this.cursorPosition, this.selectionEnd);
        int j = Math.max(this.cursorPosition, this.selectionEnd);
        int canWriteCount = this.maxStringLength - this.text.length() - (i - j);

        if (!this.text.isEmpty()) {
            result = result + this.text.substring(0, i); // write text that before cursor and without selected
        }

        int l;
        if (canWriteCount < toWrite.length()) {
            result = result + toWrite.substring(0, canWriteCount); // ignore excess part
            l = canWriteCount;
        } else {
            result = result + toWrite;
            l = toWrite.length();
        }

        if (!this.text.isEmpty() && j < this.text.length()) { // write text that after cursor
            result = result + this.text.substring(j);
        }

        result = filter.apply(text, result);
        if (!result.equals(text)) {
            this.text = result;
            setCursorPos(i + l);
            setSelectionPos(cursorPosition);
            this.onTextChanged();
            timer = 0;
        }
    }

    public void setSelectionPos(int position) {
        int i = this.text.length();
        this.selectionEnd = MathHelper.clamp(position, 0, i);

        if (this.lineScrollOffset > i) {
            this.lineScrollOffset = i;
        }

        String s = FontTools.trimStringToWidth(this.text.substring(this.lineScrollOffset), getVisibleWidth(), false);
        int k = s.length() + this.lineScrollOffset;
        if (this.selectionEnd == this.lineScrollOffset) {
            this.lineScrollOffset -= FontTools.trimStringToWidth(this.text, getVisibleWidth(), true).length();
        }

        if (this.selectionEnd > k) {
            this.lineScrollOffset += this.selectionEnd - k;
        } else if (this.selectionEnd <= this.lineScrollOffset) {
            this.lineScrollOffset -= this.lineScrollOffset - this.selectionEnd;
        }

        this.lineScrollOffset = MathHelper.clamp(this.lineScrollOffset, 0, i);
        timer = 0;
    }

    public void setCursorToEnd() {
        this.setCursorPos(text.length());
    }

    public void setCursorPos(int pos) {
        this.cursorPosition = MathHelper.clamp(pos, 0, this.text.length());
        if (!shiftDown) {
            this.setSelectionPos(cursorPosition);
        }
    }

    @Override
    public void locate(float px, float py) {
        if (decoration != null) {
            float c = decoration.getHeaderLength();
            px += c;
        }
        super.locate(px, py);
    }

    @Override
    protected void onMouseHoverEnter(double mouseX, double mouseY) {
        super.onMouseHoverEnter(mouseX, mouseY);
        MouseTools.useIBeamCursor();
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        MouseTools.useDefaultCursor();
    }

    @Override
    protected boolean onMouseLeftClick(double mouseX, double mouseY) {
        if (!editing) {
            startEditing();
        }
        if (mouseX >= x1 + leftMargin && mouseX <= x2 - rightMargin) {
            float i = (float) (mouseX - x1 - leftMargin);
            String s = FontTools.trimStringToWidth(this.text.substring(this.lineScrollOffset), getVisibleWidth(), false);
            shiftDown = Screen.hasShiftDown();
            // FIX vanilla's bug
            this.setCursorPos(FontTools.trimStringToWidth(s, i, false).length() + this.lineScrollOffset);
        }
        return true;
    }

    private void startEditing() {
        getHost().setKeyboardListener(this);
        editing = true;
        timer = 0;
    }

    @Override
    public void stopKeyboardListening() {
        editing = false;
        timer = 0;
        onTextChanged(true);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        this.shiftDown = Screen.hasShiftDown();
        if (Screen.isSelectAll(keyCode)) {
            this.setCursorToEnd();
            this.setSelectionPos(0);
            return true;
        } else if (Screen.isCopy(keyCode)) {
            Minecraft.getInstance().keyboardListener.setClipboardString(getSelectedText());
            return true;
        } else if (Screen.isPaste(keyCode)) {
            this.writeText(Minecraft.getInstance().keyboardListener.getClipboardString());
            return true;
        } else if (Screen.isCut(keyCode)) {
            Minecraft.getInstance().keyboardListener.setClipboardString(getSelectedText());
            this.writeText("");
            return true;
        } else {
            switch (keyCode) {
                case GLFW.GLFW_KEY_ESCAPE:
                    getHost().setKeyboardListener(null);
                    return true;
                case GLFW.GLFW_KEY_BACKSPACE:
                    this.shiftDown = false;
                    this.delete(-1);
                    this.shiftDown = Screen.hasShiftDown();
                    return true;
                case GLFW.GLFW_KEY_DELETE:
                    this.shiftDown = false;
                    this.delete(1);
                    this.shiftDown = Screen.hasShiftDown();
                    return true;
                case GLFW.GLFW_KEY_RIGHT:
                    if (Screen.hasControlDown()) {
                        this.setCursorPos(this.getNthWordFromCursor(1));
                    } else {
                        this.moveCursorBy(1);
                    }
                    return true;
                case GLFW.GLFW_KEY_LEFT:
                    if (Screen.hasControlDown()) {
                        this.setCursorPos(this.getNthWordFromCursor(-1));
                    } else {
                        this.moveCursorBy(-1);
                    }
                    return true;
                case GLFW.GLFW_KEY_HOME:
                    this.setCursorPos(0);
                    return true;
                case GLFW.GLFW_KEY_END:
                    this.setCursorToEnd();
                    return true;
                case GLFW.GLFW_KEY_ENTER:
                case GLFW.GLFW_KEY_KP_ENTER:
                    this.enterOperation.run();
                    return true;
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (SharedConstants.isAllowedCharacter(codePoint)) {
            writeText(Character.toString(codePoint));
            return true;
        }
        return false;
    }

    private void delete(int num) {
        if (Screen.hasControlDown()) {
            deleteWords(num);
        } else {
            deleteFromCursor(num);
        }
    }

    public void deleteWords(int num) {
        if (!this.text.isEmpty()) {
            if (this.selectionEnd != this.cursorPosition) {
                this.writeText("");
            } else {
                this.deleteFromCursor(this.getNthWordFromCursor(num) - this.cursorPosition);
            }
        }
    }

    public void deleteFromCursor(int num) {
        if (!this.text.isEmpty()) {
            if (this.selectionEnd != this.cursorPosition) {
                this.writeText("");
            } else {
                boolean reverse = num < 0;

                int left = reverse ? this.cursorPosition + num : this.cursorPosition;
                int right = reverse ? this.cursorPosition : this.cursorPosition + num;

                String result = "";

                if (left >= 0) {
                    result = this.text.substring(0, left);
                }

                if (right < this.text.length()) {
                    result = result + this.text.substring(right);
                }

                result = filter.apply(text, result);
                if (!result.equals(text)) {
                    this.text = result;
                    if (reverse) {
                        this.moveCursorBy(result.length() - text.length());
                    }
                    this.onTextChanged();
                    timer = 0;
                }
            }
        }
    }

    public void moveCursorBy(int offset) {
        this.setCursorPos(this.cursorPosition + offset);
    }

    /**
     * Get visible width if there's margin
     */
    public float getVisibleWidth() {
        return width - leftMargin - rightMargin;
    }

    @Nonnull
    private String getSelectedText() {
        int left = Math.min(this.cursorPosition, this.selectionEnd);
        int right = Math.max(this.cursorPosition, this.selectionEnd);
        return text.substring(left, right);
    }

    /**
     * Gets the starting index of the word at the specified number of words away from the cursor position.
     */
    public int getNthWordFromCursor(int numWords) {
        return this.getNthWordFromPos(numWords, cursorPosition);
    }

    /**
     * Gets the starting index of the word at a distance of the specified number of words away from the given position.
     */
    private int getNthWordFromPos(int num, int pos) {
        return this.getNthWordFromPosWS(num, pos);
    }

    /**
     * Like getNthWordFromPos (which wraps this), but adds option for skipping consecutive spaces
     */
    private int getNthWordFromPosWS(int num, int pos) {
        int i = pos;
        boolean reverse = num < 0;
        int amount = Math.abs(num);

        for (int k = 0; k < amount; ++k) {
            if (!reverse) {
                int l = this.text.length();
                i = this.text.indexOf(32, i);
                if (i == -1) {
                    i = l;
                } else {
                    while (i < l && this.text.charAt(i) == ' ') {
                        ++i;
                    }
                }
            } else {
                while (i > 0 && this.text.charAt(i - 1) == ' ') {
                    --i;
                }

                while (i > 0 && this.text.charAt(i - 1) != ' ') {
                    --i;
                }
            }
        }

        return i;
    }

    @Nonnull
    @Override
    public Class<? extends Widget.Builder> getBuilder() {
        return Builder.class;
    }

    public static class Builder extends Widget.Builder {

        public Builder() {

        }

        @Override
        public Builder setWidth(float width) {
            super.setWidth(width);
            return this;
        }

        @Override
        public Builder setHeight(float height) {
            super.setHeight(height);
            return this;
        }

        @Override
        public Builder setLocator(@Nonnull Locator locator) {
            super.setLocator(locator);
            return this;
        }

        @Override
        public Builder setAlign(@Nonnull Align9D align) {
            super.setAlign(align);
            return this;
        }

        @Nonnull
        @Override
        public TextField build(IHost host) {
            return new TextField(host, this);
        }
    }

    public static abstract class Decoration {

        protected final TextField instance;

        public Decoration(TextField instance) {
            this.instance = instance;
        }

        public abstract void draw(@Nonnull Canvas canvas, float time);

        public abstract float getHeaderLength();

        public abstract float getTrailerLength();
    }

    public static class Frame extends Decoration {

        @Nullable
        private final String title;

        private final float titleLength;

        private float r, g, b, a;

        public Frame(TextField instance, @Nullable String title, int color) {
            super(instance);
            this.title = title;
            this.titleLength = FontTools.getStringWidth(title);
            setColor(color);
        }

        @Override
        public void draw(@Nonnull Canvas canvas, float time) {
            canvas.setRGBA(0, 0, 0, 0.25f);
            canvas.drawRect(instance.x1 - getHeaderLength(), instance.y1, instance.x2, instance.y2);
            canvas.setRGBA(r, g, b, a);
            canvas.drawRectOutline(instance.x1 - getHeaderLength(), instance.y1, instance.x2, instance.y2, 0.51f);
            if (title != null) {
                canvas.setTextAlign(Align3H.LEFT);
                canvas.drawText(title, instance.x1 - titleLength, instance.y1 + (instance.height - 8) / 2f);
            }
        }

        @Override
        public float getHeaderLength() {
            if (titleLength == 0) {
                return 0;
            }
            return titleLength + 2;
        }

        @Override
        public float getTrailerLength() {
            return 0;
        }

        public void setColor(int color) {
            a = (color >> 24 & 0xff) / 255f;
            r = (color >> 16 & 0xff) / 255f;
            g = (color >> 8 & 0xff) / 255f;
            b = (color & 0xff) / 255f;
        }

    }
}
