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

package icyllis.modernui.view;

import icyllis.modernui.ModernUI;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Interface to let you add and remove child views to a view system. To get an instance
 * of this class, call {@link ModernUI#getViewManager()}.
 */
@ParametersAreNonnullByDefault
public interface ViewManager {

    /**
     * Assign the passed LayoutParams to the passed View and add the view to the window.
     *
     * @param view   The view to be added to this window.
     * @param params The LayoutParams to assign to view.
     */
    void addView(View view, ViewGroup.LayoutParams params);

    void updateViewLayout(View view, ViewGroup.LayoutParams params);

    void removeView(View view);
}
