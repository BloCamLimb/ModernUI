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

package icyllis.modernui.widget;

import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Extended {@link Adapter} that is the bridge between a
 * {@link Spinner} and its data. A spinner adapter allows defining two
 * different views: one that shows the data in the spinner itself
 * and one that shows the data in the drop-down list when the spinner is
 * pressed.
 */
public interface SpinnerAdapter extends Adapter {

    /**
     * Gets a {@link View} that displays in the drop-down popup
     * the data at the specified position in the data set.
     * <p>
     * This method should not return null unless the item count is zero.
     *
     * @param position    index of the item whose view we want.
     * @param convertView the old view to reuse, if possible. Note: You should
     *                    check that this view is non-null and of an appropriate type before
     *                    using. If it is not possible to convert this view to display the
     *                    correct data, this method can create a new view.
     * @param parent      the parent that this view will eventually be attached to
     * @return a {@link View} corresponding to the data at the specified position.
     */
    View getDropDownView(int position, @Nullable View convertView, @Nonnull ViewGroup parent);
}
