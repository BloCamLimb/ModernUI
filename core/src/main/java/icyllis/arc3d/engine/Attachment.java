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

import icyllis.modernui.annotation.NonNull;

/**
 * Base class that represents something that can be color or depth/stencil
 * attachments of framebuffers. It provides the backing store of 2D images.
 */
public abstract class Attachment extends Resource {

    protected final int mWidth;
    protected final int mHeight;
    protected final int mSampleCount;

    protected Attachment(Server server, int width, int height, int sampleCount) {
        super(server);
        assert width > 0 && height > 0 && sampleCount > 0;
        mWidth = width;
        mHeight = height;
        mSampleCount = sampleCount;
    }

    /**
     * @return the width of the attachment
     */
    public final int getWidth() {
        return mWidth;
    }

    /**
     * @return the height of the attachment
     */
    public final int getHeight() {
        return mHeight;
    }

    /**
     * Returns the number of samples per pixel in color buffers (One if non-MSAA).
     *
     * @return the number of samples, greater than (multisample) or equal to one
     */
    public final int getSampleCount() {
        return mSampleCount;
    }

    /**
     * @return the backend format of the attachment
     */
    @NonNull
    public abstract BackendFormat getBackendFormat();

    /**
     * Compressed surfaces are not recycled and read only.
     *
     * @return true if the surface is created with a compressed format
     */
    public final boolean isFormatCompressed() {
        return getBackendFormat().isCompressed();
    }
}
