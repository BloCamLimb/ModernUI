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

import icyllis.modern.system.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public final class GuiChat extends Screen {

    private int controlTimer = 0;
    private ChatInputBox inputBox;
    private EmojiTab emojiTab = new EmojiTab();

    public GuiChat() {
        super(new StringTextComponent("Chat"));
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        inputBox.draw();
        emojiTab.draw();
    }

    @Override
    public void tick() {
        inputBox.tick();
        if(controlTimer > 0) {
            --controlTimer;
        }
    }

    @Override
    protected void init() {
        Minecraft.getInstance().keyboardListener.enableRepeatEvents(true);
        inputBox = new ChatInputBox();
        inputBox.resize(width, height);
        children.add(inputBox);
        setFocusedDefault(inputBox);
        emojiTab.open();
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
    public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
        if(Screen.hasControlDown()) {
            if(Screen.hasControlDown() && controlTimer > 0) {
                ModernUI.LOGGER.info("double con");
            } else {
                controlTimer = 6;
            }
        }
        return super.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

}
