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

import icyllis.arcui.engine.ShaderVar;

import java.util.Formatter;
import java.util.Locale;

/**
 * Base class for all shaders builders.
 */
public abstract class ShaderBuilderBase implements ShaderBuilder {

    protected static final int
            EXTENSIONS = 0,
            DEFINITIONS = 1,
            PRECISION_QUALIFIER = 2,
            LAYOUT_QUALIFIERS = 3,
            UNIFORMS = 4,
            INPUTS = 5,
            OUTPUTS = 6,
            FUNCTIONS = 7,
            MAIN = 8,
            CODE = 9;
    // Reasonable upper bound on number of processor stages
    protected static final int PREALLOC = CODE + 7;

    protected final ProgramBuilder mProgramBuilder;
    protected final StringBuilder[] mShaderStrings = new StringBuilder[PREALLOC];
    protected String mCompilerString;

    protected int mCodeIndex;

    private Formatter mCodeFormatter;
    private Formatter mCodeFormatterPre;

    public ShaderBuilderBase(ProgramBuilder programBuilder) {
        mProgramBuilder = programBuilder;
        for (int i = 0; i <= CODE; i++) {
            mShaderStrings[i] = new StringBuilder();
        }
        mCodeIndex = CODE;
    }

    public void codeAppend(String str) {
        code().append(str);
    }

    /**
     * Writes a formatted string to one of the shaders using the specified format
     * string and arguments.
     */
    public void codeAppendf(String format, Object... args) {
        if (mCodeFormatter == null)
            mCodeFormatter = new Formatter(code(), Locale.ROOT);
        mCodeFormatter.format(Locale.ROOT, format, args);
    }

    /**
     * Similar to {@link #codeAppendf(String, Object...)}, but writes at the beginning.
     */
    public void codePrependf(String format, Object... args) {
        if (mCodeFormatterPre == null)
            mCodeFormatterPre = new Formatter(new Prependable(code()), Locale.ROOT);
        mCodeFormatterPre.format(Locale.ROOT, format, args);
    }

    /**
     * Appends a variable declaration to one of the shaders
     */
    public void declAppend(ShaderVar var) {
        var.appendDecl(code());
        codeAppend(";");
    }

    protected final void nextStage() {
        mShaderStrings[++mCodeIndex] = new StringBuilder();
        mCodeFormatter = null;
        mCodeFormatterPre = null;
    }

    protected final void deleteStage() {
        mShaderStrings[mCodeIndex--] = null;
        mCodeFormatter = null;
        mCodeFormatterPre = null;
    }

    protected final StringBuilder extensions() {
        return mShaderStrings[EXTENSIONS];
    }

    protected final StringBuilder definitions() {
        return mShaderStrings[DEFINITIONS];
    }

    protected final StringBuilder precisionQualifier() {
        return mShaderStrings[PRECISION_QUALIFIER];
    }

    protected final StringBuilder layoutQualifiers() {
        return mShaderStrings[LAYOUT_QUALIFIERS];
    }

    protected final StringBuilder uniforms() {
        return mShaderStrings[UNIFORMS];
    }

    protected final StringBuilder inputs() {
        return mShaderStrings[INPUTS];
    }

    protected final StringBuilder outputs() {
        return mShaderStrings[OUTPUTS];
    }

    protected final StringBuilder functions() {
        return mShaderStrings[FUNCTIONS];
    }

    protected final StringBuilder main() {
        return mShaderStrings[MAIN];
    }

    protected final StringBuilder code() {
        return mShaderStrings[mCodeIndex];
    }

    private static class Prependable implements Appendable {

        private final StringBuilder mBuilder;

        public Prependable(StringBuilder builder) {
            mBuilder = builder;
        }

        @Override
        public Appendable append(CharSequence csq) {
            mBuilder.insert(0, csq);
            return this;
        }

        @Override
        public Appendable append(CharSequence csq, int start, int end) {
            mBuilder.insert(0, csq, start, end);
            return this;
        }

        @Override
        public Appendable append(char c) {
            mBuilder.insert(0, c);
            return this;
        }
    }
}
