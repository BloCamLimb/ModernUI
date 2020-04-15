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

package icyllis.modernui.gui.master;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

//TODO Not working on with container gui now
// because I want to rewrite vanilla code, make item slots can be renderer with alpha

/**
 * This is required, because ALL(?) mods check if instanceof {@link ContainerScreen} rather than {@link IHasContainer}, like JEI. However,
 * Vanilla interface: IScreenFactory<T extends Container, U extends Screen & IHasContainer<T>>, see {@link ScreenManager.IScreenFactory}
 * Vanilla is Screen & IHasContainer<T>, not ContainerScreen, FUCK YOU, no one want to break the rules
 * @param <G> container
 */
@OnlyIn(Dist.CLIENT)
public final class ModernContainerScreen<G extends Container> extends ContainerScreen<G> implements IHasContainer<G> {

    private final GlobalModuleManager manager = GlobalModuleManager.INSTANCE;

    @SuppressWarnings("ConstantConditions")
    public ModernContainerScreen(ITextComponent title, G container) {
        super(container, Minecraft.getInstance().player.inventory, title);
    }

    @Override
    public void init(Minecraft minecraft, int width, int height) {
        super.init(minecraft, width, height);
        manager.init(width, height);
    }

    @Override
    public void resize(@Nonnull Minecraft minecraft, int width, int height) {
        manager.resize(width, height);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        manager.draw();
    }

    @Override
    public void removed() {
        manager.clear();
    }

    @Override
    public final void mouseMoved(double mouseX, double mouseY) {
        manager.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        return manager.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        return manager.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        return manager.mouseDragged(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return manager.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return manager.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return manager.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return manager.charTyped(codePoint, modifiers);
    }
}
