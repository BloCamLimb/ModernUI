/*
 * Modern UI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
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

package icyllis.modernui.test;

import com.code_intelligence.jazzer.junit.FuzzTest;
import icyllis.arc3d.core.MathUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MainTest {

    @Test
    public void ceilLog() {
        assertEquals(0, MathUtil.ceilLog2(Float.NEGATIVE_INFINITY));
        System.out.println(MathUtil.ceilLog2(1));
        System.out.println(MathUtil.ceilLog2(2));
        System.out.println(MathUtil.ceilLog2(3));
        System.out.println(MathUtil.ceilLog2(7));
        System.out.println(MathUtil.ceilLog2(8));
        assertEquals(128, MathUtil.ceilLog2(Float.POSITIVE_INFINITY));
        assertEquals(0, MathUtil.ceilLog2(Float.NaN));
    }

    @FuzzTest
    public void ceilLogF(float f) {
        int v = MathUtil.ceilLog2(f);
        if (!(f > 1) && !Float.isInfinite(f)) {
            // NaN
            assertEquals(0, v);
        }
    }
}
