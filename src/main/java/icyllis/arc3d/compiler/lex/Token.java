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

package icyllis.arc3d.compiler.lex;

/**
 * Represents a lexer token that encapsulates the token kind and position.
 * It's packed as a <code>long</code> value to reduce object allocation.
 * This class provides static methods to pack and unpack lexer tokens.
 */
public final class Token {

    /**
     * Token Kinds, 0 is reserved for END_OF_FILE, others are sequential and match with lexicon.
     */
    public static final int
            TK_END_OF_FILE = 0,
            TK_INTLITERAL = 1,
            TK_FLOATLITERAL = 2,
            TK_TRUE = 3,
            TK_FALSE = 4,
            TK_BREAK = 5,
            TK_CONTINUE = 6,
            TK_DO = 7,
            TK_FOR = 8,
            TK_WHILE = 9,
            TK_IF = 10,
            TK_ELSE = 11,
            TK_SWITCH = 12,
            TK_CASE = 13,
            TK_DEFAULT = 14,
            TK_DISCARD = 15,
            TK_RETURN = 16,
            TK_IN = 17,
            TK_OUT = 18,
            TK_INOUT = 19,
            TK_CONST = 20,
            TK_UNIFORM = 21,
            TK_BUFFER = 22,
            TK_WORKGROUP = 23,
            TK_SMOOTH = 24,
            TK_FLAT = 25,
            TK_NOPERSPECTIVE = 26,
            TK_COHERENT = 27,
            TK_VOLATILE = 28,
            TK_RESTRICT = 29,
            TK_READONLY = 30,
            TK_WRITEONLY = 31,
            TK_SUBROUTINE = 32,
            TK_LAYOUT = 33,
            TK_STRUCT = 34,
            TK_USING = 35,
            TK_INLINE = 36,
            TK_NOINLINE = 37,
            TK_PURE = 38,
            TK_RESERVED = 39,
            TK_IDENTIFIER = 40,
            TK_HASH = 41,
            TK_LPAREN = 42,
            TK_RPAREN = 43,
            TK_LBRACE = 44,
            TK_RBRACE = 45,
            TK_LBRACKET = 46,
            TK_RBRACKET = 47,
            TK_DOT = 48,
            TK_COMMA = 49,
            TK_EQ = 50,
            TK_LT = 51,
            TK_GT = 52,
            TK_BANG = 53,
            TK_TILDE = 54,
            TK_QUES = 55,
            TK_COLON = 56,
            TK_EQEQ = 57,
            TK_LTEQ = 58,
            TK_GTEQ = 59,
            TK_BANGEQ = 60,
            TK_PLUSPLUS = 61,
            TK_MINUSMINUS = 62,
            TK_PLUS = 63,
            TK_MINUS = 64,
            TK_STAR = 65,
            TK_SLASH = 66,
            TK_PERCENT = 67,
            TK_LTLT = 68,
            TK_GTGT = 69,
            TK_AMPAMP = 70,
            TK_PIPEPIPE = 71,
            TK_CARETCARET = 72,
            TK_AMP = 73,
            TK_PIPE = 74,
            TK_CARET = 75,
            TK_PLUSEQ = 76,
            TK_MINUSEQ = 77,
            TK_STAREQ = 78,
            TK_SLASHEQ = 79,
            TK_PERCENTEQ = 80,
            TK_LTLTEQ = 81,
            TK_GTGTEQ = 82,
            TK_AMPEQ = 83,
            TK_PIPEEQ = 84,
            TK_CARETEQ = 85,
            TK_SEMICOLON = 86,
            TK_NEWLINE = 87,
            TK_WHITESPACE = 88,
            TK_LINE_COMMENT = 89,
            TK_BLOCK_COMMENT = 90,
            TK_INVALID = 91,
            TK_NONE = 92;

    public static final long NO_TOKEN = -1;

    // encode token state
    // 0-16 bits: token kind
    // 16-40 bits: token offset
    // 40-64 bits: token length
    public static long make(int kind, int offset, int length) {
        assert (kind >= 0 && kind <= 0xFFFF);
        assert (offset >= 0 && offset <= 0xFFFFFF);
        assert (length >= 0 && length <= 0xFFFFFF);
        long token = kind | (long) offset << 16 | (long) length << 40;
        assert (token != NO_TOKEN);
        return token;
    }

    public static int kind(long token) {
        return (int) (token & 0xFFFF);
    }

    public static int offset(long token) {
        return (int) (token >> 16) & 0xFFFFFF;
    }

    public static int length(long token) {
        return (int) (token >>> 40);
    }

    public static long replace(long token, int kind) {
        assert (kind >= 0 && kind <= 0xFFFF);
        return (token & ~0xFFFF) | kind;
    }

    private Token() {
    }
}
