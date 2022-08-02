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

    public DSLParser(Compiler compiler, String text, byte kind, ProgramSettings settings) {
        mCompiler = compiler;
    }
}
