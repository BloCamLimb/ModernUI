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

package icyllis.modernui.graphics.texture;

import static icyllis.modernui.graphics.GLWrapper.*;

/**
 * Represents OpenGL multisample 2D texture objects at low-level. The
 * OpenGL texture associated with this object may be changed by recycling.
 */
public class Texture2DMultisample extends GLTexture {

    public Texture2DMultisample() {
    }

    @Override
    public int getTarget() {
        return GL_TEXTURE_2D_MULTISAMPLE;
    }

    public void init(int internalFormat, int width, int height, int samples) {
        if (samples < 1) {
            throw new IllegalArgumentException();
        }
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException();
        }
        glTextureStorage2DMultisample(get(), samples, internalFormat, width, height, true);
    }

    public void setWrap(int wrapS, int wrapT) {
        final int texture = get();
        glTextureParameteri(texture, GL_TEXTURE_WRAP_S, wrapS);
        glTextureParameteri(texture, GL_TEXTURE_WRAP_T, wrapT);
    }

    public int getWidth() {
        return glGetTextureLevelParameteri(get(), 0, GL_TEXTURE_WIDTH);
    }

    public int getHeight() {
        return glGetTextureLevelParameteri(get(), 0, GL_TEXTURE_HEIGHT);
    }
}
