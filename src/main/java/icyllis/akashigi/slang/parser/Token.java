/*
 * Akashi GI.
 * Copyright (C) 2022-2023 BloCamLimb. All rights reserved.
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

package icyllis.akashigi.slang.parser;

/**
 * Represents a lexer token that encapsulates the token kind and position.
 * It's packed as a <code>long</code> value to reduce object allocation.
 * This class provides static methods to pack and unpack lexer tokens.
 */
public final class Token {

    // encode token state
    // 0-16 bits: token kind
    // 16-40 bits: token offset
    // 40-64 bits: token length
    public static long make(int kind, int offset, int length) {
        assert (kind >= 0 && kind <= 0xFFFF);
        assert (offset >= 0 && offset <= 0xFFFFFF);
        assert (length >= 0 && length <= 0xFFFFFF);
        long token = kind | (long) offset << 16 | (long) length << 40;
        assert (token != -1);
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
