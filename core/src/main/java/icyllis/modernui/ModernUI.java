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

package icyllis.modernui;

import icyllis.modernui.annotation.*;
import icyllis.modernui.core.Window;
import icyllis.modernui.core.*;
import icyllis.modernui.fragment.*;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.drawable.ImageDrawable;
import icyllis.modernui.graphics.font.FontFamily;
import icyllis.modernui.graphics.opengl.*;
import icyllis.modernui.lifecycle.*;
import icyllis.modernui.graphics.Matrix4;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.text.Typeface;
import icyllis.modernui.view.*;
import icyllis.modernui.view.menu.ContextMenuBuilder;
import icyllis.modernui.view.menu.MenuHelper;
import icyllis.modernui.widget.CoordinatorLayout;
import icyllis.modernui.widget.EditText;
import org.apache.logging.log4j.*;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.glfw.GLFWMonitorCallback;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.io.*;
import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.nio.channels.*;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static icyllis.modernui.graphics.opengl.GLCore.*;
import static org.lwjgl.glfw.GLFW.*;

/**
 * The core class of Modern UI.
 */
public class ModernUI implements AutoCloseable, LifecycleOwner {

    public static final String ID = "modernui"; // as well as the namespace
    public static final String NAME_CPT = "ModernUI";

    public static final Logger LOGGER = LogManager.getLogger(NAME_CPT);
    public static final Marker MARKER = MarkerManager.getMarker("Core");

    private static volatile ModernUI sInstance;

    private static final Cleaner sCleaner = Cleaner.create();

    private static final int fragment_container = 0x01020007;

    static {
        if (Runtime.version().feature() < 17) {
            throw new RuntimeException("JRE 17 or above is required");
        }
    }

    private final VulkanManager mVulkanManager = VulkanManager.getInstance();

    private volatile MainWindow mWindow;

    private ViewRootImpl mRoot;
    private CoordinatorLayout mDecor;
    private FragmentContainerView mFragmentContainerView;

    private LifecycleRegistry mLifecycleRegistry;
    private OnBackPressedDispatcher mOnBackPressedDispatcher;

    private ViewModelStore mViewModelStore;
    private FragmentController mFragmentController;

    private Typeface mDefaultTypeface;

    private volatile Looper mUiLooper;
    private volatile Thread mUiThread;
    private volatile Thread mRenderThread;

    public ModernUI() {
        synchronized (ModernUI.class) {
            if (sInstance == null) {
                sInstance = this;
            } else {
                throw new RuntimeException("Multiple instances");
            }
        }
    }

    /**
     * Get Modern UI instance.
     *
     * @return the Modern UI
     */
    public static ModernUI getInstance() {
        return sInstance;
    }

    /**
     * Registers a target and a cleaning action to run when the target becomes phantom
     * reachable. It will be registered with the global cleaner shared across Modern UI.
     * The action object should never hold any reference to the target object.
     *
     * @param target the target to monitor
     * @param action a {@code Runnable} to invoke when the target becomes phantom reachable
     * @return a {@code Cleanable} instance for explicit cleaning
     */
    @Nonnull
    public static Cleanable registerCleanup(@Nonnull Object target, @Nonnull Runnable action) {
        return sCleaner.register(target, action);
    }

    /**
     * Runs the Modern UI with the default application setups.
     * This method is only called by the <code>main()</code> on the main thread.
     */
    @MainThread
    public void run(@Nonnull Fragment fragment) {
        Thread.currentThread().setName("Main-Thread");
        // should be true
        LOGGER.info(MARKER, "AWT headless: {}", GraphicsEnvironment.isHeadless());

        Core.initialize();

        mVulkanManager.initialize();

        LOGGER.info(MARKER, "Initializing window system");
        Monitor monitor = Monitor.getPrimary();

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
        glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_NATIVE_CONTEXT_API);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_ANY_PROFILE);
        glfwWindowHintString(GLFW_X11_CLASS_NAME, NAME_CPT);
        glfwWindowHintString(GLFW_X11_INSTANCE_NAME, NAME_CPT);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        if (monitor == null) {
            LOGGER.info(MARKER, "No monitor connected");
            mWindow = MainWindow.initialize(NAME_CPT, 1280, 720);
        } else {
            VideoMode mode = monitor.getCurrentMode();
            mWindow = MainWindow.initialize(NAME_CPT, (int) (mode.getWidth() * 0.75f),
                    (int) (mode.getHeight() * 0.75f));
            mWindow.center(monitor);
            int[] w = {0}, h = {0};
            glfwGetMonitorPhysicalSize(monitor.getHandle(), w, h);
            LOGGER.info(MARKER, "Primary monitor DPI: {}",
                    25.4 * Math.hypot(mode.getWidth(), mode.getHeight()) / Math.hypot(w[0], h[0]));
        }

        loadDefaultTypeface();

        LOGGER.info(MARKER, "Preparing threads");
        Looper.prepare(mWindow);

        mRenderThread = new Thread(this::runRender, "Render-Thread");
        mRenderThread.start();
        mUiThread = new Thread(() -> runUI(fragment), "UI-Thread");
        mUiThread.start();

        try (NativeImage i16 = NativeImage.decode(null, getResourceChannel(ID, "AppLogo16x.png"));
             NativeImage i32 = NativeImage.decode(null, getResourceChannel(ID, "AppLogo32x.png"));
             NativeImage i48 = NativeImage.decode(null, getResourceChannel(ID, "AppLogo48x.png"))) {
            mWindow.setIcon(i16, i32, i48);
        } catch (IOException e) {
            e.printStackTrace();
        }

        LOGGER.info(MARKER, "Looping main thread");
        Looper.loop();
    }

    @RenderThread
    private void runRender() {
        LOGGER.info(MARKER, "Initializing render thread");
        final Window window = mWindow;
        window.makeCurrent();
        Core.initOpenGL();
        GLCore.showCapsErrorDialog();

        final GLSurfaceCanvas canvas = GLSurfaceCanvas.initialize();
        ShaderManager.getInstance().reload();

        glEnable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_STENCIL_TEST);
        glEnable(GL_MULTISAMPLE);

        final GLFramebuffer framebuffer = new GLFramebuffer(4);
        framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT0, GL_RGBA8);
        framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT1, GL_RGBA8);
        framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT2, GL_RGBA8);
        framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT3, GL_RGBA8);
        framebuffer.addRenderbufferAttachment(GL_STENCIL_ATTACHMENT, GL_STENCIL_INDEX8);
        framebuffer.setDrawBuffer(GL_COLOR_ATTACHMENT0);

        final Matrix4 projection = new Matrix4();

        window.swapInterval(1);
        LOGGER.info(MARKER, "Looping render thread");

        while (!window.shouldClose()) {
            int width = window.getWidth(), height = window.getHeight();
            glBindFramebuffer(GL_FRAMEBUFFER, DEFAULT_FRAMEBUFFER);
            glDisable(GL_CULL_FACE);
            resetFrame(window);
            if (mRoot != null) {
                canvas.setProjection(projection.setOrthographic(width, height, 0, Window.LAST_SYSTEM_WINDOW * 2 + 1,
                        true));
                mRoot.flushDrawCommands(canvas, framebuffer);
            }
            if (framebuffer.getAttachment(GL_COLOR_ATTACHMENT0).getWidth() > 0) {
                glBlitNamedFramebuffer(framebuffer.get(), DEFAULT_FRAMEBUFFER, 0, 0,
                        width, height, 0, 0,
                        width, height, GL_COLOR_BUFFER_BIT, GL_LINEAR);
            }
            if (mRoot != null) {
                mRoot.mChoreographer.scheduleFrameAsync(Core.timeNanos());
            }
            window.swapBuffers();
        }
        LOGGER.info(MARKER, "Quited render thread");
    }

    @UiThread
    private void runUI(@Nonnull Fragment fragment) {
        LOGGER.info(MARKER, "Initializing UI thread");
        mUiLooper = Core.initUiThread();

        ViewConfiguration.get().setViewScale(2);

        mRoot = new ViewRootImpl();

        mDecor = new CoordinatorLayout();
        mDecor.setClickable(true);
        mDecor.setFocusableInTouchMode(true);
        mDecor.setWillNotDraw(true);
        mDecor.setId(R.id.content);

        mFragmentContainerView = new FragmentContainerView();
        mFragmentContainerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mFragmentContainerView.setWillNotDraw(true);
        mFragmentContainerView.setId(fragment_container);
        mDecor.addView(mFragmentContainerView);
        mDecor.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);

        try {
            GLTexture texture = TextureManager.getInstance().create(
                    FileChannel.open(Path.of("F:", "eromanga.png"), StandardOpenOption.READ), true);
            Image image = new Image(texture);
            Drawable drawable = new ImageDrawable(image);
            drawable.setTint(0xFF808080);
            mDecor.setBackground(drawable);
        } catch (IOException ignored) {
        }

        mRoot.setView(mDecor);

        LOGGER.info(MARKER, "Installing view protocol");

        mWindow.install(mRoot);

        mLifecycleRegistry = new LifecycleRegistry(this);
        mOnBackPressedDispatcher = new OnBackPressedDispatcher(() -> mWindow.setShouldClose(true));
        mViewModelStore = new ViewModelStore();
        mFragmentController = FragmentController.createController(new HostCallbacks());

        mFragmentController.attachHost(null);

        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mFragmentController.dispatchCreate();

        mFragmentController.dispatchActivityCreated();
        mFragmentController.execPendingActions();

        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        mFragmentController.dispatchStart();

        LOGGER.info(MARKER, "Starting main fragment");

        mFragmentController.getFragmentManager().beginTransaction()
                .add(fragment_container, fragment, "main")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack("main")
                .commit();

        Core.executeOnMainThread(mWindow::show);

        LOGGER.info(MARKER, "Looping UI thread");

        Looper.loop();

        mFragmentController.dispatchStop();
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);

        mFragmentController.dispatchDestroy();
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);

        LOGGER.info(MARKER, "Quited UI thread");
    }

    private void loadDefaultTypeface() {
        Set<FontFamily> set = new LinkedHashSet<>();

        try (InputStream stream = new FileInputStream("F:/Torus Regular.otf")) {
            Font f = Font.createFont(Font.TRUETYPE_FONT, stream);
            set.add(new FontFamily(f));
        } catch (FontFormatException | IOException ignored) {
        }

        for (FontFamily family : FontFamily.getSystemFontMap().values()) {
            String name = family.getFamilyName();
            if (name.startsWith("Calibri") ||
                    name.startsWith("Microsoft YaHei UI") ||
                    name.startsWith("STHeiti") ||
                    name.startsWith("Segoe UI") ||
                    name.startsWith("SimHei")) {
                set.add(family);
            }
        }

        set.add(Objects.requireNonNull(FontFamily.getSystemFontMap().get(Font.SANS_SERIF)));

        mDefaultTypeface = Typeface.createTypeface(set.toArray(new FontFamily[0]));
    }

    @Nonnull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    /**
     * Get the default or preferred locale set by user.
     *
     * @return the selected locale
     */
    protected Locale onGetSelectedLocale() {
        return Locale.getDefault();
    }

    /**
     * Get the default or preferred locale set by user.
     *
     * @return the selected locale
     */
    @Nonnull
    public static Locale getSelectedLocale() {
        return sInstance == null ? Locale.getDefault() : sInstance.onGetSelectedLocale();
    }

    /**
     * Get the default or preferred typeface set by user.
     *
     * @return the selected typeface
     */
    @Nonnull
    protected Typeface onGetSelectedTypeface() {
        return Objects.requireNonNullElse(mDefaultTypeface, Typeface.SANS_SERIF);
    }

    /**
     * Get the default or preferred typeface set by user.
     *
     * @return the selected typeface
     */
    @Nonnull
    public static Typeface getSelectedTypeface() {
        return sInstance == null ? Typeface.SANS_SERIF : sInstance.onGetSelectedTypeface();
    }

    /**
     * Whether to enable RTL support, it should always be true.
     *
     * @return whether RTL is supported
     */
    @ApiStatus.Experimental
    public boolean hasRtlSupport() {
        return true;
    }

    @ApiStatus.Experimental
    @Nonnull
    public InputStream getResourceStream(@Nonnull String res, @Nonnull String path) throws IOException {
        InputStream stream = ModernUI.class.getResourceAsStream("/assets/" + res + "/" + path);
        if (stream == null) {
            throw new FileNotFoundException();
        }
        return stream;
    }

    @ApiStatus.Experimental
    @Nonnull
    public ReadableByteChannel getResourceChannel(@Nonnull String res, @Nonnull String path) throws IOException {
        return Channels.newChannel(getResourceStream(res, path));
    }

    /**
     * Get the view manager of the application window (i.e. main window).
     *
     * @return window view manager
     */
    @ApiStatus.Internal
    public ViewManager getViewManager() {
        return mDecor;
    }

    @Override
    public void close() {
        try {
            synchronized (Core.class) {
                if (mUiThread != null) {
                    LOGGER.info(MARKER, "Quiting UI thread");
                    try {
                        Core.getUiHandlerAsync().post(() -> mUiLooper.quitSafely());
                        mUiThread.join(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (mRenderThread != null && mRenderThread.isAlive()) {
                    LOGGER.info(MARKER, "Quiting render thread");
                    try {
                        mRenderThread.join(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (mWindow != null) {
                mWindow.close();
                LOGGER.info(MARKER, "Closed main window");
            }
            GLFWMonitorCallback cb = glfwSetMonitorCallback(null);
            if (cb != null) {
                cb.free();
            }
            mVulkanManager.close();
        } finally {
            Core.terminate();
        }
        LOGGER.info(MARKER, "Stopped");
    }

    @UiThread
    class ViewRootImpl extends ViewRoot {

        private final Rect mGlobalRect = new Rect();

        @Nonnull
        @Override
        protected Canvas beginRecording(int width, int height) {
            GLSurfaceCanvas canvas = GLSurfaceCanvas.getInstance();
            canvas.reset(width, height);
            return canvas;
        }

        @Override
        protected boolean dispatchTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View v = mView.findFocus();
                if (v instanceof EditText) {
                    v.getGlobalVisibleRect(mGlobalRect);
                    if (!mGlobalRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                        v.clearFocus();
                    }
                }
            }
            return super.dispatchTouchEvent(event);
        }

        @Override
        protected void onKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getKeyCode() == KeyEvent.KEY_ESCAPE) {
                    View v = mView.findFocus();
                    if (v instanceof EditText) {
                        mView.requestFocus();
                    } else {
                        mOnBackPressedDispatcher.onBackPressed();
                    }
                }
            }
        }

        @RenderThread
        private void flushDrawCommands(GLSurfaceCanvas canvas, GLFramebuffer framebuffer) {
            synchronized (mRenderLock) {
                if (mRedrawn) {
                    mRedrawn = false;
                    canvas.draw(framebuffer);
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

        @Override
        protected void applyPointerIcon(int pointerType) {
            Core.executeOnMainThread(() -> glfwSetCursor(mWindow.getHandle(),
                    PointerIcon.getSystemIcon(pointerType).getHandle()));
        }

        ContextMenuBuilder mContextMenu;
        MenuHelper mContextMenuHelper;

        @Override
        public boolean showContextMenuForChild(View originalView, float x, float y) {
            if (mContextMenuHelper != null) {
                mContextMenuHelper.dismiss();
                mContextMenuHelper = null;
            }

            if (mContextMenu == null) {
                mContextMenu = new ContextMenuBuilder();
                //mContextMenu.setCallback(callback);
            } else {
                mContextMenu.clearAll();
            }

            final MenuHelper helper;
            final boolean isPopup = !Float.isNaN(x) && !Float.isNaN(y);
            if (isPopup) {
                helper = mContextMenu.showPopup(originalView, x, y);
            } else {
                helper = mContextMenu.showPopup(originalView, 0, 0);
            }

            if (helper != null) {
                //helper.setPresenterCallback(callback);
            }

            mContextMenuHelper = helper;
            return helper != null;
        }
    }

    @UiThread
    class HostCallbacks extends FragmentHostCallback<Object> implements
            ViewModelStoreOwner,
            OnBackPressedDispatcherOwner {
        HostCallbacks() {
            super(new Handler(Looper.myLooper()));
            assert Core.isOnUiThread();
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
            return mLifecycleRegistry;
        }
    }
}
