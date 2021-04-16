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
 * Copyright (C) 2014 The Android Open Source Project
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

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;

/**
 * A utility class for Emoji.
 */
// https://android.googlesource.com/platform/frameworks/minikin/+/refs/heads/master/libs/minikin/Emoji.cpp
public final class Emoji {

    public static boolean isEmoji(int codePoint) {
        return UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI);
    }

    public static boolean isEmojiModifier(int codePoint) {
        return UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI_MODIFIER);
    }

    public static boolean isEmojiBase(int codePoint) {
        // These two characters were removed from Emoji_Modifier_Base in Emoji 4.0, but we need to
        // keep them as emoji modifier bases since there are fonts and user-generated text out there
        // that treats these as potential emoji bases.
        if (codePoint == 0x1F91D || codePoint == 0x1F93C) {
            return true;
        }
        return UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI_MODIFIER_BASE);
    }

    public static boolean isRegionalIndicatorSymbol(int codePoint) {
        return 0x1F1E6 <= codePoint && codePoint <= 0x1F1FF;
    }
}
