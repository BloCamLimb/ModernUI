/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.test.discard;

import javax.annotation.Nullable;

/**
 * Relative to previous widget or host
 */
@Deprecated
public class RelativeLocator implements ILocator {

    private float xOffset = 0.0f;

    private float yOffset = 0.0f;

    private float wOffset = 0.0f;

    private float hOffset = 0.0f;

    @Override
    public float getLocatedX(@Nullable Widget prev, float hostWidth) {
        if (prev != null) {
            return prev.getLeft() + xOffset;
        }
        return xOffset;
    }

    @Override
    public float getLocatedY(@Nullable Widget prev, float hostHeight) {
        if (prev != null) {
            return prev.getTop() + yOffset;
        }
        return yOffset;
    }

    @Override
    public float getSizedW(@Nullable Widget prev, float hostWidth) {
        if (prev != null) {
            return prev.getWidth() + wOffset;
        }
        return wOffset;
    }

    @Override
    public float getSizedH(@Nullable Widget prev, float hostHeight) {
        if (prev != null) {
            return prev.getHeight() + hOffset;
        }
        return hOffset;
    }
}
