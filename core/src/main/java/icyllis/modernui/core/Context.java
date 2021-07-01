/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.core;

import icyllis.modernui.graphics.Sprite;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;

// Work in Process
public abstract class Context {

    /**
     * Gets a resource from file system with the given path.
     *
     * @param path the path of the resource
     * @return a readable channel
     * @throws IOException cannot to get the resource
     */
    public abstract ReadableByteChannel getResource(@Nonnull Path path) throws IOException;

    /**
     * Gets an image resource with the given path. Each call returns a new object,
     * but the associated texture is shared.
     *
     * @param path the path of the image
     * @return an image or <code>null</code> if errors occurred
     */
    @Nullable
    public abstract Sprite getImage(@Nonnull Path path, boolean antiAliasing);
}
