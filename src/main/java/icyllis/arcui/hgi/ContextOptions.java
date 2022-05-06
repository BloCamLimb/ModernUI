/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.hgi;

public final class ContextOptions {

    public static final int
            DISABLE = -1, // Forces an option to be disabled
            DEFAULT = 0,  // Uses default behavior, which may use runtime properties (e.g. driver version).
            ENABLE = 1;   // Forces an option to be enabled.

    /**
     * Controls whether we check for GL errors after functions that allocate resources (e.g.
     * glTexImage2D), for shader compilation success, and program link success. Ignored on
     * backends other than GL.
     */
    public int mSkipGLErrorChecks = DEFAULT;

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
    public long mGlyphCacheTextureMaximumBytes = 2048 * 1024 * 4;

    /**
     * Can the glyph atlas use multiple textures. If allowed, each texture's size is bound by
     * {@link #mGlyphCacheTextureMaximumBytes}.
     */
    public int mAllowMultipleGlyphCacheTextures = DEFAULT;

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
     * If present, use this object to report shader compilation failures. If not, report failures
     * via err and assert.
     */
    public ShaderErrorHandler mShaderErrorHandler = null;

    /**
     * Specifies the number of samples Arc UI should use when performing internal draws with MSAA
     * (hardware capabilities permitting).
     * <p>
     * If 0, Arc UI will disable internal code paths that use multisampling.
     */
    public int mInternalMultisampleCount = 4;

    /**
     * Maximum number of GL programs or Vk pipelines to keep active in the runtime cache.
     */
    public int mMaxRuntimeProgramCacheSize = 256;

    /**
     * In Arc UI vulkan backend a single Context submit equates to the submission of a single
     * primary command buffer to the VkQueue. This value specifies how many vulkan secondary command
     * buffers we will cache for reuse on a given primary command buffer. A single submit may use
     * more than this many secondary command buffers, but after the primary command buffer is
     * finished on the GPU it will only hold on to this many secondary command buffers for reuse.
     * <p>
     * A value of -1 means we will pick a limit value internally.
     */
    public int mMaxVkSecondaryCommandBufferCacheSize = -1;

    public ContextOptions() {
    }
}
