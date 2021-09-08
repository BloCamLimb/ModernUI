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

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package icyllis.modernui.text;

import javax.annotation.Nonnull;

/**
 * This is the class for text whose content and markup are immutable.
 * For mutable markup, see {@link SpannableString}.
 */
public final class SpannedString extends SpannableStringInternal implements Spanned, GetChars {

    /**
     * @param source           source object to copy from
     * @param ignoreNoCopySpan whether to copy NoCopySpans in the {@code source}
     */
    public SpannedString(@Nonnull CharSequence source, boolean ignoreNoCopySpan) {
        super(source, 0, source.length(), ignoreNoCopySpan);
    }

    private SpannedString(@Nonnull CharSequence source) {
        this(source, false);
    }

    private SpannedString(@Nonnull CharSequence source, int start, int end) {
        super(source, start, end, false);
    }

    @Nonnull
    public static SpannedString valueOf(@Nonnull CharSequence source) {
        if (source instanceof SpannedString) {
            return (SpannedString) source;
        } else {
            return new SpannedString(source);
        }
    }

    @Nonnull
    @Override
    public CharSequence subSequence(int start, int end) {
        return new SpannedString(this, start, end);
    }
}
