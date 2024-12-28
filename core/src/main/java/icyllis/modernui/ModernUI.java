/*
 * Modern UI.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
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

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.Engine;
import icyllis.arc3d.engine.ImmediateContext;
import icyllis.arc3d.granite.GraniteSurface;
import icyllis.arc3d.granite.RootTask;
import icyllis.modernui.annotation.*;
import icyllis.modernui.app.Activity;
import icyllis.modernui.core.*;
import icyllis.modernui.fragment.*;
import icyllis.modernui.graphics.BlendMode;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.*;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.drawable.ImageDrawable;
import icyllis.modernui.graphics.text.FontFamily;
import icyllis.modernui.lifecycle.*;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.text.Typeface;
import icyllis.modernui.util.DisplayMetrics;
import icyllis.modernui.view.*;
import icyllis.modernui.view.menu.ContextMenuBuilder;
import icyllis.modernui.view.menu.MenuHelper;
import icyllis.modernui.widget.TextView;
import org.apache.logging.log4j.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.TestOnly;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.Platform;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.LongConsumer;

import static org.lwjgl.glfw.GLFW.*;

/**
 * The core class of Modern UI.
 */
public class ModernUI extends Activity implements AutoCloseable, LifecycleOwner {

    public static final String ID = "modernui"; // as well as the namespace
    public static final String NAME_CPT = "ModernUI";

    public static final Logger LOGGER = LogManager.getLogger(NAME_CPT);
    public static final Marker MARKER = MarkerManager.getMarker("Core");

    @SuppressWarnings("unused")
    public static final Properties props = new Properties();

    private static volatile ModernUI sInstance;

    private static final int fragment_container = 0x01020007;

    static {
        if (Runtime.version().feature() < 17) {
            throw new RuntimeException("JRE 17 or above is required");
        }
    }

    //private final VulkanManager mVulkanManager = VulkanManager.getInstance();

    private volatile ActivityWindow mWindow;

    private ViewRootImpl mRoot;
    private WindowGroup mDecor;
    private FragmentContainerView mFragmentContainerView;

    private LifecycleRegistry mLifecycleRegistry;
    private OnBackPressedDispatcher mOnBackPressedDispatcher;

    private ViewModelStore mViewModelStore;
    private FragmentController mFragmentController;

    private volatile Typeface mDefaultTypeface;

    private volatile Thread mRenderThread;

    private volatile Looper mRenderLooper;
    private volatile Handler mRenderHandler;

    private Resources mResources = new Resources();

    private Image mBackgroundImage;

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

    private void findHighestGLVersion() {
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        final int[][] versions = {{4, 6}, {4, 5}, {4, 4}, {4, 3}, {4, 2}, {4, 1}, {4, 0}, {3, 3}};
        long window = 0;
        try {
            for (int[] version : versions) {
                glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, version[0]);
                glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, version[1]);
                LOGGER.debug(MARKER, "Trying OpenGL {}.{}", version[0], version[1]);
                window = glfwCreateWindow(640, 480, "System Testing", 0, 0);
                if (window != 0) {
                    LOGGER.info(MARKER, "Will use OpenGL {}.{} Core Profile",
                            version[0], version[1]);
                    return;
                }
            }
            throw new RuntimeException("OpenGL 3.3 or OpenGL ES 3.0 is required");
        } catch (Exception e) {
            throw new RuntimeException("OpenGL 3.3 or OpenGL ES 3.0 is required");
        } finally {
            if (window != 0) {
                glfwDestroyWindow(window);
            }
            glfwSetErrorCallback(callback);
        }
    }

    /**
     * Runs the Modern UI with the default application setups.
     * This method is only called by the <code>main()</code> on the main thread.
     */
    @MainThread
    public void run(@NonNull Fragment fragment) {
        run(fragment, null);
    }

    /**
     * Runs the Modern UI with the default application setups.
     * This method is only called by the <code>main()</code> on the main thread.
     *
     * @hidden
     */
    @TestOnly
    @MainThread
    public void run(@NonNull Fragment fragment, LongConsumer windowCallback) {
        Thread.currentThread().setName("Main-Thread");

        Core.initialize();

        LOGGER.debug(MARKER, "Preparing main thread");
        Looper.prepareMainLooper();

        var loadTypeface = CompletableFuture.runAsync(this::loadDefaultTypeface);

        //mVulkanManager.initialize();

        LOGGER.debug(MARKER, "Initializing window system");
        Monitor monitor = Monitor.getPrimary();

        /*TinyFileDialogs.tinyfd_messageBox(
                "ModernUI Test",
                "ModernUI starting with pid: " + ProcessHandle.current().pid(),
                "ok",
                "info",
                true
        );*/

        String name = Configuration.OPENGL_LIBRARY_NAME.get();
        if (name != null) {
            // non-system library should load before window creation
            LOGGER.debug(ModernUI.MARKER, "OpenGL library: {}", name);
            Objects.requireNonNull(GL.getFunctionProvider(), "Implicit OpenGL loading is required");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
        glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_NATIVE_CONTEXT_API);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_DEPTH_BITS, 0);
        glfwWindowHint(GLFW_STENCIL_BITS, 0);
        glfwWindowHintString(GLFW_X11_CLASS_NAME, NAME_CPT);
        glfwWindowHintString(GLFW_X11_INSTANCE_NAME, NAME_CPT);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        if (Platform.get() == Platform.MACOSX) {
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        } else {
            findHighestGLVersion();
        }
        //glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);
        if (monitor == null) {
            LOGGER.info(MARKER, "No monitor connected");
            mWindow = ActivityWindow.createMainWindow("Modern UI", 1280, 720);
        } else {
            VideoMode mode = monitor.getCurrentMode();
            mWindow = ActivityWindow.createMainWindow("Modern UI",
                    (int) (mode.getWidth() * 0.75), (int) (mode.getHeight() * 0.75));
        }

        CountDownLatch latch = new CountDownLatch(1);

        LOGGER.debug(MARKER, "Preparing render thread");
        mRenderThread = new Thread(() -> runRender(latch), "Render-Thread");
        mRenderThread.start();

        if (windowCallback != null) {
            windowCallback.accept(mWindow.getHandle());
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                Bitmap i16 = BitmapFactory.decodeStream(getResourceStream(ID, "AppLogo16x.png"));
                Bitmap i32 = BitmapFactory.decodeStream(getResourceStream(ID, "AppLogo32x.png"));
                Bitmap i48 = BitmapFactory.decodeStream(getResourceStream(ID, "AppLogo48x.png"));
                return new Bitmap[]{i16, i32, i48};
            } catch (Throwable e) {
                LOGGER.info(MARKER, "Failed to load window icons", e);
            }
            return null;
        }).thenAcceptAsync(icons -> mWindow.setIcon(icons), Core.getMainThreadExecutor());

        if (monitor != null) {
            mWindow.center(monitor);
            int[] physw = {0}, physh = {0};
            glfwGetMonitorPhysicalSize(monitor.getHandle(), physw, physh);
            float[] xscale = {0}, yscale = {0};
            glfwGetMonitorContentScale(monitor.getHandle(), xscale, yscale);
            DisplayMetrics metrics = new DisplayMetrics();
            metrics.setToDefaults();
            metrics.widthPixels = mWindow.getWidth();
            metrics.heightPixels = mWindow.getHeight();
            VideoMode mode = monitor.getCurrentMode();
            metrics.xdpi = 25.4f * mode.getWidth() / physw[0];
            metrics.ydpi = 25.4f * mode.getHeight() / physh[0];
            LOGGER.info(MARKER, "Primary monitor physical size: {}x{} mm, xScale: {}, yScale: {}",
                    physw[0], physh[0], xscale[0], yscale[0]);
            int density = Math.round(DisplayMetrics.DENSITY_DEFAULT * xscale[0] / 12) * 12;
            metrics.density = density * DisplayMetrics.DENSITY_DEFAULT_SCALE;
            metrics.densityDpi = density;
            metrics.scaledDensity = metrics.density;
            LOGGER.info(MARKER, "Display metrics: {}", metrics);
            mResources.updateMetrics(metrics);
        }

        glfwSetWindowCloseCallback(mWindow.getHandle(), new GLFWWindowCloseCallback() {
            @Override
            public void invoke(long window) {
                LOGGER.debug(MARKER, "Window closed from callback");
                stop();
            }
        });

        // wait render thread init
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        LOGGER.debug(MARKER, "Initializing UI system");

        Core.initUiThread();

        mRoot = new ViewRootImpl();
        mRoot.loadSystemProperties(() -> Boolean.getBoolean("icyllis.modernui.display.debug.layout"));

        mDecor = new WindowGroup(this);
        mDecor.setWillNotDraw(true);
        mDecor.setId(R.id.content);

        CompletableFuture.supplyAsync(() -> {
            Path p = Path.of("assets/modernui/raw/eromanga.png").toAbsolutePath();
            try (FileChannel channel = FileChannel.open(p, StandardOpenOption.READ)) {
                return BitmapFactory.decodeChannel(channel);
            } catch (Throwable e) {
                LOGGER.info(MARKER, "Failed to load background image", e);
            }
            return null;
        }).thenAcceptAsync(bitmap -> {
            try (bitmap) {
                Image image = Image.createTextureFromBitmap(bitmap);
                if (image != null) {
                    Drawable drawable = new ImageDrawable(getResources(), image);
                    drawable.setTintBlendMode(BlendMode.MODULATE);
                    drawable.setTint(0xFF808080);
                    mDecor.setBackground(drawable);
                    synchronized (Core.class) {
                        mBackgroundImage = image;
                    }
                }
            }
        }, Core.getUiThreadExecutor());

        mFragmentContainerView = new FragmentContainerView(this);
        mFragmentContainerView.setLayoutParams(new WindowManager.LayoutParams());
        mFragmentContainerView.setWillNotDraw(true);
        mFragmentContainerView.setId(fragment_container);
        mDecor.addView(mFragmentContainerView);
        mDecor.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        mDecor.setIsRootNamespace(true);

        mRoot.setView(mDecor);

        LOGGER.debug(MARKER, "Installing view protocol");

        mWindow.install(mRoot);

        mLifecycleRegistry = new LifecycleRegistry(this);
        mOnBackPressedDispatcher = new OnBackPressedDispatcher(() -> {
            mWindow.setShouldClose(true);
            stop();
        });
        mViewModelStore = new ViewModelStore();
        mFragmentController = FragmentController.createController(new HostCallbacks());

        mFragmentController.attachHost(null);

        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mFragmentController.dispatchCreate();

        mFragmentController.dispatchActivityCreated();
        mFragmentController.execPendingActions();

        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        mFragmentController.dispatchStart();

        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        mFragmentController.dispatchResume();

        mFragmentController.getFragmentManager().beginTransaction()
                .add(fragment_container, fragment, "main")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack("main")
                .commit();

        /*long hWnd = GLFWNativeWin32.glfwGetWin32Window(mWindow.getHandle());
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer margin = stack.ints(-1, -1, -1, -1);
            int hr = Dwmapi.DwmExtendFrameIntoClientArea(hWnd, MemoryUtil.memAddress(margin));
            LOGGER.info("DwmExtendFrameIntoClientArea {}", hr);
        }*/

        mWindow.show();

        loadTypeface.join();

        LOGGER.info(MARKER, "Looping main thread");
        Looper.loop();

        mRoot.mSurface = RefCnt.move(mRoot.mSurface);

        Core.requireUiRecordingContext().unref();
        LOGGER.info(MARKER, "Quited main thread");
    }

    @RenderThread
    private void runRender(CountDownLatch latch) {
        final Window window = mWindow;
        window.makeCurrent();
        try {
            if (!Core.initOpenGL()) {
                Core.glShowCapsErrorDialog();
                throw new IllegalStateException("Failed to initialize OpenGL");
            }
            mRenderLooper = Looper.prepare();
            mRenderHandler = new Handler(mRenderLooper);

            Core.glSetupDebugCallback();
        } finally {
            latch.countDown();
        }

        window.swapInterval(1);
        LOGGER.info(MARKER, "Looping render thread");

        Looper.loop();

        synchronized (Core.class) {
            if (mBackgroundImage != null) {
                mBackgroundImage.close();
                mBackgroundImage = null;
            }
        }

        Core.requireImmediateContext().unref();
        LOGGER.info(MARKER, "Quited render thread");
    }

    public ActivityWindow getWindow() {
        return mWindow;
    }

    private void loadDefaultTypeface() {
        Set<FontFamily> set = new LinkedHashSet<>();

        try (InputStream stream = new FileInputStream("E:/Free Fonts/biliw.otf")) {
            set.add(FontFamily.createFamily(stream, true));
        } catch (Exception ignored) {
        }

        for (String name : new String[]{"Microsoft YaHei UI", "Calibri", "STHeiti", "Segoe UI", "SimHei"}) {
            FontFamily family = FontFamily.getSystemFontWithAlias(name);
            if (family != null) {
                set.add(family);
            }
        }

        mDefaultTypeface = Typeface.createTypeface(set.toArray(new FontFamily[0]));
    }

    private void stop() {
        mDecor.setBackground(null);

        mFragmentController.dispatchStop();
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);

        mFragmentController.dispatchDestroy();
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);

        Looper.getMainLooper().quitSafely();
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    @Override
    public Resources getResources() {
        return mResources;
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
    @NonNull
    public static Locale getSelectedLocale() {
        return sInstance == null ? Locale.getDefault() : sInstance.onGetSelectedLocale();
    }

    /**
     * Get the default or preferred typeface set by user.
     *
     * @return the selected typeface
     */
    @NonNull
    protected Typeface onGetSelectedTypeface() {
        return Objects.requireNonNullElse(mDefaultTypeface, Typeface.SANS_SERIF);
    }

    /**
     * Get the default or preferred typeface set by user.
     *
     * @return the selected typeface
     */
    @NonNull
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
    @NonNull
    public InputStream getResourceStream(@NonNull String namespace, @NonNull String path) throws IOException {
        InputStream stream = ModernUI.class.getResourceAsStream("/assets/" + namespace + "/" + path);
        if (stream == null) {
            throw new FileNotFoundException();
        }
        return stream;
    }

    @ApiStatus.Experimental
    @NonNull
    public ReadableByteChannel getResourceChannel(@NonNull String namespace, @NonNull String path) throws IOException {
        return Channels.newChannel(getResourceStream(namespace, path));
    }

    /**
     * Get the view manager of the application window (i.e. main window).
     *
     * @return window view manager
     */
    @ApiStatus.Internal
    @Override
    public WindowManager getWindowManager() {
        return mDecor;
    }

    @Override
    public void close() {
        try {
            mRenderLooper.quit();
            if (mRenderThread != null && mRenderThread.isAlive()) {
                try {
                    mRenderThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            if (mWindow != null) {
                mWindow.close();
                LOGGER.debug(MARKER, "Closed main window");
            }
            GLFWMonitorCallback cb = glfwSetMonitorCallback(null);
            if (cb != null) {
                cb.free();
            }
            //mVulkanManager.close();
        } finally {
            Core.terminate();
        }
    }

    @UiThread
    class ViewRootImpl extends ViewRoot {

        private final Rect mGlobalRect = new Rect();

        Surface mSurface;
        RootTask mLastFrameTask;

        @Override
        protected boolean dispatchTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View v = mView.findFocus();
                if (v instanceof TextView tv && tv.getMovementMethod() != null) {
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
                    if (v instanceof TextView tv && tv.getMovementMethod() != null) {
                        mView.requestFocus();
                    } else {
                        mOnBackPressedDispatcher.onBackPressed();
                    }
                }
            }
        }

        @Override
        public void setFrame(int width, int height) {
            super.setFrame(width, height);
            if (mSurface == null ||
                    mSurface.getWidth() != width ||
                    mSurface.getHeight() != height) {
                if (width > 0 && height > 0) {
                    mSurface = RefCnt.move(mSurface, GraniteSurface.makeRenderTarget(
                            Core.requireUiRecordingContext(),
                            ImageInfo.make(width, height,
                                    ColorInfo.CT_RGBA_8888, ColorInfo.AT_PREMUL,
                                    ColorSpace.get(ColorSpace.Named.SRGB)),
                            false,
                            Engine.SurfaceOrigin.kLowerLeft,
                            null
                    ));
                }
            }
        }

        @Override
        protected Canvas beginDrawLocked(int width, int height) {
            if (mSurface != null && width > 0 && height > 0) {
                return new ArcCanvas(mSurface.getCanvas());
            }
            return null;
        }

        @Override
        protected void endDrawLocked(@NonNull Canvas canvas) {
            RootTask task = Core.requireUiRecordingContext().snap();
            synchronized (mRenderLock) {
                mLastFrameTask = RefCnt.move(mLastFrameTask, task);
                mRenderHandler.post(this::render);
                try {
                    mRenderLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                mLastFrameTask = RefCnt.move(mLastFrameTask);
            }
        }

        @RenderThread
        private void render() {
            ImmediateContext context = Core.requireImmediateContext();
            int width, height;
            RootTask task;
            synchronized (mRenderLock) {
                width = mSurface.getWidth();
                height = mSurface.getHeight();
                task = mLastFrameTask;
                mLastFrameTask = null;
                mRenderLock.notifyAll();
            }
            if (task == null) {
                return;
            }
            boolean added = context.addTask(task);
            RefCnt.move(task);
            if (added) {
                GL33C.glBindFramebuffer(GL33C.GL_DRAW_FRAMEBUFFER, 0);
                GL33C.glBlitFramebuffer(0, 0, width, height,
                        0, 0, width, height, GL33C.GL_COLOR_BUFFER_BIT,
                        GL33C.GL_NEAREST);
                context.submit();
                mWindow.swapBuffers();
            } else {
                LOGGER.error("Failed to add draw commands");
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
                mContextMenu = new ContextMenuBuilder(ModernUI.this);
                //mContextMenu.setCallback(callback);
            } else {
                mContextMenu.clearAll();
            }

            final MenuHelper helper;
            final boolean isPopup = !Float.isNaN(x) && !Float.isNaN(y);
            if (isPopup) {
                helper = mContextMenu.showPopup(ModernUI.this, originalView, x, y);
            } else {
                helper = mContextMenu.showPopup(ModernUI.this, originalView, 0, 0);
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
            super(ModernUI.this, new Handler(Looper.myLooper()));
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

        @NonNull
        @Override
        public ViewModelStore getViewModelStore() {
            return mViewModelStore;
        }

        @NonNull
        @Override
        public OnBackPressedDispatcher getOnBackPressedDispatcher() {
            return mOnBackPressedDispatcher;
        }

        @NonNull
        @Override
        public Lifecycle getLifecycle() {
            return mLifecycleRegistry;
        }
    }
}
