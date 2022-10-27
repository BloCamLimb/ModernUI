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

package icyllis.akashigi.aksl.ir;

import javax.annotation.Nonnull;

/**
 * Represents a node in the AST. The AST is a fully-resolved version of the program
 * (all types determined, everything validated), ready for code generation.
 */
public abstract class Node {

    // position of this element within the program being compiled, for error reporting purposes
    public int mPosition;

    protected final int mKind;

    /**
     * @param position see {@link #range(int, int)}
     */
    protected Node(int position, int kind) {
        mPosition = position;
        mKind = kind;
    }

    public final int getStartOffset() {
        assert (mPosition != -1);
        return (mPosition & 0xFFFFFF);
    }

    public final int getEndOffset() {
        assert (mPosition != -1);
        return (mPosition & 0xFFFFFF) + (mPosition >>> 24);
    }

    public static int getStartOffset(int position) {
        return (position & 0xFFFFFF);
    }

    public static int getEndOffset(int position) {
        return (position & 0xFFFFFF) + (position >>> 24);
    }

    /**
     * Pack a range into a position.
     * <ul>
     * <li>0-24 bits: start offset, less than 0x7FFFFF or invalid</li>
     * <li>24-32 bits: length, truncate at 0xFF</li>
     * </ul>
     */
    public static int range(int start, int end) {
        if ((start | end - start | 0x7FFFFF - end) < 0) {
            return -1;
        }
        return start | Math.min(end - start, 0xFF) << 24;
    }

    /**
     * @return a string representation of this AST node
     */
    @Nonnull
    @Override
    public abstract String toString();
}
