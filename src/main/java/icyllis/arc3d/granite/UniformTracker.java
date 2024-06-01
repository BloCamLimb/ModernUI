/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2024 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.engine.BufferViewInfo;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import java.nio.ByteBuffer;
import java.util.ArrayList;

//TODO
public class UniformTracker {

    // uniform data is already de-duplicated, raw ptr
    ArrayList<Reference2IntOpenHashMap<ByteBuffer>> mPerPipelineCaches = new ArrayList<>();
    ArrayList<BufferViewInfo> mGpuBuffers = new ArrayList<>();

    public int trackUniforms(int pipelineIndex,
                             ByteBuffer cpuData) {
        if (cpuData == null) {
            return -1;
        }
        mPerPipelineCaches.ensureCapacity(pipelineIndex + 1);
        mGpuBuffers.ensureCapacity(pipelineIndex + 1);

        var cache = mPerPipelineCaches.get(pipelineIndex);
        if (cache == null) {
        }
        return cache.computeIfAbsent(cpuData, __ -> cache.size());
    }
}
