/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.compiler;

import icyllis.arc3d.compiler.spirv.SPIRVCodeGenerator;
import icyllis.arc3d.compiler.tree.Node;
import icyllis.arc3d.compiler.tree.TranslationUnit;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Objects;

/**
 * Main compiler entry point. The compiler parses the source text directly into a tree of
 * {@link Node Nodes}, while performing basic optimizations such as constant-folding and
 * dead-code elimination. Then the {@link TranslationUnit} is passed into a {@link CodeGenerator}
 * to produce compiled output.
 * <p>
 * This class is not thread-safe, you may want a thread-local instance.
 */
public class ShaderCompiler {

    public static final String INVALID_TAG = "<INVALID>";
    public static final String POISON_TAG = "<POISON>"; // error value

    private final StringBuilder mErrorBuilder = new StringBuilder();
    private final ErrorHandler mErrorHandler = new ErrorHandler() {
        private void log(int start, int end, String msg) {
            boolean showLocation = false;
            char[] source = getSource();
            if (start != -1 && source != null) {
                int offset = Math.min(start, source.length);
                int line = 1;
                for (int i = 0; i < offset; ++i) {
                    if (source[i] == '\n') {
                        ++line;
                    }
                }
                showLocation = start < source.length;
                mErrorBuilder.append(line).append(": ");
            }
            mErrorBuilder.append(msg).append('\n');
            if (showLocation) {
                // Find the beginning of the line
                int lineStart = start;
                while (lineStart > 0) {
                    if (source[lineStart - 1] == '\n') {
                        break;
                    }
                    --lineStart;
                }

                // echo the line
                for (int i = lineStart; i < source.length; i++) {
                    switch (source[i]) {
                        case '\t' -> mErrorBuilder.append("    ");
                        case '\0' -> mErrorBuilder.append(" ");
                        case '\n' -> i = source.length;
                        default -> mErrorBuilder.append(source[i]);
                    }
                }
                mErrorBuilder.append('\n');

                // print the carets underneath it, pointing to the range in question
                for (int i = lineStart; i < source.length; i++) {
                    if (i >= end) {
                        break;
                    }
                    switch (source[i]) {
                        case '\t' -> mErrorBuilder.append((i >= start) ? "^^^^" : "    ");
                        case '\n' -> {
                            assert (i >= start);
                            // use an ellipsis if the error continues past the end of the line
                            mErrorBuilder.append((end > i + 1) ? "..." : "^");
                            i = source.length;
                        }
                        default -> mErrorBuilder.append((i >= start) ? '^' : ' ');
                    }
                }
                mErrorBuilder.append('\n');
            }
        }

        @Override
        protected void handleError(int start, int end, String msg) {
            mErrorBuilder.append("error: ");
            log(start, end, msg);
        }

        @Override
        protected void handleWarning(int start, int end, String msg) {
            mErrorBuilder.append("warning: ");
            log(start, end, msg);
        }
    };

    private final Context mContext;
    private final Inliner mInliner;

    public ShaderCompiler() {
        mContext = new Context(mErrorHandler);
        mInliner = new Inliner();
    }

    public Context getContext() {
        if (mContext.isActive()) {
            return mContext;
        }
        throw new IllegalStateException("DSL is not started");
    }

    /**
     * Parse the source into an abstract syntax tree.
     *
     * @param source  the source text, a copy will be created
     * @param kind    the shader kind
     * @param options the compile options
     * @param parent  the parent module that contains common declarations
     * @return the parsed result, or null if there's an error
     */
    @Nullable
    public TranslationUnit parse(@Nonnull CharSequence source,
                                 @Nonnull ShaderKind kind,
                                 @Nonnull CompileOptions options,
                                 @Nonnull ModuleUnit parent) {
        return parse(toChars(source), kind, options, parent);
    }

    /**
     * Parse the source into an abstract syntax tree.
     *
     * @param source  the source text, will be passed to the returned TranslationUnit
     * @param kind    the shader kind
     * @param options the compile options
     * @param parent  the parent module that contains common declarations
     * @return the parsed result, or null if there's an error
     */
    @Nullable
    public TranslationUnit parse(@Nonnull char[] source,
                                 @Nonnull ShaderKind kind,
                                 @Nonnull CompileOptions options,
                                 @Nonnull ModuleUnit parent) {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(parent);
        startContext(kind, options, parent, false, false, source);
        try {
            Parser parser = new Parser(this, kind,
                    options,
                    source);
            return parser.parse(parent);
        } finally {
            endContext();
        }
    }

    /**
     * Parse the source into an abstract syntax tree.
     * <p>
     * A module unit is a pre-compiled result that contains pre-declared variables
     * and optimized functions, used to compile multiple files.
     *
     * @param source the source text
     * @param kind   the shader kind
     * @param parent the parent module inherited by the new module
     * @return the parsed result, or null if there's an error
     */
    @Nullable
    public ModuleUnit parseModule(@Nonnull CharSequence source,
                                  @Nonnull ShaderKind kind,
                                  @Nonnull ModuleUnit parent,
                                  boolean builtin) {
        return parseModule(toChars(source), kind, parent, builtin);
    }

    /**
     * Parse the source into an abstract syntax tree.
     * <p>
     * A module unit is a pre-compiled result that contains pre-declared variables
     * and optimized functions, used to compile multiple files.
     *
     * @param source the source text
     * @param kind   the shader kind
     * @param parent the parent module inherited by the new module
     * @return the parsed result, or null if there's an error
     */
    @Nullable
    public ModuleUnit parseModule(@Nonnull char[] source,
                                  @Nonnull ShaderKind kind,
                                  @Nonnull ModuleUnit parent,
                                  boolean builtin) {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(parent);
        CompileOptions options = new CompileOptions();
        startContext(kind, options, parent, builtin, true, source);
        try {
            Parser parser = new Parser(this, kind,
                    options,
                    source);
            return parser.parseModule(parent);
        } finally {
            endContext();
        }
    }

    /**
     * Generates translated SPIR-V code and returns the pointer result. The code size
     * in bytes is {@link ByteBuffer#remaining()}.
     * <p>
     * The return value is a direct buffer, see {@link ByteBuffer#allocateDirect(int)}.
     * A direct buffer wraps an address that points to off-heap memory, i.e. a native
     * pointer. The byte order is {@link java.nio.ByteOrder#nativeOrder()} (i.e. host
     * endianness) and it's safe to pass the result to OpenGL and Vulkan API. There is
     * no way to free this buffer explicitly, as it is subject to GC.
     * <p>
     * Check errors via {@link #getErrorMessage()}.
     *
     * @return the translated shader code (uint32_t *), or null if there's an error
     */
    @Nullable
    public ByteBuffer generateSPIRV(@Nonnull TranslationUnit translationUnit,
                                    @Nonnull ShaderCaps shaderCaps) {
        startContext(translationUnit.getKind(),
                translationUnit.getOptions(),
                null,
                false,
                false,
                translationUnit.getSource());
        try {
            CodeGenerator generator = new SPIRVCodeGenerator(
                    this, translationUnit, shaderCaps);
            return generator.generateCode();
        } finally {
            endContext();
        }
    }

    /**
     * Combination of {@link #parse} and {@link #generateSPIRV}.
     *
     * @see #parse(CharSequence, ShaderKind, CompileOptions, ModuleUnit)
     * @see #generateSPIRV(TranslationUnit, ShaderCaps)
     */
    @Nullable
    public ByteBuffer compileIntoSPIRV(@Nonnull CharSequence source,
                                       @Nonnull ShaderKind kind,
                                       @Nonnull ShaderCaps shaderCaps,
                                       @Nonnull CompileOptions options,
                                       @Nonnull ModuleUnit parent) {
        return compileIntoSPIRV(toChars(source), kind, shaderCaps, options, parent);
    }

    /**
     * Combination of {@link #parse} and {@link #generateSPIRV}.
     *
     * @see #parse(char[], ShaderKind, CompileOptions, ModuleUnit)
     * @see #generateSPIRV(TranslationUnit, ShaderCaps)
     */
    @Nullable
    public ByteBuffer compileIntoSPIRV(@Nonnull char[] source,
                                       @Nonnull ShaderKind kind,
                                       @Nonnull ShaderCaps shaderCaps,
                                       @Nonnull CompileOptions options,
                                       @Nonnull ModuleUnit parent) {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(parent);
        startContext(kind, options, parent, false, false, source);
        try {
            Parser parser = new Parser(this, kind,
                    options,
                    source);
            TranslationUnit translationUnit = parser.parse(parent);
            if (translationUnit == null) {
                return null;
            }
            CodeGenerator generator = new SPIRVCodeGenerator(
                    this, translationUnit, shaderCaps);
            return generator.generateCode();
        } finally {
            endContext();
        }
    }

    @ApiStatus.Internal
    public void startContext(ShaderKind kind,
                             CompileOptions options,
                             ModuleUnit parent,
                             boolean isBuiltin,
                             boolean isModule,
                             char[] source) {
        assert isModule || !isBuiltin;
        resetErrors(); // make a clean start
        mContext.start(kind, options, parent, isBuiltin, isModule);
        mContext.getErrorHandler().setSource(source);
    }

    @ApiStatus.Internal
    public void endContext() {
        mContext.end();
        mContext.getErrorHandler().setSource(null);
    }

    @ApiStatus.Internal
    @Nonnull
    public static char[] toChars(@Nonnull CharSequence s) {
        if (s instanceof String) {
            return ((String) s).toCharArray();
        }
        int n = s.length();
        char[] chars = new char[n];
        if (s instanceof StringBuffer)
            ((StringBuffer) s).getChars(0, n, chars, 0);
        else if (s instanceof StringBuilder)
            ((StringBuilder) s).getChars(0, n, chars, 0);
        else if (s instanceof CharBuffer buf)
            buf.get(buf.position(), chars, 0, n);
        else {
            for (int i = 0; i < n; i++)
                chars[i] = s.charAt(i);
        }
        return chars;
    }

    /**
     * Returns the concatenated error (and warning) message during the last parsing
     * or code generation. This may be empty or contain multiple lines.
     */
    @Nonnull
    public String getErrorMessage() {
        return getErrorMessage(true);
    }

    /**
     * Returns the concatenated error (and warning) message during the last parsing
     * or code generation. This may be empty or contain multiple lines.
     *
     * @param showCount show the number of errors and warnings, if there are any
     */
    @Nonnull
    public String getErrorMessage(boolean showCount) {
        if (!showCount) {
            return mErrorBuilder.toString();
        }
        int errors = errorCount();
        int warnings = warningCount();
        if (errors == 0 && warnings == 0) {
            assert mErrorBuilder.isEmpty();
            return "";
        }
        int start = mErrorBuilder.length();
        mErrorBuilder.append(errors).append(" error");
        if (errors != 1) {
            mErrorBuilder.append('s');
        }
        mErrorBuilder.append(", ").append(warnings).append(" warning");
        if (warnings != 1) {
            mErrorBuilder.append('s');
        }
        mErrorBuilder.append('\n');
        String msg = mErrorBuilder.toString();
        mErrorBuilder.delete(start, mErrorBuilder.length());
        return msg;
    }

    /**
     * Returns the Compiler's error handler.
     */
    public ErrorHandler getErrorHandler() {
        return mErrorHandler;
    }

    /**
     * Returns the number of errors during the last parsing or code generation.
     */
    public int errorCount() {
        return mErrorHandler.errorCount();
    }

    /**
     * Returns the number of warnings during the last parsing or code generation.
     */
    public int warningCount() {
        return mErrorHandler.warningCount();
    }

    private void resetErrors() {
        boolean trim = mErrorBuilder.length() > 8192;
        mErrorBuilder.setLength(0);
        if (trim) {
            mErrorBuilder.trimToSize();
        }
        mErrorHandler.reset();
    }
}
