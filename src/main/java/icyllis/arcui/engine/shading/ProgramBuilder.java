/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.engine.shading;

import icyllis.arcui.engine.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import javax.annotation.Nonnull;

public abstract class ProgramBuilder {

    /**
     * Each root processor has an stage index. The GP is stage 0. The first root FP is stage 1,
     * the second root FP is stage 2, etc. The XP's stage index is last and its value depends on
     * how many root FPs there are. Names are mangled by appending _S<stage-index>.
     */
    private int mStageIndex = -1;

    /**
     * When emitting FP stages we track the children FPs as "substages" and do additional name
     * mangling based on where in the FP hierarchy we are. The first FP is stage index 1. It's first
     * child would be substage 0 of stage 1. If that FP also has three children then its third child
     * would be substage 2 of stubstage 0 of stage 1 and would be mangled as "_S1_c0_c2".
     */
    private final IntArrayList mSubstageIndices = new IntArrayList();

    public final VertexShaderBuilder mVS = new VertexShaderBuilder(this);
    public final FragmentShaderBuilder mFS = new FragmentShaderBuilder(this);

    public ProgramBuilder(ProgramDesc desc, ProgramInfo programInfo) {
    }

    public abstract Caps caps();

    public final ShaderCaps shaderCaps() {
        return caps().shaderCaps();
    }

    public final String nameVariable(char prefix, String name) {
        return nameVariable(prefix, name, true);
    }

    /**
     * Generates a name for a variable. The generated string will be name-prefixed by the prefix
     * char (unless the prefix is '\0'). It also will mangle the name to be stage-specific unless
     * explicitly asked not to. `nameVariable` can also be used to generate names for functions or
     * other types of symbols where unique names are important.
     */
    public final String nameVariable(char prefix, String name, boolean mangle) {
        String out;
        if (prefix == '\0') {
            out = name;
        } else {
            // Names containing "__" are reserved; add "x" if needed to avoid consecutive underscores.
            if (name.startsWith("_")) {
                out = prefix + "_x" + name;
            } else {
                out = prefix + "_" + name;
            }
        }
        if (mangle) {
            String suffix = getMangleSuffix();
            // Names containing "__" are reserved; add "x" if needed to avoid consecutive underscores.
            if (out.endsWith("_")) {
                out += "x" + suffix;
            } else {
                out += suffix;
            }
        }
        assert !out.contains("__");
        return out;
    }

    @Nonnull
    private String getMangleSuffix() {
        assert mStageIndex >= 0;
        StringBuilder suffix = new StringBuilder("_S" + mStageIndex);
        for (var c : mSubstageIndices) {
            suffix.append("_c").append(c);
        }
        return suffix.toString();
    }
}
