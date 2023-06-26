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

import icyllis.modernui.graphics.RefCnt;
import icyllis.modernui.graphics.SharedPtr;
import icyllis.arc3d.engine.*;

import javax.annotation.Nonnull;
import java.util.Arrays;

import static icyllis.arc3d.opengl.GLCore.*;

/**
 * The main command buffer of OpenGL context. The commands executed on {@link GLCommandBuffer} are
 * mostly the same as that on {@link GLEngine}, but {@link GLCommandBuffer} assumes some values
 * and will not handle dirty context.
 *
 * @see GLEngine#beginRenderPass(GLSurfaceManager, int, int, float[])
 */
public final class GLCommandBuffer {

    private static final int
            TriState_Disabled = 0,
            TriState_Enabled = 1,
            TriState_Unknown = 2;

    private final GLEngine mEngine;

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
    private GLSurfaceManager mHWRenderTarget;

    @SharedPtr
    private GLPipeline mHWPipeline;
    private int mHWProgram;
    private int mHWVertexArray;

    // OpenGL 3 only.
    private int mHWActiveTextureUnit;

    /**
     * Represents a non-zero texture ID is bound, but no GLTexture object is created.
     *
     * @see #bindTextureForSetup(int)
     */
    // OpenGL 3 only.
    private static final GLTexture.UniqueID SETUP_TEXTURE_ID = new GLTexture.UniqueID();

    // target is Texture2D
    private final GLTexture.UniqueID[] mHWTextureStates;

    static final class HWSamplerState {
        // default to invalid, we use 0 because it's not a valid sampler state
        int mSamplerState = 0;
        @SharedPtr
        GLSampler mBoundSampler = null;
    }

    private final HWSamplerState[] mHWSamplerStates;

    GLCommandBuffer(GLEngine engine) {
        mEngine = engine;
        mHWTextureStates = new GLTexture.UniqueID[engine.maxTextureUnits()];
        mHWSamplerStates = new HWSamplerState[mHWTextureStates.length];
        for (int i = 0; i < mHWSamplerStates.length; i++) {
            mHWSamplerStates[i] = new HWSamplerState();
        }
    }

    void resetStates(int states) {
        if ((states & Engine.GLBackendState.kRenderTarget) != 0) {
            mHWFramebuffer = INVALID_ID;
            mHWRenderTarget = RefCnt.move(mHWRenderTarget);
        }

        if ((states & Engine.GLBackendState.kPipeline) != 0) {
            mHWPipeline = RefCnt.move(mHWPipeline);
            mHWProgram = 0;
            mHWVertexArray = INVALID_ID;
        }

        if ((states & Engine.GLBackendState.kTexture) != 0) {
            Arrays.fill(mHWTextureStates, null);
            //TODO
            for (var ss : mHWSamplerStates) {
                ss.mSamplerState = 0;
                ss.mBoundSampler = RefCnt.move(ss.mBoundSampler);
            }
        }

        mHWActiveTextureUnit = -1; // invalid

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
    public void flushRenderTarget(GLSurfaceManager target) {
        if (target == null) {
            mHWRenderTarget = RefCnt.move(mHWRenderTarget);
        } else {
            int framebuffer = target.getSampleFramebuffer();
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

    public void bindPipeline(@Nonnull GLPipeline pipeline) {
        if (mHWPipeline != pipeline) {
            // active program will not be deleted, so no collision
            assert (pipeline.getProgram() != mHWProgram);
            glUseProgram(pipeline.getProgram());
            bindVertexArray(pipeline.getVertexArray());
            mHWPipeline = RefCnt.create(mHWPipeline, pipeline);
            mHWProgram = pipeline.getProgram();
            assert (mHWProgram != 0 && mHWVertexArray != 0);
        }
    }

    public void bindVertexArray(int vertexArray) {
        if (mHWVertexArray != vertexArray) {
            glBindVertexArray(vertexArray);
            mHWVertexArray = vertexArray;
        }
    }

    /**
     * Bind texture for rendering.
     *
     * @param binding      the binding index (texture unit)
     * @param texture      the texture object
     * @param samplerState the state of texture sampler or 0, see {@link SamplerState}
     * @param readSwizzle  the read swizzle of texture sampler, see {@link Swizzle}
     */
    public void bindTexture(int binding, GLTexture texture,
                            int samplerState, short readSwizzle) {
        assert (texture != null);
        if (SamplerState.isMipmapped(samplerState)) {
            if (!texture.isMipmapped()) {
                assert (!SamplerState.isAnisotropy(samplerState));
                samplerState = SamplerState.resetMipmapMode(samplerState);
            } else {
                assert (!texture.isMipmapsDirty());
            }
        }
        boolean dsa = mEngine.getCaps().hasDSASupport();
        if (mHWTextureStates[binding] != texture.getUniqueID()) {
            if (dsa) {
                glBindTextureUnit(binding, texture.getHandle());
            } else {
                setTextureUnit(binding);
                glBindTexture(GL_TEXTURE_2D, texture.getHandle());
            }
            mHWTextureStates[binding] = texture.getUniqueID();
        }
        var ss = mHWSamplerStates[binding];
        if (ss.mSamplerState != samplerState) {
            GLSampler sampler = samplerState != 0
                    ? mEngine.getResourceProvider().findOrCreateCompatibleSampler(samplerState)
                    : null;
            glBindSampler(binding, sampler != null
                    ? sampler.getHandle()
                    : 0);
            ss.mBoundSampler = RefCnt.move(ss.mBoundSampler, sampler);
        }
        GLTextureParameters parameters = texture.getParameters();
        if (parameters.baseMipmapLevel != 0) {
            if (dsa) {
                glTextureParameteri(texture.getHandle(), GL_TEXTURE_BASE_LEVEL, 0);
            } else {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            }
            parameters.baseMipmapLevel = 0;
        }
        int maxLevel = texture.getMaxMipmapLevel();
        if (parameters.maxMipmapLevel != maxLevel) {
            if (dsa) {
                glTextureParameteri(texture.getHandle(), GL_TEXTURE_MAX_LEVEL, maxLevel);
            } else {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, maxLevel);
            }
            parameters.maxMipmapLevel = maxLevel;
        }
        // texture view is available since 4.3, but less used in OpenGL
        boolean swizzleChanged = false;
        for (int i = 0; i < 4; ++i) {
            int swiz = switch (readSwizzle & 0xF) {
                case 0 -> GL_RED;
                case 1 -> GL_GREEN;
                case 2 -> GL_BLUE;
                case 3 -> GL_ALPHA;
                case 4 -> GL_ZERO;
                case 5 -> GL_ONE;
                default -> throw new AssertionError(readSwizzle);
            };
            if (parameters.swizzle[i] != swiz) {
                parameters.swizzle[i] = swiz;
                swizzleChanged = true;
            }
            readSwizzle >>= 4;
        }
        if (swizzleChanged) {
            if (dsa) {
                glTextureParameteriv(texture.getHandle(), GL_TEXTURE_SWIZZLE_RGBA, parameters.swizzle);
            } else {
                glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_RGBA, parameters.swizzle);
            }
        }
    }

    /**
     * Binds texture unit in context. OpenGL 3 only.
     *
     * @param unit 0-based texture unit index
     */
    public void setTextureUnit(int unit) {
        assert (unit >= 0 && unit < mHWTextureStates.length);
        if (unit != mHWActiveTextureUnit) {
            glActiveTexture(GL_TEXTURE0 + unit);
            mHWActiveTextureUnit = unit;
        }
    }

    /**
     * Bind raw texture ID to a seldom used texture unit. OpenGL 3 only.
     *
     * @param texture the texture
     */
    public void bindTextureForSetup(int texture) {
        int lastUnit = mHWTextureStates.length - 1;
        setTextureUnit(lastUnit);
        mHWTextureStates[lastUnit] = SETUP_TEXTURE_ID;
        glBindTexture(GL_TEXTURE_2D, texture);
    }
}
