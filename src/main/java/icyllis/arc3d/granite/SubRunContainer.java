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
import icyllis.arc3d.engine.RecordingContext;
import icyllis.arc3d.engine.SamplerDesc;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.lwjgl.system.MemoryUtil;

/**
 * A SubRun represents a method to draw a subregion of a GlyphRun, where
 * GlyphRun represents the shaped text (positioned glyphs) and a strike.
 * SubRun is the basic unit that is ready for GPU task generation, except
 * that it does not contain the current transformation matrix.
 */
public class SubRunContainer {

    /**
     * SubRun defines the most basic functionality of a SubRun; the ability to draw, and the
     * ability to be in a list.
     */
    public static abstract class SubRun {
        SubRun mNext;
    }

    public static abstract class AtlasSubRun extends SubRun {

        final GlyphVector mGlyphs;
        final int mMaskFormat;
        final boolean mCanDrawDirect;
        final Matrix mCreationMatrix;
        final Rect2f mCreationBounds;
        final float[] mPositions;
        // bounds and positions are in sub run's space

        AtlasSubRun(StrikeDesc strikeDesc,
                    Matrixc creationMatrix,
                    Rect2fc creationBounds,
                    int maskFormat,
                    IntArrayList acceptedGlyphs,
                    FloatArrayList acceptedPositions,
                    boolean canDrawDirect) {
            assert !creationMatrix.hasPerspective();
            mGlyphs = new GlyphVector(strikeDesc, acceptedGlyphs.toIntArray());
            mMaskFormat = maskFormat;
            mCanDrawDirect = canDrawDirect;
            mCreationMatrix = new Matrix(creationMatrix);
            mCreationBounds = new Rect2f(creationBounds);
            mPositions = acceptedPositions.toFloatArray();
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

        public Rect2fc getBounds() {
            return mCreationBounds;
        }

        /**
         * Compute sub-run-to-local matrix with the given origin and store
         * in <var>outSubRunToLocal</var>. Compute filter based on
         * local-to-device matrix and origin, and return it.
         */
        @SuppressWarnings("AssertWithSideEffects")
        public int getSubRunToLocalAndFilter(Matrix4c localToDevice,
                                             float originX, float originY,
                                             Matrix outSubRunToLocal) {
            // the creation matrix has no perspective
            if (mCreationMatrix.invert(outSubRunToLocal)) {
                outSubRunToLocal.postTranslate(originX, originY);
                assert !outSubRunToLocal.hasPerspective();
            } else {
                outSubRunToLocal.setIdentity();
            }
            if (mCanDrawDirect) {
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
                    float offsetX = mappedOriginX - mCreationMatrix.getTranslateX();
                    float offsetY = mappedOriginY - mCreationMatrix.getTranslateY();
                    if (offsetX == (int) offsetX && offsetY == (int) offsetY) {
                        // integer translate
                        return SamplerDesc.FILTER_NEAREST;
                    }
                }
            }
            return SamplerDesc.FILTER_LINEAR;
        }
    }

    public static final class DirectMaskSubRun extends AtlasSubRun {

        public DirectMaskSubRun(StrikeDesc strikeDesc,
                                Matrixc creationMatrix,
                                Rect2fc creationBounds,
                                int maskFormat,
                                IntArrayList acceptedGlyphs,
                                FloatArrayList acceptedPositions) {
            super(strikeDesc, creationMatrix, creationBounds,
                    maskFormat, acceptedGlyphs, acceptedPositions,
                    true);
        }
    }

    public static final class TransformedMaskSubRun extends AtlasSubRun {

        public TransformedMaskSubRun(StrikeDesc strikeDesc,
                                     Matrixc creationMatrix,
                                     Rect2fc creationBounds,
                                     int maskFormat,
                                     IntArrayList acceptedGlyphs,
                                     FloatArrayList acceptedPositions) {
            super(strikeDesc, creationMatrix, creationBounds,
                    maskFormat, acceptedGlyphs, acceptedPositions,
                    false);
        }
    }
}
