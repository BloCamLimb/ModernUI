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

package icyllis.arcui.graphics;

import javax.annotation.Nonnull;

/**
 * Base class for an internal, cacheable, server-side resource object of a surface.
 */
public abstract class Surface extends Resource {

    private final int mWidth;
    private final int mHeight;

    private int mFlags;

    public Surface(Server server, int width, int height, boolean isProtected) {
        super(server);
        mWidth = width;
        mHeight = height;
        if (isProtected) {
            mFlags |= Types.INTERNAL_SURFACE_FLAG_PROTECTED;
        }
    }

    /**
     * Retrieves the width of the surface.
     */
    public final int getWidth() {
        return mWidth;
    }

    /**
     * Retrieves the height of the surface.
     */
    public final int getHeight() {
        return mHeight;
    }

    @Nonnull
    public abstract BackendFormat getBackendFormat();

    public final int getFlags() {
        return mFlags;
    }

    public final boolean isReadOnly() {
        return (mFlags & Types.INTERNAL_SURFACE_FLAG_READ_ONLY) != 0;
    }

    /**
     * @return true if we are working with protected content.
     */
    public final boolean isProtected() {
        return (mFlags & Types.INTERNAL_SURFACE_FLAG_PROTECTED) != 0;
    }
}
