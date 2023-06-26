/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.engine;

import icyllis.arc3d.opengl.GLEngine;
import icyllis.arc3d.vulkan.VkBackendContext;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

/**
 * Represents a backend context of 3D graphics API (OpenGL or Vulkan) on the render thread.
 */
public final class DirectContext extends RecordingContext {

    private Engine mEngine;
    private ResourceCache mResourceCache;
    private ResourceProvider mResourceProvider;

    private DirectContext(int backend, ContextOptions options) {
        super(new ContextThreadSafeProxy(backend, options));
    }

    /**
     * Creates a DirectContext for a backend context, using default context options.
     *
     * @return context or null if failed to create
     * @see #makeOpenGL(ContextOptions)
     */
    @Nullable
    public static DirectContext makeOpenGL() {
        return makeOpenGL(new ContextOptions());
    }

    /**
     * Creates a DirectContext for a backend context, using specified context options.
     * <p>
     * Example with GLFW:
     * <pre>{@code
     *  public static void main(String[] args) {
     *      System.setProperty("java.awt.headless", Boolean.TRUE.toString());
     *      if (!glfwInit()) {
     *          throw new IllegalStateException();
     *      }
     *      // default hints use highest OpenGL version and native API
     *      glfwDefaultWindowHints();
     *      long window = glfwCreateWindow(1280, 720, "Example Window", NULL, NULL);
     *      if (window == NULL) {
     *          throw new IllegalStateException();
     *      }
     *      // you can make a thread a render thread
     *      glfwMakeContextCurrent(window);
     *      DirectContext direct = DirectContext.makeOpenGL();
     *      if (direct == null) {
     *          throw new IllegalStateException();
     *      }
     *      ...
     *      // destroy and close
     *      direct.discard();
     *      direct.unref();
     *      glfwDestroyWindow(window);
     *      glfwTerminate();
     *  }
     * }</pre>
     *
     * @return context or null if failed to create
     */
    @Nullable
    public static DirectContext makeOpenGL(ContextOptions options) {
        DirectContext context = new DirectContext(Engine.BackendApi.kOpenGL, options);
        context.mEngine = GLEngine.make(context, options);
        if (context.init()) {
            return context;
        }
        context.unref();
        return null;
    }

    /**
     * Creates a DirectContext for a backend context, using default context options.
     *
     * @return context or null if failed to create
     * @see #makeVulkan(VkBackendContext, ContextOptions)
     */
    @Nullable
    public static DirectContext makeVulkan(VkBackendContext backendContext) {
        return makeVulkan(backendContext, new ContextOptions());
    }

    /**
     * Creates a DirectContext for a backend context, using specified context options.
     * <p>
     * The Vulkan context (VkQueue, VkDevice, VkInstance) must be kept alive until the returned
     * DirectContext is destroyed. This also means that any objects created with this
     * DirectContext (e.g. Surfaces, Images, etc.) must also be released as they may hold
     * refs on the DirectContext. Once all these objects and the DirectContext are released,
     * then it is safe to delete the Vulkan objects.
     *
     * @return context or null if failed to create
     */
    @Nullable
    public static DirectContext makeVulkan(VkBackendContext backendContext, ContextOptions options) {
        return null;
    }

    /**
     * The context normally assumes that no outsider is setting state
     * within the underlying 3D API's context/device. This call informs
     * the context that the state was modified and it should resend.
     * <p>
     * The flag bits, state, is dependent on which backend is used by the
     * context, only GL.
     *
     * @see Engine.GLBackendState
     */
    public void resetContext(int state) {
        checkOwnerThread();
        mEngine.markContextDirty(state);
    }

    @ApiStatus.Internal
    public Engine getEngine() {
        return mEngine;
    }

    @ApiStatus.Internal
    public ResourceCache getResourceCache() {
        return mResourceCache;
    }

    @ApiStatus.Internal
    public ResourceProvider getResourceProvider() {
        return mResourceProvider;
    }

    @Override
    protected boolean init() {
        assert isOwnerThread();
        if (mEngine == null) {
            return false;
        }

        mThreadSafeProxy.init(mEngine.getCaps());
        if (!super.init()) {
            return false;
        }

        assert getThreadSafeCache() != null;

        mResourceCache = new ResourceCache(getContextID());
        mResourceProvider = new ResourceProvider(mEngine, mResourceCache);
        return true;
    }

    @Override
    protected void deallocate() {
        super.deallocate();
        if (mResourceCache != null) {
            mResourceCache.releaseAll();
        }
        mEngine.disconnect(true);
    }
}
