/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.core.MathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestFrexpLdexp {

    public static final Logger LOGGER = LoggerFactory.getLogger(TestFrexpLdexp.class);

    public static void main(String[] args) {
        int[] exp = new int[1];
        float x=0;
        for (int i = 0; i < 1000000; i++) {
            x = MathUtil.frexp(1234.56f, exp);
        }
        LOGGER.info("significand: {}, exp: {}", x, exp);
        LOGGER.info("ldexp: {}", MathUtil.ldexp(x, exp[0]));
        LOGGER.info("ldexp: {}", MathUtil.ldexp(+0.0f, 500));
        LOGGER.info("ldexp: {}", MathUtil.ldexp(-0.0f, 500));
        LOGGER.info("ldexp: {}", MathUtil.ldexp(Float.NaN, -500));
        LOGGER.info("ldexp: {}", MathUtil.ldexp(Float.POSITIVE_INFINITY, -500));
        LOGGER.info("ldexp: {}", MathUtil.ldexp(Float.NEGATIVE_INFINITY, -500));
    }
}
