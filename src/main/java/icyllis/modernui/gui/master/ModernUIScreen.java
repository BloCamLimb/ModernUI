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

import icyllis.modernui.api.global.IModuleFactory;
import icyllis.modernui.system.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHelper;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ModernUIScreen extends Screen implements IMasterScreen {

    static final StringTextComponent EMPTY_TITLE = new StringTextComponent("");

    private GlobalModuleManager manager = GlobalModuleManager.INSTANCE;

    private boolean hasPopup = false;

    public ModernUIScreen(Consumer<IModuleFactory> factory) {
        this(EMPTY_TITLE, factory);
    }

    public ModernUIScreen(ITextComponent title, Consumer<IModuleFactory> factory) {
        super(title);
        factory.accept(manager);
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
    public void render(int mouseX, int mouseY, float partialTicks) {
        manager.draw();
    }

    @Override
    public void removed() {
        manager.clear();
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
    public void setHasPopup(boolean hasPopup) {
        this.hasPopup = hasPopup;
        if (hasPopup) {
            children.forEach(e -> e.mouseMoved(0, 0));
        } else {
            refreshCursor();
        }
    }

    @Override
    public void refreshCursor() {
        MouseHelper mouseHelper = Minecraft.getInstance().mouseHelper;
        double scale = Minecraft.getInstance().getMainWindow().getGuiScaleFactor();
        double x = mouseHelper.getMouseX() / scale;
        double y = mouseHelper.getMouseY() / scale;
        if (hasPopup) {
            manager.popup.mouseMoved(x, y);
        } else {
            children.forEach(e -> e.mouseMoved(x, y));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (hasPopup) {
            manager.popup.mouseMoved(mouseX, mouseY);
        } else {
            children.forEach(e -> e.mouseMoved(mouseX, mouseY));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (hasPopup) {
            return manager.popup.mouseClicked(mouseX, mouseY, mouseButton);
        } else {
            for (IGuiEventListener listener : children) {
                if (listener.mouseClicked(mouseX, mouseY, mouseButton)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (hasPopup) {
            return manager.popup.mouseReleased(mouseX, mouseY, mouseButton);
        } else {
            for (IGuiEventListener listener : children) {
                if (listener.mouseReleased(mouseX, mouseY, mouseButton)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double RDX, double RDY) {
        if (hasPopup) {
            return manager.popup.mouseDragged(mouseX, mouseY, mouseButton, RDX, RDY);
        } else {
            for (IGuiEventListener listener : children) {
                if (listener.mouseDragged(mouseX, mouseY, mouseButton, RDX, RDY)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double shift) {
        if (hasPopup) {
            return manager.popup.mouseScrolled(mouseX, mouseY, shift);
        } else {
            for (IGuiEventListener listener : children) {
                if (listener.mouseScrolled(mouseX, mouseY, shift)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifier) {
        if (key == GLFW.GLFW_KEY_ESCAPE && shouldCloseOnEsc()) {
            onClose();
            return true;
        }
        if (hasPopup) {
            return manager.popup.keyPressed(key, scanCode, modifier);
        } else {
            for (IGuiEventListener listener : children) {
                if (listener.keyPressed(key, scanCode, modifier)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifier) {
        if (hasPopup) {
            return manager.popup.keyReleased(key, scanCode, modifier);
        } else {
            for (IGuiEventListener listener : children) {
                if (listener.keyReleased(key, scanCode, modifier)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char charCode, int modifier) {
        if (hasPopup) {
            return manager.popup.charTyped(charCode, modifier);
        } else {
            for (IGuiEventListener listener : children) {
                if (listener.charTyped(charCode, modifier)) {
                    return true;
                }
            }
        }
        return false;
    }
}
