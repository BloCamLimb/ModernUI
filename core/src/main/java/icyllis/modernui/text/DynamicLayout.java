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

package icyllis.modernui.text;

import icyllis.modernui.text.style.UpdateLayout;
import icyllis.modernui.util.GrowingArrayUtils;
import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

public class DynamicLayout extends Layout {

    private static final Pool<Builder> sPool = Pools.concurrent(2);

    /**
     * Obtain a builder for constructing DynamicLayout objects.
     */
    @Nonnull
    public static Builder builder(@Nonnull CharSequence base, @Nonnull TextPaint paint,
                                  int width) {
        Builder b = sPool.acquire();
        if (b == null) {
            b = new Builder();
        }

        // set default initial values
        b.mBase = base;
        b.mDisplay = base;
        b.mPaint = paint;
        b.mWidth = width;
        b.mAlignment = Alignment.ALIGN_NORMAL;
        b.mTextDir = TextDirectionHeuristics.FIRSTSTRONG_LTR;
        b.mIncludePad = true;
        b.mFallbackLineSpacing = true; // default true
        b.mEllipsizedWidth = width;
        b.mEllipsize = null;
        return b;
    }

    /**
     * Builder for dynamic layouts. The builder is the preferred pattern for constructing
     * DynamicLayout objects and should be preferred over the constructors, particularly to access
     * newer features. To build a dynamic layout, first call {@link #builder} with the required
     * arguments (base, paint, and width), then call setters for optional parameters, and finally
     * {@link #build} to build the DynamicLayout object. Parameters not explicitly set will get
     * default values.
     */
    @SuppressWarnings("unused")
    public static final class Builder {

        // cached instance
        private final FontMetricsInt mFontMetricsInt = new FontMetricsInt();

        private CharSequence mBase;
        private CharSequence mDisplay;
        private TextPaint mPaint;
        private int mWidth;
        private Alignment mAlignment;
        private TextDirectionHeuristic mTextDir;
        private boolean mIncludePad;
        private boolean mFallbackLineSpacing;
        private TextUtils.TruncateAt mEllipsize;
        private int mEllipsizedWidth;

        private Builder() {
        }

        /**
         * This method should be called after the layout is finished getting constructed and the
         * builder needs to be cleaned up and returned to the pool.
         */
        private void recycle() {
            mBase = null;
            mDisplay = null;
            mPaint = null;
            sPool.release(this);
        }

        /**
         * Set the transformed text (password transformation being the primary example of a
         * transformation) that will be updated as the base text is changed. The default is the
         * 'base' text passed to the builder's constructor.
         *
         * @param display the transformed text
         * @return this builder, useful for chaining
         */
        @Nonnull
        public Builder setDisplayText(@Nonnull CharSequence display) {
            mDisplay = display;
            return this;
        }

        /**
         * Set the alignment. The default is {@link Layout.Alignment#ALIGN_NORMAL}.
         *
         * @param alignment Alignment for the resulting {@link DynamicLayout}
         * @return this builder, useful for chaining
         */
        @Nonnull
        public Builder setAlignment(@Nonnull Alignment alignment) {
            mAlignment = alignment;
            return this;
        }

        /**
         * Set the text direction heuristic. The text direction heuristic is used to resolve text
         * direction per-paragraph based on the input text. The default is
         * {@link TextDirectionHeuristics#FIRSTSTRONG_LTR}.
         *
         * @param textDir text direction heuristic for resolving bidi behavior.
         * @return this builder, useful for chaining
         */
        @Nonnull
        public Builder setTextDirection(@Nonnull TextDirectionHeuristic textDir) {
            mTextDir = textDir;
            return this;
        }

        /**
         * Set whether to include extra space beyond font ascent and descent (which is needed to
         * avoid clipping in some languages, such as Arabic and Kannada). The default is
         * {@code true}.
         *
         * @param includePad whether to include padding
         * @return this builder, useful for chaining
         */
        @Nonnull
        public Builder setIncludePad(boolean includePad) {
            mIncludePad = includePad;
            return this;
        }

        /**
         * Set whether to respect the ascent and descent of the fallback fonts that are used in
         * displaying the text (which is needed to avoid text from consecutive lines running into
         * each other). If set, fallback fonts that end up getting used can increase the ascent
         * and descent of the lines that they are used on.
         * <p>
         * The default is {@code true}. It is required to be true if text could be in
         * languages like Burmese or Tibetan where text is typically much taller or deeper than
         * Latin text.
         *
         * @param fallbackLineSpacing whether to expand line spacing based on fallback fonts
         * @return this builder, useful for chaining
         */
        @Nonnull
        public Builder setFallbackLineSpacing(boolean fallbackLineSpacing) {
            mFallbackLineSpacing = fallbackLineSpacing;
            return this;
        }

        /**
         * Set the width as used for ellipsizing purposes, if it differs from the normal layout
         * width. The default is the {@code width} passed to {@link #build()}.
         *
         * @param ellipsizedWidth width used for ellipsizing, in pixels
         * @return this builder, useful for chaining
         */
        @Nonnull
        public Builder setEllipsizedWidth(int ellipsizedWidth) {
            mEllipsizedWidth = ellipsizedWidth;
            return this;
        }

        /**
         * Set ellipsizing on the layout. Causes words that are longer than the view is wide, or
         * exceeding the number of lines (see #setMaxLines) in the case of
         * {@link TextUtils.TruncateAt#END} or
         * {@link TextUtils.TruncateAt#MARQUEE}, to be ellipsized instead of broken.
         * The default is {@code null}, indicating no ellipsis is to be applied.
         *
         * @param ellipsize type of ellipsis behavior
         * @return this builder, useful for chaining
         */
        public Builder setEllipsize(@Nullable TextUtils.TruncateAt ellipsize) {
            mEllipsize = ellipsize;
            return this;
        }

        /**
         * Build the {@link DynamicLayout} after options have been set.
         *
         * <p>Note: the builder object must not be reused in any way after calling this method.
         * Setting parameters after calling this method, or calling it a second time on the same
         * builder object, will likely lead to unexpected results.
         *
         * @return the newly constructed {@link DynamicLayout} object
         */
        @Nonnull
        public DynamicLayout build() {
            final DynamicLayout result = new DynamicLayout(this);
            recycle();
            return result;
        }
    }

    private static StaticLayout sStaticLayout = null;
    private static StaticLayout.Builder sBuilder = null;

    private static final Object[] sLock = new Object[0];

    private static final int PRIORITY = 128;
    private static final int BLOCK_MINIMUM_CHARACTER_LENGTH = 400;

    // START, DIR, and TAB share the same entry.
    private static final int START = 0;
    private static final int DIR = START;
    private static final int TAB = START;
    private static final int TOP = 1;
    // DESCENT and MAY_PROTRUDE_FROM_TOP_OR_BOTTOM share the same entry.
    private static final int DESCENT = 2;
    private static final int COLUMNS_NORMAL = 3;

    private static final int ELLIPSIS_START = 3;
    private static final int ELLIPSIS_COUNT = 4;
    private static final int COLUMNS_ELLIPSIZE = 5;

    private static final int START_MASK = 0x1FFFFFFF;
    private static final int DIR_SHIFT = 30;
    private static final int TAB_MASK = 0x20000000;

    private static final int ELLIPSIS_UNDEFINED = 0x80000000;

    //// Member Variables \\\\

    private CharSequence mBase;
    private final CharSequence mDisplay;
    private ChangeWatcher mWatcher;
    private final boolean mIncludePad;
    private boolean mFallbackLineSpacing;
    private boolean mEllipsize;
    private int mEllipsizedWidth;
    private TextUtils.TruncateAt mEllipsizeAt;

    private PackedIntVector mInts;
    private PackedObjectVector<Directions> mObjects;

    /**
     * Value used in mBlockIndices when a block has been created or recycled and indicating that its
     * display list needs to be re-created.
     */
    public static final int INVALID_BLOCK_INDEX = -1;
    // Stores the line numbers of the last line of each block (inclusive)
    private int[] mBlockEndLines;
    // The indices of this block's display list in TextView's internal display list array or
    // INVALID_BLOCK_INDEX if this block has been invalidated during an edition
    private int[] mBlockIndices;
    // Set of blocks that always need to be redrawn.
    private IntArrayList mBlocksAlwaysNeedToBeRedrawn;
    // Number of items actually currently being used in the above 2 arrays
    private int mNumberOfBlocks;
    // The first index of the blocks whose locations are changed
    private int mIndexFirstChangedBlock;

    private int mTopPadding, mBottomPadding;

    private DynamicLayout(@Nonnull Builder b) {
        super(createEllipsizer(b.mEllipsize, b.mDisplay),
                b.mPaint, b.mWidth, b.mAlignment, b.mTextDir);

        mDisplay = b.mDisplay;
        mIncludePad = b.mIncludePad;

        generate(b);
    }

    @Nonnull
    private static CharSequence createEllipsizer(@Nullable TextUtils.TruncateAt ellipsize,
                                                 @Nonnull CharSequence display) {
        if (ellipsize == null) {
            return display;
        } else if (display instanceof Spanned) {
            return new SpannedEllipsizer(display);
        } else {
            return new Ellipsizer(display);
        }
    }

    private void generate(@Nonnull Builder b) {
        mBase = b.mBase;
        mFallbackLineSpacing = b.mFallbackLineSpacing;
        if (b.mEllipsize != null) {
            mInts = new PackedIntVector(COLUMNS_ELLIPSIZE);
            mEllipsizedWidth = b.mEllipsizedWidth;
            mEllipsizeAt = b.mEllipsize;

            /*
             * This is annoying, but we can't refer to the layout until superclass construction is
             * finished, and the superclass constructor wants the reference to the display text.
             *
             * In other words, the two Ellipsizer classes in Layout.java need a
             * (Dynamic|Static)Layout as a parameter to do their calculations, but the Ellipsizers
             * also need to be the input to the superclass's constructor (Layout). In order to go
             * around the circular dependency, we construct the Ellipsizer with only one of the
             * parameters, the text (in createEllipsizer). And we fill in the rest of the needed
             * information (layout, width, and method) later, here.
             *
             * This will break if the superclass constructor ever actually cares about the content
             * instead of just holding the reference.
             */
            final Ellipsizer e = (Ellipsizer) getText();
            e.mLayout = this;
            e.mWidth = b.mEllipsizedWidth;
            e.mMethod = b.mEllipsize;
            mEllipsize = true;
        } else {
            mInts = new PackedIntVector(COLUMNS_NORMAL);
            mEllipsizedWidth = b.mWidth;
            mEllipsizeAt = null;
        }

        mObjects = new PackedObjectVector<>(1);

        // Initial state is a single line with 0 characters (0 to 0), with top at 0 and bottom at
        // whatever is natural, and undefined ellipsis.

        int[] start;

        if (b.mEllipsize != null) {
            start = new int[COLUMNS_ELLIPSIZE];
            start[ELLIPSIS_START] = ELLIPSIS_UNDEFINED;
        } else {
            start = new int[COLUMNS_NORMAL];
        }

        final Directions[] dirs = new Directions[]{Directions.ALL_LEFT_TO_RIGHT};

        final FontMetricsInt fm = b.mFontMetricsInt;
        b.mPaint.getFontMetricsInt(fm);
        final int asc = fm.ascent;
        final int desc = fm.descent;

        start[DIR] = DIR_LEFT_TO_RIGHT << DIR_SHIFT;
        start[TOP] = 0;
        start[DESCENT] = desc;
        mInts.insertAt(0, start);

        start[TOP] = desc + asc;
        mInts.insertAt(1, start);

        mObjects.insertAt(0, dirs);

        // Update from 0 characters to whatever the displayed text is
        reflow(mBase, 0, 0, mDisplay.length());

        if (mBase instanceof final Spannable sp) {
            if (mWatcher == null)
                mWatcher = new ChangeWatcher(this);

            // Strip out any watchers for other DynamicLayouts.
            final int baseLength = mBase.length();
            final ChangeWatcher[] spans = sp.getSpans(0, baseLength, ChangeWatcher.class);
            if (spans != null) {
                for (ChangeWatcher span : spans) {
                    sp.removeSpan(span);
                }
            }

            sp.setSpan(mWatcher, 0, baseLength,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE |
                            (PRIORITY << Spannable.SPAN_PRIORITY_SHIFT));
        }
    }

    public void reflow(CharSequence s, int where, int before, int after) {
        if (s != mBase)
            return;

        CharSequence text = mDisplay;
        int len = text.length();

        // seek back to the start of the paragraph

        int find = TextUtils.lastIndexOf(text, '\n', where - 1);
        if (find < 0)
            find = 0;
        else
            find = find + 1;

        {
            int diff = where - find;
            before += diff;
            after += diff;
            where -= diff;
        }

        // seek forward to the end of the paragraph

        int look = TextUtils.indexOf(text, '\n', where + after);
        if (look < 0)
            look = len;
        else
            look++; // we want the index after the \n

        int change = look - (where + after);
        before += change;
        after += change;

        // seek further out to cover anything that is forced to wrap together

        //TODO
        if (text instanceof Spanned) {
            Spanned sp = (Spanned) text;
        }

        // find affected region of old layout

        int startline = getLineForOffset(where);
        int startv = getLineTop(startline);

        int endline = getLineForOffset(where + before);
        if (where + after == len)
            endline = getLineCount();
        int endv = getLineTop(endline);
        boolean islast = (endline == getLineCount());

        // generate new layout for affected text

        StaticLayout reflowed;
        StaticLayout.Builder b;

        synchronized (sLock) {
            reflowed = sStaticLayout;
            b = sBuilder;
            sStaticLayout = null;
            sBuilder = null;
        }

        if (reflowed == null) {
            reflowed = new StaticLayout(null);
            b = StaticLayout.builder(text, where, where + after, getPaint(), getWidth());
        }

        b.setText(text, where, where + after)
                .setPaint(getPaint())
                .setWidth(getWidth())
                .setTextDirection(getTextDirectionHeuristic())
                .setFallbackLineSpacing(mFallbackLineSpacing)
                .setEllipsizedWidth(mEllipsizedWidth)
                .setEllipsize(mEllipsizeAt);

        reflowed.generate(b, false /*includepad*/, true /*trackpad*/);
        int n = reflowed.getLineCount();
        // If the new layout has a blank line at the end, but it is not
        // the very end of the buffer, then we already have a line that
        // starts there, so disregard the blank line.

        if (where + after != len && reflowed.getLineStart(n - 1) == where + after)
            n--;

        // remove affected lines from old layout
        mInts.deleteAt(startline, endline - startline);
        mObjects.deleteAt(startline, endline - startline);

        // adjust offsets in layout for new height and offsets

        int ht = reflowed.getLineTop(n);
        int toppad = 0, botpad = 0;

        if (mIncludePad && startline == 0) {
            toppad = reflowed.getTopPadding();
            mTopPadding = toppad;
            ht -= toppad;
        }
        if (mIncludePad && islast) {
            botpad = reflowed.getBottomPadding();
            mBottomPadding = botpad;
            ht += botpad;
        }

        mInts.adjustValuesBelow(startline, START, after - before);
        mInts.adjustValuesBelow(startline, TOP, startv - endv + ht);

        // insert new layout

        int[] ints;

        if (mEllipsize) {
            ints = new int[COLUMNS_ELLIPSIZE];
            ints[ELLIPSIS_START] = ELLIPSIS_UNDEFINED;
        } else {
            ints = new int[COLUMNS_NORMAL];
        }

        Directions[] objects = new Directions[1];

        for (int i = 0; i < n; i++) {
            final int start = reflowed.getLineStart(i);
            ints[START] = start;
            ints[DIR] |= reflowed.getParagraphDirection(i) << DIR_SHIFT;
            ints[TAB] |= reflowed.getLineContainsTab(i) ? TAB_MASK : 0;

            int top = reflowed.getLineTop(i) + startv;
            if (i > 0)
                top -= toppad;
            ints[TOP] = top;

            int desc = reflowed.getLineDescent(i);
            if (i == n - 1)
                desc += botpad;

            ints[DESCENT] = desc;
            objects[0] = reflowed.getLineDirections(i);

            if (mEllipsize) {
                ints[ELLIPSIS_START] = reflowed.getEllipsisStart(i);
                ints[ELLIPSIS_COUNT] = reflowed.getEllipsisCount(i);
            }

            mInts.insertAt(startline + i, ints);
            mObjects.insertAt(startline + i, objects);
        }

        updateBlocks(startline, endline - 1, n);

        b.release();
        synchronized (sLock) {
            sStaticLayout = reflowed;
            sBuilder = b;
        }
    }

    /**
     * Create the initial block structure, cutting the text into blocks of at least
     * BLOCK_MINIMUM_CHARACTER_SIZE characters, aligned on the ends of paragraphs.
     */
    private void createBlocks() {
        int offset = BLOCK_MINIMUM_CHARACTER_LENGTH;
        mNumberOfBlocks = 0;
        final CharSequence text = mDisplay;

        while (true) {
            offset = TextUtils.indexOf(text, '\n', offset);
            if (offset < 0) {
                addBlockAtOffset(text.length());
                break;
            } else {
                addBlockAtOffset(offset);
                offset += BLOCK_MINIMUM_CHARACTER_LENGTH;
            }
        }

        // mBlockIndices and mBlockEndLines should have the same length
        mBlockIndices = new int[mBlockEndLines.length];
        for (int i = 0; i < mBlockEndLines.length; i++) {
            mBlockIndices[i] = INVALID_BLOCK_INDEX;
        }
    }

    @Nullable
    public IntArrayList getBlocksAlwaysNeedToBeRedrawn() {
        return mBlocksAlwaysNeedToBeRedrawn;
    }

    private void updateAlwaysNeedsToBeRedrawn(int blockIndex) {
        if (mBlocksAlwaysNeedToBeRedrawn == null) {
            mBlocksAlwaysNeedToBeRedrawn = new IntArrayList();
        }
        if (!mBlocksAlwaysNeedToBeRedrawn.contains(blockIndex)) {
            mBlocksAlwaysNeedToBeRedrawn.add(blockIndex);
        }
    }

    /**
     * Create a new block, ending at the specified character offset.
     * A block will actually be created only if has at least one line, i.e. this offset is
     * not on the end line of the previous block.
     */
    private void addBlockAtOffset(int offset) {
        final int line = getLineForOffset(offset);
        if (mBlockEndLines == null) {
            // Initial creation of the array, no test on previous block ending line
            mBlockEndLines = new int[1];
            mBlockEndLines[mNumberOfBlocks] = line;
            updateAlwaysNeedsToBeRedrawn(mNumberOfBlocks);
            mNumberOfBlocks++;
            return;
        }

        final int previousBlockEndLine = mBlockEndLines[mNumberOfBlocks - 1];
        if (line > previousBlockEndLine) {
            mBlockEndLines = GrowingArrayUtils.append(mBlockEndLines, mNumberOfBlocks, line);
            updateAlwaysNeedsToBeRedrawn(mNumberOfBlocks);
            mNumberOfBlocks++;
        }
    }

    /**
     * This method is called every time the layout is reflowed after an edition.
     * It updates the internal block data structure. The text is split in blocks
     * of contiguous lines, with at least one block for the entire text.
     * When a range of lines is edited, new blocks (from 0 to 3 depending on the
     * overlap structure) will replace the set of overlapping blocks.
     * Blocks are listed in order and are represented by their ending line number.
     * An index is associated to each block (which will be used by display lists),
     * this class simply invalidates the index of blocks overlapping a modification.
     *
     * @param startLine    the first line of the range of modified lines
     * @param endLine      the last line of the range, possibly equal to startLine, lower
     *                     than getLineCount()
     * @param newLineCount the number of lines that will replace the range, possibly 0
     */
    public void updateBlocks(int startLine, int endLine, int newLineCount) {
        if (mBlockEndLines == null) {
            createBlocks();
            return;
        }

        /*final*/
        int firstBlock = -1;
        /*final*/
        int lastBlock = -1;
        for (int i = 0; i < mNumberOfBlocks; i++) {
            if (mBlockEndLines[i] >= startLine) {
                firstBlock = i;
                break;
            }
        }
        for (int i = firstBlock; i < mNumberOfBlocks; i++) {
            if (mBlockEndLines[i] >= endLine) {
                lastBlock = i;
                break;
            }
        }
        final int lastBlockEndLine = mBlockEndLines[lastBlock];

        final boolean createBlockBefore = startLine > (firstBlock == 0 ? 0 :
                mBlockEndLines[firstBlock - 1] + 1);
        final boolean createBlock = newLineCount > 0;
        final boolean createBlockAfter = endLine < mBlockEndLines[lastBlock];

        int numAddedBlocks = 0;
        if (createBlockBefore) numAddedBlocks++;
        if (createBlock) numAddedBlocks++;
        if (createBlockAfter) numAddedBlocks++;

        final int numRemovedBlocks = lastBlock - firstBlock + 1;
        final int newNumberOfBlocks = mNumberOfBlocks + numAddedBlocks - numRemovedBlocks;

        if (newNumberOfBlocks == 0) {
            // Even when text is empty, there is actually one line and hence one block
            mBlockEndLines[0] = 0;
            mBlockIndices[0] = INVALID_BLOCK_INDEX;
            mNumberOfBlocks = 1;
            return;
        }

        if (newNumberOfBlocks > mBlockEndLines.length) {
            int[] blockEndLines = new int[Math.max(mBlockEndLines.length * 2, newNumberOfBlocks)];
            int[] blockIndices = new int[blockEndLines.length];
            System.arraycopy(mBlockEndLines, 0, blockEndLines, 0, firstBlock);
            System.arraycopy(mBlockIndices, 0, blockIndices, 0, firstBlock);
            System.arraycopy(mBlockEndLines, lastBlock + 1,
                    blockEndLines, firstBlock + numAddedBlocks, mNumberOfBlocks - lastBlock - 1);
            System.arraycopy(mBlockIndices, lastBlock + 1,
                    blockIndices, firstBlock + numAddedBlocks, mNumberOfBlocks - lastBlock - 1);
            mBlockEndLines = blockEndLines;
            mBlockIndices = blockIndices;
        } else if (numAddedBlocks + numRemovedBlocks != 0) {
            System.arraycopy(mBlockEndLines, lastBlock + 1,
                    mBlockEndLines, firstBlock + numAddedBlocks, mNumberOfBlocks - lastBlock - 1);
            System.arraycopy(mBlockIndices, lastBlock + 1,
                    mBlockIndices, firstBlock + numAddedBlocks, mNumberOfBlocks - lastBlock - 1);
        }

        if (numAddedBlocks + numRemovedBlocks != 0 && mBlocksAlwaysNeedToBeRedrawn != null) {
            final IntArrayList list = new IntArrayList();
            final int changedBlockCount = numAddedBlocks - numRemovedBlocks;
            for (int i = 0; i < mBlocksAlwaysNeedToBeRedrawn.size(); i++) {
                int block = mBlocksAlwaysNeedToBeRedrawn.getInt(i);
                if (block < firstBlock) {
                    // block index is before firstBlock add it since it did not change
                    if (!list.contains(block)) {
                        list.add(block);
                    }
                }

                if (block > lastBlock) {
                    // block index is after lastBlock, the index reduced to += changedBlockCount
                    block += changedBlockCount;
                    if (!list.contains(block)) {
                        list.add(block);
                    }
                }
            }
            mBlocksAlwaysNeedToBeRedrawn = list;
        }

        mNumberOfBlocks = newNumberOfBlocks;
        int newFirstChangedBlock;
        final int deltaLines = newLineCount - (endLine - startLine + 1);
        if (deltaLines != 0) {
            // Display list whose index is >= mIndexFirstChangedBlock is valid
            // but it needs to update its drawing location.
            newFirstChangedBlock = firstBlock + numAddedBlocks;
            for (int i = newFirstChangedBlock; i < mNumberOfBlocks; i++) {
                mBlockEndLines[i] += deltaLines;
            }
        } else {
            newFirstChangedBlock = mNumberOfBlocks;
        }
        mIndexFirstChangedBlock = Math.min(mIndexFirstChangedBlock, newFirstChangedBlock);

        int blockIndex = firstBlock;
        if (createBlockBefore) {
            mBlockEndLines[blockIndex] = startLine - 1;
            updateAlwaysNeedsToBeRedrawn(blockIndex);
            mBlockIndices[blockIndex] = INVALID_BLOCK_INDEX;
            blockIndex++;
        }

        if (createBlock) {
            mBlockEndLines[blockIndex] = startLine + newLineCount - 1;
            updateAlwaysNeedsToBeRedrawn(blockIndex);
            mBlockIndices[blockIndex] = INVALID_BLOCK_INDEX;
            blockIndex++;
        }

        if (createBlockAfter) {
            mBlockEndLines[blockIndex] = lastBlockEndLine + deltaLines;
            updateAlwaysNeedsToBeRedrawn(blockIndex);
            mBlockIndices[blockIndex] = INVALID_BLOCK_INDEX;
        }
    }

    /**
     * This method is used for test purposes only.
     */
    public void setBlocksDataForTest(int[] blockEndLines, int[] blockIndices, int numberOfBlocks,
                                     int totalLines) {
        mBlockEndLines = new int[blockEndLines.length];
        mBlockIndices = new int[blockIndices.length];
        System.arraycopy(blockEndLines, 0, mBlockEndLines, 0, blockEndLines.length);
        System.arraycopy(blockIndices, 0, mBlockIndices, 0, blockIndices.length);
        mNumberOfBlocks = numberOfBlocks;
        while (mInts.size() < totalLines) {
            mInts.insertAt(mInts.size(), new int[COLUMNS_NORMAL]);
        }
    }

    public int[] getBlockEndLines() {
        return mBlockEndLines;
    }

    public int[] getBlockIndices() {
        return mBlockIndices;
    }

    public int getBlockIndex(int index) {
        return mBlockIndices[index];
    }

    public void setBlockIndex(int index, int blockIndex) {
        mBlockIndices[index] = blockIndex;
    }

    public int getNumberOfBlocks() {
        return mNumberOfBlocks;
    }

    public int getIndexFirstChangedBlock() {
        return mIndexFirstChangedBlock;
    }

    public void setIndexFirstChangedBlock(int i) {
        mIndexFirstChangedBlock = i;
    }

    @Override
    public int getLineCount() {
        return mInts.size() - 1;
    }

    @Override
    public int getLineTop(int line) {
        return mInts.getValue(line, TOP);
    }

    @Override
    public int getLineDescent(int line) {
        return mInts.getValue(line, DESCENT);
    }

    @Override
    public int getLineStart(int line) {
        return mInts.getValue(line, START) & START_MASK;
    }

    @Override
    public boolean getLineContainsTab(int line) {
        return (mInts.getValue(line, TAB) & TAB_MASK) != 0;
    }

    @Override
    public int getParagraphDirection(int line) {
        return mInts.getValue(line, DIR) >> DIR_SHIFT;
    }

    @Override
    public final Directions getLineDirections(int line) {
        return mObjects.getValue(line, 0);
    }

    @Override
    public int getTopPadding() {
        return mTopPadding;
    }

    @Override
    public int getBottomPadding() {
        return mBottomPadding;
    }

    @Override
    public int getEllipsizedWidth() {
        return mEllipsizedWidth;
    }

    @Override
    public int getEllipsisStart(int line) {
        if (mEllipsizeAt == null) {
            return 0;
        }

        return mInts.getValue(line, ELLIPSIS_START);
    }

    @Override
    public int getEllipsisCount(int line) {
        if (mEllipsizeAt == null) {
            return 0;
        }

        return mInts.getValue(line, ELLIPSIS_COUNT);
    }

    private static class ChangeWatcher implements TextWatcher, SpanWatcher {

        private final WeakReference<DynamicLayout> mLayout;

        public ChangeWatcher(DynamicLayout layout) {
            mLayout = new WeakReference<>(layout);
        }

        private void reflow(CharSequence s, int where, int before, int after) {
            DynamicLayout ml = mLayout.get();

            if (ml != null) {
                ml.reflow(s, where, before, after);
            } else if (s instanceof Spannable) {
                ((Spannable) s).removeSpan(this);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int where, int before, int after) {
            // Intentionally empty
        }

        @Override
        public void onTextChanged(CharSequence s, int where, int before, int after) {
            reflow(s, where, before, after);
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Intentionally empty
        }

        @Override
        public void onSpanAdded(Spannable s, Object o, int start, int end) {
            if (o instanceof UpdateLayout)
                reflow(s, start, end - start, end - start);
        }

        @Override
        public void onSpanRemoved(Spannable s, Object o, int start, int end) {
            if (o instanceof UpdateLayout)
                reflow(s, start, end - start, end - start);
        }

        @Override
        public void onSpanChanged(Spannable s, Object o, int start, int end, int nstart, int nend) {
            if (o instanceof UpdateLayout) {
                if (start > end) {
                    // Bug: 67926915 start cannot be determined, fallback to reflow from start
                    // instead of causing an exception
                    start = 0;
                }
                reflow(s, start, end - start, end - start);
                reflow(s, nstart, nend - nstart, nend - nstart);
            }
        }
    }
}
