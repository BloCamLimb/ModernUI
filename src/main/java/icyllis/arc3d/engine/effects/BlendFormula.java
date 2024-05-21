/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine.effects;

/**
 * Wraps the shader outputs and HW blend state that comprise a Porter Duff blend mode with coverage.
 */
public class BlendFormula {

    public static final int
            OUTPUT_TYPE_ZERO = 0, //<! 0
            OUTPUT_TYPE_COVERAGE = 1,    //<! inputCoverage
            OUTPUT_TYPE_MODULATE = 2,    //<! inputColor * inputCoverage
            OUTPUT_TYPE_SRC_ALPHA_MODULATE = 3,  //<! inputColor.a * inputCoverage
            OUTPUT_TYPE_ONE_MINUS_SRC_ALPHA_MODULATE = 4, //<! (1 - inputColor.a) * inputCoverage
            OUTPUT_TYPE_ONE_MINUS_SRC_COLOR_MODULATE = 5; //<! (1 - inputColor) * inputCoverage
}
