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

/**
 * Represents 2D textures can be read/write pixels, can be used as attachments of render targets.
 */
public abstract class Texture extends Attachment {

    private int mFlags;

    public Texture(Server server, int width, int height, boolean isProtected) {
        super(server, width, height);
        if (isProtected) {
            mFlags |= Types.INTERNAL_SURFACE_FLAG_PROTECTED;
        }
    }

    public final int getFlags() {
        return mFlags;
    }

    /**
     * @return true if pixels in the texture are read-only.
     */
    @Override
    public final boolean isReadOnly() {
        return (mFlags & Types.INTERNAL_SURFACE_FLAG_READ_ONLY) != 0;
    }

    /**
     * @return true if we are working with protected content.
     */
    @Override
    public final boolean isProtected() {
        return (mFlags & Types.INTERNAL_SURFACE_FLAG_PROTECTED) != 0;
    }
}
