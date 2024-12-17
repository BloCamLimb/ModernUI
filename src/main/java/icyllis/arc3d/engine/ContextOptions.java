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

import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

/**
 * Holds the options for creating a {@link ImmediateContext}, all fields should remain unchanged
 * after creating the context.
 * <p>
 * Boolean value represents a tristate:
 * <ul>
 * <li>{@link Boolean#FALSE}: Forces an option to be disabled.</li>
 * <li>{@link Boolean#TRUE}: Forces an option to be enabled.</li>
 * <li>{@code null}: Uses default behavior, which may use runtime properties (e.g. driver version).</li>
 * </ul>
 * <p>
 * This class is part of public API.
 */
public final class ContextOptions extends BaseContextOptions {

    /**
     * Controls whether we check for GL errors after functions that allocate resources (e.g.
     * glTexImage2D), for shader compilation success, and program link success. Ignored on
     * backends other than GL.
     */
    public Boolean mSkipGLErrorChecks = null;

    /**
     * Overrides: These options override feature detection using backend API queries. These
     * overrides can only reduce the feature set or limits, never increase them beyond the
     * detected values.
     */
    public int mMaxTextureSizeOverride = Integer.MAX_VALUE;

    /**
     * If true, texture fetches from mip-mapped textures will be biased to read larger MIP levels.
     * This has the effect of sharpening those textures, at the cost of some aliasing, and possible
     * performance impact.
     */
    public boolean mSharpenMipmappedTextures = false;

    /**
     * If true, then add 1 pixel padding to all glyph masks in the atlas to support bi-lerp
     * rendering of all glyphs. This must be set to true to use Slug.
     */
    public boolean mSupportBilerpFromGlyphAtlas = false;

    /**
     * Uses a reduced variety of shaders. May perform less optimally in steady state but can reduce
     * jank due to shader compilations.
     */
    public boolean mReducedShaderVariations = false;

    /**
     * The maximum size of cache textures used for Glyph cache.
     */
    public long mGlyphCacheTextureMaximumBytes = 2048 * 2048;

    /**
     * Can the glyph atlas use multiple textures. If allowed, each texture's size is bound by
     * {@link #mGlyphCacheTextureMaximumBytes}.
     */
    public Boolean mAllowMultipleGlyphCacheTextures = null;

    /**
     * Below this threshold size in device space distance field fonts won't be used. Distance field
     * fonts don't support hinting which is more important at smaller sizes.
     */
    public float mMinDistanceFieldFontSize = 18;

    /**
     * Above this threshold size in device space glyphs are drawn as individual paths.
     */
    public float mGlyphsAsPathsFontSize = 384;

    /**
     * If present, use this logger to send info/warning/error message that generated
     * by Arc3D engine.
     */
    public Logger mLogger = NOPLogger.NOP_LOGGER;

    /**
     * Specifies the number of samples Engine should use when performing internal draws with MSAA
     * (hardware capabilities permitting).
     * <p>
     * If 0, Engine will disable internal code paths that use multisampling.
     */
    public int mInternalMultisampleCount = 4;

    /**
     * OpenGL backend only. Setting to true to use actual staging buffers
     * for pixel upload and buffer upload. Otherwise use CPU staging buffer
     * and pass the client pointer to glTexSubImage* and glBufferSubData.
     * In most cases, this will confuse the driver and make the performance
     * worse than traditional methods, so it is recommended to keep it false.
     */
    public boolean mUseStagingBuffers = false;

    /**
     * Maximum number of GL programs or Vk pipelines to keep active in the runtime cache.
     */
    public int mMaxRuntimeProgramCacheSize = 256;

    /**
     * In vulkan backend a single Context submit equates to the submission of a single
     * primary command buffer to the VkQueue. This value specifies how many vulkan secondary command
     * buffers we will cache for reuse on a given primary command buffer. A single submit may use
     * more than this many secondary command buffers, but after the primary command buffer is
     * finished on the GPU it will only hold on to this many secondary command buffers for reuse.
     * <p>
     * A value of -1 means we will pick a limit value internally.
     */
    public int mMaxVkSecondaryCommandBufferCacheSize = -1;

    /**
     * OpenGL backend only. If context is volatile, then Arc3D is considered embedded in
     * another program and shared with its OpenGL context. When making GL function calls that may
     * alter the context's state (especially binding states) outside the command buffer execution,
     * Arc3D will query the binding state and restore the state of the context after that.
     * This is only used for non-DSA methods and may reduce performance. But this can prevent
     * other programs from out-of-order due to assumptions about context's state, especially
     * for mixed API usage between Arc3D and other programs.
     */
    public boolean mVolatileContext = false;

    public long mVulkanVMALargeHeapBlockSize = 0;

    public DriverBugWorkarounds mDriverBugWorkarounds;

    public ContextOptions() {
    }
}
