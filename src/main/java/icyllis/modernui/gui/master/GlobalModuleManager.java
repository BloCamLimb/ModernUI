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

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.animation.IAnimation;
import icyllis.modernui.system.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.text.ITextComponent;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Manage current GUI (or screen / modules), and post events to base module
 */
public enum GlobalModuleManager {
    INSTANCE;

    public static final Marker MARKER = MarkerManager.getMarker("SCREEN");

    private Minecraft minecraft = Minecraft.getInstance();

    @Nullable
    private Screen guiToOpen;

    /**
     * Current screen instance, must be ModernScreen or ModernContainerScreen
     * This is not the same instance as Minecraft.currentScreen
     */
    private Screen rootScreen;


    //private PacketBuffer extraData;

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

    private List<DelayedTask> tasks = new CopyOnWriteArrayList<>();

    private int ticks = 0;

    private float animationTime = 0;

    public void openGuiScreen(ITextComponent title, @Nonnull Supplier<IModule> root) {
        this.supplier = root;
        minecraft.displayGuiScreen(new ModernScreen(title));
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Nonnull
    private <T extends Container, U extends Screen & IHasContainer<T>> ScreenManager.IScreenFactory<T, U> castModernScreen(@Nonnull Function<T, Supplier<IModule>> root) {
        return (c, p, t) -> {
            // The client container can be null sometimes, but a container screen doesn't allow the container to be null
            // so return null, there's no gui will be open, and the server container will be closed automatically
            if (c == null) {
                return null;
            }
            this.supplier = root.apply(c);
            return (U) new ModernContainerScreen<>(c, p, t);
        };
    }

    /**
     * Register a container screen on client
     *
     * @param type container type
     * @param root root module factory
     * @param <T>  container
     */
    public <T extends Container> void registerContainerScreen(@Nonnull ContainerType<? extends T> type, @Nonnull Function<T, Supplier<IModule>> root) {
        ScreenManager.registerFactory(type, castModernScreen(root));
    }

    public void closeGuiScreen() {
        minecraft.displayGuiScreen(null);
    }

    void init(Screen master, int width, int height) {
        this.rootScreen = master;
        this.width = width;
        this.height = height;
        if (supplier != null) {
            root = supplier.get();
            supplier = null;
        }
        if (root == null) {
            closeGuiScreen();
            return;
        }
        resize(width, height);
    }

    public void onGuiOpen(Screen gui, Consumer<Boolean> cancel) {
        this.guiToOpen = gui;
        if (gui == null) {
            clear();
            return;
        }
        if (rootScreen != gui && ((gui instanceof ModernScreen) || (gui instanceof ModernContainerScreen<?>))) {
            if (root != null) {
                cancel.accept(true);
                ModernUI.LOGGER.fatal(MARKER, "ModernUI doesn't allow to keep other screens, use module group instead. RootScreen: {}, GuiToOpen: {}", rootScreen, gui);
                return;
            }
            resetTicks();
        }
        // hotfix 1.5.2, but no idea with screens that will pause game
        if (rootScreen != gui && rootScreen != null) {
            mouseMoved(-1, -1);
        }
        // for non-modernui screens
        if (rootScreen == null) {
            resetTicks();
        }
    }

    /**
     * Open a popup module, a special module
     *
     * @param popup      popup module
     * @param resetMouse true will post mouseMoved(-1, -1) to root module
     *                   confirm window should reset mouse
     *                   context menu should not reset mouse
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

    @Nullable
    public Screen getModernScreen() {
        return rootScreen;
    }

    @Nullable
    public IModule getRootModule() {
        return root;
    }

    public void addAnimation(IAnimation animation) {
        if (!animations.contains(animation)) {
            animations.add(animation);
        }
    }

    public void scheduleTask(DelayedTask task) {
        tasks.add(task);
    }

    void mouseMoved(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        if (popup != null) {
            popup.mouseMoved(mouseX, mouseY);
        } else if (root != null) { // hotfix 1.4.4
            root.mouseMoved(mouseX, mouseY);
        }
    }

    boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (popup != null) {
            return popup.mouseClicked(mouseX, mouseY, mouseButton);
        } else {
            return root.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (popup != null) {
            return popup.mouseReleased(mouseX, mouseY, mouseButton);
        } else {
            return root.mouseReleased(mouseX, mouseY, mouseButton);
        }
    }

    boolean mouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (popup != null) {
            return popup.mouseDragged(mouseX, mouseY, deltaX, deltaY);
        } else {
            return root.mouseDragged(mouseX, mouseY, deltaX, deltaY);
        }
    }

    boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (popup != null) {
            return popup.mouseScrolled(mouseX, mouseY, amount);
        } else {
            return root.mouseScrolled(mouseX, mouseY, amount);
        }
    }

    boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (popup != null) {
            return popup.keyPressed(keyCode, scanCode, modifiers);
        } else {
            return root.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (popup != null) {
            return popup.keyReleased(keyCode, scanCode, modifiers);
        } else {
            return root.keyReleased(keyCode, scanCode, modifiers);
        }
    }

    boolean charTyped(char codePoint, int modifiers) {
        if (popup != null) {
            return popup.charTyped(codePoint, modifiers);
        } else {
            return root.charTyped(codePoint, modifiers);
        }
    }

    boolean changeKeyboardListener(boolean searchNext) {
        //TODO change focus
        return false;
    }

    boolean onBack() {
        if (popup != null) {
            closePopup();
            return true;
        }
        return root.onBack();
    }

    /**
     * Refocus mouse cursor and update mouse position
     */
    void refreshMouse() {
        mouseMoved(mouseX, mouseY);
    }

    void draw() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();
        RenderSystem.disableDepthTest();
        root.draw(animationTime);
        if (popup != null) {
            popup.draw(animationTime);
        }
        Canvas.setLineAA0(false);
        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    void resize(int width, int height) {
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

    void clear() {
        // Hotfix 1.4.7
        if (guiToOpen == null) {
            animations.clear();
            tasks.clear();
            root = null;
            popup = null;
            //extraData = null;
            rootScreen = null;
            LayoutEditingGui.INSTANCE.setHoveredWidget(null);
            MouseTools.useDefaultCursor();
            // Hotfix 1.5.8
            minecraft.keyboardListener.enableRepeatEvents(false);
        }
    }

    public void clientTick() {
        ticks++;
        if (root != null) {
            root.tick(ticks);
        }
        if (popup != null) {
            popup.tick(ticks);
        }
        // tick listeners are always called before runnables
        for (DelayedTask runnable : tasks) {
            runnable.tick(ticks);
        }
        tasks.removeIf(DelayedTask::shouldRemove);
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    public void renderTick(float partialTick) {
        animationTime = ticks + partialTick;
        animations.removeIf(IAnimation::shouldRemove);
        // list size is dynamically changeable
        for (int i = 0; i < animations.size(); i++) {
            animations.get(i).update(animationTime);
        }
    }

    private void resetTicks() {
        ticks = 0;
        animationTime = 0;
    }

    public float getAnimationTime() {
        return animationTime;
    }

    int getTicks() {
        return ticks;
    }

    int getWindowWidth() {
        return width;
    }

    int getWindowHeight() {
        return height;
    }

    double getMouseX() {
        return mouseX;
    }

    double getMouseY() {
        return mouseY;
    }

    /*public void setExtraData(PacketBuffer extraData) {
        this.extraData = extraData;
    }


    @Nullable
    public PacketBuffer getExtraData() {
        return extraData;
    }*/
}
