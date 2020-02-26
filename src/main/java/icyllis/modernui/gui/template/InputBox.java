package icyllis.modernui.gui.template;

import icyllis.modernui.gui.element.EventListener;
import icyllis.modernui.gui.font.IFontRenderer;
import icyllis.modernui.gui.font.StringRenderer;

import java.util.function.Predicate;

@Deprecated
public class InputBox {

    public static final Predicate<String> digitFilter = s -> s.matches("[0-9]+"),
            hexFilter = s -> s.matches("(?i)[0-9a-f]+");

    protected IFontRenderer renderer = StringRenderer.STRING_RENDERER;

    /** if can focus and write things**/
    /*protected boolean enabled, canLoseFocus;
    protected int maxStringLength = 32;
    protected int cursorPosition, selectionEnd, lineScrollOffset;
    private Predicate<String> filter = s -> true;
    protected String text = "";

    public InputBox() {
        enabled = true;
        canLoseFocus = true;
        visible = true;
    }

    @Override
    public void draw() {
        if (visible) {
            if (true) {
                DrawTools.fillRectWithColor(this.x - 1, this.y - 1, this.x + this.w + 1, this.y + this.h + 1, -6250336);
                DrawTools.fillRectWithColor(this.x, this.y, this.x + this.w, this.y + this.h, -16777216);
            }

            int i = 14737632;
            int j = this.cursorPosition - this.lineScrollOffset;
            int k = this.selectionEnd - this.lineScrollOffset;
            String s = this.renderer.trimStringToWidth(this.text.substring(this.lineScrollOffset), w, false);
            boolean flag = j >= 0 && j <= s.length();
            boolean flag1 = true; //this.isFocused() && this.cursorCounter / 6 % 2 == 0 && flag;
            float i1 = y;
            float j1 = x;
            if (k > s.length()) {
                k = s.length();
            }

            if (!s.isEmpty()) {
                String s1 = flag ? s.substring(0, j) : s;
                j1 = this.renderer.drawString(s1, x, y, i, (int) (alpha * 0xff), 0);
            }

            boolean flag2 = this.cursorPosition < this.text.length() || this.text.length() >= maxStringLength;
            float k1 = j1;
            if (!flag) {
                k1 = j > 0 ? (int) (x + this.w) : x;
            } else if (flag2) {
                k1 = j1 - 1;
                --j1;
            }

            if (!s.isEmpty() && flag && j < s.length()) {
                //this.renderer.drawString(s.substring(j), j1, i1, i, (int) (alpha * 0xff), 0);
            }

            if (flag1) {
                *//*if (flag2) {
                    AbstractGui.fill(k1, i1 - 1, k1 + 1, i1 + 1 + 9, -3092272);
                } else {
                    this.fontRenderer.drawStringWithShadow("_", (float)k1, (float)i1, i);
                }*//*
            }

            *//*if (k != j) {
                int l1 = l + this.fontRenderer.getStringWidth(s.substring(0, k));
                this.drawSelectionBox(k1, i1 - 1, l1 - 1, i1 + 1 + 9);
            }*//*
        }
    }

    *//**
     * Adds the given text after the cursor, or replaces the currently selected text if there is a selection.
     *//*
    public void writeText(String textToWrite) {
        String result = "";
        String filter = SharedConstants.filterAllowedCharacters(textToWrite);
        int i = Math.min(this.cursorPosition, this.selectionEnd);
        int j = Math.max(this.cursorPosition, this.selectionEnd);
        int canWriteCount = this.maxStringLength - this.text.length() - (i - j);

        if (!this.text.isEmpty()) {
            result = result + this.text.substring(0, i); // write text that before cursor and without selected
        }

        int l;
        if (canWriteCount < filter.length()) {
            result = result + filter.substring(0, canWriteCount); // ignore excess part
            l = canWriteCount;
        } else {
            result = result + filter;
            l = filter.length();
        }

        if (!this.text.isEmpty() && j < this.text.length()) { // write text that after cursor
            result = result + this.text.substring(j);
        }
        
        if (this.filter.test(result)) { // if result is legal
            this.text = result;
            setCursorPos(i + l);
            setSelectionPos(cursorPosition);
        }
    }

    *//**
     * Sets the position of the selection anchor (the selection anchor and the cursor position mark the edges of the
     * selection). If the anchor is set beyond the bounds of the current text, it will be put back inside.
     *//*
    public void setSelectionPos(int position) {
        int i = this.text.length();
        this.selectionEnd = MathHelper.clamp(position, 0, i);
        if (this.renderer != null) {
            if (this.lineScrollOffset > i) {
                this.lineScrollOffset = i;
            }

            String s = this.renderer.trimStringToWidth(this.text.substring(this.lineScrollOffset), w, false);
            int k = s.length() + this.lineScrollOffset;
            if (this.selectionEnd == this.lineScrollOffset) {
                this.lineScrollOffset -= this.renderer.trimStringToWidth(this.text, w, true).length();
            }

            if (this.selectionEnd > k) {
                this.lineScrollOffset += this.selectionEnd - k;
            } else if (this.selectionEnd <= this.lineScrollOffset) {
                this.lineScrollOffset -= this.lineScrollOffset - this.selectionEnd;
            }

            this.lineScrollOffset = MathHelper.clamp(this.lineScrollOffset, 0, i);
        }

    }

    public void setCursorPos(int pos) {
        this.cursorPosition = MathHelper.clamp(pos, 0, this.text.length());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if(!enabled || !visible) {
            focused = false;
            return false;
        }
        if(canLoseFocus) {
            if (focused != mouseHovered) {
                onFocusChanged(mouseHovered);
            }
            focused = mouseHovered;
        }
        return mouseHovered;
    }

    @Override
    public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
        return false;
    }

    @Override
    public boolean charTyped(char c, int i) {
        if(!(enabled && focused && visible)) {
            return false;
        } else if (SharedConstants.isAllowedCharacter(c)) {
            writeText(Character.toString(c));
            return true;
        }
        return false;
    }*/
}
