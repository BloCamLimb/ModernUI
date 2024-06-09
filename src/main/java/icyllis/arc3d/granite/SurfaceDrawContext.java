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

package icyllis.arc3d.granite;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;

/**
 * Used by {@link SurfaceDevice}
 */
public class SurfaceDrawContext implements AutoCloseable {

    private final RecordingContext mContext;
    private final ImageInfo mImageInfo;

    private final ImageViewProxy mReadView;
    private final short mWriteSwizzle;

    private ImageViewProxy mDepthStencilTarget;

    private DrawList mPendingDrawOps = new DrawList();

    public SurfaceDrawContext(RecordingContext context,
                              ImageViewProxy readView,
                              short writeSwizzle,
                              int colorType,
                              int alphaType,
                              ColorSpace colorSpace) {
        assert !context.isDiscarded();
        mContext = context;
        mReadView = readView;
        mWriteSwizzle = writeSwizzle;
        mImageInfo = new ImageInfo(
                getWidth(), getHeight(),
                colorType, alphaType, colorSpace
        );
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
        if (desc == null) {
            return null;
        }
        short readSwizzle = rContext.getCaps().getReadSwizzle(desc, colorType);
        ImageViewProxy targetView = ImageViewProxy.make(rContext, desc,
                origin, readSwizzle,
                /*budgeted*/true, "SurfaceDrawContext");
        if (targetView == null) {
            return null;
        }
        short writeSwizzle = rContext.getCaps().getWriteSwizzle(desc, colorType);

        return new SurfaceDrawContext(rContext, targetView, writeSwizzle,
                colorType, ColorInfo.AT_PREMUL, colorSpace);
    }

    /*public static SurfaceDrawContext make(RecordingContext rContext,
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
    }*/

    /**
     * @return raw ptr to the context
     */
    @RawPtr
    public final RecordingContext getContext() {
        return mContext;
    }

    /**
     * @return raw ptr to the read view
     */
    @RawPtr
    public final ImageViewProxy getReadView() {
        return mReadView;
    }

    public final ImageInfo getImageInfo() {
        return mImageInfo;
    }

    /**
     * @see ColorInfo.ColorType
     */
    public final int getColorType() {
        return mImageInfo.colorType();
    }

    /**
     * @see ColorInfo.AlphaType
     */
    public final int getAlphaType() {
        return mImageInfo.alphaType();
    }

    public final int getWidth() {
        return mReadView.getWidth();
    }

    public final int getHeight() {
        return mReadView.getHeight();
    }

    public final boolean isMipmapped() {
        return mReadView.isMipmapped();
    }

    /**
     * Read view and write view have the same origin.
     *
     * @see Engine.SurfaceOrigin
     */
    public final int getOrigin() {
        return mReadView.getOrigin();
    }

    /**
     * @see Swizzle
     */
    public final short getReadSwizzle() {
        return mReadView.getSwizzle();
    }

    public final Caps getCaps() {
        return mContext.getCaps();
    }

    protected final RenderTaskManager getDrawingManager() {
        return mContext.getRenderTaskManager();
    }

    /**
     * Destructs this context.
     */
    @Override
    public void close() {
        mReadView.unref();
    }

    public void recordDraw(Draw draw) {
        mPendingDrawOps.recordDrawOp(draw);
    }

    public void flush(RecordingContext context) {

    }
}
