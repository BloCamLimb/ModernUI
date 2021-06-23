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

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.InputConstants;
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
import icyllis.modernui.graphics.BlurHandler;
import icyllis.modernui.graphics.Framebuffer;
import icyllis.modernui.graphics.GLCanvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.textmc.TextLayoutProcessor;
import icyllis.modernui.graphics.texture.Texture2D;
import icyllis.modernui.math.Matrix4;
import icyllis.modernui.platform.Bitmap;
import icyllis.modernui.platform.RenderCore;
import icyllis.modernui.test.TestHUD;
import icyllis.modernui.test.TestPauseUI;
import icyllis.modernui.util.TimedTask;
import icyllis.modernui.view.*;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.LongConsumer;

import static icyllis.modernui.graphics.GLWrapper.*;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Manage UI thread and connect Minecraft to Modern UI view system at bottom level.
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

    // application window
    private final ViewRootImpl mRoot = new ViewRootImpl();

    // the top-level view of the window
    private final DecorView mDecor = new DecorView();

    // true if there will be no screen to open
    private boolean mCloseScreen;

    // indicates whether the current screen is a Modern UI screen, also a callback to the screen
    @Nullable
    private MuiScreen mScreen;

    // application UI used to send lifecycle events
    private ScreenCallback mCallback;

    // a list of UI tasks
    private final List<TimedTask> mTasks = new CopyOnWriteArrayList<>();

    private final LongConsumer mAnimationCallback;

    // elapsed ticks from a gui open, update every tick, 20 = 1 second
    private int mTicks = 0;

    // elapsed time from a gui open in milliseconds, update every frame
    private long mElapsedTimeMillis;

    private long mFrameTimeMillis;

    // lazy loading
    private GLCanvas mCanvas;
    private Framebuffer mFramebuffer;

    private final Thread mUiThread;
    private final Object mRenderLock = new Object();

    boolean mLayoutRequested = false;
    private long mLastLayoutTime = 0;

    boolean mPendingRepostCursorEvent = false;

    private UIManager() {
        mAnimationCallback = AnimationHandler.init();
        mUiThread = new Thread(this::run, "UI thread");
        MinecraftForge.EVENT_BUS.register(this);
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
    public void initRenderer() {
        RenderCore.checkRenderThread();
        if (mCanvas == null) {
            mCanvas = GLCanvas.initialize();
            Window window = minecraft.getWindow();
            mFramebuffer = new Framebuffer(window.getWidth(), window.getHeight());
            mFramebuffer.attachTexture(GL_COLOR_ATTACHMENT0, GL_RGBA8);
            mFramebuffer.attachRenderbuffer(GL_DEPTH_STENCIL_ATTACHMENT, GL_DEPTH24_STENCIL8);
            mUiThread.start();
            ModernUI.LOGGER.info(MARKER, "UIManager initialized");
        }
    }

    @UiThread
    private void run() {
        mRoot.setView(mDecor);
        for (; ; ) {
            // holds the lock
            synchronized (mRenderLock) {
                Paint paint = Paint.take();
                paint.setStrokeWidth(6);
                mCanvas.drawLine(20, 20, 200, 200, paint);
                if (mTicks < 1) {
                    ModernUI.LOGGER.info("Draw on UI thread");
                }
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
     * @see #init(MuiScreen, int, int)
     */
    public void openGui(@Nonnull ScreenCallback screen) {
        mCallback = screen;
        minecraft.setScreen(new SimpleScreen(this));
    }

    // Internal method
    public boolean openMenu(@Nonnull LocalPlayer player, @Nonnull AbstractContainerMenu menu, @Nonnull String modid) {
        OpenMenuEvent event = new OpenMenuEvent(menu);
        ModernUIForge.get().post(modid, event);
        ScreenCallback screen = event.getScreen();
        if (screen == null) {
            return false;
        }
        mCallback = screen;
        player.containerMenu = menu;
        minecraft.setScreen(new MenuScreen<>(menu, player.inventory, this));
        return true;
    }

    void setContentView(View view, ViewGroup.LayoutParams params) {
        mDecor.removeAllViews();
        mDecor.addView(view, params);
    }

    /**
     * Called when open a mui screen, or back to the mui screen
     *
     * @param screen the current Screen
     * @param width  scaled game main window width
     * @param height scaled game main window height
     */
    void init(@Nonnull MuiScreen screen, int width, int height) {
        if (mScreen == null) {
            mCallback.host = this;
            mCallback.onCreate();
        }
        mScreen = screen;

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
        final Screen next = event.getGui();
        mCloseScreen = next == null;

        if (TestHUD.sDing && !TestHUD.sFirstScreenOpened) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f));
            TestHUD.sFirstScreenOpened = true;
        }

        if (mCloseScreen) {
            removed();
            return;
        }

        if (mScreen != next && next instanceof MuiScreen) {
            mTicks = 0;
            mElapsedTimeMillis = 0;
        }
        if (mScreen != next && mScreen != null) {
            onCursorPos();
        }
        // for non-mui screens
        if (mScreen == null) {
            mTicks = 0;
            mElapsedTimeMillis = 0;
        }
    }

    @Nullable
    public ScreenCallback getCallback() {
        return mCallback;
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
        mTasks.add(new TimedTask(runnable, getElapsedTicks() + delayedTicks));
    }

    //TODO move inside view
    public void repostCursorEvent() {
        mPendingRepostCursorEvent = true;
    }

    /**
     * From screen
     *
     * @see org.lwjgl.glfw.GLFWCursorPosCallbackI
     * @see net.minecraft.client.MouseHandler
     * @see SimpleScreen
     * @see MenuScreen
     */
    void onCursorPos() {
        final long now = Util.getNanos();
        double x = minecraft.mouseHandler.xpos();
        double y = minecraft.mouseHandler.ypos();
        MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_HOVER_MOVE,
                (float) x, (float) y, 0);
        mRoot.onInputEvent(event);
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
    void onMouseButton(@Nonnull InputEvent.RawMouseEvent event) {
        // We should ensure (overlay == null && screen != null)
        // and the screen must be a mui screen
        if (minecraft.overlay == null && mScreen != null) {
            ModernUI.LOGGER.debug(MARKER, "Button: {} {} {}", event.getButton(), event.getAction(), event.getMods());
        }
    }

    void onMouseButton() {

    }

    // Internal method
    public void onScroll(double scrollX, double scrollY) {
        final long now = Util.getNanos();
        double x = minecraft.mouseHandler.xpos();
        double y = minecraft.mouseHandler.ypos();
        MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_SCROLL,
                (float) x, (float) y, 0);
        event.setRawAxisValue(MotionEvent.AXIS_HSCROLL, (float) scrollX);
        event.setRawAxisValue(MotionEvent.AXIS_VSCROLL, (float) scrollY);
        boolean handled = mRoot.onInputEvent(event);
        if (handled) {
            mPendingRepostCursorEvent = true;
        }
    }

    @SubscribeEvent
    void onPostKeyInput(@Nonnull InputEvent.KeyInputEvent event) {
        if (mScreen != null && event.getAction() == GLFW_PRESS) {
            //TODO dispatch to views
            InputConstants.Key key = InputConstants.getKey(event.getKey(), event.getScanCode());
            if (mScreen instanceof MenuScreen<?> && minecraft.options.keyInventory.isActiveAndMatches(key)) {
                if (minecraft.player != null) {
                    minecraft.player.closeContainer();
                }
                return;
            } else if (event.getKey() == GLFW_KEY_ESCAPE) {
                //TODO check should close on esc
                minecraft.setScreen(null);
                return;
            }
        }
        if (!ModernUIForge.isDeveloperMode() || event.getAction() != GLFW_PRESS || !Screen.hasControlDown()) {
            return;
        }
        switch (event.getKey()) {
            case GLFW_KEY_R:
                TextLayoutProcessor.getInstance().reload();
                break;
            case GLFW_KEY_G:
                if (minecraft.screen == null && minecraft.hasSingleplayerServer() &&
                        minecraft.getSingleplayerServer() != null && !minecraft.getSingleplayerServer().isPublished()) {
                    openGui(new TestPauseUI());
                }
                /*minecraft.getLanguageManager().getLanguages().forEach(l ->
                        ModernUI.LOGGER.info(MARKER, "Locale {} RTL {}", l.getCode(), ULocale.forLocale(l.getJavaLocale()).isRightToLeft()));*/
                break;
            case GLFW_KEY_P:
                if (minecraft.screen == null) {
                    break;
                }
                StringBuilder builder = new StringBuilder();
                builder.append("Modern UI Debug Info:\n");

                builder.append("[0] From Modern UI: ");
                builder.append(mScreen != null);
                builder.append("\n");

                builder.append("[1] Container Menu: ");
                builder.append(minecraft.player != null ? minecraft.player.containerMenu : null);
                builder.append("\n");

                builder.append("[2] Callback or Screen: ");
                builder.append(Objects.requireNonNullElseGet(mCallback, () -> minecraft.screen));
                builder.append("\n");

                ModernUI.LOGGER.info(MARKER, builder.toString());
                break;
        }
    }

    boolean sCharTyped(char codePoint, int modifiers) {
        //TODO
        /*if (popup != null) {
            return popup.charTyped(codePoint, modifiers);
        }*/
        /*if (mKeyboard != null) {
            return mKeyboard.onCharTyped(codePoint, modifiers);
        }*/
        return false;//root.charTyped(codePoint, modifiers);
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

        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();

        GlStateManager._matrixMode(5889);
        GlStateManager._loadIdentity();
        GlStateManager._ortho(0.0D, width, height, 0.0D, 1000.0D, 3000.0D);

        int texture;
        // wait UI thread, if slow
        synchronized (mRenderLock) {
            // upload textures
            RenderCore.flushRenderCalls();
            Framebuffer framebuffer = mFramebuffer;
            framebuffer.resize(width, height);
            framebuffer.bindDraw();
            framebuffer.setDrawBuffer(GL_COLOR_ATTACHMENT0);
            Matrix4 projection = Matrix4.makeOrthographic(width, -height, 0, 2000);
            mCanvas.setProjection(projection);
            mCanvas.render();
            texture = framebuffer.getAttachedTextureRaw(GL_COLOR_ATTACHMENT0);
        }
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, minecraft.getMainRenderTarget().frameBufferId);
        RenderSystem.enableTexture();
        RenderSystem.bindTexture(texture);
        GlStateManager._color4f(1.0F, 1.0F, 1.0F, 1.0F);
        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        //TODO global alpha transformation
        builder.vertex(0, height, 0).color(255, 255, 255, 255).uv(0, 0).endVertex();
        builder.vertex(width, height, 0).color(255, 255, 255, 255).uv(1, 0).endVertex();
        builder.vertex(width, 0, 0).color(255, 255, 255, 255).uv(1, 1).endVertex();
        builder.vertex(0, 0, 0).color(255, 255, 255, 255).uv(0, 1).endVertex();
        tesselator.end();
        RenderSystem.bindTexture(0);
        RenderSystem.depthMask(true);

        GlStateManager._loadIdentity();
        GlStateManager._ortho(0.0D, width / getGuiScale(), height / getGuiScale(), 0.0D, 1000.0D, 3000.0D);
        GlStateManager._matrixMode(5888);
    }

    @SubscribeEvent
    void onRenderGameOverlay(@Nonnull RenderGameOverlayEvent.Pre event) {
        switch (event.getType()) {
            case CROSSHAIRS:
                event.setCanceled(mScreen != null);
                break;
            case ALL:
                // hotfix 1.16 vanilla, using shader makes TEXTURE_2D disabled
                RenderSystem.enableTexture();
                break;
            /*case HEALTH:
                if (TestHUD.sBars)
                    TestHUD.sInstance.drawBars(mFCanvas);
                break;*/
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    void onRenderTooltip(@Nonnull RenderTooltipEvent.Pre event) {
        /*if (TestHUD.sTooltip) {
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
        }*/
    }

    /**
     * Called when game window size changed, used to re-layout the UI
     *
     * @param width  scaled game window width
     * @param height scaled game window height
     */
    void resize(int width, int height) {
        doLayout();
    }

    /**
     * Directly layout UI window
     */
    private void doLayout() {
        long startTime = Util.getNanos();
        final Window window = minecraft.getWindow();
        int width = window.getWidth();
        int height = window.getHeight();
        int widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.Mode.EXACTLY);
        int heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.Mode.EXACTLY);

        mRoot.performLayout(widthSpec, heightSpec);

        if (ModernUIForge.isDeveloperMode()) {
            ModernUI.LOGGER.info(MARKER, "Layout done in {} \u03bcs, framebuffer size: {}x{}",
                    (Util.getNanos() - startTime) / 1000.0f, width, height);
            //UITools.runViewTraversal(mDecorView, v -> ModernUI.LOGGER.debug(MARKER, "{}: {}x{}", v, v.getWidth(), v.getHeight()));
        }
        onCursorPos();
        mLayoutRequested = false;
    }

    void removed() {
        if (!mCloseScreen || mScreen == null) {
            return;
        }
        mTasks.clear();
        mScreen = null;
        mCallback.host = null;
        mCallback = null;
        mLastLayoutTime = 0;
        mLayoutRequested = false;
        mDecor.removeAllViews();
        UITools.useDefaultCursor();
        minecraft.keyboardHandler.setSendRepeatsToGui(false);
    }

    @SubscribeEvent
    void onClientTick(@Nonnull TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ++mTicks;
            mRoot.tick(mTicks);
            // view ticking is always performed before tasks
            if (!mTasks.isEmpty()) {
                mTasks.removeIf(task -> task.doExecuteTask(mTicks));
            }
        } else {
            if (mPendingRepostCursorEvent) {
                onCursorPos();
            }
        }
    }

    @SubscribeEvent
    void onRenderTick(@Nonnull TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (mCallback != null) {
                mUiThread.interrupt();
            }
            // the Timer is different from that in Event when game paused
            long deltaMillis = (long) (minecraft.getDeltaFrameTime() * 50);
            mFrameTimeMillis = RenderCore.timeMillis();
            mElapsedTimeMillis += deltaMillis;
            mAnimationCallback.accept(mFrameTimeMillis);
            BlurHandler.INSTANCE.update(mElapsedTimeMillis);
        } else {
            // layout after updating animations and before drawing
            if (mLayoutRequested) {
                // fixed at 40Hz
                if (mElapsedTimeMillis - mLastLayoutTime >= 25) {
                    mLastLayoutTime = mElapsedTimeMillis;
                    doLayout();
                }
            }
        }
    }

    /**
     * Get elapsed time in UI, update every frame
     *
     * @return drawing time in milliseconds
     */
    public long getElapsedTime() {
        return mElapsedTimeMillis;
    }

    /**
     * Get elapsed ticks from a gui open, update every tick, 20 = 1 second
     *
     * @return elapsed ticks
     */
    public int getElapsedTicks() {
        return mTicks;
    }

    public double getGuiScale() {
        return minecraft.getWindow().getGuiScale();
    }

    // main fragment of a UI
    //private Fragment fragment;

    // main UI view that created from main fragment
    //private View view;

    //private IModule popup;

    // registered menu screens
    //private final Map<ContainerType<?>, Function<? extends Container, ApplicationUI>> mScreenRegistry = new HashMap<>();

    // the most child hovered view, render at the top of other hovered ancestor views
    /*@Nullable
    private View mHovered;*/

    // focused view
    /*@Nullable
    private View mDragging;
    @Nullable
    private View mKeyboard;*/

    /*public int getScreenWidth() {
        return mWidth;
    }

    public int getScreenHeight() {
        return mHeight;
    }

    public double getCursorX() {
        return mCursorX;
    }

    public double getCursorY() {
        return mCursorY;
    }

    public double getViewMouseX(@Nonnull View view) {
        ViewParent parent = view.getParent();
        double mouseX = mCursorX;

        while (parent != null) {
            mouseX += parent.getScrollX();
            parent = parent.getParent();
        }

        return mouseX;
    }

    public double getViewMouseY(@Nonnull View view) {
        ViewParent parent = view.getParent();
        double mouseY = mCursorY;

        while (parent != null) {
            mouseY += parent.getScrollY();
            parent = parent.getParent();
        }

        return mouseY;
    }*/

    /*void setHovered(@Nullable View view) {
        mHovered = view;
    }

    @Nullable
    public View getHovered() {
        return mHovered;
    }

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
    }*/

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

    /*boolean screenKeyDown(int keyCode, int scanCode, int modifiers) {
     *//*if (popup != null) {
            return popup.keyPressed(keyCode, scanCode, modifiers);
        }*//*
        ModernUI.LOGGER.debug(MARKER, "KeyDown{keyCode:{}, scanCode:{}, mods:{}}", keyCode, scanCode, modifiers);
        *//*if (mKeyboard != null) {
            return mKeyboard.onKeyPressed(keyCode, scanCode, modifiers);
        }*//*
        return false;
    }*/

    /*boolean screenKeyUp(int keyCode, int scanCode, int modifiers) {
     *//*if (popup != null) {
            return popup.keyReleased(keyCode, scanCode, modifiers);
        }*//*
     *//*if (mKeyboard != null) {
            return mKeyboard.onKeyReleased(keyCode, scanCode, modifiers);
        }*//*
        return false;//root.keyReleased(keyCode, scanCode, modifiers);
    }*/

    /*boolean sChangeKeyboard(boolean searchNext) {
        return false;
    }

    boolean onBackPressed() {
        *//*if (popup != null) {
            closePopup();
            return true;
        }*//*
        return false;//root.onBack();
    }*/

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

    /*public boolean hasOpenGUI() {
        return mScreen != null;
    }*/
    
    /*@Deprecated
    boolean screenMouseDown(double mouseX, double mouseY, int mouseButton) {
        setMousePos(mouseX, mouseY);
        MotionEvent event = null;
        event.button = mouseButton;
        //List<ViewRootImpl> windows = this.windows;
        boolean handled = false;
        if (mouseButton == GLFW_MOUSE_BUTTON_LEFT && lastLmTick >= 0 && ticks - lastLmTick < 6) {
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
        if (mouseButton == GLFW_MOUSE_BUTTON_LEFT && lastLmTick < 0) {
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
}
