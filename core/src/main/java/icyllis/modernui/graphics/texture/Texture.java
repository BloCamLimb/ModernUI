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
 * Represents OpenGL texture objects at low-level.
 */
public abstract class Texture implements AutoCloseable {

    /**
     * ERROR: if target is GL_TEXTURE_RECTANGLE and either of wrap mode GL_TEXTURE_WRAP_S or GL_TEXTURE_WRAP_T is set to
     * either GL_MIRROR_CLAMP_TO_EDGE, GL_MIRRORED_REPEAT or GL_REPEAT.
     */
    public void setWrapMode(int wrapS) {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrapS);
    }

    public void setWrapMode(int wrapS, int wrapT, int wrapR) {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrapS);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrapT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_R, wrapR);
    }
}
