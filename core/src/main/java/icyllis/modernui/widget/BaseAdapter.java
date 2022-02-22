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

import icyllis.modernui.util.DataSetObservable;
import icyllis.modernui.util.DataSetObserver;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Common base class of common implementation for an {@link Adapter} that can be
 * used in both {@link ListView} (by implementing the specialized
 * {@link ListAdapter} interface) and {@link Spinner} (by implementing the
 * specialized {@link SpinnerAdapter} interface).
 */
public abstract class BaseAdapter implements ListAdapter, SpinnerAdapter {

    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public void registerDataSetObserver(@Nonnull DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(@Nonnull DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    /**
     * Notifies the attached observers that the underlying data has been changed
     * and any View reflecting the data set should refresh itself.
     */
    public void notifyDataSetChanged() {
        mDataSetObservable.notifyChanged();
    }

    /**
     * Notifies the attached observers that the underlying data is no longer valid
     * or available. Once invoked this adapter is no longer valid and should
     * not report further data set changes.
     */
    public void notifyDataSetInvalidated() {
        mDataSetObservable.notifyInvalidated();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Nullable
    @Override
    public View getDropDownView(int position, @Nullable View convertView, @Nonnull ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return getCount() == 0;
    }
}
