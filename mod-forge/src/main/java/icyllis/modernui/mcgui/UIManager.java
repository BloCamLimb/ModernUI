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

package icyllis.modernui.mcgui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.ModernUI;
import icyllis.modernui.animation.AnimationHandler;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.forge.ModernUIForge;
import icyllis.modernui.forge.MuiForgeBridge;
import icyllis.modernui.graphics.GLCanvas;
import icyllis.modernui.graphics.GLFramebuffer;
import icyllis.modernui.graphics.texture.GLTexture;
import icyllis.modernui.math.Matrix4;
import icyllis.modernui.platform.RenderCore;
import icyllis.modernui.test.TestMain;
import icyllis.modernui.test.TestPauseUI;
import icyllis.modernui.text.Editable;
import icyllis.modernui.textmc.TextLayoutEngine;
import icyllis.modernui.util.TimedAction;
import icyllis.modernui.view.*;
import icyllis.modernui.widget.DecorView;
import icyllis.modernui.widget.TextView;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongConsumer;
import java.util.function.Predicate;

import static icyllis.modernui.graphics.GLWrapper.*;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Manage UI thread and connect Minecraft to Modern UI view system at bottom level.
 */
@ApiStatus.Internal
@NotThreadSafe
@OnlyIn(Dist.CLIENT)
public final class UIManager extends ViewRootBase {

    // logger marker
    public static final Marker MARKER = MarkerManager.getMarker("UIManager");

    // config values
    public static volatile boolean sPlaySoundOnLoaded;

    // the global instance
    static volatile UIManager sInstance;

    // minecraft client
    private final Minecraft minecraft = Minecraft.getInstance();

    // minecraft window
    private final Window mWindow = minecraft.getWindow();

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
    private final ConcurrentLinkedQueue<TimedAction> mTasks = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<TimedAction> mAnimationTasks = new ConcurrentLinkedQueue<>();

    // animation update callback
    private final LongConsumer mAnimationHandler;

    // elapsed ticks from a gui open, update every tick, 20 = 1 second
    private int mTicks = 0;

    // elapsed time from a gui open in milliseconds, update every frame
    private long mElapsedTimeMillis;

    // time for drawing
    private long mFrameTimeMillis;

    // time for tasks
    private long mUptimeMillis;

    // lazy loading
    private final GLFramebuffer mFramebuffer;

    private final Object mRenderLock = new Object();

    private MotionEvent mPendingMouseEvent;

    private boolean mFirstScreenOpened = false;
    private boolean mProjectionChanged = false;

    private PointerIcon mOldCursor = PointerIcon.getSystemIcon(PointerIcon.TYPE_DEFAULT);
    private PointerIcon mNewCursor = mOldCursor;

    private final Matrix4 mProjectionMatrix = new Matrix4();

    private final Predicate<? super TimedAction> mUiHandler = task -> task.execute(mUptimeMillis);

    private UIManager() {
        mAnimationHandler = AnimationHandler.init();
        mFramebuffer = new GLFramebuffer(4);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @RenderThread
    public static void initialize() {
        RenderCore.checkRenderThread();
        if (sInstance == null) {
            final UIManager m = new UIManager();
            sInstance = m;
            if (m.mCanvas == null) {
                m.mCanvas = GLCanvas.initialize();
                glEnable(GL_MULTISAMPLE);
                m.mFramebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT0, GL_RGBA8);
                m.mFramebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT1, GL_RGBA8);
                m.mFramebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT2, GL_RGBA8);
                m.mFramebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT3, GL_RGBA8);
                // no depth buffer
                m.mFramebuffer.addRenderbufferAttachment(GL_STENCIL_ATTACHMENT, GL_STENCIL_INDEX8);
                m.mFramebuffer.setDrawBuffer(GL_COLOR_ATTACHMENT0);
                new Thread(m::run, "UI thread").start();
                ModernUI.LOGGER.info(MARKER, "UI system initialized");
            }
            try {
                Field f = MuiForgeBridge.class.getDeclaredField("sUIManager");
                f.setAccessible(true);
                f.set(null, m);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Open an application UI and create views.
     *
     * @param callback the application user interface
     * @see #start(MuiScreen)
     */
    public void openGui(@Nonnull ScreenCallback callback) {
        mCallback = callback;
        minecraft.setScreen(new SimpleScreen(this));
    }

    // Internal method
    public boolean openMenu(@Nonnull LocalPlayer p, @Nonnull AbstractContainerMenu menu, String namespace) {
        if (!minecraft.isSameThread()) {
            throw new IllegalStateException("Not on main thread");
        }
        OpenMenuEvent event = new OpenMenuEvent(menu);
        ModernUIForge.fire(namespace, event);
        ScreenCallback callback = event.getCallback();
        if (callback == null) {
            return false;
        }
        mCallback = callback;
        p.containerMenu = menu;
        minecraft.setScreen(new MenuScreen<>(menu, p.getInventory(), this));
        return true;
    }

    /**
     * Get elapsed time in UI, update every frame. Internal use only.
     *
     * @return drawing time in milliseconds
     */
    public long getElapsedTime() {
        return mElapsedTimeMillis;
    }

    /**
     * Get synced frame time, update every frame
     *
     * @return frame time in milliseconds
     */
    public long getFrameTimeMillis() {
        return mFrameTimeMillis;
    }

    @Nullable
    public ScreenCallback getCallback() {
        return mCallback;
    }

    // Called when open a screen from Modern UI, or back to the screen
    void start(@Nonnull MuiScreen screen) {
        if (mScreen == null) {
            mCallback.host = this;
            post(mCallback::onCreate);
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
            stop();
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

    @Override
    protected void updatePointerIcon(@Nullable PointerIcon pointerIcon) {
        mNewCursor = pointerIcon == null ? PointerIcon.getSystemIcon(PointerIcon.TYPE_DEFAULT) : pointerIcon;
    }

    /**
     * Post a task that will run on UI thread in specified milliseconds.
     *
     * @param r           runnable task
     * @param delayMillis delayed time to run the task in milliseconds
     * @return if successful
     */
    @Override
    public boolean postDelayed(@Nonnull Runnable r, long delayMillis) {
        if (delayMillis < 0) {
            delayMillis = 0;
        }
        return mTasks.offer(TimedAction.obtain(r, System.currentTimeMillis() + delayMillis));
    }

    @Override
    public void postOnAnimationDelayed(@Nonnull Runnable r, long delayMillis) {
        if (delayMillis < 0) {
            delayMillis = 0;
        }
        mAnimationTasks.offer(TimedAction.obtain(r, System.currentTimeMillis() + delayMillis));
    }

    @Override
    public void removeCallbacks(@Nonnull Runnable r) {
        Predicate<? super TimedAction> pred = t -> t.remove(r);
        mTasks.removeIf(pred);
        mAnimationTasks.removeIf(pred);
    }

    @UiThread
    private void run() {
        initBase();
        mDecor = new DecorView();
        mDecor.setClickable(true);
        mDecor.setFocusableInTouchMode(true);
        mDecor.setWillNotDraw(true);
        setView(mDecor);
        ModernUI.LOGGER.info(MARKER, "View system initialized");
        for (; ; ) {
            LockSupport.park();

            // holds the lock
            synchronized (mRenderLock) {
                try {
                    // 1. handle tasks
                    mUptimeMillis = System.currentTimeMillis();
                    if (!mTasks.isEmpty()) {
                        // batched processing
                        mTasks.removeIf(mUiHandler);
                    }

                    // 2. do input events
                    doProcessInputEvents();

                    // 3. do animations
                    mAnimationHandler.accept(mFrameTimeMillis);
                    if (!mAnimationTasks.isEmpty()) {
                        mAnimationTasks.removeIf(mUiHandler);
                    }

                    // 4. do traversal
                    doTraversal();
                } catch (Throwable t) {
                    ModernUI.LOGGER.error(MARKER, "An error occurred on UI thread", t);
                    Minecraft.getInstance().delayCrash(CrashReport.forThrowable(t, "Exception on UI thread"));
                    throw t;
                }
            }
        }

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
        enqueueInputEvent(event);
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
        if (minecraft.getOverlay() == null && mScreen != null) {
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
            int actionButton = 1 << event.getButton();
            int action = event.getAction() == GLFW_PRESS ?
                    MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP;
            if ((action == MotionEvent.ACTION_DOWN && (buttonState ^ actionButton) == 0)
                    || (action == MotionEvent.ACTION_UP && buttonState == 0)) {
                mPendingMouseEvent = MotionEvent.obtain(now, now, action, actionButton,
                        x, y, event.getMods(), buttonState, 0);
            }
        }
    }

    void onMouseButton() {
        if (mPendingMouseEvent != null) {
            enqueueInputEvent(mPendingMouseEvent);
            mPendingMouseEvent = null;
        }
    }

    // Hook method, DO NOT CALL
    public static void onScroll(double scrollX, double scrollY) {
        if (sInstance.mScreen != null) {
            final long now = RenderCore.timeNanos();
            float x = (float) sInstance.minecraft.mouseHandler.xpos();
            float y = (float) sInstance.minecraft.mouseHandler.ypos();
            MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_SCROLL,
                    x, y, 0);
            event.setAxisValue(MotionEvent.AXIS_HSCROLL, (float) scrollX);
            event.setAxisValue(MotionEvent.AXIS_VSCROLL, (float) scrollY);
            sInstance.enqueueInputEvent(event);
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
            case GLFW_KEY_C:
                // take a screenshot, MSAA framebuffer doesn't support that
                /*Bitmap bitmap = Bitmap.download(Bitmap.Format.RGBA, mFramebuffer, true);
                Util.ioPool().execute(() -> {
                    try (bitmap) {
                        bitmap.saveDialog(Bitmap.SaveFormat.PNG, 0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });*/
                break;

            case GLFW_KEY_H:
                TestMain.sTrack.play();
                break;

            case GLFW_KEY_J:
                TestMain.sTrack.pause();
                break;

            case GLFW_KEY_G:
                if (minecraft.screen == null && minecraft.hasSingleplayerServer() &&
                        minecraft.getSingleplayerServer() != null && !minecraft.getSingleplayerServer().isPublished()) {
                    openGui(new TestPauseUI());
                }
                /*minecraft.getLanguageManager().getLanguages().forEach(l ->
                        ModernUI.LOGGER.info(MARKER, "Locale {} RTL {}", l.getCode(), ULocale.forLocale(l
                        .getJavaLocale()).isRightToLeft()));*/
                break;

            case GLFW_KEY_P:
                printDebugInfo();
                break;
        }
    }

    private void printDebugInfo() {
        StringBuilder builder = new StringBuilder();
        builder.append("Modern UI debug info\n");

        builder.append("Use Modern UI: ");
        builder.append(mScreen != null);
        builder.append('\n');

        builder.append("Container Menu: ");
        AbstractContainerMenu menu = null;
        if (minecraft.player != null) {
            menu = minecraft.player.containerMenu;
        }
        if (menu != null) {
            builder.append(menu.getClass().getName());
            builder.append('\n');
            try {
                //noinspection ConstantConditions
                String ns = menu.getType().getRegistryName().getNamespace();
                builder.append("Namespace: ");
                builder.append(ns);
                builder.append('\n');
            } catch (Exception ignored) {
            }
        } else {
            builder.append("null");
            builder.append('\n');
        }

        builder.append("Callback or Screen: ");
        builder.append(mCallback != null ? mCallback.getClass() : minecraft.screen == null ? null :
                minecraft.screen.getClass());
        builder.append('\n');

        builder.append("Layout Cache Entries: ");
        builder.append(TextLayoutEngine.getInstance().countEntries());

        String str = builder.toString();

        ModernUI.LOGGER.info(MARKER, str);
        if (minecraft.level != null && ModernUIForge.isDeveloperMode()) {
            minecraft.gui.getChat().addMessage(Component.nullToEmpty(str));
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
        post(() -> {
            View v = mDecor.findFocus();
            if (v instanceof TextView tv) {
                Editable editable = tv.getEditableText();
                if (editable != null) {
                    editable.append(ch);
                }
            }
        });
        return true;//root.charTyped(codePoint, modifiers);
    }

    @RenderThread
    void render() {
        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        RenderSystem.activeTexture(GL_TEXTURE0);
        RenderSystem.disableDepthTest();

        // blend alpha correctly, since the Minecraft.mainRenderTarget has no alpha (always 1)
        // and our framebuffer is always a transparent layer
        RenderSystem.blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        int width = mWindow.getWidth();
        int height = mWindow.getHeight();

        GLCanvas canvas = mCanvas;
        GLFramebuffer framebuffer = mFramebuffer;

        if (mProjectionChanged) {
            // Test
            mProjectionChanged = false;
        }
        canvas.setProjection(mProjectionMatrix.setOrthographic(width, -height, 0, 2000));

        // This is on Main thread
        if (mNewCursor != mOldCursor) {
            glfwSetCursor(mWindow.getWindow(), mNewCursor.getHandle());
            mOldCursor = mNewCursor;
        }

        final int oldVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        final int oldProgram = glGetInteger(GL_CURRENT_PROGRAM);

        // wait UI thread, if slow
        synchronized (mRenderLock) {
            if (hasDrawn()) {
                glEnable(GL_STENCIL_TEST);

                try {
                    canvas.draw(framebuffer);
                } catch (Throwable t) {
                    ModernUI.LOGGER.fatal(MARKER,
                            "Failed to invoke rendering callbacks, please report the issue to related mods", t);
                    printDebugInfo();
                    throw t;
                }

                glDisable(GL_STENCIL_TEST);
            }
        }
        GLTexture texture = framebuffer.getAttachedTexture(GL_COLOR_ATTACHMENT0);

        // draw MSAA off-screen result to Minecraft main framebuffer (not the default framebuffer)
        RenderSystem.defaultBlendFunc();
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, minecraft.getMainRenderTarget().frameBufferId);

        // do alpha fade in
        int alpha = (int) Math.min(0xff, mElapsedTimeMillis);
        canvas.drawTexture(texture, 0, 0, width, height, alpha << 24 | 0xffffff, true);
        canvas.draw(null);

        glBindVertexArray(oldVertexArray);
        glUseProgram(oldProgram);

        // force changing Blaze3D state
        RenderSystem.bindTexture(DEFAULT_TEXTURE);
    }

    @SubscribeEvent
    void onRenderGameOverlayLayer(@Nonnull RenderGameOverlayEvent.PreLayer event) {
        /*switch (event.getType()) {
            case CROSSHAIRS:
                event.setCanceled(mScreen != null);
                break;
            case ALL:
                // hotfix 1.16 vanilla, using shader makes TEXTURE_2D disabled
                RenderSystem.enableTexture();
                break;
            *//*case HEALTH:
                if (TestHUD.sBars)
                    TestHUD.sInstance.drawBars(mFCanvas);
                break;*//*
        }*/
        if (event.getOverlay() == ForgeIngameGui.CROSSHAIR_ELEMENT) {
            if (mScreen != null) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    void onRenderTooltip(@Nonnull RenderTooltipEvent.Pre event) {
        if (TooltipRenderer.sTooltip) {
            /*if (!(minecraft.font instanceof ModernFontRenderer)) {
                ModernUI.LOGGER.fatal(MARKER, "Failed to hook FontRenderer, tooltip disabled");
                TestHUD.sTooltip = false;
                return;
            }*/
            final Window window = minecraft.getWindow();
            // screen coordinates to pixels for rendering
            final MouseHandler mouseHandler = minecraft.mouseHandler;
            // screen coordinates to pixels for rendering
            double cursorX = mouseHandler.xpos() *
                    (double) window.getGuiScaledWidth() / (double) window.getScreenWidth();
            double cursorY = mouseHandler.ypos() *
                    (double) window.getGuiScaledHeight() / (double) window.getScreenHeight();
            if (event.getLines().isEmpty()) {
                TooltipRenderer.drawTooltip(mCanvas, window, event.getMatrixStack(), event.getComponents(),
                        event.getX(), event.getY(), event.getFontRenderer(), event.getScreenWidth(),
                        event.getScreenHeight(), cursorX, cursorY, minecraft.getItemRenderer(), minecraft);
            } else {
                TooltipRenderer.drawTooltip(mCanvas, event.getLines(), event.getFontRenderer(), event.getStack(),
                        event.getMatrixStack(), event.getX(), event.getY(), (float) cursorX, (float) cursorY,
                        event.getMaxWidth(), event.getScreenWidth(), event.getScreenHeight(), window.getWidth(),
                        window.getHeight());
            }
            event.setCanceled(true);
        }
    }

    /**
     * Called when game window size changed, used to re-layout the window.
     */
    void resize() {
        post(() -> setFrame(mWindow.getWidth(), mWindow.getHeight()));
        mProjectionChanged = true;
    }

    void stop() {
        if (!mCloseScreen || mScreen == null) {
            return;
        }
        mTasks.clear();
        mAnimationTasks.clear();
        mScreen = null;
        if (mCallback != null) {
            mCallback.host = null;
            mCallback = null;
        }
        updatePointerIcon(null);
        minecraft.keyboardHandler.setSendRepeatsToGui(false);
        post(() -> mDecor.removeAllViews());
    }

    @Deprecated
    @SubscribeEvent
    void onClientTick(@Nonnull TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ++mTicks;
            post(this::tick);
        }
        /* else {
            if (mPendingRepostCursorEvent) {
                onCursorPos();
            }
        }*/
    }

    @Deprecated
    private void tick() {
        if (mView != null) {
            mView.tick();
        }
    }

    @SubscribeEvent
    void onRenderTick(@Nonnull TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            final long lastFrameTime = mFrameTimeMillis;
            mFrameTimeMillis = RenderCore.timeMillis();
            final long deltaMillis = mFrameTimeMillis - lastFrameTime;
            mElapsedTimeMillis += deltaMillis;
            if (mScreen != null && minecraft.screen == mScreen) {
                LockSupport.unpark(mThread);
            }
            BlurHandler.INSTANCE.update(mElapsedTimeMillis);
            if (TooltipRenderer.sTooltip) {
                TooltipRenderer.update(deltaMillis);
            }
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
    //private final Map<ContainerType<?>, Function<? extends Container, ApplicationUI>> mScreenRegistry = new
    // HashMap<>();

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
            ModernUI.LOGGER.warn(MARKER, "#openPopup() shouldn't be called when there's already a popup, the previous
             one has been overwritten");
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
