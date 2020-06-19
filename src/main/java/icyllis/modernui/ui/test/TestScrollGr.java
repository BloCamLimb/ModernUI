/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.test;

import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.graphics.font.TextAlign;
import icyllis.modernui.ui.widget.IScrollHost;
import icyllis.modernui.ui.widget.UniformScrollEntry;
import icyllis.modernui.ui.widget.UniformScrollGroup;

import javax.annotation.Nonnull;

@Deprecated
public class TestScrollGr extends UniformScrollGroup<TestScrollGr.TestScrollEn> {

    public TestScrollGr(IScrollHost window) {
        super(window, 18);
        for (int i = 0; i < 20; i++) {
            entries.add(new TestScrollEn(window));
        }
        height = entries.size() * entryHeight;
    }

    @Override
    public void locate(float px, float py) {
        super.locate(px, py);
        int i = 0;
        for (TestScrollEn entry : entries) {
            float cy = py + i * entryHeight;
            entry.locate(px, cy);
            i++;
        }
    }

    public static class TestScrollEn extends UniformScrollEntry {

        public TestScrollEn(@Nonnull IScrollHost window) {
            super(window, 100, 18);
        }

        @Override
        protected void onDraw(@Nonnull Canvas canvas, float time) {
            canvas.setRGBA(0, 0, 0, 0.1f);
            canvas.drawRect(x1, y1 + 1, x2, y2 - 1);
            canvas.resetColor();
            canvas.setTextAlign(TextAlign.LEFT);
            canvas.drawText(String.valueOf(hashCode()), x1 + 10, y1 + 5);
        }
    }
}
