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

package icyllis.arc3d.sketch;

import icyllis.arc3d.core.Rect2f;
import icyllis.arc3d.core.Rect2fc;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
    private final int[] mColors;
    private final short[] mIndices;

    private final int     mVertexCount;
    private final int     mIndexCount;

    private final Rect2f mBounds;

    Vertices(int vertexMode, float[] positions, float[] texCoords,
             int[] colors, short[] indices, int vertexCount, int indexCount) {
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

    @NonNull
    public static Vertices makeCopy(int vertexMode, int vertexCount,
            float @NonNull[] positions, int positionOffset,
            float @Nullable[] texCoords, int texCoordOffset,
            int @Nullable[] colors, int colorOffset) {
        return makeCopy(vertexMode, vertexCount,
                positions, positionOffset,
                texCoords, texCoordOffset,
                colors, colorOffset,
                null, 0, 0);
    }

    @NonNull
    public static Vertices makeCopy(int vertexMode, int vertexCount,
            float @NonNull[] positions, int positionOffset,
            float @Nullable[] texCoords, int texCoordOffset,
            int @Nullable[] colors, int colorOffset,
            short @Nullable[] indices, int indexOffset, int indexCount) {
        float[] newPositions = Arrays.copyOfRange(positions, positionOffset, positionOffset + vertexCount * 2);
        float[] newTexCoords = null;
        if (texCoords != null) {
            newTexCoords = Arrays.copyOfRange(texCoords, texCoordOffset, texCoordOffset + vertexCount * 2);
        }
        int[] newColors = null;
        if (colors != null) {
            newColors = Arrays.copyOfRange(colors, colorOffset, colorOffset + vertexCount);
        }
        short[] newIndices = null;
        if (indices != null) {
            newIndices = Arrays.copyOfRange(indices, indexOffset, indexOffset + indexCount);
        }
        return new Vertices(vertexMode, newPositions, newTexCoords, newColors, newIndices,
                vertexCount, indexCount);
    }

    @NonNull
    public static Vertices makeCopy(int vertexMode,
                                    @NonNull FloatBuffer positions,
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
        int[] newColors = null;
        if (colors != null) {
            newColors = new int[vertexCount];
            colors.get(colors.position(), newColors);
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

    @NonNull
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
    public int[] getColors() {
        return mColors;
    }

    @ApiStatus.Internal
    public short[] getIndices() {
        return mIndices;
    }
}
