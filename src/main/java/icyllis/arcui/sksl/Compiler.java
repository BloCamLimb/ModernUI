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

package icyllis.arcui.sksl;

import icyllis.arcui.engine.ShaderCaps;
import icyllis.arcui.sksl.ir.SymbolTable;

import javax.annotation.Nonnull;

/**
 * Main compiler entry point. The compiler parses the SkSL text directly into a tree of IRNodes,
 * while performing basic optimizations such as constant-folding and dead-code elimination. Then the
 * Program is passed into a CodeGenerator to produce compiled output.
 */
public class Compiler {

    public static final String ORTHOPROJ_NAME = "sk_OrthoProj";
    public static final String INVALID_TAG = "<INVALID>";
    public static final String POISON_TAG = "<POISON>";

    private final CompilerErrorReporter mErrorReporter = new CompilerErrorReporter();
    private final Context mContext;

    // holds ModifiersPools belonging to the core includes for lifetime purposes
    final ModifiersPool mCoreModifiers = new ModifiersPool();

    final Mangler mMangler = new Mangler();
    final Inliner mInliner;
    // This is the current symbol table of the code we are processing, and therefore changes during
    // compilation
    private SymbolTable mSymbolTable;

    private final StringBuilder mErrorText = new StringBuilder();

    public Compiler(ShaderCaps caps) {
        mContext = new Context(caps, mErrorReporter, mMangler);
        mInliner = new Inliner(mContext);
    }

    /**
     * Returns the Compiler's context.
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Returns current symbol table.
     */
    public SymbolTable getSymbolTable() {
        return mSymbolTable;
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
     * Returns the Compiler's error reporter, which is also the error reporter of {@link #getContext()}.
     */
    public ErrorReporter getErrorReporter() {
        assert mErrorReporter == mContext.mErrors;
        return mErrorReporter;
    }

    /**
     * Returns the number of errors. Shortcut for getErrorReporter().getErrorCount().
     */
    public int getErrorCount() {
        return getErrorReporter().getErrorCount();
    }

    private void resetErrors() {
        mErrorText.setLength(0); // reset but do not trim the internal array
        getErrorReporter().resetErrorCount();
    }

    private class CompilerErrorReporter extends ErrorReporter {

        @Override
        protected void handleError(int start, int end, String msg) {
            assert getErrorReporter() == this;
            assert (start <= end);
            assert (start <= 0xFFFFFF);

            mErrorText.append("error: ");
            boolean printLocation = false;
            String src = getSource();
            assert src != null;
            if (start != -1) {
                // we allow the offset to equal the length, because that's where TK_END_OF_FILE is reported
                int offset = Math.min(start, src.length());
                int line = 1;
                for (int i = 0; i < offset; i++) {
                    if (src.charAt(i) == '\n') {
                        line++;
                    }
                }
                printLocation = start < src.length();
                mErrorText.append(line).append(": ");
            }
            mErrorText.append(msg).append('\n');
            if (printLocation) {
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
