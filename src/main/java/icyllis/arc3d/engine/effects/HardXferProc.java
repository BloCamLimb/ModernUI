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

import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.BlendFormula;
import icyllis.arc3d.granite.shading.XPFragmentBuilder;

import javax.annotation.Nonnull;

public class HardXferProc extends TransferProcessor {

    /**
     * This XP implements non-LCD src-over using HW blend.
     */
    public static final HardXferProc SIMPLE_SRC_OVER = new HardXferProc(
            BlendFormula.OUTPUT_TYPE_MODULATE,
            BlendFormula.OUTPUT_TYPE_ZERO,
            false,
            new BlendInfo(
                    BlendInfo.EQUATION_ADD,
                    BlendInfo.FACTOR_ONE,
                    BlendInfo.FACTOR_ONE_MINUS_SRC_ALPHA,
                    true
            )
    );

    private final BlendInfo mBlendInfo;

    private final int mPrimaryOutputType;
    private final int mSecondaryOutputType;

    public HardXferProc(int primaryOutputType, int secondaryOutputType,
                        boolean isLCDCoverage, BlendInfo blendInfo) {
        super(Hard_XferProc_ClassID, false, isLCDCoverage);
        mPrimaryOutputType = primaryOutputType;
        mSecondaryOutputType = secondaryOutputType;
        mBlendInfo = blendInfo;
    }

    @Nonnull
    @Override
    public String name() {
        return "Hardware Transfer Processor";
    }

    @Nonnull
    @Override
    public ProgramImpl makeProgramImpl() {
        return new Impl();
    }

    @Nonnull
    @Override
    public BlendInfo getBlendInfo() {
        return mBlendInfo;
    }

    @Override
    public boolean hasSecondaryOutput() {
        return mSecondaryOutputType != BlendFormula.OUTPUT_TYPE_ZERO;
    }

    private static class Impl extends ProgramImpl {

        private static void appendOutput(XPFragmentBuilder fragBuilder,
                                         int outputType,
                                         String output,
                                         String inColor,
                                         String inCoverage) {
            switch (outputType) {
                case BlendFormula.OUTPUT_TYPE_ZERO:
                    fragBuilder.codeAppendf("%s = vec4(0.0);", output);
                    break;
                case BlendFormula.OUTPUT_TYPE_COVERAGE:
                    fragBuilder.codeAppendf("%s = %s;", output, inCoverage);
                    break;
                case BlendFormula.OUTPUT_TYPE_MODULATE:
                    fragBuilder.codeAppendf("%s = %s * %s;", output, inColor, inCoverage);
                    break;
                case BlendFormula.OUTPUT_TYPE_SRC_ALPHA_MODULATE:
                    fragBuilder.codeAppendf("%s = %s.a * %s;", output, inColor, inCoverage);
                    break;
                case BlendFormula.OUTPUT_TYPE_ONE_MINUS_SRC_ALPHA_MODULATE:
                    fragBuilder.codeAppendf("%s = (1.0 - %s.a) * %s;", output, inColor, inCoverage);
                    break;
                case BlendFormula.OUTPUT_TYPE_ONE_MINUS_SRC_COLOR_MODULATE:
                    fragBuilder.codeAppendf("%s = (1.0 - %s) * %s;", output, inColor, inCoverage);
                    break;
                default:
                    throw new AssertionError("Unsupported output type.");
            }
        }

        @Override
        protected void emitOutputsForBlendState(EmitArgs args) {
            var xp = (HardXferProc) args.xferProc;
            if (xp.hasSecondaryOutput()) {
                appendOutput(
                        args.fragBuilder,
                        xp.mSecondaryOutputType,
                        args.outputSecondary,
                        args.inputColor,
                        args.inputCoverage
                );
            }
            appendOutput(
                    args.fragBuilder,
                    xp.mPrimaryOutputType,
                    args.outputPrimary,
                    args.inputColor,
                    args.inputCoverage
            );
        }
    }
}
