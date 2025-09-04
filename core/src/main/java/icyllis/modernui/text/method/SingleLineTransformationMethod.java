/*
 * Modern UI.
 * Copyright (C) 2021-2025 BloCamLimb. All rights reserved.
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
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *   Copyright (C) 2006 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package icyllis.modernui.text.method;

import icyllis.modernui.annotation.NonNull;

/**
 * This transformation method causes any newline characters (\n) to be
 * displayed as spaces instead of causing line breaks, and causes
 * carriage return characters (\r) to have no appearance.
 */
public class SingleLineTransformationMethod extends ReplacementTransformationMethod {

    private static final char[] ORIGINAL = new char[]{'\n', '\r'};
    private static final char[] REPLACEMENT = new char[]{' ', '\uFEFF'};

    private static final SingleLineTransformationMethod sInstance = new SingleLineTransformationMethod();

    protected SingleLineTransformationMethod() {
    }

    public static SingleLineTransformationMethod getInstance() {
        return sInstance;
    }

    /**
     * The characters to be replaced are \n and \r.
     */
    @NonNull
    @Override
    protected char[] getOriginal() {
        return ORIGINAL;
    }

    /**
     * The character \n is replaced with is space;
     * the character \r is replaced with is FEFF (zero width space).
     */
    @NonNull
    @Override
    protected char[] getReplacement() {
        return REPLACEMENT;
    }
}
