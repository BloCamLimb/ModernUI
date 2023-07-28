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

import icyllis.arc3d.core.Matrix4;
import icyllis.arc3d.opengl.GLCore;
import icyllis.arc3d.opengl.GLFramebufferCompat;
import icyllis.modernui.annotation.*;
import icyllis.modernui.app.Activity;
import icyllis.modernui.core.*;
import icyllis.modernui.fragment.*;
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
import icyllis.modernui.widget.EditText;
import org.apache.logging.log4j.*;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.glfw.GLFWMonitorCallback;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Configuration;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.LockSupport;

import static icyllis.arc3d.opengl.GLCore.*;
import static org.lwjgl.glfw.GLFW.*;

/**
 * The core class of Modern UI.
 */
public class ModernUI extends Activity implements AutoCloseable, LifecycleOwner {

    public static final String ID = "modernui"; // as well as the namespace
    public static final String NAME_CPT = "ModernUI";

    public static final Logger LOGGER = LogManager.getLogger(NAME_CPT);
    public static final Marker MARKER = MarkerManager.getMarker("Core");

    private static volatile ModernUI sInstance;

    private static final int fragment_container = 0x01020007;

    static {
        if (Runtime.version().feature() < 17) {
            throw new RuntimeException("JRE 17 or above is required");
        }
    }

    //private final VulkanManager mVulkanManager = VulkanManager.getInstance();

    private volatile MainWindow mWindow;

    private ViewRootImpl mRoot;
    private WindowGroup mDecor;
    private FragmentContainerView mFragmentContainerView;

    private LifecycleRegistry mLifecycleRegistry;
    private OnBackPressedDispatcher mOnBackPressedDispatcher;

    private ViewModelStore mViewModelStore;
    private FragmentController mFragmentController;

    private Typeface mDefaultTypeface;

    private volatile Looper mUiLooper;
    private volatile Thread mRenderThread;

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

    /**
     * Runs the Modern UI with the default application setups.
     * This method is only called by the <code>main()</code> on the main thread.
     */
    @MainThread
    public void run(@NonNull Fragment fragment) {
        Thread.currentThread().setName("Main-Thread");
        // should be true
        LOGGER.info(MARKER, "AWT headless: {}", java.awt.GraphicsEnvironment.isHeadless());

        Core.initialize();

        //mVulkanManager.initialize();

        LOGGER.info(MARKER, "Initializing window system");
        Monitor monitor = Monitor.getPrimary();

        String name = Configuration.OPENGL_LIBRARY_NAME.get();
        if (name != null) {
            // non-system library should load before window creation
            LOGGER.info(ModernUI.MARKER, "OpenGL library: {}", name);
            Objects.requireNonNull(GL.getFunctionProvider(), "Implicit OpenGL loading is required");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
        glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_NATIVE_CONTEXT_API);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHintString(GLFW_X11_CLASS_NAME, NAME_CPT);
        glfwWindowHintString(GLFW_X11_INSTANCE_NAME, NAME_CPT);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        //glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);
        if (monitor == null) {
            LOGGER.info(MARKER, "No monitor connected");
            mWindow = MainWindow.initialize("Modern UI", 1280, 720);
        } else {
            VideoMode mode = monitor.getCurrentMode();
            mWindow = MainWindow.initialize("Modern UI",
                    (int) (mode.getWidth() * 0.75), (int) (mode.getHeight() * 0.75));
            mWindow.center(monitor);
            int[] physw = {0}, physh = {0};
            glfwGetMonitorPhysicalSize(monitor.getHandle(), physw, physh);
            float[] xscale = {0}, yscale = {0};
            glfwGetMonitorContentScale(monitor.getHandle(), xscale, yscale);
            DisplayMetrics metrics = new DisplayMetrics();
            metrics.setToDefaults();
            metrics.widthPixels = mWindow.getWidth();
            metrics.heightPixels = mWindow.getHeight();
            metrics.xdpi = 25.4f * mode.getWidth() / physw[0];
            metrics.ydpi = 25.4f * mode.getHeight() / physh[0];
            LOGGER.info(MARKER, "Primary monitor physical size: {}x{} mm, xScale: {}, yScale: {}",
                    physw[0], physh[0], xscale[0], yscale[0]);
            int density = Math.round(metrics.xdpi * xscale[0] / 12) * 12;
            metrics.density = density * DisplayMetrics.DENSITY_DEFAULT_SCALE;
            metrics.densityDpi = density;
            metrics.scaledDensity = metrics.density;
            LOGGER.info(MARKER, "Display metrics: {}", metrics);
            mResources.updateMetrics(metrics);
        }

        loadDefaultTypeface();

        LOGGER.info(MARKER, "Preparing threads");
        Looper.prepareMainLooper();

        glfwSetWindowCloseCallback(mWindow.getHandle(), new GLFWWindowCloseCallback() {
            @Override
            public void invoke(long window) {
                LOGGER.info(MARKER, "Window closed from callback");
                Looper.getMainLooper().quitSafely();
            }
        });

        CountDownLatch latch = new CountDownLatch(1);

        mRenderThread = new Thread(() -> runRender(latch), "Render-Thread");
        mRenderThread.start();

        try (Bitmap i16 = BitmapFactory.decodeStream(getResourceStream(ID, "AppLogo16x.png"));
             Bitmap i32 = BitmapFactory.decodeStream(getResourceStream(ID, "AppLogo32x.png"));
             Bitmap i48 = BitmapFactory.decodeStream(getResourceStream(ID, "AppLogo48x.png"))) {
            mWindow.setIcon(i16, i32, i48);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // wait render thread init
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        LOGGER.info(MARKER, "Initializing UI system");

        mUiLooper = Core.initUiThread();

        mRoot = new ViewRootImpl();

        mDecor = new WindowGroup(this);
        mDecor.setWillNotDraw(true);
        mDecor.setId(R.id.content);

        mFragmentContainerView = new FragmentContainerView(this);
        mFragmentContainerView.setLayoutParams(new WindowManager.LayoutParams());
        mFragmentContainerView.setWillNotDraw(true);
        mFragmentContainerView.setId(fragment_container);
        mDecor.addView(mFragmentContainerView);
        mDecor.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        mDecor.setIsRootNamespace(true);

        try {
            Path p = Path.of("assets/modernui/raw/eromanga.png").toAbsolutePath();
            FileChannel channel = FileChannel.open(p, StandardOpenOption.READ);
            Image image = ImageStore.getInstance().create(channel);
            if (image != null) {
                Drawable drawable = new ImageDrawable(image);
                drawable.setTint(0xFF808080);
                mDecor.setBackground(drawable);
                mBackgroundImage = image;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        mRoot.setView(mDecor);

        LOGGER.info(MARKER, "Installing view protocol");

        mWindow.install(mRoot);

        mLifecycleRegistry = new LifecycleRegistry(this);
        mOnBackPressedDispatcher = new OnBackPressedDispatcher(() -> {
            mWindow.setShouldClose(true);
            Looper.getMainLooper().quitSafely();
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

        LOGGER.info(MARKER, "Starting main fragment");

        mFragmentController.getFragmentManager().beginTransaction()
                .add(fragment_container, fragment, "main")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack("main")
                .commit();

        mWindow.show();

        LOGGER.info(MARKER, "Looping main thread");
        Looper.loop();

        mFragmentController.dispatchStop();
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);

        mFragmentController.dispatchDestroy();
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);

        Core.requireUiRecordingContext().unref();
        LOGGER.info(MARKER, "Quited main thread");
    }

    @RenderThread
    private void runRender(CountDownLatch latch) {
        LOGGER.info(MARKER, "Initializing render thread");
        final Window window = mWindow;
        window.makeCurrent();
        try {
            if (!Core.initOpenGL()) {
                GLCore.showCapsErrorDialog();
                throw new IllegalStateException("Failed to initialize OpenGL");
            }
        } finally {
            latch.countDown();
        }

        GLCore.setupDebugCallback();

        final GLSurfaceCanvas canvas = GLSurfaceCanvas.initialize();

        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_STENCIL_TEST);
        glEnable(GL_MULTISAMPLE);

        final GLFramebufferCompat framebuffer = new GLFramebufferCompat(4);
        framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT0, GL_RGBA8);
        framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT1, GL_RGBA8);
        framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT2, GL_RGBA8);
        framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT3, GL_RGBA8);
        framebuffer.addRenderbufferAttachment(GL_STENCIL_ATTACHMENT, GL_STENCIL_INDEX8);

        window.swapInterval(1);
        LOGGER.info(MARKER, "Looping render thread");

        while (!window.shouldClose()) {
            boolean flushSurface = false;
            if (mRoot != null) {
                flushSurface = mRoot.flushDrawCommands(canvas, window, framebuffer);
            }
            if (mRoot != null) {
                mRoot.mChoreographer.scheduleFrameAsync(Core.timeNanos());
            }
            if (flushSurface) {
                int width = window.getWidth(), height = window.getHeight();
                if (framebuffer.getAttachment(GL_COLOR_ATTACHMENT0).getWidth() > 0) {
                    glBlitNamedFramebuffer(framebuffer.get(), DEFAULT_FRAMEBUFFER, 0, 0,
                            width, height, 0, 0,
                            width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
                }
                window.swapBuffers();
            } else {
                LockSupport.parkNanos((long) (1.0 / 576 * 1e9));
            }
        }
        GLSurfaceCanvas.getInstance().destroy();
        Core.requireDirectContext().unref();
        LOGGER.info(MARKER, "Quited render thread");
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
    public InputStream getResourceStream(@NonNull String res, @NonNull String path) throws IOException {
        InputStream stream = ModernUI.class.getResourceAsStream("/assets/" + res + "/" + path);
        if (stream == null) {
            throw new FileNotFoundException();
        }
        return stream;
    }

    @ApiStatus.Experimental
    @NonNull
    public ReadableByteChannel getResourceChannel(@NonNull String res, @NonNull String path) throws IOException {
        return Channels.newChannel(getResourceStream(res, path));
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
            synchronized (Core.class) {
                if (mBackgroundImage != null) {
                    mBackgroundImage.close();
                    mBackgroundImage = null;
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
            //mVulkanManager.close();
        } finally {
            Core.terminate();
        }
        LOGGER.info(MARKER, "Stopped");
    }

    @UiThread
    class ViewRootImpl extends ViewRoot {

        private final Rect mGlobalRect = new Rect();

        @NonNull
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
        private boolean flushDrawCommands(GLSurfaceCanvas canvas, Window window, GLFramebufferCompat framebuffer) {
            synchronized (mRenderLock) {
                int width = window.getWidth(), height = window.getHeight();
                if (mRedrawn) {
                    final Matrix4 projection = new Matrix4();
                    canvas.setProjection(projection.setOrthographic(width, height, 0, Window.LAST_SYSTEM_WINDOW * 2 + 1,
                            true));
                    mRedrawn = false;
                    canvas.executeDrawOps(framebuffer);
                    return true;
                }
                return false;
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
