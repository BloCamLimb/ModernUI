/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.slang;

import icyllis.akashigi.slang.codegen.CodeGenerator;
import icyllis.akashigi.slang.ir.Node;
import icyllis.akashigi.slang.ir.Program;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Main compiler entry point. The compiler parses the source text directly into a tree of
 * {@link Node Nodes}, while performing basic optimizations such as constant-folding and
 * dead-code elimination. Then the {@link Program} is passed into a {@link CodeGenerator}
 * to produce compiled output.
 */
public class Compiler {

    public static final String INVALID_TAG = "<INVALID>";
    public static final String POISON_TAG = "<POISON>"; // bad value

    private final CompilerErrorHandler mErrorHandler = new CompilerErrorHandler();

    final Inliner mInliner;

    private final StringBuilder mErrorText = new StringBuilder();

    public Compiler() {
        mInliner = new Inliner();
    }

    /**
     * Parse the source code into an abstract syntax tree of the final program.
     *
     * @param kind    the source type
     * @param options the compiler options
     * @param source  the source code of the module to be parsed
     * @param parent  the parent module of the module to be parsed
     * @return the program, or null if there's an error
     */
    @Nullable
    public Program parse(ModuleKind kind,
                         ModuleOptions options,
                         String source,
                         Module parent) {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(parent);
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("Source code is empty");
        }
        resetErrors();
        options = Objects.requireNonNullElseGet(options, ModuleOptions::new);
        Parser parser = new Parser(this, kind, options, source);
        return parser.parse(parent);
    }

    /**
     * Parse the source code into an abstract syntax tree to further parse
     * other modules or final programs.
     *
     * @param kind   the source type
     * @param source the source code of the module to be parsed
     * @param parent the parent module of the module to be parsed
     * @return the module, or null if there's an error
     */
    @Nullable
    public Module parseModule(ModuleKind kind,
                              String source,
                              Module parent) {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(parent);
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("Source code is empty");
        }
        resetErrors();
        ModuleOptions options = new ModuleOptions();
        Parser parser = new Parser(this, kind, options, source);
        return parser.parseModule(parent);
    }

    /**
     * Returns the concatenated error text and clears the buffer.
     *
     * @param showCount whether to write the number of errors
     */
    @Nonnull
    public String getErrorText(boolean showCount) {
        if (showCount) {
            int count = getErrorCount();
            if (count != 0) {
                mErrorText.append(count).append(" error");
                if (count > 1) {
                    mErrorText.append('s');
                }
                mErrorText.append('\n');
            }
        }
        String result = mErrorText.toString();
        resetErrors();
        return result;
    }

    /**
     * Returns the Compiler's error reporter.
     */
    public ErrorHandler getErrorHandler() {
        return mErrorHandler;
    }

    /**
     * Returns the number of errors. Shortcut for getErrorHandler().getErrorCount().
     */
    public int getErrorCount() {
        return mErrorHandler.getErrorCount();
    }

    private void resetErrors() {
        mErrorText.setLength(0); // reset but do not trim the internal array
        mErrorHandler.resetErrorCount();
    }

    private class CompilerErrorHandler extends ErrorHandler {

        @Override
        protected void handleError(int start, int end, String msg) {
            assert (start <= end);
            assert (start <= 0xFFFFFF);

            mErrorText.append("error: ");
            boolean showLocation = false;
            String src = getSource();
            assert src != null;
            if (start != 0xFFFFFF) {
                // we allow the offset to equal the length, because that's where TK_END_OF_FILE is reported
                int offset = Math.min(start, src.length());
                int line = 1;
                for (int i = 0; i < offset; i++) {
                    if (src.charAt(i) == '\n') {
                        line++;
                    }
                }
                showLocation = start < src.length();
                mErrorText.append(line).append(": ");
            }
            mErrorText.append(msg).append('\n');
            if (showLocation) {
                // Find the beginning of the line
                int lineStart = start;
                while (lineStart > 0) {
                    if (src.charAt(lineStart - 1) == '\n') {
                        break;
                    }
                    --lineStart;
                }

                // echo the line
                for (int i = lineStart; i < src.length(); i++) {
                    switch (src.charAt(i)) {
                        case '\t' -> mErrorText.append("    ");
                        case '\0' -> mErrorText.append(" ");
                        case '\n' -> i = src.length();
                        default -> mErrorText.append(src.charAt(i));
                    }
                }
                mErrorText.append('\n');

                // print the carets underneath it, pointing to the range in question
                for (int i = lineStart; i < src.length(); i++) {
                    if (i >= end) {
                        break;
                    }
                    switch (src.charAt(i)) {
                        case '\t' -> mErrorText.append((i >= start) ? "^^^^" : "    ");
                        case '\n' -> {
                            assert (i >= start);
                            // use an ellipsis if the error continues past the end of the line
                            mErrorText.append((end > i + 1) ? "..." : "^");
                            i = src.length();
                        }
                        default -> mErrorText.append((i >= start) ? '^' : ' ');
                    }
                }
                mErrorText.append('\n');
            }
        }
    }
}
