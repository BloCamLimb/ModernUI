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

package icyllis.modernui.view;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import icyllis.modernui.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.Objects;

//TODO rewrite vanilla code, make item slots can be rendered with alpha
// And render layer for tooltips etc

/**
 * ContainerScreen can hold a container menu including item stack interaction and
 * network communication. Actually we don't know the menu type, so generic doesn't matter.
 * This is required because most of mods (like JEI) check if instanceof {@link AbstractContainerScreen}
 * rather than {@link net.minecraft.client.gui.screens.inventory.MenuAccess}, however, we don't need
 * anything in the super class.
 *
 * @param <T> container menu type
 * @see MMainScreen
 * @see net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor
 */
@OnlyIn(Dist.CLIENT)
final class MMenuScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements IMuiScreen {

    private final UIManager master;

    MMenuScreen(@Nonnull T menu, Inventory inventory, UIManager window) {
        super(menu, inventory, TextComponent.EMPTY);
        master = window;
    }

    @Override
    public void init(@Nonnull Minecraft minecraft, int width, int height) {
        //TODO remove super.init()
        super.init(minecraft, width, height);
        master.start(this, width, height);
    }

    @Override
    public void resize(@Nonnull Minecraft minecraft, int width, int height) {
        // these are two public fields
        this.width = width;
        this.height = height;

        master.resize(width, height);

        // compatibility with Forge mods, like JEI
        if (!MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.InitGuiEvent.Pre(this, buttons, this::logWidget, this::logWidget))) {
            MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.InitGuiEvent.Post(this, buttons, this::logWidget, this::logWidget));
        }

        /*ModernUI.LOGGER.debug("Scaled: {}x{} Framebuffer: {}x{} Window: {}x{}", width, height, minecraft.getMainWindow().getFramebufferWidth(),
                minecraft.getMainWindow().getFramebufferHeight(), minecraft.getMainWindow().getWidth(), minecraft.getMainWindow().getHeight());*/
    }

    private void logWidget(Widget widget) {
        if (widget != null) {
            ModernUI.LOGGER.warn(UIManager.MARKER, "Usage of {} is deprecated in Modern UI", widget);
        }
    }

    @Override
    public void render(@Nonnull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        master.render();
    }

    @Override
    protected void renderBg(@Nonnull PoseStack matrixStack, float partialTicks, int x, int y) {
        renderBackground(matrixStack);
    }

    @Override
    protected void renderLabels(@Nonnull PoseStack matrixStack, int x, int y) {

    }

    @Override
    public void onClose() {
        super.onClose();
        master.stop();
    }

    // IMPL - IGuiEventListener

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        master.onCursorEvent(mouseX, mouseY);
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
        return master.onScrollEvent();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (master.screenKeyDown(keyCode, scanCode, modifiers)) {
            return true;
        } else {
            InputConstants.Key mouseKey = InputConstants.getKey(keyCode, scanCode);
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || Objects.requireNonNull(this.minecraft).options.keyInventory.isActiveAndMatches(mouseKey)) {
                if (master.onBackPressed()) {
                    return true;
                }
                Objects.requireNonNull(Objects.requireNonNull(this.minecraft).player).closeContainer();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                boolean searchNext = !hasShiftDown();
                if (!master.sChangeKeyboard(searchNext)) {
                    return master.sChangeKeyboard(searchNext);
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
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return master.screenKeyUp(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return master.sCharTyped(codePoint, modifiers);
    }
}
