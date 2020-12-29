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
import icyllis.modernui.graphics.BlurHandler;
import icyllis.modernui.graphics.math.Point;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.plugin.event.OpenMenuEvent;
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.system.mixin.MixinMouseHandler;
import icyllis.modernui.test.TestHUD;
import icyllis.modernui.test.discard.IModule;
import icyllis.modernui.widget.FrameLayout;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * UI system service, manages everything related to UI in Modern UI.
 */
@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
public final class UIManager {

    // the global instance
    private static UIManager instance;

    // logger marker
    public static final Marker MARKER = MarkerManager.getMarker("UIManager");

    // minecraft client
    private final Minecraft minecraft = Minecraft.getInstance();

    // registered menu screens
    //private final Map<ContainerType<?>, Function<? extends Container, ApplicationUI>> mScreenRegistry = new HashMap<>();

    // application window
    private final ViewRootImpl mAppWindow = new ViewRootImpl(this);

    // the top-level view of the window
    private final DecorView mDecorView = new DecorView();

    // true if there will be no screen to open
    private boolean mCloseScreen;

    // indicates whether the current screen is a Modern UI screen, also a callback to the screen
    @Nullable
    private IMuiScreen mMuiScreen;

    // application UI used to send lifecycle events
    private ApplicationUI mApplicationUI;

    // main fragment of a UI
    //private Fragment fragment;

    // main UI view that created from main fragment
    //private View view;

    //private IModule popup;

    // scaled game window width / height, the pixels that are less than 1.0
    // are not considered as a valid area on screen
    private int mWidth;
    private int mHeight;

    // a list of animations in render loop
    private final List<Animation> animations = new ArrayList<>();

    // a list of UI tasks
    private final List<DelayedTask> tasks = new CopyOnWriteArrayList<>();

    // elapsed ticks from a gui open, update every tick, 20 = 1 second
    private int mTicks = 0;

    // elapsed time from a gui open in milliseconds, update every frame
    private long mDrawingTimeMillis;

    // the canvas to draw things shared in all views and drawables
    // lazy loading because this class is loaded before GL initialization
    // will be init when Minecraft finished loading, and open MainMenuScreen
    // also init font renderer when loaded
    private Canvas mCanvas;

    // the most child hovered view, render at the top of other hovered ancestor views
    @Nullable
    private View mHovered;

    // focused view
    @Nullable
    private View mDragging;
    @Nullable
    private View mKeyboard;

    // current scaled cursor position on the gui screen
    private double mCursorX;
    private double mCursorY;

    // captured input event values
    private double mScrollX;
    private double mScrollY;

    // schedule layout on next frame
    boolean mLayoutRequested = false;

    // schedule a cursor event on next tick due to scroll amount changed
    boolean mPendingRepostCursorEvent = false;

    // to fix layout freq at 40Hz at most
    private long mLastLayoutTime = 0;

    // drag event instance, also marks whether a drag and drop operation is ongoing
    @Nullable
    DragEvent dragEvent;

    // drag shadow
    View.DragShadow dragShadow;

    // drag shadow center for render offset
    Point dragShadowCenter;

    {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(BlurHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(UIEditor.INSTANCE);
    }

    private UIManager() {
        mAppWindow.setView(mDecorView);
    }

    /**
     * Returns the global UI service
     *
     * @return the instance
     */
    //TODO remove this method
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

    // internal method
    public static void initRenderer() {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        if (instance.mCanvas == null) {
            instance.mCanvas = Canvas.getInstance();
        } else {
            throw new IllegalStateException("Already initialized");
        }
    }

    /**
     * Open an application UI and create views
     *
     * @param applicationUI the application user interface
     * @see #start(IMuiScreen, int, int)
     */
    public void openGUI(@Nonnull ApplicationUI applicationUI) {
        mApplicationUI = applicationUI;
        minecraft.displayGuiScreen(new MMainScreen(this));
    }

    /**
     * Close all screens and destroy current application UI
     *
     * @see #stop()
     */
    public void closeGUI() {
        minecraft.displayGuiScreen(null);
    }

    // Internal method
    public void openGUI(@Nonnull ClientPlayerEntity player, int containerId, int menuId, @Nonnull PacketBuffer buffer) {
        ContainerType<?> type = Registry.MENU.getByValue(menuId);
        if (type == null) {
            ModernUI.LOGGER.warn(MARKER, "Trying to open invalid screen for menu id: {}", menuId);
        } else {
            Container menu = type.create(containerId, player.inventory, buffer);
            //noinspection ConstantConditions
            if (menu == null) {
                ModernUI.LOGGER.warn(MARKER, "Failed to create menu for type: {}", Registry.MENU.getKey(type));
            } else {
                OpenMenuEvent event = new OpenMenuEvent(menu);
                ModernUI.post(event);
                ApplicationUI applicationUI = event.getApplicationUI();
                if (applicationUI != null) {
                    mApplicationUI = applicationUI;
                    player.openContainer = menu;
                    minecraft.displayGuiScreen(new MMenuScreen<>(menu, player.inventory, this));
                }
            }
        }
    }

    /*@Nonnull
    private <T extends Container> ScreenManager.IScreenFactory<T, MuiMenuScreen<T>> getFactory(
            @Nonnull Function<T, Fragment> factory) {
        return (container, inventory, title) -> {
            this.fragment = factory.apply(container);
            return new MuiMenuScreen<>(container, inventory, title, this);
        };
    }*/

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
        throw new UnsupportedOperationException();
        /*if (root == null) {
            ModernUI.LOGGER.fatal(MARKER, "#openPopup() shouldn't be called when there's NO gui open");
            return;
        }*/
        /*if (this.popup != null) {
            ModernUI.LOGGER.warn(MARKER, "#openPopup() shouldn't be called when there's already a popup, the previous one has been overwritten");
        }
        if (refresh) {
            this.screenMouseMove(-1, -1);
        }
        this.popup = popup;
        this.popup.resize(width, height);*/
    }

    /**
     * Close current popup
     */
    @Deprecated
    public void closePopup() {
        throw new UnsupportedOperationException();
        /*if (popup != null) {
            popup = null;
        }*/
    }

    void setContentView(View view, FrameLayout.LayoutParams params) {
        mDecorView.removeAllViews();
        mDecorView.addView(view, params);
    }

    /**
     * Called when open a mui screen, or back to the mui screen
     *
     * @param screen the current Screen
     * @param width  scaled game main window width
     * @param height scaled game main window height
     */
    void start(@Nonnull IMuiScreen screen, int width, int height) {
        if (mMuiScreen == null) {
            mApplicationUI.window = this;
            mApplicationUI.onCreate();
        }
        mMuiScreen = screen;

        // init view of this UI
        /*if (mAppWindow.mView == null) {
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
            mAppWindow.setView(view);
        }*/

        resize(width, height);
    }

    @SubscribeEvent
    void onGuiOpen(@Nonnull GuiOpenEvent event) {
        final Screen nextScreen = event.getGui();
        mCloseScreen = nextScreen == null;

        // create canvas, init render engine, also font engine
        if (mCanvas == null) {
            mCanvas = Canvas.getInstance();
        }

        if (mCloseScreen) {
            stop();
            return;
        }

        if (mMuiScreen != nextScreen && nextScreen instanceof IMuiScreen) {
            mTicks = 0;
            mDrawingTimeMillis = 0;
        }
        if (mMuiScreen != nextScreen && mMuiScreen != null) {
            onCursorEvent(-1, -1);
        }
        // for non-mui screens
        if (mMuiScreen == null) {
            mTicks = 0;
            mDrawingTimeMillis = 0;
        }
    }

    /*
     * Get current open screen differently from Minecraft's,
     * which will only return Modern UI's screen or null
     *
     * @return open modern screen
     * @see Minecraft#currentScreen
     */
    /*@Nullable
    public Screen getModernScreen() {
        return mMuiScreen;
    }*/

    public boolean hasRunningUI() {
        return mMuiScreen != null;
    }

    @Nullable
    public ApplicationUI getApplicationUI() {
        return mApplicationUI;
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

    /*private void setMousePos(double mouseX, double mouseY) {
     *//*this.mouseX = mouseEvent.x = mouseX;
        this.mouseY = mouseEvent.y = mouseY;*//*
    }

    @Deprecated
    void screenMouseMove(double mouseX, double mouseY) {
        setMousePos(mouseX, mouseY);
        MotionEvent event = null;
        //event.action = MotionEvent.ACTION_MOVE;
        *//*List<ViewRootImpl> windows = this.windows;
        boolean anyHovered = false;
        for (int i = windows.size() - 1; i >= 0; i--) {
            if (!anyHovered && windows.get(i).onMouseEvent(event)) {
                anyHovered = true;
            } else {
                windows.get(i).ensureMouseHoverExit();
            }
        }*//*
        mAppWindow.onMouseEvent(event);
        cursorRefreshRequested = false;
    }*/

    /**
     * From screen
     *
     * @see org.lwjgl.glfw.GLFWCursorPosCallbackI
     * @see net.minecraft.client.MouseHelper
     * @see MMainScreen
     * @see MMenuScreen
     */
    void onCursorEvent(double cursorX, double cursorY) {
        mCursorX = cursorX;
        mCursorY = cursorY;
        final long now = Util.nanoTime();
        MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_HOVER_MOVE,
                (float) cursorX, (float) cursorY, 0);
        mAppWindow.onInputEvent(event);
        mPendingRepostCursorEvent = false;
    }

    /**
     * Intercept the Forge event
     *
     * @see org.lwjgl.glfw.GLFWMouseButtonCallbackI
     * @see net.minecraft.client.MouseHelper
     * @see net.minecraftforge.client.event.InputEvent
     */
    @SubscribeEvent
    void onRawMouseButton(InputEvent.RawMouseEvent event) {
        // We should ensure (overlay == null && screen != null)
        // and the screen must be a mui screen
        if (minecraft.loadingGui == null && mMuiScreen != null) {
            ModernUI.LOGGER.debug(MARKER, "Button: {} {} {}", event.getButton(), event.getAction(), event.getMods());
        }
    }

    /**
     * From mouse or touchpad, we need the horizontal scroll offset
     *
     * @see org.lwjgl.glfw.GLFWScrollCallbackI
     * @see net.minecraft.client.MouseHelper
     * @see MixinMouseHandler
     */
    boolean onScrollEvent() {
        final long now = Util.nanoTime();
        MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_SCROLL,
                (float) mCursorX, (float) mCursorY, 0);
        event.setRawAxisValue(MotionEvent.AXIS_HSCROLL, (float) mScrollX);
        event.setRawAxisValue(MotionEvent.AXIS_VSCROLL, (float) mScrollY);
        boolean handled = mAppWindow.onInputEvent(event);
        if (handled) {
            mPendingRepostCursorEvent = true;
        }
        return handled;
    }

    // Internal method
    public void onEarlyScrollCallback(double scrollX, double scrollY) {
        mScrollX = scrollX;
        mScrollY = scrollY;
    }

    public void repostCursorEvent() {
        mPendingRepostCursorEvent = true;
    }

    /*@Deprecated
    boolean screenMouseDown(double mouseX, double mouseY, int mouseButton) {
        setMousePos(mouseX, mouseY);
        MotionEvent event = null;
        event.button = mouseButton;
        //List<ViewRootImpl> windows = this.windows;
        boolean handled = false;
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT && lastLmTick >= 0 && ticks - lastLmTick < 6) {
            //event.action = MotionEvent.ACTION_DOUBLE_CLICK;
            *//*for (int i = windows.size() - 1; i >= 0; i--) {
                if (windows.get(i).onMouseEvent(event)) {
                    return true;
                }
            }*//*
            if (lastLmView != null && lastLmView.isMouseHovered() && lastLmView.onGenericMotionEvent(event)) {
                handled = true;
            }
        }
        lastLmView = null;
        if (handled) {
            return true;
        }
        //event.action = MotionEvent.ACTION_PRESS;
        return mAppWindow.onMouseEvent(event);
        *//*for (int i = windows.size() - 1; i >= 0; i--) {
            if (windows.get(i).onMouseEvent(event)) {
                return true;
            }
        }*//*
     *//*if (popup != null) {
            return popup.mouseClicked(mouseX, mouseY, mouseButton);
        }*//*
     *//*if (mHovered != null) {
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
            *//**//*if (mHovered.mouseClicked(viewMX, viewMY, mouseButton)) {
                return true;
            }*//**//*
            parent = mHovered.getParent();
            while (parent instanceof View) {
                view = (View) parent;
                viewMX -= parent.getScrollX();
                viewMY -= parent.getScrollY();
                *//**//*if (view.mouseClicked(viewMX, viewMY, mouseButton)) {
                    return true;
                }*//**//*
                parent = parent.getParent();
            }
        }*//*
    }

    @Deprecated
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
        *//*for (int i = windows.size() - 1; i >= 0; i--) {
            if (windows.get(i).onMouseEvent(event)) {
                handled = true;
                break;
            }
        }*//*
        if (mAppWindow.onMouseEvent(event)) {
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
        *//*if (popup != null) {
            return popup.mouseReleased(mouseX, mouseY, mouseButton);
        }*//*
        if (mDragging != null) {
            setDragging(null);
            return true;
        }
        *//*if (mHovered != null) {
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
        }*//*
        return false;//root.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Deprecated
    boolean screenMouseDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        setMousePos(mouseX, mouseY);
        *//*if (popup != null) {
            return popup.mouseDragged(mouseX, mouseY, deltaX, deltaY);
        }*//*
        if (mDragging != null) {
            return mDragging.onMouseDragged(getViewMouseX(mDragging), getViewMouseY(mDragging), deltaX, deltaY);
        }
        return false;
    }

    @Deprecated
    boolean screenMouseScroll(double mouseX, double mouseY, double amount) {
        setMousePos(mouseX, mouseY);
        MotionEvent event = motionEvent;
        //event.action = MotionEvent.ACTION_SCROLL;
        event.scrollDelta = amount;
        *//*List<ViewRootImpl> windows = this.windows;
        for (int i = windows.size() - 1; i >= 0; i--) {
            if (windows.get(i).onMouseEvent(event)) {
                return true;
            }
        }*//*
        return mAppWindow.onMouseEvent(event);
        *//*if (popup != null) {
            return popup.mouseScrolled(mouseX, mouseY, amount);
        }*//*
     *//*if (mHovered != null) {
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
        }*//*
    }*/

    boolean screenKeyDown(int keyCode, int scanCode, int modifiers) {
        /*if (popup != null) {
            return popup.keyPressed(keyCode, scanCode, modifiers);
        }*/
        ModernUI.LOGGER.debug(MARKER, "KeyDown{keyCode:{}, scanCode:{}, mods:{}}", keyCode, scanCode, modifiers);
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

    boolean onBackPressed() {
        /*if (popup != null) {
            closePopup();
            return true;
        }*/
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
     * Runtime rendering
     */
    void render() {
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

        mCanvas.setDrawingTime(mDrawingTimeMillis);

        mAppWindow.onDraw(mCanvas);
        /*if (popup != null) {
            popup.draw(drawTime);
        }*/
        //UIEditor.INSTANCE.draw(canvas);
        //TODO use shader uniforms
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.0f);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    @SubscribeEvent
    void onRenderGameOverlay(@Nonnull RenderGameOverlayEvent.Pre event) {
        switch (event.getType()) {
            case CROSSHAIRS:
                if (mMuiScreen != null)
                    event.setCanceled(true);
                break;
            case ALL:
                // hotfix 1.16 vanilla, using shader makes TEXTURE_2D disabled
                RenderSystem.enableTexture();
                break;
            case HEALTH:
                if (ModernUI.isDeveloperMode())
                    TestHUD.drawHUD(mCanvas);
                break;
        }
    }

    /**
     * Called when game window size changed, used to re-layout the UI
     *
     * @param width  scaled game window width
     * @param height scaled game window height
     */
    void resize(int width, int height) {
        mWidth = width;
        mHeight = height;
        final MainWindow window = minecraft.getMainWindow();
        mCursorX = minecraft.mouseHelper.getMouseX() * (double) window.getScaledWidth() / (double) window.getWidth();
        mCursorY = minecraft.mouseHelper.getMouseY() * (double) window.getScaledHeight() / (double) window.getHeight();
        doLayout();
    }

    /**
     * Directly layout UI window
     */
    private void doLayout() {
        long startTime = System.nanoTime();

        int widthSpec = MeasureSpec.makeMeasureSpec(mWidth, MeasureSpec.Mode.EXACTLY);
        int heightSpec = MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.Mode.EXACTLY);

        mAppWindow.performLayout(widthSpec, heightSpec);

        if (ModernUI.isDeveloperMode()) {
            ModernUI.LOGGER.debug(MARKER, "Layout performed in {} \u03bcs", (System.nanoTime() - startTime) / 1000.0f);
        }
        onCursorEvent(mCursorX, mCursorY);
        mLayoutRequested = false;
    }

    void stop() {
        // Hotfix 1.4.7
        if (mCloseScreen) {
            animations.clear();
            tasks.clear();
            mMuiScreen = null;
            if (mApplicationUI != null) {
                mApplicationUI.window = null;
                mApplicationUI = null;
            }
            mLastLayoutTime = 0;
            mLayoutRequested = false;
            UIEditor.INSTANCE.setHoveredWidget(null);
            mDecorView.removeAllViews();
            UITools.useDefaultCursor();
            // Hotfix 1.5.8
            minecraft.keyboardListener.enableRepeatEvents(false);
        }
    }

    @SubscribeEvent
    void onClientTick(@Nonnull TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ++mTicks;
            mAppWindow.tick(mTicks);
            // view ticking is always performed before tasks
            if (!tasks.isEmpty()) {
                Iterator<DelayedTask> iterator = tasks.iterator();
                DelayedTask task;
                while (iterator.hasNext()) {
                    task = iterator.next();
                    task.tick(mTicks);
                    if (task.shouldRemove()) {
                        iterator.remove();
                    }
                }
            }
        } else {
            if (mPendingRepostCursorEvent) {
                onCursorEvent(mCursorX, mCursorY);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    void onRenderTick(@Nonnull TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            // ticks to millis, the Timer in Minecraft is different from that in Event when game paused
            mDrawingTimeMillis = (long) ((mTicks + minecraft.getRenderPartialTicks()) * 50.0f);

            for (Animation animation : animations) {
                animation.update(mDrawingTimeMillis);
            }
        } else {
            // remove animations from loop on end
            if (!animations.isEmpty()) {
                animations.removeIf(Animation::shouldRemove);
            }

            // layout after updating animations and before drawing
            if (mLayoutRequested) {
                // fixed at 40Hz
                if (mDrawingTimeMillis - mLastLayoutTime >= 25) {
                    mLastLayoutTime = mDrawingTimeMillis;
                    doLayout();
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
        return mDrawingTimeMillis;
    }

    /**
     * Get elapsed ticks from a gui open, update every tick, 20 = 1 second
     *
     * @return elapsed ticks
     */
    public int getElapsedTicks() {
        return mTicks;
    }

    /**
     * Get scaled UI screen width which is equal to game main window width
     *
     * @return window width
     */
    public int getScreenWidth() {
        return mWidth;
    }

    /**
     * Get scaled UI screen height which is equal to game main window height
     *
     * @return window height
     */
    public int getScreenHeight() {
        return mHeight;
    }

    /**
     * Get scaled mouse X position on screen
     *
     * @return mouse x
     */
    public double getCursorX() {
        return mCursorX;
    }

    /**
     * Get scaled mouse Y position on screen
     *
     * @return mouse y
     */
    public double getCursorY() {
        return mCursorY;
    }

    /**
     * Get logical mouse x for a view, generally used by system
     *
     * @param view view
     * @return relative mouse x
     */
    public double getViewMouseX(@Nonnull View view) {
        IViewParent parent = view.getParent();
        double mouseX = mCursorX;

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
        double mouseY = mCursorY;

        while (parent != null) {
            mouseY += parent.getScrollY();
            parent = parent.getParent();
        }

        return mouseY;
    }

    public double getGuiScale() {
        return minecraft.getMainWindow().getGuiScaleFactor();
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
}
