/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.engine.graphene;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.ops.DrawOp;
import icyllis.arc3d.engine.ops.RectOp;

import javax.annotation.Nullable;

public class SurfaceDrawContext extends SurfaceFillContext {

    private ImageProxy mDepthStencilTarget;

    private DrawOpList mPendingDrawOps = new DrawOpList();

    public SurfaceDrawContext(RecordingContext context,
                              ImageProxyView readView,
                              ImageProxyView writeView,
                              int colorType,
                              ColorSpace colorSpace) {
        super(context, readView, writeView, colorType, ColorInfo.AT_PREMUL, colorSpace);
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

        /*BackendFormat format = rContext.getCaps().getDefaultBackendFormat(colorType, true);
        if (format == null) {
            return null;
        }

        @SharedPtr
        RenderTargetProxy renderTarget = rContext.getSurfaceProvider().createRenderTexture(
                format,
                width,
                height,
                sampleCount,
                surfaceFlags
        );
        if (renderTarget == null) {
            return null;
        }*/
        ImageDesc desc = rContext.getCaps().getDefaultColorImageDesc(Engine.ImageType.k2D,
                colorType,
                width,
                height,
                1,
                0,
                sampleCount,
                surfaceFlags);
        if (!desc.isValid()) {
            return null;
        }
        ImageProxy proxy = ImageProxy.make(rContext, desc, true, "SurfaceDrawContext");
        if (proxy == null) {
            return null;
        }

        short readSwizzle = rContext.getCaps().getReadSwizzle(desc, colorType);
        short writeSwizzle = rContext.getCaps().getWriteSwizzle(desc, colorType);

        // two views, inc one more ref
        proxy.ref();
        ImageProxyView readView = new ImageProxyView(proxy, origin, readSwizzle);
        ImageProxyView writeView = new ImageProxyView(proxy, origin, writeSwizzle);

        return new SurfaceDrawContext(rContext, readView, writeView, colorType, colorSpace);
    }

    public static SurfaceDrawContext make(RecordingContext rContext,
                                          int colorType,
                                          ColorSpace colorSpace,
                                          ImageProxy imageProxy,
                                          int origin) {
        ImageDesc desc = imageProxy.getDesc();

        short readSwizzle = rContext.getCaps().getReadSwizzle(desc, colorType);
        short writeSwizzle = rContext.getCaps().getWriteSwizzle(desc, colorType);

        // two views, inc one more ref
        imageProxy.ref();
        ImageProxyView readView = new ImageProxyView(imageProxy, origin, readSwizzle);
        ImageProxyView writeView = new ImageProxyView(imageProxy, origin, writeSwizzle);

        return new SurfaceDrawContext(rContext, readView, writeView, colorType, colorSpace);
    }

    private final Rect2f mTmpBounds = new Rect2f();
    private final ClipResult_old mTmpClipResult = new ClipResult_old();

    public void fillRect(@Nullable Clip_old clip,
                         int color,
                         Rect2f rect,
                         Matrixc viewMatrix,
                         boolean aa) {

        var op = new RectOp(color, rect, 0, 0, viewMatrix, false, aa);

        addDrawOp(clip, op);
    }

    public void recordDraw(icyllis.arc3d.engine.graphene.DrawOp draw) {
        mPendingDrawOps.recordDrawOp(draw);
    }

    /**
     * @param clip the clip function, or null
     * @param op   a newly-created Op instance
     */
    public void addDrawOp(@Nullable Clip_old clip,
                          DrawOp op) {


        var surface = getReadView().getProxy();

        var bounds = mTmpBounds;
        bounds.set(op);
        if (op.hasZeroArea()) {
            bounds.outset(1, 1);
        }
        ClipResult_old clipResult;

        boolean rejected;
        if (clip != null) {
            clipResult = mTmpClipResult;
            clipResult.init(
                    surface.getWidth(), surface.getHeight(),
                    surface.getWidth(), surface.getHeight()
            );
            rejected = clip.apply(
                    this, op.hasAABloat(), clipResult, bounds
            ) == Clip_old.CLIPPED_OUT;
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

    public void flush(RecordingContext context) {

    }
}
