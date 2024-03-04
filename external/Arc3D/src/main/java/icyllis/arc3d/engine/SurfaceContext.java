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

import icyllis.arc3d.core.ColorSpace;
import icyllis.arc3d.core.ImageInfo;

import static icyllis.arc3d.engine.Engine.SurfaceOrigin;

/**
 * Helper to orchestrate commands for a particular surface.
 * <p>
 * The base class assumes the surface is not renderable, so you can only
 * perform simple read and write operations.
 */
public class SurfaceContext implements AutoCloseable {

    protected final RecordingContext mContext;
    protected final SurfaceView mReadView;

    private final ImageInfo mImageInfo;

    /**
     * @param context  raw ptr to the context
     * @param readView raw ptr to the read view
     */
    public SurfaceContext(RecordingContext context,
                          SurfaceView readView,
                          int colorType,
                          int alphaType,
                          ColorSpace colorSpace) {
        assert !context.isDiscarded();
        mContext = context;
        mReadView = readView;
        mImageInfo = new ImageInfo(
                getWidth(), getHeight(),
                colorType, alphaType, colorSpace
        );
    }

    /**
     * @return raw ptr to the context
     */
    public final RecordingContext getContext() {
        return mContext;
    }

    /**
     * @return raw ptr to the read view
     */
    public final SurfaceView getReadView() {
        return mReadView;
    }

    public final ImageInfo getImageInfo() {
        return mImageInfo;
    }

    /**
     * @see ImageInfo.ColorType
     */
    public final int getColorType() {
        return mImageInfo.colorType();
    }

    /**
     * @see ImageInfo.AlphaType
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
     * @see SurfaceOrigin
     */
    public final int getOrigin() {
        return mReadView.mOrigin;
    }

    /**
     * @see Swizzle
     */
    public final short getReadSwizzle() {
        return mReadView.mSwizzle;
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
        mReadView.close();
    }
}
