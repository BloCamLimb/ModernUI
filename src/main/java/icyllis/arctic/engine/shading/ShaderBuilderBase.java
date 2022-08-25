/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arctic.engine.shading;

import icyllis.arctic.engine.ShaderVar;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Base class for all shaders builders.
 */
public abstract class ShaderBuilderBase implements ShaderBuilder {

    protected static final int
            EXTENSIONS = 0,
            DEFINITIONS = 1,
            LAYOUT_QUALIFIERS = 2,
            UNIFORMS = 3,
            INPUTS = 4,
            OUTPUTS = 5,
            FUNCTIONS = 6,
            CODE = 7;
    // Reasonable upper bound on number of processor stages
    protected static final int PREALLOC = CODE + 7;

    protected final ProgramBuilder mProgramBuilder;
    protected final StringBuilder[] mShaderStrings = new StringBuilder[PREALLOC];

    protected int mCodeIndex;

    private Formatter mCodeFormatter;
    private Formatter mCodeFormatterPre;

    public ShaderBuilderBase(ProgramBuilder programBuilder) {
        mProgramBuilder = programBuilder;
        for (int i = 0; i <= CODE; i++) {
            mShaderStrings[i] = new StringBuilder();
        }
        mCodeIndex = CODE;
        codeAppend("void main() {\n");
    }

    /**
     * Writes the specified string to one of the shaders.
     */
    @Override
    public void codeAppend(String str) {
        code().append(str);
    }

    /**
     * Writes a formatted string to one of the shaders using the specified format
     * string and arguments.
     */
    @Override
    public void codeAppendf(String format, Object... args) {
        if (mCodeFormatter == null)
            mCodeFormatter = new Formatter(code(), Locale.ROOT);
        mCodeFormatter.format(Locale.ROOT, format, args);
    }

    /**
     * Similar to {@link #codeAppendf(String, Object...)}, but writes at the beginning.
     */
    @Override
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
        codeAppend(";\n");
    }

    @Override
    public String getMangledName(String baseName) {
        return mProgramBuilder.nameVariable('\0', baseName);
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

    protected final StringBuilder code() {
        return mShaderStrings[mCodeIndex];
    }

    public final String finish() {
        extensions().append(mProgramBuilder.shaderCaps().mVersionDeclString);
        onFinish();
        // append the 'footer' to code
        code().append("}");

        return Arrays.stream(mShaderStrings, 0, mCodeIndex + 1)
                .filter(s -> s.length() > 0)
                .collect(Collectors.joining("\n"));
    }

    protected abstract void onFinish();

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
