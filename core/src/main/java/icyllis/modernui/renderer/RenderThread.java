/*
 * ModernUI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.renderer;

import icyllis.arc3d.core.RawPtr;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.ContextOptions;
import icyllis.arc3d.engine.ImmediateContext;
import icyllis.arc3d.granite.GraniteUtil;
import icyllis.modernui.core.Core;
import icyllis.modernui.core.LooperThread;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

import static icyllis.modernui.core.Core.MARKER;
import static icyllis.modernui.util.Log.LOGGER;

public final class RenderThread extends LooperThread {

    private GLManager mGLManager;
    private VulkanManager mVulkanManager;

    private ImmediateContext mImmediateContext;
    private RenderPipeline mRenderPipeline;
    private CountDownLatch mCountDownLatch;

    public RenderThread(GLManager glManager, VulkanManager vulkanManager,
                        CountDownLatch latch) {
        super("Render-Thread");
        mGLManager = glManager;
        mVulkanManager = vulkanManager;
        mCountDownLatch = latch;
    }

    @Override
    protected void onLooperPrepared() {
        try {
            ContextOptions options = new ContextOptions();
            options.mLogger = LoggerFactory.getLogger("Arc3D");
            @SharedPtr final ImmediateContext dc;
            if (mVulkanManager != null) {
                dc = mVulkanManager.createContext(options);
            } else {
                dc = mGLManager.createContext(options);
            }
            if (dc == null) {
                LOGGER.error(MARKER, "Failed to create ImmediateContext");
                return;
            }
            // create static GPU buffers and so on...
            if (GraniteUtil.init(dc)) {
                mImmediateContext = dc;
            } else {
                dc.unref();
                LOGGER.error(MARKER, "Failed to initialize Granite Renderer, destroying ImmediateContext");
                return;
            }
            Core.setRenderThread(mImmediateContext);
            StringBuilder sb = new StringBuilder("\n");
            mImmediateContext.getCaps().dump(sb, false);
            if (mVulkanManager != null) {
                mRenderPipeline = new VulkanRenderPipeline(getLooper(), mImmediateContext, mVulkanManager);
                LOGGER.debug(MARKER, "Vulkan caps: {}", sb);
            } else {
                mRenderPipeline = new GLRenderPipeline(getLooper(), mImmediateContext, mGLManager);
                LOGGER.debug(MARKER, "GL caps: {}", sb);
            }
        } finally {
            mCountDownLatch.countDown();
            mCountDownLatch = null;
        }
        LOGGER.debug(MARKER, "Looping render thread");
    }

    @RawPtr
    public ImmediateContext getImmediateContext() {
        return mImmediateContext;
    }

    public RenderPipeline getRenderPipeline() {
        return mRenderPipeline;
    }

    @RawPtr
    public GLManager getGLManager() {
        return mGLManager;
    }

    @RawPtr
    public VulkanManager getVulkanManager() {
        return mVulkanManager;
    }
}
