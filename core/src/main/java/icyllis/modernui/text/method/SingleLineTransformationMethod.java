/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.text.method;

/**
 * This transformation method causes any newline characters (\n) to be
 * displayed as spaces instead of causing line breaks, and causes
 * carriage return characters (\r) to have no appearance.
 */
public class SingleLineTransformationMethod extends ReplacementTransformationMethod {

    private static final SingleLineTransformationMethod sInstance = new SingleLineTransformationMethod();

    private static final char[] ORIGINAL = new char[]{'\n', '\r'};
    private static final char[] REPLACEMENT = new char[]{' ', '\uFEFF'};

    private SingleLineTransformationMethod() {
    }

    public static SingleLineTransformationMethod getInstance() {
        return sInstance;
    }

    /**
     * The characters to be replaced are \n and \r.
     */
    @Override
    protected char[] getOriginal() {
        return ORIGINAL;
    }

    /**
     * The character \n is replaced with is space;
     * the character \r is replaced with is FEFF (zero width space).
     */
    @Override
    protected char[] getReplacement() {
        return REPLACEMENT;
    }
}
