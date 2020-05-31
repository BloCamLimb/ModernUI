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

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.ui.animation.IAnimation;
import icyllis.modernui.ui.test.IModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
 * Manage current gui screen, mainly Modern UI's, and post events to root view and popup view
 */
@OnlyIn(Dist.CLIENT)
public enum UIManager implements IViewParent {
    /**
     * Render thread instance only
     */
    INSTANCE;

    public static final Marker MARKER = MarkerManager.getMarker("UI");

    private final Minecraft minecraft = Minecraft.getInstance();

    /**
     * Cached gui to open, used for logic check
     */
    @Nullable
    private Screen guiToOpen;

    /**
     * Current screen instance, must be ModernScreen or ModernContainerScreen
     * This is not the same instance as Minecraft.currentScreen
     */
    private Screen modernScreen;

    /**
     * Cached factory for instantiation
     */
    @Nullable
    private Supplier<View> factory;

    /**
     * The origin of all modules
     */
    /*private IModule root;*/

    private View rootView;

    @Nullable
    private IModule popup;

    private int width, height;

    private double mouseX, mouseY;

    private final List<IAnimation> animations = new ArrayList<>();

    private final List<DelayedTask> tasks = new CopyOnWriteArrayList<>();

    private int ticks = 0;

    private float animationTime = 0;

    // lazy loading
    private Canvas canvas = null;

    @Nullable
    private View vHovered = null;

    @Nullable
    private View vDragging = null;

    @Nullable
    private View vKeyboard = null;

    private int dClickTime = -10;

    UIManager() {

    }

    /**
     * Open a gui screen on client side
     *
     * @param title   screen title
     * @param factory root view factory
     */
    public void openGuiScreen(ITextComponent title, @Nonnull Supplier<View> factory) {
        this.factory = factory;
        minecraft.displayGuiScreen(new ModernScreen(title));
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Nonnull
    private <T extends Container, U extends Screen & IHasContainer<T>> ScreenManager.IScreenFactory<T, U> castModernScreen(@Nonnull Function<T, Supplier<View>> factory) {
        return (c, p, t) -> {
            // The client container can be null sometimes, but a container screen doesn't allow the container to be null
            // so return null, there's no gui will be open, and the server container will be closed automatically
            if (c == null) {
                return null;
            }
            this.factory = factory.apply(c);
            return (U) new ModernContainerScreen<>(c, p, t);
        };
    }

    /**
     * Register a container screen on client
     * <p>
     * Use {@link net.minecraftforge.fml.network.NetworkHooks#openGui(ServerPlayerEntity, INamedContainerProvider, Consumer)}
     * to open a client gui on server
     *
     * @param type container type
     * @param factory root view factory
     * @param <T>  container
     */
    public <T extends Container> void registerContainerScreen(@Nonnull ContainerType<? extends T> type, @Nonnull Function<T, Supplier<View>> factory) {
        ScreenManager.registerFactory(type, castModernScreen(factory));
    }

    /**
     * Close current gui screen
     */
    public void closeGuiScreen() {
        minecraft.displayGuiScreen(null);
    }

    /**
     * Open a popup module, a special module
     *
     * @param popup   popup module
     * @param refresh true will post mouseMoved(-1, -1) to root module
     *                confirm window should reset mouse
     *                context menu should not reset mouse
     */
    public void openPopup(IModule popup, boolean refresh) {
        /*if (root == null) {
            ModernUI.LOGGER.fatal(MARKER, "#openPopup() shouldn't be called when there's NO gui open");
            return;
        }*/
        if (this.popup != null) {
            ModernUI.LOGGER.warn(MARKER, "#openPopup() shouldn't be called when there's already a popup, the previous one has been overwritten");
        }
        if (refresh) {
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

    void init(Screen mui, int width, int height) {
        this.modernScreen = mui;
        this.width = width;
        this.height = height;
        if (factory != null) {
            rootView = factory.get();
            factory = null;
        }
        if (rootView == null) {
            closeGuiScreen();
            return;
        }
        rootView.setParent(this);
        if (canvas == null) {
            canvas = new Canvas();
        }
        resize(width, height);
    }

    public void onGuiOpen(Screen gui, Consumer<Boolean> cancel) {
        this.guiToOpen = gui;
        if (gui == null) {
            clear();
            return;
        }
        if (modernScreen != gui && ((gui instanceof ModernScreen) || (gui instanceof ModernContainerScreen<?>))) {
            if (rootView != null) {
                cancel.accept(true);
                ModernUI.LOGGER.fatal(MARKER, "ModernUI doesn't allow to keep other screens, use module group instead. RootScreen: {}, GuiToOpen: {}", modernScreen, gui);
                return;
            }
            resetTicks();
        }
        // hotfix 1.5.2, but there's no way to work with screens that will pause game
        if (modernScreen != gui && modernScreen != null) {
            mouseMoved(-1, -1);
        }
        // for non-modern-ui screens
        if (modernScreen == null) {
            resetTicks();
        }
    }

    @Nullable
    public Screen getModernScreen() {
        return modernScreen;
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
            return;
        }
        if (rootView != null) {
            if (!rootView.updateMouseHover(mouseX, mouseY)) {
                setHoveredView(null);
            }
        }
    }

    boolean mouseClicked0(double mouseX, double mouseY, int mouseButton) {
        if (popup != null) {
            return popup.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (vHovered != null) {
            if (mouseButton == 0) {
                int delta = ticks - dClickTime;
                if (delta < 10) {
                    dClickTime = -10;
                    if (vHovered.onMouseDoubleClicked(vHovered.getRelativeMX(), vHovered.getRelativeMY())) {
                        return true;
                    }
                } else {
                    dClickTime = ticks;
                }
                return vHovered.onMouseLeftClicked(vHovered.getRelativeMX(), vHovered.getRelativeMY());
            } else if (mouseButton == 1) {
                return vHovered.onMouseRightClicked(vHovered.getRelativeMX(), vHovered.getRelativeMY());
            }
        }
        return false;
    }

    boolean mouseReleased0(double mouseX, double mouseY, int mouseButton) {
        if (popup != null) {
            return popup.mouseReleased(mouseX, mouseY, mouseButton);
        }
        if (vDragging != null) {
            setDragging(null);
            return true;
        }
        return false;//root.mouseReleased(mouseX, mouseY, mouseButton);
    }

    boolean mouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (popup != null) {
            return popup.mouseDragged(mouseX, mouseY, deltaX, deltaY);
        }
        if (vDragging != null) {
            return vDragging.onMouseDragged(vDragging.getRelativeMX(), vDragging.getRelativeMY(), deltaX, deltaY);
        }
        return false;
    }

    boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (popup != null) {
            return popup.mouseScrolled(mouseX, mouseY, amount);
        }
        if (vHovered != null) {
            return vHovered.onMouseScrolled(vHovered.getRelativeMX(), vHovered.getRelativeMY(), amount);
        }
        return false;
    }

    boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (popup != null) {
            return popup.keyPressed(keyCode, scanCode, modifiers);
        } else {
            return false;//root.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (popup != null) {
            return popup.keyReleased(keyCode, scanCode, modifiers);
        } else {
            return false;//root.keyReleased(keyCode, scanCode, modifiers);
        }
    }

    boolean charTyped(char codePoint, int modifiers) {
        if (popup != null) {
            return popup.charTyped(codePoint, modifiers);
        } else {
            return false;//root.charTyped(codePoint, modifiers);
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
        return false;//root.onBack();
    }

    /**
     * Refocus mouse cursor and update mouse position
     */
    public void refreshMouse() {
        mouseMoved(mouseX, mouseY);
    }

    void draw() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();
        RenderSystem.disableDepthTest();
        rootView.draw(canvas, animationTime);
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
        //root.resize(width, height);
        rootView.layout(this);
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
            rootView = null;
            popup = null;
            //extraData = null;
            modernScreen = null;
            UIEditor.INSTANCE.setHoveredWidget(null);
            UITools.useDefaultCursor();
            // Hotfix 1.5.8
            minecraft.keyboardListener.enableRepeatEvents(false);
        }
    }

    public void clientTick() {
        ++ticks;
        if (rootView != null) {
            rootView.tick(ticks);
        }
        if (popup != null) {
            popup.tick(ticks);
        }
        // tick listeners are always called before runnables
        for (DelayedTask task : tasks) {
            task.tick(ticks);
        }
        tasks.removeIf(DelayedTask::shouldRemove);
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    public void renderTick(float partialTick) {
        animationTime = ticks + partialTick;

        // remove animations from loop in next frame
        animations.removeIf(IAnimation::shouldRemove);

        // list size is dynamically changeable, due to animation chain
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

    void setHoveredView(@Nullable View view) {
        if (vHovered != view) {
            if (vHovered != null) {
                vHovered.onMouseHoverExit();
            }
            vHovered = view;
            if (vHovered != null) {
                vHovered.onMouseHoverEnter();
            }
            dClickTime = -10;
            UIEditor.INSTANCE.setHoveredWidget(view);
        }
    }

    @Nullable
    public View getHoveredView() {
        return vHovered;
    }

    /**
     * Set current active dragging view
     *
     * @param view dragging view
     */
    public void setDragging(@Nullable View view) {
        if (vDragging != view) {
            if (vDragging != null) {
                vDragging.onStopDragging();
            }
            vDragging = view;
            if (vDragging != null) {
                vDragging.onStartDragging();
            }
        }
    }

    @Nullable
    public View getDragging() {
        return vDragging;
    }

    /**
     * Set active keyboard listener to listen key events
     *
     * @param view keyboard view
     */
    public void setKeyboard(@Nullable View view) {
        if (vKeyboard != view) {
            minecraft.keyboardListener.enableRepeatEvents(view != null);
            if (vKeyboard != null) {
                vKeyboard.onStopKeyboard();
            }
            vKeyboard = view;
            if (vKeyboard != null) {
                vKeyboard.onStartKeyboard();
            }
        }
    }

    @Nullable
    public View getKeyboard() {
        return vKeyboard;
    }

    public View getRootView() {
        return rootView;
    }

    @Override
    public IViewParent getParent() {
        throw new RuntimeException("System view!");
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getLeft() {
        return 0;
    }

    @Override
    public int getTop() {
        return 0;
    }

    @Override
    public int getRight() {
        return width;
    }

    @Override
    public int getBottom() {
        return height;
    }

    @Override
    public double getRelativeMX() {
        return mouseX;
    }

    @Override
    public double getRelativeMY() {
        return mouseY;
    }

    @Override
    public float toAbsoluteX(float rx) {
        return rx;
    }

    @Override
    public float toAbsoluteY(float ry) {
        return ry;
    }

    @Override
    public float getTranslationX() {
        return 0;
    }

    @Override
    public float getTranslationY() {
        return 0;
    }

    @Override
    public void relayoutChild(@Nonnull View view) {
        view.layout(this);
    }
}
