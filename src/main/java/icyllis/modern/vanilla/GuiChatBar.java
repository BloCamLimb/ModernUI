package icyllis.modern.vanilla;

import icyllis.modern.ui.button.ChatInputBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
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
        Minecraft.getInstance().keyboardListener.enableRepeatEvents(true);
        inputBox = new ChatInputBox();
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
    public void removed() {
        Minecraft.getInstance().keyboardListener.enableRepeatEvents(false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
