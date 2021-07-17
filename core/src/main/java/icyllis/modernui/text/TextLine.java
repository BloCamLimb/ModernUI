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

/**
 * Controls the layout of a single line of styled text, for measuring in visual
 * order and rendering. It can be used in any view without a TextView to draw a
 * line of text.
 */
public class TextLine {

    private TextPaint mPaint;
    private CharSequence mText;
    private int mStart;
    private int mLen;
    private int mDir;
    private Directions mDirections;

    public void draw() {
        float h = 0;
        final int e = mDirections.getRunCount();
        for (int i = 0; i < e; i++) {
            final int st = mDirections.getRunStart(i);
            if (st > mLen) {
                break;
            }
            final int lim = Math.min(st + mDirections.getRunLength(i), mLen);
            final boolean isRtl = mDirections.isRunRtl(i);

            /*h += drawRun(c, st, lim, isRtl, x + h, top, y, bottom,
                    i != (e - 1) || lim != mLen);*/
        }
    }
}
