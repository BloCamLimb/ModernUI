/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.graphics.font;

public enum TextAlign {
    LEFT(0.0f),
    CENTER(0.25f),
    RIGHT(0.5f);

    /* Divided by 2, because font renderer is based on a gui scale of 2 */
    private float offsetFactor;

    TextAlign(float offsetFactor) {
        this.offsetFactor = offsetFactor;
    }

    public float getOffsetFactor() {
        return offsetFactor;
    }
}
