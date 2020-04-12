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
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.system.MouseTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Manage current GUI (or screen / modules), and post events to base module
 */
public enum GlobalModuleManager {
    INSTANCE;

    public static final Marker MARKER = MarkerManager.getMarker("SCREEN");

    private Minecraft minecraft = Minecraft.getInstance();

    /**
     * For container gui, the last packet from server
     */
    private PacketBuffer extraData;

    /**
     * Cached for init
     */
    @Nullable
    private Supplier<IModule> supplier;

    /**
     * The origin of all modules
     */
    private IModule root;

    @Nullable
    private IModule popup;

    private int width, height;

    private double mouseX, mouseY;

    private List<IAnimation> animations = new ArrayList<>();

    private List<DelayedRunnable> runnables = new ArrayList<>();

    private int ticks = 0;

    private float animationTime = 0;

    public void openGuiScreen(ITextComponent title, @Nonnull Supplier<IModule> root) {
        this.supplier = root;
        minecraft.displayGuiScreen(new ModernUIScreen(title));
    }

    public void closeGuiScreen() {
        minecraft.displayGuiScreen(null);
    }

    public void init(int width, int height) {
        if (supplier != null) {
            root = Objects.requireNonNull(supplier.get());
            supplier = null;
        }
        resize(width, height);
    }

    /**
     * Open a popup module, a special module
     * @param popup popup module
     * @param resetMouse true will post mouseMoved(-1, -1) to root module
     */
    public void openPopup(IModule popup, boolean resetMouse) {
        if (root == null) {
            ModernUI.LOGGER.fatal(MARKER, "#openPopup() shouldn't be called when there's NO gui open");
            return;
        }
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

    /**
     * Close current popup
     */
    public void closePopup() {
        if (popup != null) {
            popup = null;
            this.refreshMouse();
        }
    }

    public void addAnimation(IAnimation animation) {
        animations.add(animation);
    }

    public void scheduleRunnable(DelayedRunnable event) {
        runnables.add(event);
    }

    protected void mouseMoved(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        if (popup != null) {
            popup.mouseMoved(mouseX, mouseY);
        } else {
            root.mouseMoved(mouseX, mouseY);
        }
    }

    protected boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (popup != null) {
            return popup.mouseClicked(mouseX, mouseY, mouseButton);
        } else {
            return root.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    protected boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (popup != null) {
            return popup.mouseReleased(mouseX, mouseY, mouseButton);
        } else {
            return root.mouseReleased(mouseX, mouseY, mouseButton);
        }
    }

    protected boolean mouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (popup != null) {
            return popup.mouseDragged(mouseX, mouseY, deltaX, deltaY);
        } else {
            return root.mouseDragged(mouseX, mouseY, deltaX, deltaY);
        }
    }

    protected boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (popup != null) {
            return popup.mouseScrolled(mouseX, mouseY, amount);
        } else {
            return root.mouseScrolled(mouseX, mouseY, amount);
        }
    }

    protected boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (popup != null) {
            if (popup.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closePopup();
                return true;
            }
        } else {
            if (root.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (root.onBack()) {
                    return true;
                } else {
                    closeGuiScreen();
                }
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_TAB) {
                boolean searchNext = !Screen.hasShiftDown();
                if (!this.changeFocus(searchNext)) {
                    return this.changeFocus(searchNext);
                }
                return true;
            }
        }
        return false;
    }

    protected boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (popup != null) {
            return popup.keyReleased(keyCode, scanCode, modifiers);
        } else {
            return root.keyReleased(keyCode, scanCode, modifiers);
        }
    }

    protected boolean charTyped(char codePoint, int modifiers) {
        if (popup != null) {
            return popup.charTyped(codePoint, modifiers);
        } else {
            return root.charTyped(codePoint, modifiers);
        }
    }

    private boolean changeFocus(boolean searchNext) {
        //TODO change focus
        return false;
    }

    /**
     * Refocus mouse cursor and update mouse position
     */
    public void refreshMouse() {
        mouseMoved(mouseX, mouseY);
    }

    protected void draw() {
        root.draw(animationTime);
        if (popup != null) {
            popup.draw(animationTime);
        }
    }

    protected void resize(int width, int height) {
        this.width = width;
        this.height = height;
        root.resize(width, height);
        if (popup != null) {
            popup.resize(width, height);
        }
        double scale = minecraft.getMainWindow().getGuiScaleFactor();
        mouseX = minecraft.mouseHelper.getMouseX() / scale;
        mouseY = minecraft.mouseHelper.getMouseY() / scale;
        refreshMouse();
    }

    public void resizeForModule(IModule module) {
        module.resize(width, height);
    }

    protected void clear() {
        animations.clear();
        runnables.clear();
        root = null;
        popup = null;
        extraData = null;
        MouseTools.useDefaultCursor();
    }

    public void clientTick() {
        ticks++;
        if (root != null) {
            root.tick(ticks);
        }
        if (popup != null) {
            popup.tick(ticks);
        }
        runnables.forEach(e -> e.tick(ticks));
        runnables.removeIf(DelayedRunnable::shouldRemove);
    }

    public void renderTick(float partialTick) {
        animationTime = ticks + partialTick;
        animations.forEach(e -> e.update(animationTime));
        animations.removeIf(IAnimation::shouldRemove);
    }

    public void resetTicks() {
        ticks = 0;
        animationTime = 0;
    }

    public float getAnimationTime() {
        return animationTime;
    }

    public int getTicks() {
        return ticks;
    }

    public int getWindowWidth() {
        return width;
    }

    public int getWindowHeight() {
        return height;
    }

    public double getMouseX() {
        return mouseX;
    }

    public double getMouseY() {
        return mouseY;
    }

    public void setExtraData(PacketBuffer extraData) {
        this.extraData = extraData;
    }

    /**
     * Get extra data from server side
     * @return packet buffer
     */
    @Nullable
    public PacketBuffer getExtraData() {
        return extraData;
    }
}
