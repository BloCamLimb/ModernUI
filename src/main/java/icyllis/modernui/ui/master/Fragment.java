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

package icyllis.modernui.ui.master;

import icyllis.modernui.ui.layout.FrameLayout;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * Part of a UI, used to manage specified view creation and its logic
 * A UI can contain multiple fragments, which are controlled by UIManager
 * to determine whether they are instantiated and enabled.
 * Different fragments can communicate with each other,
 * and there can be transition animation when switching etc.
 */
//TODO experimental
@OnlyIn(Dist.CLIENT)
public class Fragment {

    /**
     * The view created from this fragment
     */
    View root;

    /**
     * Create the view belong to this fragment.
     * <p>
     * If this fragment is main fragment of a UI, this method
     * should create the main view of the UI, and can't be null.
     * <p>
     * The main view of a UI is considered as a child of FrameLayout,
     * so you can use LayoutParams of FrameLayout to layout the view.
     * See {@link FrameLayout.LayoutParams}
     *
     * @return view instance or null
     */
    @Nullable
    public View createView() {
        return null;
    }

    /**
     * Get the view created from this fragment
     *
     * @return root view
     */
    public View getView() {
        return root;
    }
}
