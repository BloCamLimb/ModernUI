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

package icyllis.akashigi.slang.ir;

import javax.annotation.Nonnull;

/**
 * Represents a node in the AST. The AST is a fully-resolved version of the program
 * (all types determined, everything validated), ready for code generation.
 */
public abstract class Node {

    public static final class ElementKind {

        public static final int kFirst = 0;
        public static final int
                kExtension = kFirst,
                kFunctionDefinition = kFirst + 1,
                kFunctionPrototype = kFirst + 2,
                kGlobalVar = kFirst + 3,
                kInterfaceBlock = kFirst + 4,
                kModifiers = kFirst + 5,
                kStructDeclaration = kFirst + 6;
        public static final int kLast = kStructDeclaration;

        private ElementKind() {
        }
    }

    public static final class SymbolKind {

        public static final int kFirst = ElementKind.kLast + 1;
        public static final int
                kAnonymousField = kFirst,
                kFunctionDeclaration = kFirst + 1,
                kType = kFirst + 2,
                kVariable = kFirst + 3;
        public static final int kLast = kVariable;

        private SymbolKind() {
        }
    }

    public static final class StatementKind {

        public static final int kFirst = SymbolKind.kLast + 1;
        public static final int
                kBlock = kFirst,
                kBreak = kFirst + 1,
                kContinue = kFirst + 2,
                kDiscard = kFirst + 3,
                kDo = kFirst + 4,
                kExpression = kFirst + 5,
                kFor = kFirst + 6,
                kIf = kFirst + 7,
                kNop = kFirst + 8,
                kReturn = kFirst + 9,
                kSwitch = kFirst + 10,
                kSwitchCase = kFirst + 11,
                kVarDeclaration = kFirst + 12;
        public static final int kLast = kVarDeclaration;

        private StatementKind() {
        }
    }

    public static final class ExpressionKind {

        public static final int kFirst = StatementKind.kLast + 1;
        public static final int
                kArrayAccess = kFirst,
                kBinary = kFirst + 1,
                kConditional = kFirst + 2,
                kConstructorArray = kFirst + 3,
                kConstructorArrayCast = kFirst + 4,
                kConstructorCompound = kFirst + 5,
                kConstructorCompoundCast = kFirst + 6,
                kConstructorMatrixDiag = kFirst + 7,
                kConstructorMatrixResize = kFirst + 8,
                kConstructorScalarCast = kFirst + 9,
                kConstructorStruct = kFirst + 10,
                kConstructorVectorSplat = kFirst + 11,
                kFieldAccess = kFirst + 12,
                kFunctionCall = kFirst + 13,
                kFunctionReference = kFirst + 14,
                kLiteral = kFirst + 15,
                kPoison = kFirst + 16,
                kPostfix = kFirst + 17,
                kPrefix = kFirst + 18,
                kSwizzle = kFirst + 19,
                kTypeReference = kFirst + 20,
                kVarReference = kFirst + 21;
        public static final int kLast = kVarReference;

        private ExpressionKind() {
        }
    }

    // position of this element within the program being compiled, for error reporting purposes
    public int mPosition;

    protected final int mKind;

    /**
     * @param position see {@link #makeRange(int, int)}
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
    public static int makeRange(int start, int end) {
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
