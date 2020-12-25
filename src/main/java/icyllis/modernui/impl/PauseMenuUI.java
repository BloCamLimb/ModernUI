/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.impl;

import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.view.ApplicationUI;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.FrameLayout;

import javax.annotation.Nonnull;

import static icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class PauseMenuUI extends ApplicationUI {

    @Override
    public void onCreate() {
        FrameLayout frameLayout = new FrameLayout();
        View child = new NavigationBar();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(32, MATCH_PARENT);
        frameLayout.addView(child, params);
        setContentView(frameLayout, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
    }

    private static class NavigationBar extends View {

        @Override
        protected void onDraw(@Nonnull Canvas canvas) {
            canvas.setColor(186, 227, 249, 96);
            canvas.drawRect(0, 0, getRight(), getBottom());
            canvas.setColor(128, 128, 128, 255);
            canvas.drawRect(getRight() - 1, 0, getRight(), getBottom());
        }
    }
}
