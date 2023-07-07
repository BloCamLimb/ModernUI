/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.text;

import java.util.Objects;

/**
 * Text shaping result object for single style text.
 */
public class Layout {

    public static final int BIDI_LTR = 0;
    public static final int BIDI_RTL = 1;
    public static final int BIDI_DEFAULT_LTR = 2;
    public static final int BIDI_DEFAULT_RTL = 3;
    public static final int BIDI_OVERRIDE_LTR = 4;
    public static final int BIDI_OVERRIDE_RTL = 5;

    private final float[] mAdvances;
    private float mAdvance;

    public Layout(char[] buf, int contextStart, int contextEnd, int start, int end,
                  int bidiFlags, FontPaint paint) {
        Objects.requireNonNull(buf);
        Objects.checkFromToIndex(contextStart, contextEnd, buf.length);
        if (contextStart > start || contextEnd > contextStart || contextEnd < end) {
            throw new IndexOutOfBoundsException();
        }
        if (bidiFlags < 0 || bidiFlags > 5) {
            throw new IllegalArgumentException();
        }
        mAdvances = new float[end - start];
    }

    private static float doLayoutWord(char[] buf, int contextStart, int contextEnd, int start, int end,
                                     boolean isRtl, FontPaint paint) {
        float advance = 0;

        return 0;
    }
}
