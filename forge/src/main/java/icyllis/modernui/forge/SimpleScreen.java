/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * Represents the GUI screen that receives events from Minecraft.
 * All vanilla methods are completely taken over by Modern UI.
 *
 * @see MenuScreen
 */
@OnlyIn(Dist.CLIENT)
final class SimpleScreen extends Screen implements MuiScreen {

    SimpleScreen() {
        super(TextComponent.EMPTY);
    }

    /*@Override
    public void init(@Nonnull Minecraft minecraft, int width, int height) {
        this.minecraft = minecraft;
        this.width = width;
        this.height = height;
    }*/

    @Override
    protected void init() {
        super.init();
        UIManager.sInstance.initScreen(this);
        if (UIManager.sInstance.mCallback == null || UIManager.sInstance.mCallback.shouldBlurBackground()) {
            BlurHandler.INSTANCE.forceBlur();
        }
    }

    @Override
    public void resize(@Nonnull Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        UIManager.sInstance.resize();
    }

    @Override
    public void render(@Nonnull PoseStack poseStack, int mouseX, int mouseY, float deltaTick) {
        if (UIManager.sInstance.mCallback == null || UIManager.sInstance.mCallback.hasDefaultBackground()) {
            renderBackground(poseStack);
        }
        UIManager.sInstance.render();
    }

    @Override
    public void removed() {
        super.removed();
        UIManager.sInstance.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return UIManager.sInstance.mCallback == null || UIManager.sInstance.mCallback.isPauseScreen();
    }

    // IMPL - GuiEventListener

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        UIManager.sInstance.onCursorPos();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        UIManager.sInstance.onMouseButton();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        UIManager.sInstance.onMouseButton();
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean charTyped(char ch, int modifiers) {
        return UIManager.sInstance.charTyped(ch);
    }
}
