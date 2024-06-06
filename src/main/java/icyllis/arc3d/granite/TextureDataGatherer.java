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

import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.ImageViewProxy;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.IdentityHashMap;
import java.util.function.Function;

public class TextureDataGatherer {

    final IdentityHashMap<ImageViewProxy, Integer> mTextureToIndex = new IdentityHashMap<>();
    final ObjectArrayList<@SharedPtr ImageViewProxy> mIndexToTexture = new ObjectArrayList<>();
    final Function<ImageViewProxy, Integer> mTextureAccumulator = image -> {
        int index = mIndexToTexture.size();
        mIndexToTexture.add(RefCnt.create(image));
        return index;
    };

    final Int2IntOpenHashMap mSamplerToIndex = new Int2IntOpenHashMap();
    final IntArrayList mIndexToSampler = new IntArrayList();
    final Int2IntFunction mSamplerAccumulator = sampler -> {
        int index = mIndexToSampler.size();
        mIndexToSampler.add(sampler);
        return index;
    };

    final IntArrayList mTextureData = new IntArrayList();

    public void add(@SharedPtr ImageViewProxy textureView, int samplerDesc) {
        int textureIndex = mTextureToIndex.computeIfAbsent(textureView, mTextureAccumulator);
        textureView.unref();
        int samplerIndex = mSamplerToIndex.computeIfAbsent(samplerDesc, mSamplerAccumulator);
        mTextureData.add(textureIndex);
        mTextureData.add(samplerIndex);
    }

    public void reset() {
        mTextureData.clear();
    }

    /**
     * Returns a copied int array representing the texture binding.
     * Holds repeated texture index and sampler index.
     */
    public int[] finish() {
        return mTextureData.toIntArray();
    }
}
