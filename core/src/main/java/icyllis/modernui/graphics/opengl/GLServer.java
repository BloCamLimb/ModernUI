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

package icyllis.modernui.graphics.opengl;

import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.graphics.engine.*;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import java.util.*;

public final class GLServer extends Server {

    private final GLCaps mCaps;

    //private final GLCommandBuffer mMainCmdBuffer;

    //private final GLPipelineStateCache mProgramCache;
    //private final GLResourceProvider mResourceProvider;

    private final CpuBufferCache mCpuBufferCache;

    private final BufferAllocPool mVertexPool;
    private final BufferAllocPool mInstancePool;

    // unique ptr
    //private GLOpsRenderPass mCachedOpsRenderPass;

    private final ArrayDeque<FlushInfo.FinishedCallback> mFinishedCallbacks = new ArrayDeque<>();
    private final LongArrayFIFOQueue mFinishedFences = new LongArrayFIFOQueue();

    private boolean mNeedsFlush;

    private GLServer(DirectContext context, GLCaps caps) {
        super(context, caps);
        mCaps = caps;
        //mMainCmdBuffer = new GLCommandBuffer(this);
        //mProgramCache = new GLPipelineStateCache(this, 256);
        //mResourceProvider = new GLResourceProvider(this);
        mCpuBufferCache = new CpuBufferCache(6);
        mVertexPool = BufferAllocPool.makeVertexPool(this);
        mInstancePool = BufferAllocPool.makeInstancePool(this);
    }

    /**
     * Create a GLServer with OpenGL context current in the current thread.
     *
     * @param context the owner context
     * @param options the context options
     * @return the server or null if failed to create
     */
    @SuppressWarnings("ConstantConditions")
    @Nullable
    public static GLServer make(DirectContext context, ContextOptions options) {
        GLCapabilities capabilities;
        try {
            // checks may be disabled
            capabilities = Objects.requireNonNullElseGet(GL.getCapabilities(), GL::createCapabilities);
        } catch (Exception e) {
            // checks may be enabled
            capabilities = GL.createCapabilities();
        }
        if (capabilities == null) {
            return null;
        }
        try {
            return new GLServer(context, new GLCaps(options, capabilities));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public GLCaps getCaps() {
        return mCaps;
    }

    @Override
    public ThreadSafePipelineBuilder getPipelineBuilder() {
        return null;
    }

    @Override
    public void disconnect(boolean cleanup) {
        super.disconnect(cleanup);
        mVertexPool.reset();
        mInstancePool.reset();
        mCpuBufferCache.releaseAll();

        /*if (cleanup) {
            mProgramCache.destroy();
            mResourceProvider.destroy();
        } else {
            mProgramCache.discard();
            mResourceProvider.discard();
        }*/

        //callAllFinishedCallbacks(cleanup);
    }

    @Override
    public BufferAllocPool getVertexPool() {
        return null;
    }

    @Override
    public BufferAllocPool getInstancePool() {
        return null;
    }

    @icyllis.modernui.annotation.Nullable
    @Override
    protected Texture onCreateTexture(int width, int height, BackendFormat format, int levelCount, int sampleCount,
                                      int surfaceFlags) {
        return null;
    }

    @icyllis.modernui.annotation.Nullable
    @Override
    protected Texture onWrapRenderableBackendTexture(BackendTexture texture, int sampleCount, boolean ownership) {
        return null;
    }

    @Override
    protected boolean onWritePixels(Texture texture, int x, int y, int width, int height, int dstColorType, int srcColorType, int rowBytes, long pixels) {
        return false;
    }

    @Override
    protected OpsRenderPass onGetOpsRenderPass(SurfaceProxyView writeView, Rect contentBounds, byte colorOps,
                                               byte stencilOps, float[] clearColor, Set<TextureProxy> sampledTextures
            , int pipelineFlags) {
        return null;
    }

    @Override
    protected void onResolveRenderTarget(RenderTarget renderTarget, int resolveLeft, int resolveTop, int resolveRight
            , int resolveBottom) {

    }

    @Override
    public long insertFence() {
        return 0;
    }

    @Override
    public boolean checkFence(long fence) {
        return false;
    }

    @Override
    public void deleteFence(long fence) {

    }

    @Override
    public void addFinishedCallback(FlushInfo.FinishedCallback callback) {

    }

    @Override
    public void checkFinishedCallbacks() {

    }

    @Override
    public void waitForQueue() {

    }
}
