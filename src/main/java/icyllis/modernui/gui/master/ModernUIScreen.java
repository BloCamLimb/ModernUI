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

import com.google.common.collect.Lists;
import icyllis.modernui.api.global.IModuleFactory;
import icyllis.modernui.system.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHelper;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ModernUIScreen extends Screen implements IMasterScreen {

    static final StringTextComponent EMPTY_TITLE = new StringTextComponent("");

    private GlobalModuleManager manager = GlobalModuleManager.INSTANCE;

    private boolean hasPopup = false;

    public ModernUIScreen(Consumer<IModuleFactory> factory) {
        super(EMPTY_TITLE);
        factory.accept(manager);
    }

    @Nonnull
    @Override
    public List<? extends IGuiEventListener> children() {
        return hasPopup ? manager.popupListener : children;
    }

    @Override
    protected void init() {
        manager.build(this, width, height);
    }

    @Override
    public void resize(@Nonnull Minecraft minecraft, int width, int height) {
        manager.resize(width, height);
    }

    @Override
    public void mouseMoved(double xPos, double p_212927_3_) {
        children().forEach(e -> e.mouseMoved(xPos, p_212927_3_));
    }

    @Override
    public void onClose() {
        if (hasPopup) {
            manager.closePopup();
        } else {
            super.onClose();
        }
    }

    @Override
    public void addEventListener(IGuiEventListener eventListener) {
        children.add(eventListener);
    }

    @Override
    public void setHasPopup(boolean bool) {
        if (bool) {
            children().forEach(e -> e.mouseMoved(0, 0));
        }
        hasPopup = bool;
        if (!bool) {
            refreshCursor();
        }
    }

    @Override
    public void refreshCursor() {
        MouseHelper mouseHelper = Minecraft.getInstance().mouseHelper;
        double scale = Minecraft.getInstance().getMainWindow().getGuiScaleFactor();
        children().forEach(e -> e.mouseMoved(mouseHelper.getMouseX() / scale, mouseHelper.getMouseY() / scale));
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        manager.draw();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        manager.clear();
    }

}
