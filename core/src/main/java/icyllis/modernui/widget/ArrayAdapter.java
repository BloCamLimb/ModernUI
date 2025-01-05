/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

import icyllis.modernui.core.Context;
import icyllis.modernui.resources.SystemTheme;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * You can use this adapter to provide views for an {@link AdapterView},
 * Returns a view for each object in a collection of data objects you
 * provide, and can be used with list-based user interface widgets such as
 * {@link ListView} or {@link Spinner}.
 * <p>
 * By default, the array adapter creates a view by calling {@link Object#toString()} on each
 * data object in the collection you provide, and places the result in a {@link TextView}.
 * You may also customize what type of view is used for the data object in the collection.
 * To customize what type of view is used for the data object,
 * override {@link #getView(int, View, ViewGroup)}
 * and inflate a view resource.
 * </p>
 * <p>
 * For an example of using an array adapter with a {@link ListView}, see the
 * <a href="https://developer.android.com/guide/topics/ui/declaring-layout#AdapterViews">
 * Adapter Views</a> guide.
 * </p>
 * <p>
 * For an example of using an array adapter with a {@link Spinner}, see the
 * <a href="https://developer.android.com/guide/topics/ui/controls/spinner">Spinners</a> guide.
 * </p>
 * <p class="note"><strong>Note:</strong>
 * If you are considering using array adapter with a ListView, consider using
 * {@link RecyclerView} instead.
 * RecyclerView offers similar features with better performance and more flexibility than
 * ListView provides.
 * See the
 * <a href="https://developer.android.com/guide/topics/ui/layout/recyclerview">
 * Recycler View</a> guide.</p>
 */
public class ArrayAdapter<T> extends BaseAdapter implements Filterable {

    /**
     * Lock used to modify the content of {@link #mObjects}. Any write operation
     * performed on the array should be synchronized on this lock. This lock is also
     * used by the filter (see {@link #getFilter()} to make a synchronized copy of
     * the original array of data.
     */
    private final Object mLock = new Object();

    private final Context mContext;

    /**
     * Contains the list of objects that represent the data of this ArrayAdapter.
     * The content of this list is referred to as "the array" in the documentation.
     */
    private List<T> mObjects;

    /**
     * Indicates whether or not {@link #notifyDataSetChanged()} must be called whenever
     * {@link #mObjects} is modified.
     */
    private boolean mNotifyOnChange = true;

    // A copy of the original mObjects array, initialized from and then used instead as soon as
    // the mFilter ArrayFilter is used. mObjects will then only contain the filtered values.
    private ArrayList<T> mOriginalValues;
    private ArrayFilter mFilter;

    /**
     * Constructor. This constructor will result in the underlying data collection being
     * immutable, so methods such as {@link #clear()} will throw an exception.
     *
     * @param objects The objects to represent in the ListView.
     */
    public ArrayAdapter(Context context, @Nonnull T[] objects) {
        this(context, Arrays.asList(objects));
    }

    /**
     * Constructor
     *
     * @param objects The objects to represent in the ListView.
     */
    public ArrayAdapter(Context context, @Nonnull List<T> objects) {
        mContext = context;
        mObjects = objects;
    }

    /**
     * Adds the specified object at the end of the array.
     *
     * @param object The object to add at the end of the array.
     * @throws UnsupportedOperationException if the underlying data collection is immutable
     */
    public void add(@Nullable T object) {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                mOriginalValues.add(object);
            } else {
                mObjects.add(object);
            }
        }
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    /**
     * Adds the specified Collection at the end of the array.
     *
     * @param collection The Collection to add at the end of the array.
     * @throws UnsupportedOperationException if the <tt>addAll</tt> operation
     *                                       is not supported by this list
     * @throws ClassCastException            if the class of an element of the specified
     *                                       collection prevents it from being added to this list
     * @throws NullPointerException          if the specified collection contains one
     *                                       or more null elements and this list does not permit null
     *                                       elements, or if the specified collection is null
     * @throws IllegalArgumentException      if some property of an element of the
     *                                       specified collection prevents it from being added to this list
     */
    public void addAll(@Nonnull Collection<? extends T> collection) {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                mOriginalValues.addAll(collection);
            } else {
                mObjects.addAll(collection);
            }
        }
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    /**
     * Adds the specified items at the end of the array.
     *
     * @param items The items to add at the end of the array.
     * @throws UnsupportedOperationException if the underlying data collection is immutable
     */
    public void addAll(@Nonnull T[] items) {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                Collections.addAll(mOriginalValues, items);
            } else {
                Collections.addAll(mObjects, items);
            }
        }
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    /**
     * Inserts the specified object at the specified index in the array.
     *
     * @param object The object to insert into the array.
     * @param index  The index at which the object must be inserted.
     * @throws UnsupportedOperationException if the underlying data collection is immutable
     */
    public void insert(@Nullable T object, int index) {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                mOriginalValues.add(index, object);
            } else {
                mObjects.add(index, object);
            }
        }
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    /**
     * Removes the specified object from the array.
     *
     * @param object The object to remove.
     * @throws UnsupportedOperationException if the underlying data collection is immutable
     */
    public void remove(@Nullable T object) {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                mOriginalValues.remove(object);
            } else {
                mObjects.remove(object);
            }
        }
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    /**
     * Remove all elements from the list.
     *
     * @throws UnsupportedOperationException if the underlying data collection is immutable
     */
    public void clear() {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                mOriginalValues.clear();
            } else {
                mObjects.clear();
            }
        }
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    /**
     * Sorts the content of this adapter using the specified comparator.
     *
     * @param comparator The comparator used to sort the objects contained
     *                   in this adapter.
     */
    public void sort(@Nonnull Comparator<? super T> comparator) {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                mOriginalValues.sort(comparator);
            } else {
                mObjects.sort(comparator);
            }
        }
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        mNotifyOnChange = true;
    }

    /**
     * Control whether methods that change the list ({@link #add}, {@link #addAll(Collection)},
     * {@link #addAll(Object[])}, {@link #insert}, {@link #remove}, {@link #clear},
     * {@link #sort(Comparator)}) automatically call {@link #notifyDataSetChanged}.  If set to
     * false, caller must manually call notifyDataSetChanged() to have the changes
     * reflected in the attached view.
     * <p>
     * The default is true, and calling notifyDataSetChanged()
     * resets the flag to true.
     *
     * @param notifyOnChange if true, modifications to the list will
     *                       automatically call {@link
     *                       #notifyDataSetChanged}
     */
    public void setNotifyOnChange(boolean notifyOnChange) {
        mNotifyOnChange = notifyOnChange;
    }

    @Override
    public int getCount() {
        return mObjects.size();
    }

    @Override
    public T getItem(int position) {
        return mObjects.get(position);
    }

    /**
     * Returns the position of the specified item in the array.
     *
     * @param item The item to retrieve the position of.
     * @return The position of the specified item.
     */
    public int getPosition(@Nullable T item) {
        return mObjects.indexOf(item);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Nonnull
    @Override
    public View getView(int position, @Nullable View convertView, @Nonnull ViewGroup parent) {
        return createViewInner(position, convertView, false);
    }

    @Nonnull
    private View createViewInner(int position, @Nullable View convertView, boolean dropdown) {
        final TextView tv;

        if (convertView == null) {
            tv = new TextView(mContext);
        } else {
            tv = (TextView) convertView;
        }

        final T item = getItem(position);
        if (item instanceof CharSequence) {
            tv.setText((CharSequence) item);
        } else {
            tv.setText(String.valueOf(item));
        }

        var theme = SystemTheme.currentTheme();
        tv.setTextSize(14);
        if (dropdown) {
            tv.setTextColor(theme.textColorPrimaryDisableOnly);
        } else {
            tv.setTextColor(theme.textColorPrimary);
        }
        if (!dropdown) {
            tv.setTextAlignment(View.TEXT_ALIGNMENT_INHERIT);
        } // else GRAVITY
        final int dp8 = tv.dp(8);
        tv.setPadding(dp8, 0, dp8, 0);
        if (dropdown) {
            tv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, tv.dp(32)));
        } // else default START|TOP and (MATCH_PARENT,WRAP_CONTENT)

        return tv;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @Nonnull ViewGroup parent) {
        return createViewInner(position, convertView, true);
    }

    @Nonnull
    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new ArrayFilter();
        }
        return mFilter;
    }

    /**
     * <p>An array filter constrains the content of the array adapter with
     * a prefix. Each item that does not start with the supplied prefix
     * is removed from the list.</p>
     */
    private class ArrayFilter extends Filter {

        @Nonnull
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            final FilterResults results = new FilterResults();

            if (mOriginalValues == null) {
                synchronized (mLock) {
                    mOriginalValues = new ArrayList<>(mObjects);
                }
            }

            if (prefix == null || prefix.length() == 0) {
                final ArrayList<T> list;
                synchronized (mLock) {
                    list = new ArrayList<>(mOriginalValues);
                }
                results.values = list;
                results.count = list.size();
            } else {
                final String prefixString = prefix.toString().toLowerCase();

                final ArrayList<T> values;
                synchronized (mLock) {
                    values = new ArrayList<>(mOriginalValues);
                }

                final ArrayList<T> newValues = new ArrayList<>();

                for (final T value : values) {
                    final String valueText = value.toString().toLowerCase();

                    // First match against the whole, non-splitted value
                    if (valueText.startsWith(prefixString)) {
                        newValues.add(value);
                    } else {
                        final String[] words = valueText.split(" ");
                        for (String word : words) {
                            if (word.startsWith(prefixString)) {
                                newValues.add(value);
                                break;
                            }
                        }
                    }
                }

                results.values = newValues;
                results.count = newValues.size();
            }

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, @Nonnull FilterResults results) {
            mObjects = (List<T>) results.values;
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}
