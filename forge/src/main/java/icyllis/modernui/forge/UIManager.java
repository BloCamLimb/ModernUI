/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.forge;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.animation.AnimationHandler;
import icyllis.modernui.animation.AnimationUtils;
import icyllis.modernui.animation.LayoutTransition;
import icyllis.modernui.annotation.MainThread;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.core.*;
import icyllis.modernui.fragment.*;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.GLFramebuffer;
import icyllis.modernui.graphics.GLSurfaceCanvas;
import icyllis.modernui.graphics.opengl.GLTexture;
import icyllis.modernui.lifecycle.*;
import icyllis.modernui.math.Matrix4;
import icyllis.modernui.text.Editable;
import icyllis.modernui.text.Selection;
import icyllis.modernui.util.TimedAction;
import icyllis.modernui.view.*;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.TextView;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.LongConsumer;
import java.util.function.Predicate;

import static icyllis.modernui.ModernUI.LOGGER;
import static icyllis.modernui.graphics.GLWrapper.*;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Manage UI thread and connect Minecraft to Modern UI view system at most bottom level.
 * This class is public only for some hooking methods.
 */
@ApiStatus.Internal
@NotThreadSafe
@OnlyIn(Dist.CLIENT)
public final class UIManager implements LifecycleOwner {

    // the logger marker
    static final Marker MARKER = MarkerManager.getMarker("UIManager");

    // configs
    static volatile boolean sPlaySoundOnLoaded;

    // the global instance
    static volatile UIManager sInstance;

    // message IDs
    private static final int MSG_DO_FRAME = 1;
    private static final int MSG_SET_FRAME = 2;

    // Constant IDs for Framework package.
    private static final int content = 0x01020001;
    private static final int fragment_container = 0x01020007;

    static final KeyMapping OPEN_CENTER_KEY = new KeyMapping(
            "key.modernui.openCenter", KeyConflictContext.UNIVERSAL, KeyModifier.CONTROL,
            InputConstants.Type.KEYSYM, GLFW_KEY_K, "Modern UI");

    static final Method SEND_TO_CHAT = ObfuscationReflectionHelper.findMethod(ChatComponent.class, "m_93790_",
            Component.class, int.class, int.class, boolean.class);

    // minecraft
    private final Minecraft minecraft = Minecraft.getInstance();

    // minecraft window
    private final Window mWindow = minecraft.getWindow();

    // the UI thread
    private volatile Thread mUiThread;
    private volatile Looper mLooper;
    private volatile boolean mRunning;

    // the view root impl
    private ViewRootImpl mViewRoot;

    // the top-level view of the window
    private FrameLayout mDecor;
    private FragmentContainerView mFragmentContainerView;


    /// Task Handling \\\

    // elapsed time from a screen open in milliseconds, Render thread
    private long mElapsedTimeMillis;

    // time for drawing, Render thread
    private long mFrameTimeMillis;

    // time for tasks, UI thread
    private long mUptimeMillis;

    // animation update callback
    private LongConsumer mAnimationHandler;
    // other animation tasks
    private final ConcurrentLinkedQueue<TimedAction> mAnimationTasks = new ConcurrentLinkedQueue<>();
    private final Predicate<? super TimedAction> mAnimationTaskHandler = task -> task.execute(mUptimeMillis);


    /// Rendering \\\

    // the UI framebuffer
    private final GLFramebuffer mFramebuffer;
    GLSurfaceCanvas mCanvas;
    private final Matrix4 mProjectionMatrix = new Matrix4();


    /// User Interface \\\

    // indicates whether the current screen is a Modern UI screen
    @Nullable
    private MuiScreen mScreen;

    private boolean mFirstScreenOpened = false;
    // true if there will be no screen to open
    private boolean mCloseScreen;

    // the main fragment used to send lifecycle events
    private Fragment mFragment;
    UICallback mCallback;


    /// Lifecycle \\\

    LifecycleRegistry mFragmentLifecycleRegistry;
    private final OnBackPressedDispatcher mOnBackPressedDispatcher =
            new OnBackPressedDispatcher(() -> minecraft.tell(this::onBackPressed));

    private ViewModelStore mViewModelStore;
    volatile FragmentController mFragmentController;


    /// Input Event \\\

    private MotionEvent mPendingMouseEvent;
    private int mButtonState;

    private PointerIcon mOldCursor = PointerIcon.getSystemIcon(PointerIcon.TYPE_DEFAULT);
    private PointerIcon mNewCursor = mOldCursor;

    private UIManager() {
        mFramebuffer = new GLFramebuffer(4);
        mFramebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT0, GL_RGBA8);
        mFramebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT1, GL_RGBA8);
        mFramebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT2, GL_RGBA8);
        mFramebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT3, GL_RGBA8);
        // no depth buffer
        mFramebuffer.addRenderbufferAttachment(GL_STENCIL_ATTACHMENT, GL_STENCIL_INDEX8);
        mFramebuffer.setDrawBuffer(GL_COLOR_ATTACHMENT0);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @RenderThread
    static void initialize() {
        ArchCore.checkRenderThread();
        assert sInstance == null;
        final UIManager mgr = new UIManager();
        sInstance = mgr;
        assert mgr.mCanvas == null;
        mgr.mCanvas = GLSurfaceCanvas.initialize();
        glEnable(GL_MULTISAMPLE);
        (sInstance.mUiThread = new Thread(UIManager::run, "UI thread")).start();
        sInstance.mRunning = true;
        LOGGER.info(MARKER, "UI system initialized");
    }

    @UiThread
    private static void run() {
        sInstance.init();
        while (true) {
            try {
                Looper.loop();
            } catch (Throwable e) {
                LOGGER.error(MARKER, "An error occurred on UI thread", e);
                // dev can add breakpoints
                if (sInstance.mRunning && ModernUIForge.isDeveloperMode()) {
                    continue;
                } else {
                    Minecraft.getInstance().tell(sInstance::dump);
                    Minecraft.getInstance().tell(() -> Minecraft.crash(
                            CrashReport.forThrowable(e, "Exception on UI thread")));
                }
            }
            break;
        }
    }

    /**
     * Schedule UI and create views.
     *
     * @param fragment the main fragment
     * @param callback the user interface callbacks
     */
    @MainThread
    void start(@Nonnull Fragment fragment, @Nullable UICallback callback) {
        mFragment = fragment;
        mCallback = callback;
        clearFramebuffer();
        minecraft.setScreen(new SimpleScreen());
    }

    @MainThread
    void start(LocalPlayer p, AbstractContainerMenu menu, @Nonnull ResourceLocation key) {
        assert minecraft.isSameThread();
        final OpenMenuEvent event = new OpenMenuEvent(menu);
        ModernUIForge.post(key.getNamespace(), event);
        final Fragment fragment = event.getFragment();
        if (fragment == null) {
            p.closeContainer(); // close server menu whatever it is
            LOGGER.debug(MARKER, "No fragment set, closing {} keyed {}", menu, key);
            return;
        }
        mFragment = fragment;
        mCallback = event.getCallback();
        clearFramebuffer();
        p.containerMenu = menu;
        minecraft.setScreen(new MenuScreen<>(menu, p.getInventory()));
    }

    void clearFramebuffer() {
        if (mFramebuffer.getAttachment(GL_COLOR_ATTACHMENT0).getWidth() > 0) {
            mFramebuffer.clearColorBuffer();
            mFramebuffer.clearDepthStencilBuffer();
        }
    }

    void onBackPressed() {
        if (mScreen == null)
            return;
        if (mCallback != null && !mCallback.shouldClose()) {
            return;
        }
        if (mScreen instanceof MenuScreen) {
            if (minecraft.player != null) {
                minecraft.player.closeContainer();
            }
        } else {
            minecraft.setScreen(null);
        }
    }

    /**
     * Get elapsed time in UI, update every frame. Internal use only.
     *
     * @return drawing time in milliseconds
     */
    long getElapsedTime() {
        return mElapsedTimeMillis;
    }

    /**
     * Get synced frame time, update every frame
     *
     * @return frame time in milliseconds
     */
    long getFrameTime() {
        return mFrameTimeMillis;
    }

    FrameLayout getDecorView() {
        return mDecor;
    }

    @Nonnull
    @Override
    public Lifecycle getLifecycle() {
        // constant reference
        return mFragmentLifecycleRegistry;
    }

    // Called when open a screen from Modern UI, or back to the screen
    void initScreen(@Nonnull MuiScreen screen) {
        if (mScreen == null) {
            if (mFragment != null) {
                mFragmentController.getFragmentManager().beginTransaction()
                        .add(fragment_container, mFragment, "main")
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .runOnCommit(mViewRoot::resetCanvasLocked)
                        .commit();
            }
            mViewRoot.mHandler.post(this::restoreLayoutTransition);
            mScreen = screen;
        }

        resize();
    }

    void suppressLayoutTransition() {
        LayoutTransition transition = mDecor.getLayoutTransition();
        transition.disableTransitionType(LayoutTransition.APPEARING);
        transition.disableTransitionType(LayoutTransition.DISAPPEARING);
    }

    void restoreLayoutTransition() {
        LayoutTransition transition = mDecor.getLayoutTransition();
        transition.enableTransitionType(LayoutTransition.APPEARING);
        transition.enableTransitionType(LayoutTransition.DISAPPEARING);
    }

    @SubscribeEvent
    void onGuiOpen(@Nonnull ScreenOpenEvent event) {
        final Screen next = event.getScreen();
        mCloseScreen = next == null;

        if (!mFirstScreenOpened) {
            if (sPlaySoundOnLoaded) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f));
            }
            mFirstScreenOpened = true;
        }

        if (mCloseScreen) {
            removed();
            return;
        }

        if (mScreen != next && next instanceof MuiScreen) {
            //mTicks = 0;
            mElapsedTimeMillis = 0;
        }
        if (mScreen != next && mScreen != null) {
            onCursorPos();
        }
        // for non-mui screens
        if (mScreen == null) {
            //mTicks = 0;
            mElapsedTimeMillis = 0;
        }
    }

    @MainThread
    private void applyPointerIcon() {
        if (mNewCursor != mOldCursor) {
            glfwSetCursor(mWindow.getWindow(), mNewCursor.getHandle());
            mOldCursor = mNewCursor;
        }
    }

    @UiThread
    private void init() {
        long startTime = System.nanoTime();
        mViewRoot = this.new ViewRootImpl();

        mAnimationHandler = AnimationHandler.getInstance().getCallback();

        mDecor = new FrameLayout();
        // make the root view clickable through, so that views can lose focus
        mDecor.setClickable(true);
        mDecor.setFocusableInTouchMode(true);
        mDecor.setWillNotDraw(true);
        mDecor.setId(content);

        mFragmentContainerView = new FragmentContainerView();
        mFragmentContainerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mFragmentContainerView.setWillNotDraw(true);
        mFragmentContainerView.setId(fragment_container);
        mDecor.addView(mFragmentContainerView);

        mDecor.setLayoutTransition(new LayoutTransition());
        suppressLayoutTransition();

        mViewRoot.setView(mDecor);

        mFragmentLifecycleRegistry = new LifecycleRegistry(this);
        mViewModelStore = new ViewModelStore();
        mFragmentController = FragmentController.createController(this.new HostCallbacks());

        mFragmentController.attachHost(null);

        mFragmentLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mFragmentController.dispatchCreate();

        mFragmentController.dispatchActivityCreated();
        mFragmentController.execPendingActions();

        mFragmentLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        mFragmentController.dispatchStart();

        mLooper = Objects.requireNonNull(Looper.myLooper());

        LOGGER.info(MARKER, "View system initialized in {}ms", (System.nanoTime() - startTime) / 1000000);

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

    @UiThread
    private void finish() {
        mFragmentController.dispatchStop();
        mFragmentLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);

        mFragmentController.dispatchDestroy();
        mFragmentLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);

        mLooper.quitSafely();
    }

    /**
     * From screen
     *
     * @see org.lwjgl.glfw.GLFWCursorPosCallbackI
     * @see net.minecraft.client.MouseHandler
     * @see MuiScreen
     */
    void onCursorPos() {
        final long now = ArchCore.timeNanos();
        float x = (float) minecraft.mouseHandler.xpos();
        float y = (float) minecraft.mouseHandler.ypos();
        MotionEvent event = MotionEvent.obtain(now, MotionEvent.ACTION_HOVER_MOVE,
                x, y, 0);
        mViewRoot.enqueueInputEvent(event);
        //mPendingRepostCursorEvent = false;
        if (mButtonState > 0) {
            event = MotionEvent.obtain(now, MotionEvent.ACTION_MOVE, 0, x, y, 0, mButtonState, 0);
            mViewRoot.enqueueInputEvent(event);
        }
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
            final long now = ArchCore.timeNanos();
            float x = (float) minecraft.mouseHandler.xpos();
            float y = (float) minecraft.mouseHandler.ypos();
            int buttonState = 0;
            for (int i = 0; i < 5; i++) {
                if (glfwGetMouseButton(mWindow.getWindow(), i) == GLFW_PRESS) {
                    buttonState |= 1 << i;
                }
            }
            mButtonState = buttonState;
            int actionButton = 1 << event.getButton();
            int action = event.getAction() == GLFW_PRESS ?
                    MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP;
            if ((action == MotionEvent.ACTION_DOWN && (buttonState ^ actionButton) == 0)
                    || (action == MotionEvent.ACTION_UP && buttonState == 0)) {
                if (mPendingMouseEvent != null) {
                    mPendingMouseEvent.recycle();
                }
                mPendingMouseEvent = MotionEvent.obtain(now, action, actionButton,
                        x, y, event.getModifiers(), buttonState, 0);
            }
        }
    }

    void onMouseButton() {
        // Only response at most one mouse button event each frame
        if (mPendingMouseEvent != null) {
            mViewRoot.enqueueInputEvent(mPendingMouseEvent);
            mPendingMouseEvent = null;
        }
    }

    // Hook method, DO NOT CALL
    public static void onScroll(double scrollX, double scrollY) {
        if (sInstance.mScreen != null) {
            final long now = ArchCore.timeNanos();
            float x = (float) sInstance.minecraft.mouseHandler.xpos();
            float y = (float) sInstance.minecraft.mouseHandler.ypos();
            MotionEvent event = MotionEvent.obtain(now, MotionEvent.ACTION_SCROLL,
                    x, y, 0);
            event.setAxisValue(MotionEvent.AXIS_HSCROLL, (float) scrollX);
            event.setAxisValue(MotionEvent.AXIS_VSCROLL, (float) scrollY);
            sInstance.mViewRoot.enqueueInputEvent(event);
        }
    }

    @SubscribeEvent
    void onPostKeyInput(@Nonnull InputEvent.KeyInputEvent event) {
        if (mScreen != null) {
            int action = event.getAction() == GLFW_RELEASE ? KeyEvent.ACTION_UP : KeyEvent.ACTION_DOWN;
            KeyEvent keyEvent = KeyEvent.obtain(ArchCore.timeNanos(), action, event.getKey(), 0,
                    event.getModifiers(), event.getScanCode(), 0);
            mViewRoot.enqueueInputEvent(keyEvent);
        }
        if (event.getAction() != GLFW_PRESS) {
            return;
        }
        InputConstants.Key key = InputConstants.getKey(event.getKey(), event.getScanCode());
        if (OPEN_CENTER_KEY.isActiveAndMatches(key)) {
            start(new CenterFragment(), new UICallback());
            return;
        }
        if (!Screen.hasControlDown() || !ModernUIForge.isDeveloperMode()) {
            return;
        }
        switch (event.getKey()) {
            case GLFW_KEY_C:
                // take a screenshot from MSAA framebuffer
                GLTexture sampled = GLFramebuffer.swap(mFramebuffer, GL_COLOR_ATTACHMENT0);
                NativeImage image = NativeImage.download(NativeImage.Format.RGBA, sampled, true);
                Util.ioPool().execute(() -> {
                    try (image) {
                        image.saveDialog(NativeImage.SaveFormat.PNG, 0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                break;

            /*case GLFW_KEY_H:
                TestMain.sTrack.play();
                break;

            case GLFW_KEY_J:
                TestMain.sTrack.pause();
                break;

            case GLFW_KEY_N:
                mViewRoot.mHandler.post(() -> mDecor.setLayoutDirection(View.LAYOUT_DIRECTION_RTL));
                break;

            case GLFW_KEY_M:
                mViewRoot.mHandler.post(() -> mDecor.setLayoutDirection(View.LAYOUT_DIRECTION_INHERIT));
                break;*/

            case GLFW_KEY_P:
                dump();
                break;

            case GLFW_KEY_G:
                /*if (minecraft.screen == null && minecraft.isLocalServer() &&
                        minecraft.getSingleplayerServer() != null && !minecraft.getSingleplayerServer().isPublished()) {
                    start(new TestPauseUI());
                }*/
                /*minecraft.getLanguageManager().getLanguages().forEach(l ->
                        ModernUI.LOGGER.info(MARKER, "Locale {} RTL {}", l.getCode(), ULocale.forLocale(l
                        .getJavaLocale()).isRightToLeft()));*/
                break;
        }
    }

    private void dump() {
        StringBuilder builder = new StringBuilder();
        try (var w = new PrintWriter(new StringBuilderWriter(builder))) {
            dump(w);
        }
        String str = builder.toString();
        if (minecraft.level != null) {
            try {
                SEND_TO_CHAT.invoke(minecraft.gui.getChat(), new TextComponent(str).withStyle(ChatFormatting.GRAY),
                        0xCBD366, minecraft.gui.getGuiTicks(), false);
            } catch (IllegalAccessException | InvocationTargetException ignored) {
            }
        }
        LOGGER.info(MARKER, str);
    }

    private void dump(@Nonnull PrintWriter w) {
        w.println(">>> Modern UI debug info <<<");

        w.print("Graphics API: OpenGL ");
        w.println(glGetString(GL_VERSION));
        w.println("Render Pipeline: OpenGL 4.5 Core (ARB enabled)");

        w.print("Container Menu: ");
        LocalPlayer player = minecraft.player;
        AbstractContainerMenu menu = null;
        if (player != null) {
            menu = player.containerMenu;
        }
        if (menu != null) {
            w.println(menu.getClass().getSimpleName());
            try {
                ResourceLocation name = menu.getType().getRegistryName();
                w.print(" \u21B3Registry Name: ");
                w.println(name);
            } catch (Exception ignored) {
            }
        } else {
            w.println((Object) null);
        }

        if (mCallback != null) {
            w.print("Callback: ");
            w.println(mCallback.getClass());
        } else {
            Screen screen = minecraft.screen;
            if (screen != null) {
                w.print("Screen: ");
                w.println(screen.getClass());
            }
        }

        if (mFragmentController != null) {
            mFragmentController.getFragmentManager().dump("FM", null, w);
        }

        ModernUIForge.dispatchOnDebugDump(w);
    }

    boolean charTyped(char ch) {
        /*if (popup != null) {
            return popup.charTyped(codePoint, modifiers);
        }*/
        /*if (mKeyboard != null) {
            return mKeyboard.onCharTyped(codePoint, modifiers);
        }*/
        Message msg = Message.obtain(mViewRoot.mHandler, () -> {
            final Editable content;
            if (mDecor.findFocus() instanceof TextView tv && (content = tv.getEditableText()) != null) {
                int selStart = tv.getSelectionStart();
                int selEnd = tv.getSelectionEnd();
                if (selStart >= 0 && selEnd >= 0) {
                    Selection.setSelection(content, Math.max(selStart, selEnd));
                    content.replace(Math.min(selStart, selEnd), Math.max(selStart, selEnd), String.valueOf(ch));
                }
            }
        });
        msg.setAsynchronous(true);
        msg.sendToTarget();
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

        GLSurfaceCanvas canvas = mCanvas;

        canvas.setProjection(mProjectionMatrix.setOrthographic(width, height, 0, 3000));

        // This is on Main thread
        applyPointerIcon();

        final int oldVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        final int oldProgram = glGetInteger(GL_CURRENT_PROGRAM);

        mViewRoot.flushDrawCommands(canvas, mFramebuffer, width, height);

        glBindVertexArray(oldVertexArray);
        glUseProgram(oldProgram);

        RenderSystem.defaultBlendFunc();
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
            final Window window = mWindow;
            // screen coordinates to pixels for rendering
            final MouseHandler mouseHandler = minecraft.mouseHandler;
            // screen coordinates to pixels for rendering
            double cursorX = mouseHandler.xpos() *
                    (double) window.getGuiScaledWidth() / (double) window.getScreenWidth();
            double cursorY = mouseHandler.ypos() *
                    (double) window.getGuiScaledHeight() / (double) window.getScreenHeight();
            //if (event.getLines().isEmpty()) {
            TooltipRenderer.drawTooltip(mCanvas, window, event.getPoseStack(), event.getComponents(),
                    event.getX(), event.getY(), event.getFont(), event.getScreenWidth(),
                    event.getScreenHeight(), cursorX, cursorY, minecraft.getItemRenderer());
            /*} else {
                TooltipRenderer.drawTooltip(mCanvas, event.getLines(), event.getFontRenderer(), event.getStack(),
                        event.getMatrixStack(), event.getX(), event.getY(), (float) cursorX, (float) cursorY,
                        event.getMaxWidth(), event.getScreenWidth(), event.getScreenHeight(), window.getWidth(),
                        window.getHeight());
            }*/
            event.setCanceled(true);
        }
    }

    /**
     * Called when game window size changed, used to re-layout the window.
     */
    void resize() {
        mViewRoot.mHandler.sendEmptyMessage(MSG_SET_FRAME);
    }

    void removed() {
        if ((!mCloseScreen && minecraft.player != null) || mScreen == null) {
            return;
        }
        mScreen = null;
        if (mCallback != null) {
            mCallback = null;
        }
        mViewRoot.mHandler.post(this::suppressLayoutTransition);
        if (mFragment != null) {
            mFragmentController.getFragmentManager().beginTransaction()
                    .remove(mFragment)
                    .runOnCommit(mFragmentContainerView::removeAllViews)
                    .commit();
        }
        mViewRoot.updatePointerIcon(null);
        applyPointerIcon();
        minecraft.keyboardHandler.setSendRepeatsToGui(false);
    }

    @SubscribeEvent
    void onRenderTick(@Nonnull TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            final long lastFrameTime = mFrameTimeMillis;
            mFrameTimeMillis = ArchCore.timeMillis();
            final long deltaMillis = mFrameTimeMillis - lastFrameTime;
            mElapsedTimeMillis += deltaMillis;
            // coordinates UI thread
            if (mRunning) {
                // do not async, this is on main thread
                mViewRoot.mHandler.sendEmptyMessage(MSG_DO_FRAME);
                // update extension animations
                BlurHandler.INSTANCE.update(mElapsedTimeMillis);
                if (TooltipRenderer.sTooltip) {
                    TooltipRenderer.update(deltaMillis);
                }
            }
        } else {
            // main thread
            if (!minecraft.isRunning() && mRunning) {
                LOGGER.info(MARKER, "Finishing UI thread");
                mRunning = false;
                mViewRoot.mHandler.post(this::finish);
                try {
                    // in case of GLFW is terminated too early
                    mUiThread.join(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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

    class ViewRootImpl extends ViewRoot {

        @Nonnull
        @Override
        protected Canvas beginRecording(int width, int height) {
            mCanvas.reset(width, height);
            return mCanvas;
        }

        @Override
        protected void onKeyEvent(KeyEvent event) {
            if (mScreen != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (!mDecor.isFocused() && mDecor.hasFocus()) {
                    return;
                }
                final boolean back;
                if (mCallback != null) {
                    back = mCallback.isBackKey(event.getKeyCode(), event);
                } else if (mScreen instanceof MenuScreen) {
                    if (event.getKeyCode() == KeyEvent.KEY_ESCAPE) {
                        back = true;
                    } else {
                        InputConstants.Key key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
                        back = Minecraft.getInstance().options.keyInventory.isActiveAndMatches(key);
                    }
                } else {
                    back = event.getKeyCode() == KeyEvent.KEY_ESCAPE;
                }
                if (back) {
                    mOnBackPressedDispatcher.onBackPressed();
                }
            }
        }

        @Override
        protected void updatePointerIcon(@Nullable PointerIcon pointerIcon) {
            mNewCursor = pointerIcon == null ? PointerIcon.getSystemIcon(PointerIcon.TYPE_DEFAULT) : pointerIcon;
        }

        private void resetCanvasLocked() {
            synchronized (mRenderLock) {
                mCanvas.reset(mWindow.getWidth(), mWindow.getHeight());
            }
        }

        @Override
        public void postOnAnimationDelayed(@Nonnull Runnable r, long delayMillis) {
            if (delayMillis < 0) {
                delayMillis = 0;
            }
            mAnimationTasks.offer(TimedAction.obtain(r, ArchCore.timeMillis() + delayMillis));
        }

        @Override
        public void removeCallbacks(@Nonnull Runnable r) {
            Predicate<? super TimedAction> pred = t -> t.remove(r);
            //mTasks.removeIf(pred);
            mAnimationTasks.removeIf(pred);
        }

        @UiThread
        private void doFrame() {
            // 1. handle tasks
            mUptimeMillis = ArchCore.timeMillis();
            AnimationUtils.lockAnimationClock(mFrameTimeMillis);
            /*if (!mTasks.isEmpty()) {
                // batched processing
                mTasks.removeIf(mUiHandler);
            }*/

            // 2. do input events
            doProcessInputEvents();

            // 3. do animations
            mAnimationHandler.accept(mFrameTimeMillis);
            if (!mAnimationTasks.isEmpty()) {
                mAnimationTasks.removeIf(mAnimationTaskHandler);
            }

            // 4. do traversal
            doTraversal();

            AnimationUtils.unlockAnimationClock();
        }

        @Override
        protected boolean handleMessage(@Nonnull Message msg) {
            switch (msg.what) {
                case MSG_DO_FRAME -> doFrame();
                case MSG_SET_FRAME -> setFrame(mWindow.getWidth(), mWindow.getHeight());
            }
            return true;
        }

        @RenderThread
        private void flushDrawCommands(GLSurfaceCanvas canvas, GLFramebuffer framebuffer, int width, int height) {
            // wait UI thread, if slow
            synchronized (mRenderLock) {
                if (mRedrawn) {
                    mRedrawn = false;
                    glEnable(GL_STENCIL_TEST);
                    try {
                        canvas.draw(framebuffer);
                    } catch (Throwable t) {
                        LOGGER.fatal(MARKER,
                                "Failed to invoke rendering callbacks, please report the issue to related mods", t);
                        dump();
                        throw t;
                    }
                    glDisable(GL_STENCIL_TEST);
                }

                GLTexture texture = framebuffer.getAttachedTexture(GL_COLOR_ATTACHMENT0);
                if (texture.getWidth() > 0) {
                    // draw MSAA off-screen target to Minecraft main target (not the default framebuffer)
                    RenderSystem.blendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
                    glBindFramebuffer(GL_DRAW_FRAMEBUFFER, minecraft.getMainRenderTarget().frameBufferId);

                    // do alpha fade in
                    int alpha = (int) Math.min(0xff, mElapsedTimeMillis);
                    alpha = alpha << 8 | alpha;
                    // premultiplied alpha
                    canvas.drawLayer(texture, width, height, alpha << 16 | alpha, true);
                    canvas.draw(null);
                }
            }
        }

        @Override
        public void playSoundEffect(int effectId) {

        }

        @Override
        public boolean performHapticFeedback(int effectId, boolean always) {
            return false;
        }
    }

    @UiThread
    class HostCallbacks extends FragmentHostCallback<Object> implements
            ViewModelStoreOwner,
            OnBackPressedDispatcherOwner {
        HostCallbacks() {
            super(new Handler(Looper.myLooper()));
            assert ArchCore.isOnUiThread();
        }

        @Nullable
        @Override
        public Object onGetHost() {
            // intentionally null
            return null;
        }

        @Nullable
        @Override
        public View onFindViewById(int id) {
            return mDecor.findViewById(id);
        }

        @Nonnull
        @Override
        public ViewModelStore getViewModelStore() {
            return mViewModelStore;
        }

        @Nonnull
        @Override
        public OnBackPressedDispatcher getOnBackPressedDispatcher() {
            return mOnBackPressedDispatcher;
        }

        @Nonnull
        @Override
        public Lifecycle getLifecycle() {
            return mFragmentLifecycleRegistry;
        }
    }

    //boolean mPendingRepostCursorEvent = false;

    // main fragment of a UI
    //private Fragment fragment;

    // main UI view that created from main fragment
    //private View view;

    //private IModule popup;

    //boolean mLayoutRequested = false;
    //private long mLastLayoutTime = 0;

    // elapsed ticks from a gui open, update every tick, 20 = 1 second
    //private int mTicks = 0;

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
