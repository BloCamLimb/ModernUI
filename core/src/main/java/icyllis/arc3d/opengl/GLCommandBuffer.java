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

package icyllis.arc3d.opengl;

import icyllis.arc3d.engine.*;
import icyllis.modernui.annotation.SharedPtr;
import icyllis.modernui.core.RefCnt;

import javax.annotation.Nullable;
import java.util.Arrays;

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
        if ((states & Engine.GLBackendState.kRenderTarget) != 0) {
            mHWFramebuffer = 0;
            mHWRenderTarget = RefCnt.move(mHWRenderTarget);
        }

        if ((states & Engine.GLBackendState.kPipeline) != 0) {
            mHWPipeline = RefCnt.move(mHWPipeline);
            mHWProgram = 0;
            mHWVertexArray = 0;
        }

        if ((states & Engine.GLBackendState.kTexture) != 0) {
            Arrays.fill(mHWTextureBindings, null);
            for (int i = 0, e = mHWTextureBindings.length; i < e; i++) {
                mHWTextureSamplers[i] = RefCnt.move(mHWTextureSamplers[i]);
            }
        }

        if ((states & Engine.GLBackendState.kView) != 0) {
            mHWScissorTest = TriState_Unknown;
            mHWScissorX = -1;
            mHWScissorY = -1;
            mHWScissorWidth = -1;
            mHWScissorHeight = -1;
            mHWViewportWidth = -1;
            mHWViewportHeight = -1;
        }

        if ((states & Engine.GLBackendState.kMisc) != 0) {
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
     * @see Engine.SurfaceOrigin
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
        if (origin == Engine.SurfaceOrigin.kUpperLeft) {
            scissorY = scissorTop;
        } else {
            assert (origin == Engine.SurfaceOrigin.kLowerLeft);
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

    public void bindPipeline(@Nullable GLPipeline pipeline) {
        if (mHWPipeline != pipeline) {
            if (pipeline != null) {
                // active program will not be deleted, so no collision
                assert (pipeline.getProgram() != mHWProgram);
                assert (pipeline.getVertexArray() != mHWVertexArray);
                glUseProgram(pipeline.getProgram());
                glBindVertexArray(pipeline.getVertexArray());
                mHWPipeline = RefCnt.create(mHWPipeline, pipeline);
                mHWProgram = pipeline.getProgram();
                mHWVertexArray = pipeline.getVertexArray();
                assert (mHWProgram != 0 && mHWVertexArray != 0);
            } else {
                assert (mHWProgram != 0 && mHWVertexArray != 0);
                glUseProgram(0);
                glBindVertexArray(0);
                mHWPipeline = RefCnt.move(mHWPipeline);
                mHWProgram = 0;
                mHWVertexArray = 0;
            }
        }
    }

    /**
     * Bind texture for rendering.
     *
     * @param binding the binding index (texture unit)
     * @param texture the texture object
     * @param state   the state of texture sampler, see {@link SamplerState}
     * @param swizzle the read swizzle of texture sampler, see {@link Swizzle}
     * @return success or not
     */
    public boolean bindTexture(int binding, GLTexture texture, int state, short swizzle) {
        assert (texture != null);
        if (binding >= mHWTextureBindings.length) {
            return false;
        }
        if (SamplerState.isMipmapped(state)) {
            if (!texture.isMipmapped()) {
                assert (!SamplerState.isAnisotropy(state));
                state = SamplerState.resetMipmapMode(state);
            } else {
                assert (!texture.isMipmapsDirty());
            }
        }
        GLSampler sampler = mServer.getResourceProvider()
                .findOrCreateCompatibleSampler(state);
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
        if (parameters.baseMipmapLevel != 0) {
            glTextureParameteri(texture.getTextureID(), GL_TEXTURE_BASE_LEVEL, 0);
            parameters.baseMipmapLevel = 0;
        }
        int maxLevel = texture.getMaxMipmapLevel();
        if (parameters.maxMipmapLevel != maxLevel) {
            glTextureParameteri(texture.getTextureID(), GL_TEXTURE_MAX_LEVEL, maxLevel);
            parameters.maxMipmapLevel = maxLevel;
        }
        // texture view is available since 4.3, but less used in OpenGL
        boolean swizzleChanged = false;
        for (int i = 0; i < 4; ++i) {
            int swiz = switch (swizzle & 0xF) {
                case 0 -> GL_RED;
                case 1 -> GL_GREEN;
                case 2 -> GL_BLUE;
                case 3 -> GL_ALPHA;
                case 4 -> GL_ZERO;
                case 5 -> GL_ONE;
                default -> throw new AssertionError(swizzle);
            };
            if (parameters.swizzle[i] != swiz) {
                parameters.swizzle[i] = swiz;
                swizzleChanged = true;
            }
            swizzle >>= 4;
        }
        if (swizzleChanged) {
            glTextureParameteriv(texture.getTextureID(), GL_TEXTURE_SWIZZLE_RGBA, parameters.swizzle);
        }
        return true;
    }
}
