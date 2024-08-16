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

import icyllis.arc3d.core.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;

public class FramebufferCache implements AutoCloseable {

    private final HashMap<FramebufferDesc, @SharedPtr Framebuffer> mFramebufferMap =
            new HashMap<>();

    @Nullable
    @SharedPtr
    public Framebuffer findFramebuffer(@Nonnull FramebufferDesc desc) {
        Framebuffer framebuffer = mFramebufferMap.get(desc);
        if (framebuffer != null) {
            framebuffer.setLastUsedTime();
            framebuffer.ref();
        }
        return framebuffer;
    }

    public void insertFramebuffer(@Nonnull FramebufferDesc desc,
                                  @RawPtr Framebuffer framebuffer) {
        var old = mFramebufferMap.put(desc, framebuffer);
        assert old == null;
        framebuffer.setLastUsedTime();
        framebuffer.ref();
    }

    public void purgeAllFramebuffers() {
        mFramebufferMap.values().forEach(RefCnt::unref);
        mFramebufferMap.clear();
    }

    public void purgeFramebuffersNotUsedSince(long timeMillis) {
        if (mFramebufferMap.isEmpty()) {
            return;
        }
        var framebuffersToDelete = new ObjectArrayList<FramebufferDesc>();
        for (var e : mFramebufferMap.entrySet()) {
            if (e.getKey().isStale() || (e.getValue().getLastUsedTime() < timeMillis)) {
                framebuffersToDelete.add(e.getKey());
            }
        }
        for (var desc : framebuffersToDelete) {
            mFramebufferMap.remove(desc).unref();
        }
    }

    public void purgeStaleFramebuffers() {
        if (mFramebufferMap.isEmpty()) {
            return;
        }
        // If there are too many framebuffers, delete ones that have not been used for more than 20 seconds
        boolean useTime = mFramebufferMap.size() > 32;
        long timeMillis = useTime ? System.currentTimeMillis() - 20000 : 0;
        var framebuffersToDelete = new ObjectArrayList<FramebufferDesc>();
        for (var e : mFramebufferMap.entrySet()) {
            if (e.getKey().isStale() || (useTime && e.getValue().getLastUsedTime() < timeMillis)) {
                framebuffersToDelete.add(e.getKey());
            }
        }
        for (var desc : framebuffersToDelete) {
            mFramebufferMap.remove(desc).unref();
        }
    }

    @Override
    public void close() {
        purgeAllFramebuffers();
    }
}
