/*
 * Modern UI.
 * Copyright (C) 2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.widget;

import icyllis.modernui.annotation.Nullable;

/**
 * <p>Defines a filterable behavior. A filterable class can have its data
 * constrained by a filter. Filterable classes are usually
 * {@link Adapter} implementations.</p>
 *
 * @see Filter
 */
public interface Filterable {

    /**
     * <p>Returns a filter that can be used to constrain data with a filtering
     * pattern.</p>
     *
     * <p>This method is usually implemented by {@link Adapter} classes.</p>
     *
     * @return a filter used to constrain data
     */
    @Nullable
    Filter getFilter();
}
