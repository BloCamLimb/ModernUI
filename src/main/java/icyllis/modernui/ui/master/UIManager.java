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
import icyllis.modernui.ui.layout.FrameLayout;
import icyllis.modernui.ui.layout.Gravity;
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
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
 * Modern UI's UI system service, almost everything is here
 */
@SuppressWarnings("unused")
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

    // cached gui to open
    @Nullable
    private Screen guiToOpen;

    // current screen instance, must be ModernScreen or ModernContainerScreen or null
    @Nullable
    private Screen modernScreen;

    // main fragment of a UI
    private Fragment fragment;

    // main view that created from main fragment
    private View view;

    @Deprecated
    @Nullable
    private IModule popup;

    // scaled game window width / height
    private int width, height;

    // scaled mouseX, mouseY on screen
    private int mouseX, mouseY;

    // a list of animations in render loop
    private final List<Animation> animations = new ArrayList<>();

    // a list of UI tasks
    private final List<DelayedTask> tasks = new CopyOnWriteArrayList<>();

    // elapsed ticks from a gui open, update every tick, 20 = 1 second
    private int ticks = 0;

    // elapsed time from a gui open, update every frame, 20.0 = 1 second
    private float time = 0;

    // lazy loading, should be final
    private Canvas canvas = null;

    // only post events to focused views
    @Nullable
    private View mHovered  = null;
    @Nullable
    private View mDragging = null;
    @Nullable
    private View mKeyboard = null;

    // for double click check, 10 tick = 0.5s
    private int doubleClickTime = -10;

    // to schedule layout on next frame
    private boolean layoutRequested = false;

    // to fix layout freq at 60Hz at most
    private float lastLayoutTime = 0;

    UIManager() {

    }

    /**
     * Open a gui screen
     *
     * @param mainFragment main fragment of the UI
     */
    public void openGuiScreen(@Nonnull Fragment mainFragment) {
        this.fragment = mainFragment;
        minecraft.displayGuiScreen(new ModernScreen());
    }

    /**
     * Close current gui screen
     */
    public void closeGuiScreen() {
        minecraft.displayGuiScreen(null);
    }

    /**
     * Register a container screen. To open the gui screen,
     * see {@link net.minecraftforge.fml.network.NetworkHooks#openGui(ServerPlayerEntity, INamedContainerProvider, Consumer)}
     *
     * @param type    container type
     * @param factory main fragment factory
     * @param <T>     container
     */
    public <T extends Container> void registerContainerScreen(@Nonnull ContainerType<? extends T> type,
                                                              @Nonnull Function<T, Fragment> factory) {
        ScreenManager.registerFactory(type, castModernScreen(factory));
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Nonnull
    private <T extends Container, U extends Screen & IHasContainer<T>> ScreenManager.IScreenFactory<T, U> castModernScreen(
            @Nonnull Function<T, Fragment> factory) {
        return (c, p, t) -> {
            // The client container can be null sometimes, but a container screen doesn't allow the container to be null
            // so return null, there's no gui will be open, and the server container will be closed automatically
            if (c == null) {
                return null;
            }
            this.fragment = factory.apply(c);
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
            this.sMouseMoved(-1, -1);
        }
        this.popup = popup;
        this.popup.resize(width, height);
        refreshMouse();
    }

    /**
     * Close current popup
     */
    public void closePopup() {
        if (popup != null) {
            popup = null;
            refreshMouse();
        }
    }

    /**
     * Called when open a gui screen, or back to the gui screen
     *
     * @param mui    modern screen or modern container screen
     * @param width  screen width (= game main window width)
     * @param height screen height (= game main window height)
     */
    void sInit(@Nonnull Screen mui, int width, int height) {
        this.modernScreen = mui;

        // init view of this UI
        if (view == null) {
            if (fragment == null) {
                ModernUI.LOGGER.fatal(MARKER, "Fragment can't be null when opening a gui screen");
                closeGuiScreen();
                return;
            }
            view = fragment.createView();
            if (view == null) {
                ModernUI.LOGGER.fatal(MARKER, "The main view created from the fragment shouldn't be null");
                view = new View();
            }

            ViewGroup.LayoutParams params = view.getLayoutParams();
            // convert layout params
            if (!(params instanceof FrameLayout.LayoutParams)) {
                if (params instanceof ViewGroup.MarginLayoutParams) {
                    params = new FrameLayout.LayoutParams((ViewGroup.MarginLayoutParams) params);
                } else {
                    params = new FrameLayout.LayoutParams(params);
                }
                view.setLayoutParams(params);
            }

            view.assignParent(this);
        }

        // create canvas
        if (canvas == null) {
            canvas = new Canvas();
        }

        sResize(width, height);
    }

    /**
     * Inner method, do not call
     */
    @SubscribeEvent
    void gGuiOpen(@Nonnull GuiOpenEvent event) {
        guiToOpen = event.getGui();

        if (guiToOpen == null) {
            sDestroy();
            return;
        }

        if (modernScreen != guiToOpen && ((guiToOpen instanceof ModernScreen) || (guiToOpen instanceof ModernContainerScreen<?>))) {
            if (view != null) {
                ModernUI.LOGGER.fatal(MARKER, "Modern UI doesn't allow to keep other screens. ModernScreen: {}, GuiToOpen: {}", modernScreen, guiToOpen);
                event.setCanceled(true);
                return;
            }
            ticks = 0;
            time = 0;
        }
        // hotfix 1.5.2, but there's no way to work with screens that will pause game
        if (modernScreen != guiToOpen && modernScreen != null) {
            sMouseMoved(-1, -1);
        }
        // for non-modern-ui screens
        if (modernScreen == null) {
            ticks = 0;
            time = 0;
        }
    }

    /**
     * Get current open screen differently from Minecraft's,
     * which will only return Modern UI's screen or null
     *
     * @return modern screen
     */
    @Nullable
    public Screen getModernScreen() {
        return modernScreen;
    }

    /**
     * Add an active animation, which will be removed from list if finished
     *
     * @param animation animation to add
     */
    public void addAnimation(@Nonnull Animation animation) {
        if (!animations.contains(animation)) {
            animations.add(animation);
        }
    }

    /**
     * Post a task that will run on next pre-tick
     *
     * @param runnable     runnable
     * @param delayedTicks delayed ticks to run the task
     */
    public void postTask(@Nonnull Runnable runnable, int delayedTicks) {
        tasks.add(new DelayedTask(runnable, delayedTicks));
    }

    void sMouseMoved(int mouseX, int mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        focus();
        /*if (popup != null) {
            popup.mouseMoved(mouseX, mouseY);
            return;
        }*/
    }

    boolean sMouseClicked(int mouseX, int mouseY, int mouseButton) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        /*if (popup != null) {
            return popup.mouseClicked(mouseX, mouseY, mouseButton);
        }*/
        if (mHovered != null) {
            if (mouseButton == 0) {
                int delta = ticks - doubleClickTime;
                if (delta < 10) {
                    doubleClickTime = -10;
                    if (mHovered.onMouseDoubleClicked(getViewMouseX(mHovered), getViewMouseY(mHovered))) {
                        return true;
                    }
                } else {
                    doubleClickTime = ticks;
                }
                return mHovered.onMouseLeftClicked(getViewMouseX(mHovered), getViewMouseY(mHovered));
            } else if (mouseButton == 1) {
                return mHovered.onMouseRightClicked(getViewMouseX(mHovered), getViewMouseY(mHovered));
            }
        }
        return false;
    }

    boolean sMouseReleased(int mouseX, int mouseY, int mouseButton) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        /*if (popup != null) {
            return popup.mouseReleased(mouseX, mouseY, mouseButton);
        }*/
        if (mDragging != null) {
            setDragging(null);
            return true;
        }
        return false;//root.mouseReleased(mouseX, mouseY, mouseButton);
    }

    boolean sMouseDragged(int mouseX, int mouseY, double deltaX, double deltaY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        /*if (popup != null) {
            return popup.mouseDragged(mouseX, mouseY, deltaX, deltaY);
        }*/
        if (mDragging != null) {
            return mDragging.onMouseDragged(getViewMouseX(mDragging), getViewMouseY(mDragging), deltaX, deltaY);
        }
        return false;
    }

    boolean sMouseScrolled(int mouseX, int mouseY, double amount) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        /*if (popup != null) {
            return popup.mouseScrolled(mouseX, mouseY, amount);
        }*/
        if (mHovered != null) {
            return mHovered.onMouseScrolled(getViewMouseX(mHovered), getViewMouseY(mHovered), amount);
        }
        return false;
    }

    boolean sKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (popup != null) {
            return popup.keyPressed(keyCode, scanCode, modifiers);
        } else {
            return false;//root.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    boolean sKeyReleased(int keyCode, int scanCode, int modifiers) {
        if (popup != null) {
            return popup.keyReleased(keyCode, scanCode, modifiers);
        } else {
            return false;//root.keyReleased(keyCode, scanCode, modifiers);
        }
    }

    boolean sCharTyped(char codePoint, int modifiers) {
        if (popup != null) {
            return popup.charTyped(codePoint, modifiers);
        } else {
            return false;//root.charTyped(codePoint, modifiers);
        }
    }

    boolean sChangeKeyboard(boolean searchNext) {
        //TODO change focus implementation
        return false;
    }

    boolean sBack() {
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
        focus();
    }

    /**
     * Find focus in UI
     */
    private void focus() {
        if (view != null) {
            if (!view.updateMouseHover(mouseX, mouseY)) {
                setHoveredView(null);
            }
        }
    }

    /**
     * Raw draw method, draw entire UI
     */
    void sDraw() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();
        RenderSystem.disableDepthTest();
        view.draw(canvas, time);
        /*if (popup != null) {
            popup.draw(drawTime);
        }*/
        UIEditor.INSTANCE.draw(canvas);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    @SubscribeEvent
    void gRenderGameOverlay(@Nonnull RenderGameOverlayEvent.Pre event) {
        if (modernScreen != null && event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            event.setCanceled(true);
        }
    }

    /**
     * Called when game window size changed, used to re-layout the UI
     *
     * @param width  scaled game window width
     * @param height scaled game window height
     */
    void sResize(int width, int height) {
        this.width = width;
        this.height = height;
        double scale = minecraft.getMainWindow().getGuiScaleFactor();
        mouseX = (int) (minecraft.mouseHelper.getMouseX() / scale);
        mouseY = (int) (minecraft.mouseHelper.getMouseY() / scale);
        layout();
    }

    /**
     * Layout entire UI views
     * {@link #requestLayout()}
     */
    private void layout() {
        long startTime = System.nanoTime();
        // main view should be forced to layout as FrameLayout
        int widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.Mode.EXACTLY);
        int heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.Mode.EXACTLY);

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();

        int childWidthMeasureSpec = ViewGroup.getChildMeasureSpec(widthSpec,
                lp.leftMargin + lp.rightMargin, lp.width);
        int childHeightMeasureSpec = ViewGroup.getChildMeasureSpec(heightSpec,
                lp.topMargin + lp.bottomMargin, lp.height);

        view.measure(childWidthMeasureSpec, childHeightMeasureSpec);

        int measuredWidth = view.getMeasuredWidth();
        int measuredHeight = view.getMeasuredHeight();

        int childLeft;
        int childTop;

        switch (lp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.HORIZONTAL_CENTER:
                childLeft = (width - measuredWidth) / 2 +
                        lp.leftMargin - lp.rightMargin;
                break;
            case Gravity.RIGHT:
                childLeft = width - measuredWidth - lp.rightMargin;
                break;
            default:
                childLeft = lp.leftMargin;
        }

        switch (lp.gravity & Gravity.VERTICAL_GRAVITY_MASK) {
            case Gravity.VERTICAL_CENTER:
                childTop = (height - measuredHeight) / 2 +
                        lp.topMargin - lp.bottomMargin;
                break;
            case Gravity.BOTTOM:
                childTop = height - measuredHeight - lp.bottomMargin;
                break;
            default:
                childTop = lp.topMargin;
        }

        view.layout(childLeft, childTop, childLeft + measuredWidth, childTop + measuredHeight);
        /*if (popup != null) {
            popup.resize(width, height);
        }*/
        layoutRequested = false;

        ModernUI.LOGGER.debug(MARKER, "Layout performed in {} " + '\u03bc' + "s", (System.nanoTime() - startTime) / 1000);
        focus();
    }

    void sDestroy() {
        // Hotfix 1.4.7
        if (guiToOpen == null) {
            animations.clear();
            tasks.clear();
            view = null;
            popup = null;
            fragment = null;
            modernScreen = null;
            doubleClickTime = -10;
            lastLayoutTime = 0;
            layoutRequested = false;
            UIEditor.INSTANCE.setHoveredWidget(null);
            UITools.useDefaultCursor();
            // Hotfix 1.5.8
            minecraft.keyboardListener.enableRepeatEvents(false);
        }
    }

    /**
     * Inner method, do not call
     */
    @SubscribeEvent
    void gClientTick(@Nonnull TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        ++ticks;
        if (popup != null) {
            popup.tick(ticks);
        }
        if (view != null) {
            view.tick(ticks);
        }
        // view tick() is always called before tasks
        for (DelayedTask task : tasks) {
            task.tick(ticks);
        }
        tasks.removeIf(DelayedTask::shouldRemove);
    }

    /**
     * Inner method, do not call
     */
    @SuppressWarnings("ForLoopReplaceableByForEach")
    @SubscribeEvent
    void gRenderTick(@Nonnull TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        time = ticks + event.renderTickTime;

        // remove animations from loop in next frame
        animations.removeIf(Animation::shouldRemove);

        // list size is dynamically changeable, because updating animation may add new animation to the list
        for (int i = 0; i < animations.size(); i++) {
            animations.get(i).update(time);
        }

        // layout after updating animations and before drawing
        if (layoutRequested) {
            // fixed at 60Hz
            if (time - lastLayoutTime > 0.3333333f) {
                lastLayoutTime = time;
                layout();
            }
        }
    }

    /**
     * Get elapsed time from a gui open, update every frame, 20.0 = 1 second
     *
     * @return drawing time
     */
    public float getDrawingTime() {
        return time;
    }

    /**
     * Get elapsed ticks from a gui open, update every tick, 20 = 1 second
     *
     * @return elapsed ticks
     */
    public int getElapsedTicks() {
        return ticks;
    }

    /**
     * Get scaled UI screen width which is equal to game main window width
     *
     * @return window width
     */
    public int getWindowWidth() {
        return width;
    }

    /**
     * Get scaled UI screen height which is equal to game main window height
     *
     * @return window height
     */
    public int getWindowHeight() {
        return height;
    }

    /**
     * Get scaled mouse X position on screen
     *
     * @return mouse x
     */
    public int getMouseX() {
        return mouseX;
    }

    /**
     * Get scaled mouse Y position on screen
     *
     * @return mouse y
     */
    public int getMouseY() {
        return mouseY;
    }

    /**
     * Get logical mouse x for a view, generally used by system
     *
     * @param view view
     * @return relative mouse x
     */
    public int getViewMouseX(@Nonnull View view) {
        IViewParent parent = view.getParent();
        float mouseX = this.mouseX;

        while (parent != this) {
            mouseX += parent.getScrollX();
            parent = parent.getParent();
        }

        return (int) mouseX;
    }

    /**
     * Get logical mouse y for a view, generally used by system
     *
     * @param view view
     * @return relative mouse y
     */
    public int getViewMouseY(@Nonnull View view) {
        IViewParent parent = view.getParent();
        float mouseY = this.mouseY;

        while (parent != this) {
            mouseY += parent.getScrollY();
            parent = parent.getParent();
        }

        return (int) mouseY;
    }

    /**
     * Get main view of current UI
     *
     * @return main view
     */
    public View getMainView() {
        return view;
    }

    /**
     * Request layout all views with force layout mark in next frame
     * See {@link View#requestLayout()}
     * See {@link View#markForceLayout()}
     */
    @Override
    public void requestLayout() {
        layoutRequested = true;
    }

    // inner method
    void setHoveredView(@Nullable View view) {
        if (mHovered != view) {
            if (mHovered != null) {
                mHovered.onMouseHoverExit();
            }
            mHovered = view;
            if (mHovered != null) {
                mHovered.onMouseHoverEnter();
            }
            doubleClickTime = -10;
            UIEditor.INSTANCE.setHoveredWidget(view);
        }
    }

    @Nullable
    public View getHoveredView() {
        return mHovered;
    }

    /**
     * Set current active dragging view
     *
     * @param view dragging view
     */
    public void setDragging(@Nullable View view) {
        if (mDragging != view) {
            if (mDragging != null) {
                mDragging.onStopDragging();
            }
            mDragging = view;
            if (mDragging != null) {
                mDragging.onStartDragging();
            }
        }
    }

    @Nullable
    public View getDragging() {
        return mDragging;
    }

    /**
     * Set active keyboard listener to listen key events
     *
     * @param view keyboard view
     */
    public void setKeyboard(@Nullable View view) {
        if (mKeyboard != view) {
            minecraft.keyboardListener.enableRepeatEvents(view != null);
            if (mKeyboard != null) {
                mKeyboard.onStopKeyboard();
            }
            mKeyboard = view;
            if (mKeyboard != null) {
                mKeyboard.onStartKeyboard();
            }
        }
    }

    @Nullable
    public View getKeyboard() {
        return mKeyboard;
    }

    /**
     * Inner method, do not call
     */
    @Deprecated
    @Override
    public IViewParent getParent() {
        throw new RuntimeException("System view!");
    }

    /**
     * Inner method, do not call
     */
    @Deprecated
    @Override
    public float getScrollX() {
        throw new RuntimeException("System view!");
    }

    /**
     * Inner method, do not call
     */
    @Deprecated
    @Override
    public float getScrollY() {
        throw new RuntimeException("System view!");
    }

}
