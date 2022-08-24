/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arctic.engine;

/**
 * A helper object to orchestrate commands for a particular surface.
 */
public abstract class SurfaceContext implements AutoCloseable {

    protected final RecordingContext mContext;
    protected final SurfaceProxyView mReadView;
    protected final int mColorInfo;

    protected SurfaceContext(RecordingContext context,
                             SurfaceProxyView readView,
                             int colorInfo) {
        assert !context.isDropped();
        mContext = context;
        mReadView = readView;
        mColorInfo = colorInfo;
    }

    /**
     * @return raw ptr to recording context
     */
    public final RecordingContext getContext() {
        return mContext;
    }

    /**
     * @return raw ptr to read surface view
     */
    public final SurfaceProxyView getReadView() {
        return mReadView;
    }

    public final int getColorInfo() {
        return mColorInfo;
    }

    public final int getWidth() {
        return mReadView.mProxy.getWidth();
    }

    public final int getHeight() {
        return mReadView.mProxy.getHeight();
    }

    public final boolean isMipmapped() {
        return mReadView.mProxy.isMipmapped();
    }

    /**
     * Boolean flag, true for BottomLeft, false for TopLeft.
     * Read view and write view should have the same origin.
     */
    public final int getOrigin() {
        return mReadView.mOrigin;
    }

    public final short getReadSwizzle() {
        return mReadView.mSwizzle;
    }

    public final Caps getCaps() {
        return mContext.caps();
    }

    protected final DrawingManager getDrawingManager() {
        return mContext.getDrawingManager();
    }

    @Override
    public void close() throws Exception {
        mReadView.close();
    }
}
