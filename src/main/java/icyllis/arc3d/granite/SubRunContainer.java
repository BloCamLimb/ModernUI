/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2025 BloCamLimb <pocamelards@gmail.com>
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
import icyllis.arc3d.engine.RecordingContext;
import icyllis.arc3d.engine.SamplerDesc;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.lwjgl.system.MemoryUtil;

import java.util.Arrays;

/**
 * A SubRun represents a method to draw a subregion of a GlyphRun, where
 * GlyphRun represents the shaped text (positioned glyphs) and a strike.
 * SubRun is the basic unit that is ready for GPU task generation, except
 * that it does not contain the current transformation matrix.
 */
public class SubRunContainer {

    @Contract(pure = true)
    public static boolean isDirect(float approximateDeviceTextSize,
                                   Paint paint, Matrixc matrix) {
        //TODO try to avoid use direct sub runs for animations
        return 0 < approximateDeviceTextSize &&
                approximateDeviceTextSize <= Glyph.kMaxTextSizeForMask &&
                !matrix.hasPerspective();
    }

    @Contract(pure = true)
    public static boolean isDirect(float approximateDeviceTextSize) {
        return 0 < approximateDeviceTextSize &&
                approximateDeviceTextSize <= Glyph.kMaxTextSizeForMask;
    }

    /**
     * SubRun defines the most basic functionality of a SubRun; the ability to draw, and the
     * ability to be in a list.
     */
    public static abstract class SubRun {
        SubRun mNext;

        public abstract void draw(Canvas canvas, float originX, float originY,
                                  Paint paint, GraniteDevice device);

        @Contract(pure = true)
        public abstract boolean canReuse(@NonNull Paint paint, @NonNull Matrixc positionMatrix,
                                         float glyphRunListX, float glyphRunListY);

        public abstract long getMemorySize();
    }

    public static abstract class AtlasSubRun extends SubRun {

        final GlyphVector mGlyphs;
        final int mMaskFormat;
        final boolean mCanDrawDirect;
        final Matrix mCreationMatrix;
        final Rect2f mCreationBounds;
        final float[] mPositions;
        // bounds and positions are in sub run's space

        /**
         * All params are read-only, copy will be made.
         */
        @Contract(pure = true)
        AtlasSubRun(StrikeDesc strikeDesc,
                    Matrixc creationMatrix,
                    Rect2fc creationBounds,
                    int maskFormat,
                    int[] acceptedGlyphs,
                    int acceptedGlyphOffset,
                    float[] acceptedPositions,
                    int acceptedPositionOffset,
                    int acceptedGlyphCount,
                    boolean canDrawDirect) {
            assert !creationMatrix.hasPerspective();
            mGlyphs = new GlyphVector(strikeDesc, acceptedGlyphs, acceptedGlyphOffset,
                    acceptedGlyphOffset + acceptedGlyphCount);
            mMaskFormat = maskFormat;
            mCanDrawDirect = canDrawDirect;
            mCreationMatrix = new Matrix(creationMatrix);
            mCreationBounds = new Rect2f(creationBounds);
            mPositions = Arrays.copyOfRange(acceptedPositions, acceptedPositionOffset,
                    acceptedPositionOffset + acceptedGlyphCount * 2);
        }

        @Override
        public void draw(Canvas canvas, float originX, float originY,
                         Paint paint, GraniteDevice device) {
            device.drawAtlasSubRun(this, originX, originY, paint);
        }

        /**
         * Returns the number of visible glyphs.
         */
        public int getGlyphCount() {
            return mGlyphs.getGlyphCount();
        }

        /**
         * Returns the GPU mask format.
         */
        public int getMaskFormat() {
            return mMaskFormat;
        }

        /**
         * Update atlas for glyphs in the given range if needed, returns the number
         * of glyphs that are updated (may less than end-start if atlas is full).
         * If an error occurred, returns the bitwise NOT (a negative value).
         */
        // This call is not thread safe. It should only be called from a known single-threaded env.
        public int prepareGlyphs(int start, int end,
                                 RecordingContext context) {
            return mGlyphs.prepareGlyphs(start, end,
                    mMaskFormat, context);
        }

        /**
         * @see icyllis.arc3d.granite.geom.RasterTextStep
         */
        public void fillInstanceData(MeshDrawWriter writer,
                                     int offset, int count,
                                     float depth) {
            writer.beginInstances(null, null, 4);
            long instanceData = writer.append(count);

            var glyphs = mGlyphs.getGlyphs();
            var positions = mPositions;
            for (int i = offset, j = offset << 1, e = offset + count; i < e; i += 1, j += 2) {
                var glyph = glyphs[i];
                // xy pos
                MemoryUtil.memPutFloat(instanceData, positions[j]);
                MemoryUtil.memPutFloat(instanceData + 4, positions[j | 1]);
                // uv pos
                MemoryUtil.memPutShort(instanceData + 8, glyph.u1);
                MemoryUtil.memPutShort(instanceData + 10, glyph.v1);
                // size
                MemoryUtil.memPutShort(instanceData + 12, glyph.width());
                MemoryUtil.memPutShort(instanceData + 14, glyph.height());
                // painter's depth
                MemoryUtil.memPutFloat(instanceData + 16, depth);
                instanceData += 20;
            }

            writer.endAppender();
        }

        /**
         * @see icyllis.arc3d.granite.geom.RasterTextStep
         */
        public void fillInstanceData(MeshDrawWriter writer,
                                     int offset, int count,
                                     float offsetX, float offsetY,
                                     float depth) {
            writer.beginInstances(null, null, 4);
            long instanceData = writer.append(count);

            var glyphs = mGlyphs.getGlyphs();
            var positions = mPositions;
            for (int i = offset, j = offset << 1, e = offset + count; i < e; i += 1, j += 2) {
                var glyph = glyphs[i];
                // xy pos
                MemoryUtil.memPutFloat(instanceData, positions[j] + offsetX);
                MemoryUtil.memPutFloat(instanceData + 4, positions[j | 1] + offsetY);
                // uv pos
                MemoryUtil.memPutShort(instanceData + 8, glyph.u1);
                MemoryUtil.memPutShort(instanceData + 10, glyph.v1);
                // size
                MemoryUtil.memPutShort(instanceData + 12, glyph.width());
                MemoryUtil.memPutShort(instanceData + 14, glyph.height());
                // painter's depth
                MemoryUtil.memPutFloat(instanceData + 16, depth);
                instanceData += 20;
            }

            writer.endAppender();
        }

        public Rect2fc getBounds() {
            return mCreationBounds;
        }

        /**
         * Compute sub-run-to-local matrix and sub-run-to-device with the
         * given origin. Compute filter based on local-to-device matrix and
         * origin, and return it.
         */
        @SuppressWarnings("AssertWithSideEffects")
        @Contract(mutates = "param4,param5")
        public int getMatrixAndFilter(Matrixc localToDevice,
                                      float originX, float originY,
                                      Matrix outSubRunToLocal,
                                      Matrix outSubRunToDevice) {
            // the creation matrix has no perspective
            if (mCreationMatrix.invert(outSubRunToLocal)) {
                outSubRunToLocal.postTranslate(originX, originY);
                assert !outSubRunToLocal.hasPerspective();
            } else {
                outSubRunToLocal.setIdentity();
            }
            boolean compatible = !localToDevice.hasPerspective() &&
                    localToDevice.m11() == mCreationMatrix.m11() &&
                    localToDevice.m12() == mCreationMatrix.m12() &&
                    localToDevice.m21() == mCreationMatrix.m21() &&
                    localToDevice.m22() == mCreationMatrix.m22();
            if (compatible) {
                // compatible means only the difference is translation
                float mappedOriginX = localToDevice.m11() * originX +
                        localToDevice.m21() * originY + localToDevice.m41();
                float mappedOriginY = localToDevice.m12() * originX +
                        localToDevice.m22() * originY + localToDevice.m42();
                // creation matrix has no perspective, so translate vector is its origin
                float offsetX = mappedOriginX - mCreationMatrix.getTranslateX();
                float offsetY = mappedOriginY - mCreationMatrix.getTranslateY();
                outSubRunToDevice.setTranslate(offsetX, offsetY);
                if (mCanDrawDirect &&
                        offsetX == (float) Math.floor(offsetX) &&
                        offsetY == (float) Math.floor(offsetY)) {
                    // integer translate in device space
                    return SamplerDesc.FILTER_NEAREST;
                }
            } else {
                outSubRunToDevice.set(localToDevice);
                outSubRunToDevice.preConcat(outSubRunToLocal);
            }
            return SamplerDesc.FILTER_LINEAR;
        }

        @Override
        public long getMemorySize() {
            long size = 32;
            size += 8 + mGlyphs.getMemorySize();
            size += 8 + 56;
            size += 8 + 32;
            size += 16 + (long) mPositions.length * 4 + 8;
            return size;
        }
    }

    public static final class DirectMaskSubRun extends AtlasSubRun {

        /**
         * All params are read-only, copy will be made.
         */
        @Contract(pure = true)
        public DirectMaskSubRun(StrikeDesc strikeDesc,
                                Matrixc creationMatrix,
                                Rect2fc creationBounds,
                                int maskFormat,
                                int[] acceptedGlyphs,
                                int acceptedGlyphOffset,
                                float[] acceptedPositions,
                                int acceptedPositionOffset,
                                int acceptedGlyphCount,
                                float minScaleFactor,
                                float maxScaleFactor) {
            super(strikeDesc, creationMatrix, creationBounds,
                    maskFormat, acceptedGlyphs, acceptedGlyphOffset,
                    acceptedPositions, acceptedPositionOffset, acceptedGlyphCount,
                    true);
            assert minScaleFactor == 1 && maxScaleFactor == 1;
        }

        @Override
        public boolean canReuse(@NonNull Paint paint, @NonNull Matrixc positionMatrix,
                                float glyphRunListX, float glyphRunListY) {
            // direct mask is always created from/reused by an exact position matrix,
            // no need for a second check
            return true;
        }
    }

    public static final class TransformedMaskSubRun extends AtlasSubRun {

        private final float mMinScaleFactor;
        private final float mMaxScaleFactor;

        /**
         * All params are read-only, copy will be made.
         */
        @Contract(pure = true)
        public TransformedMaskSubRun(StrikeDesc strikeDesc,
                                     Matrixc creationMatrix,
                                     Rect2fc creationBounds,
                                     int maskFormat,
                                     int[] acceptedGlyphs,
                                     int acceptedGlyphOffset,
                                     float[] acceptedPositions,
                                     int acceptedPositionOffset,
                                     int acceptedGlyphCount,
                                     float minScaleFactor,
                                     float maxScaleFactor) {
            super(strikeDesc, creationMatrix, creationBounds,
                    maskFormat, acceptedGlyphs, acceptedGlyphOffset,
                    acceptedPositions, acceptedPositionOffset, acceptedGlyphCount,
                    false);
            mMinScaleFactor = minScaleFactor;
            mMaxScaleFactor = maxScaleFactor;
        }

        @Override
        public boolean canReuse(@NonNull Paint paint, @NonNull Matrixc positionMatrix,
                                float glyphRunListX, float glyphRunListY) {
            // We want to reuse the subrun that scales to the range 0.5 to 1.5 relative to the
            // creation matrix.
            float scaleFactor = 1;
            if (positionMatrix.hasPerspective()) {
                float maxAreaScale = positionMatrix.differentialAreaScale(
                        glyphRunListX, glyphRunListY
                );
                if (Float.isFinite(maxAreaScale) && maxAreaScale > MathUtil.PATH_TOLERANCE) {
                    scaleFactor = (float) Math.sqrt(maxAreaScale);
                }
            } else {
                scaleFactor = positionMatrix.getMaxScale();
            }
            return mMinScaleFactor < scaleFactor && scaleFactor <= mMaxScaleFactor;
        }
    }

    /**
     * A set of buffer views to compute sub runs. Source is read-only,
     * so we don't own its memory for the initial run.
     * <p>
     * Glyphs are typeface-specific glyph IDs; positions are pairs of X/Y positions
     * relative to baseline (origin). Source and rejected positions are in local space,
     * accepted positions are in sub run's space, with bearing applied.
     */
    static class Buffers {

        int[] mSourceGlyphs;
        int mSourceGlyphOffset;

        float[] mSourcePositions;
        int mSourcePositionOffset;

        int mSourceGlyphCount;

        final int[] mAcceptedGlyphs;
        final float[] mAcceptedPositions;
        // Mask::Format
        final byte[] mAcceptedFormats;

        int mAcceptedGlyphCount;

        final int[] mRejectedGlyphs;
        final float[] mRejectedPositions;

        int mRejectedGlyphCount;

        Buffers(int maxGlyphRunSize) {
            mAcceptedGlyphs = new int[maxGlyphRunSize];
            mAcceptedPositions = new float[maxGlyphRunSize * 2];
            mAcceptedFormats = new byte[maxGlyphRunSize];
            mRejectedGlyphs = new int[maxGlyphRunSize];
            mRejectedPositions = new float[maxGlyphRunSize * 2];
        }

        // let source points to glyph run buffer
        void setSource(@NonNull GlyphRun glyphRun) {
            mSourceGlyphs = glyphRun.mGlyphs;
            mSourceGlyphOffset = glyphRun.mGlyphOffset;
            mSourcePositions = glyphRun.mPositions;
            mSourcePositionOffset = glyphRun.mPositionOffset;
            mSourceGlyphCount = glyphRun.mGlyphCount;
        }

        // let source points to rejected buffer
        void setSourceToRejected() {
            mSourceGlyphs = mRejectedGlyphs;
            mSourceGlyphOffset = 0;
            mSourcePositions = mRejectedPositions;
            mSourcePositionOffset = 0;
            mSourceGlyphCount = mRejectedGlyphCount;
        }
    }

    static Rect2f prepare_for_direct_mask_drawing(Strike strike,
                                                  Matrixc creationMatrix,
                                                  Buffers buffers) {
        // Build up the mapping from source space to sub run space. Add the rounding constant,
        // so we just need to floor to get the device result.
        int acceptedSize = 0,
                rejectedSize = 0;
        var runBounds = Rect2f.makeInfiniteInverted();
        float subpixelRounding = strike.getSubpixelRounding();
        int subpixelFieldMask = strike.getSubpixelFieldMask();
        var bounds = new Rect2f();
        var mappedPos = new float[2];
        strike.lock();
        try {
            for (int i = buffers.mSourceGlyphOffset, j = buffers.mSourcePositionOffset,
                 e = buffers.mSourceGlyphOffset + buffers.mSourceGlyphCount; i < e; i += 1, j += 2) {
                float posX = buffers.mSourcePositions[j];
                float posY = buffers.mSourcePositions[j + 1];
                if (!Float.isFinite(posX) || !Float.isFinite(posY)) {
                    continue;
                }
                int glyphID = buffers.mSourceGlyphs[i];
                creationMatrix.mapPoints(
                        buffers.mSourcePositions, j,
                        mappedPos, 0, 1
                );
                mappedPos[0] += subpixelRounding;
                mappedPos[1] += 0.5f;
                int packedGlyphID = Glyph.packGlyphID(glyphID, mappedPos[0], subpixelFieldMask);
                var glyph = strike.digestFor(Glyph.kDirectMask, packedGlyphID);
                switch (glyph.actionFor(Glyph.kDirectMask)) {
                    case Glyph.kAccept_Action -> {
                        float roundedPosX = (float) Math.floor(mappedPos[0]);
                        float roundedPosY = (float) Math.floor(mappedPos[1]);
                        glyph.getBounds(bounds);
                        bounds.offset(roundedPosX, roundedPosY);
                        runBounds.joinNoCheck(bounds);
                        buffers.mAcceptedGlyphs[acceptedSize] = packedGlyphID;
                        // accepted buffer index starts from zero, it's safe to use OR
                        buffers.mAcceptedPositions[acceptedSize << 1] = bounds.x();
                        buffers.mAcceptedPositions[(acceptedSize << 1) | 1] = bounds.y();
                        buffers.mAcceptedFormats[acceptedSize] = glyph.getMaskFormat();
                        acceptedSize++;
                    }
                    case Glyph.kReject_Action -> {
                        buffers.mRejectedGlyphs[rejectedSize] = glyphID;
                        // rejected buffer index starts from zero, it's safe to use OR
                        buffers.mRejectedPositions[rejectedSize << 1] = posX;
                        buffers.mRejectedPositions[(rejectedSize << 1) | 1] = posY;
                        rejectedSize++;
                    }
                    case Glyph.kDrop_Action -> {
                        // empty glyphs are dropped
                    }
                }
            }
        } finally {
            strike.unlock();
        }

        buffers.mAcceptedGlyphCount = acceptedSize;
        buffers.mRejectedGlyphCount = rejectedSize;
        return runBounds;
    }

    static Rect2f prepare_for_transformed_mask_drawing(Strike strike,
                                                       Matrixc creationMatrix,
                                                       Buffers buffers) {
        int acceptedSize = 0,
                rejectedSize = 0;
        var runBounds = Rect2f.makeInfiniteInverted();
        var bounds = new Rect2f();
        var mappedPos = new float[2];
        strike.lock();
        try {
            for (int i = buffers.mSourceGlyphOffset, j = buffers.mSourcePositionOffset,
                 e = buffers.mSourceGlyphOffset + buffers.mSourceGlyphCount; i < e; i += 1, j += 2) {
                float posX = buffers.mSourcePositions[j];
                float posY = buffers.mSourcePositions[j + 1];
                if (!Float.isFinite(posX) || !Float.isFinite(posY)) {
                    continue;
                }
                int glyphID = buffers.mSourceGlyphs[i];
                var glyph = strike.digestFor(Glyph.kTransformedMask, glyphID);
                switch (glyph.actionFor(Glyph.kTransformedMask)) {
                    case Glyph.kAccept_Action -> {
                        creationMatrix.mapPoints(
                                buffers.mSourcePositions, j,
                                mappedPos, 0, 1
                        );
                        glyph.getBounds(bounds);
                        bounds.offset(mappedPos[0], mappedPos[1]);
                        runBounds.joinNoCheck(bounds);
                        buffers.mAcceptedGlyphs[acceptedSize] = glyphID;
                        // accepted buffer index starts from zero, it's safe to use OR
                        buffers.mAcceptedPositions[acceptedSize << 1] = bounds.x();
                        buffers.mAcceptedPositions[(acceptedSize << 1) | 1] = bounds.y();
                        buffers.mAcceptedFormats[acceptedSize] = glyph.getMaskFormat();
                        acceptedSize++;
                    }
                    case Glyph.kReject_Action -> {
                        buffers.mRejectedGlyphs[rejectedSize] = glyphID;
                        // rejected buffer index starts from zero, it's safe to use OR
                        buffers.mRejectedPositions[rejectedSize << 1] = posX;
                        buffers.mRejectedPositions[(rejectedSize << 1) | 1] = posY;
                        rejectedSize++;
                    }
                    case Glyph.kDrop_Action -> {
                        // empty glyphs are dropped
                    }
                }
            }
        } finally {
            strike.unlock();
        }

        buffers.mAcceptedGlyphCount = acceptedSize;
        buffers.mRejectedGlyphCount = rejectedSize;
        return runBounds;
    }

    interface AtlasSubRunFactory {

        AtlasSubRun create(StrikeDesc strikeDesc,
                           Matrixc creationMatrix,
                           Rect2fc creationBounds,
                           int maskFormat,
                           int[] acceptedGlyphs,
                           int acceptedGlyphOffset,
                           float[] acceptedPositions,
                           int acceptedPositionOffset,
                           int acceptedGlyphCount,
                           float minScaleFactor,
                           float maxScaleFactor);
    }

    static void add_multi_mask_format(SubRunContainer container,
                                      StrikeDesc strikeDesc,
                                      Matrixc creationMatrix,
                                      Rect2fc creationBounds,
                                      float minScaleFactor,
                                      float maxScaleFactor,
                                      AtlasSubRunFactory factory,
                                      Buffers buffers) {
        assert buffers.mAcceptedGlyphCount > 0;

        byte[] masks = buffers.mAcceptedFormats;
        int prevFormat = BakedGlyph.chooseMaskFormat(masks[0]);
        int prevIndex = 0;
        for (int index = 1; index < buffers.mAcceptedGlyphCount; index++) {
            int format = BakedGlyph.chooseMaskFormat(masks[index]);
            if (prevFormat != format) {
                container.append(
                        factory.create(
                                strikeDesc,
                                creationMatrix,
                                creationBounds,
                                prevFormat,
                                buffers.mAcceptedGlyphs,
                                prevIndex,
                                buffers.mAcceptedPositions,
                                prevIndex * 2,
                                index - prevIndex,
                                minScaleFactor,
                                maxScaleFactor
                        )
                );
                prevFormat = format;
                prevIndex = index;
            }
        }
        container.append(
                factory.create(
                        strikeDesc,
                        creationMatrix,
                        creationBounds,
                        prevFormat,
                        buffers.mAcceptedGlyphs,
                        prevIndex,
                        buffers.mAcceptedPositions,
                        prevIndex * 2,
                        buffers.mAcceptedGlyphCount - prevIndex,
                        minScaleFactor,
                        maxScaleFactor
                )
        );
    }

    static float find_max_glyph_dimension(Strike strike, int[] glyphs,
                                          int glyphOffset, int glyphCount) {
        strike.lock();
        try {
            float maxDimension = 0;
            for (int i = glyphOffset, e = glyphOffset + glyphCount; i < e; i++) {
                var glyph = strike.digestFor(Glyph.kTransformedMask, glyphs[i]);
                maxDimension = Math.max(maxDimension, glyph.getMaxDimension());
            }
            return maxDimension;
        } finally {
            strike.unlock();
        }
    }

    @NonNull
    public static SubRunContainer make(
            @NonNull GlyphRunList glyphRunList,
            @NonNull Matrixc positionMatrix,
            @NonNull Paint runPaint,
            @NonNull StrikeCache strikeCache
    ) {
        SubRunContainer container = new SubRunContainer(positionMatrix);

        Buffers buffers = new Buffers(
                glyphRunList.maxGlyphRunSize()
        );

        float glyphRunListX = glyphRunList.getSourceBounds().centerX();
        float glyphRunListY = glyphRunList.getSourceBounds().centerY();

        var strikeDesc = new StrikeDesc.Lookup();

        // Handle all the runs in the glyphRunList
        for (int i = 0; i < glyphRunList.mGlyphRunCount; i++) {
            var glyphRun = glyphRunList.mGlyphRuns[i];
            var runFont = glyphRun.font();

            float approximateDeviceFontSize =
                    runFont.approximateTransformedFontSize(positionMatrix,
                            glyphRunListX, glyphRunListY);
            if (approximateDeviceFontSize <= 0 || !Float.isFinite(approximateDeviceFontSize)) {
                // Drop infinity, NaN, negative, zero, singular matrix
                continue;
            }

            buffers.setSource(glyphRun);

            // Atlas mask cases - SDF and direct mask
            // Only consider using direct or SDF drawing if not drawing hairlines and not too big.
            if ((runPaint.getStyle() != Paint.STROKE || runPaint.getStrokeWidth() != 0) &&
                    approximateDeviceFontSize <= Glyph.kMaxTextSizeForMask) {
                //TODO SDF case

                // Direct Mask case
                // Handle all the directly mapped mask subruns.
                if (buffers.mSourceGlyphCount > 0 && !positionMatrix.hasPerspective()) {
                    // No actual subpixel positioning support, you can also think that there is
                    // always subpixel positioning support. As long as the draw origin mapped by
                    // the matrix is not an integer, bilinear sampling will be used. Pixel grid
                    // alignment is always done in the sub run space without fractional offset.
                    // If clients use linear metrics and want grid alignment, they should align
                    // the origin instead of having us align the position of each glyph, which can
                    // greatly improve the cache hit rate.
                    strikeDesc.updateForMask(
                            runFont, runPaint, positionMatrix
                    );

                    Strike strike = strikeDesc.findOrCreateStrike(strikeCache);

                    var creationBounds = prepare_for_direct_mask_drawing(
                            strike, positionMatrix, buffers
                    );
                    buffers.setSourceToRejected();

                    if (buffers.mAcceptedGlyphCount > 0) {
                        add_multi_mask_format(container,
                                strikeDesc,
                                positionMatrix, creationBounds,
                                1, 1,
                                DirectMaskSubRun::new,
                                buffers);
                    }
                }
            }

            //TODO drawable case

            //TODO path case

            // Drawing of last resort case
            // Draw all the rest of the rejected glyphs from above. This scales out of the atlas to
            // the screen, so quality will suffer. This mainly handles large color or perspective
            // color not handled by Drawables.
            if (buffers.mSourceGlyphCount > 0 &&
                    approximateDeviceFontSize > MathUtil.PATH_TOLERANCE) {
                // Creation matrix will be changed below to meet the following criteria:
                // * No perspective - the font scaler and the strikes can't handle perspective masks.
                // * Fits atlas - creationMatrix will be conditioned so that the maximum glyph
                //   dimension for this run will be < kMaxBilerpAtlasDimension.
                Matrix creationMatrix = new Matrix(positionMatrix);
                // We want to reuse the subrun that scales to the range 0.5 to 1.5 relative to the
                // creation matrix.
                float minScaleFactor;
                float maxScaleFactor;

                if (creationMatrix.hasPerspective()) {
                    float maxAreaScale = creationMatrix.differentialAreaScale(
                            glyphRunListX, glyphRunListY
                    );
                    float perspectiveFactor = 1;
                    if (Float.isFinite(maxAreaScale) && maxAreaScale > MathUtil.PATH_TOLERANCE) {
                        perspectiveFactor = (float) Math.sqrt(maxAreaScale);
                    }

                    // Masks can not be created in perspective. Create a non-perspective font with a
                    // scale that will support the perspective keystoning.
                    creationMatrix.setScale(perspectiveFactor, perspectiveFactor);

                    minScaleFactor = 0.5f * perspectiveFactor;
                    maxScaleFactor = 1.5f * perspectiveFactor;
                } else {
                    float scale = creationMatrix.getMaxScale();

                    minScaleFactor = 0.5f * scale;
                    maxScaleFactor = 1.5f * scale;
                }

                // Condition the creationMatrix so that glyphs fit in the atlas.
                // The number of iterations is limited to 2, then the font size is limited to 16,908,804.
                int maxIter = 2;
                while (maxIter-- != 0) {
                    strikeDesc.updateForMask(
                            runFont, runPaint, creationMatrix
                    );
                    Strike gaugingStrike = strikeDesc.findOrCreateStrike(strikeCache);
                    float maxDimension = find_max_glyph_dimension(gaugingStrike,
                            buffers.mSourceGlyphs, buffers.mSourceGlyphOffset, buffers.mSourceGlyphCount);
                    if (maxDimension <= Glyph.kMaxBilerpAtlasDimension) {
                        break;
                    }
                    float reductionFactor = Glyph.kMaxBilerpAtlasDimension / maxDimension;
                    creationMatrix.postScale(reductionFactor, reductionFactor);
                    // If the metric is too large, then we remove the upper limit,
                    // because it will eventually fall back onto the same path
                    maxScaleFactor = Float.POSITIVE_INFINITY;
                }

                // Draw using the creationMatrix.
                strikeDesc.updateForMask(
                        runFont, runPaint, creationMatrix
                );

                Strike strike = strikeDesc.findOrCreateStrike(strikeCache);

                var creationBounds = prepare_for_transformed_mask_drawing(
                        strike, creationMatrix, buffers
                );
                buffers.setSourceToRejected();

                if (buffers.mAcceptedGlyphCount > 0) {
                    add_multi_mask_format(container,
                            strikeDesc,
                            creationMatrix, creationBounds,
                            minScaleFactor, maxScaleFactor,
                            TransformedMaskSubRun::new,
                            buffers);
                }
            }
        }

        return container;
    }

    private final Matrix mInitialPositionMatrix;

    private SubRun mHead;
    private SubRun mTail;

    public SubRunContainer(Matrixc initialPositionMatrix) {
        mInitialPositionMatrix = new Matrix(initialPositionMatrix);
    }

    public void draw(Canvas canvas, float originX, float originY,
                     Paint paint, GraniteDevice device) {
        for (var subRun = mHead; subRun != null; subRun = subRun.mNext) {
            subRun.draw(canvas, originX, originY, paint, device);
        }
    }

    public Matrixc initialPosition() {
        return mInitialPositionMatrix;
    }

    public boolean isEmpty() {
        return mHead == null;
    }

    @Contract(pure = true)
    public boolean canReuse(@NonNull Paint paint, @NonNull Matrixc positionMatrix,
                            float glyphRunListX, float glyphRunListY) {
        for (var subRun = mHead; subRun != null; subRun = subRun.mNext) {
            if (!subRun.canReuse(paint, positionMatrix, glyphRunListX, glyphRunListY)) {
                return false;
            }
        }
        return true;
    }

    public long getMemorySize() {
        long size = 16 + 8 + 8 + 56 + 8;
        for (var subRun = mHead; subRun != null; subRun = subRun.mNext) {
            size += subRun.getMemorySize();
        }
        return size;
    }

    void append(SubRun entry) {
        SubRun tail = mTail;
        mTail = entry;
        if (tail == null) {
            mHead = entry;
        } else {
            tail.mNext = entry;
        }
    }
}
