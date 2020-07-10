/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.impl.chat;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public final class GuiChat extends Screen {

    private ChatInputBox inputBox;
    private EmojiTab emojiTab;

    public GuiChat() {
        super(new StringTextComponent("Chat"));
    }

    @Override
    public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        inputBox.draw();
        emojiTab.draw(mouseX, mouseY);
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
        emojiTab = new EmojiTab(inputBox);
        emojiTab.resize(width, height);
        inputBox.setLineChanged(emojiTab::setDoubleLine);
    }

    @Override
    public void resize(@Nonnull Minecraft mc, int width, int height) {
        this.width = width;
        this.height = height;
        inputBox.resize(width, height);
        emojiTab.resize(width, height);
    }

    @Override
    public void removed() {
        Minecraft.getInstance().keyboardListener.enableRepeatEvents(false);
    }

    @Override
    public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
        return super.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
    }

    @Override
    public void mouseMoved(double xPos, double yPos) {
        emojiTab.mouseMoved(xPos, yPos);
    }

    @Override
    public boolean mouseClicked(double p_mouseClicked_1_, double p_mouseClicked_3_, int p_mouseClicked_5_) {
        if(emojiTab.mouseClicked(p_mouseClicked_1_, p_mouseClicked_3_, p_mouseClicked_5_)) {
            return true;
        } else {
            return super.mouseClicked(p_mouseClicked_1_, p_mouseClicked_3_, p_mouseClicked_5_);
        }
    }

    @Override
    public boolean mouseScrolled(double p_mouseScrolled_1_, double p_mouseScrolled_3_, double p_mouseScrolled_5_) {
        return emojiTab.mouseScrolled(p_mouseScrolled_1_, p_mouseScrolled_3_, p_mouseScrolled_5_);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

}
