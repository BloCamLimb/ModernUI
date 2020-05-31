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

package icyllis.modernui.ui.master;

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
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.Objects;

//TODO rewrite vanilla code, make item slots can be renderer with alpha

/**
 * This is required, because most mods check if instanceof {@link ContainerScreen} rather than {@link IHasContainer}
 * But vanilla is IScreenFactory<T extends Container, U extends Screen & IHasContainer<T>>, see {@link ScreenManager.IScreenFactory}
 * @param <G> container
 */
@OnlyIn(Dist.CLIENT)
public final class ModernContainerScreen<G extends Container> extends ContainerScreen<G> implements IHasContainer<G> {

    private final UIManager manager = UIManager.INSTANCE;

    public ModernContainerScreen(@Nonnull G container, PlayerInventory inventory, ITextComponent title) {
        super(container, inventory, title);
    }

    @Override
    public void init(Minecraft minecraft, int width, int height) {
        super.init(minecraft, width, height);
        manager.init(this, width, height);
    }

    @Override
    public void resize(@Nonnull Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        manager.resize(width, height);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);
        manager.draw();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        renderBackground();
    }

    @Override
    public void removed() {
        super.removed();
        manager.clear();
    }

    @Override
    public final void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        manager.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        return manager.mouseClicked0(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
        return manager.mouseReleased0(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        super.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY);
        return manager.mouseDragged(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        super.mouseScrolled(mouseX, mouseY, delta);
        return manager.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (manager.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else {
            InputMappings.Input mouseKey = InputMappings.getInputByCode(keyCode, scanCode);
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || Objects.requireNonNull(this.minecraft).gameSettings.keyBindInventory.isActiveAndMatches(mouseKey)) {
                if (manager.onBack()) {
                    return true;
                }
                Objects.requireNonNull(Objects.requireNonNull(this.minecraft).player).closeScreen();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                boolean searchNext = !hasShiftDown();
                if (!manager.changeKeyboardListener(searchNext)) {
                    return manager.changeKeyboardListener(searchNext);
                }
                return true;
            }

            if (this.func_195363_d(keyCode, scanCode))
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
        super.keyReleased(keyCode, scanCode, modifiers);
        return manager.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        super.charTyped(codePoint, modifiers);
        return manager.charTyped(codePoint, modifiers);
    }

    @Nonnull
    @Override
    public String toString() {
        if (manager.getRootView() != null) {
            return getClass().getSimpleName() + "-" + manager.getRootView().getClass().getSimpleName() + "(" + hashCode() + ")";
        }
        return getClass().getSimpleName() + "(" + hashCode() + ")";
    }
}
