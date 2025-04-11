/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.sketch;

import icyllis.arc3d.core.MathUtil;
import icyllis.arc3d.core.Rect2f;
import icyllis.arc3d.core.Rect2fc;
import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.RefCounted;
import org.jspecify.annotations.NonNull;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

/**
 * This class holds {@link Path} data.
 * <br>This class can be shared/accessed between multiple threads.
 * <p>
 * The reference count is only used to check whether these internal buffers
 * are uniquely referenced by a Path object, otherwise when any of these
 * shared Path objects are modified, these buffers will be copied.
 *
 * @see #unique()
 */
final class PathRef implements RefCounted {

    private static final VarHandle USAGE_CNT;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            USAGE_CNT = lookup.findVarHandle(PathRef.class, "mUsageCnt", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static final byte[] EMPTY_VERBS = {};
    static final float[] EMPTY_COORDS = {};

    static final PathRef EMPTY;

    static {
        EMPTY = new PathRef();
        EMPTY.updateBounds();
    }

    transient volatile int mUsageCnt = 1;

    // unsorted and finite = dirty
    final Rect2f mBounds = new Rect2f(0, 0, -1, -1);

    byte[] mVerbs;
    float[] mCoords; // x0 y0 x1 y1 x2 y2 ...

    int mVerbSize;
    int mCoordSize;

    @Path.SegmentMask
    byte mSegmentMask;

    PathRef() {
        this(EMPTY_VERBS, EMPTY_COORDS);
    }

    PathRef(int verbSize, int coordSize) {
        assert coordSize % 2 == 0;
        if (verbSize > 0) {
            assert coordSize > 0;
            mVerbs = new byte[verbSize];
            mCoords = new float[coordSize];
        } else {
            mVerbs = EMPTY_VERBS;
            mCoords = EMPTY_COORDS;
        }
    }

    PathRef(byte[] verbs, float[] coords) {
        assert coords.length % 2 == 0;
        mVerbs = verbs;
        mCoords = coords;
    }

    @SuppressWarnings("IncompleteCopyConstructor")
    PathRef(@NonNull PathRef other) {
        mBounds.set(other.mBounds);
        // trim
        if (other.mVerbSize > 0) {
            assert other.mCoordSize > 0;
            mVerbs = Arrays.copyOf(other.mVerbs, other.mVerbSize);
            mCoords = Arrays.copyOf(other.mCoords, other.mCoordSize);
        } else {
            mVerbs = EMPTY_VERBS;
            mCoords = EMPTY_COORDS;
        }
        mVerbSize = other.mVerbSize;
        mCoordSize = other.mCoordSize;
        mSegmentMask = other.mSegmentMask;
    }

    PathRef(@NonNull PathRef other, int incVerbs, int incCoords) {
        assert incVerbs >= 0 && incCoords >= 0;
        assert incCoords % 2 == 0;
        mVerbs = new byte[other.mVerbSize + incVerbs];
        mCoords = new float[other.mCoordSize + incCoords];
        mBounds.set(other.mBounds);
        System.arraycopy(other.mVerbs, 0, mVerbs, 0, other.mVerbSize);
        System.arraycopy(other.mCoords, 0, mCoords, 0, other.mCoordSize);
        mVerbSize = other.mVerbSize;
        mCoordSize = other.mCoordSize;
        mSegmentMask = other.mSegmentMask;
    }

    boolean unique() {
        return (int) USAGE_CNT.getAcquire(this) == 1
                // the EMPTY's usage is not counted normally, must be excluded here!!
                && this != EMPTY;
    }

    @Override
    public void ref() {
        USAGE_CNT.getAndAddAcquire(this, 1);
    }

    @Override
    public void unref() {
        USAGE_CNT.getAndAdd(this, -1);
    }

    void createTransformedCopy(Matrixc matrix, Path dstPath) {
        if (matrix.isIdentity()) {
            if (this != dstPath.mPathRef) {
                dstPath.mPathRef = RefCnt.create(dstPath.mPathRef, this);
            }
            return;
        }

        boolean keepThisAlive = false;
        if (!dstPath.mPathRef.unique()) {
            // If dst and src are the same then we are about to drop our only ref on the common path
            // ref. Some other thread may have owned src when we checked unique() above but it may not
            // continue to do so. Add another ref so we continue to be an owner until we're done.
            if (dstPath.mPathRef == this) {
                ref();
                keepThisAlive = true;
            }
            dstPath.mPathRef = RefCnt.move(dstPath.mPathRef, new PathRef());
        }

        PathRef dst = dstPath.mPathRef;

        if (this != dst) {
            dst.mVerbs = Arrays.copyOf(mVerbs, mVerbSize);
            dst.mVerbSize = mVerbSize;
            // don't copy, just allocate the points
            if (dst.mCoords.length < mCoordSize) {
                if (dst.mCoords.length == 0) {
                    dst.mCoords = new float[mCoordSize];
                } else {
                    dst.mCoords = Path.growCoords(dst.mCoords, mCoordSize - dst.mCoords.length);
                }
            }
            dst.mCoordSize = mCoordSize;
        }
        matrix.mapPoints(mCoords, dst.mCoords, mCoordSize >> 1);

        dst.dirtyBounds();

        dst.mSegmentMask = mSegmentMask;

        //TODO GenID, specialized PathType, validation...

        if (keepThisAlive) {
            unref();
        }
    }

    void dirtyBounds() {
        mBounds.set(0, 0, -1, -1);
    }

    boolean boundsIsDirty() {
        return !mBounds.isSorted() && mBounds.isFinite();
    }

    void reset() {
        dirtyBounds();
        mSegmentMask = 0;
        mVerbSize = 0;
        mCoordSize = 0;
    }

    void reserve(int incVerbs, int incCoords) {
        assert incVerbs >= 0 && incCoords >= 0;
        assert incCoords % 2 == 0;
        if (mVerbSize > mVerbs.length - incVerbs) { // prevent overflow
            mVerbs = Path.growVerbs(mVerbs, incVerbs - mVerbs.length + mVerbSize);
        }
        if (mCoordSize > mCoords.length - incCoords) { // prevent overflow
            mCoords = Path.growCoords(mCoords, incCoords - mCoords.length + mCoordSize);
        }
    }

    void trimToSize() {
        if (mVerbSize > 0) {
            assert mCoordSize > 0;
            if (mVerbSize < mVerbs.length) {
                mVerbs = Arrays.copyOf(mVerbs, mVerbSize);
            }
            if (mCoordSize < mCoords.length) {
                mCoords = Arrays.copyOf(mCoords, mCoordSize);
            }
        } else {
            mVerbs = EMPTY_VERBS;
            mCoords = EMPTY_COORDS;
        }
    }

    PathRef addVerb(byte verb) {
        int coords = switch (verb) {
            case Path.VERB_MOVE -> 2;
            case Path.VERB_LINE -> {
                mSegmentMask |= Path.SEGMENT_LINE;
                yield 2;
            }
            case Path.VERB_QUAD -> {
                mSegmentMask |= Path.SEGMENT_QUAD;
                yield 4;
            }
            case Path.VERB_CUBIC -> {
                mSegmentMask |= Path.SEGMENT_CUBIC;
                yield 6;
            }
            default -> 0;
        };
        reserve(1, coords);
        mVerbs[mVerbSize++] = verb;
        return this;
    }

    PathRef addPoint(float x, float y) {
        mCoords[mCoordSize++] = x;
        mCoords[mCoordSize++] = y;
        return this;
    }

    void updateBounds() {
        if (boundsIsDirty()) {
            assert mCoordSize % 2 == 0;
            mBounds.setBoundsNoCheck(mCoords, 0, mCoordSize >> 1);
        }
    }

    boolean isFinite() {
        updateBounds();
        return mBounds.isFinite();
    }

    Rect2fc getBounds() {
        if (isFinite()) {
            return mBounds;
        }
        return Rect2f.empty();
    }

    long estimatedByteSize() {
        if (this == EMPTY) {
            return 0;
        }
        long size = 16 + 32 + 16 + 16;
        if (mVerbs != EMPTY_VERBS) {
            size += 16 + MathUtil.align8(mVerbs.length);
        }
        if (mCoords != EMPTY_COORDS) {
            assert mCoords.length % 2 == 0;
            size += 16 + ((long) mCoords.length << 2);
        }
        return size;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        for (int i = 0, count = mVerbSize; i < count; i++) {
            hash = 11 * hash + mVerbs[i];
        }
        for (int i = 0, count = mCoordSize; i < count; i++) {
            hash = 11 * hash + Float.floatToIntBits(mCoords[i]);
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PathRef other)) {
            return false;
        }
        // quick reject
        if (mSegmentMask != other.mSegmentMask) {
            return false;
        }
        if (mCoordSize != other.mCoordSize) {
            return false;
        }
        if (!Arrays.equals(
                mVerbs, 0, mVerbSize,
                other.mVerbs, 0, other.mVerbSize)) {
            return false;
        }
        // use IEEE comparison rather than memory comparison
        for (int i = 0, count = mCoordSize; i < count; i++) {
            if (mCoords[i] != other.mCoords[i]) {
                return false;
            }
        }
        return true;
    }
}
