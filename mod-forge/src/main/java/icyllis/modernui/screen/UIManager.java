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
import icyllis.modernui.graphics.Framebuffer;
import icyllis.modernui.graphics.GLCanvas;
import icyllis.modernui.graphics.texture.Texture;
import icyllis.modernui.graphics.texture.Texture2D;
import icyllis.modernui.math.Matrix4;
import icyllis.modernui.platform.Bitmap;
import icyllis.modernui.platform.RenderCore;
import icyllis.modernui.test.TestPauseUI;
import icyllis.modernui.textmc.TextLayoutProcessor;
import icyllis.modernui.util.TimedTask;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.view.ViewRootImpl;
import icyllis.modernui.widget.DecorView;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
@OnlyIn(Dist.CLIENT)
public final class UIManager implements ViewRootImpl.Handler {

    // logger marker
    public static final Marker MARKER = MarkerManager.getMarker("UIManager");

    // the global instance
    private static volatile UIManager sInstance;

    // config value
    public static boolean sPlaySoundOnLoaded;

    // minecraft client
    private final Minecraft minecraft = Minecraft.getInstance();

    // minecraft window
    private final Window mWindow = minecraft.getWindow();

    // root view
    private ViewRootImpl mRoot;

    // the top-level view of the window
    private DecorView mDecor;

    // true if there will be no screen to open
    private boolean mCloseScreen;

    // indicates whether the current screen is a Modern UI screen, also a callback to the screen
    @Nullable
    private MuiScreen mScreen;

    // application UI used to send lifecycle events
    private ScreenCallback mCallback;

    // a list of UI tasks
    private final List<TimedTask> mTasks = new CopyOnWriteArrayList<>();

    // animation update callback
    private final LongConsumer mAnimationCallback;

    // elapsed ticks from a gui open, update every tick, 20 = 1 second
    private int mTicks = 0;

    // elapsed time from a gui open in milliseconds, update every frame
    private long mElapsedTimeMillis;

    private long mFrameTimeMillis;

    // lazy loading
    private GLCanvas mCanvas;
    private final Framebuffer mFramebuffer;

    private final Thread mUiThread;
    private final Object mRenderLock = new Object();

    private MotionEvent mPendingMouseEvent;

    private boolean mFirstScreenOpened = false;
    private boolean mProjectionChanged = false;

    private UIManager() {
        mAnimationCallback = AnimationHandler.init();
        mFramebuffer = new Framebuffer(mWindow.getWidth(), mWindow.getHeight());
        mUiThread = new Thread(this::run, "UI thread");
        MinecraftForge.EVENT_BUS.register(this);
    }

    // internal use
    @Nonnull
    public static UIManager getInstance() {
        if (sInstance == null) {
            synchronized (UIManager.class) {
                if (sInstance == null) {
                    sInstance = new UIManager();
                }
            }
        }
        return sInstance;
    }

    @RenderThread
    public void initRenderer() {
        RenderCore.checkRenderThread();
        if (mCanvas == null) {
            mCanvas = GLCanvas.initialize();
            mFramebuffer.attachTexture(GL_COLOR_ATTACHMENT0, GL_RGBA8);
            // no depth buffer
            mFramebuffer.attachRenderbuffer(GL_STENCIL_ATTACHMENT, GL_STENCIL_INDEX8);
            mFramebuffer.setDrawBuffer(GL_COLOR_ATTACHMENT0);
            mUiThread.start();
            ModernUI.LOGGER.info(MARKER, "UIManager initialized");
        }
    }

    /**
     * Open an application UI and create views.
     *
     * @param screen the application user interface
     * @see #start(MuiScreen)
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

    /**
     * Get elapsed time in UI, update every frame
     *
     * @return drawing time in milliseconds
     */
    public long getElapsedTime() {
        return mElapsedTimeMillis;
    }

    @Nullable
    public ScreenCallback getCallback() {
        return mCallback;
    }

    // Called when open a screen from Modern UI, or back to the screen
    void start(@Nonnull MuiScreen screen) {
        if (mScreen == null) {
            mCallback.host = this;
            postTask(() -> mCallback.onCreate(), 0);
        }
        mScreen = screen;

        // init view of this UI


        resize();
    }

    void setContentView(View view, ViewGroup.LayoutParams params) {
        mDecor.removeAllViews();
        mDecor.addView(view, params);
    }

    @SubscribeEvent
    void onGuiOpen(@Nonnull GuiOpenEvent event) {
        final Screen next = event.getGui();
        mCloseScreen = next == null;

        if (!mFirstScreenOpened) {
            if (sPlaySoundOnLoaded) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f));
            }
            mFirstScreenOpened = true;
        }

        if (mCloseScreen) {
            finish();
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

    /**
     * Post a task that will run on UI thread in specified milliseconds.
     *
     * @param action runnable task
     * @param delay  delayed time to run the task in milliseconds
     */
    @Override
    public void postTask(@Nonnull Runnable action, long delay) {
        mTasks.add(new TimedTask(action, mFrameTimeMillis + delay));
    }

    @Override
    public void removeTask(@Nonnull Runnable action) {
        mTasks.removeIf(t -> t.mRunnable == action);
    }

    @UiThread
    private void run() {
        mRoot = new ViewRootImpl(mCanvas, this);
        mDecor = new DecorView();
        mRoot.setView(mDecor);
        ModernUI.LOGGER.info(MARKER, "View system initialized");
        for (; ; ) {
            try {
                mUiThread.join();
            } catch (InterruptedException ignored) {
            }

            // holds the lock
            synchronized (mRenderLock) {
                // 1. do tasks
                if (!mTasks.isEmpty()) {
                    // batched processing
                    mTasks.removeIf(task -> task.doExecuteTask(mFrameTimeMillis));
                }
                if (mScreen == null) {
                    return;
                }

                // 2. do input events
                mRoot.doProcessInputEvents();

                // 3. do animations
                mAnimationCallback.accept(mFrameTimeMillis);

                // 4. do traversal
                mRoot.doTraversal();

                // test stuff
                /*Paint paint = Paint.take();
                paint.setStrokeWidth(6);
                int c = (int) mElapsedTimeMillis / 300;
                c = Math.min(c, 8);
                float[] pts = new float[c * 2 + 2];
                pts[0] = 90;
                pts[1] = 30;
                for (int i = 0; i < c; i++) {
                    pts[2 + i * 2] = Math.min((i + 2) * 60, mElapsedTimeMillis / 5) + 30;
                    if ((i & 1) == 0) {
                        if (mElapsedTimeMillis >= (i + 2) * 300) {
                            pts[3 + i * 2] = 90;
                        } else {
                            pts[3 + i * 2] = 30 + (mElapsedTimeMillis % 300) / 5f;
                        }
                    } else {
                        if (mElapsedTimeMillis >= (i + 2) * 300) {
                            pts[3 + i * 2] = 30;
                        } else {
                            pts[3 + i * 2] = 90 - (mElapsedTimeMillis % 300) / 5f;
                        }
                    }
                }
                mCanvas.drawStripLines(pts, paint);

                paint.setRGBA(255, 180, 100, 255);
                mCanvas.drawCircle(90, 30, 6, paint);
                mCanvas.drawCircle(150, 90, 6, paint);
                mCanvas.drawCircle(210, 30, 6, paint);
                mCanvas.drawCircle(270, 90, 6, paint);
                mCanvas.drawCircle(330, 30, 6, paint);
                mCanvas.drawCircle(390, 90, 6, paint);
                mCanvas.drawCircle(450, 30, 6, paint);
                mCanvas.drawCircle(510, 90, 6, paint);
                mCanvas.drawCircle(570, 30, 6, paint);*/
            }
        }
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
        final long now = RenderCore.timeNanos();
        float x = (float) minecraft.mouseHandler.xpos();
        float y = (float) minecraft.mouseHandler.ypos();
        MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_HOVER_MOVE,
                x, y, 0);
        mRoot.enqueueInputEvent(event);
        //mPendingRepostCursorEvent = false;
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
            //ModernUI.LOGGER.info(MARKER, "Button: {} {} {}", event.getButton(), event.getAction(), event.getMods());
            final long now = RenderCore.timeNanos();
            float x = (float) minecraft.mouseHandler.xpos();
            float y = (float) minecraft.mouseHandler.ypos();
            int buttonState = 0;
            for (int i = 0; i < 5; i++) {
                if (glfwGetMouseButton(mWindow.getWindow(), i) == GLFW_PRESS) {
                    buttonState |= 1 << i;
                }
            }
            mPendingMouseEvent = MotionEvent.obtain(now, now, 0, event.getAction() == GLFW_PRESS ?
                            MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP, x, y, event.getMods(),
                    buttonState, 0);
        }
    }

    void onMouseButton() {
        mRoot.enqueueInputEvent(mPendingMouseEvent);
    }

    // Internal method
    public void onScroll(double scrollX, double scrollY) {
        final long now = RenderCore.timeNanos();
        float x = (float) minecraft.mouseHandler.xpos();
        float y = (float) minecraft.mouseHandler.ypos();
        MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_SCROLL,
                x, y, 0);
        event.setRawAxisValue(MotionEvent.AXIS_HSCROLL, (float) scrollX);
        event.setRawAxisValue(MotionEvent.AXIS_VSCROLL, (float) scrollY);
        mRoot.enqueueInputEvent(event);
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
            case GLFW_KEY_C:
                // make a screenshot
                Texture texture = mFramebuffer.getAttachedTexture(GL_COLOR_ATTACHMENT0);
                Bitmap bitmap = Bitmap.download(Bitmap.Format.RGBA, (Texture2D) texture);
                Util.ioPool().execute(() -> {
                    try (bitmap) {
                        bitmap.saveDialog(Bitmap.SaveFormat.PNG, 0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

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

    boolean charTyped(char ch) {
        //TODO
        /*if (popup != null) {
            return popup.charTyped(codePoint, modifiers);
        }*/
        /*if (mKeyboard != null) {
            return mKeyboard.onCharTyped(codePoint, modifiers);
        }*/
        return false;//root.charTyped(codePoint, modifiers);
    }

    @RenderThread
    void render() {
        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        RenderSystem.activeTexture(GL_TEXTURE0);
        RenderSystem.disableDepthTest();

        // blend alpha correctly, since the Minecraft.mainRenderTarget has no alpha (always 1)
        // and our framebuffer is totally a transparent layer
        RenderSystem.blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        int width = mWindow.getWidth();
        int height = mWindow.getHeight();

        GLCanvas canvas = mCanvas;
        Framebuffer framebuffer = mFramebuffer;

        if (mProjectionChanged) {
            Matrix4 projection = Matrix4.makeOrthographic(width, -height, 0, 2000);
            canvas.setProjection(projection);
            mProjectionChanged = false;
        }

        // wait UI thread, if slow
        synchronized (mRenderLock) {
            if (mRoot.isReadyForRendering()) {
                final int oldVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING);
                final int oldProgram = glGetInteger(GL_CURRENT_PROGRAM);
                glEnable(GL_STENCIL_TEST);

                framebuffer.resize(width, height);
                framebuffer.clearColorBuffer();
                framebuffer.clearDepthStencilBuffer();
                framebuffer.bindDraw();
                // flush tasks from UI thread, such as texture uploading
                RenderCore.flushRenderCalls();
                canvas.render();

                glBindVertexArray(oldVertexArray);
                glUseProgram(oldProgram);
                glDisable(GL_STENCIL_TEST);
            }
        }
        int texture = framebuffer.getAttachedTexture(GL_COLOR_ATTACHMENT0).get();

        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, minecraft.getMainRenderTarget().frameBufferId);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableTexture();
        RenderSystem.bindTexture(texture);

        GlStateManager._matrixMode(5889);
        GlStateManager._loadIdentity();
        GlStateManager._ortho(0.0D, width, height, 0.0D, 1000.0D, 3000.0D);

        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        int alpha = (int) Math.min(255, mElapsedTimeMillis);
        builder.vertex(0, height, 0).color(255, 255, 255, alpha).uv(0, 0).endVertex();
        builder.vertex(width, height, 0).color(255, 255, 255, alpha).uv(1, 0).endVertex();
        builder.vertex(width, 0, 0).color(255, 255, 255, alpha).uv(1, 1).endVertex();
        builder.vertex(0, 0, 0).color(255, 255, 255, alpha).uv(0, 1).endVertex();
        tesselator.end();
        RenderSystem.bindTexture(DEFAULT_TEXTURE);

        GlStateManager._loadIdentity();
        GlStateManager._ortho(0.0D, width / mWindow.getGuiScale(), height / mWindow.getGuiScale(),
                0.0D, 1000.0D, 3000.0D);
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
     * Called when game window size changed, used to re-layout the window.
     */
    void resize() {
        postTask(() -> mRoot.setFrame(mWindow.getWidth(), mWindow.getHeight()), 0);
        mProjectionChanged = true;
    }

    void finish() {
        if (!mCloseScreen || mScreen == null) {
            return;
        }
        mTasks.clear();
        mScreen = null;
        if (mCallback != null) {
            mCallback.host = null;
            mCallback = null;
        }
        UITools.useDefaultCursor();
        minecraft.keyboardHandler.setSendRepeatsToGui(false);
        postTask(() -> mDecor.removeAllViews(), 0);
    }

    @Deprecated
    @SubscribeEvent
    void onClientTick(@Nonnull TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ++mTicks;
            postTask(() -> mRoot.tick(mTicks), 0);
        }
        /* else {
            if (mPendingRepostCursorEvent) {
                onCursorPos();
            }
        }*/
    }

    @SubscribeEvent
    void onRenderTick(@Nonnull TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            final long lastFrameTime = mFrameTimeMillis;
            mFrameTimeMillis = RenderCore.timeMillis();
            mElapsedTimeMillis += mFrameTimeMillis - lastFrameTime;
            if (mScreen != null) {
                mUiThread.interrupt();
            }
            BlurHandler.INSTANCE.update(mElapsedTimeMillis);
        }
        /* else {
            // layout after updating animations and before drawing
            if (mLayoutRequested) {
                // fixed at 40Hz
                if (mElapsedTimeMillis - mLastLayoutTime >= 25) {
                    mLastLayoutTime = mElapsedTimeMillis;
                    doLayout();
                }
            }
        }*/
    }

    //boolean mPendingRepostCursorEvent = false;

    // main fragment of a UI
    //private Fragment fragment;

    // main UI view that created from main fragment
    //private View view;

    //private IModule popup;

    //boolean mLayoutRequested = false;
    //private long mLastLayoutTime = 0;

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

    /*
     * Get elapsed ticks from a gui open, update every tick, 20 = 1 second
     *
     * @return elapsed ticks
     */
    /*public int getElapsedTicks() {
        return mTicks;
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

    /*public void repostCursorEvent() {
        mPendingRepostCursorEvent = true;
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

    /*private void mouseMoved() {
        if (view != null && !view.updateMouseHover(mouseX, mouseY)) {
            setHovered(null);
        }
    }*/
}
