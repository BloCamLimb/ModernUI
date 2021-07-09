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

package icyllis.modernui.graphics;

import icyllis.modernui.graphics.texture.Texture2D;

/**
 * Images that can be used for drawing and processing. The image data is
 * stored in GPU memory, and an instance of this is associated with an OpenGL
 * texture object.
 */
public class Image implements AutoCloseable {

    private final Source mSource;

    public Image(Source source) {
        mSource = source;
    }

    Source getSource() {
        return mSource;
    }

    @Override
    public void close() {

    }

    /**
     * The shared, immutable texture source for a Sprite.
     */
    public static class Source {

        final Texture2D texture;
        final int width;
        final int height;

        public Source(Texture2D texture, int width, int height) {
            this.texture = texture;
            this.width = width;
            this.height = height;
        }
    }
}
