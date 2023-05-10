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

package icyllis.modernui.util;

/**
 * Receives callbacks when a data set has been changed, or made invalid. The typical data sets
 * that are observed are {@link icyllis.modernui.widget.Adapter Adapters}.<br>
 * {@link DataSetObserver} must be implemented by objects which are added to a
 * {@link DataSetObservable}.
 */
public interface DataSetObserver {

    /**
     * This method is called when the entire data set has changed.
     */
    default void onChanged() {
        // Do nothing
    }

    /**
     * This method is called when the entire data becomes invalid.
     */
    default void onInvalidated() {
        // Do nothing
    }
}
