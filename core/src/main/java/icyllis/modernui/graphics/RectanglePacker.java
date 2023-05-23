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

package icyllis.modernui.graphics;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;

import java.util.*;

/**
 * Packs rectangles into a larger rectangle.
 */
public abstract class RectanglePacker {

    /**
     * Available rectangle packing algorithms, skyline is always recommended.
     */
    public static final int ALGORITHM_SKYLINE = 0;
    public static final int ALGORITHM_HORIZON = 1;
    public static final int ALGORITHM_HORIZON_OLD = 2;
    public static final int ALGORITHM_BINARY_TREE = 3;
    public static final int ALGORITHM_POWER2_LINE = 4;

    protected final short mWidth;
    protected final short mHeight;

    protected int mArea;

    /**
     * Constructor assumes both width and height > 0 && <= 32767.
     *
     * @param width  the max width
     * @param height the max height
     */
    public RectanglePacker(int width, int height) {
        assert width > 0 && height > 0;
        assert width <= Short.MAX_VALUE && height <= Short.MAX_VALUE;
        mWidth = (short) width;
        mHeight = (short) height;
    }

    /**
     * Creates a rectangle packer using the best algorithm.
     *
     * @param width  the max width, typical value is 256, 512 or 1024
     * @param height the max height, typical value is 256, 512 or 1024
     */
    @NonNull
    public static RectanglePacker make(int width, int height) {
        // skyline has the best coverage, and is fast enough
        return new Skyline(width, height);
    }

    public static RectanglePacker make(int width, int height, int algorithm) {
        return switch (algorithm) {
            case ALGORITHM_SKYLINE -> new Skyline(width, height);
            case ALGORITHM_HORIZON -> new Horizon(width, height);
            case ALGORITHM_HORIZON_OLD -> new HorizonOld(width, height);
            case ALGORITHM_BINARY_TREE -> new BinaryTree(width, height);
            case ALGORITHM_POWER2_LINE -> new Power2Line(width, height);
            default -> throw new AssertionError(algorithm);
        };
    }

    public final int getWidth() {
        return mWidth;
    }

    public final int getHeight() {
        return mHeight;
    }

    /**
     * Clears all the tracked data.
     */
    public abstract void clear();

    /**
     * Decides upon an (x, y) position for the given rectangle (leaving
     * its width and height unchanged).
     *
     * @return true on success; false on failure
     */
    public abstract boolean addRect(@NonNull Rect rect);

    /**
     * Gets the ratio of current area to total area, higher values indicate better packing.
     *
     * @return a fractional value
     */
    public final float getCoverage() {
        return mArea / ((float) mWidth * mHeight);
    }

    /**
     * Tracks the current silhouette, optimized.
     */
    public static final class Skyline extends RectanglePacker {

        private static final int X = 0;
        private static final int Y = 1;
        private static final int WIDTH = 2;
        private static final int COLUMNS = 3;

        // skylines
        private short[] mData;
        private int mSize;

        /**
         * Constructor assumes both width and height > 0 && <= 32767.
         *
         * @param width  the max width
         * @param height the max height
         */
        public Skyline(int width, int height) {
            super(width, height);
            mData = new short[10 * COLUMNS];
            mData[WIDTH] = mWidth;
            mSize = 1;
        }

        @Override
        public void clear() {
            mArea = 0;
            mData[X] = 0;
            mData[Y] = 0;
            mData[WIDTH] = mWidth;
            mSize = 1;
        }

        @Override
        public boolean addRect(@NonNull Rect rect) {
            final int width = rect.width();
            final int height = rect.height();
            assert width > 0 && height > 0;
            if (width > mWidth || height > mHeight) {
                return false;
            }

            // find position for new rectangle
            int bestWidth = mWidth + 1;
            int bestX = 0;
            int bestY = mHeight + 1;
            int bestIndex = -1;
            final var data = mData;
            FITTING:
            for (int index = 0, limit = mSize * COLUMNS; index < limit; index += COLUMNS) {
                // Can a width x height rectangle fit in the free space represented by
                // the skyline segments >= 'index'?
                int x = data[index + X];
                if (x + width > mWidth) {
                    continue;
                }

                int y = data[index + Y];
                for (int i = index, widthLeft = width; widthLeft > 0; ) {
                    y = Math.max(y, data[i + Y]);
                    if (y + height > mHeight) {
                        continue FITTING;
                    }
                    widthLeft -= data[i + WIDTH];
                    i += COLUMNS;
                    assert (i < limit || widthLeft <= 0);
                }

                // minimize y position first, then width of skyline
                if (y < bestY || (y == bestY && data[index + WIDTH] < bestWidth)) {
                    bestIndex = index;
                    bestWidth = data[index + WIDTH];
                    bestX = x;
                    bestY = y;
                }
            }

            if (bestIndex != -1) {
                addLevel(bestIndex, bestX, bestY, width, height);
                rect.offsetTo(bestX, bestY);

                mArea += width * height;
                return true;
            }

            return false;
        }

        // Update the skyline structure to include a width x height rect located
        // at x,y.
        private void addLevel(int index, int x, int y, int width, int height) {
            assert x + width <= mWidth;
            assert y + height <= mHeight;
            int s = mSize;
            if (s * COLUMNS == mData.length) {
                int newSize = (s + (s >> 1)) * COLUMNS;
                mData = Arrays.copyOf(mData, newSize);
            }
            final var data = mData;
            System.arraycopy(data, index,
                    data, index + COLUMNS, s * COLUMNS - index);
            data[index + X] = (short) x;
            data[index + Y] = (short) (y + height);
            data[index + WIDTH] = (short) width;
            mSize = s + 1;

            // delete width of the new skylines from following ones
            for (int i = index + COLUMNS; i < mSize * COLUMNS; ) {
                // The new segment subsumes all or part of i
                assert data[i + X] >= data[i - COLUMNS + X];

                if (data[i + X] < data[i - COLUMNS + X] + data[i - COLUMNS + WIDTH]) {
                    int shrink = data[i - COLUMNS + X] + data[i - COLUMNS + WIDTH] - data[i + X];

                    data[i + X] += shrink;
                    data[i + WIDTH] -= shrink;

                    if (data[i + WIDTH] <= 0) {
                        // fully consumed, remove i
                        System.arraycopy(data, i + COLUMNS,
                                data, i, (mSize - 1) * COLUMNS - i);
                        --mSize;
                    } else {
                        // only partially consumed
                        break;
                    }
                } else {
                    break;
                }
            }

            // merge skylines
            for (int i = 0; i < (mSize - 1) * COLUMNS; ) {
                if (data[i + Y] == data[i + COLUMNS + Y]) {
                    data[i + WIDTH] += data[i + COLUMNS + WIDTH];
                    // remove i + 1
                    System.arraycopy(data, i + 2 * COLUMNS,
                            data, i + COLUMNS, (mSize - 2) * COLUMNS - i);
                    --mSize;
                } else {
                    i += COLUMNS;
                }
            }
        }
    }

    // also good, faster than skyline but less efficient
    public static final class Horizon extends RectanglePacker {

        // Maintained in sorted order by increasing Y coordinate
        private final List<Level> mLevels = new ArrayList<>();
        private static final int MIN_HEIGHT = 8; // The minimum size of level
        private static final int ROUND_UP = 4; // Round up to multiple of 4
        private int mRecentUsedLevelIndex = 0;
        private int mHeightOffset;

        /**
         * Constructor assumes both width and height > 0 && <= 32767.
         *
         * @param width  the max width
         * @param height the max height
         */
        public Horizon(int width, int height) {
            super(width, height);
        }

        @Override
        public void clear() {
            mArea = 0;
            mLevels.clear();
            mHeightOffset = 0;
            mRecentUsedLevelIndex = 0;
        }

        @Override
        public boolean addRect(@NonNull Rect rect) {
            final int width = rect.width();
            final int height = rect.height();
            assert width > 0 && height > 0;
            if (width > mWidth || height > mHeight) {
                return false;
            }

            int newHeight = MathUtil.alignUp(Math.max(MIN_HEIGHT, height), ROUND_UP);

            int newIndex;
            // If it does not match recent used level, using binary search to find
            // the best fit level's index
            if (mRecentUsedLevelIndex < mLevels.size() &&
                    mLevels.get(mRecentUsedLevelIndex).height != newHeight) {
                newIndex = binarySearch(mLevels, newHeight);
            } else {
                newIndex = mRecentUsedLevelIndex;
            }

            // Can create a new level with newHeight
            final boolean newLevelFlag = mHeightOffset + newHeight <= mHeight;

            // Go through the levels check whether we can satisfy the allocation
            // request
            for (int i = newIndex, max = mLevels.size(); i < max; i++) {
                Level level = mLevels.get(i);
                // If level's height is more than (newHeight + ROUND_UP * 2) and
                // the cache still has some space left, go create a new level
                if (level.height > (newHeight + ROUND_UP * 2) && newLevelFlag) {
                    break;
                } else if (level.add(rect, mWidth, width, height)) {
                    mRecentUsedLevelIndex = i;
                    mArea += width * height;
                    return true;
                }
            }

            // Try to add a new Level.
            if (!newLevelFlag) {
                return false;
            }

            Level newLevel = new Level(mHeightOffset, newHeight);
            mHeightOffset += newHeight;

            // For a rect that cannot fit into the existing level, create a new
            // level and add at the end of levels that have the same height
            if (newIndex < mLevels.size() && mLevels.get(newIndex).height <= newHeight) {
                mLevels.add(newIndex + 1, newLevel);
                mRecentUsedLevelIndex = newIndex + 1;
            } else {
                mLevels.add(newIndex, newLevel);
                mRecentUsedLevelIndex = newIndex;
            }
            if (newLevel.add(rect, mWidth, width, height)) {
                mArea += width * height;
                return true;
            } else {
                return false;
            }
        }

        /**
         * Using binary search to find the last index of best fit level for k,
         * where k is a rounded-up value.
         */
        private static int binarySearch(List<Level> levels, int k) {

            // k+1 is used to find the last index of the level with height of k. Because of rounding up, more
            // likely, there are a bunch of levels has the same height. But, we always keep adding levels and
            // rects at the end. k+1 is a trick to find the last index by finding the next greater value's index
            // and go back one.
            // Note that since the sizes are quantized, k+1 is a special value that will not appear in the list
            // of level sizes and so the search for it will find the gap between the size for k and the size
            // for the next quantum.
            int key = k + 1;
            int from = 0, to = levels.size() - 1;
            int mid = 0;
            int midSize = 0;

            if (to < 0) {
                return 0;
            }

            while (from <= to) {
                mid = (from + to) / 2;
                midSize = levels.get(mid).height;
                if (key < midSize) {
                    to = mid - 1;
                } else {
                    from = mid + 1;
                }
            }

            if (midSize < k) {
                return mid + 1;
            } else if (midSize > k) {
                return mid > 0 ? mid - 1 : 0;
            } else {
                return mid;
            }
        }

        private static final class Level {

            private final int y;
            private final int height;
            private int x;

            Level(int y, int height) {
                this.y = y;
                this.height = height;
            }

            /**
             * Tries to add the given rectangle to this level.
             */
            boolean add(Rect rect, int levelWidth, int requestedLength, int requestedSize) {
                // See whether we can add at the end
                if (x + requestedLength <= levelWidth && requestedSize <= height) {
                    rect.offsetTo(x, y);
                    x += requestedLength;
                    return true;
                }
                return false;
            }
        }
    }

    // fastest, only good for rectangles that have similar heights
    public static final class HorizonOld extends RectanglePacker {

        public static final int INITIAL_SIZE = 512;

        private int mPosX;
        private int mPosY;

        // max height of current line
        private int mLineHeight;

        private int mCurrWidth;
        private int mCurrHeight;

        /**
         * Constructor assumes both width and height > 0 && <= 32767.
         *
         * @param width  the max width
         * @param height the max height
         */
        public HorizonOld(int width, int height) {
            super(width, height);
        }

        @Override
        public void clear() {
            mArea = 0;
            mPosX = 0;
            mPosY = 0;
            mLineHeight = 0;
            mCurrWidth = 0;
            mCurrHeight = 0;
        }

        @Override
        public boolean addRect(@NonNull Rect rect) {
            final int width = rect.width();
            final int height = rect.height();
            assert width > 0 && height > 0;
            if (width > mWidth || height > mHeight) {
                return false;
            }
            if (mCurrWidth == 0) {
                resize(); // first init
            }
            if (mPosX + width >= mCurrWidth) {
                mPosX = 0;
                // we are on the right half
                if (mCurrWidth == mCurrHeight && mCurrWidth != INITIAL_SIZE) {
                    mPosX += mCurrWidth >> 1;
                }
                mPosY += mLineHeight;
                mLineHeight = 0;
            }
            if (mPosY + height >= mCurrHeight) {
                // move to the right half
                if (mCurrWidth != mCurrHeight) {
                    mPosX = mCurrWidth;
                    mPosY = 0;
                }
                if (!resize()) {
                    return false;
                }
            }
            rect.offsetTo(mPosX, mPosY);

            mPosX += width;
            mLineHeight = Math.max(mLineHeight, height);
            mArea += width * height;
            return true;
        }

        private boolean resize() {
            // never initialized
            if (mCurrWidth == 0) {
                mCurrWidth = mCurrHeight = INITIAL_SIZE;
            } else {
                if (mCurrWidth == super.mWidth && mCurrHeight == super.mHeight) {
                    return false;
                }
                if (mCurrHeight != mCurrWidth) {
                    mCurrWidth <<= 1;
                } else {
                    mCurrHeight <<= 1;
                }
            }
            return true;
        }
    }

    // very slow when width and height is big (>512)
    public static final class BinaryTree extends RectanglePacker {

        private Node mRoot;

        /**
         * Constructor assumes both width and height > 0 && <= 32767.
         *
         * @param width  the max width
         * @param height the max height
         */
        public BinaryTree(int width, int height) {
            super(width, height);
            mRoot = new Node(0, 0, width, height);
        }

        @Override
        public void clear() {
            mArea = 0;
            mRoot = new Node(0, 0, mWidth, mHeight);
        }

        @Override
        public boolean addRect(@NonNull Rect rect) {
            final int width = rect.width();
            final int height = rect.height();
            assert width > 0 && height > 0;
            if (width > mWidth || height > mHeight) {
                return false;
            }
            Node node = mRoot.insert(width, height);
            if (node != null) {
                rect.offsetTo(node.x, node.y);
                mArea += width * height;
                return true;
            } else {
                return false;
            }
        }

        private static final class Node {

            private final int x;
            private final int y;
            private final int width;
            private final int height;
            @Nullable
            private Node left;
            @Nullable
            private Node right;
            private boolean filled;

            Node(int x, int y, int width, int height) {
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;
            }

            @Nullable
            Node insert(int width, int height) {
                if (left != null && right != null) {
                    Node node = left.insert(width, height);
                    if (node == null) {
                        node = right.insert(width, height);
                    }
                    return node;
                } else if (filled) {
                    return null;
                } else if (width <= this.width && height <= this.height) {
                    if (width == this.width && height == this.height) {
                        filled = true;
                        return this;
                    } else {
                        int widthLeft = this.width - width;
                        int heightLeft = this.height - height;
                        if (widthLeft > heightLeft) {
                            left = new Node(this.x, this.y, width, this.height);
                            right = new Node(this.x + width + 1, this.y,
                                    this.width - width - 1, this.height);
                        } else {
                            left = new Node(this.x, this.y, this.width, height);
                            right = new Node(this.x, this.y + height + 1,
                                    this.width, this.height - height - 1);
                        }
                        return left.insert(width, height);
                    }
                }
                return null;
            }
        }
    }

    public static final class Power2Line extends RectanglePacker {

        private int mNextStripY;

        private static final class Row {
            private int x;
            private int y;
            private int height;

            boolean canAddWidth(int width, int containerWidth) {
                return x + width <= containerWidth;
            }
        }

        private final Row[] mRows = new Row[16];

        {
            for (int i = 0; i < 16; i++) {
                mRows[i] = new Row();
            }
        }

        public Power2Line(int width, int height) {
            super(width, height);
        }

        @Override
        public void clear() {
            mArea = 0;
            mNextStripY = 0;
            for (int i = 0; i < 16; i++) {
                mRows[i] = new Row();
            }
        }

        @Override
        public boolean addRect(@NonNull Rect rect) {
            final int width = rect.width();
            int height = rect.height();
            assert width > 0 && height > 0;
            if (width > mWidth || height > mHeight) {
                return false;
            }

            int area = width * height; // computed here since height will be modified

            height = MathUtil.ceilPow2(height);

            height = Math.max(height, 2);
            Row row = mRows[toRowIndex(height)];
            assert (row.height == 0 || row.height == height);

            if (0 == row.height) {
                if (mNextStripY + height > mHeight) {
                    return false;
                }
                initRow(row, height);
            } else {
                if (!row.canAddWidth(width, mWidth)) {
                    if (mNextStripY + height > mHeight) {
                        return false;
                    }
                    // that row is now "full", so retarget our Row record for
                    // another one
                    initRow(row, height);
                }
            }

            assert (row.height == height);
            assert (row.canAddWidth(width, mWidth));
            rect.offsetTo(row.x, row.y);
            row.x += width;

            assert (row.x <= mWidth);
            assert (row.y <= mHeight);
            assert (mNextStripY <= mHeight);
            mArea += area;
            return true;
        }

        void initRow(Row row, int rowHeight) {
            row.x = 0;
            row.y = mNextStripY;
            row.height = rowHeight;
            mNextStripY += rowHeight;
        }

        static int toRowIndex(int height) {
            assert (height >= 2);
            int index = 32 - Integer.numberOfLeadingZeros(height - 1);
            assert (index < 16);
            return index;
        }
    }
}
