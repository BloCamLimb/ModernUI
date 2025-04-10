/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite.trash;

import icyllis.arc3d.engine.BlendInfo;
import icyllis.arc3d.engine.KeyBuilder;
import icyllis.arc3d.granite.shading.UniformHandler;
import icyllis.arc3d.granite.shading.XPFragmentBuilder;
import org.jspecify.annotations.NonNull;

import javax.annotation.concurrent.Immutable;

@Deprecated
@Immutable
public abstract class TransferProcessor extends Processor {

    protected final boolean mReadsDstColor;
    protected final boolean mIsLCDCoverage;

    protected TransferProcessor(int classID) {
        this(classID, false, false);
    }

    protected TransferProcessor(int classID, boolean readsDstColor, boolean isLCDCoverage) {
        super(classID);
        mReadsDstColor = readsDstColor;
        mIsLCDCoverage = isLCDCoverage;
    }

    public void addToKey(KeyBuilder b) {

    }

    /**
     * Returns a new instance of the appropriate implementation class
     * for the given TransferProcessor.
     */
    @NonNull
    public abstract ProgramImpl makeProgramImpl();

    // must override by subclass if XP will not read dst color
    @NonNull
    public BlendInfo getBlendInfo() {
        assert readsDstColor();
        return BlendInfo.BLEND_SRC;
    }

    // must override by subclass if XP will not read dst color
    public boolean hasSecondaryOutput() {
        assert readsDstColor();
        return false;
    }

    public final boolean readsDstColor() {
        return mReadsDstColor;
    }

    public final boolean isLCDCoverage() {
        return mIsLCDCoverage;
    }

    /**
     * Every {@link TransferProcessor} must be capable of creating a subclass of ProgramImpl. The
     * ProgramImpl emits the shader code combines determines the fragment shader output(s) from
     * the color and coverage FP outputs, is attached to the generated backend API pipeline/program,
     * and used to extract uniform data from TransferProcessor instances.
     */
    @Deprecated
    public static abstract class ProgramImpl {

        public static final class EmitArgs {

            public XPFragmentBuilder fragBuilder;
            public UniformHandler uniformHandler;
            public TransferProcessor xferProc;

            public String inputColor;
            public String inputCoverage;
            public String outputPrimary;
            public String outputSecondary;

            public EmitArgs(XPFragmentBuilder fragBuilder,
                            UniformHandler uniformHandler,
                            TransferProcessor xferProc,
                            String inputColor,
                            String inputCoverage,
                            String outputPrimary,
                            String outputSecondary) {
                this.fragBuilder = fragBuilder;
                this.uniformHandler = uniformHandler;
                this.xferProc = xferProc;
                this.inputColor = inputColor;
                this.inputCoverage = inputCoverage;
                this.outputPrimary = outputPrimary;
                this.outputSecondary = outputSecondary;
            }
        }

        public final void emitCode(EmitArgs args) {
            if (args.xferProc.readsDstColor()) {

            } else {
                emitOutputsForBlendState(args);
            }
        }

        protected void emitOutputsForBlendState(EmitArgs args) {
            throw new UnsupportedOperationException("emitOutputsForBlendState not implemented");
        }

        protected void emitBlendCodeForDstRead(EmitArgs args) {
            throw new UnsupportedOperationException("emitBlendCodeForDstRead not implemented");
        }
    }
}
