/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import icyllis.modernui.annotation.SharedPtr;
import icyllis.modernui.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

public final class TextureResolveTask extends RenderTask {

    private record Resolve(int flags, int msaaLeft, int msaaTop, int msaaRight, int msaaBottom) {
    }

    private final List<Resolve> mResolves = new ArrayList<>(4);

    public TextureResolveTask(DrawingManager drawingMgr) {
        super(drawingMgr);
    }

    public void addProxy(@SharedPtr TextureProxy proxy, int resolveFlags) {
        // Ensure the last render task that operated on the proxy is closed. That's where msaa and
        // mipmaps should have been marked dirty.
        assert (mDrawingMgr.getLastRenderTask(proxy) == null ||
                mDrawingMgr.getLastRenderTask(proxy).isClosed());
        assert (resolveFlags != 0);

        Rect msaaRect = null;
        if ((resolveFlags & RESOLVE_FLAG_MSAA) != 0) {
            assert (proxy.isMSAADirty());
            msaaRect = proxy.getMSAADirtyRect();
            proxy.setMSAADirty(0, 0, 0, 0);
        }

        if ((resolveFlags & RESOLVE_FLAG_MIPMAPS) != 0) {
            assert (proxy.isMipmapped() && proxy.isMipmapsDirty());
            proxy.setMipmapsDirty(false);
        }

        mResolves.add(new Resolve(resolveFlags,
                msaaRect != null ? msaaRect.left : 0,
                msaaRect != null ? msaaRect.top : 0,
                msaaRect != null ? msaaRect.right : 0,
                msaaRect != null ? msaaRect.bottom : 0));

        // Add the proxy as a dependency: We will read the existing contents of this texture while
        // generating mipmap levels and/or resolving MSAA.
        addDependency(proxy, SamplerState.DEFAULT);
        addTarget(proxy);
    }

    @Override
    public boolean execute(OpFlushState flushState) {
        return false;
    }
}
