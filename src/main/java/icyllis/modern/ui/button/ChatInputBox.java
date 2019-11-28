package icyllis.modern.ui.button;

import icyllis.modern.system.ModernUI;
import icyllis.modern.ui.element.UIButton;
import icyllis.modern.ui.font.EmojiStringRenderer;
import icyllis.modern.ui.font.IFontRenderer;
import icyllis.modern.ui.font.StringRenderer;
import icyllis.modern.ui.master.DrawTools;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.math.MathHelper;

import java.util.function.Predicate;

/**
 * Double lined, intended for chat bar
 */
public class ChatInputBox extends UIButton {

    private final IFontRenderer renderer = EmojiStringRenderer.INSTANCE;
    private final IFontRenderer sizer = StringRenderer.STRING_RENDERER;

    private int timer;

    private int cursor = 0, selector = 0;
    private String text = "";
    private Predicate<String> filter = s -> true;

    public ChatInputBox(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        w = width;
        h = height;
        visible = true;
        focused = true;
        alpha = 1.f;
    }

    @Override
    public void draw() {
        DrawTools.fill(x - 1, y, x + w + 2, y + h, 0x80000000);
        String line1 = renderer.trimStringToWidth(text, w, false);
        renderer.drawString(line1, x, y + 1.5f, 0xdddddd, 0xff, 0);

        if(timer < 10) {

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
            ModernUI.LOGGER.info(result);
        }
    }

    private void setCursorSafety(int pos) {
        this.selector = this.cursor = MathHelper.clamp(pos, 0, this.text.length());
    }
}
