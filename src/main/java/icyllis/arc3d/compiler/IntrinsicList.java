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

package icyllis.arc3d.compiler;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * A list of every supported intrinsic.
 */
public final class IntrinsicList {

    public static final int kNotIntrinsic = -1;

    /**
     * General Math Functions
     */
    public static final int
            kRound           = 0,
            kRoundEven       = 1,
            kTrunc           = 2,
            kAbs             = 3,
            kSign            = 4,
            kFloor           = 5,
            kCeil            = 6,
            kFract           = 7,
            kRadians         = 8,
            kDegrees         = 9,
            kSin             = 10,
            kCos             = 11,
            kTan             = 12,
            kAsin            = 13,
            kAcos            = 14,
            kAtan            = 15,
            kSinh            = 16,
            kCosh            = 17,
            kTanh            = 18,
            kAsinh           = 19,
            kAcosh           = 20,
            kAtanh           = 21,
            kPow             = 22,
            kExp             = 23,
            kLog             = 24,
            kExp2            = 25,
            kLog2            = 26,
            kSqrt            = 27,
            kInverseSqrt     = 28,
            kMod             = 29,
            kModf            = 30,
            kMin             = 31,
            kMax             = 32,
            kClamp           = 33,
            kSaturate        = 34,
            kMix             = 35,
            kStep            = 36,
            kSmoothStep      = 37,
            kIsNan           = 38,
            kIsInf           = 39,
            kFloatBitsToInt  = 40,
            kFloatBitsToUint = 41,
            kIntBitsToFloat  = 42,
            kUintBitsToFloat = 43,
            kFma             = 44,
            kFrexp           = 45,
            kLdexp           = 46;
    /**
     * Floating-Point Pack and Unpack Functions
     */
    public static final int
            kPackSnorm4x8     = 47,
            kPackUnorm4x8     = 48,
            kPackSnorm2x16    = 49,
            kPackUnorm2x16    = 50,
            kPackHalf2x16     = 51,
            kPackDouble2x32   = 52,
            kUnpackSnorm4x8   = 53,
            kUnpackUnorm4x8   = 54,
            kUnpackSnorm2x16  = 55,
            kUnpackUnorm2x16  = 56,
            kUnpackHalf2x16   = 57,
            kUnpackDouble2x32 = 58;
    /**
     * Geometric Functions
     */
    public static final int
            kLength      = 59,
            kDistance    = 60,
            kDot         = 61,
            kCross       = 62,
            kNormalize   = 63,
            kFaceForward = 64,
            kReflect     = 65,
            kRefract     = 66;
    /**
     * Vector Relational Functions
     */
    public static final int
            kAny              = 67,
            kAll              = 68,
            kLogicalNot       = 69,
            kEqual            = 70,
            kNotEqual         = 71,
            kLessThan         = 72,
            kGreaterThan      = 73,
            kLessThanEqual    = 74,
            kGreaterThanEqual = 75;
    /**
     * Matrix Functions
     */
    public static final int
            kMatrixCompMult = 76,
            kOuterProduct   = 77,
            kDeterminant    = 78,
            kMatrixInverse  = 79,
            kTranspose      = 80;
    /**
     * Derivative Functions
     */
    public static final int
            kDPdx         = 81,
            kDPdy         = 82,
            kFwidth       = 83,
            kDPdxFine     = 84,
            kDPdyFine     = 85,
            kFwidthFine   = 86,
            kDPdxCoarse   = 87,
            kDPdyCoarse   = 88,
            kFwidthCoarse = 89;
    /**
     * Interpolation Functions
     */
    public static final int
            kInterpolateAtCentroid = 90,
            kInterpolateAtSample   = 91,
            kInterpolateAtOffset   = 92;
    /**
     * Integer Functions
     */
    public static final int
            kAddCarry        = 93,
            kAddBorrow       = 94,
            kUMulExtended    = 95,
            kIMulExtended    = 96,
            kBitfieldExtract = 97,
            kBitfieldInsert  = 98,
            kBitReverse      = 99,
            kBitCount        = 100,
            kFindLSB         = 101,
            kFindMSB         = 102;
    /**
     * Atomic Memory Functions
     */
    public static final int
            kAtomicAdd      = 103,
            kAtomicMin      = 104,
            kAtomicMax      = 105,
            kAtomicAnd      = 106,
            kAtomicOr       = 107,
            kAtomicXor      = 108,
            kAtomicExchange = 109,
            kAtomicCompSwap = 110;
    /**
     * Shader Control Functions
     */
    public static final int
            kBarrier             = 111,
            kMemoryBarrier       = 112,
            kMemoryBarrierBuffer = 113,
            kMemoryBarrierShared = 114,
            kMemoryBarrierImage  = 115,
            kWorkgroupBarrier    = 116;
    /**
     * Shader Invocation Group Functions
     */
    public static final int
            kAnyInvocation       = 117,
            kAllInvocations      = 118,
            kAllInvocationsEqual = 119;
    /**
     * Texture Query Functions
     */
    public static final int
            kTextureQuerySize    = 120,
            kTextureQueryLod     = 121,
            kTextureQueryLevels  = 122,
            kTextureQuerySamples = 123;
    /**
     * Texture Lookup Functions
     */
    public static final int
            kTexture               = 124,
            kTextureProj           = 125,
            kTextureLod            = 126,
            kTextureOffset         = 127,
            kTextureFetch          = 128,
            kTextureFetchOffset    = 129,
            kTextureProjOffset     = 130,
            kTextureLodOffset      = 131,
            kTextureProjLod        = 132,
            kTextureProjLodOffset  = 133,
            kTextureGrad           = 134,
            kTextureGradOffset     = 135,
            kTextureProjGrad       = 136,
            kTextureProjGradOffset = 137;
    /**
     * Texture Gather Functions
     */
    public static final int
            kTextureGather        = 138,
            kTextureGatherOffset  = 139,
            kTextureGatherOffsets = 140;
    /**
     * Image Functions
     */
    public static final int
            kImageQuerySize      = 141,
            kImageQuerySamples   = 142,
            kImageLoad           = 143,
            kImageStore          = 144,
            kImageAtomicAdd      = 145,
            kImageAtomicMin      = 146,
            kImageAtomicMax      = 147,
            kImageAtomicAnd      = 148,
            kImageAtomicOr       = 149,
            kImageAtomicXor      = 150,
            kImageAtomicExchange = 151,
            kImageAtomicCompSwap = 152;
    /**
     * Subpass-Input Functions
     */
    public static final int
            kSubpassLoad = 153;

    private static final Object2IntOpenHashMap<String> sIntrinsicMap;

    static {
        var map = new Object2IntOpenHashMap<String>(191);
        map.defaultReturnValue(kNotIntrinsic);

        map.put("round"                 , kRound                    );
        map.put("roundEven"             , kRoundEven                );
        map.put("trunc"                 , kTrunc                    );
        map.put("abs"                   , kAbs                      );
        map.put("sign"                  , kSign                     );
        map.put("floor"                 , kFloor                    );
        map.put("ceil"                  , kCeil                     );
        map.put("fract"                 , kFract                    );
        map.put("radians"               , kRadians                  );
        map.put("degrees"               , kDegrees                  );
        map.put("sin"                   , kSin                      );
        map.put("cos"                   , kCos                      );
        map.put("tan"                   , kTan                      );
        map.put("asin"                  , kAsin                     );
        map.put("acos"                  , kAcos                     );
        map.put("atan"                  , kAtan                     );
        map.put("sinh"                  , kSinh                     );
        map.put("cosh"                  , kCosh                     );
        map.put("tanh"                  , kTanh                     );
        map.put("asinh"                 , kAsinh                    );
        map.put("acosh"                 , kAcosh                    );
        map.put("atanh"                 , kAtanh                    );
        map.put("pow"                   , kPow                      );
        map.put("exp"                   , kExp                      );
        map.put("log"                   , kLog                      );
        map.put("exp2"                  , kExp2                     );
        map.put("log2"                  , kLog2                     );
        map.put("sqrt"                  , kSqrt                     );
        map.put("inversesqrt"           , kInverseSqrt              );
        map.put("mod"                   , kMod                      );
        map.put("modf"                  , kModf                     );
        map.put("min"                   , kMin                      );
        map.put("max"                   , kMax                      );
        map.put("clamp"                 , kClamp                    );
        map.put("saturate"              , kSaturate                 );
        map.put("mix"                   , kMix                      );
        map.put("step"                  , kStep                     );
        map.put("smoothstep"            , kSmoothStep               );
        map.put("isnan"                 , kIsNan                    );
        map.put("isinf"                 , kIsInf                    );
        map.put("floatBitsToInt"        , kFloatBitsToInt           );
        map.put("floatBitsToUint"       , kFloatBitsToUint          );
        map.put("intBitsToFloat"        , kIntBitsToFloat           );
        map.put("uintBitsToFloat"       , kUintBitsToFloat          );
        map.put("fma"                   , kFma                      );
        map.put("frexp"                 , kFrexp                    );
        map.put("ldexp"                 , kLdexp                    );

        map.put("packSnorm4x8"          , kPackSnorm4x8             );
        map.put("packUnorm4x8"          , kPackUnorm4x8             );
        map.put("packSnorm2x16"         , kPackSnorm2x16            );
        map.put("packUnorm2x16"         , kPackUnorm2x16            );
        map.put("packHalf2x16"          , kPackHalf2x16             );
        map.put("packDouble2x32"        , kPackDouble2x32           );
        map.put("unpackSnorm4x8"        , kUnpackSnorm4x8           );
        map.put("unpackUnorm4x8"        , kUnpackUnorm4x8           );
        map.put("unpackSnorm2x16"       , kUnpackSnorm2x16          );
        map.put("unpackUnorm2x16"       , kUnpackUnorm2x16          );
        map.put("unpackHalf2x16"        , kUnpackHalf2x16           );
        map.put("unpackDouble2x32"      , kUnpackDouble2x32         );

        map.put("length"                , kLength                   );
        map.put("distance"              , kDistance                 );
        map.put("dot"                   , kDot                      );
        map.put("cross"                 , kCross                    );
        map.put("normalize"             , kNormalize                );
        map.put("faceforward"           , kFaceForward              );
        map.put("reflect"               , kReflect                  );
        map.put("refract"               , kRefract                  );

        map.put("any"                   , kAny                      );
        map.put("all"                   , kAll                      );
        map.put("not"                   , kLogicalNot               );
        map.put("equal"                 , kEqual                    );
        map.put("notEqual"              , kNotEqual                 );
        map.put("lessThan"              , kLessThan                 );
        map.put("greaterThan"           , kGreaterThan              );
        map.put("lessThanEqual"         , kLessThanEqual            );
        map.put("greaterThanEqual"      , kGreaterThanEqual         );

        map.put("matrixCompMult"        , kMatrixCompMult           );
        map.put("outerProduct"          , kOuterProduct             );
        map.put("determinant"           , kDeterminant              );
        map.put("inverse"               , kMatrixInverse            );
        map.put("transpose"             , kTranspose                );

        map.put("dFdx"                  , kDPdx                     );
        map.put("dFdy"                  , kDPdy                     );
        map.put("fwidth"                , kFwidth                   );
        map.put("dFdxFine"              , kDPdxFine                 );
        map.put("dFdyFine"              , kDPdyFine                 );
        map.put("fwidthFine"            , kFwidthFine               );
        map.put("dFdxCoarse"            , kDPdxCoarse               );
        map.put("dFdyCoarse"            , kDPdyCoarse               );
        map.put("fwidthCoarse"          , kFwidthCoarse             );

        sIntrinsicMap = map;
    }

    public static int findIntrinsicKind(String name) {
        return sIntrinsicMap.getInt(name);
    }

    private IntrinsicList() {
    }
}
