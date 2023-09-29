/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

/**
 * Additional methods that when implemented make an
 * {@link ExpandableListAdapter} take advantage of the {@link Adapter} view type
 * mechanism.
 * <p>
 * An {@link ExpandableListAdapter} declares it has one view type for its group items
 * and one view type for its child items. Although adapted for most {@link ExpandableListView}s,
 * these values should be tuned for heterogeneous {@link ExpandableListView}s.
 * </p>
 * Lists that contain different types of group and/or child item views, should use an adapter that
 * implements this interface. This way, the recycled views that will be provided to
 * {@link ExpandableListAdapter#getGroupView(int, boolean, View, ViewGroup)}
 * and
 * {@link ExpandableListAdapter#getChildView(int, int, boolean, View, ViewGroup)}
 * will be of the appropriate group or child type, resulting in a more efficient reuse of the
 * previously created views.
 */
public interface HeterogeneousExpandableList {
    /**
     * Get the type of group View that will be created by
     * {@link ExpandableListAdapter#getGroupView(int, boolean, View, ViewGroup)}
     * . for the specified group item.
     *
     * @param groupPosition the position of the group for which the type should be returned.
     * @return An integer representing the type of group View. Two group views should share the same
     * type if one can be converted to the other in
     * {@link ExpandableListAdapter#getGroupView(int, boolean, View, ViewGroup)}
     * . Note: Integers must be in the range 0 to {@link #getGroupTypeCount} - 1.
     * {@link Adapter#IGNORE_ITEM_VIEW_TYPE} can also be returned.
     * @see Adapter#IGNORE_ITEM_VIEW_TYPE
     * @see #getGroupTypeCount()
     */
    int getGroupType(int groupPosition);

    /**
     * Get the type of child View that will be created by
     * {@link ExpandableListAdapter#getChildView(int, int, boolean, View, ViewGroup)}
     * for the specified child item.
     *
     * @param groupPosition the position of the group that the child resides in
     * @param childPosition the position of the child with respect to other children in the group
     * @return An integer representing the type of child View. Two child views should share the same
     * type if one can be converted to the other in
     * {@link ExpandableListAdapter#getChildView(int, int, boolean, View, ViewGroup)}
     * Note: Integers must be in the range 0 to {@link #getChildTypeCount} - 1.
     * {@link Adapter#IGNORE_ITEM_VIEW_TYPE} can also be returned.
     * @see Adapter#IGNORE_ITEM_VIEW_TYPE
     * @see #getChildTypeCount()
     */
    int getChildType(int groupPosition, int childPosition);

    /**
     * <p>
     * Returns the number of types of group Views that will be created by
     * {@link ExpandableListAdapter#getGroupView(int, boolean, View, ViewGroup)}
     * . Each type represents a set of views that can be converted in
     * {@link ExpandableListAdapter#getGroupView(int, boolean, View, ViewGroup)}
     * . If the adapter always returns the same type of View for all group items, this method should
     * return 1.
     * </p>
     * This method will only be called when the adapter is set on the {@link AdapterView}.
     *
     * @return The number of types of group Views that will be created by this adapter.
     * @see #getChildTypeCount()
     * @see #getGroupType(int)
     */
    int getGroupTypeCount();

    /**
     * <p>
     * Returns the number of types of child Views that will be created by
     * {@link ExpandableListAdapter#getChildView(int, int, boolean, View, ViewGroup)}
     * . Each type represents a set of views that can be converted in
     * {@link ExpandableListAdapter#getChildView(int, int, boolean, View, ViewGroup)}
     * , for any group. If the adapter always returns the same type of View for
     * all child items, this method should return 1.
     * </p>
     * This method will only be called when the adapter is set on the {@link AdapterView}.
     *
     * @return The total number of types of child Views that will be created by this adapter.
     * @see #getGroupTypeCount()
     * @see #getChildType(int, int)
     */
    int getChildTypeCount();
}
