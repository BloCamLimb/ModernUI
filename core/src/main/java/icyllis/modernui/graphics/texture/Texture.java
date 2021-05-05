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

public class Texture implements AutoCloseable {

    private int mId = INVALID_ID;

    private int mTarget;

    public Texture() {
        this(GL_TEXTURE_2D);
    }

    public Texture(int target) {
        mTarget = target;
    }

    /**
     * Returns the OpenGL texture object name represented by this object.
     * It will be generated if it's unassigned. This operation does not
     * allocate GPU memory.
     *
     * @return texture object name
     */
    public int getId() {
        if (mId == INVALID_ID)
            mId = glGenTextures();
        return mId;
    }

    public void setTarget(int target) {
        if (mId == INVALID_ID)
            mTarget = target;
    }

    public void bind() {
        bindTexture(mTarget, getId());
    }

    public void destroy() {
        if (mId != INVALID_ID) {
            deleteTexture(mId);
            mId = INVALID_ID;
        }
    }

    @Override
    public void close() throws Exception {
        destroy();
    }
}
