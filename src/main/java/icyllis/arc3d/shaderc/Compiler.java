/*
 * Arc 3D.
 * Copyright (C) 2022-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.shaderc;

import icyllis.arc3d.shaderc.codegen.CodeGenerator;
import icyllis.arc3d.shaderc.tree.Node;
import icyllis.arc3d.shaderc.tree.Program;

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
    public static final String POISON_TAG = "<POISON>"; // error value

    private final StringBuilder mLogBuilder = new StringBuilder();
    private final ErrorHandler mErrorHandler = new ErrorHandler() {
        @Override
        protected void handleError(int start, int end, String msg) {
            mLogBuilder.append("error: ");
            boolean showLocation = false;
            String source = getSource();
            if (start != -1) {
                int offset = Math.min(start, source.length());
                int line = 1;
                for (int i = 0; i < offset; ++i) {
                    if (source.charAt(i) == '\n') {
                        ++line;
                    }
                }
                showLocation = start < source.length();
                mLogBuilder.append(line).append(": ");
            }
            mLogBuilder.append(msg).append('\n');
            if (showLocation) {
                // Find the beginning of the line
                int lineStart = start;
                while (lineStart > 0) {
                    if (source.charAt(lineStart - 1) == '\n') {
                        break;
                    }
                    --lineStart;
                }

                // echo the line
                for (int i = lineStart; i < source.length(); i++) {
                    switch (source.charAt(i)) {
                        case '\t' -> mLogBuilder.append("    ");
                        case '\0' -> mLogBuilder.append(" ");
                        case '\n' -> i = source.length();
                        default -> mLogBuilder.append(source.charAt(i));
                    }
                }
                mLogBuilder.append('\n');

                // print the carets underneath it, pointing to the range in question
                for (int i = lineStart; i < source.length(); i++) {
                    if (i >= end) {
                        break;
                    }
                    switch (source.charAt(i)) {
                        case '\t' -> mLogBuilder.append((i >= start) ? "^^^^" : "    ");
                        case '\n' -> {
                            assert (i >= start);
                            // use an ellipsis if the error continues past the end of the line
                            mLogBuilder.append((end > i + 1) ? "..." : "^");
                            i = source.length();
                        }
                        default -> mLogBuilder.append((i >= start) ? '^' : ' ');
                    }
                }
                mLogBuilder.append('\n');
            }
        }
    };

    final Inliner mInliner;

    public Compiler() {
        mInliner = new Inliner();
    }

    /**
     * Parse the source into an abstract syntax tree.
     *
     * @param kind    the language
     * @param options the compiler options
     * @param source  the source code of the program to be parsed
     * @param parent  the parent module of the program to be parsed
     * @return the program, or null if there's an error
     */
    @Nullable
    public Program parse(ModuleKind kind,
                         ModuleOptions options,
                         String source,
                         Module parent) {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(parent);
        Objects.requireNonNull(source);
        resetLog(); // make a clean start
        Parser parser = new Parser(this, kind,
                Objects.requireNonNullElseGet(options, ModuleOptions::new), source);
        return parser.parse(parent);
    }

    /**
     * Parse the source into an abstract syntax tree for further parsing.
     *
     * @param kind   the language
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
        Objects.requireNonNull(source);
        resetLog(); // make a clean start
        Parser parser = new Parser(this, kind,
                new ModuleOptions(), source);
        return parser.parseModule(parent);
    }

    /**
     * Returns the concatenated log message and clears the buffer.
     */
    @Nonnull
    public String getLogMessage() {
        int errors = getNumErrors();
        int warnings = getNumWarnings();
        if (errors > 0 || warnings > 0) {
            mLogBuilder.append(errors).append(" error");
            if (errors > 1) {
                mLogBuilder.append('s');
            }
            mLogBuilder.append(", ");
            mLogBuilder.append(warnings).append(" warning");
            if (warnings > 1) {
                mLogBuilder.append('s');
            }
            mLogBuilder.append('\n');
        }
        String result = mLogBuilder.toString();
        resetLog();
        return result;
    }

    /**
     * Returns the Compiler's error handler.
     */
    public ErrorHandler getErrorHandler() {
        return mErrorHandler;
    }

    public int getNumErrors() {
        return mErrorHandler.getNumErrors();
    }

    public int getNumWarnings() {
        return mErrorHandler.getNumWarnings();
    }

    private void resetLog() {
        boolean trim = mLogBuilder.length() > 8192;
        mLogBuilder.setLength(0);
        if (trim) {
            mLogBuilder.trimToSize();
        }
        mErrorHandler.reset();
    }
}
