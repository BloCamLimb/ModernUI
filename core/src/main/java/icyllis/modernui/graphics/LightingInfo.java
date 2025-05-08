/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

import org.jetbrains.annotations.ApiStatus;

/**
 * @hidden
 */
@ApiStatus.Internal
public class LightingInfo {

    private static float sLightX = 320;
    private static float sLightY = 0;
    private static float sLightZ = 500;
    private static float sLightRadius = 800;

    public static float getLightRadius() {
        return sLightRadius;
    }

    public static float getLightX() {
        return sLightX;
    }

    public static float getLightY() {
        return sLightY;
    }

    public static float getLightZ() {
        return sLightZ;
    }

    public static void setLightCenter(float x, float y, float z) {
        sLightX = x;
        sLightY = y;
        sLightZ = z;
    }

    public static void setLightGeometry(float x, float y, float z, float radius) {
        sLightX = x;
        sLightY = y;
        sLightZ = z;
        sLightRadius = radius;
    }
}
