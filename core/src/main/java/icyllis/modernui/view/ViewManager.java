/*
 * ModernUI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.view;

import icyllis.modernui.annotation.NonNull;

/**
 * Interface for adding/removing child views to/from a view hierarchy.
 */
public interface ViewManager {

    /**
     * Assign the passed LayoutParams to the passed View and add the view to the window.
     *
     * @param view   The view to be added to this window.
     * @param params The LayoutParams to assign to view.
     */
    void addView(@NonNull View view, @NonNull ViewGroup.LayoutParams params);

    void updateViewLayout(@NonNull View view, @NonNull ViewGroup.LayoutParams params);

    void removeView(@NonNull View view);
}
