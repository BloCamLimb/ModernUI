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

package icyllis.modernui.view;

import com.mojang.blaze3d.matrix.MatrixStack;
import icyllis.modernui.graphics.BlurHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;

/**
 * This class serves as a bridge to receive events from system.
 * All vanilla methods are completely deprecated by Modern UI.
 */
@OnlyIn(Dist.CLIENT)
public final class MuiScreen extends Screen {

    private final UIManager manager = UIManager.getInstance();

    MuiScreen() {
        super(StringTextComponent.EMPTY);
    }

    @Override
    public void init(@Nonnull Minecraft minecraft, int width, int height) {
        manager.prepareWindows(this, width, height);
        BlurHandler.INSTANCE.forceBlur();
    }

    @Override
    public void resize(@Nonnull Minecraft minecraft, int width, int height) {
        manager.prepareWindows(this, width, height);
    }

    @Override
    public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        manager.draw();
    }

    @Override
    public void onClose() {
        manager.recycleWindows();
    }

    @Override
    public boolean isPauseScreen() {
        //TODO configurable
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        manager.screenMouseMove(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        return manager.screenMouseDown(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        return manager.screenMouseUp(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        return manager.screenMouseDrag(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return manager.screenMouseScroll(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (manager.screenKeyDown(keyCode, scanCode, modifiers)) {
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE && shouldCloseOnEsc()) {
            if (manager.sBack()) {
                return true;
            }
            manager.closeGui();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_TAB) {
            boolean searchNext = !hasShiftDown();
            if (!manager.sChangeKeyboard(searchNext)) {
                return manager.sChangeKeyboard(searchNext);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return manager.screenKeyUp(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return manager.sCharTyped(codePoint, modifiers);
    }
}
