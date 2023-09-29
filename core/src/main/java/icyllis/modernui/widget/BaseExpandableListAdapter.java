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

import icyllis.modernui.util.DataSetObservable;
import icyllis.modernui.util.DataSetObserver;

/**
 * Base class for a {@link ExpandableListAdapter} used to provide data and Views
 * from some data to an expandable list view.
 * <p>
 * Adapters inheriting this class should verify that the base implementations of
 * {@link #getCombinedChildId(long, long)} and {@link #getCombinedGroupId(long)}
 * are correct in generating unique IDs from the group/children IDs.
 * <p>
 */
public abstract class BaseExpandableListAdapter implements ExpandableListAdapter,
        HeterogeneousExpandableList {
    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    /**
     * @see DataSetObservable#notifyInvalidated()
     */
    public void notifyDataSetInvalidated() {
        mDataSetObservable.notifyInvalidated();
    }

    /**
     * @see DataSetObservable#notifyChanged()
     */
    public void notifyDataSetChanged() {
        mDataSetObservable.notifyChanged();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
    }

    /**
     * Override this method if you foresee a clash in IDs based on this scheme:
     * <p>
     * Base implementation returns a long:
     * <li> bit 0: Whether this ID points to a child (unset) or group (set), so for this method
     * this bit will be 1.
     * <li> bit 1-31: Lower 31 bits of the groupId
     * <li> bit 32-63: Lower 32 bits of the childId.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public long getCombinedChildId(long groupId, long childId) {
        return 0x8000000000000000L | ((groupId & 0x7FFFFFFF) << 32) | (childId & 0xFFFFFFFFL);
    }

    /**
     * Override this method if you foresee a clash in IDs based on this scheme:
     * <p>
     * Base implementation returns a long:
     * <li> bit 0: Whether this ID points to a child (unset) or group (set), so for this method
     * this bit will be 0.
     * <li> bit 1-31: Lower 31 bits of the groupId
     * <li> bit 32-63: Lower 32 bits of the childId.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public long getCombinedGroupId(long groupId) {
        return (groupId & 0x7FFFFFFF) << 32;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return getGroupCount() == 0;
    }


    /**
     * {@inheritDoc}
     *
     * @return 0 for any group or child position, since only one child type count is declared.
     */
    @Override
    public int getChildType(int groupPosition, int childPosition) {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @return 1 as a default value in BaseExpandableListAdapter.
     */
    @Override
    public int getChildTypeCount() {
        return 1;
    }

    /**
     * {@inheritDoc}
     *
     * @return 0 for any groupPosition, since only one group type count is declared.
     */
    @Override
    public int getGroupType(int groupPosition) {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @return 1 as a default value in BaseExpandableListAdapter.
     */
    @Override
    public int getGroupTypeCount() {
        return 1;
    }
}
