/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.animation.Animation;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.BlurHandler;
import icyllis.modernui.graphics.math.Point;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.ui.discard.IModule;
import icyllis.modernui.ui.TestHUD;
import net.minecraft.client.Minecraft;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * UI system service, manages everything related to UI in Modern UI
 */
@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
public final class UIManager {

    // singleton
    private static UIManager instance;

    // logger marker
    public static final Marker MARKER = MarkerManager.getMarker("UIManager");

    // minecraft client instance
    private final Minecraft mMinecraftClient = Minecraft.getInstance();

    // UI event bus
    private final IEventBus mEventBus = BusBuilder.builder().setTrackPhases(false).build();

    // cached screen to open for logic check
    @Nullable
    private Screen screenToOpen;

    // current modern screen instance, must be ModernScreen or ModernContainerScreen or null
    // indicates whether the current screen is a Modern UI screen
    @Nullable
    private Screen modernScreen;

    // main fragment of a UI
    private Fragment fragment;

    // main UI view that created from main fragment
    private View view;

    // application window
    private final ViewRootImpl appWindow = new ViewRootImpl(this, ViewRootImpl.TYPE_APPLICATION);

    @Deprecated
    @Nullable
    private IModule popup;

    // scaled game window width / height
    private int width;
    private int height;

    // a list of animations in render loop
    private final List<Animation> animations = new ArrayList<>();

    // a list of UI tasks
    private final List<DelayedTask> tasks = new CopyOnWriteArrayList<>();

    // elapsed ticks from a gui open, update every tick, 20 = 1 second
    private int ticks = 0;

    // elapsed time from a gui open in milliseconds, update every frame
    private long timeMillis;

    // the canvas to draw things shared in all views and drawables
    // lazy loading because this class is loaded before GL initialization
    // will be init when Minecraft finished loading, and open MainMenuScreen
    // also init font renderer when loaded
    private Canvas canvas;

    // the most child hovered view, render at the top of other hovered ancestor views
    @Nullable
    private View mHovered;

    // focused view
    @Nullable
    private View mDragging;
    @Nullable
    private View mKeyboard;

    private final MotionEvent motionEvent = MotionEvent.obtain();

    // for double click check
    private int lastLmTick = Integer.MIN_VALUE;
    @Nullable
    private View lastLmView;

    @Nullable
    private View capturedView;

    // to schedule layout on next frame
    boolean layoutRequested = false;

    boolean cursorRefreshRequested = false;

    // to fix layout freq at 40Hz at most
    private long lastLayoutTime = 0;

    // drag event instance, also marks whether a drag and drop operation is ongoing
    @Nullable
    DragEvent dragEvent;

    // drag shadow
    View.DragShadow dragShadow;

    // drag shadow center for render offset
    Point dragShadowCenter;

    final InputHandler mInputHandler = new InputHandler();

    // dispatch mouse/key/drag event
    @Deprecated
    final GlobalEventDispatcher eventDispatcher = new GlobalEventDispatcher(new ArrayList<>());

    {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(BlurHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(UIEditor.INSTANCE);
    }

    /**
     * Returns the UI service instance
     *
     * @return instance
     */
    @Nonnull
    public static UIManager getInstance() {
        if (instance == null) {
            synchronized (UIManager.class) {
                if (instance == null) {
                    instance = new UIManager();
                }
            }
        }
        return instance;
    }

    /**
     * Open GUI screen and UI window
     *
     * @param fragment main fragment of the UI
     * @see #prepareWindows(Screen, int, int)
     */
    public void openGui(@Nonnull Fragment fragment) {
        this.fragment = fragment;
        mMinecraftClient.displayGuiScreen(new MuiScreen());
    }

    /**
     * Close GUI screen, put UI window and all windows above it into recycler
     *
     * @see #recycleWindows()
     */
    public void closeGui() {
        mMinecraftClient.displayGuiScreen(null);
    }

    /**
     * Register an activity factory relative to a menu type to open the gui screen.
     *
     * @param type    registered menu type
     * @param factory activity factory
     * @param <T>     menu type
     * @see net.minecraftforge.fml.network.NetworkHooks#openGui(ServerPlayerEntity, INamedContainerProvider, Consumer)
     */
    public <T extends Container> void registerFactory(
            @Nonnull ContainerType<? extends T> type, @Nonnull Function<T, Fragment> factory) {
        ScreenManager.registerFactory(type, getFactory(factory));
    }

    @Nonnull
    private <T extends Container> ScreenManager.IScreenFactory<T, MuiMenuScreen<T>> getFactory(
            @Nonnull Function<T, Fragment> factory) {
        return (container, inventory, title) -> {
            this.fragment = factory.apply(container);
            return new MuiMenuScreen<>(container, inventory, title);
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
    @Deprecated
    public void openPopup(IModule popup, boolean refresh) {
        /*if (root == null) {
            ModernUI.LOGGER.fatal(MARKER, "#openPopup() shouldn't be called when there's NO gui open");
            return;
        }*/
        if (this.popup != null) {
            ModernUI.LOGGER.warn(MARKER, "#openPopup() shouldn't be called when there's already a popup, the previous one has been overwritten");
        }
        if (refresh) {
            this.screenMouseMove(-1, -1);
        }
        this.popup = popup;
        this.popup.resize(width, height);
    }

    /**
     * Close current popup
     */
    public void closePopup() {
        if (popup != null) {
            popup = null;
        }
    }

    /**
     * Called when open a modern screen, or back to the modern screen
     *
     * @param mui    modern screen or modern container screen
     * @param width  scaled game main window width
     * @param height scaled game main window height
     */
    void prepareWindows(@Nonnull Screen mui, int width, int height) {
        modernScreen = mui;

        // init view of this UI
        if (appWindow.mView == null) {
            if (fragment == null) {
                ModernUI.LOGGER.fatal(MARKER, "Fragment can't be null when opening a gui screen");
                closeGui();
                return;
            }
            View view = fragment.onCreateView();
            if (view == null) {
                ModernUI.LOGGER.fatal(MARKER, "The view created from the main fragment shouldn't be null");
                closeGui();
                return;
            }
            appWindow.install(view);
        }

        resizeWindows(width, height);
    }

    /**
     * Inner method, do not call
     */
    @SubscribeEvent
    void globalGuiOpen(@Nonnull GuiOpenEvent event) {
        screenToOpen = event.getGui();

        // create canvas, also font renderer
        if (canvas == null) {
            canvas = Canvas.getInstance();
        }

        if (screenToOpen == null) {
            recycleWindows();
            return;
        }

        if (modernScreen != screenToOpen &&
                ((screenToOpen instanceof MuiScreen) || (screenToOpen instanceof MuiMenuScreen<?>))) {
            if (view != null) {
                // prevent repeated opening sometimes
                event.setCanceled(true);
                return;
            }
            ticks = 0;
            timeMillis = 0;
        }
        // hotfix 1.5.2, but there's no way to work with screens that will pause game
        if (modernScreen != screenToOpen && modernScreen != null) {
            screenMouseMove(-1, -1);
        }
        // for non-modern-ui screens
        if (modernScreen == null) {
            ticks = 0;
            timeMillis = 0;
        }
    }

    /**
     * Get current open screen differently from Minecraft's,
     * which will only return Modern UI's screen or null
     *
     * @return open modern screen
     * @see Minecraft#currentScreen
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
     * Post a task that will run on next pre-tick after delayed ticks
     *
     * @param runnable     runnable task
     * @param delayedTicks delayed ticks to run the task
     */
    public void postTask(@Nonnull Runnable runnable, int delayedTicks) {
        if (delayedTicks <= 0) {
            runnable.run();
            return;
        }
        tasks.add(new DelayedTask(runnable, delayedTicks));
    }

    private void setMousePos(double mouseX, double mouseY) {
        /*this.mouseX = mouseEvent.x = mouseX;
        this.mouseY = mouseEvent.y = mouseY;*/
    }

    // OS EVENT HANDLE IN SCREEN

    void screenMouseMove(double mouseX, double mouseY) {
        setMousePos(mouseX, mouseY);
        MotionEvent event = motionEvent;
        //event.action = MotionEvent.ACTION_MOVE;
        /*List<ViewRootImpl> windows = this.windows;
        boolean anyHovered = false;
        for (int i = windows.size() - 1; i >= 0; i--) {
            if (!anyHovered && windows.get(i).onMouseEvent(event)) {
                anyHovered = true;
            } else {
                windows.get(i).ensureMouseHoverExit();
            }
        }*/
        appWindow.onMouseEvent(event);
        cursorRefreshRequested = false;
    }

    boolean screenMouseDown(double mouseX, double mouseY, int mouseButton) {
        ModernUI.LOGGER.debug("MouseButtonDown: {}", mouseButton);
        setMousePos(mouseX, mouseY);
        MotionEvent event = motionEvent;
        event.button = mouseButton;
        //List<ViewRootImpl> windows = this.windows;
        boolean handled = false;
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT && lastLmTick >= 0 && ticks - lastLmTick < 6) {
            //event.action = MotionEvent.ACTION_DOUBLE_CLICK;
            /*for (int i = windows.size() - 1; i >= 0; i--) {
                if (windows.get(i).onMouseEvent(event)) {
                    return true;
                }
            }*/
            if (lastLmView != null && lastLmView.isMouseHovered() && lastLmView.onMouseEvent(event)) {
                handled = true;
            }
        }
        lastLmView = null;
        if (handled) {
            return true;
        }
        //event.action = MotionEvent.ACTION_PRESS;
        return appWindow.onMouseEvent(event);
        /*for (int i = windows.size() - 1; i >= 0; i--) {
            if (windows.get(i).onMouseEvent(event)) {
                return true;
            }
        }*/
        /*if (popup != null) {
            return popup.mouseClicked(mouseX, mouseY, mouseButton);
        }*/
        /*if (mHovered != null) {
            IViewParent parent;
            View view;
            double viewMX = getViewMouseX(mHovered);
            double viewMY = getViewMouseY(mHovered);
            if (mouseButton == 0) {
                int delta = ticks - lastDClickTick;
                if (delta < 10) {
                    lastDClickTick = Integer.MIN_VALUE;
                    if (mHovered.onMouseDoubleClicked(viewMX, viewMY)) {
                        return true;
                    }
                    parent = mHovered.getParent();
                    double viewMX2 = viewMX;
                    double viewMY2 = viewMY;
                    while (parent instanceof View) {
                        view = (View) parent;
                        viewMX2 -= parent.getScrollX();
                        viewMY2 -= parent.getScrollY();
                        if (view.onMouseDoubleClicked(viewMX2, viewMY2)) {
                            return true;
                        }
                        parent = parent.getParent();
                    }
                } else {
                    lastDClickTick = ticks;
                }
            }
            *//*if (mHovered.mouseClicked(viewMX, viewMY, mouseButton)) {
                return true;
            }*//*
            parent = mHovered.getParent();
            while (parent instanceof View) {
                view = (View) parent;
                viewMX -= parent.getScrollX();
                viewMY -= parent.getScrollY();
                *//*if (view.mouseClicked(viewMX, viewMY, mouseButton)) {
                    return true;
                }*//*
                parent = parent.getParent();
            }
        }*/
    }

    boolean screenMouseUp(double mouseX, double mouseY, int mouseButton) {
        setMousePos(mouseX, mouseY);
        MotionEvent event = motionEvent;
        //event.action = MotionEvent.ACTION_RELEASE;
        event.button = mouseButton;
        boolean dCheck = false;
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT && lastLmTick < 0) {
            dCheck = event.pressMap.get(mouseButton) != null;
        } else {
            lastLmTick = Integer.MIN_VALUE;
        }
        //List<ViewRootImpl> windows = this.windows;
        boolean handled = false;
        /*for (int i = windows.size() - 1; i >= 0; i--) {
            if (windows.get(i).onMouseEvent(event)) {
                handled = true;
                break;
            }
        }*/
        if (appWindow.onMouseEvent(event)) {
            handled = true;
        }
        if (dCheck && event.clicked != null) {
            lastLmTick = ticks;
        } else {
            lastLmTick = Integer.MIN_VALUE;
        }
        lastLmView = event.clicked;
        event.clicked = null;
        if (handled) {
            return true;
        }
        /*if (popup != null) {
            return popup.mouseReleased(mouseX, mouseY, mouseButton);
        }*/
        if (mDragging != null) {
            setDragging(null);
            return true;
        }
        /*if (mHovered != null) {
            double viewMX = getViewMouseX(mHovered);
            double viewMY = getViewMouseY(mHovered);
            if (mHovered.onMouseReleased(viewMX, viewMY, mouseButton)) {
                return true;
            }
            IViewParent parent = mHovered.getParent();
            View view;
            while (parent instanceof View) {
                view = (View) parent;
                viewMX -= parent.getScrollX();
                viewMY -= parent.getScrollY();
                if (view.onMouseReleased(viewMX, viewMY, mouseButton)) {
                    return true;
                }
                parent = parent.getParent();
            }
        }*/
        return false;//root.mouseReleased(mouseX, mouseY, mouseButton);
    }

    //TODO
    boolean screenMouseDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        setMousePos(mouseX, mouseY);
        /*if (popup != null) {
            return popup.mouseDragged(mouseX, mouseY, deltaX, deltaY);
        }*/
        if (mDragging != null) {
            return mDragging.onMouseDragged(getViewMouseX(mDragging), getViewMouseY(mDragging), deltaX, deltaY);
        }
        return false;
    }

    boolean screenMouseScroll(double mouseX, double mouseY, double amount) {
        ModernUI.LOGGER.debug(MARKER, "SHIGAO {}", amount);
        setMousePos(mouseX, mouseY);
        MotionEvent event = motionEvent;
        //event.action = MotionEvent.ACTION_SCROLL;
        event.scrollDelta = amount;
        /*List<ViewRootImpl> windows = this.windows;
        for (int i = windows.size() - 1; i >= 0; i--) {
            if (windows.get(i).onMouseEvent(event)) {
                return true;
            }
        }*/
        return appWindow.onMouseEvent(event);
        /*if (popup != null) {
            return popup.mouseScrolled(mouseX, mouseY, amount);
        }*/
        /*if (mHovered != null) {
            double viewMX = getViewMouseX(mHovered);
            double viewMY = getViewMouseY(mHovered);
            if (mHovered.onMouseScrolled(viewMX, getViewMouseY(mHovered), amount)) {
                return true;
            }
            IViewParent parent = mHovered.getParent();
            View view;
            while (parent != null) {
                view = (View) parent;
                viewMX -= parent.getScrollX();
                viewMY -= parent.getScrollY();
                if (view.onMouseScrolled(mouseX, mouseY, amount)) {
                    return true;
                }
                parent = parent.getParent();
            }
        }*/
    }

    boolean screenKeyDown(int keyCode, int scanCode, int modifiers) {
        /*if (popup != null) {
            return popup.keyPressed(keyCode, scanCode, modifiers);
        }*/
        //ModernUI.LOGGER.info("KeyDown{keyCode:{}, scanCode:{}, mod:{}}", keyCode, scanCode, modifiers);
        if (mKeyboard != null) {
            return mKeyboard.onKeyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    boolean screenKeyUp(int keyCode, int scanCode, int modifiers) {
        /*if (popup != null) {
            return popup.keyReleased(keyCode, scanCode, modifiers);
        }*/
        if (mKeyboard != null) {
            return mKeyboard.onKeyReleased(keyCode, scanCode, modifiers);
        }
        return false;//root.keyReleased(keyCode, scanCode, modifiers);
    }

    boolean sCharTyped(char codePoint, int modifiers) {
        /*if (popup != null) {
            return popup.charTyped(codePoint, modifiers);
        }*/
        if (mKeyboard != null) {
            return mKeyboard.onCharTyped(codePoint, modifiers);
        }
        return false;//root.charTyped(codePoint, modifiers);
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

    void performDrag(int action) {

    }

    /*private void mouseMoved() {
        if (view != null && !view.updateMouseHover(mouseX, mouseY)) {
            setHovered(null);
        }
    }*/

    /**
     * Raw draw method, draw entire UI
     */
    void draw() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        /*canvas.moveToZero();
        canvas.setColor(0, 0, 0, 51);
        canvas.drawRect(0, 0, width, height);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        canvas.setColor(255, 255, 255, (int) (255 * (255.0f / (255 - 51) - 1)));
        canvas.drawRect(60, 60, width - 60, height - 60);
        RenderSystem.defaultBlendFunc();*/

        canvas.setDrawingTime(timeMillis);

        appWindow.onDraw(canvas);
        /*if (popup != null) {
            popup.draw(drawTime);
        }*/
        //UIEditor.INSTANCE.draw(canvas);
        //TODO use shader
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.0f);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    @SubscribeEvent
    void gRenderGameOverlay(@Nonnull RenderGameOverlayEvent.Pre event) {
        switch (event.getType()) {
            case CROSSHAIRS:
                if (modernScreen != null) {
                    event.setCanceled(true);
                }
                break;
            case HOTBAR:
            case HELMET:
                // hotfix 1.16 for BlurHandler shader
                RenderSystem.enableTexture();
                break;
            case HEALTH:
                if (ModernUI.isDeveloperMode())
                    TestHUD.drawHUD(canvas);
                break;
        }
    }

    /**
     * Called when game window size changed, used to re-layout the UI
     *
     * @param width  scaled game window width
     * @param height scaled game window height
     */
    void resizeWindows(int width, int height) {
        this.width = width;
        this.height = height;
        double scale = mMinecraftClient.getMainWindow().getGuiScaleFactor();
        eventDispatcher.mouseX = (mMinecraftClient.mouseHelper.getMouseX() / scale);
        eventDispatcher.mouseY = (mMinecraftClient.mouseHelper.getMouseY() / scale);
        layoutWindows(true);
    }

    /**
     * Layout all windows
     */
    private void layoutWindows(boolean forceLayout) {
        long startTime = System.nanoTime();

        int widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.Mode.EXACTLY);
        int heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.Mode.EXACTLY);

        appWindow.performLayout(widthSpec, heightSpec, forceLayout);

        if (ModernUI.isDeveloperMode()) {
            ModernUI.LOGGER.debug(MARKER, "Layout performed in {} \u03bcs", (System.nanoTime() - startTime) / 1000.0f);
        }
        //screenMouseMove(mouseX, mouseY);
    }

    void recycleWindows() {
        // Hotfix 1.4.7
        if (screenToOpen == null) {
            animations.clear();
            tasks.clear();
            view = null;
            popup = null;
            fragment = null;
            modernScreen = null;
            appWindow.mView = null;
            lastLmTick = Integer.MIN_VALUE;
            lastLmView = null;
            lastLayoutTime = 0;
            layoutRequested = false;
            motionEvent.pressMap.clear();
            UIEditor.INSTANCE.setHoveredWidget(null);
            UITools.useDefaultCursor();
            // Hotfix 1.5.8
            mMinecraftClient.keyboardListener.enableRepeatEvents(false);
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
        /*if (popup != null) {
            popup.tick(ticks);
        }*/
        if (view != null) {
            view.tick(ticks);
        }
        appWindow.tick(ticks);
        // view ticking is always performed before tasks
        if (!tasks.isEmpty()) {
            Iterator<DelayedTask> iterator = tasks.iterator();
            DelayedTask task;
            while (iterator.hasNext()) {
                task = iterator.next();
                task.tick(ticks);
                if (task.shouldRemove()) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Inner method
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    void globalRenderTick(@Nonnull TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            // ticks to millis, the Timer in Minecraft is different from that in Event when game paused
            timeMillis = (long) ((ticks + mMinecraftClient.getRenderPartialTicks()) * 50.0f);

            for (Animation animation : animations) {
                animation.update(timeMillis);
            }
        } else {
            // remove animations from loop on end
            if (!animations.isEmpty()) {
                animations.removeIf(Animation::shouldRemove);
            }

            // layout after updating animations and before drawing
            if (layoutRequested || cursorRefreshRequested) {
                // fixed at 40Hz
                if (timeMillis - lastLayoutTime >= 25) {
                    lastLayoutTime = timeMillis;
                    if (layoutRequested) {
                        layoutWindows(false);
                    } else {
                        //screenMouseMove(mouseX, mouseY);
                    }
                }
            }
        }
    }

    /**
     * Get elapsed time in UI window, update every frame
     *
     * @return drawing time in milliseconds
     */
    public long getDrawingTime() {
        return timeMillis;
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
    public int getScreenWidth() {
        return width;
    }

    /**
     * Get scaled UI screen height which is equal to game main window height
     *
     * @return window height
     */
    public int getScreenHeight() {
        return height;
    }

    /**
     * Get scaled mouse X position on screen
     *
     * @return mouse x
     */
    public double getMouseX() {
        return eventDispatcher.mouseX;
    }

    /**
     * Get scaled mouse Y position on screen
     *
     * @return mouse y
     */
    public double getMouseY() {
        return eventDispatcher.mouseY;
    }

    /**
     * Get logical mouse x for a view, generally used by system
     *
     * @param view view
     * @return relative mouse x
     */
    public double getViewMouseX(@Nonnull View view) {
        IViewParent parent = view.getParent();
        double mouseX = eventDispatcher.mouseX;

        while (parent != null) {
            mouseX += parent.getScrollX();
            parent = parent.getParent();
        }

        return mouseX;
    }

    /**
     * Get logical mouse y for a view, generally used by system
     *
     * @param view view
     * @return relative mouse y
     */
    public double getViewMouseY(@Nonnull View view) {
        IViewParent parent = view.getParent();
        double mouseY = eventDispatcher.mouseY;

        while (parent != null) {
            mouseY += parent.getScrollY();
            parent = parent.getParent();
        }

        return mouseY;
    }

    public double getGuiScale() {
        return mMinecraftClient.getMainWindow().getGuiScaleFactor();
    }

    /**
     * Get main view of current UI
     *
     * @return main view
     */
    public View getMainView() {
        return appWindow.mView;
    }

    public void requestCursorRefresh() {
        cursorRefreshRequested = true;
    }

    /**
     * Internal method, to tell the manager the most child view hovered
     *
     * @param view the most child view hovered
     */
    void setHovered(@Nullable View view) {
        mHovered = view;
    }

    @Nullable
    public View getHovered() {
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
            mMinecraftClient.keyboardListener.enableRepeatEvents(view != null);
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

    final class InputHandler {

        private int mButtonState;

        void onHoverMove(double x, double y) {

        }
    }
}
