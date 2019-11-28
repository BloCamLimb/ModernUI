package icyllis.modern.vanilla;

import icyllis.modern.ui.button.ChatInputBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

import javax.annotation.Nonnull;

public class GuiChatBar extends Screen {

    private ChatInputBox inputBox;

    public GuiChatBar() {
        super(new StringTextComponent("Chat Bar"));
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        inputBox.draw();
    }

    @Override
    public void tick() {
        inputBox.tick();
    }

    @Override
    protected void init() {
        inputBox = new ChatInputBox(2, 0, 0, 12);
        inputBox.resize(width, height);
        children.add(inputBox);
        setFocusedDefault(inputBox);
    }

    @Override
    public void resize(@Nonnull Minecraft mc, int width, int height) {
        this.width = width;
        this.height = height;
        inputBox.resize(width, height);
    }

    @Override
    public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
        return super.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
