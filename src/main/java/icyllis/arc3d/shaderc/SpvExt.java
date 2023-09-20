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

package icyllis.arc3d.shaderc;

public final class SpvExt {

    /**
     * See <a href="https://github.com/KhronosGroup/SPIRV-Headers/blob/master/include/spirv/unified1/GLSL.std.450.h">
     * GLSL.std.450</a>
     */
    public static final int GLSLstd450Version = 100;
    public static final int GLSLstd450Revision = 3;

    /**
     * {@code GLSLstd450}
     *
     * <h5>Enum values:</h5>
     */
    public static final int
            GLSLstd450Bad = 0,
            GLSLstd450Round = 1,
            GLSLstd450RoundEven = 2,
            GLSLstd450Trunc = 3,
            GLSLstd450FAbs = 4,
            GLSLstd450SAbs = 5,
            GLSLstd450FSign = 6,
            GLSLstd450SSign = 7,
            GLSLstd450Floor = 8,
            GLSLstd450Ceil = 9,
            GLSLstd450Fract = 10,
            GLSLstd450Radians = 11,
            GLSLstd450Degrees = 12,
            GLSLstd450Sin = 13,
            GLSLstd450Cos = 14,
            GLSLstd450Tan = 15,
            GLSLstd450Asin = 16,
            GLSLstd450Acos = 17,
            GLSLstd450Atan = 18,
            GLSLstd450Sinh = 19,
            GLSLstd450Cosh = 20,
            GLSLstd450Tanh = 21,
            GLSLstd450Asinh = 22,
            GLSLstd450Acosh = 23,
            GLSLstd450Atanh = 24,
            GLSLstd450Atan2 = 25,
            GLSLstd450Pow = 26,
            GLSLstd450Exp = 27,
            GLSLstd450Log = 28,
            GLSLstd450Exp2 = 29,
            GLSLstd450Log2 = 30,
            GLSLstd450Sqrt = 31,
            GLSLstd450InverseSqrt = 32,
            GLSLstd450Determinant = 33,
            GLSLstd450MatrixInverse = 34,
            GLSLstd450Modf = 35,
            GLSLstd450ModfStruct = 36,
            GLSLstd450FMin = 37,
            GLSLstd450UMin = 38,
            GLSLstd450SMin = 39,
            GLSLstd450FMax = 40,
            GLSLstd450UMax = 41,
            GLSLstd450SMax = 42,
            GLSLstd450FClamp = 43,
            GLSLstd450UClamp = 44,
            GLSLstd450SClamp = 45,
            GLSLstd450FMix = 46,
            GLSLstd450IMix = 47,
            GLSLstd450Step = 48,
            GLSLstd450SmoothStep = 49,
            GLSLstd450Fma = 50,
            GLSLstd450Frexp = 51,
            GLSLstd450FrexpStruct = 52,
            GLSLstd450Ldexp = 53,
            GLSLstd450PackSnorm4x8 = 54,
            GLSLstd450PackUnorm4x8 = 55,
            GLSLstd450PackSnorm2x16 = 56,
            GLSLstd450PackUnorm2x16 = 57,
            GLSLstd450PackHalf2x16 = 58,
            GLSLstd450PackDouble2x32 = 59,
            GLSLstd450UnpackSnorm2x16 = 60,
            GLSLstd450UnpackUnorm2x16 = 61,
            GLSLstd450UnpackHalf2x16 = 62,
            GLSLstd450UnpackSnorm4x8 = 63,
            GLSLstd450UnpackUnorm4x8 = 64,
            GLSLstd450UnpackDouble2x32 = 65,
            GLSLstd450Length = 66,
            GLSLstd450Distance = 67,
            GLSLstd450Cross = 68,
            GLSLstd450Normalize = 69,
            GLSLstd450FaceForward = 70,
            GLSLstd450Reflect = 71,
            GLSLstd450Refract = 72,
            GLSLstd450FindILsb = 73,
            GLSLstd450FindSMsb = 74,
            GLSLstd450FindUMsb = 75,
            GLSLstd450InterpolateAtCentroid = 76,
            GLSLstd450InterpolateAtSample = 77,
            GLSLstd450InterpolateAtOffset = 78,
            GLSLstd450NMin = 79,
            GLSLstd450NMax = 80,
            GLSLstd450NClamp = 81;
}
