/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package icyllis.modernui.graphics.font;

public enum TextAlign {
    LEFT(0.0f),
    CENTER(0.25f),
    RIGHT(0.5f);

    /* Divided by 2, because font renderer is based on a gui scale of 2 */
    final float offsetFactor;

    TextAlign(float offsetFactor) {
        this.offsetFactor = offsetFactor;
    }

    public float getOffsetFactor() {
        return offsetFactor;
    }
}
