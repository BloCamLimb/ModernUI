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

import icyllis.modernui.util.DataSetObserver;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An Adapter object acts as a bridge between an {@link AdapterView} and the
 * underlying data for that view. The Adapter provides access to the data items.
 * The Adapter is also responsible for making a {@link View} for
 * each item in the data set.
 *
 * @see ArrayAdapter
 */
public interface Adapter {

    /**
     * Register an observer that is called when changes happen to the data used by this adapter.
     *
     * @param observer the object that gets notified when the data set changes.
     */
    void registerDataSetObserver(@Nonnull DataSetObserver observer);

    /**
     * Unregister an observer that has previously been registered with this
     * adapter via {@link #registerDataSetObserver}.
     *
     * @param observer the object to unregister.
     */
    void unregisterDataSetObserver(@Nonnull DataSetObserver observer);

    /**
     * How many items are in the data set represented by this Adapter.
     *
     * @return Count of items.
     */
    int getCount();

    /**
     * Get the data item associated with the specified position in the data set.
     *
     * @param position Position of the item whose data we want within the adapter's
     *                 data set.
     * @return The data at the specified position.
     */
    @Nullable
    Object getItem(int position);

    /**
     * Get the row id associated with the specified position in the list.
     *
     * @param position The position of the item within the adapter's data set whose row id we want.
     * @return The id of the item at the specified position.
     */
    long getItemId(int position);

    /**
     * Indicates whether the item ids are stable across changes to the
     * underlying data.
     *
     * @return True if the same id always refers to the same object.
     */
    boolean hasStableIds();

    /**
     * Get a View that displays the data at the specified position in the data set. You can
     * create a View manually.
     * <p>
     * This method should not return null unless the item count is zero.
     *
     * @param position    The position of the item within the adapter's data set of the item whose view
     *                    we want.
     * @param convertView The old view to reuse, if possible. Note: You should check that this view
     *                    is non-null and of an appropriate type before using. If it is not possible to convert
     *                    this view to display the correct data, this method can create a new view.
     *                    Heterogeneous lists can specify their number of view types, so that this View is
     *                    always of the right type (see {@link #getViewTypeCount()} and
     *                    {@link #getItemViewType(int)}).
     * @param parent      The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position.
     */
    View getView(int position, @Nullable View convertView, @Nonnull ViewGroup parent);

    /**
     * An item view type that causes the {@link AdapterView} to ignore the item
     * view. For example, this can be used if the client does not want a
     * particular view to be given for conversion in
     * {@link #getView(int, View, ViewGroup)}.
     *
     * @see #getItemViewType(int)
     * @see #getViewTypeCount()
     */
    int IGNORE_ITEM_VIEW_TYPE = AdapterView.ITEM_VIEW_TYPE_IGNORE;

    /**
     * Get the type of View that will be created by {@link #getView} for the specified item.
     *
     * @param position The position of the item within the adapter's data set whose view type we
     *                 want.
     * @return An integer representing the type of View. Two views should share the same type if one
     * can be converted to the other in {@link #getView}. Note: Integers must be in the
     * range 0 to {@link #getViewTypeCount} - 1. {@link #IGNORE_ITEM_VIEW_TYPE} can
     * also be returned.
     * @see #IGNORE_ITEM_VIEW_TYPE
     */
    int getItemViewType(int position);

    /**
     * <p>
     * Returns the number of types of Views that will be created by
     * {@link #getView}. Each type represents a set of views that can be
     * converted in {@link #getView}. If the adapter always returns the same
     * type of View for all items, this method should return 1.
     * </p>
     * <p>
     * This method will only be called when the adapter is set on the {@link AdapterView}.
     * </p>
     *
     * @return The number of types of Views that will be created by this adapter
     */
    int getViewTypeCount();

    int NO_SELECTION = Integer.MIN_VALUE;

    /**
     * @return true if this adapter doesn't contain any data.  This is used to determine
     * whether the empty view should be displayed.  A typical implementation will return
     * getCount() == 0 but since getCount() includes the headers and footers, specialized
     * adapters might want a different behavior.
     */
    boolean isEmpty();
}
