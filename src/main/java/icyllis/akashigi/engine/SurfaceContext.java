/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.engine;

import icyllis.akashigi.core.ImageInfo;
import icyllis.akashigi.core.ImageInfo.AlphaType;

import static icyllis.akashigi.engine.Engine.SurfaceOrigin;

/**
 * Helper to orchestrate commands for a particular surface.
 * <p>
 * The base class assumes the surface is not renderable, so you can only
 * perform simple read and write operations.
 */
public class SurfaceContext implements AutoCloseable {

    protected final RecordingContext mContext;
    protected final SurfaceProxyView mReadView;

    private final int mColorInfo;

    /**
     * @param context   raw ptr to the context
     * @param readView  raw ptr to the read view
     * @param colorInfo see {@link ImageInfo#makeColorInfo(int, int)}
     */
    public SurfaceContext(RecordingContext context,
                          SurfaceProxyView readView,
                          int colorInfo) {
        assert !context.isDiscarded();
        mContext = context;
        mReadView = readView;
        mColorInfo = colorInfo;
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
    public final SurfaceProxyView getReadView() {
        return mReadView;
    }

    /**
     * @see ImageInfo#makeColorInfo(int, int)
     */
    public final int getColorInfo() {
        return mColorInfo;
    }

    /**
     * @see ImageInfo#COLOR_TYPE_UNKNOWN
     */
    public final int getColorType() {
        return ImageInfo.colorType(mColorInfo);
    }

    /**
     * @see ImageInfo#ALPHA_TYPE_UNKNOWN
     */
    @AlphaType
    public final int getAlphaType() {
        return ImageInfo.alphaType(mColorInfo);
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

    protected final DrawingManager getDrawingManager() {
        return mContext.getDrawingManager();
    }

    /**
     * Destructs this context.
     */
    @Override
    public void close() {
        mReadView.close();
    }
}
