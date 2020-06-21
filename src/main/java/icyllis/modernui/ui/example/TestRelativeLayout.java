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

package icyllis.modernui.ui.example;

import icyllis.modernui.graphics.font.TextAlign;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.ui.layout.Gravity;
import icyllis.modernui.ui.layout.RelativeLayout;
import icyllis.modernui.ui.master.View;
import icyllis.modernui.ui.master.ViewGroup;

import javax.annotation.Nonnull;

public class TestRelativeLayout extends RelativeLayout {

    public TestRelativeLayout() {
        LayoutParams lp = new LayoutParams(40, 20);
        lp.gravity = Gravity.CENTER;
        CView view = new CView();
        view.setId(1);
        view.setLayoutParams(lp);
        view.text = "First One!";
        addView(view);

        lp = new LayoutParams(60, 20);
        lp.setRule(ABOVE, 1);
        lp.gravity = Gravity.HORIZONTAL_CENTER;
        view = new CView();
        view.setId(2);
        view.setLayoutParams(lp);
        view.text = "Second On The Top";
        addView(view);

        lp = new LayoutParams(LayoutParams.MATCH_PARENT, 30);
        lp.setRule(LEFT_OF, 1);
        lp.gravity = Gravity.CENTER;
        view = new CView();
        view.setId(3);
        view.setLayoutParams(lp);
        view.text = "3rd";
        addView(view);
    }

    private static class CView extends View {

        public String text;

        @Override
        protected void onDraw(@Nonnull Canvas canvas, float time) {
            canvas.resetColor();
            canvas.setTextAlign(TextAlign.CENTER);
            canvas.drawText(text, getLeft() + getWidth() / 2.0f, getTop() + 4.0f);
        }
    }
}
