/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.test;

import icyllis.arc3d.core.BlendMode;
import icyllis.arc3d.core.Color;

public class TestBlendMode {

    public static void main(String[] args) {
        int src = Color.argb(240, 33, 150, 243);
        int dst = Color.argb(220, 233, 30, 99);
        int result = Color.blend(BlendMode.OVERLAY, src, dst);
        System.out.printf("(%d, %d, %d, %d)",
                Color.alpha(result), Color.red(result), Color.green(result), Color.blue(result));
    }
}
