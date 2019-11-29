package icyllis.modern.ui.button;

import icyllis.modern.system.ModernUI;
import icyllis.modern.ui.element.UIButton;
import icyllis.modern.ui.font.EmojiStringRenderer;
import icyllis.modern.ui.font.IFontRenderer;
import icyllis.modern.ui.master.DrawTools;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.math.MathHelper;

import java.util.function.Predicate;

/**
 * Double lined, intended for chat bar
 */
public class ChatInputBox extends UIButton {

    private final IFontRenderer renderer = EmojiStringRenderer.INSTANCE;

    private int timer;

    private int cursor = 0, selector = 0;
    private float cursorX;
    private String text = "";
    private Predicate<String> filter = s -> true;

    public ChatInputBox(float x, float y, float width, float height) {
        this.x = x;
        cursorX = x;
        this.y = y;
        w = width;
        h = height;
        visible = true;
        focused = true;
        alpha = 1.f;
    }

    @Override
    public void draw() {

        int line1c = renderer.sizeStringToWidth(text, w);
        // double line mode
        if(text.length() > line1c) {
            DrawTools.fill(x - 2, y - 12, x + w + 4, y + h, 0x80000000);
            String line1 = text.substring(0, line1c);
            float line1d = renderer.getStringWidth(line1);
            renderer.drawString(line1, x, y - 10.5f, 0xdddddd, 0xff, 0);
            renderer.drawString(text.substring(line1c), x, y + 1.5f, 0xdddddd, 0xff, 0);
            if(timer < 10) {
                DrawTools.fill(cursorX - line1d, y + 0.5f, cursorX - line1d + 0.5f, y + 11.5f, 0xffdddddd);
            }
        } else {
            DrawTools.fill(x - 2, y, x + w + 4, y + h, 0x80000000);
            renderer.drawString(text, x, y + 1.5f, 0xdddddd, 0xff, 0);
            if(timer < 10) {
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
        w = width - 10;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {

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
            this.text = renderer.trimStringToWidth(result, w * 2, false);
            setCursorSafety(this.text.equals(result) ? left + toWrite.length() : this.text.length());
        }
    }

    private void setCursorSafety(int pos) {
        this.selector = this.cursor = MathHelper.clamp(pos, 0, this.text.length());
        if (cursor > 0) {
            cursorX = x + renderer.getStringWidth(text.substring(0, cursor));
        }
    }
}
