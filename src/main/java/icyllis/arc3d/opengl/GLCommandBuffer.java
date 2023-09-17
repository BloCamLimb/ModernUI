/*
 * Arc 3D.
 * Copyright (C) 2022-2023 BloCamLimb. All rights reserved.
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.opengl;

import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.SamplerState;
import org.lwjgl.system.MemoryUtil;

import java.util.Arrays;

import static icyllis.arc3d.engine.Engine.*;
import static icyllis.arc3d.opengl.GLCore.*;

/**
 * The main command buffer of OpenGL context. The commands executed on {@link GLCommandBuffer} are
 * mostly the same as that on {@link GLServer}, but {@link GLCommandBuffer} assumes some values
 * and will not handle dirty context.
 *
 * @see GLServer#beginRenderPass(GLRenderTarget, int, int, float[])
 */
public final class GLCommandBuffer {

    private static final int
            TriState_Disabled = 0,
            TriState_Enabled = 1,
            TriState_Unknown = 2;

    private static final long RGBA_SWIZZLE_MASK;

    static {
        RGBA_SWIZZLE_MASK = MemoryUtil.nmemAllocChecked(16);
        MemoryUtil.memPutInt(RGBA_SWIZZLE_MASK, GL_RED);
        MemoryUtil.memPutInt(RGBA_SWIZZLE_MASK + 4, GL_GREEN);
        MemoryUtil.memPutInt(RGBA_SWIZZLE_MASK + 8, GL_BLUE);
        MemoryUtil.memPutInt(RGBA_SWIZZLE_MASK + 12, GL_ALPHA);
    }

    private final GLServer mServer;

    private int mHWViewportWidth;
    private int mHWViewportHeight;

    private int mHWScissorTest;
    private int mHWColorWrite;

    private int mHWScissorX;
    private int mHWScissorY;
    private int mHWScissorWidth;
    private int mHWScissorHeight;

    private int mHWFramebuffer;
    @SharedPtr
    private GLRenderTarget mHWRenderTarget;

    @SharedPtr
    private GLPipeline mHWPipeline;
    private int mHWProgram;
    private int mHWVertexArray;

    // raw ptr since managed by ResourceCache
    private final GLTexture[] mHWTextureBindings;
    @SharedPtr
    private final GLSampler[] mHWTextureSamplers;

    GLCommandBuffer(GLServer server) {
        mServer = server;
        mHWTextureBindings = new GLTexture[mServer.getCaps().shaderCaps().mMaxFragmentSamplers];
        mHWTextureSamplers = new GLSampler[mHWTextureBindings.length];
    }

    void resetStates(int states) {
        if ((states & GLBackendState.kRenderTarget) != 0) {
            mHWFramebuffer = 0;
            mHWRenderTarget = RefCnt.move(mHWRenderTarget);
        }

        if ((states & GLBackendState.kPipeline) != 0) {
            mHWPipeline = RefCnt.move(mHWPipeline);
            mHWProgram = 0;
            mHWVertexArray = 0;
        }

        if ((states & GLBackendState.kTexture) != 0) {
            Arrays.fill(mHWTextureBindings, null);
            for (int i = 0, e = mHWTextureBindings.length; i < e; i++) {
                mHWTextureSamplers[i] = RefCnt.move(mHWTextureSamplers[i]);
            }
        }

        if ((states & GLBackendState.kView) != 0) {
            mHWScissorTest = TriState_Unknown;
            mHWScissorX = -1;
            mHWScissorY = -1;
            mHWScissorWidth = -1;
            mHWScissorHeight = -1;
            mHWViewportWidth = -1;
            mHWViewportHeight = -1;
        }

        if ((states & GLBackendState.kMisc) != 0) {
            mHWColorWrite = TriState_Unknown;
        }
    }

    /**
     * Flush viewport.
     *
     * @param width  the effective width of color attachment
     * @param height the effective height of color attachment
     */
    public void flushViewport(int width, int height) {
        assert (width >= 0 && height >= 0);
        if (width != mHWViewportWidth || height != mHWViewportHeight) {
            glViewportIndexedf(0, 0.0f, 0.0f, width, height);
            glDepthRangeIndexed(0, 0.0f, 1.0f);
            mHWViewportWidth = width;
            mHWViewportHeight = height;
        }
    }

    /**
     * Flush scissor.
     *
     * @param width  the effective width of color attachment
     * @param height the effective height of color attachment
     * @param origin the surface origin
     * @see SurfaceOrigin
     */
    public void flushScissorRect(int width, int height, int origin,
                                 int scissorLeft, int scissorTop,
                                 int scissorRight, int scissorBottom) {
        assert (width >= 0 && height >= 0);
        final int scissorWidth = scissorRight - scissorLeft;
        final int scissorHeight = scissorBottom - scissorTop;
        assert (scissorLeft >= 0 && scissorTop >= 0 &&
                scissorWidth >= 0 && scissorWidth <= width &&
                scissorHeight >= 0 && scissorHeight <= height);
        final int scissorY;
        if (origin == SurfaceOrigin.kUpperLeft) {
            scissorY = scissorTop;
        } else {
            assert (origin == SurfaceOrigin.kLowerLeft);
            scissorY = height - scissorBottom;
        }
        assert (scissorY >= 0);
        if (scissorLeft != mHWScissorX ||
                scissorY != mHWScissorY ||
                scissorWidth != mHWScissorWidth ||
                scissorHeight != mHWScissorHeight) {
            glScissorIndexed(0, scissorLeft, scissorY,
                    scissorWidth, scissorHeight);
            mHWScissorX = scissorLeft;
            mHWScissorY = scissorY;
            mHWScissorWidth = scissorWidth;
            mHWScissorHeight = scissorHeight;
        }
    }

    /**
     * Flush scissor test.
     *
     * @param enable whether to enable scissor test
     */
    public void flushScissorTest(boolean enable) {
        if (enable) {
            if (mHWScissorTest != TriState_Enabled) {
                glEnablei(GL_SCISSOR_TEST, 0);
                mHWScissorTest = TriState_Enabled;
            }
        } else {
            if (mHWScissorTest != TriState_Disabled) {
                glDisablei(GL_SCISSOR_TEST, 0);
                mHWScissorTest = TriState_Disabled;
            }
        }
    }

    /**
     * Flush color mask for draw buffer 0.
     *
     * @param enable whether to write color
     */
    public void flushColorWrite(boolean enable) {
        if (enable) {
            if (mHWColorWrite != TriState_Enabled) {
                glColorMaski(0, true, true, true, true);
                mHWColorWrite = TriState_Enabled;
            }
        } else {
            if (mHWColorWrite != TriState_Disabled) {
                glColorMaski(0, false, false, false, false);
                mHWColorWrite = TriState_Disabled;
            }
        }
    }

    /**
     * Bind raw framebuffer and flush render target to be invalid.
     */
    public void bindFramebuffer(int framebuffer) {
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        mHWRenderTarget = null;
    }

    /**
     * Flush framebuffer and viewport at the same time.
     *
     * @param target raw ptr to render target
     */
    public void flushRenderTarget(GLRenderTarget target) {
        if (target == null) {
            mHWRenderTarget = RefCnt.move(mHWRenderTarget);
        } else {
            int framebuffer = target.getRenderFramebuffer();
            if (mHWFramebuffer != framebuffer ||
                    mHWRenderTarget != target) {
                glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
                mHWFramebuffer = framebuffer;
                mHWRenderTarget = RefCnt.create(mHWRenderTarget, target);
                flushViewport(target.getWidth(), target.getHeight());
            }
            target.bindStencil();
        }
    }

    @SuppressWarnings("AssertWithSideEffects")
    public void bindPipeline(GLPipeline pipeline) {
        assert (pipeline != null);
        if (mHWPipeline != pipeline) {
            // active program will not be deleted, so no collision
            assert (pipeline.getProgram() != mHWProgram);
            assert (pipeline.getVertexArray() != mHWVertexArray);
            glUseProgram(pipeline.getProgram());
            glBindVertexArray(pipeline.getVertexArray());
            mHWPipeline = RefCnt.create(mHWPipeline, pipeline);
            assert ((mHWProgram = pipeline.getProgram()) != 0);
            assert ((mHWVertexArray = pipeline.getVertexArray()) != 0);
        }
    }

    public boolean bindTexture(GLTexture texture, int binding, int samplerState) {
        assert (texture != null);
        if (binding >= mHWTextureBindings.length) {
            return false;
        }
        if (SamplerState.isMipmapped(samplerState)) {
            if (!texture.isMipmapped()) {
                assert (!SamplerState.isAnisotropy(samplerState));
                samplerState = SamplerState.resetMipmapMode(samplerState);
            } else {
                assert (!texture.isMipmapsDirty());
            }
        }
        GLSampler sampler = mServer.getResourceProvider().findOrCreateCompatibleSampler(samplerState);
        if (sampler == null) {
            return false;
        }
        if (mHWTextureBindings[binding] != texture) {
            glBindTextureUnit(binding, texture.getTextureID());
            mHWTextureBindings[binding] = texture;
        }
        if (mHWTextureSamplers[binding] != sampler) {
            glBindSampler(binding, sampler.getSamplerID());
            mHWTextureSamplers[binding] = RefCnt.create(mHWTextureSamplers[binding], sampler);
        }
        GLTextureParameters parameters = texture.getParameters();
        if (parameters.mBaseMipMapLevel != 0) {
            glTextureParameteri(texture.getTextureID(), GL_TEXTURE_BASE_LEVEL, 0);
            parameters.mBaseMipMapLevel = 0;
        }
        int maxLevel = texture.getMaxMipmapLevel();
        if (parameters.mMaxMipmapLevel != maxLevel) {
            glTextureParameteri(texture.getTextureID(), GL_TEXTURE_MAX_LEVEL, maxLevel);
            parameters.mMaxMipmapLevel = maxLevel;
        }
        if (!parameters.mSwizzleIsRGBA) {
            nglTextureParameteriv(texture.getTextureID(), GL_TEXTURE_SWIZZLE_RGBA, RGBA_SWIZZLE_MASK);
            parameters.mSwizzleIsRGBA = true;
        }
        return true;
    }
}
