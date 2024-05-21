/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.core;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

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
    public static final int ALGORITHM_SKYLINE_NEW = 7;

    protected final int mWidth;
    protected final int mHeight;

    protected int mArea;

    /**
     * Constructor assumes both width and height are in the range 1..32768.
     *
     * @param width  the max width
     * @param height the max height
     */
    protected RectanglePacker(int width, int height) {
        if (width < 1 || height < 1 || width > 32768 || height > 32768) {
            throw new IllegalArgumentException("width " + width + " or height " + height +
                    " is out of range 1..32768");
        }
        mWidth = width;
        mHeight = height;
    }

    /**
     * Creates a rectangle packer using the best algorithm.
     *
     * @param width  the max width, typical value is 256, 512 or 1024
     * @param height the max height, typical value is 256, 512 or 1024
     */
    public static RectanglePacker make(int width, int height) {
        // skyline has the best coverage, and is fast enough
        return new Skyline(width, height);
    }

    // test only
    public static RectanglePacker make(int width, int height, int algorithm) {
        return switch (algorithm) {
            case ALGORITHM_SKYLINE -> new Skyline(width, height);
            case ALGORITHM_HORIZON -> new Horizon(width, height);
            case ALGORITHM_HORIZON_OLD -> new HorizonOld(width, height);
            case ALGORITHM_BINARY_TREE -> new BinaryTree(width, height);
            case ALGORITHM_POWER2_LINE -> new Power2Line(width, height);
            case ALGORITHM_SKYLINE_NEW -> new SkylineNew(width, height);
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
    public abstract boolean addRect(Rect2i rect);

    /**
     * Returns the ratio of the used area to the maximum area, higher is better.
     *
     * @return the ratio of coverage, 0..1
     */
    public final double getCoverage() {
        return (double) mArea / mWidth / mHeight;
    }

    /**
     * For native rectangle packer, this must be called to free its native resources.
     */
    public void free() {
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
        private final short[] mData;
        private int mSize;

        /**
         * Constructor assumes both width and height are in the range 1..32768.
         *
         * @param width  the max width
         * @param height the max height
         */
        public Skyline(int width, int height) {
            super(width, height);
            mData = new short[(width + 10) * COLUMNS];
            mData[WIDTH] = (short) mWidth;
            mSize = 1;
        }

        @Override
        public void clear() {
            mArea = 0;
            mData[X] = 0;
            mData[Y] = 0;
            mData[WIDTH] = (short) mWidth;
            mSize = 1;
        }

        @Override
        public boolean addRect(Rect2i rect) {
            final int width = rect.width();
            final int height = rect.height();
            if (width <= 0 || height <= 0) {
                rect.offsetTo(0, 0);
                return true;
            }
            if (width > mWidth || height > mHeight) {
                return false;
            }

            // find position for new rectangle
            int bestWidth = mWidth + 1;
            int bestX = 0;
            int bestY = mHeight + 1;
            int bestIndex = -1;
            final short[] data = mData;
            FITTING:
            for (int index = 0, limit = mSize * COLUMNS; index < limit; index += COLUMNS) {
                // Can a width x height rectangle fit in the free space represented by
                // the skyline segments >= 'index'?
                int x = data[index + X] & 0xFFFF;
                if (x + width > mWidth) {
                    continue;
                }

                int y = data[index + Y] & 0xFFFF;
                for (int i = index, widthLeft = width; widthLeft > 0; ) {
                    y = Math.max(y, data[i + Y] & 0xFFFF);
                    if (y + height > mHeight) {
                        continue FITTING;
                    }
                    widthLeft -= data[i + WIDTH] & 0xFFFF;
                    i += COLUMNS;
                    assert (i < limit || widthLeft <= 0);
                }

                // minimize y position first, then width of skyline
                int w = data[index + WIDTH] & 0xFFFF;
                if (y < bestY || (y == bestY && w < bestWidth)) {
                    bestIndex = index;
                    bestWidth = w;
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
            final short[] data = mData;
            System.arraycopy(data, index,
                    data, index + COLUMNS,
                    mSize * COLUMNS - index);
            data[index + X] = (short) x;
            data[index + Y] = (short) (y + height);
            data[index + WIDTH] = (short) width;
            ++mSize;

            // delete width of the new skylines from following ones
            for (int i = index + COLUMNS; i < mSize * COLUMNS; ) {
                // The new segment subsumes all or part of i
                int cx = (data[i + X] & 0xFFFF);
                int px = (data[i - COLUMNS + X] & 0xFFFF);
                assert cx >= px;
                int cw = (data[i + WIDTH] & 0xFFFF);
                int pw = (data[i - COLUMNS + WIDTH] & 0xFFFF);
                int shrink = px + pw - cx;
                if (shrink > 0) {
                    int nw = cw - shrink;
                    if (nw <= 0) {
                        // fully consumed, remove i
                        System.arraycopy(data, i + COLUMNS,
                                data, i,
                                (mSize - 1) * COLUMNS - i);
                        --mSize;
                    } else {
                        // only partially consumed
                        data[i + X] = (short) (cx + shrink);
                        data[i + WIDTH] = (short) nw;
                        break;
                    }
                } else {
                    break;
                }
            }

            // merge skylines
            for (int i = 0; i < (mSize - 1) * COLUMNS; ) {
                if (data[i + Y] == data[i + COLUMNS + Y]) {
                    data[i + WIDTH] = (short) ((data[i + WIDTH] & 0xFFFF) + (data[i + COLUMNS + WIDTH] & 0xFFFF));
                    // remove i + 1
                    System.arraycopy(data, i + 2 * COLUMNS,
                            data, i + COLUMNS,
                            (mSize - 2) * COLUMNS - i);
                    --mSize;
                } else {
                    i += COLUMNS;
                }
            }
        }
    }

    private static final class SkylineNew extends RectanglePacker {

        private static final int X_SHIFT = 0;
        private static final int Y_SHIFT = 16;
        private static final int WIDTH_SHIFT = 32;
        private static final int NEXT_SHIFT = 48;
        // each value is uint16
        private static final int SLOT_MASK = 0xFFFF;

        // 'next' points to 'null'
        private static final int NULL_PTR = 0xFFFF;

        // skylines
        private final long[] mData;

        private int mActiveHead;
        private int mFreeHead;

        private SkylineNew(int width, int height) {
            super(width, height);
            mData = new long[width + 10];
            clear();
        }

        @Override
        public void clear() {
            mArea = 0;
            mData[mActiveHead = 0] = ((long) mWidth << WIDTH_SHIFT) | ((long) NULL_PTR << NEXT_SHIFT);
            for (int i = mFreeHead = 1, e = mData.length; i < e; i++) {
                mData[i] = (long) (i + 1) << NEXT_SHIFT;
            }
        }

        @Override
        public boolean addRect(Rect2i rect) {
            final int width = rect.width();
            final int height = rect.height();
            if (width <= 0 || height <= 0) {
                rect.offsetTo(0, 0);
                return true;
            }
            if (width > mWidth || height > mHeight) {
                return false;
            }

            // find position for new rectangle
            int bestWidth = mWidth + 1;
            int bestX = 0;
            int bestY = mHeight + 1;
            int bestIndex = -1;
            int bestPrev = -1;
            final long[] data = mData;
            int index = mActiveHead;
            int prev = NULL_PTR;
            FITTING:
            for (; index != NULL_PTR; prev = index, index = (int) (data[prev] >>> NEXT_SHIFT)) {
                // Can a width x height rectangle fit in the free space represented by
                // the skyline segments >= 'index'?
                long value = data[index];
                int x = (int) ((value >> X_SHIFT) & SLOT_MASK);
                if (x + width > mWidth) {
                    continue;
                }

                int y = (int) ((value >> Y_SHIFT) & SLOT_MASK);
                for (int i = index, widthLeft = width; widthLeft > 0; ) {
                    long v = data[i];
                    y = Math.max(y, (int) ((v >> Y_SHIFT) & SLOT_MASK));
                    if (y + height > mHeight) {
                        continue FITTING;
                    }
                    widthLeft -= (int) ((v >> WIDTH_SHIFT) & SLOT_MASK);
                    i = (int) (v >>> NEXT_SHIFT);
                    assert (i != NULL_PTR || widthLeft <= 0);
                }

                // minimize y position first, then width of skyline
                int w = (int) ((value >> WIDTH_SHIFT) & SLOT_MASK);
                if (y < bestY || (y == bestY && w < bestWidth)) {
                    bestIndex = index;
                    bestPrev = prev;
                    bestWidth = w;
                    bestX = x;
                    bestY = y;
                }
            }

            if (bestIndex != -1) {
                addLevel(bestPrev, bestIndex, bestX, bestY, width, height);
                rect.offsetTo(bestX, bestY);

                mArea += width * height;
                return true;
            }

            return false;
        }

        // Update the skyline structure to include a width x height rect located
        // at x,y.
        private void addLevel(int prev, int index, int x, int y, int width, int height) {
            assert x + width <= mWidth;
            assert y + height <= mHeight;

            final long[] data = mData;
            // insert at index
            {
                int freeIndex = mFreeHead;
                assert freeIndex != NULL_PTR;
                mFreeHead = (int) (data[freeIndex] >>> NEXT_SHIFT);
                data[freeIndex] = ((long) x << X_SHIFT) | ((long) (y + height) << Y_SHIFT) |
                        ((long) width << WIDTH_SHIFT) | ((long) index << NEXT_SHIFT);
                if (prev == NULL_PTR) {
                    assert index == mActiveHead;
                    mActiveHead = freeIndex;
                } else {
                    assert index != mActiveHead;
                    assert (int) (data[prev] >>> NEXT_SHIFT) == index;
                    data[prev] &= ~((long) SLOT_MASK << NEXT_SHIFT);
                    data[prev] |= ((long) freeIndex << NEXT_SHIFT);
                }
                prev = freeIndex;
            }

            // delete width of the new skylines from following ones
            for (int i = index; i != NULL_PTR; ) {
                // The new segment subsumes all or part of i
                long value = data[i];
                int cx = (int) ((data[i] >> X_SHIFT) & SLOT_MASK);
                assert cx >= ((data[prev] >> X_SHIFT) & SLOT_MASK);

                int right = (int) (((data[prev] >> X_SHIFT) & SLOT_MASK) + ((data[prev] >> WIDTH_SHIFT) & SLOT_MASK));
                if (cx < right) {
                    int shrink = right - cx;
                    int newWidth = (int) ((value >> WIDTH_SHIFT) & SLOT_MASK) - shrink;

                    if (newWidth <= 0) {
                        // fully consumed, remove i
                        int next = (int) (value >>> NEXT_SHIFT);
                        data[i] = ((long) mFreeHead << NEXT_SHIFT);
                        mFreeHead = i;

                        data[prev] &= ~((long) SLOT_MASK << NEXT_SHIFT);
                        data[prev] |= (long) next << NEXT_SHIFT;

                        i = next;
                    } else {
                        // only partially consumed
                        data[i] &= ~(((long) SLOT_MASK << X_SHIFT) | ((long) SLOT_MASK << WIDTH_SHIFT));
                        data[i] |= ((long) (cx + shrink) << X_SHIFT);
                        data[i] |= ((long) newWidth << WIDTH_SHIFT);
                        break;
                    }
                } else {
                    break;
                }
            }

            // merge skylines
            for (int i = mActiveHead; ; ) {
                long value = data[i];
                int next = (int) (value >>> NEXT_SHIFT);
                if (next == NULL_PTR) {
                    break;
                }
                int cy = (int) ((value >> Y_SHIFT) & SLOT_MASK);
                int ny = (int) ((data[next] >> Y_SHIFT) & SLOT_MASK);
                if (cy == ny) {
                    data[i] &= ~((long) SLOT_MASK << WIDTH_SHIFT);
                    int cw = (int) ((value >> WIDTH_SHIFT) & SLOT_MASK);
                    int nw = (int) ((data[next] >> WIDTH_SHIFT) & SLOT_MASK);
                    data[i] |= (long) (cw + nw) << WIDTH_SHIFT;
                    // remove i + 1
                    int nextNext = (int) (data[next] >>> NEXT_SHIFT);
                    data[next] = ((long) mFreeHead << NEXT_SHIFT);
                    mFreeHead = next;

                    data[i] &= ~((long) SLOT_MASK << NEXT_SHIFT);
                    data[i] |= (long) nextNext << NEXT_SHIFT;

                    i = nextNext;
                } else {
                    i = next;
                }
                if (i == NULL_PTR) {
                    break;
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
        public boolean addRect(Rect2i rect) {
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
            boolean add(Rect2i rect, int levelWidth, int requestedLength, int requestedSize) {
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
        public boolean addRect(Rect2i rect) {
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
        public boolean addRect(Rect2i rect) {
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
                }
                if (filled) {
                    return null;
                }
                if (width > this.width || height > this.height) {
                    return null;
                }
                if (width == this.width && height == this.height) {
                    filled = true;
                    return this;
                }
                int widthLeft = this.width - width;
                int heightLeft = this.height - height;
                if (widthLeft > heightLeft) {
                    left = new Node(x, y, width, this.height);
                    right = new Node(x + width, y,
                            widthLeft, this.height);
                } else {
                    left = new Node(x, y, this.width, height);
                    right = new Node(x, y + height,
                            this.width, heightLeft);
                }
                return left.insert(width, height);
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
        public boolean addRect(Rect2i rect) {
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
