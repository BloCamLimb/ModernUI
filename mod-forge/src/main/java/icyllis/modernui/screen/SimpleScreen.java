/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import icyllis.modernui.graphics.BlurHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;

/**
 * Represents the GUI screen that receives events from Minecraft.
 * All vanilla methods are completely taken over by Modern UI.
 *
 * @see MenuScreen
 */
@OnlyIn(Dist.CLIENT)
final class SimpleScreen extends Screen implements MuiScreen {

    private final UIManager host;

    SimpleScreen(UIManager window) {
        super(TextComponent.EMPTY);
        host = window;
    }

    @Override
    public void init(@Nonnull Minecraft minecraft, int width, int height) {
        this.minecraft = minecraft;
        this.width = width;
        this.height = height;
        host.start(this, width, height);
        BlurHandler.INSTANCE.forceBlur();
    }

    @Override
    public void resize(@Nonnull Minecraft minecraft, int width, int height) {
        this.width = width;
        this.height = height;
        host.resize(width, height);
    }

    @Override
    public void render(@Nonnull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        host.render();
    }

    @Override
    public void onClose() {
        super.onClose();
        host.stop();
    }

    //TODO configurable
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // IMPL - IGuiEventListener

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        host.onCursorEvent(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        // Pass the event
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        // Pass the event
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        // Consume the event but do nothing
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return host.onScrollEvent();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (host.screenKeyDown(keyCode, scanCode, modifiers)) {
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE && shouldCloseOnEsc()) {
            if (host.onBackPressed()) {
                return true;
            }
            host.closeGUI();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_TAB) {
            boolean searchNext = !hasShiftDown();
            if (!host.sChangeKeyboard(searchNext)) {
                return host.sChangeKeyboard(searchNext);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return host.screenKeyUp(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return host.sCharTyped(codePoint, modifiers);
    }
}
