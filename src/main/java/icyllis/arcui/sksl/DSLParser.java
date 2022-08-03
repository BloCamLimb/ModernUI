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

import icyllis.arcui.sksl.lex.*;

/**
 * Domain-specific language parser, consumes SkSL text and invokes DSL functions to instantiate the program.
 */
public class DSLParser {

    public static final byte
            LAYOUT_TOKEN_LOCATION = 0,
            LAYOUT_TOKEN_OFFSET = 1,
            LAYOUT_TOKEN_BINDING = 2,
            LAYOUT_TOKEN_INDEX = 3,
            LAYOUT_TOKEN_SET = 4,
            LAYOUT_TOKEN_BUILTIN = 5,
            LAYOUT_TOKEN_INPUT_ATTACHMENT_INDEX = 6,
            LAYOUT_TOKEN_ORIGIN_UPPER_LEFT = 7,
            LAYOUT_TOKEN_BLEND_SUPPORT_ALL_EQUATIONS = 8,
            LAYOUT_TOKEN_PUSH_CONSTANT = 9,
            LAYOUT_TOKEN_COLOR = 10;

    private final Compiler mCompiler;
    private final String mText;

    private int mOffset;
    private int mLastOffset;

    public DSLParser(Compiler compiler, String text, byte kind, ProgramSettings settings) {
        mCompiler = compiler;
        mText = text;
    }

    public int next() {
        // note that we cheat here: normally a lexer needs to worry about the case
        // where a token has a prefix which is not itself a valid token - for instance,
        // maybe we have a valid token 'while', but 'w', 'wh', etc. are not valid
        // tokens. Our grammar doesn't have this property, so we can simplify the logic
        // a bit.
        int startOffset = mOffset;
        int state = 1;
        for (;;) {
            if (mOffset >= mText.length()) {
                if (startOffset == mText.length() || Lexer.ACCEPTS[state] == DFA.INVALID) {
                    return Lexer.TK_END_OF_FILE;
                }
                break;
            }
            int c = (mText.charAt(mOffset) - NFAtoDFA.START_CHAR);
            if (c >= NFAtoDFA.END_CHAR - NFAtoDFA.START_CHAR) {
                c = Lexer.INVALID_CHAR;
            }
            int newState = Lexer.getTransition(Lexer.MAPPINGS[c], state);
            if (newState == 0) {
                break;
            }
            state = newState;
            ++mOffset;
        }
        mLastOffset = startOffset;
        return Lexer.ACCEPTS[state];
    }
}
