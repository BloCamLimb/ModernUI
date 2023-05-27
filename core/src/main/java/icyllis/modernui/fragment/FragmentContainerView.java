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

package icyllis.modernui.fragment;

import icyllis.modernui.core.Context;
import icyllis.modernui.widget.FrameLayout;

public final class FragmentContainerView extends FrameLayout {

    // Used to indicate whether the FragmentContainerView should override the default ViewGroup
    // drawing order.
    private boolean mDrawDisappearingViewsFirst = true;

    public FragmentContainerView(Context context) {
        super(context);
    }

    // Used to indicate the container should change the default drawing order.
    void setDrawDisappearingViewsLast(boolean drawDisappearingViewsFirst) {
        mDrawDisappearingViewsFirst = drawDisappearingViewsFirst;
    }
}
