/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.opengl;

import icyllis.arc3d.engine.ImageMutableState;

import static org.lwjgl.opengl.GL11C.*;

/**
 * Only used when OpenGL 4.3 texture view is unavailable.
 */
public final class GLImageMutableState extends ImageMutableState {

    // Texture parameter state that is not overridden by a bound sampler object.
    public int baseMipmapLevel;
    public int maxMipmapLevel;
    // The read swizzle, identity by default.
    public int swizzleR = GL_RED;
    public int swizzleG = GL_GREEN;
    public int swizzleB = GL_BLUE;
    public int swizzleA = GL_ALPHA;

    public GLImageMutableState() {
        // These are the OpenGL defaults.
        baseMipmapLevel = 0;
        maxMipmapLevel = 1000;
    }

    /**
     * Makes parameters invalid, forces GLContext to refresh.
     */
    public void invalidate() {
        baseMipmapLevel = ~0;
        maxMipmapLevel = ~0;
        swizzleR = 0;
        swizzleG = 0;
        swizzleB = 0;
        swizzleA = 0;
    }

    public int getSwizzle(int i) {
        return switch (i) {
            case 0 -> swizzleR;
            case 1 -> swizzleG;
            case 2 -> swizzleB;
            case 3 -> swizzleA;
            default -> throw new IndexOutOfBoundsException(i);
        };
    }

    public void setSwizzle(int i, int swiz) {
        switch (i) {
            case 0 -> swizzleR = swiz;
            case 1 -> swizzleG = swiz;
            case 2 -> swizzleB = swiz;
            case 3 -> swizzleA = swiz;
            default -> throw new IndexOutOfBoundsException(i);
        }
    }

    @Override
    public String toString() {
        return '{' +
                "baseMipmapLevel=" + baseMipmapLevel +
                ", maxMipmapLevel=" + maxMipmapLevel +
                ", swizzleR=" + swizzleR +
                ", swizzleG=" + swizzleG +
                ", swizzleB=" + swizzleB +
                ", swizzleA=" + swizzleA +
                '}';
    }
}
