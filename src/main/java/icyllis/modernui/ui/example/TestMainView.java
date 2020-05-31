/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import icyllis.modernui.ui.layout.ListLayout;
import icyllis.modernui.ui.layout.NormalLayout;
import icyllis.modernui.ui.layout.ScriptLayout;
import icyllis.modernui.ui.master.Canvas;
import icyllis.modernui.ui.master.View;
import icyllis.modernui.ui.master.ViewGroup;
import icyllis.modernui.ui.scroll.ScrollView;

import javax.annotation.Nonnull;

public class TestMainView extends ViewGroup {

    public TestMainView() {
        setLayout(new ScriptLayout(
                "parent.getWidth() / 2 - 60",
                "parent.getHeight() / 2 - 60",
                "120.0",
                "120.0"
        ));
        ScrollView s = new ScrollView();
        s.setLayout(new ScriptLayout(
                "prev.getLeft() + 20.0",
                "prev.getTop()",
                "30.0",
                "90"
        ));
        {
            View v = new View();
            v.setLayout(new NormalLayout());
            s.addActiveViewToPool(v);
        }
        for (int i = 0; i < 10; i++) {
            View v = new CView();
            v.setLayout(new ListLayout());
            s.addActiveViewToPool(v);
        }
        addActiveViewToPool(s);
    }

    @Override
    protected void onDraw(@Nonnull Canvas canvas, float time) {
        super.onDraw(canvas, time);
    }

    private static class CView extends View {
        @Override
        protected void onDraw(@Nonnull Canvas canvas, float time) {
            super.onDraw(canvas, time);
            canvas.drawText("233", getLeft(), getTop() + 6);
        }
    }
}
