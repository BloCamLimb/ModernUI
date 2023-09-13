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


import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Context;
import icyllis.modernui.util.SparseBooleanArray;
import icyllis.modernui.view.*;

import java.util.regex.Pattern;

/**
 * <p>A layout that arranges its children into rows and columns.
 * A TableLayout consists of a number of {@link TableRow} objects,
 * each defining a row (actually, you can have other children, which will be
 * explained below). TableLayout containers do not display border lines for
 * their rows, columns, or cells. Each row has zero or more cells; each cell can
 * hold one {@link View View} object. The table has as many columns
 * as the row with the most cells. A table can leave cells empty. Cells can span
 * columns, as they can in HTML.</p>
 *
 * <p>The width of a column is defined by the row with the widest cell in that
 * column. However, a TableLayout can specify certain columns as shrinkable or
 * stretchable by calling
 * {@link #setColumnShrinkable(int, boolean) setColumnShrinkable()}
 * or {@link #setColumnStretchable(int, boolean) setColumnStretchable()}. If
 * marked as shrinkable, the column width can be shrunk to fit the table into
 * its parent object. If marked as stretchable, it can expand in width to fit
 * any extra space. The total width of the table is defined by its parent
 * container. It is important to remember that a column can be both shrinkable
 * and stretchable. In such a situation, the column will change its size to
 * always use up the available space, but never more. Finally, you can hide a
 * column by calling
 * {@link #setColumnCollapsed(int, boolean) setColumnCollapsed()}.</p>
 *
 * <p>The children of a TableLayout cannot specify the <code>layout_width</code>
 * attribute. Width is always <code>MATCH_PARENT</code>. However, the
 * <code>layout_height</code> attribute can be defined by a child; default value
 * is {@link TableLayout.LayoutParams#WRAP_CONTENT}. If the child
 * is a {@link TableRow}, then the height is always
 * {@link TableLayout.LayoutParams#WRAP_CONTENT}.</p>
 *
 * <p> Cells must be added to a row in increasing column order, both in code and
 * XML. Column numbers are zero-based. If you don't specify a column number for
 * a child cell, it will autoincrement to the next available column. If you skip
 * a column number, it will be considered an empty cell in that row. See the
 * TableLayout examples in ApiDemos for examples of creating tables in XML.</p>
 *
 * <p>Although the typical child of a TableLayout is a TableRow, you can
 * actually use any View subclass as a direct child of TableLayout. The View
 * will be displayed as a single row that spans all the table columns.</p>
 */
// Modified from Android
public class TableLayout extends LinearLayout {
    private int[] mMaxWidths;
    private SparseBooleanArray mStretchableColumns;
    private SparseBooleanArray mShrinkableColumns;
    private SparseBooleanArray mCollapsedColumns;

    private boolean mShrinkAllColumns;
    private boolean mStretchAllColumns;

    private boolean mInitialized;

    /**
     * <p>Creates a new TableLayout for the given context.</p>
     *
     * @param context the application environment
     */
    public TableLayout(Context context) {
        super(context);
        initTableLayout();
    }

    /**
     * <p>Parses a sequence of columns ids defined in a CharSequence with the
     * following pattern (regex): \d+(\s*,\s*\d+)*</p>
     *
     * <p>Examples: "1" or "13, 7, 6" or "".</p>
     *
     * <p>The result of the parsing is stored in a sparse boolean array. The
     * parsed column ids are used as the keys of the sparse array. The values
     * are always true.</p>
     *
     * @param sequence a sequence of column ids, can be empty but not null
     * @return a sparse array of boolean mapping column indexes to the columns
     * collapse state
     */
    private static SparseBooleanArray parseColumns(String sequence) {
        SparseBooleanArray columns = new SparseBooleanArray();
        Pattern pattern = Pattern.compile("\\s*,\\s*");
        String[] columnDefs = pattern.split(sequence);

        for (String columnIdentifier : columnDefs) {
            try {
                int columnIndex = Integer.parseInt(columnIdentifier);
                // only valid, i.e. positive, columns indexes are handled
                if (columnIndex >= 0) {
                    // putting true in this sparse array indicates that the
                    // column index was defined in the XML file
                    columns.put(columnIndex, true);
                }
            } catch (NumberFormatException e) {
                // we just ignore columns that don't exist
            }
        }

        return columns;
    }

    /**
     * <p>Performs initialization common to prorgrammatic use and XML use of
     * this widget.</p>
     */
    private void initTableLayout() {
        if (mCollapsedColumns == null) {
            mCollapsedColumns = new SparseBooleanArray();
        }
        if (mStretchableColumns == null) {
            mStretchableColumns = new SparseBooleanArray();
        }
        if (mShrinkableColumns == null) {
            mShrinkableColumns = new SparseBooleanArray();
        }

        // TableLayouts are always in vertical orientation; keep this tracked
        // for shared LinearLayout code.
        setOrientation(VERTICAL);

        mInitialized = true;
    }

    @Override
    protected void onViewAdded(View child) {
        super.onViewAdded(child);
        trackCollapsedColumns(child);
    }

    private void requestRowsLayout() {
        if (mInitialized) {
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                getChildAt(i).requestLayout();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestLayout() {
        if (mInitialized) {
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                getChildAt(i).forceLayout();
            }
        }

        super.requestLayout();
    }

    /**
     * <p>Indicates whether all columns are shrinkable or not.</p>
     *
     * @return true if all columns are shrinkable, false otherwise
     */
    public boolean isShrinkAllColumns() {
        return mShrinkAllColumns;
    }

    /**
     * <p>Convenience method to mark all columns as shrinkable.</p>
     *
     * @param shrinkAllColumns true to mark all columns shrinkable
     */
    public void setShrinkAllColumns(boolean shrinkAllColumns) {
        mShrinkAllColumns = shrinkAllColumns;
    }

    /**
     * <p>Indicates whether all columns are stretchable or not.</p>
     *
     * @return true if all columns are stretchable, false otherwise
     */
    public boolean isStretchAllColumns() {
        return mStretchAllColumns;
    }

    /**
     * <p>Convenience method to mark all columns as stretchable.</p>
     *
     * @param stretchAllColumns true to mark all columns stretchable
     */
    public void setStretchAllColumns(boolean stretchAllColumns) {
        mStretchAllColumns = stretchAllColumns;
    }

    /**
     * <p>Collapses or restores a given column. When collapsed, a column
     * does not appear on screen and the extra space is reclaimed by the
     * other columns. A column is collapsed/restored only when it belongs to
     * a {@link TableRow}.</p>
     *
     * <p>Calling this method requests a layout operation.</p>
     *
     * @param columnIndex the index of the column
     * @param isCollapsed true if the column must be collapsed, false otherwise
     */
    public void setColumnCollapsed(int columnIndex, boolean isCollapsed) {
        // update the collapse status of the column
        mCollapsedColumns.put(columnIndex, isCollapsed);

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View view = getChildAt(i);
            if (view instanceof TableRow) {
                ((TableRow) view).setColumnCollapsed(columnIndex, isCollapsed);
            }
        }

        requestRowsLayout();
    }

    /**
     * <p>Returns the collapsed state of the specified column.</p>
     *
     * @param columnIndex the index of the column
     * @return true if the column is collapsed, false otherwise
     */
    public boolean isColumnCollapsed(int columnIndex) {
        return mCollapsedColumns.get(columnIndex);
    }

    /**
     * <p>Makes the given column stretchable or not. When stretchable, a column
     * takes up as much as available space as possible in its row.</p>
     *
     * <p>Calling this method requests a layout operation.</p>
     *
     * @param columnIndex   the index of the column
     * @param isStretchable true if the column must be stretchable,
     *                      false otherwise. Default is false.
     */
    public void setColumnStretchable(int columnIndex, boolean isStretchable) {
        mStretchableColumns.put(columnIndex, isStretchable);
        requestRowsLayout();
    }

    /**
     * <p>Returns whether the specified column is stretchable or not.</p>
     *
     * @param columnIndex the index of the column
     * @return true if the column is stretchable, false otherwise
     */
    public boolean isColumnStretchable(int columnIndex) {
        return mStretchAllColumns || mStretchableColumns.get(columnIndex);
    }

    /**
     * <p>Makes the given column shrinkable or not. When a row is too wide, the
     * table can reclaim extra space from shrinkable columns.</p>
     *
     * <p>Calling this method requests a layout operation.</p>
     *
     * @param columnIndex  the index of the column
     * @param isShrinkable true if the column must be shrinkable,
     *                     false otherwise. Default is false.
     */
    public void setColumnShrinkable(int columnIndex, boolean isShrinkable) {
        mShrinkableColumns.put(columnIndex, isShrinkable);
        requestRowsLayout();
    }

    /**
     * <p>Returns whether the specified column is shrinkable or not.</p>
     *
     * @param columnIndex the index of the column
     * @return true if the column is shrinkable, false otherwise. Default is false.
     */
    public boolean isColumnShrinkable(int columnIndex) {
        return mShrinkAllColumns || mShrinkableColumns.get(columnIndex);
    }

    /**
     * <p>Applies the columns collapse status to a new row added to this
     * table. This method is invoked by PassThroughHierarchyChangeListener
     * upon child insertion.</p>
     *
     * <p>This method only applies to {@link TableRow}
     * instances.</p>
     *
     * @param child the newly added child
     */
    private void trackCollapsedColumns(View child) {
        if (child instanceof final TableRow row) {
            final SparseBooleanArray collapsedColumns = mCollapsedColumns;
            final int count = collapsedColumns.size();
            for (int i = 0; i < count; i++) {
                int columnIndex = collapsedColumns.keyAt(i);
                boolean isCollapsed = collapsedColumns.valueAt(i);
                // the collapse status is set only when the column should be
                // collapsed; otherwise, this might affect the default
                // visibility of the row's children
                if (isCollapsed) {
                    row.setColumnCollapsed(columnIndex, true);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addView(@NonNull View child) {
        super.addView(child);
        requestRowsLayout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addView(@NonNull View child, int index) {
        super.addView(child, index);
        requestRowsLayout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addView(@NonNull View child, @NonNull ViewGroup.LayoutParams params) {
        super.addView(child, params);
        requestRowsLayout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addView(@NonNull View child, int index, @NonNull ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        requestRowsLayout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // enforce vertical layout
        measureVertical(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // enforce vertical layout
        layoutVertical(l, t, r, b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void measureChildBeforeLayout(View child, int childIndex,
                                  int widthMeasureSpec, int totalWidth,
                                  int heightMeasureSpec, int totalHeight) {
        // when the measured child is a table row, we force the width of its
        // children with the widths computed in findLargestCells()
        if (child instanceof TableRow) {
            ((TableRow) child).setColumnsWidthConstraints(mMaxWidths);
        }

        super.measureChildBeforeLayout(child, childIndex,
                widthMeasureSpec, totalWidth, heightMeasureSpec, totalHeight);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void measureVertical(int widthMeasureSpec, int heightMeasureSpec) {
        findLargestCells(widthMeasureSpec, heightMeasureSpec);
        shrinkAndStretchColumns(widthMeasureSpec);

        super.measureVertical(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * <p>Finds the largest cell in each column. For each column, the width of
     * the largest cell is applied to all the other cells.</p>
     *
     * @param widthMeasureSpec the measure constraint imposed by our parent
     */
    private void findLargestCells(int widthMeasureSpec, int heightMeasureSpec) {
        boolean firstRow = true;

        // find the maximum width for each column
        // the total number of columns is dynamically changed if we find
        // wider rows as we go through the children
        // the array is reused for each layout operation; the array can grow
        // but never shrinks. Unused extra cells in the array are just ignored
        // this behavior avoids to unnecessary grow the array after the first
        // layout operation
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            if (child instanceof final TableRow row) {
                // forces the row's height
                final ViewGroup.LayoutParams layoutParams = row.getLayoutParams();
                layoutParams.height = LayoutParams.WRAP_CONTENT;

                final int[] widths = row.getColumnsWidths(widthMeasureSpec, heightMeasureSpec);
                final int newLength = widths.length;
                // this is the first row, we just need to copy the values
                if (firstRow) {
                    if (mMaxWidths == null || mMaxWidths.length != newLength) {
                        mMaxWidths = new int[newLength];
                    }
                    System.arraycopy(widths, 0, mMaxWidths, 0, newLength);
                    firstRow = false;
                } else {
                    int length = mMaxWidths.length;
                    final int difference = newLength - length;
                    // the current row is wider than the previous rows, so
                    // we just grow the array and copy the values
                    if (difference > 0) {
                        final int[] oldMaxWidths = mMaxWidths;
                        mMaxWidths = new int[newLength];
                        System.arraycopy(oldMaxWidths, 0, mMaxWidths, 0,
                                oldMaxWidths.length);
                        System.arraycopy(widths, oldMaxWidths.length,
                                mMaxWidths, oldMaxWidths.length, difference);
                    }

                    // the row is narrower or of the same width as the previous
                    // rows, so we find the maximum width for each column
                    // if the row is narrower than the previous ones,
                    // difference will be negative
                    final int[] maxWidths = mMaxWidths;
                    length = Math.min(length, newLength);
                    for (int j = 0; j < length; j++) {
                        maxWidths[j] = Math.max(maxWidths[j], widths[j]);
                    }
                }
            }
        }
    }

    /**
     * <p>Shrinks the columns if their total width is greater than the
     * width allocated by widthMeasureSpec. When the total width is less
     * than the allocated width, this method attempts to stretch columns
     * to fill the remaining space.</p>
     *
     * @param widthMeasureSpec the width measure specification as indicated
     *                         by this widget's parent
     */
    private void shrinkAndStretchColumns(int widthMeasureSpec) {
        // when we have no row, mMaxWidths is not initialized and the loop
        // below could cause a NPE
        if (mMaxWidths == null) {
            return;
        }

        // should we honor AT_MOST, EXACTLY and UNSPECIFIED?
        int totalWidth = 0;
        for (int width : mMaxWidths) {
            totalWidth += width;
        }

        int size = MeasureSpec.getSize(widthMeasureSpec) - mPaddingLeft - mPaddingRight;

        if ((totalWidth > size) && (mShrinkAllColumns || mShrinkableColumns.size() > 0)) {
            // oops, the largest columns are wider than the row itself
            // fairly redistribute the row's width among the columns
            mutateColumnsWidth(mShrinkableColumns, mShrinkAllColumns, size, totalWidth);
        } else if ((totalWidth < size) && (mStretchAllColumns || mStretchableColumns.size() > 0)) {
            // if we have some space left, we distribute it among the
            // expandable columns
            mutateColumnsWidth(mStretchableColumns, mStretchAllColumns, size, totalWidth);
        }
    }

    private void mutateColumnsWidth(SparseBooleanArray columns,
                                    boolean allColumns, int size, int totalWidth) {
        int skipped = 0;
        final int[] maxWidths = mMaxWidths;
        final int length = maxWidths.length;
        final int count = allColumns ? length : columns.size();
        final int totalExtraSpace = size - totalWidth;
        int extraSpace = totalExtraSpace / count;

        // Column's widths are changed: force child table rows to re-measure.
        // (done by super.measureVertical after shrinkAndStretchColumns.)
        final int nbChildren = getChildCount();
        for (int i = 0; i < nbChildren; i++) {
            View child = getChildAt(i);
            if (child instanceof TableRow) {
                child.forceLayout();
            }
        }

        if (!allColumns) {
            for (int i = 0; i < count; i++) {
                int column = columns.keyAt(i);
                if (columns.valueAt(i)) {
                    if (column < length) {
                        maxWidths[column] += extraSpace;
                    } else {
                        skipped++;
                    }
                }
            }
        } else {
            for (int i = 0; i < count; i++) {
                maxWidths[i] += extraSpace;
            }

            // we don't skip any column so we can return right away
            return;
        }

        if (skipped > 0 && skipped < count) {
            // reclaim any extra space we left to columns that don't exist
            extraSpace = skipped * extraSpace / (count - skipped);
            for (int i = 0; i < count; i++) {
                int column = columns.keyAt(i);
                if (columns.valueAt(i) && column < length) {
                    if (extraSpace > maxWidths[column]) {
                        maxWidths[column] = 0;
                    } else {
                        maxWidths[column] += extraSpace;
                    }
                }
            }
        }
    }

    /**
     * Returns a set of layout parameters with a width of
     * {@link ViewGroup.LayoutParams#MATCH_PARENT},
     * and a height of {@link ViewGroup.LayoutParams#WRAP_CONTENT}.
     */
    @NonNull
    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof TableLayout.LayoutParams;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    protected LinearLayout.LayoutParams generateLayoutParams(@NonNull ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    /**
     * <p>This set of layout parameters enforces the width of each child to be
     * {@link #MATCH_PARENT} and the height of each child to be
     * {@link #WRAP_CONTENT}, but only if the height is not specified.</p>
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public static class LayoutParams extends LinearLayout.LayoutParams {

        /**
         * {@inheritDoc}
         */
        public LayoutParams(int w, int h) {
            super(MATCH_PARENT, h);
        }

        /**
         * {@inheritDoc}
         */
        public LayoutParams(int w, int h, float initWeight) {
            super(MATCH_PARENT, h, initWeight);
        }

        /**
         * <p>Sets the child width to
         * {@link ViewGroup.LayoutParams} and the child height to
         * {@link ViewGroup.LayoutParams#WRAP_CONTENT}.</p>
         */
        public LayoutParams() {
            super(MATCH_PARENT, WRAP_CONTENT);
        }

        /**
         * {@inheritDoc}
         */
        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
            width = MATCH_PARENT;
        }

        /**
         * {@inheritDoc}
         */
        public LayoutParams(MarginLayoutParams source) {
            super(source);
            width = MATCH_PARENT;
            if (source instanceof TableLayout.LayoutParams) {
                weight = ((TableLayout.LayoutParams) source).weight;
            }
        }
    }
}
