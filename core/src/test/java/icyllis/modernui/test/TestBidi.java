/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.test;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterDirection;
import com.ibm.icu.text.Bidi;
import icyllis.modernui.util.Log;

public class TestBidi {

    public static void main(String[] args) {
        char[] charArray = "ABC\r\r\r\r\r\r\n\r\r\r\r".toCharArray();
        Log.LOGGER.info("text {}, length {}", charArray.toString(), charArray.length);
        Bidi bidi = new Bidi(charArray.length, 0);
        bidi.setPara(charArray, Bidi.LEVEL_DEFAULT_LTR, null);
        Log.LOGGER.info("Bidi last char para index {}", bidi.getParagraphIndex(charArray.length - 1));
        Log.LOGGER.info("Bidi text {}, length {}", bidi.getText().toString(), bidi.getLength());

        for (int i = 0; i < charArray.length; ++i) {
            if (Character.isSurrogate(charArray[i])) {
                continue;
            }
            if (UCharacter.getDirection(charArray[i])
                    == UCharacterDirection.BLOCK_SEPARATOR) {
                charArray[i] = '\uFFFC';
            }
        }
        bidi.setPara(charArray, Bidi.LEVEL_DEFAULT_LTR, null);
        Log.LOGGER.info("Bidi last char para index {}", bidi.getParagraphIndex(charArray.length - 1));
        Log.LOGGER.info("Bidi text {}, length {}", bidi.getText().toString(), bidi.getLength());
    }
}
