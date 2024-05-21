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

package icyllis.arc3d.engine.shading;

import icyllis.arc3d.engine.ShaderVar;
import org.intellij.lang.annotations.PrintFormat;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Base class for all shaders builders.
 */
public abstract class ShaderBuilderBase implements ShaderBuilder {

    protected static final int
            DEFINITIONS = 0,
            LAYOUT_QUALIFIERS = 1,
            UNIFORMS = 2,
            INPUTS = 3,
            OUTPUTS = 4,
            FUNCTIONS = 5,
            CODE = 6;
    // Reasonable upper bound on number of processor stages
    protected static final int PREALLOC = CODE + 6;

    protected final GraphicsPipelineBuilder mPipelineBuilder;
    protected final StringBuilder[] mShaderStrings = new StringBuilder[PREALLOC];

    private final HashMap<String, String> mExtensions = new HashMap<>();

    protected int mCodeIndex;

    private Formatter mCodeFormatter;
    private Formatter mCodeFormatterPre;

    private boolean mFinished;

    public ShaderBuilderBase(GraphicsPipelineBuilder pipelineBuilder) {
        mPipelineBuilder = pipelineBuilder;
        for (int i = 0; i <= CODE; i++) {
            mShaderStrings[i] = new StringBuilder();
        }
        definitions().append(pipelineBuilder.shaderCaps().mGLSLVersion.mVersionDecl);
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
    public void codeAppendf(@PrintFormat String format, Object... args) {
        if (mCodeFormatter == null)
            mCodeFormatter = new Formatter(code(), Locale.ROOT);
        mCodeFormatter.format(Locale.ROOT, format, args);
    }

    /**
     * Similar to {@link #codeAppendf(String, Object...)}, but writes at the beginning.
     */
    @Override
    public void codePrependf(@PrintFormat String format, Object... args) {
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
        return mPipelineBuilder.nameVariable('\0', baseName);
    }

    protected final void nextStage() {
        assert !mFinished;
        mShaderStrings[++mCodeIndex] = new StringBuilder();
        mCodeFormatter = null;
        mCodeFormatterPre = null;
    }

    protected final void deleteStage() {
        assert !mFinished;
        mShaderStrings[mCodeIndex--] = null;
        mCodeFormatter = null;
        mCodeFormatterPre = null;
    }

    protected final StringBuilder definitions() {
        assert !mFinished;
        return mShaderStrings[DEFINITIONS];
    }

    protected final StringBuilder layoutQualifiers() {
        assert !mFinished;
        return mShaderStrings[LAYOUT_QUALIFIERS];
    }

    protected final StringBuilder uniforms() {
        assert !mFinished;
        return mShaderStrings[UNIFORMS];
    }

    protected final StringBuilder inputs() {
        assert !mFinished;
        return mShaderStrings[INPUTS];
    }

    protected final StringBuilder outputs() {
        assert !mFinished;
        return mShaderStrings[OUTPUTS];
    }

    protected final StringBuilder functions() {
        assert !mFinished;
        return mShaderStrings[FUNCTIONS];
    }

    protected final StringBuilder code() {
        assert !mFinished;
        return mShaderStrings[mCodeIndex];
    }

    public void addExtension(@Nullable String extensionName) {
        if (extensionName != null) {
            mExtensions.put(extensionName, "require");
        }
    }

    /**
     * Complete this builder and do post-processing before getting the result.
     */
    public final void finish() {
        if (mFinished) {
            return;
        }
        onFinish();
        // append the 'footer' to code
        code().append("}");
        mFinished = true;
    }

    public final CharSequence[] getStrings() {
        return mShaderStrings;
    }

    public final int getCount() {
        return mCodeIndex + 1;
    }

    public final Map<String, String> getExtensions() {
        return mExtensions;
    }

    @Nonnull
    public final ByteBuffer toUTF8() {
        finish();
        int len = 0;
        for (int i = 0; i <= mCodeIndex; i++) {
            StringBuilder shaderString = mShaderStrings[i];
            // we assume ASCII only, so 1 byte per char
            len += shaderString.length();
        }

        ByteBuffer buffer = BufferUtils.createByteBuffer(len);
        len = 0;
        for (int i = 0; i <= mCodeIndex; i++) {
            StringBuilder shaderString = mShaderStrings[i];
            len += MemoryUtil.memUTF8(shaderString, false, buffer, len);
        }
        assert len == buffer.capacity() && len == buffer.remaining();
        return buffer;
    }

    @Override
    public final String toString() {
        finish();
        return Arrays.stream(getStrings(), 0, getCount())
                .filter(s -> !s.isEmpty())
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
