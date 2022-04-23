/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.hgi;

import javax.annotation.Nonnull;

/**
 * Base class that represents something that can be color/depth/stencil attachment
 * of render targets. To be exact, GLTexture, GLRenderbuffer and VkImage.
 * <p>
 * The reason why we abstract this class from Texture is that GLRenderbuffer can
 * be used as attachments, but they are not really textures.
 */
public abstract class Attachment extends Resource {

    private final int mWidth;
    private final int mHeight;

    public Attachment(Server server, int width, int height) {
        super(server);
        mWidth = width;
        mHeight = height;
    }

    /**
     * Returns the width of this attachment.
     */
    public final int getWidth() {
        return mWidth;
    }

    /**
     * Returns the height of this attachment.
     */
    public final int getHeight() {
        return mHeight;
    }

    /**
     * Describes the backend format of this attachment.
     */
    @Nonnull
    public abstract BackendFormat getBackendFormat();

    public abstract int getSampleCount();

    /**
     * @return true if pixels in this attachment are read-only.
     */
    public abstract boolean isReadOnly();

    /**
     * @return true if we are working with protected content.
     */
    public abstract boolean isProtected();
}
