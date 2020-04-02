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

import icyllis.modernui.gui.animation.IAnimation;
import icyllis.modernui.api.global.IModuleFactory;
import icyllis.modernui.api.manager.IModuleManager;
import icyllis.modernui.gui.module.IGuiModule;
import icyllis.modernui.shader.BlurHandler;
import icyllis.modernui.system.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketBuffer;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

/**
 * Manager current GUI (screen / modules) (singleton)
 */
public enum GlobalModuleManager implements IModuleFactory, IModuleManager, IGuiEventListener {
    INSTANCE;

    private final Marker MARKER = MarkerManager.getMarker("SCREEN");

    private boolean initialized = false;

    /* For container gui, the packet from server */
    private PacketBuffer extraData;

    private List<ModuleBuilder> builders = new ArrayList<>();

    private List<IAnimation> animations = new ArrayList<>();

    private List<TickEvent> tickEvents = new ArrayList<>();

    @Nullable
    public IGuiModule popup;

    private int ticks = 0;

    private float animationTime = 0;

    private int width, height;

    private double mouseX, mouseY;

    public void init(int width, int height) {
        this.width = width;
        this.height = height;
        // called on resource reload, so re-blur game renderer, and prevent from re-switching to 0
        if (!initialized) {
            this.switchModule(0);
            initialized = true;
        }
        BlurHandler.INSTANCE.blur(true);
    }

    public void setExtraData(PacketBuffer extraData) {
        this.extraData = extraData;
    }

    @Override
    public void switchModule(int newID) {
        builders.stream().filter(m -> m.test(newID)).forEach(ModuleBuilder::build);
        builders.forEach(e -> e.onModuleChanged(newID));
        builders.stream().filter(m -> !m.test(newID)).forEach(ModuleBuilder::removed);
        builders.forEach(e -> e.resize(width, height));
    }

    @Override
    public void openPopup(IGuiModule popup, boolean resetMouse) {
        if (this.popup != null) {
            ModernUI.LOGGER.warn(MARKER, "#openPopup() shouldn't be called when there's already a popup, the previous one has been overwritten");
        }
        if (resetMouse) {
            this.mouseMoved(-1, -1);
        }
        this.popup = popup;
        this.popup.resize(width, height);
        this.refreshMouse();
    }

    @Override
    public void closePopup() {
        if (popup != null) {
            popup = null;
            this.refreshMouse();
        } else {
            ModernUI.LOGGER.info(MARKER, "#closePopup() shouldn't be called when there's no popup, this is not an error");
        }
    }

    @Override
    public void addAnimation(IAnimation animation) {
        animations.add(animation);
    }

    @Override
    public void addTickEvent(TickEvent event) {
        tickEvents.add(event);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        if (popup != null) {
            popup.mouseMoved(mouseX, mouseY);
        } else {
            builders.forEach(e -> e.mouseMoved(mouseX, mouseY));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (popup != null) {
            return popup.mouseClicked(mouseX, mouseY, mouseButton);
        } else {
            for (IGuiEventListener listener : builders) {
                if (listener.mouseClicked(mouseX, mouseY, mouseButton)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (popup != null) {
            return popup.mouseReleased(mouseX, mouseY, mouseButton);
        } else {
            for (IGuiEventListener listener : builders) {
                if (listener.mouseReleased(mouseX, mouseY, mouseButton)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        if (popup != null) {
            return popup.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY);
        } else {
            for (IGuiEventListener listener : builders) {
                if (listener.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (popup != null) {
            return popup.mouseScrolled(mouseX, mouseY, delta);
        } else {
            for (IGuiEventListener listener : builders) {
                if (listener.mouseScrolled(mouseX, mouseY, delta)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            boolean searchNext = !Screen.hasShiftDown();
            if (!this.changeFocus(searchNext)) {
                return this.changeFocus(searchNext);
            }
            return true;
        } else if (popup != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closePopup();
                return true;
            } else {
                return popup.keyPressed(keyCode, scanCode, modifiers);
            }
        } else {
            for (IGuiEventListener listener : builders) {
                if (listener.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (popup != null) {
            return popup.keyReleased(keyCode, scanCode, modifiers);
        } else {
            for (IGuiEventListener listener : builders) {
                if (listener.keyReleased(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char ch, int modifiers) {
        if (popup != null) {
            return popup.charTyped(ch, modifiers);
        } else {
            for (IGuiEventListener listener : builders) {
                if (listener.charTyped(ch, modifiers)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean changeFocus(boolean searchNext) {
        //TODO change focus
        return false;
    }

    @Override
    public void refreshMouse() {
        mouseMoved(mouseX, mouseY);
    }

    public void draw() {
        animations.forEach(e -> e.update(animationTime));
        builders.forEach(e -> e.draw(animationTime));
        if (popup != null) {
            popup.draw(animationTime);
        }
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        builders.forEach(e -> e.resize(width, height));
        if (popup != null)
            popup.resize(width, height);
        Minecraft minecraft = Minecraft.getInstance();
        double scale = minecraft.getMainWindow().getGuiScaleFactor();
        this.mouseX = minecraft.mouseHelper.getMouseX() / scale;
        this.mouseY = minecraft.mouseHelper.getMouseY() / scale;
        refreshMouse();
    }

    public void clear() {
        builders.clear();
        animations.clear();
        popup = null;
        extraData = null;
        initialized = false;
    }

    /* return false will not close this gui/screen */
    public boolean onGuiClosed() {
        if (popup != null) {
            closePopup();
            return false;
        }
        return true;
    }

    @Override
    public void addModule(IntPredicate availability, Supplier<IGuiModule> module) {
        ModuleBuilder builder = new ModuleBuilder(availability, module);
        builders.add(builder);
    }

    public void clientTick() {
        ticks++;
        builders.forEach(e -> e.tick(ticks));
        if (popup != null)
            popup.tick(ticks);
        tickEvents.removeIf(TickEvent::shouldRemove);
        tickEvents.forEach(e -> e.tick(ticks));
    }

    public void renderTick(float partialTick) {
        animationTime = ticks + partialTick;
        animations.removeIf(IAnimation::shouldRemove);
    }

    public void resetTicks() {
        ticks = 0;
        animationTime = 0;
    }

    @Override
    public float getAnimationTime() {
        return animationTime;
    }

    @Override
    public int getTicks() {
        return ticks;
    }

    @Override
    public int getWindowHeight() {
        return height;
    }

    @Override
    public PacketBuffer getExtraData() {
        return extraData;
    }
}
