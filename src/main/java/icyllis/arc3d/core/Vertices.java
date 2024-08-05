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

package icyllis.arc3d.core;

import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.*;
import java.util.Arrays;

/**
 * An immutable set of vertex data that can be used with {@link Canvas#drawVertices}.
 */
public class Vertices {

    public static final int
            kPoints_VertexMode = 0,
            kLines_VertexMode = 1,
            kLineStrip_VertexMode = 2,
            kTriangles_VertexMode = 3,
            kTriangleStrip_VertexMode = 4;
    public static final int
            kVertexModeCount = 5;

    private final int mVertexMode;

    private final float[] mPositions;
    private final float[] mTexCoords;
    // convert to (r,g,b,a) array, so no need to consider endianness
    private final byte[] mColors;
    private final short[] mIndices;

    private final int     mVertexCount;
    private final int     mIndexCount;

    private final Rect2f mBounds;

    Vertices(int vertexMode, float[] positions, float[] texCoords,
             byte[] colors, short[] indices, int vertexCount, int indexCount) {
        mVertexMode = vertexMode;
        mPositions = positions;
        mTexCoords = texCoords;
        mColors = colors;
        mIndices = indices;
        mVertexCount = vertexCount;
        mIndexCount = indexCount;
        Rect2f bounds = new Rect2f();
        bounds.setBounds(positions, 0, vertexCount);
        if (vertexMode == kPoints_VertexMode ||
                vertexMode == kLines_VertexMode ||
                vertexMode == kLineStrip_VertexMode) {
            bounds.outset(1.0f, 1.0f);
        }
        mBounds = bounds;
    }

    @Nonnull
    public static Vertices makeCopy(int vertexMode, int vertexCount,
                                    @Nonnull float[] positions, int positionOffset,
                                    @Nullable float[] texCoords, int texCoordOffset,
                                    @Nullable int[] colors, int colorOffset) {
        return makeCopy(vertexMode, vertexCount,
                positions, positionOffset,
                texCoords, texCoordOffset,
                colors, colorOffset,
                null, 0, 0);
    }

    @Nonnull
    public static Vertices makeCopy(int vertexMode, int vertexCount,
                                    @Nonnull float[] positions, int positionOffset,
                                    @Nullable float[] texCoords, int texCoordOffset,
                                    @Nullable int[] colors, int colorOffset,
                                    @Nullable short[] indices, int indexOffset, int indexCount) {
        float[] newPositions = Arrays.copyOfRange(positions, positionOffset, positionOffset + vertexCount * 2);
        float[] newTexCoords = null;
        if (texCoords != null) {
            newTexCoords = Arrays.copyOfRange(texCoords, texCoordOffset, texCoordOffset + vertexCount * 2);
        }
        byte[] newColors = null;
        if (colors != null) {
            newColors = new byte[vertexCount * 4];
            for (int i = 0, j = colorOffset, k = 0; i < vertexCount; i++, j++, k += 4) {
                int color = colors[j];
                newColors[j] =   (byte) ((color >> 16) & 0xFF);
                newColors[j|1] = (byte) ((color >> 8) & 0xFF);
                newColors[j|2] = (byte) (color & 0xFF);
                newColors[j|3] = (byte) (color >>> 24);
            }
        }
        short[] newIndices = null;
        if (indices != null) {
            newIndices = Arrays.copyOfRange(indices, indexOffset, indexOffset + indexCount);
        }
        return new Vertices(vertexMode, newPositions, newTexCoords, newColors, newIndices,
                vertexCount, indexCount);
    }

    @Nonnull
    public static Vertices makeCopy(int vertexMode,
                                    @Nonnull FloatBuffer positions,
                                    @Nullable FloatBuffer texCoords,
                                    @Nullable IntBuffer colors,
                                    @Nullable ShortBuffer indices) {
        int vertexCount = positions.remaining() / 2;
        float[] newPositions = new float[vertexCount * 2];
        positions.get(positions.position(), newPositions);
        float[] newTexCoords = null;
        if (texCoords != null) {
            newTexCoords = new float[vertexCount * 2];
            texCoords.get(texCoords.position(), newTexCoords);
        }
        byte[] newColors = null;
        if (colors != null) {
            newColors = new byte[vertexCount * 4];
            for (int i = 0, j = colors.position(), k = 0; i < vertexCount; i++, j++, k += 4) {
                int color = colors.get(j);
                newColors[j] =   (byte) ((color >> 16) & 0xFF);
                newColors[j|1] = (byte) ((color >> 8) & 0xFF);
                newColors[j|2] = (byte) (color & 0xFF);
                newColors[j|3] = (byte) (color >>> 24);
            }
        }
        int indexCount = 0;
        short[] newIndices = null;
        if (indices != null) {
            indexCount = indices.remaining();
            newIndices = new short[indexCount];
            indices.get(indices.position(), newIndices);
        }
        return new Vertices(vertexMode, newPositions, newTexCoords, newColors, newIndices,
                vertexCount, indexCount);
    }

    @Nonnull
    public Rect2fc getBounds() {
        return mBounds;
    }

    public void getBounds(Rect2f bounds) {
        mBounds.store(bounds);
    }

    // used internally

    @ApiStatus.Internal
    public int getVertexMode() {
        return mVertexMode;
    }

    @ApiStatus.Internal
    public boolean hasColors() {
        return mColors != null;
    }

    @ApiStatus.Internal
    public boolean hasTexCoords() {
        return mTexCoords != null;
    }

    @ApiStatus.Internal
    public boolean hasIndices() {
        return mIndices != null;
    }

    @ApiStatus.Internal
    public int getVertexCount() {
        return mVertexCount;
    }

    @ApiStatus.Internal
    public int getIndexCount() {
        return mIndexCount;
    }

    @ApiStatus.Internal
    public float[] getPositions() {
        return mPositions;
    }

    @ApiStatus.Internal
    public float[] getTexCoords() {
        return mTexCoords;
    }

    @ApiStatus.Internal
    public byte[] getColors() {
        return mColors;
    }

    @ApiStatus.Internal
    public short[] getIndices() {
        return mIndices;
    }
}
