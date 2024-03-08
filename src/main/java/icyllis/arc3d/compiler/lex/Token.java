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
            TK_STRINGLITERAL = 3,
            TK_TRUE = 4,
            TK_FALSE = 5,
            TK_BREAK = 6,
            TK_CONTINUE = 7,
            TK_DO = 8,
            TK_FOR = 9,
            TK_WHILE = 10,
            TK_IF = 11,
            TK_ELSE = 12,
            TK_SWITCH = 13,
            TK_CASE = 14,
            TK_DEFAULT = 15,
            TK_DISCARD = 16,
            TK_RETURN = 17,
            TK_IN = 18,
            TK_OUT = 19,
            TK_INOUT = 20,
            TK_CONST = 21,
            TK_UNIFORM = 22,
            TK_BUFFER = 23,
            TK_WORKGROUP = 24,
            TK_SMOOTH = 25,
            TK_FLAT = 26,
            TK_NOPERSPECTIVE = 27,
            TK_COHERENT = 28,
            TK_VOLATILE = 29,
            TK_RESTRICT = 30,
            TK_READONLY = 31,
            TK_WRITEONLY = 32,
            TK_SUBROUTINE = 33,
            TK_LAYOUT = 34,
            TK_STRUCT = 35,
            TK_USING = 36,
            TK_INLINE = 37,
            TK_NOINLINE = 38,
            TK_PURE = 39,
            TK_RESERVED = 40,
            TK_IDENTIFIER = 41,
            TK_HASH = 42,
            TK_LPAREN = 43,
            TK_RPAREN = 44,
            TK_LBRACE = 45,
            TK_RBRACE = 46,
            TK_LBRACKET = 47,
            TK_RBRACKET = 48,
            TK_DOT = 49,
            TK_COMMA = 50,
            TK_EQ = 51,
            TK_LT = 52,
            TK_GT = 53,
            TK_BANG = 54,
            TK_TILDE = 55,
            TK_QUES = 56,
            TK_COLON = 57,
            TK_EQEQ = 58,
            TK_LTEQ = 59,
            TK_GTEQ = 60,
            TK_BANGEQ = 61,
            TK_PLUSPLUS = 62,
            TK_MINUSMINUS = 63,
            TK_PLUS = 64,
            TK_MINUS = 65,
            TK_STAR = 66,
            TK_SLASH = 67,
            TK_PERCENT = 68,
            TK_LTLT = 69,
            TK_GTGT = 70,
            TK_AMPAMP = 71,
            TK_PIPEPIPE = 72,
            TK_CARETCARET = 73,
            TK_AMP = 74,
            TK_PIPE = 75,
            TK_CARET = 76,
            TK_PLUSEQ = 77,
            TK_MINUSEQ = 78,
            TK_STAREQ = 79,
            TK_SLASHEQ = 80,
            TK_PERCENTEQ = 81,
            TK_LTLTEQ = 82,
            TK_GTGTEQ = 83,
            TK_AMPEQ = 84,
            TK_PIPEEQ = 85,
            TK_CARETEQ = 86,
            TK_SEMICOLON = 87,
            TK_NEWLINE = 88,
            TK_WHITESPACE = 89,
            TK_LINE_COMMENT = 90,
            TK_BLOCK_COMMENT = 91,
            TK_INVALID = 92,
            TK_NONE = 93;

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
