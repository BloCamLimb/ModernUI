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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * ContainerScreen holds a container menu for item stack interaction and
 * network communication. As a feature of Minecraft, GUI initiated by the
 * server will always be this class. It behaves like JEI checking if
 * instanceof {@link AbstractContainerScreen}. Therefore, this class serves
 * as a marker, the complexity of business logic is not reflected in this
 * class, we don't need anything in the super class.
 *
 * @param <T> the type of container menu
 * @see SimpleScreen
 */
@OnlyIn(Dist.CLIENT)
final class MenuScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements MuiScreen {

    MenuScreen(@Nonnull T menu, Inventory inventory) {
        super(menu, inventory, TextComponent.EMPTY);
    }

    /*@Override
    public void init(@Nonnull Minecraft minecraft, int width, int height) {
        this.minecraft = minecraft;
        this.width = width;
        this.height = height;
        init();
        MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.InitGuiEvent.Post(this, buttons, this::widget, this::widget));
    }*/

    @Override
    protected void init() {
        super.init();
        UIManager.sInstance.initScreen(this);
    }

    @Override
    public void resize(@Nonnull Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        UIManager.sInstance.resize();
        //MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.InitGuiEvent.Post(this, buttons, this::widget,
        // this::widget));

        /*ModernUI.LOGGER.debug("Scaled: {}x{} Framebuffer: {}x{} Window: {}x{}", width, height, minecraft
        .getMainWindow().getFramebufferWidth(),
                minecraft.getMainWindow().getFramebufferHeight(), minecraft.getMainWindow().getWidth(), minecraft
                .getMainWindow().getHeight());*/
    }

    @Override
    public void render(@Nonnull PoseStack poseStack, int mouseX, int mouseY, float deltaTick) {
        if (UIManager.sInstance.mCallback == null || UIManager.sInstance.mCallback.hasDefaultBackground()) {
            renderBackground(poseStack);
        }
        UIManager.sInstance.render();
    }

    @Override
    protected void renderBg(@Nonnull PoseStack poseStack, float deltaTick, int x, int y) {
    }

    @Override
    public void removed() {
        super.removed();
        UIManager.sInstance.removed();
    }

    // IMPL - GuiEventListener

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        UIManager.sInstance.onHoverMove(true);
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
        return UIManager.sInstance.charTyped(ch);
    }
}
