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
import icyllis.modernui.fragment.Fragment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents the GUI screen that receives events from Minecraft.
 * All vanilla methods are completely taken over by Modern UI.
 *
 * @see MenuScreen
 */
final class SimpleScreen extends Screen implements MuiScreen {

    private final UIManager mHost;
    private final Fragment mFragment;
    @Nullable
    private final UICallback mCallback;

    SimpleScreen(UIManager host, Fragment fragment, @Nullable UICallback callback) {
        super(CommonComponents.EMPTY);
        mHost = host;
        mFragment = fragment;
        mCallback = callback;
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
        mHost.initScreen(this);
        if (mCallback == null || mCallback.shouldBlurBackground()) {
            BlurHandler.INSTANCE.forceBlur();
        }
    }

    @Override
    public void resize(@Nonnull Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
    }

    @Override
    public void render(@Nonnull PoseStack poseStack, int mouseX, int mouseY, float deltaTick) {
        if (mCallback == null || mCallback.hasDefaultBackground()) {
            renderBackground(poseStack);
        }
        mHost.render();
    }

    @Override
    public void removed() {
        super.removed();
        mHost.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return mCallback == null || mCallback.isPauseScreen();
    }

    @Nonnull
    @Override
    public Fragment getFragment() {
        return mFragment;
    }

    @Nullable
    @Override
    public UICallback getCallback() {
        return mCallback;
    }

    // IMPL - GuiEventListener

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        mHost.onHoverMove(true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        return false;
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
        return mHost.onCharTyped(ch);
    }
}
