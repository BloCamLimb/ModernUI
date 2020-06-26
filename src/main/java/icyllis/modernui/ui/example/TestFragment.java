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

import icyllis.modernui.ui.layout.FrameLayout;
import icyllis.modernui.ui.layout.Gravity;
import icyllis.modernui.ui.master.Fragment;
import icyllis.modernui.ui.master.View;
import icyllis.modernui.ui.master.ViewGroup;
import icyllis.modernui.ui.widget.ScrollView;

import javax.annotation.Nullable;

public class TestFragment extends Fragment {

    @Nullable
    @Override
    public View createView() {
        ScrollView scrollView = new ScrollView();
        // main view can use FrameLayout params
        scrollView.setLayoutParams(new FrameLayout.LayoutParams(100, 100, Gravity.CENTER));
        View content = new TestLinearLayout();
        content.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200));
        scrollView.addView(content);
        return scrollView;
    }
}
