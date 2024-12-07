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

import icyllis.arc3d.compiler.spirv.SPIRVCodeGenerator;
import icyllis.arc3d.compiler.tree.Node;
import org.jetbrains.annotations.ApiStatus;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.*;

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
            char[] source = mSource;
            int sourceStart = mOffset;
            int sourceLimit = mOffset + mLength;
            if (start != -1 && source != null) {
                start += sourceStart;
                end += sourceStart;
                int offset = Math.min(start, sourceLimit);
                int line = 1;
                for (int i = sourceStart; i < offset; ++i) {
                    boolean isCR = source[i] == '\r';
                    if (isCR || source[i] == '\n') {
                        ++line;
                        if (isCR && i + 1 < offset &&
                                source[i + 1] == '\n') {
                            ++i;
                        }
                    }
                }
                showLocation = start < sourceLimit;
                mErrorBuilder.append(line).append(": ");
            }
            mErrorBuilder.append(msg).append('\n');
            if (showLocation) {
                // Find the beginning of the line
                int lineStart = start;
                while (lineStart > sourceStart && start - lineStart < 128) {
                    if (source[lineStart - 1] == '\n') {
                        break;
                    }
                    --lineStart;
                }

                // echo the line
                for (int i = lineStart; i < sourceLimit; i++) {
                    switch (source[i]) {
                        case '\t' -> mErrorBuilder.append("    ");
                        case '\0' -> mErrorBuilder.append(" ");
                        case '\n' -> i = sourceLimit;
                        default -> mErrorBuilder.append(source[i]);
                    }
                }
                mErrorBuilder.append('\n');

                // print the carets underneath it, pointing to the range in question
                for (int i = lineStart; i < sourceLimit; i++) {
                    if (i >= end) {
                        break;
                    }
                    switch (source[i]) {
                        case '\t' -> mErrorBuilder.append((i >= start) ? "^^^^" : "    ");
                        case '\n' -> {
                            assert (i >= start);
                            // use an ellipsis if the error continues past the end of the line
                            mErrorBuilder.append((end > i + 1) ? "..." : "^");
                            i = sourceLimit;
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
     * If the source is a {@link CharBuffer} and backed by a heap array, then
     * the array is used directly. Otherwise, a copy will be made.
     * Typically, if you want to compile a UTF-8 encoded file, you map the file
     * and call {@link java.nio.charset.CharsetDecoder#decode(ByteBuffer)} to
     * obtain the CharBuffer, and directly pass it to here. No copy will be made.
     *
     * @param source  the source text
     * @param kind    the shader kind
     * @param options the compile options
     * @param parent  the parent module that contains common declarations
     * @return the parsed result, or null if there's an error
     */
    @Nullable
    public TranslationUnit parse(@NonNull CharSequence source,
                                 @NonNull ShaderKind kind,
                                 @NonNull CompileOptions options,
                                 @NonNull ModuleUnit parent) {
        final char[] chars;
        final int offset, length;
        if (source instanceof CharBuffer buffer &&
                buffer.hasArray()) {
            chars = buffer.array();
            offset = buffer.arrayOffset() + buffer.position();
            length = buffer.remaining();
        } else {
            chars = toChars(source);
            offset = 0;
            length = chars.length;
        }
        return parse(chars, offset, length, kind, options, parent);
    }

    /**
     * Parse the source into an abstract syntax tree.
     *
     * @param source  the source text, must be immutable
     * @param offset  the offset to the first char as the first line
     * @param length  the effective number of chars of the source
     * @param kind    the shader kind
     * @param options the compile options
     * @param parent  the parent module that contains common declarations
     * @return the parsed result, or null if there's an error
     */
    @Nullable
    public TranslationUnit parse(char @NonNull[] source,
                                 int offset, int length,
                                 @NonNull ShaderKind kind,
                                 @NonNull CompileOptions options,
                                 @NonNull ModuleUnit parent) {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(parent);
        Objects.checkFromIndexSize(offset, length, source.length);
        startContext(kind, options, parent, false, false, source, offset, length);
        try {
            Parser parser = new Parser(this, kind,
                    options,
                    source,
                    offset,
                    length);
            if (options.mPreprocess) {
                List<Map.Entry<String, Boolean>> includes = parser.preprocess();
                if (includes == null) {
                    return null;
                }
                //TODO
            }
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
     * @param source the source text, a copy will be created
     * @param kind   the shader kind
     * @param parent the parent module inherited by the new module
     * @return the parsed result, or null if there's an error
     */
    @Nullable
    public ModuleUnit parseModule(@NonNull CharSequence source,
                                  @NonNull ShaderKind kind,
                                  @NonNull ModuleUnit parent,
                                  boolean builtin) {
        final char[] chars;
        final int offset, length;
        if (source instanceof CharBuffer buffer &&
                buffer.hasArray()) {
            chars = buffer.array();
            offset = buffer.arrayOffset() + buffer.position();
            length = buffer.remaining();
        } else {
            chars = toChars(source);
            offset = 0;
            length = chars.length;
        }
        return parseModule(chars, offset, length, kind, parent, builtin);
    }

    /**
     * Parse the source into an abstract syntax tree.
     * <p>
     * A module unit is a pre-compiled result that contains pre-declared variables
     * and optimized functions, used to compile multiple files.
     *
     * @param source the source text, must be immutable
     * @param kind   the shader kind
     * @param parent the parent module inherited by the new module
     * @return the parsed result, or null if there's an error
     */
    @Nullable
    public ModuleUnit parseModule(char @NonNull[] source,
                                  int offset, int length,
                                  @NonNull ShaderKind kind,
                                  @NonNull ModuleUnit parent,
                                  boolean builtin) {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(parent);
        Objects.checkFromIndexSize(offset, length, source.length);
        CompileOptions options = new CompileOptions();
        startContext(kind, options, parent, builtin, true, source, offset, length);
        try {
            Parser parser = new Parser(this, kind,
                    options,
                    source,
                    offset,
                    length);
            List<Map.Entry<String, Boolean>> includes = parser.preprocess();
            if (includes == null) {
                return null;
            }
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
    public ByteBuffer generateSPIRV(@NonNull TranslationUnit translationUnit,
                                    @NonNull ShaderCaps shaderCaps) {
        startContext(translationUnit.getKind(),
                translationUnit.getOptions(),
                null,
                false,
                false,
                translationUnit.getSource(),
                translationUnit.getSourceOffset(),
                translationUnit.getSourceLength());
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
     * <p>
     * Use this method if you don't need parsed IR, then this method can do some
     * optimizations.
     *
     * @see #parse(CharSequence, ShaderKind, CompileOptions, ModuleUnit)
     * @see #generateSPIRV(TranslationUnit, ShaderCaps)
     */
    @Nullable
    public ByteBuffer compileIntoSPIRV(@NonNull CharSequence source,
                                       @NonNull ShaderKind kind,
                                       @NonNull ShaderCaps shaderCaps,
                                       @NonNull CompileOptions options,
                                       @NonNull ModuleUnit parent) {
        final char[] chars;
        final int offset, length;
        if (source instanceof CharBuffer buffer &&
                buffer.hasArray()) {
            chars = buffer.array();
            offset = buffer.arrayOffset() + buffer.position();
            length = buffer.remaining();
        } else {
            chars = toChars(source);
            offset = 0;
            length = chars.length;
        }
        return compileIntoSPIRV(chars, offset, length, kind, shaderCaps, options, parent);
    }

    /**
     * Combination of {@link #parse} and {@link #generateSPIRV}.
     * <p>
     * Use this method if you don't need parsed IR, then this method can do some
     * optimizations.
     *
     * @see #parse(char[], int, int, ShaderKind, CompileOptions, ModuleUnit)
     * @see #generateSPIRV(TranslationUnit, ShaderCaps)
     */
    @Nullable
    public ByteBuffer compileIntoSPIRV(char @NonNull[] source,
                                       int offset, int length,
                                       @NonNull ShaderKind kind,
                                       @NonNull ShaderCaps shaderCaps,
                                       @NonNull CompileOptions options,
                                       @NonNull ModuleUnit parent) {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(parent);
        Objects.checkFromIndexSize(offset, length, source.length);
        startContext(kind, options, parent, false, false, source, offset, length);
        try {
            Parser parser = new Parser(this, kind,
                    options,
                    source,
                    offset,
                    length);
            if (options.mPreprocess) {
                List<Map.Entry<String, Boolean>> includes = parser.preprocess();
                if (includes == null) {
                    return null;
                }
                //TODO
            }
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
                             char[] source,
                             int offset,
                             int length) {
        assert isModule || !isBuiltin;
        resetErrors(); // make a clean start
        mContext.start(kind, options, parent, isBuiltin, isModule);
        mContext.getErrorHandler().setSource(source, offset, length);
    }

    @ApiStatus.Internal
    public void endContext() {
        mContext.end();
        mContext.getErrorHandler().setSource(null, 0, 0);
    }


    /**
     * Helper method to copy a char sequence.
     *
     * @return a new char buffer copied from the given element
     */
    public static char @NonNull[] toChars(@NonNull CharSequence s) {
        if (s instanceof String) {
            return ((String) s).toCharArray();
        }
        int n = s.length();
        char[] chars = new char[n];
        getChars(s, chars, 0, n);
        return chars;
    }


    /**
     * Helper method to copy a char sequence array. Character sequences will
     * be concatenated together.
     *
     * @return a new char buffer copied from the given elements
     */
    public static char @NonNull[] toChars(@NonNull CharSequence... elements) {
        return toChars(elements, 0, elements.length);
    }


    /**
     * Helper method to copy a sub-range of char sequences. Character sequences will
     * be concatenated together. Empty sequences will be ignored.
     *
     * @param start start index (inclusive) in elements
     * @param end   end index (exclusive) in elements
     * @return a new char buffer copied from the given elements
     */
    public static char @NonNull[] toChars(CharSequence @NonNull[] elements, int start, int end) {
        Objects.checkFromToIndex(start, end, elements.length);
        if (start == end) {
            return new char[0];
        }
        if (start + 1 == end) {
            return toChars(elements[start]);
        }
        int n = 0;
        for (int i = start; i < end; i++) {
            int len = elements[i].length();
            n += len;
        }
        if (n == 0) {
            return new char[0];
        }
        char[] chars = new char[n];
        int p = 0;
        for (int i = start; i < end; i++) {
            CharSequence s = elements[i];
            int len = s.length();
            if (len == 0) continue;
            p += getChars(s, chars, p, len);
        }
        assert p == n;
        return chars;
    }


    /**
     * Helper method to copy a sequence of char sequences. Character sequences will
     * be concatenated together. Empty sequences will be ignored.
     *
     * @return a new char buffer copied from the given elements
     */
    public static char @NonNull[] toChars(@NonNull List<CharSequence> elements) {
        int size = elements.size();
        if (size == 0) {
            return new char[0];
        }
        if (size == 1) {
            return toChars(elements.get(0));
        }
        int n = 0;
        for (int i = 0; i < size; i++) {
            int len = elements.get(i).length();
            n += len;
        }
        if (n == 0) {
            return new char[0];
        }
        char[] chars = new char[n];
        int p = 0;
        for (int i = 0; i < size; i++) {
            CharSequence s = elements.get(i);
            int len = s.length();
            if (len == 0) continue;
            p += getChars(s, chars, p, len);
        }
        assert p == n;
        return chars;
    }

    private static int getChars(@NonNull CharSequence s,
            char @NonNull[] dst, int offset, int n) {
        if (s instanceof String)
            ((String) s).getChars(0, n, dst, offset);
        else if (s instanceof StringBuffer)
            ((StringBuffer) s).getChars(0, n, dst, offset);
        else if (s instanceof StringBuilder)
            ((StringBuilder) s).getChars(0, n, dst, offset);
        else if (s instanceof CharBuffer buf)
            buf.get(buf.position(), dst, offset, n);
        else {
            for (int i = 0; i < n; i++)
                dst[offset++] = s.charAt(i);
        }
        return n;
    }

    /**
     * Returns the concatenated error (and warning) message during the last parsing
     * or code generation. This may be empty or contain multiple lines.
     */
    @NonNull
    public String getErrorMessage() {
        return getErrorMessage(true);
    }

    /**
     * Returns the concatenated error (and warning) message during the last parsing
     * or code generation. This may be empty or contain multiple lines.
     *
     * @param showCount show the number of errors and warnings, if there are any
     */
    @NonNull
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
