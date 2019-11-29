package icyllis.modern.ui.button;

import icyllis.modern.system.ModernUI;
import icyllis.modern.ui.element.UIButton;
import icyllis.modern.ui.font.EmojiStringRenderer;
import icyllis.modern.ui.font.IFontRenderer;
import icyllis.modern.ui.font.StringRenderer;
import icyllis.modern.ui.font.TrueTypeRenderer;
import icyllis.modern.ui.master.DrawTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.function.Predicate;

/**
 * Double lined, intended for chat bar
 */
public class ChatInputBox extends UIButton {

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
    private String text = "";

    /** for blacklist **/
    private Predicate<String> filter = s -> true;

    private boolean shiftDown = false;

    public ChatInputBox() {
        selectorX = cursorX = x = 4;
        focused = true;
    }

    @Override
    public void draw() {

        if(isDoubleLined) {
            DrawTools.fill(x - 2, y - 12, x + w + 2, y + 12, 0x80000000);
            if (cursor != selector) {
                int left = Math.min(this.cursor, this.selector);
                int right = Math.max(this.cursor, this.selector);
                float le = Math.min(this.cursorX, this.selectorX);
                float ri = Math.max(this.cursorX, this.selectorX);
                if(left > firstLength) {
                    DrawTools.fill(le, y + 0.5f, ri, y + 11.5f, 0x8097def0);
                } else {
                    if(right > firstLength) {
                        DrawTools.fill(le, y - 11.5f, x + w, y - 0.5f, 0x8097def0);
                        DrawTools.fill(x, y + 0.5f, ri, y + 11.5f, 0x8097def0);
                    } else {
                        DrawTools.fill(le, y - 11.5f, ri, y - 0.5f, 0x8097def0);
                    }
                }
            }
            renderer.drawString(text.substring(0, firstLength), x, y - 10.5f, 0xdddddd, 0xff, 0);
            renderer.drawString(text.substring(firstLength), x, y + 1.5f, 0xdddddd, 0xff, 0);
        } else {
            DrawTools.fill(x - 2, y, x + w + 2, y + 12, 0x80000000);
            if (cursor != selector) {
                float le = Math.min(this.cursorX, this.selectorX);
                float ri = Math.max(this.cursorX, this.selectorX);
                DrawTools.fill(le, y + 0.5f, ri, y + 11.5f, 0x8097def0);
            }
            renderer.drawString(text, x, y + 1.5f, 0xdddddd, 0xff, 0);
        }

        if (timer < 10) {
            if(isDoubleLined) {
                DrawTools.fill(cursorX, cursor > firstLength ? y + 0.5f : y - 11.5f, cursorX + 0.5f, cursor > firstLength ? y + 11.5f : y - 0.5f, 0xffdddddd);
            } else {
                DrawTools.fill(cursorX, y + 0.5f, cursorX + 0.5f, y + 11.5f, 0xffdddddd);
            }
        }

    }

    public void tick() {
        timer++;
        timer %= 20;
    }

    @Override
    public void resize(int width, int height) {
        y = height - 14;
        w = width - 8;
        String r = text;
        text = "";
        writeText(r);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {

    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifier) {
        this.shiftDown = Screen.hasShiftDown();
        if (Screen.isSelectAll(key)) {
            setCursorSafety(0);
            setSelectorToEnd();
            return true;
        } else if (Screen.isCopy(key)) {
            Minecraft.getInstance().keyboardListener.setClipboardString(getSelectedText());
            return true;
        } else if (Screen.isPaste(key)) {
            writeText(Minecraft.getInstance().keyboardListener.getClipboardString());
            return true;
        } else if (Screen.isCut(key)) {
            Minecraft.getInstance().keyboardListener.setClipboardString(getSelectedText());
            writeText("");
            return true;
        } else {
            switch (key) {
                case GLFW.GLFW_KEY_BACKSPACE:
                    shiftDown = false;
                    delete(-1);
                    shiftDown = Screen.hasShiftDown();
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

    private void writeText(String textToWrite) {
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
            int line1s = renderer.sizeStringToWidth(result, w);
            if(result.length() > line1s) {
                this.firstLength = line1s;
                float line1width = renderer.getStringWidth(result.substring(0, line1s));
                this.text = renderer.trimStringToWidth(result, line1width + w, false);
                this.isDoubleLined = true;
            } else {
                this.firstLength = line1s;
                this.text = result;
                this.isDoubleLined = false;
            }
            this.setCursorSafety(left + toWrite.length()); // don't worry, it's safe
        }
    }

    private void setSelectorToEnd() {
        selector = text.length();
        if(isDoubleLined) {
            selectorX = x + renderer.getStringWidth(text.substring(firstLength));
        } else {
            selectorX = x + renderer.getStringWidth(text);
        }
    }

    private void setCursorSafety(int pos) {
        int checked = MathHelper.clamp(pos, 0, this.text.length());
        this.selector = this.cursor = checked;
        if (cursor > 0) {
            selectorX = cursorX = x + (cursor > firstLength ? renderer.getStringWidth(text.substring(firstLength, cursor)) : renderer.getStringWidth(text.substring(0, cursor)));
        } else {
            selectorX = cursorX = x;
        }
    }

    private String getSelectedText() {
        int left = Math.min(this.cursor, this.selector);
        int right = Math.max(this.cursor, this.selector);
        return text.substring(left, right);
    }

    private void delete(int amount) {

    }
}
