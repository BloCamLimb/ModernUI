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
import icyllis.modernui.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiContainerEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

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
 * @see net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor
 */
@OnlyIn(Dist.CLIENT)
final class MenuScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements MuiScreen {

    private final UIManager host;

    MenuScreen(@Nonnull T menu, Inventory inventory, UIManager window) {
        super(menu, inventory, TextComponent.EMPTY);
        host = window;
    }

    @Override
    public void init(@Nonnull Minecraft minecraft, int width, int height) {
        this.minecraft = minecraft;
        this.width = width;
        this.height = height;
        init();
        host.start(this);
        MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.InitGuiEvent.Post(this, buttons, this::widget, this::widget));
    }

    @Override
    public void resize(@Nonnull Minecraft minecraft, int width, int height) {
        this.width = width;
        this.height = height;
        init();
        host.resize();
        MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.InitGuiEvent.Post(this, buttons, this::widget, this::widget));

        /*ModernUI.LOGGER.debug("Scaled: {}x{} Framebuffer: {}x{} Window: {}x{}", width, height, minecraft.getMainWindow().getFramebufferWidth(),
                minecraft.getMainWindow().getFramebufferHeight(), minecraft.getMainWindow().getWidth(), minecraft.getMainWindow().getHeight());*/
    }

    private void widget(Widget widget) {
        if (widget != null) {
            ModernUI.LOGGER.warn(UIManager.MARKER, "Usage of {} is deprecated in Modern UI", widget);
        }
    }

    @Override
    public void render(@Nonnull PoseStack poseStack, int mouseX, int mouseY, float deltaTick) {
        //TODO configurable bg
        renderBackground(poseStack);
        MinecraftForge.EVENT_BUS.post(new GuiContainerEvent.DrawBackground(this, poseStack, mouseX, mouseY));
        host.render();
    }

    @Override
    protected void renderBg(@Nonnull PoseStack poseStack, float deltaTick, int x, int y) {
    }

    @Override
    public void removed() {
        super.removed();
        host.finish();
    }

    // IMPL - GuiEventListener

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        host.onCursorPos();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        host.onMouseButton();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        host.onMouseButton();
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
        /*if (host.screenKeyDown(keyCode, scanCode, modifiers)) {
            return true;
        } else {
            InputConstants.Key mouseKey = InputConstants.getKey(keyCode, scanCode);
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || Objects.requireNonNull(this.minecraft).options.keyInventory.isActiveAndMatches(mouseKey)) {
                if (host.onBackPressed()) {
                    return true;
                }
                Objects.requireNonNull(Objects.requireNonNull(this.minecraft).player).closeContainer();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                boolean searchNext = !hasShiftDown();
                if (!host.sChangeKeyboard(searchNext)) {
                    return host.sChangeKeyboard(searchNext);
                }
                return true;
            }

            if (this.checkHotbarKeyPressed(keyCode, scanCode))
                return true;
            if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
                if (this.minecraft.options.keyPickItem.isActiveAndMatches(mouseKey)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 0, ClickType.CLONE);
                    return true;
                } else if (this.minecraft.options.keyDrop.isActiveAndMatches(mouseKey)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, hasControlDown() ? 1 : 0, ClickType.THROW);
                    return true;
                }
            } else return this.minecraft.options.keyDrop.isActiveAndMatches(mouseKey);
        }*/
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean charTyped(char ch, int modifiers) {
        return host.charTyped(ch);
    }
}
