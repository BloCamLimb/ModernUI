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

package icyllis.modernui.ui.example;

import icyllis.modernui.ui.layout.FrameLayout;
import icyllis.modernui.ui.layout.Gravity;
import icyllis.modernui.ui.master.*;
import icyllis.modernui.ui.widget.ScrollView;

import javax.annotation.Nullable;

public class TestFragment extends Fragment {

    @Nullable
    @Override
    public View createView() {
        ScrollView scrollView = new ScrollView();
        // main view can use FrameLayout params
        ViewRootImpl.LayoutParams params = new ViewRootImpl.LayoutParams(140, 140);
        params.gravity = Gravity.CENTER;
        scrollView.setLayoutParams(params);
        View content = new TestLinearLayout();
        content.setLayoutParams(new FrameLayout.LayoutParams(140, 800));
        scrollView.addView(content);
        return scrollView;
    }
}
