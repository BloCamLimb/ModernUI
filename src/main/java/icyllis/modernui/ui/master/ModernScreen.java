/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.master;

import icyllis.modernui.graphics.BlurHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public final class ModernScreen extends Screen {

    private final UIManager manager = UIManager.INSTANCE;

    ModernScreen() {
        super(new StringTextComponent(""));
    }

    @Override
    public void init(Minecraft minecraft, int width, int height) {
        manager.sInit(this, width, height);
        BlurHandler.INSTANCE.forceBlur();
    }

    @Override
    public void resize(@Nonnull Minecraft minecraft, int width, int height) {
        manager.sResize(width, height);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        manager.sDraw();
    }

    @Override
    public void removed() {
        manager.sDestroy();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public final void mouseMoved(double mouseX, double mouseY) {
        manager.sMouseMoved((int) mouseX, (int) mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        return manager.sMouseClicked((int) mouseX, (int) mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        return manager.sMouseReleased((int) mouseX, (int) mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        return manager.sMouseDragged((int) mouseX, (int) mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return manager.sMouseScrolled((int) mouseX, (int) mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (manager.sKeyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE && shouldCloseOnEsc()) {
            if (manager.sBack()) {
                return true;
            }
            manager.closeGuiScreen();
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
        return manager.sKeyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return manager.sCharTyped(codePoint, modifiers);
    }

    @Nonnull
    @Override
    public String toString() {
        if (manager.getMainView() != null) {
            return getClass().getSimpleName() + "-" + manager.getMainView().getClass().getSimpleName() + "(" + hashCode() + ")";
        }
        return getClass().getSimpleName() + "(" + hashCode() + ")";
    }
}
