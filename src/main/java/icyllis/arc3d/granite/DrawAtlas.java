/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.task.ImageUploadTask;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * This class manages one or more atlas textures on behalf of primitive draws in Device. The
 * drawing processes that use the atlas add preceding UploadTasks when generating RenderPassTasks.
 * The class provides facilities for using DrawTokens to detect data hazards. Plots that need
 * uploads are tracked until it is impossible to add data without overwriting texels read by draws
 * that have not yet been snapped to a RenderPassTask. At that point, the atlas will attempt to
 * allocate a new atlas texture (or "page") of the same size, up to a maximum number of textures,
 * and upload to that texture. If that's not possible, then the atlas will fail to add a subimage.
 * This gives the Device the chance to end the current draw, snap a RenderpassTask, and begin a new
 * one. Additional uploads will then succeed.
 * <p>
 * When the atlas has multiple pages, new uploads are prioritized to the lower index pages, i.e.,
 * it will try to upload to page 0 before page 1 or 2. To keep the atlas from continually using
 * excess space, periodic garbage collection is needed to shift data from the higher index pages to
 * the lower ones, and then eventually remove any pages that are no longer in use. "In use" is
 * determined by using the AtlasToken system: After a DrawPass is snapped a subarea of the page, or
 * "plot" is checked to see whether it was used in that DrawPass. If less than a quarter of the
 * plots have been used recently (within kPlotRecentlyUsedCount iterations) and there are available
 * plots in lower index pages, the higher index page will be deactivated, and its glyphs will
 * gradually migrate to other pages via the usual upload system.
 * <p>
 * Garbage collection is initiated by the DrawAtlas's client via the compact() method.
 */
public class DrawAtlas implements AutoCloseable {

    /**
     * Keep track of generation number for atlases and Plots.
     */
    public static class AtlasGenerationCounter {
        // must be 0
        public static final long INVALID_GENERATION = 0;
        // max generation (inclusive), also used as bit mask
        public static final long MAX_GENERATION = (1L << 48) - 1;

        private long mGeneration = 1;

        /**
         * Returns next valid generation number.
         */
        public long next() {
            if (mGeneration > MAX_GENERATION) {
                mGeneration = 1;
            }
            return mGeneration++;
        }
    }

    /**
     * AtlasToken is used to sequence uploads relative to each other and to batches of draws.
     * <p>
     * AtlasToken is represented as an unsigned long sequence number. Wrapping is allowed,
     * 0 is reserved for the invalid token. Do not use operator to compare, use method instead.
     */
    public static class AtlasToken {
        // must be 0
        public static final long INVALID_TOKEN = 0;

        /**
         * Returns next valid sequence number.
         */
        public static long next(long token) {
            long next = token + 1;
            if (next == INVALID_TOKEN) {
                return 1;
            }
            return next;
        }

        /**
         * Compares two tokens with wrapping.
         */
        public static int compare(long lhs, long rhs) {
            if (lhs == rhs) {
                return 0;
            }
            if (lhs == INVALID_TOKEN) {
                return -1;
            }
            if (rhs == INVALID_TOKEN) {
                return 1;
            }
            // use subtraction since it can wrap
            return (rhs - lhs) > 0 ? -1 : 1;
        }

        /**
         * Returns true if the token is in the [start, end] inclusive interval.
         */
        public static boolean inInterval(long token, long start, long end) {
            assert end != INVALID_TOKEN;
            if (start == INVALID_TOKEN) {
                return true;
            }
            if (token == INVALID_TOKEN) {
                return false;
            }
            // use subtraction since it can wrap
            return token - start >= 0 && end - token >= 0;
        }

        private AtlasToken() {
        }
    }

    /**
     * The TokenTracker encapsulates the incrementing and distribution of AtlasTokens.
     */
    public static class AtlasTokenTracker {

        private long mCurrentFlushToken = AtlasToken.INVALID_TOKEN;

        /**
         * Gets the token one beyond the last token that has been flushed,
         * either in GrDrawingManager::flush() or Device::flushPendingWorkToRecorder()
         */
        public long nextFlushToken() {
            return AtlasToken.next(mCurrentFlushToken);
        }

        // Advances the next token for a flush.
        public void issueFlushToken() {
            mCurrentFlushToken = AtlasToken.next(mCurrentFlushToken);
        }
    }

    /**
     * Used to locate a plot in a {@link DrawAtlas}.
     */
    public static class PlotLocator {
        // this can be 1,2,4
        public static final int MAX_PAGES = 4;
        // this can be 32,64
        public static final int MAX_PLOTS = 64;

        //  0-48: generation
        // 48-56: plot index
        // 56-64: page index
        private long loc;

        public PlotLocator() {
        }

        public long getGeneration() {
            return loc & 0xFFFF_FFFF_FFFFL;
        }

        public int getPlotIndex() {
            return (int) (loc >> 48) & 0xFFFF;
        }

        public int getPageIndex() {
            return (int) (loc >>> 56);
        }

        public boolean isValid() {
            return loc != 0;
        }

        public void setLocation(@Nonnull PlotLocator plotLocator) {
            loc = plotLocator.loc;
        }

        public void setLocation(int pageIndex, int plotIndex, long generation) {
            assert pageIndex < MAX_PAGES;
            assert plotIndex < MAX_PLOTS;
            assert generation < (1L << 48);
            loc = ((long) pageIndex << 56) | ((long) plotIndex << 48) | generation;
        }

        public void setGeneration(long generation) {
            assert generation < (1L << 48);
            loc = (loc & 0xFFFF_0000_0000_0000L) | generation;
        }

        @Override
        public String toString() {
            return "PlotLocator{" +
                    "generation=" + getGeneration() +
                    ", plotIndex=" + getPlotIndex() +
                    ", pageIndex=" + getPageIndex() +
                    '}';
        }
    }

    /**
     * AtlasLocator handles atlas position information.
     */
    public static class AtlasLocator extends PlotLocator {

        // unsigned integer texture coordinates.
        public short u1;
        public short v1;
        public short u2;
        public short v2;

        public AtlasLocator() {
        }

        // unsigned short
        public short width() {
            return (short) (u2 - u1);
        }

        // unsigned short
        public short height() {
            return (short) (v2 - v1);
        }

        // set UVs in integer texture coordinates
        public void setRect(Rect2ic rect) {
            assert rect.left() >= 0 && rect.right() <= 0xFFFF;
            u1 = (short) rect.left();
            v1 = (short) rect.top();
            u2 = (short) rect.right();
            v2 = (short) rect.bottom();
        }

        // inset the UVs
        public void insetRect(int padding) {
            assert padding >= 0;
            assert (2 * padding <= (width() & 0xFFFF));
            assert (2 * padding <= (height() & 0xFFFF));
            // padding is 0..32767, safe to cast
            u1 += (short) padding;
            v1 += (short) padding;
            u2 -= (short) padding;
            v2 -= (short) padding;
        }

        @Override
        public String toString() {
            return "AtlasLocator{" +
                    "generation=" + getGeneration() +
                    ", plotIndex=" + getPlotIndex() +
                    ", pageIndex=" + getPageIndex() +
                    ", u1=" + Short.toUnsignedInt(u1) +
                    ", v1=" + Short.toUnsignedInt(v1) +
                    ", u2=" + Short.toUnsignedInt(u2) +
                    ", v2=" + Short.toUnsignedInt(v2) +
                    '}';
        }
    }

    /**
     * An interface for eviction callbacks. Whenever an atlas evicts a specific PlotLocator,
     * it will call all of the registered listeners so they can process the eviction.
     */
    public interface PlotEvictionCallback {

        void onEvict(PlotLocator locator);
    }

    /**
     * A class which can be handed back to an atlas for updating plots in bulk.
     */
    public static class PlotBulkUseUpdater {
        // 64 plots per page, long is sufficient
        private final long[] mBitSet = new long[PlotLocator.MAX_PAGES];
        // plots to update, max count is 4 * 64 = 256
        // the data is sync with the bitset, but it can be iterated faster
        //  0-16: plot index
        // 16-32: page index
        private int[] mData = new int[4]; // <- initial size must be power of two
        private int mCount = 0;

        public PlotBulkUseUpdater() {
        }

        // returns added or not
        public boolean add(AtlasLocator locator) {
            int plotIndex = locator.getPlotIndex();
            int pageIndex = locator.getPageIndex();
            assert plotIndex < PlotLocator.MAX_PLOTS;
            assert pageIndex < PlotLocator.MAX_PAGES;
            if (((mBitSet[pageIndex] >>> plotIndex) & 1) != 0) {
                // already set
                return false;
            }
            mBitSet[pageIndex] |= (1L << plotIndex);
            if (mCount >= mData.length) {
                // double the length, max length is 256
                mData = Arrays.copyOf(mData, mData.length << 1);
            }
            mData[mCount++] = (plotIndex) | (pageIndex << 16);
            return true;
        }

        public void clear() {
            mCount = 0;
            Arrays.fill(mBitSet, 0);
        }

        public int count() {
            return mCount;
        }

        // unsigned int
        //  0-16: plot index
        // 16-32: page index
        public int dataAt(int index) {
            return mData[index];
        }

        public long getMemorySize() {
            long size = 16;
            size += 16 + (long) mBitSet.length * 8 + 8;
            size += 16 + (long) mData.length * 4 + 8;
            return size;
        }
    }

    /**
     * The backing texture for an atlas is broken into a spatial grid of Plots. The Plots
     * keep track of sub-image placement via their {@link RectanglePacker}.
     */
    public static class Plot extends PlotLocator implements AutoCloseable {
        // LinkedList
        private Plot mPrev;
        private Plot mNext;

        // AtlasToken
        private long mLastUseToken;
        private int mFlushesSinceLastUsed;

        private final AtlasGenerationCounter mGenerationCounter;
        // memory address to plot data in off-heap
        private long mData;
        // the dimension of the plot in texels
        private final int mWidth;
        private final int mHeight;

        private final RectanglePacker.Skyline mRectanglePacker;
        // the offset of the plot in the backing texture in texels
        private final int mOffsetX;
        private final int mOffsetY;

        private final int mColorType;
        private final int mBytesPerPixel;
        // area in the Plot that needs to be uploaded
        private final Rect2i mDirtyRect = new Rect2i();
        private final Rect2i mTmpRect = new Rect2i();

        public Plot(int pageIndex, int plotIndex, AtlasGenerationCounter generationCounter,
                    int plotX, int plotY, int width, int height, int colorType, int bpp) {
            mLastUseToken = AtlasToken.INVALID_TOKEN;
            mFlushesSinceLastUsed = 0;
            mGenerationCounter = generationCounter;
            setLocation(pageIndex, plotIndex, generationCounter.next());
            mData = MemoryUtil.NULL;
            mWidth = width;
            mHeight = height;
            mRectanglePacker = new RectanglePacker.Skyline(width, height);
            mOffsetX = plotX * width;
            mOffsetY = plotY * height;
            mColorType = colorType;
            mBytesPerPixel = bpp;
            // row bytes must be a multiple of 4 bytes
            assert MathUtil.isAlign4(width * bpp);
            // bpp must be aligned
            assert bpp == 1 || bpp == 2 || bpp == 4;
        }

        @Override
        public void close() {
            if (mData != MemoryUtil.NULL) {
                MemoryUtil.nmemFree(mData);
            }
            mData = MemoryUtil.NULL;
        }

        /**
         * To add data to the Plot, first call addRect to see if it's possible. If successful,
         * use the atlasLocator to get a pointer to the location in the atlas via
         * {@link #dataAt(AtlasLocator)} and render to that location, or if you already have data
         * use {@link #copySubImage(AtlasLocator, Object, long)}.
         */
        public boolean addRect(int width, int height, AtlasLocator atlasLocator) {
            assert (width <= mWidth && height <= mHeight);
            Rect2i rect = mTmpRect;
            rect.set(0, 0, width, height);
            if (!mRectanglePacker.addRect(rect)) {
                return false;
            }

            mDirtyRect.join(rect);
            rect.offset(mOffsetX, mOffsetY);
            atlasLocator.setRect(rect);

            return true;
        }

        public long dataAt(AtlasLocator atlasLocator) {
            long bpp = mBytesPerPixel;
            if (mData == MemoryUtil.NULL) {
                mData = MemoryUtil.nmemAlloc(bpp * mWidth * mHeight);
                if (mData == MemoryUtil.NULL) {
                    throw new OutOfMemoryError();
                }
            }
            // point ourselves at the right starting spot
            long dataPtr = mData;
            int x = atlasLocator.u1 & 0xFFFF;
            int y = atlasLocator.v1 & 0xFFFF;
            // Assert if we're not accessing the correct Plot
            assert (x >= mOffsetX && x < mOffsetX + mWidth &&
                    y >= mOffsetY && y < mOffsetY + mHeight);
            x -= mOffsetX;
            y -= mOffsetY;
            dataPtr += bpp * mWidth * y;
            dataPtr += bpp * x;
            return dataPtr;
        }

        /**
         * Copy sub-image, src data must be tightly packed.
         * This must be called and can only be called once after
         * {@link #addRect(int, int, AtlasLocator)}.
         */
        public void copySubImage(AtlasLocator atlasLocator,
                                 Object srcBase, long srcAddr) {
            long bpp = mBytesPerPixel;
            long dstAddr = dataAt(atlasLocator);
            int w = atlasLocator.width() & 0xFFFF;
            int h = atlasLocator.height() & 0xFFFF;

            PixelUtils.copyImage(
                    srcBase, srcAddr, bpp * w,
                    null, dstAddr, bpp * mWidth,
                    bpp * w, h
            );
        }

        /**
         * To manage the lifetime of a plot, we use last use token to determine when
         * we can evict a plot from the cache, i.e. if the last use
         * has already flushed through the gpu then we can reuse the plot.
         */
        public long getLastUseToken() {
            return mLastUseToken;
        }

        public void setLastUseToken(long token) {
            assert token != AtlasToken.INVALID_TOKEN;
            mLastUseToken = token;
        }

        public int numFlushesSinceLastUsed() {
            return mFlushesSinceLastUsed;
        }

        public void incFlushesSinceLastUsed() {
            mFlushesSinceLastUsed++;
        }

        public void resetFlushesSinceLastUsed() {
            mFlushesSinceLastUsed = 0;
        }

        /**
         * Returns true if there's dirty data.
         */
        public boolean needsUpload() {
            return !mDirtyRect.isEmpty();
        }

        /**
         * Returns aligned, read-only memory address to the image data to upload,
         * as it will be copied into a staging buffer. The sub-image region will be
         * stored in <var>outRect</var> in integer texture coordinates.
         */
        public long prepareForUpload(Rect2i outRect) {
            assert needsUpload();
            if (mData == MemoryUtil.NULL) {
                outRect.setEmpty();
                return MemoryUtil.NULL;
            }
            // Clamp to 4-byte aligned boundaries
            int clearBits = 0x3 / mBytesPerPixel;
            mDirtyRect.mLeft &= ~clearBits;
            mDirtyRect.mRight += clearBits;
            mDirtyRect.mRight &= ~clearBits;
            assert (mDirtyRect.mRight <= mWidth);
            // Set up pointer
            long dstAddr = mData;
            dstAddr += (long) mBytesPerPixel * mWidth * mDirtyRect.y();
            dstAddr += (long) mBytesPerPixel * mDirtyRect.x();
            outRect.set(mDirtyRect);
            outRect.offset(mOffsetX, mOffsetY);

            mDirtyRect.setEmpty();

            return dstAddr;
        }

        /**
         * Reset the Plot.
         * <p>
         * Zero out all the tracked data, {@link #getGeneration()} is incremented.
         */
        public void clear() {
            mRectanglePacker.clear();
            setGeneration(mGenerationCounter.next());
            mLastUseToken = AtlasToken.INVALID_TOKEN;

            // zero out the plot
            if (mData != MemoryUtil.NULL) {
                MemoryUtil.memSet(mData, 0, (long) mBytesPerPixel * mWidth * mHeight);
            }

            mDirtyRect.setEmpty();
        }
    }

    private static class Page {
        // allocated array of Plots
        Plot[] mPlots;
        // LRU list of Plots (MRU at head - LRU at tail)
        Plot mHead;
        Plot mTail;

        @SharedPtr
        ImageViewProxy mTexture;

        Page() {
        }

        void addToHead(Plot entry) {
            entry.mPrev = null;
            entry.mNext = mHead;
            if (mHead != null) {
                mHead.mPrev = entry;
            }
            mHead = entry;
            if (mTail == null) {
                mTail = entry;
            }
        }

        void moveToHead(Plot entry) {
            assert mHead != null && mTail != null;
            if (mHead == entry) {
                return;
            }

            Plot prev = entry.mPrev;
            Plot next = entry.mNext;

            if (prev != null) {
                prev.mNext = next;
            } else {
                mHead = next;
            }
            if (next != null) {
                next.mPrev = prev;
            } else {
                mTail = prev;
            }

            entry.mPrev = null;
            entry.mNext = mHead;
            if (mHead != null) {
                mHead.mPrev = entry;
            }
            mHead = entry;
            if (mTail == null) {
                mTail = entry;
            }
        }
    }

    private final Page[] mPages;

    private int mNumActivePages;

    private final int mColorType;
    private final int mBytesPerPixel;

    private final int mTextureWidth;
    private final int mTextureHeight;
    private final int mPlotWidth;
    private final int mPlotHeight;

    private final int mNumPlots;

    private final boolean mUseStorageTextures;
    private final String mLabel;

    // A counter to track the atlas eviction state for Glyphs. Each Glyph has a PlotLocator
    // which contains its current generation. When the atlas evicts a plot, it increases
    // the generation counter. If a Glyph's generation is less than the atlas's
    // generation, then it knows it's been evicted and is either free to be deleted or
    // re-added to the atlas if necessary.
    private final AtlasGenerationCounter mGenerationCounter;
    private long mAtlasGeneration;

    private PlotEvictionCallback mEvictionCallback;

    // nextFlushToken() value at the end of the previous DrawPass
    private long mPrevFlushToken;

    // the number of flushes since this atlas has been last used
    private int mFlushesSinceLastUsed;

    public DrawAtlas(@ColorInfo.ColorType int ct,
                     int width, int height,
                     int plotWidth, int plotHeight,
                     @Nonnull AtlasGenerationCounter generationCounter,
                     boolean useMultiPages,
                     boolean useStorageTextures,
                     String label) {
        mColorType = ct;
        mBytesPerPixel = ColorInfo.bytesPerPixel(ct);

        assert MathUtil.isPow2(width) && MathUtil.isPow2(height);
        int numPlotsX = width / plotWidth;
        int numPlotsY = height / plotHeight;
        int numPlots = numPlotsX * numPlotsY;
        assert numPlots <= PlotLocator.MAX_PLOTS;
        assert plotWidth * numPlotsX == width;
        assert plotHeight * numPlotsY == height;
        mTextureWidth = width;
        mTextureHeight = height;
        mPlotWidth = plotWidth;
        mPlotHeight = plotHeight;
        mNumPlots = numPlots;

        mUseStorageTextures = useStorageTextures;
        mLabel = label;

        mGenerationCounter = generationCounter;
        mAtlasGeneration = generationCounter.next();

        mPrevFlushToken = AtlasToken.INVALID_TOKEN;
        mFlushesSinceLastUsed = 0;

        // allocate pages
        mPages = new Page[useMultiPages
                ? PlotLocator.MAX_PAGES
                : 1];
        for (int pageIndex = 0; pageIndex < mPages.length; pageIndex++) {
            Page page = mPages[pageIndex] = new Page();
            // set up allocated plots
            page.mPlots = new Plot[numPlots];
            int i = 0;
            for (int y = numPlotsY - 1, r = 0; y >= 0; --y, ++r) {
                for (int x = numPlotsX - 1, c = 0; x >= 0; --x, ++c) {
                    int plotIndex = r * numPlotsX + c;
                    Plot plot = new Plot(
                            pageIndex, plotIndex, generationCounter, x, y,
                            mPlotWidth, mPlotHeight, mColorType, mBytesPerPixel
                    );
                    assert i == plotIndex;
                    page.mPlots[i++] = plot;
                    // build LRU list
                    page.addToHead(plot);
                }
            }
            assert i == numPlots;
        }
    }

    /**
     * Creates a DrawAtlas.
     *
     * @param ct                 The colorType which this atlas will store.
     * @param width              Width in pixels of the atlas.
     * @param height             Height in pixels of the atlas.
     * @param plotWidth          The width of each plot. width/plotWidth should be an integer.
     * @param plotHeight         The height of each plot. height/plotHeight should be an integer.
     * @param generationCounter  A pointer to the context's generation counter.
     * @param useMultiPages      Can the atlas use more than one texture.
     * @param useStorageTextures Should the atlas use storage textures.
     * @param evictor            A pointer to an eviction callback class.
     * @param label              Label for texture resources.
     */
    @Nonnull
    public static DrawAtlas make(@ColorInfo.ColorType int ct,
                                 int width, int height,
                                 int plotWidth, int plotHeight,
                                 @Nonnull AtlasGenerationCounter generationCounter,
                                 boolean useMultiPages,
                                 boolean useStorageTextures,
                                 PlotEvictionCallback evictor,
                                 String label) {
        DrawAtlas atlas = new DrawAtlas(
                ct,
                width, height,
                plotWidth, plotHeight,
                generationCounter,
                useMultiPages,
                useStorageTextures,
                label
        );
        if (evictor != null) {
            atlas.mEvictionCallback = evictor;
        }
        return atlas;
    }

    @Override
    public void close() {
        for (Page page : mPages) {
            for (Plot plot : page.mPlots) {
                plot.close();
            }
            page.mPlots = null;
            page.mTexture = RefCnt.move(page.mTexture);
        }
    }

    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_FAILURE = 1;
    public static final int RESULT_TRY_AGAIN = 2;

    /**
     * Adds a width x height sub-image to the atlas. Upon success, it returns {@link #RESULT_SUCCESS}
     * and returns the plot location and the sub-image's coordinates in the backing texture.
     * {@link #RESULT_TRY_AGAIN} is returned if the sub-image cannot fit in the atlas without overwriting
     * texels that will be read in the current list of draws. This indicates that the {@link Device_Granite}
     * should end its current draw, snap a {@link DrawPass}, and begin another before adding more data.
     * {@link #RESULT_FAILURE} will be returned when some unrecoverable error was encountered while trying
     * to add the sub-image. In this case the draw being created should be discarded.
     * <p>
     * This tracking does not generate {@link ImageUploadTask UploadTasks} per se. Instead, when the
     * {@link RenderPassTask} is ready to be snapped, {@link #recordUploads}
     * will be called by the {@link Device_Granite} and that will generate the necessary
     * {@link ImageUploadTask UploadTasks}.
     * <p>
     * NOTE: When a draw that reads from the atlas is added to the DrawList, the client using this
     * DrawAtlas must immediately call 'setLastUseToken' with the currentToken from the Recorder,
     * otherwise the next call to addToAtlas might cause the previous data to be overwritten before
     * it has been read.
     */
    public int addRect(@Nonnull RecordingContext context,
                       int width, int height,
                       @Nonnull AtlasLocator atlasLocator) {
        if (width > mPlotWidth || height > mPlotHeight || width < 0 || height < 0) {
            return RESULT_FAILURE;
        }

        // We permit zero-sized rects to allow inverse fills in the PathAtlases to work,
        // but we don't want to enter them in the RectanglePacker. So we handle this special case here.
        // For text this should be caught at a higher level, but if not the only end result
        // will be rendering a degenerate quad.
        if (width == 0 || height == 0) {
            if (mNumActivePages == 0) {
                // Make sure we have a Page for the AtlasLocator to refer to
                if (!activateNextPage(context)) {
                    return RESULT_FAILURE;
                }
            }
            atlasLocator.setRect(Rect2i.empty());
            // Use the MRU Plot from the first Page
            atlasLocator.setLocation(mPages[0].mHead);
            return RESULT_SUCCESS;
        }

        // Look through each page to see if we can upload without having to flush
        // We prioritize this upload to the first pages, not the most recently used, to make it easier
        // to remove unused pages in reverse page order.
        for (int pageIndex = 0; pageIndex < mNumActivePages; ++pageIndex) {
            if (addRectToPage(pageIndex, width, height, atlasLocator)) {
                return RESULT_SUCCESS;
            }
        }

        if (mNumActivePages < getMaxPages()) {
            // If we haven't activated all the available pages, try to create a new one and add to it
            if (activateNextPage(context)) {
                if (addRectToPage(mNumActivePages - 1, width, height, atlasLocator)) {
                    return RESULT_SUCCESS;
                } else {
                    assert false;
                    return RESULT_FAILURE;
                }
            }
        }

        if (mNumActivePages == 0) {
            // There's no page at all...
            return RESULT_FAILURE;
        }

        // If the above fails, then see if the least recently used plot per page has already been
        // queued for upload if we're at max page allocation, or if the plot has aged out otherwise.
        // We wait until we've grown to the full number of pages to begin evicting already queued
        // plots so that we can maximize the opportunity for reuse.
        // As before we prioritize this upload to the first pages, not the most recently used.
        for (int pageIndex = 0; pageIndex < mNumActivePages; ++pageIndex) {
            Page page = mPages[pageIndex];
            Plot plot = page.mTail;
            if (AtlasToken.compare(plot.getLastUseToken(), context.getAtlasTokenTracker().nextFlushToken()) < 0) {
                evictAndReset(plot);
                if (plot.addRect(width, height, atlasLocator)) {
                    page.moveToHead(plot);
                    atlasLocator.setLocation(plot);
                    return RESULT_SUCCESS;
                } else {
                    assert false;
                    return RESULT_FAILURE;
                }
            }
        }

        // All plots are currently in use by the current set of draws, so we need to fail. This
        // gives the Device a chance to snap the current set of uploads and draws, advance the draw
        // token, and call back into this function. The subsequent call will have plots available
        // for fresh uploads.
        return RESULT_TRY_AGAIN;
    }

    /**
     * Returns the pointer to the plot data at the given position.
     * The row bytes of the dst is bpp * plotWidth in the constructor.
     * This must be called and can only be called once after
     * {@link #addRect(RecordingContext, int, int, AtlasLocator)} to update the data.
     */
    public long getDataAt(@Nonnull AtlasLocator atlasLocator) {
        Plot plot = getPlot(atlasLocator);
        return plot.dataAt(atlasLocator);
    }

    /**
     * This is a combination of {@link #addRect(RecordingContext, int, int, AtlasLocator)},
     * and copy the existing data to {@link #getDataAt(AtlasLocator)} if success.
     */
    public int addToAtlas(@Nonnull RecordingContext context,
                          int width, int height,
                          Object imageBase, long imageAddr,
                          @Nonnull AtlasLocator atlasLocator) {
        int res = addRect(context, width, height, atlasLocator);
        if (res == RESULT_SUCCESS) {
            Plot plot = getPlot(atlasLocator);
            plot.copySubImage(atlasLocator, imageBase, imageAddr);
        }
        return res;
    }

    public boolean recordUploads(RecordingContext context,
                                 SurfaceDrawContext sdc) {
        for (int pageIndex = 0; pageIndex < mNumActivePages; ++pageIndex) {
            Page page = mPages[pageIndex];
            for (Plot plot = page.mHead; plot != null; plot = plot.mNext) {
                if (plot.needsUpload()) {
                    ImageViewProxy texture = page.mTexture;
                    assert texture != null;

                    var dstRect = new Rect2i();
                    long dataPtr = plot.prepareForUpload(dstRect);
                    if (dstRect.isEmpty()) {
                        continue;
                    }

                    var levels = new ImageUploadTask.MipLevel[]{
                            new ImageUploadTask.MipLevel(null, dataPtr, mBytesPerPixel * mPlotWidth)
                    };

                    // This is step one in fixing the below bug. Part of the solution is not updating the
                    // TokenTracker when a child layer flushes alone. This gives slightly less efficient usage but
                    // guarantees that the parent will not overwrite Plots that it has draws for but that haven't
                    // been flushed yet.  However, doing this exposes an issue with conditional uploads, where a
                    // follow-on upload was not being added to the CommandBuffer because its token value was the same
                    // as the previous one. We aren't going to use this system to solve out-of-order Recording
                    // playback, so we might as well switch back to the "Iron Fist" method so this bug can be addressed.

                    // Src and dst colorInfo are the same
                    if (!sdc.recordUpload(context, RefCnt.create(texture),
                            mColorType, ColorInfo.AT_UNKNOWN, null,
                            mColorType, ColorInfo.AT_UNKNOWN, null,
                            levels, dstRect, /*uploadCondition*/ null)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @RawPtr
    public ImageViewProxy getTexture(int pageIndex) {
        return mPages[pageIndex].mTexture;
    }

    public long getAtlasGeneration() {
        return mAtlasGeneration;
    }

    public int getNumActivePages() {
        return mNumActivePages;
    }

    public int getNumPlots() {
        return mNumPlots;
    }

    public int getMaxPages() {
        return mPages.length;
    }

    public int getPlotWidth() {
        return mPlotWidth;
    }

    public int getPlotHeight() {
        return mPlotHeight;
    }

    public boolean contains(@Nonnull PlotLocator plotLocator) {
        if (!plotLocator.isValid()) {
            return false;
        }
        int plotIndex = plotLocator.getPlotIndex();
        int pageIndex = plotLocator.getPageIndex();
        long plotGeneration = mPages[pageIndex].mPlots[plotIndex].getGeneration();
        long locatorGeneration = plotLocator.getGeneration();
        return plotIndex < mNumPlots && pageIndex < mNumActivePages &&
                plotGeneration == locatorGeneration;
    }

    public void setLastUseToken(@Nonnull AtlasLocator atlasLocator, long token) {
        assert contains(atlasLocator);
        int plotIndex = atlasLocator.getPlotIndex();
        int pageIndex = atlasLocator.getPageIndex();
        Page page = mPages[pageIndex];
        Plot plot = page.mPlots[plotIndex];
        // make the plot MRU
        // No MRU update for pages -- since we will always try to add from
        // the front and remove from the back there is no need for MRU.
        page.moveToHead(plot);
        plot.setLastUseToken(token);
    }

    public void setLastUseTokenBulk(@Nonnull PlotBulkUseUpdater updater, long token) {
        int count = updater.count();
        for (int i = 0; i < count; i++) {
            int data = updater.dataAt(i);
            int plotIndex = data & 0xFFFF;
            int pageIndex = data >>> 16;
            // it's possible we've added a plot to the updater and subsequently the plot's page
            // was deleted -- so we check to prevent a crash
            if (pageIndex < mNumActivePages) {
                Page page = mPages[pageIndex];
                Plot plot = page.mPlots[plotIndex];
                // make the plot MRU
                // No MRU update for pages -- since we will always try to add from
                // the front and remove from the back there is no need for MRU.
                page.moveToHead(plot);
                plot.setLastUseToken(token);
            }
        }
    }

    public void compact(long startTokenForNextFlush) {
        if (mNumActivePages == 0) {
            mPrevFlushToken = startTokenForNextFlush;
            return;
        }

        // For all plots, reset number of flushes since used if used this frame.
        boolean atlasUsedThisFlush = false;
        for (int pageIndex = 0; pageIndex < mNumActivePages; ++pageIndex) {
            Page page = mPages[pageIndex];
            for (Plot plot = page.mHead; plot != null; plot = plot.mNext) {
                // Reset number of flushes since used
                if (AtlasToken.inInterval(plot.getLastUseToken(),
                        mPrevFlushToken, startTokenForNextFlush)) {
                    plot.resetFlushesSinceLastUsed();
                    atlasUsedThisFlush = true;
                }
            }
        }

        if (atlasUsedThisFlush) {
            mFlushesSinceLastUsed = 0;
        } else {
            ++mFlushesSinceLastUsed;
        }

        // We only try to compact if the atlas was used in the recently completed flush or
        // hasn't been used in a long time.
        // This is to handle the case where a lot of text or path rendering has occurred but then just
        // a blinking cursor is drawn.
        if (atlasUsedThisFlush || mFlushesSinceLastUsed > 1200) {
            ObjectArrayList<Plot> availablePlots = null;
            int lastPageIndex = mNumActivePages - 1;

            if (lastPageIndex > 0) {
                availablePlots = new ObjectArrayList<>();
                // For all plots but the last one, update number of flushes since used, and check to see
                // if there are any in the first pages that the last page can safely upload to.
                for (int pageIndex = 0; pageIndex < lastPageIndex; ++pageIndex) {
                    Page page = mPages[pageIndex];
                    for (Plot plot = page.mHead; plot != null; plot = plot.mNext) {
                        // Update number of flushes since plot was last used
                        // We only increment the 'sinceLastUsed' count for flushes where the atlas was used
                        // to avoid deleting everything when we return to text drawing in the blinking
                        // cursor case
                        if (!AtlasToken.inInterval(plot.getLastUseToken(),
                                mPrevFlushToken, startTokenForNextFlush)) {
                            plot.incFlushesSinceLastUsed();
                        }

                        // Count plots we can potentially upload to in all pages except the last one
                        // (the potential compactee).
                        if (plot.numFlushesSinceLastUsed() > 300) {
                            availablePlots.push(plot);
                        }
                    }
                }
            }

            // Count recently used plots in the last page and evict any that are no longer in use.
            // Since we prioritize uploading to the first pages, this will eventually
            // clear out usage of this page unless we have a large need.
            Page lastPage = mPages[lastPageIndex];
            int usedPlots = 0;
            for (Plot plot = lastPage.mHead; plot != null; plot = plot.mNext) {
                // Update number of flushes since plot was last used
                if (!AtlasToken.inInterval(plot.getLastUseToken(),
                        mPrevFlushToken, startTokenForNextFlush)) {
                    plot.incFlushesSinceLastUsed();
                }

                // If this plot was used recently
                if (plot.numFlushesSinceLastUsed() <= 300) {
                    ++usedPlots;
                } else if (plot.getLastUseToken() != AtlasToken.INVALID_TOKEN) {
                    // otherwise if aged out just evict it.
                    evictAndReset(plot);
                }
            }

            // If recently used plots in the last page are using less than a quarter of the page, try
            // to evict them if there's available space in lower index pages. Since we prioritize
            // uploading to the first pages, this will eventually clear out usage of this page unless
            // we have a large need.
            if (availablePlots != null && !availablePlots.isEmpty() && usedPlots <= mNumPlots / 4) {
                for (Plot plot = lastPage.mHead; plot != null; plot = plot.mNext) {
                    // If this plot was used recently
                    if (plot.numFlushesSinceLastUsed() <= 300) {
                        // See if there's room in a lower index page and if so evict.
                        // We need to be somewhat harsh here so that a handful of plots that are
                        // consistently in use don't end up locking the page in memory.
                        if (!availablePlots.isEmpty()) {
                            evictAndReset(plot);
                            evictAndReset(availablePlots.pop());
                            --usedPlots;
                        }
                        if (usedPlots == 0 || availablePlots.isEmpty()) {
                            break;
                        }
                    }
                }
            }

            // If none of the plots in the last page have been used recently, delete it.
            if (usedPlots == 0) {
                deactivateLastPage();
                mFlushesSinceLastUsed = 0;
            }
        }

        mPrevFlushToken = startTokenForNextFlush;
    }

    public void evictAllPlots() {
        for (int pageIndex = 0; pageIndex < mNumActivePages; ++pageIndex) {
            Page page = mPages[pageIndex];
            for (Plot plot = page.mHead; plot != null; plot = plot.mNext) {
                evictAndReset(plot);
            }
        }
    }

    private Plot getPlot(@Nonnull AtlasLocator atlasLocator) {
        assert contains(atlasLocator);
        int plotIndex = atlasLocator.getPlotIndex();
        int pageIndex = atlasLocator.getPageIndex();
        return mPages[pageIndex].mPlots[plotIndex];
    }

    private boolean activateNextPage(@Nonnull RecordingContext context) {
        assert mNumActivePages < getMaxPages();
        assert mPages[mNumActivePages].mTexture == null;

        Caps caps = context.getCaps();
        ImageDesc desc = caps.getDefaultColorImageDesc(
                Engine.ImageType.k2D,
                mColorType,
                mTextureWidth, mTextureHeight,
                1,
                ISurface.FLAG_SAMPLED_IMAGE |
                        (mUseStorageTextures ? ISurface.FLAG_STORAGE_IMAGE : 0)
        );
        if (desc == null) {
            return false;
        }
        // Remember that for DrawAtlas, we do not use texture swizzle, then the
        // corresponding geometry step must handle the swizzle in shader code
        mPages[mNumActivePages].mTexture = ImageViewProxy.make(
                context,
                desc,
                Engine.SurfaceOrigin.kUpperLeft,
                Swizzle.RGBA,
                true,
                mLabel
        );
        if (mPages[mNumActivePages].mTexture == null) {
            return false;
        }

        ++mNumActivePages;
        return true;
    }

    private void deactivateLastPage() {
        assert mNumActivePages > 0;

        Page lastPage = mPages[mNumActivePages - 1];

        int numPlotsX = mTextureWidth / mPlotWidth;
        int numPlotsY = mTextureHeight / mPlotHeight;

        lastPage.mHead = null;
        lastPage.mTail = null;
        for (int r = 0; r < numPlotsY; ++r) {
            for (int c = 0; c < numPlotsX; ++c) {
                int plotIndex = r * numPlotsX + c;

                Plot currPlot = lastPage.mPlots[plotIndex];
                currPlot.clear();
                currPlot.resetFlushesSinceLastUsed();

                // rebuild the LRU list
                lastPage.addToHead(currPlot);
            }
        }

        // remove ref to the texture proxy
        lastPage.mTexture = RefCnt.move(lastPage.mTexture);
        --mNumActivePages;
    }

    private boolean addRectToPage(int pageIndex, int width, int height,
                                  AtlasLocator atlasLocator) {
        assert mPages[pageIndex].mTexture != null;

        // look through all allocated plots for one we can share, in Most Recently Used order
        Page page = mPages[pageIndex];
        for (Plot plot = page.mHead; plot != null; plot = plot.mNext) {
            if (plot.addRect(width, height, atlasLocator)) {
                page.moveToHead(plot);
                atlasLocator.setLocation(plot);
                return true;
            }
        }

        return false;
    }

    private void evictAndReset(Plot plot) {
        if (mEvictionCallback != null) {
            mEvictionCallback.onEvict(plot);
        }

        mAtlasGeneration = mGenerationCounter.next();

        plot.clear();
    }
}
