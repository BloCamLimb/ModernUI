/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.lwjgl.system.MemoryUtil;

/**
 * Manages all baked glyphs and their texture atlases for raster text rendering.
 */
public class GlyphAtlasManager extends DrawAtlas.AtlasGenerationCounter
        implements AutoCloseable {

    // font atlas is 4096x4096 at most, governed by plot size and max plots
    private static final int MAX_ATLAS_SIZE = 4096;

    private static final int SMALL_PLOT_SIZE = Glyph.MAX_ATLAS_DIMENSION;
    private static final int LARGE_PLOT_SIZE = SMALL_PLOT_SIZE * 2;

    static {
        int plots = MAX_ATLAS_SIZE / LARGE_PLOT_SIZE;
        //noinspection ConstantValue
        assert plots * plots <= DrawAtlas.Plot.MAX_PLOTS;
    }

    private final RecordingContext mRecordingContext;

    // managed by this
    private final DrawAtlas[] mAtlases = new DrawAtlas[Engine.MASK_FORMAT_COUNT];

    // max texture size for A8 atlas
    private final int mMaxTextureWidth;
    private final int mMaxTextureHeight;

    public GlyphAtlasManager(@NonNull RecordingContext context) {
        mRecordingContext = context;
        int maxSize = Math.min(context.getCaps().maxTextureSize(), MAX_ATLAS_SIZE);
        assert maxSize >= 1024;
        long maxBytes = context.getOptions().mGlyphCacheTextureMaximumBytes;
        int index = maxBytes > 0 ? MathUtil.ceilLog2(maxBytes) : 0;
        // minimum atlas size is 512x512 = 2^18
        index = MathUtil.clamp(index, 18, MathUtil.floorLog2(maxSize) * 2);
        mMaxTextureWidth = 1 << (index / 2);
        mMaxTextureHeight = 1 << (index - index / 2);
    }

    @Override
    public void close() {
        for (int i = 0; i < mAtlases.length; i++) {
            if (mAtlases[i] != null) {
                mAtlases[i].close();
            }
            mAtlases[i] = null;
        }
    }

    /**
     * Initialize atlas if not. This function *must* be called first, before other
     * functions which use the atlas.
     */
    public boolean initAtlas(int maskFormat) {
        if (mAtlases[maskFormat] == null) {
            int ct = Engine.maskFormatToColorType(maskFormat);
            int atlasWidth;
            int atlasHeight;
            int plotWidth;
            int plotHeight;
            if (maskFormat == Engine.MASK_FORMAT_A8) {
                atlasWidth = mMaxTextureWidth;
                atlasHeight = mMaxTextureHeight;
                // plot size is 512x512 for 2048x2048 atlas or above
                plotWidth = atlasWidth >= MAX_ATLAS_SIZE / 2
                        ? LARGE_PLOT_SIZE : SMALL_PLOT_SIZE;
                plotHeight = atlasHeight >= MAX_ATLAS_SIZE / 2
                        ? LARGE_PLOT_SIZE : SMALL_PLOT_SIZE;
            } else {
                // color atlas is 2048x2048 at most
                atlasWidth = mMaxTextureWidth / 2;
                atlasHeight = mMaxTextureHeight / 2;
                // color atlas always use 256x256 plots
                plotWidth = SMALL_PLOT_SIZE;
                plotHeight = SMALL_PLOT_SIZE;
            }
            // no multi pages
            mAtlases[maskFormat] = DrawAtlas.make(
                    ct,
                    atlasWidth, atlasHeight,
                    plotWidth, plotHeight,
                    /*generationCounter*/ this,
                    /*useMultiPages*/ false,
                    /*useStorageTextures*/ false,
                    /*evictor*/ null,
                    "GlyphAtlas"
            );
            return mAtlases[maskFormat] != null;
        }
        return true;
    }

    @RawPtr
    public ImageViewProxy getCurrentTexture(int maskFormat) {
        return getAtlas(maskFormat).getTexture(0);
    }

    public boolean hasGlyph(int maskFormat, @NonNull BakedGlyph glyph) {
        return getAtlas(maskFormat).contains(glyph);
    }

    private static void get_packed_glyph_image(
            Glyph src, int maskFormat, long dst, int dstRB
    ) {
        int width = src.getWidth();
        int height = src.getHeight();
        Object srcBase = src.getImageBase();
        long srcAddr = src.getImageAddress();

        int srcRB = src.getRowBytes();
        // Notice this comparison is with the glyphs raw mask format, and not its GPU MaskFormat.
        if (src.getMaskFormat() != Mask.kBW_Format) {
            assert srcRB == Engine.maskFormatBytesPerPixel(maskFormat) * width;
            PixelUtils.copyImage(
                    srcBase, srcAddr, srcRB,
                    null, dst, dstRB,
                    srcRB, height
            );
        } else {
            PixelUtils.unpackBWToA8(
                    srcBase, srcAddr, srcRB,
                    null, dst, dstRB,
                    width, height
            );
        }
    }

    public int addGlyphToAtlas(@NonNull Glyph glyph,
                               @NonNull BakedGlyph bakedGlyph) {
        if (glyph.getImageBase() == null) {
            return DrawAtlas.RESULT_FAILURE;
        }

        int maskFormat = BakedGlyph.chooseMaskFormat(glyph);
        int bytesPerPixel = Engine.maskFormatBytesPerPixel(maskFormat);

        // always add 1px padding
        int width = glyph.getWidth() + 2 * Glyph.BILERP_GLYPH_BORDER;
        int height = glyph.getHeight() + 2 * Glyph.BILERP_GLYPH_BORDER;
        long srcRB = (long) bytesPerPixel * width;

        DrawAtlas atlas = getAtlas(maskFormat);
        var res = atlas.addRect(
                mRecordingContext,
                width, height,
                bakedGlyph
        );
        if (res == DrawAtlas.RESULT_SUCCESS) {
            long dst = atlas.getDataAt(bakedGlyph);
            int dstRB = bytesPerPixel * atlas.getPlotWidth();
            // since there's padding, first zero out the dst
            for (int y = 0; y < height; y++) {
                MemoryUtil.memSet(dst + ((long) y * dstRB), 0, srcRB);
            }
            // Advance in one row and one column.
            dst = dst + (dstRB + bytesPerPixel) * Glyph.BILERP_GLYPH_BORDER;
            get_packed_glyph_image(glyph, maskFormat, dst, dstRB);
            bakedGlyph.insetRect(Glyph.BILERP_GLYPH_BORDER);
        }

        return res;
    }

    public void addGlyphAndSetLastUseToken(DrawAtlas.@NonNull PlotBulkUseUpdater updater,
                                           @NonNull BakedGlyph glyph,
                                           int maskFormat,
                                           long token) {
        if (updater.add(glyph)) {
            getAtlas(maskFormat).setLastUseToken(glyph, token);
        }
    }

    public long getAtlasGeneration(int maskFormat) {
        return getAtlas(maskFormat).getAtlasGeneration();
    }

    public void setLastUseTokenBulk(int maskFormat,
                                    DrawAtlas.PlotBulkUseUpdater updater,
                                    long token) {
        getAtlas(maskFormat).setLastUseTokenBulk(updater, token);
    }

    public boolean recordUploads(SurfaceDrawContext sdc) {
        for (var atlas : mAtlases) {
            if (atlas != null && !atlas.recordUploads(mRecordingContext, sdc)) {
                return false;
            }
        }
        return true;
    }

    public void evictAtlases() {
        for (var atlas : mAtlases) {
            if (atlas != null) {
                atlas.evictAllPlots();
            }
        }
    }

    public void compact() {
        var tokenTracker = mRecordingContext.getAtlasTokenTracker();
        for (var atlas : mAtlases) {
            if (atlas != null) {
                atlas.compact(tokenTracker.nextFlushToken());
            }
        }
    }

    public void purge() {
        var tokenTracker = mRecordingContext.getAtlasTokenTracker();
        for (var atlas : mAtlases) {
            if (atlas != null) {
                atlas.purge(tokenTracker.nextFlushToken());
            }
        }
    }

    private DrawAtlas getAtlas(int maskFormat) {
        assert maskFormat < Engine.MASK_FORMAT_COUNT;
        return mAtlases[maskFormat];
    }
}
