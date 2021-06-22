/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.screen;

import com.ibm.icu.util.ULocale;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import icyllis.modernui.ModernUI;
import icyllis.modernui.animation.AnimationHandler;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.forge.ModernUIForge;
import icyllis.modernui.forge.OpenMenuEvent;
import icyllis.modernui.graphics.*;
import icyllis.modernui.graphics.textmc.ModernFontRenderer;
import icyllis.modernui.graphics.textmc.TextLayoutProcessor;
import icyllis.modernui.math.Point;
import icyllis.modernui.platform.RenderCore;
import icyllis.modernui.test.TestHUD;
import icyllis.modernui.test.TestPauseUI;
import icyllis.modernui.util.TimedTask;
import icyllis.modernui.view.*;
import icyllis.modernui.widget.FrameLayout;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.LongConsumer;

/**
 * Manage UI thread and connect Minecraft to Modern UI view system.
 */
@NotThreadSafe
@SuppressWarnings("unused")
public final class UIManager {

    // the global instance
    private static volatile UIManager instance;

    // logger marker
    public static final Marker MARKER = MarkerManager.getMarker("UIManager");

    // minecraft client
    private final Minecraft minecraft = Minecraft.getInstance();

    // registered menu screens
    //private final Map<ContainerType<?>, Function<? extends Container, ApplicationUI>> mScreenRegistry = new HashMap<>();

    // application window
    private final ViewRootImpl mAppWindow = new ViewRootImpl();

    // the top-level view of the window
    private final DecorView mDecorView = new DecorView();

    // true if there will be no screen to open
    private boolean mCloseScreen;

    // indicates whether the current screen is a Modern UI screen, also a callback to the screen
    @Nullable
    private MuiScreen mMuiScreen;

    // application UI used to send lifecycle events
    private ScreenCallback mScreen;

    // main fragment of a UI
    //private Fragment fragment;

    // main UI view that created from main fragment
    //private View view;

    //private IModule popup;

    // scaled game window width / height, the pixels that are less than 1.0
    // are not considered as a valid area on screen
    private int mWidth;
    private int mHeight;

    // a list of UI tasks
    private final List<TimedTask> tasks = new CopyOnWriteArrayList<>();

    // elapsed ticks from a gui open, update every tick, 20 = 1 second
    private int mTicks = 0;

    // elapsed time from a gui open in milliseconds, update every frame
    private long mDrawingTimeMillis;

    // the start time of a frame in milliseconds, before draw the GUI
    private long mFrameTimeMillis;

    // the canvas to draw things shared in all views and drawables
    // lazy loading because this class is loaded before GL initialization
    // will be init when Minecraft finished loading, and open MainMenuScreen
    // also init font renderer when loaded
    private CanvasForge mFCanvas;

    private GLCanvas mCanvas;

    private Framebuffer mFramebuffer;

    // the most child hovered view, render at the top of other hovered ancestor views
    @Nullable
    private View mHovered;

    // focused view
    @Nullable
    private View mDragging;
    @Nullable
    private View mKeyboard;

    // current cursor position in the window
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

    private LongConsumer mAnimationHandler;

    private Thread mUiThread;
    private final Object mRenderLock = new Object();

    private UIManager() {
        MinecraftForge.EVENT_BUS.register(this);
        mAppWindow.setView(mDecorView);
        AnimationHandler.init(c -> mAnimationHandler = c);
    }

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

    @RenderThread
    public void initialize(int width, int height) {
        RenderCore.checkRenderThread();
        if (mUiThread == null) {
            mCanvas = GLCanvas.initialize();
            mFramebuffer = new Framebuffer(width, height);
            mFramebuffer.attachTexture(GLWrapper.GL_COLOR_ATTACHMENT0, GLWrapper.GL_RGBA8);
            mFramebuffer.attachRenderbuffer(GLWrapper.GL_DEPTH_STENCIL_ATTACHMENT, GLWrapper.GL_DEPTH24_STENCIL8);
            mFCanvas = CanvasForge.getInstance();
            mUiThread = new Thread(this::run, "UI thread");
            mUiThread.start();
            ModernUI.LOGGER.info(MARKER, "UIManager initialized");
        }
    }

    public void interrupt() {
        mUiThread.interrupt();
    }

    @UiThread
    private void run() {
        while (true) {
            synchronized (mRenderLock) {

            }
            try {
                mUiThread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * Open an application UI and create views
     *
     * @param screen the application user interface
     * @see #start(MuiScreen, int, int)
     */
    public void openGUI(@Nonnull ScreenCallback screen) {
        mScreen = screen;
        minecraft.setScreen(new SimpleScreen(this));
    }

    /**
     * Close all screens and destroy current application UI
     *
     * @see #stop()
     */
    public void closeGUI() {
        minecraft.setScreen(null);
    }

    // Internal method
    public boolean openGUI(@Nonnull LocalPlayer player, @Nonnull AbstractContainerMenu menu, @Nonnull String pid) {
        OpenMenuEvent event = new OpenMenuEvent(menu);
        ModernUIForge.get().post(pid, event);
        ScreenCallback screen = event.getScreen();
        if (screen != null) {
            mScreen = screen;
            player.containerMenu = menu;
            minecraft.setScreen(new MenuScreen<>(menu, player.inventory, this));
            return true;
        }
        return false;
    }

    /*@Nonnull
    private <T extends Container> ScreenManager.IScreenFactory<T, MuiMenuScreen<T>> getFactory(
            @Nonnull Function<T, Fragment> factory) {
        return (container, inventory, title) -> {
            this.fragment = factory.apply(container);
            return new MuiMenuScreen<>(container, inventory, title, this);
        };
    }*/

    /*@Deprecated
    public void openPopup(IModule popup, boolean refresh) {
        throw new UnsupportedOperationException();
        *//*if (root == null) {
            ModernUI.LOGGER.fatal(MARKER, "#openPopup() shouldn't be called when there's NO gui open");
            return;
        }*//*
     *//*if (this.popup != null) {
            ModernUI.LOGGER.warn(MARKER, "#openPopup() shouldn't be called when there's already a popup, the previous one has been overwritten");
        }
        if (refresh) {
            this.screenMouseMove(-1, -1);
        }
        this.popup = popup;
        this.popup.resize(width, height);*//*
    }*/

    /*@Deprecated
    public void closePopup() {
        throw new UnsupportedOperationException();
        *//*if (popup != null) {
            popup = null;
        }*//*
    }*/

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
    void start(@Nonnull MuiScreen screen, int width, int height) {
        if (mMuiScreen == null) {
            mScreen.window = this;
            mScreen.onCreate();
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
        final net.minecraft.client.gui.screens.Screen nextScreen = event.getGui();
        mCloseScreen = nextScreen == null;

        if (TestHUD.sDing && !TestHUD.sFirstScreenOpened) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f));
            TestHUD.sFirstScreenOpened = true;
        }

        if (mCloseScreen) {
            stop();
            return;
        }

        if (mMuiScreen != nextScreen && nextScreen instanceof MuiScreen) {
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

    public boolean hasOpenGUI() {
        return mMuiScreen != null;
    }

    @Nullable
    public ScreenCallback getOpenGUI() {
        return mScreen;
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
        tasks.add(new TimedTask(runnable, getElapsedTicks() + delayedTicks));
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
     * @see net.minecraft.client.MouseHandler
     * @see SimpleScreen
     * @see MenuScreen
     */
    void onCursorEvent(double cursorX, double cursorY) {
        mCursorX = minecraft.mouseHandler.xpos();
        mCursorY = minecraft.mouseHandler.ypos();
        final long now = Util.getNanos();
        MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_HOVER_MOVE,
                (float) mCursorX, (float) mCursorY, 0);
        mAppWindow.onInputEvent(event);
        mPendingRepostCursorEvent = false;
    }

    /**
     * Intercept the Forge event
     *
     * @see org.lwjgl.glfw.GLFWMouseButtonCallbackI
     * @see net.minecraft.client.MouseHandler
     * @see net.minecraftforge.client.event.InputEvent
     */
    @SubscribeEvent
    void onRawMouseButton(InputEvent.RawMouseEvent event) {
        // We should ensure (overlay == null && screen != null)
        // and the screen must be a mui screen
        if (minecraft.overlay == null && mMuiScreen != null) {
            ModernUI.LOGGER.debug(MARKER, "Button: {} {} {}", event.getButton(), event.getAction(), event.getMods());
        }
    }

    /**
     * From mouse or touchpad, we need the horizontal scroll offset
     *
     * @see org.lwjgl.glfw.GLFWScrollCallbackI
     * @see net.minecraft.client.MouseHandler
     */
    boolean onScrollEvent() {
        final long now = Util.getNanos();
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

    @SubscribeEvent
    void onPostKeyInput(InputEvent.KeyInputEvent event) {
        if (!ModernUIForge.isDeveloperMode() || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        if (!net.minecraft.client.gui.screens.Screen.hasControlDown()) {
            return;
        }
        switch (event.getKey()) {
            case GLFW.GLFW_KEY_R:
                TextLayoutProcessor.getInstance().reload();
                break;
            case GLFW.GLFW_KEY_G:
                if (minecraft.screen == null && minecraft.hasSingleplayerServer() &&
                        minecraft.getSingleplayerServer() != null && !minecraft.getSingleplayerServer().isPublished())
                    openGUI(new TestPauseUI());
                minecraft.getLanguageManager().getLanguages().forEach(l ->
                        ModernUI.LOGGER.info(MARKER, "Locale {} RTL {}", l.getCode(), ULocale.forLocale(l.getJavaLocale()).isRightToLeft()));
                break;
            case GLFW.GLFW_KEY_P:
                if (minecraft.screen == null) {
                    break;
                }
                StringBuilder builder = new StringBuilder();
                builder.append("Modern UI Debug Info:\n");

                builder.append("[0] Mui Screen: ");
                builder.append(mMuiScreen != null);
                builder.append("\n");

                builder.append("[1] Container Menu: ");
                builder.append(minecraft.player != null ? minecraft.player.containerMenu : null);
                builder.append("\n");

                builder.append("[2] Open Gui: ");
                if (mScreen == null) {
                    builder.append(minecraft.screen);
                } else {
                    builder.append(mScreen);
                }
                builder.append("\n");

                ModernUI.LOGGER.info(MARKER, builder.toString());
                break;
        }
    }

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

    @RenderThread
    public void render() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        int texture;
        synchronized (mRenderLock) {
            Framebuffer framebuffer = mFramebuffer;
            framebuffer.resize(mWidth, mHeight);
            framebuffer.bindDraw();
            framebuffer.setDrawBuffer(GLWrapper.GL_COLOR_ATTACHMENT0);
            mCanvas.render();
            texture = framebuffer.getAttachedTextureName(GLWrapper.GL_COLOR_ATTACHMENT0);
        }
        GLWrapper.glBindFramebuffer(GLWrapper.GL_DRAW_FRAMEBUFFER, GLWrapper.DEFAULT_FRAMEBUFFER);
        RenderSystem.bindTexture(texture);
        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(GLWrapper.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        bufferbuilder.vertex(0.0D, mHeight, 0.0D).color(255, 255, 255, 255).uv(0.0F, 0.0F).endVertex();
        bufferbuilder.vertex(mWidth, mHeight, 0.0D).color(255, 255, 255, 255).uv(1, 0.0F).endVertex();
        bufferbuilder.vertex(mWidth, 0.0D, 0.0D).color(255, 255, 255, 255).uv(1, 1).endVertex();
        bufferbuilder.vertex(0.0D, 0.0D, 0.0D).color(255, 255, 255, 255).uv(0.0F, 1).endVertex();
        tesselator.end();
        RenderSystem.bindTexture(0);
        RenderSystem.depthMask(true);
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
                if (TestHUD.sBars)
                    TestHUD.sInstance.drawBars(mFCanvas);
                break;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    void onRenderTooltip(@Nonnull RenderTooltipEvent.Pre event) {
        if (TestHUD.sTooltip) {
            if (!(minecraft.font instanceof ModernFontRenderer)) {
                ModernUI.LOGGER.fatal(MARKER, "Failed to hook FontRenderer, tooltip disabled");
                TestHUD.sTooltip = false;
                return;
            }
            final Window window = minecraft.getWindow();
            double cursorX = minecraft.mouseHandler.xpos() * (double) window.getGuiScaledWidth() / (double) window.getScreenWidth();
            double cursorY = minecraft.mouseHandler.ypos() * (double) window.getGuiScaledHeight() / (double) window.getScreenHeight();
            TestHUD.sInstance.drawTooltip(mFCanvas, event.getLines(), (ModernFontRenderer) minecraft.font, event.getStack(),
                    event.getMatrixStack(), event.getX(), event.getY(), (float) cursorX, (float) cursorY, event.getScreenWidth(), event.getScreenHeight());
            event.setCanceled(true);
        }
    }

    /**
     * Called when game window size changed, used to re-layout the UI
     *
     * @param width  scaled game window width
     * @param height scaled game window height
     */
    void resize(int width, int height) {
        final Window window = minecraft.getWindow();
        mWidth = window.getWidth();
        mHeight = window.getHeight();
        mCursorX = minecraft.mouseHandler.xpos();
        mCursorY = minecraft.mouseHandler.ypos();
        doLayout();
    }

    /**
     * Directly layout UI window
     */
    private void doLayout() {
        long startTime = Util.getNanos();

        int widthSpec = MeasureSpec.makeMeasureSpec(mWidth, MeasureSpec.Mode.EXACTLY);
        int heightSpec = MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.Mode.EXACTLY);

        mAppWindow.performLayout(widthSpec, heightSpec);

        if (ModernUIForge.isDeveloperMode()) {
            ModernUI.LOGGER.info(MARKER, "Layout done in {} \u03bcs, framebuffer size: {}x{}, cursor pos: ({}, {})",
                    (Util.getNanos() - startTime) / 1000.0f, mWidth, mHeight, mCursorX, mCursorY);
            //UITools.runViewTraversal(mDecorView, v -> ModernUI.LOGGER.debug(MARKER, "{}: {}x{}", v, v.getWidth(), v.getHeight()));
        }
        onCursorEvent(mCursorX, mCursorY);
        mLayoutRequested = false;
    }

    void stop() {
        // Hotfix 1.4.7
        if (mCloseScreen) {
            tasks.clear();
            mMuiScreen = null;
            if (mScreen != null) {
                mScreen.window = null;
                mScreen = null;
            }
            mLastLayoutTime = 0;
            mLayoutRequested = false;
            mDecorView.removeAllViews();
            UITools.useDefaultCursor();
            // Hotfix 1.5.8
            minecraft.keyboardHandler.setSendRepeatsToGui(false);
        }
    }

    @SubscribeEvent
    void onClientTick(@Nonnull TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ++mTicks;
            mAppWindow.tick(mTicks);
            // view ticking is always performed before tasks
            if (!tasks.isEmpty()) {
                tasks.removeIf(task -> task.doExecuteTask(mTicks));
            }
        } else {
            if (mPendingRepostCursorEvent) {
                onCursorEvent(mCursorX, mCursorY);
            }
        }
    }

    @SubscribeEvent
    void onRenderTick(@Nonnull TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            // the Timer is different from that in Event when game paused
            long deltaMillis = (long) (minecraft.getDeltaFrameTime() * 50);
            mFrameTimeMillis = RenderCore.timeMillis();
            mDrawingTimeMillis += deltaMillis;
            mAnimationHandler.accept(mFrameTimeMillis);
            BlurHandler.INSTANCE.update(mDrawingTimeMillis);
        } else {
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
        ViewParent parent = view.getParent();
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
        ViewParent parent = view.getParent();
        double mouseY = mCursorY;

        while (parent != null) {
            mouseY += parent.getScrollY();
            parent = parent.getParent();
        }

        return mouseY;
    }

    public double getGuiScale() {
        return minecraft.getWindow().getGuiScale();
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
            minecraft.keyboardHandler.setSendRepeatsToGui(view != null);
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
