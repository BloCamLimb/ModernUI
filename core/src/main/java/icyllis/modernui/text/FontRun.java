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

package icyllis.modernui.text;

import java.awt.*;

// font run, subrange of style run
public class FontRun {

    final Font mFont;
    final int mStart;
    int mEnd;

    public FontRun(Font font, int start, int end) {
        mFont = font;
        mStart = start;
        mEnd = end;
    }

    // base font without style and size
    public Font getFont() {
        return mFont;
    }

    // start index (inclusive)
    public int getStart() {
        return mStart;
    }

    // end index (exclusive)
    public int getEnd() {
        return mEnd;
    }
}
