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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.Objects;

//TODO rewrite vanilla code, make item slots can be rendered with alpha
// And render layer for tooltips etc

/**
 * This is required because most of mods check if instanceof {@link ContainerScreen} rather than {@link IHasContainer}.
 * ContainerScreen can hold a menu including item stack interaction and network communication.
 *
 * @param <T> menu type
 * @see ScreenManager.IScreenFactory
 */
@OnlyIn(Dist.CLIENT)
public final class MuiMenuScreen<T extends Container> extends ContainerScreen<T> {

    private final UIManager manager = UIManager.getInstance();

    MuiMenuScreen(@Nonnull T menu, PlayerInventory inventory, ITextComponent title) {
        super(menu, inventory, title);
    }

    @Override
    public void init(@NotNull Minecraft minecraft, int width, int height) {
        //TODO remove super.init()
        super.init(minecraft, width, height);
        manager.prepareWindows(this, width, height);
    }

    @Override
    public void resize(@Nonnull Minecraft minecraft, int width, int height) {
        this.width = width;
        this.height = height;
        manager.prepareWindows(this, width, height);
    }

    @Override
    public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        manager.draw();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(@Nonnull MatrixStack matrixStack, float partialTicks, int x, int y) {
        renderBackground(matrixStack);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(@Nonnull MatrixStack matrixStack, int x, int y) {

    }

    @Override
    public void onClose() {
        super.onClose();
        manager.recycleWindows();
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        manager.eventDispatcher.onCursorPosEvent(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        return manager.screenMouseDown(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
        return manager.screenMouseUp(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        super.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY);
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
        } else {
            InputMappings.Input mouseKey = InputMappings.getInputByCode(keyCode, scanCode);
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || Objects.requireNonNull(this.minecraft).gameSettings.keyBindInventory.isActiveAndMatches(mouseKey)) {
                if (manager.sBack()) {
                    return true;
                }
                Objects.requireNonNull(Objects.requireNonNull(this.minecraft).player).closeScreen();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                boolean searchNext = !hasShiftDown();
                if (!manager.sChangeKeyboard(searchNext)) {
                    return manager.sChangeKeyboard(searchNext);
                }
                return true;
            }

            if (this.itemStackMoved(keyCode, scanCode))
                return true;
            if (this.hoveredSlot != null && this.hoveredSlot.getHasStack()) {
                if (this.minecraft.gameSettings.keyBindPickBlock.isActiveAndMatches(mouseKey)) {
                    this.handleMouseClick(this.hoveredSlot, this.hoveredSlot.slotNumber, 0, ClickType.CLONE);
                    return true;
                } else if (this.minecraft.gameSettings.keyBindDrop.isActiveAndMatches(mouseKey)) {
                    this.handleMouseClick(this.hoveredSlot, this.hoveredSlot.slotNumber, hasControlDown() ? 1 : 0, ClickType.THROW);
                    return true;
                }
            } else return this.minecraft.gameSettings.keyBindDrop.isActiveAndMatches(mouseKey);
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
