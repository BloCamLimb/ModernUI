/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite.trash;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.ImageViewProxy;
import icyllis.arc3d.engine.RenderTargetProxy;
import icyllis.arc3d.engine.SurfaceProxy;
import icyllis.arc3d.granite.RenderTask;
import icyllis.arc3d.granite.RenderTaskManager;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public final class TextureResolveTask extends RenderTask {

    private record Resolve(int flags, int msaaLeft, int msaaTop, int msaaRight, int msaaBottom) {
    }

    private final List<Resolve> mResolves = new ArrayList<>(4);

    public TextureResolveTask(RenderTaskManager taskManager) {
        super(taskManager);
    }

    public void addResolveTarget(@SharedPtr SurfaceProxy proxy, int resolveFlags) {
        // Ensure the last render task that operated on the proxy is closed. That's where msaa and
        // mipmaps should have been marked dirty.
        assert (mTaskManager.getLastRenderTask(proxy) == null ||
                mTaskManager.getLastRenderTask(proxy).isClosed());
        assert (resolveFlags != 0);

        Rect2ic msaaRect;
        if ((resolveFlags & RESOLVE_FLAG_MSAA) != 0) {
            RenderTargetProxy renderTargetProxy = proxy.asRenderTargetProxy();
            assert renderTargetProxy != null;
            assert (renderTargetProxy.needsResolve());
            msaaRect = renderTargetProxy.getResolveRect();
            renderTargetProxy.setResolveRect(0, 0, 0, 0);
        } else {
            msaaRect = Rect2i.empty();
        }

        if ((resolveFlags & RESOLVE_FLAG_MIPMAPS) != 0) {
            ImageViewProxy imageViewProxy = proxy.asImageProxy();
            assert imageViewProxy != null;
            /*assert (imageProxy.isMipmapped() && imageProxy.isMipmapsDirty());
            imageProxy.setMipmapsDirty(false);*/
        }

        mResolves.add(new Resolve(resolveFlags,
                msaaRect.left(),
                msaaRect.top(),
                msaaRect.right(),
                msaaRect.bottom())
        );

        // Add the proxy as a dependency: We will read the existing contents of this texture while
        // generating mipmap levels and/or resolving MSAA.
        //addDependency(proxy, SamplerDesc.DEFAULT);
        addTarget(proxy);
    }

    @Override
    public boolean execute(OpFlushState flushState) {
        return false;
    }
}
