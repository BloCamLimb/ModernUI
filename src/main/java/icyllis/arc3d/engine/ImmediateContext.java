/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import icyllis.arc3d.opengl.GLDevice;
import icyllis.arc3d.vulkan.VkBackendContext;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Immediate context is used for command list execution and queue submission.
 * That thread is also known as command execution thread and submission thread.
 */
public final class ImmediateContext extends RecordingContext {

    private Device mDevice;
    private ResourceCache mResourceCache;
    private ResourceProvider mResourceProvider;

    private ImmediateContext(int backend, ContextOptions options) {
        super(new SharedContext(backend, options));
    }

    /**
     * Creates a DirectContext for a backend context, using default context options.
     *
     * @return context or null if failed to create
     * @see #makeOpenGL(ContextOptions)
     */
    @Nullable
    public static ImmediateContext makeOpenGL() {
        return makeOpenGL(new ContextOptions());
    }

    @Nullable
    public static ImmediateContext makeOpenGL(@Nonnull ContextOptions options) {
        ImmediateContext context = new ImmediateContext(Engine.BackendApi.kOpenGL, options);
        GLCapabilities capabilities;
        try {
            capabilities = Objects.requireNonNullElseGet(
                    GL.getCapabilities(),
                    GL::createCapabilities
            );
        } catch (Exception x) {
            try {
                capabilities = GL.createCapabilities();
            } catch (Exception e) {
                return null;
            }
        }
        context.mDevice = GLDevice.make(context, options, capabilities);
        if (context.init()) {
            return context;
        }
        context.unref();
        return null;
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
     *      DirectContext direct = DirectContext.makeOpenGL(
     *          GL.createCapabilities()
     *      );
     *      if (direct == null) {
     *          throw new IllegalStateException();
     *      }
     *      ...
     *      // destroy and close
     *      direct.discard();
     *      direct.unref();
     *      GL.setCapabilities(null);
     *      glfwDestroyWindow(window);
     *      glfwTerminate();
     *  }
     * }</pre>
     *
     * @return context or null if failed to create
     */
    @Nullable
    public static ImmediateContext makeOpenGL(@Nonnull Object capabilities, @Nonnull ContextOptions options) {
        ImmediateContext context = new ImmediateContext(Engine.BackendApi.kOpenGL, options);
        context.mDevice = GLDevice.make(context, options, capabilities);
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
    public static ImmediateContext makeVulkan(VkBackendContext backendContext) {
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
    public static ImmediateContext makeVulkan(VkBackendContext backendContext, ContextOptions options) {
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
        mDevice.markContextDirty(state);
    }

    @ApiStatus.Internal
    public Device getDevice() {
        return mDevice;
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
    public boolean isDiscarded() {
        if (super.isDiscarded()) {
            return true;
        }
        if (mDevice != null && mDevice.isDeviceLost()) {
            discard();
            return true;
        }
        return false;
    }

    public boolean isDeviceLost() {
        if (mDevice != null && mDevice.isDeviceLost()) {
            discard();
            return true;
        }
        return false;
    }

    @Override
    protected boolean init() {
        assert isOwnerThread();
        if (mDevice == null) {
            return false;
        }

        mContextInfo.init(mDevice);
        if (!super.init()) {
            return false;
        }

        assert getThreadSafeCache() != null;

        mResourceCache = new ResourceCache(getContextID());
        mResourceCache.setSurfaceProvider(getSurfaceProvider());
        mResourceCache.setThreadSafeCache(getThreadSafeCache());
        mResourceProvider = mDevice.getResourceProvider();
        return true;
    }

    @Override
    public void discard() {
        if (super.isDiscarded()) {
            return;
        }
        super.discard();
        if (mResourceCache != null) {
            mResourceCache.discardAll();
        }
        if (mDevice != null) {
            mDevice.disconnect(false);
        }
    }

    @Override
    protected void deallocate() {
        super.deallocate();
        if (mResourceCache != null) {
            mResourceCache.releaseAll();
        }
        if (mDevice != null) {
            mDevice.disconnect(true);
        }
    }
}
