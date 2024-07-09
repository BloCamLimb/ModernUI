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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.RawPtr;
import icyllis.arc3d.core.SharedPtr;

public class DrawAtlas {

    /**
     * Used to locate a chunk in a {@link DrawAtlas}.
     * It's packed as long.
     */
    public static class ChunkLocator {

        public static final int MAX_CHUNKS = 256;
    }

    /**
     * Keep track of generation number for atlases and Chunks.
     */
    public static class AtlasGenerationCounter {

        public static final long INVALID_GENERATION = 0;
        public static final long MAX_GENERATION = (1L << 48) - 1;

        private long mGeneration = 1;

        public long next() {
            if (mGeneration > MAX_GENERATION) {
                mGeneration = 1;
            }
            return mGeneration++;
        }
    }

    @SharedPtr
    private ImageViewProxy mTexture;

    public DrawAtlas(int colorType,
                     int width, int height,
                     int chunkWidth, int chunkHeight,
                     AtlasGenerationCounter generationCounter,
                     boolean useStorageImage,
                     String label) {

    }

    @RawPtr
    public ImageViewProxy getTexture() {
        return mTexture;
    }
}
