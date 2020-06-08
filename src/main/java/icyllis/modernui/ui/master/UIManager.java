/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
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
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.ui.animation.Animation;
import icyllis.modernui.ui.layout.MeasureSpec;
import icyllis.modernui.ui.test.IModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Manage current gui screen, mainly Modern UI's, and post events to root view and popup view
 */
@OnlyIn(Dist.CLIENT)
public enum UIManager implements IViewParent {
    /**
     * Render thread instance only
     */
    INSTANCE;

    // logger marker
    public static final Marker MARKER = MarkerManager.getMarker("UI");

    // cached minecraft instance
    private final Minecraft minecraft = Minecraft.getInstance();

    // Cached gui to open, used for logic
    @Nullable
    private Screen guiToOpen;

    // Current screen instance, must be ModernScreen or ModernContainerScreen or null
    @Nullable
    private Screen modernScreen;

    // main fragment of a UI
    private Fragment mainFragment;

    // main view that created from main fragment
    private View mainView;

    @Deprecated
    @Nullable
    private IModule popup;

    // scaled game window width / height
    private int width, height;

    // scaled mouseX, mouseY on screen
    private double mouseX, mouseY;

    // a list of animations in render loop
    private final List<Animation> animations = new ArrayList<>();

    // a list of UI tasks
    private final List<DelayedTask> tasks = new CopyOnWriteArrayList<>();

    // elapsed ticks from a gui open, update every tick, 20 = 1 second
    private int ticks = 0;

    // elapsed time from a gui open, update every frame, 20.0 = 1 second
    private float drawTime = 0;

    // lazy loading, should be final
    private Canvas canvas = null;

    // only post events to focused views
    @Nullable
    private View vHovered = null;
    @Nullable
    private View vDragging = null;
    @Nullable
    private View vKeyboard = null;

    // for double click check, 10 tick = 0.5s
    private int dClickTime = -10;

    // prevent request layout on init layout
    private boolean initLayout = true;

    UIManager() {

    }

    /**
     * Open a gui screen
     *
     * @param mainFragment main fragment of the UI
     */
    public void openGuiScreen(@Nonnull Fragment mainFragment) {
        this.mainFragment = mainFragment;
        minecraft.displayGuiScreen(new ModernScreen());
    }

    /**
     * Close current gui screen
     */
    public void closeGuiScreen() {
        minecraft.displayGuiScreen(null);
    }

    /**
     * Register a container screen
     * To open the UI,
     * see {@link net.minecraftforge.fml.network.NetworkHooks#openGui(ServerPlayerEntity, INamedContainerProvider, Consumer)}
     *
     * @param type    container type
     * @param factory main fragment factory
     * @param <T>     container
     */
    public <T extends Container> void registerContainerScreen(@Nonnull ContainerType<? extends T> type, @Nonnull Function<T, Fragment> factory) {
        ScreenManager.registerFactory(type, castModernScreen(factory));
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Nonnull
    private <T extends Container, U extends Screen & IHasContainer<T>> ScreenManager.IScreenFactory<T, U> castModernScreen(@Nonnull Function<T, Fragment> factory) {
        return (c, p, t) -> {
            // The client container can be null sometimes, but a container screen doesn't allow the container to be null
            // so return null, there's no gui will be open, and the server container will be closed automatically
            if (c == null) {
                return null;
            }
            this.mainFragment = factory.apply(c);
            return (U) new ModernContainerScreen<>(c, p, t);
        };
    }

    /**
     * Open a popup module, a special module
     *
     * @param popup   popup module
     * @param refresh true will post mouseMoved(-1, -1) to root module
     *                confirm window should reset mouse
     *                context menu should not reset mouse
     */
    //TODO new popup system
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

    // called when open a UI, or back to the UI (Modern UI's UI)
    void init(Screen mui, int width, int height) {
        this.modernScreen = mui;
        this.width = width;
        this.height = height;
        if (mainView == null) {
            mainView = mainFragment.createView();
            if (mainView == null) {
                closeGuiScreen();
                return;
            } else {
                mainView.assignParent(this);
            }
        }
        if (canvas == null) {
            canvas = new Canvas();
        }
        initLayout = true;
        resize(width, height);
        initLayout = false;
    }

    // System method, do not call
    public void handleGuiOpenEvent(Screen guiToOpen, Consumer<Boolean> cancelFunc) {
        this.guiToOpen = guiToOpen;
        if (guiToOpen == null) {
            destroy();
            return;
        }
        // modern screen != null
        if (modernScreen != guiToOpen && ((guiToOpen instanceof ModernScreen) || (guiToOpen instanceof ModernContainerScreen<?>))) {
            if (mainView != null) {
                cancelFunc.accept(true);
                ModernUI.LOGGER.fatal(MARKER, "ModernUI doesn't allow to keep other screens, use fragment instead. RootScreen: {}, GuiToOpen: {}", modernScreen, guiToOpen);
                return;
            }
            resetTicks();
        }
        // hotfix 1.5.2, but there's no way to work with screens that will pause game
        if (modernScreen != guiToOpen && modernScreen != null) {
            mouseMoved(-1, -1);
        }
        // for non-modern-ui screens
        if (modernScreen == null) {
            resetTicks();
        }
    }

    private void resetTicks() {
        ticks = 0;
        drawTime = 0;
    }

    @Nullable
    public Screen getModernScreen() {
        return modernScreen;
    }

    /**
     * Add an active animation, which will be removed from list if finished
     *
     * @param animation animation to add
     */
    public void enqueueAnimation(@Nonnull Animation animation) {
        if (!animations.contains(animation)) {
            animations.add(animation);
        }
    }

    public void enqueueTask(@Nonnull DelayedTask task) {
        if (!tasks.contains(task)) {
            tasks.add(task);
        }
    }

    void mouseMoved(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        if (popup != null) {
            popup.mouseMoved(mouseX, mouseY);
            return;
        }
        if (mainView != null) {
            if (!mainView.updateMouseHover(mouseX, mouseY)) {
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
        mainView.draw(canvas, drawTime);
        if (popup != null) {
            popup.draw(drawTime);
        }
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    void resize(int width, int height) {
        this.width = width;
        this.height = height;
        int msw = MeasureSpec.makeMeasureSpec(width, MeasureSpec.Mode.AT_MOST);
        int msh = MeasureSpec.makeMeasureSpec(height, MeasureSpec.Mode.AT_MOST);
        mainView.measure(msw, msh);
        mainView.layout(0, 0, mainView.measuredWidth, mainView.measuredHeight);
        if (popup != null) {
            popup.resize(width, height);
        }
        double scale = minecraft.getMainWindow().getGuiScaleFactor();
        mouseX = minecraft.mouseHelper.getMouseX() / scale;
        mouseY = minecraft.mouseHelper.getMouseY() / scale;
        refreshMouse();
    }

    void destroy() {
        // Hotfix 1.4.7
        if (guiToOpen == null) {
            animations.clear();
            tasks.clear();
            mainView = null;
            popup = null;
            //extraData = null;
            mainFragment = null;
            modernScreen = null;
            UIEditor.INSTANCE.setHoveredWidget(null);
            UITools.useDefaultCursor();
            // Hotfix 1.5.8
            minecraft.keyboardListener.enableRepeatEvents(false);
        }
    }

    // System method, do not call
    public void clientTick() {
        ++ticks;
        if (mainView != null) {
            mainView.tick(ticks);
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

    // System method, do not call
    @SuppressWarnings("ForLoopReplaceableByForEach")
    public void renderTick(float partialTick) {
        drawTime = ticks + partialTick;

        // remove animations from loop in next frame
        animations.removeIf(Animation::shouldRemove);

        // list size is dynamically changeable, due to animation chain
        for (int i = 0; i < animations.size(); i++) {
            animations.get(i).update(drawTime);
        }
    }

    public float getAnimationTime() {
        return drawTime;
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

    public boolean isInitLayout() {
        return initLayout;
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

    public View getMainView() {
        return mainView;
    }

    @Override
    public IViewParent getParent() {
        throw new RuntimeException("System view!");
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
    public void relayoutChildViews() {
        //TODO
    }
}
