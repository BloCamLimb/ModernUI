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
 * Base class that represents something that can be color or depth/stencil
 * attachments of framebuffers, it contains data of 2D images. To be exact,
 * GLTexture, GLRenderbuffer and VkImage. Depth and stencil are packed
 * and used together.
 * <p>
 * We abstract this class from {@link Texture} because GLRenderbuffer can
 * be used as attachments, but they are not textures.
 * <p>
 * The naming of this class is just a convention, don't confuse this with
 * {@link icyllis.arcui.core.Surface} or {@link org.lwjgl.vulkan.KHRSurface},
 */
public abstract class Surface extends Resource {

    protected final int mWidth;
    protected final int mHeight;

    protected Surface(Server server, int width, int height) {
        super(server);
        assert width > 0 && height > 0;
        mWidth = width;
        mHeight = height;
    }

    /**
     * @return the width of this surface
     */
    public final int getWidth() {
        return mWidth;
    }

    /**
     * @return the height of this surface
     */
    public final int getHeight() {
        return mHeight;
    }

    /**
     * Note non-renderable textures are always single sampled. Multisample textures are
     * created with render targets, they cannot be used directly (need MSAA resolve).
     *
     * @return the number of samples
     */
    public abstract int getSampleCount();

    /**
     * @return the backend format of this surface
     */
    @Nonnull
    public abstract BackendFormat getBackendFormat();

    /**
     * @return true if pixels in this surface are read-only
     */
    public abstract boolean isReadOnly();

    /**
     * @return true if we are working with protected content
     */
    public abstract boolean isProtected();
}
