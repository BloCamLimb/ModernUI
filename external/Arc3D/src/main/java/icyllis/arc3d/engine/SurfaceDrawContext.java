/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.ops.DrawOp;
import icyllis.arc3d.engine.ops.RectOp;

import javax.annotation.Nullable;

public class SurfaceDrawContext extends SurfaceFillContext {

    public SurfaceDrawContext(RecordingContext context,
                              SurfaceView readView,
                              SurfaceView writeView,
                              int colorType,
                              ColorSpace colorSpace) {
        super(context, readView, writeView, colorType, ImageInfo.AT_PREMUL, colorSpace);
    }

    public static SurfaceDrawContext make(
            RecordingContext rContext,
            int colorType,
            ColorSpace colorSpace,
            int width, int height,
            int sampleCount,
            int surfaceFlags,
            int origin) {
        if (rContext == null || rContext.isDiscarded()) {
            return null;
        }

        BackendFormat format = rContext.getCaps().getDefaultBackendFormat(colorType, true);
        if (format == null) {
            return null;
        }

        @SharedPtr
        TextureProxy texture = rContext.getSurfaceProvider().createRenderTexture(
                format,
                width,
                height,
                sampleCount,
                surfaceFlags
        );
        if (texture == null) {
            return null;
        }

        short readSwizzle = rContext.getCaps().getReadSwizzle(format, colorType);
        short writeSwizzle = rContext.getCaps().getWriteSwizzle(format, colorType);

        // two views, inc one more ref
        texture.ref();
        SurfaceView readView = new SurfaceView(texture, origin, readSwizzle);
        SurfaceView writeView = new SurfaceView(texture, origin, writeSwizzle);

        return new SurfaceDrawContext(rContext, readView, writeView, colorType, colorSpace);
    }

    public static SurfaceDrawContext make(RecordingContext rContext,
                                          int colorType,
                                          ColorSpace colorSpace,
                                          SurfaceProxy surfaceProxy,
                                          int origin) {
        BackendFormat format = surfaceProxy.getBackendFormat();

        short readSwizzle = rContext.getCaps().getReadSwizzle(format, colorType);
        short writeSwizzle = rContext.getCaps().getWriteSwizzle(format, colorType);

        // two views, inc one more ref
        surfaceProxy.ref();
        SurfaceView readView = new SurfaceView(surfaceProxy, origin, readSwizzle);
        SurfaceView writeView = new SurfaceView(surfaceProxy, origin, writeSwizzle);

        return new SurfaceDrawContext(rContext, readView, writeView, colorType, colorSpace);
    }

    private final Rect2f mTmpBounds = new Rect2f();
    private final ClipResult mTmpClipResult = new ClipResult();

    public void fillRect(@Nullable Clip clip,
                         int color,
                         Rect2f rect,
                         Matrixc viewMatrix,
                         boolean aa) {

        var op = new RectOp(color, rect, 0, 0, viewMatrix, false, aa);

        addDrawOp(clip, op);
    }

    /**
     * @param clip the clip function, or null
     * @param op   a newly-created Op instance
     */
    public void addDrawOp(@Nullable Clip clip,
                          DrawOp op) {


        var surface = getReadView().getSurface();

        var bounds = mTmpBounds;
        bounds.set(op);
        if (op.hasZeroArea()) {
            bounds.outset(1, 1);
        }
        ClipResult clipResult;

        boolean rejected;
        if (clip != null) {
            clipResult = mTmpClipResult;
            clipResult.init(
                    surface.getWidth(), surface.getHeight(),
                    surface.getBackingWidth(), surface.getBackingHeight()
            );
            rejected = clip.apply(
                    this, op.hasAABloat(), clipResult, bounds
            ) == Clip.CLIPPED_OUT;
        } else {
            clipResult = null;
            // No clip function, so just clip the bounds against the logical render target dimensions
            rejected = !bounds.intersects(
                    0, 0,
                    surface.getWidth(), surface.getHeight()
            );
        }

        if (rejected) {
            return;
        }

        op.setClippedBounds(bounds);

        var ops = getOpsTask();

        ops.addDrawOp(op, clipResult, 0);
    }
}
