/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.MathUtil;
import icyllis.arc3d.core.SLDataType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * Describes the vertex input state of a graphics pipeline.
 */
@Immutable
public final class VertexInputLayout {

    /**
     * Input rates other than 0 and 1 are not supported.
     */
    public static final int INPUT_RATE_VERTEX = 0;      // per-vertex data
    public static final int INPUT_RATE_INSTANCE = 1;    // per-instance data

    /**
     * Describes a vertex or instance attribute.
     */
    @Immutable
    public static class Attribute {

        // 1 is not valid because it isn't aligned.
        static final int IMPLICIT_OFFSET = 1;

        public static final int OFFSET_ALIGNMENT = 4;

        /**
         * It must be N-aligned for all types, where N is sizeof(float).
         */
        public static int alignOffset(int offset) {
            // OFFSET_ALIGNMENT = 4
            return MathUtil.align4(offset);
        }

        private final String mName;
        private final byte mSrcType;
        private final byte mDstType;
        private final short mOffset;

        /**
         * Makes an attribute whose offset will be implicitly determined by the types and ordering
         * of an array attributes.
         *
         * @param name    the attrib name, cannot be null or empty
         * @param srcType the data type in vertex buffer, see {@link Engine.VertexAttribType}
         * @param dstType the data type in vertex shader, see {@link SLDataType}
         */
        public Attribute(@NonNull String name, byte srcType, byte dstType) {
            if (name.isEmpty() || name.startsWith("_")) {
                throw new IllegalArgumentException();
            }
            if (srcType < 0 || srcType > Engine.VertexAttribType.kLast) {
                throw new IllegalArgumentException();
            }
            if (SLDataType.locations(dstType) <= 0) {
                throw new IllegalArgumentException();
            }
            mName = name;
            mSrcType = srcType;
            mDstType = dstType;
            mOffset = IMPLICIT_OFFSET;
        }

        /**
         * Makes an attribute with an explicit offset.
         *
         * @param name    the attrib name, UpperCamelCase, cannot be null or empty
         * @param srcType the data type in vertex buffer, see {@link Engine.VertexAttribType}
         * @param dstType the data type in vertex shader, see {@link SLDataType}
         * @param offset  N-aligned offset
         */
        public Attribute(@NonNull String name, byte srcType, byte dstType, int offset) {
            if (name.isEmpty() || name.startsWith("_")) {
                throw new IllegalArgumentException();
            }
            if (srcType < 0 || srcType > Engine.VertexAttribType.kLast) {
                throw new IllegalArgumentException();
            }
            if (SLDataType.locations(dstType) <= 0) {
                throw new IllegalArgumentException();
            }
            if (offset < 0 || offset >= 32768 || alignOffset(offset) != offset) {
                throw new IllegalArgumentException();
            }
            mName = name;
            mSrcType = srcType;
            mDstType = dstType;
            mOffset = (short) offset;
        }

        public final String name() {
            return mName;
        }

        /**
         * @return the data type in vertex buffer, see {@link Engine.VertexAttribType}
         */
        public final byte srcType() {
            return mSrcType;
        }

        /**
         * @return the data type in vertex shader, see {@link SLDataType}
         */
        public final byte dstType() {
            return mDstType;
        }

        /**
         * Returns the offset if attributes were specified with explicit offsets. Otherwise,
         * offsets (and total vertex stride) are implicitly determined from attribute order and
         * types. See {@link #IMPLICIT_OFFSET}.
         */
        public final int offset() {
            assert mOffset >= 0;
            return mOffset;
        }

        /**
         * @return the size of the source data in bytes
         */
        public final int size() {
            return Engine.VertexAttribType.size(mSrcType);
        }

        /**
         * @return the number of locations
         */
        public final int locations() {
            return SLDataType.locations(mDstType);
        }

        /**
         * @return the total size for this attribute in bytes
         */
        public final int stride() {
            int size = size();
            int count = locations();
            assert (size > 0 && count > 0);
            return size * count;
        }

        @NonNull
        public final ShaderVar asShaderVar() {
            return new ShaderVar(mName, mDstType, ShaderVar.kIn_TypeModifier);
        }
    }

    /**
     * A set of attributes that can iterated.
     */
    @Immutable
    public static class AttributeSet implements Iterable<Attribute> {

        private final Attribute[] mAttributes;
        private final int mStride;
        private final int mInputRate;

        final int mAllMask;

        private AttributeSet(Attribute @NonNull[] attributes, int stride, int inputRate) {
            int offset = 0;
            for (Attribute attr : attributes) {
                if (attr.offset() != Attribute.IMPLICIT_OFFSET) {
                    // offset must be not descending no matter what the mask is
                    if (attr.offset() < offset) {
                        throw new IllegalArgumentException();
                    }
                    offset = attr.offset();
                    assert Attribute.alignOffset(offset) == offset;
                }
            }
            mAttributes = attributes;
            mStride = stride;
            mInputRate = inputRate;
            mAllMask = ~0 >>> (Integer.SIZE - mAttributes.length);
        }

        /**
         * Create an attribute set with an implicit stride. Each attribute can either
         * have an implicit offset or an explicit offset aligned to 4 bytes. No attribute
         * can cross stride boundaries.
         * <p>
         * Note: GPU does not reorder vertex attributes, so when a vertex attribute has an
         * explicit offset, the subsequent implicit offsets will start from there.
         */
        @NonNull
        public static AttributeSet makeImplicit(int inputRate, @NonNull Attribute... attrs) {
            if (attrs.length == 0 || attrs.length > Integer.SIZE) {
                throw new IllegalArgumentException();
            }
            return new AttributeSet(attrs, Attribute.IMPLICIT_OFFSET, inputRate);
        }

        /**
         * Create an attribute set with an explicit stride. Each attribute can either
         * have an implicit offset or an explicit offset aligned to 4 bytes. No attribute
         * can cross stride boundaries.
         * <p>
         * Note: GPU does not reorder vertex attributes, so when a vertex attribute has an
         * explicit offset, the subsequent implicit offsets will start from there.
         */
        @NonNull
        public static AttributeSet makeExplicit(int stride, int inputRate, @NonNull Attribute... attrs) {
            if (attrs.length == 0 || attrs.length > Integer.SIZE) {
                throw new IllegalArgumentException();
            }
            if (stride <= 0 || stride > 32768) {
                throw new IllegalArgumentException();
            }
            if (Attribute.alignOffset(stride) != stride) {
                throw new IllegalArgumentException();
            }
            return new AttributeSet(attrs, stride, inputRate);
        }

        final int stride(int mask) {
            if (mStride != Attribute.IMPLICIT_OFFSET) {
                return mStride;
            }
            final int rawCount = mAttributes.length;
            int stride = 0;
            for (int i = 0, bit = 1; i < rawCount; i++, bit <<= 1) {
                final Attribute attr = mAttributes[i];
                if ((mask & bit) != 0) {
                    if (attr.offset() != Attribute.IMPLICIT_OFFSET) {
                        stride = attr.offset();
                    }
                    stride += Attribute.alignOffset(attr.stride());
                }
            }
            return stride;
        }

        final int numLocations(int mask) {
            final int rawCount = mAttributes.length;
            int locations = 0;
            for (int i = 0, bit = 1; i < rawCount; i++, bit <<= 1) {
                final Attribute attr = mAttributes[i];
                if ((mask & bit) != 0) {
                    locations += attr.locations();
                }
            }
            return locations;
        }

        final void appendToKey(@NonNull KeyBuilder b, int mask) {
            final int rawCount = mAttributes.length;
            // max attribs is no less than 16
            b.addBits(6, rawCount, "attribute count");
            int offset = 0;
            for (int i = 0, bit = 1; i < rawCount; i++, bit <<= 1) {
                final Attribute attr = mAttributes[i];
                if ((mask & bit) != 0) {
                    b.addBits(8, attr.srcType() & 0xFF, "attrType");
                    b.addBits(8, attr.dstType() & 0xFF, "attrGpuType");
                    if (attr.offset() != Attribute.IMPLICIT_OFFSET) {
                        offset = attr.offset();
                    }
                    assert (offset >= 0 && offset < 32768);
                    b.addBits(16, offset, "attrOffset");
                    offset += Attribute.alignOffset(attr.stride());
                } else {
                    b.addBits(8, 0xFF, "attrType");
                    b.addBits(8, 0xFF, "attrGpuType");
                    b.addBits(16, 0xFFFF, "attrOffset");
                }
            }
            final int stride;
            if (mStride == Attribute.IMPLICIT_OFFSET) {
                stride = offset;
            } else {
                stride = mStride;
                if (stride < offset) {
                    throw new IllegalStateException();
                }
            }
            // max stride is no less than 2048
            assert (stride > 0 && stride <= 32768);
            assert (Attribute.alignOffset(stride) == stride);
            b.addBits(16, stride, "stride");
        }

        @NonNull
        @Override
        public Iterator<Attribute> iterator() {
            return new Iter(mAllMask);
        }

        class Iter implements Iterator<Attribute> {

            private final int mMask;

            private int mIndex;
            private int mOffset;

            Iter(int mask) {
                mMask = mask;
            }

            @Override
            public boolean hasNext() {
                forward();
                return mIndex < mAttributes.length;
            }

            @NonNull
            @Override
            public Attribute next() {
                forward();
                try {
                    final Attribute ret, curr = mAttributes[mIndex++];
                    if (curr.offset() == Attribute.IMPLICIT_OFFSET) {
                        ret = new Attribute(curr.name(), curr.srcType(), curr.dstType(), mOffset);
                    } else {
                        ret = curr;
                        mOffset = curr.offset();
                    }
                    mOffset += Attribute.alignOffset(curr.stride());
                    return ret;
                } catch (IndexOutOfBoundsException e) {
                    throw new NoSuchElementException(e);
                }
            }

            private void forward() {
                while (mIndex < mAttributes.length && (mMask & (1 << mIndex)) == 0) {
                    mIndex++; // skip unused
                }
            }
        }
    }

    private final AttributeSet[] mAttributeSets;
    private final int[] mMasks;

    /**
     * Enable all attributes for each AttributeSet.
     *
     * @see #VertexInputLayout(AttributeSet[], int[])
     */
    public VertexInputLayout(@NonNull AttributeSet... attributeSets) {
        this(attributeSets, null);
    }

    /**
     * The constructor wraps the two given arrays (no copy), the caller should ensure the immutability.
     * <p>
     * Each AttributeSet contains all attributes for the corresponding binding point.
     * It may be shared across {@link VertexInputLayout} instances, then the <var>masks</var>
     * array is used to control which attributes of the corresponding AttributeSet
     * need to be used. A binding point can be empty but that is discouraged.
     * <p>
     * E.g. if you want the 0, 2, 3 attributes are enabled, then mask is 0b1101.
     */
    public VertexInputLayout(AttributeSet @NonNull[] attributeSets,
            int @Nullable[] masks) {
        assert attributeSets.length > 0 && attributeSets.length <= Caps.MAX_VERTEX_BINDINGS;
        assert masks == null || attributeSets.length == masks.length;
        mAttributeSets = attributeSets;
        if (masks != null) {
            for (int i = 0; i < masks.length; i++) {
                if (masks[i] != 0) {
                    // mask is non-zero then AttributeSet is non-null
                    masks[i] |= attributeSets[i].mAllMask; // sanitize
                }
            }
        }
        mMasks = masks;
    }

    /**
     * Returns the number of binding points.
     */
    public int getBindingCount() {
        return mAttributeSets.length;
    }

    /**
     * Returns the number of used attributes (input variables).
     * Note: attribute of a matrix type counts as just one.
     *
     * @see #getLocationCount(int)
     */
    public int getAttributeCount(int binding) {
        if (mMasks != null) {
            return Integer.bitCount(mMasks[binding]);
        }
        var attributes = mAttributeSets[binding];
        return attributes != null ? attributes.mAttributes.length : 0;
    }

    /**
     * Returns the number of used per-vertex attribute locations (slots).
     * An attribute (variable) may take up multiple consecutive locations.
     * The max number of locations matches the max number of attributes in {@link Caps}.
     *
     * @see SLDataType#locations(byte)
     * @see #getAttributeCount(int)
     * @see Caps#MAX_VERTEX_ATTRIBUTES
     */
    public int getLocationCount(int binding) {
        var attributes = mAttributeSets[binding];
        if (mMasks != null) {
            int mask = mMasks[binding];
            return mask != 0 ? attributes.numLocations(mask) : 0;
        }
        return attributes != null ? attributes.numLocations(attributes.mAllMask) : 0;
    }

    /**
     * Returns the number of bytes from one vertex to the next vertex, including paddings.
     * A common practice is to populate the vertex's memory using an implicit array of
     * structs. In this case, it is best to assert that: stride == sizeof(struct).
     */
    public int getStride(int binding) {
        var attributes = mAttributeSets[binding];
        if (mMasks != null) {
            int mask = mMasks[binding];
            return mask != 0 ? attributes.stride(mask) : 0;
        }
        return attributes != null ? attributes.stride(attributes.mAllMask) : 0;
    }

    /**
     * Returns the input rate for the given binding point. 0 means per-vertex data,
     * 1 means per-instance data.
     */
    public int getInputRate(int binding) {
        var attributes = mAttributeSets[binding];
        return attributes != null ? attributes.mInputRate : 0;
    }

    /**
     * Returns an iterator of used attributes. It's safe to call even if there's no attribute.
     * The iterator handles hides two pieces of complexity:
     * <ol>
     * <li>It skips unused attributes (see mask in constructor).</li>
     * <li>It always returns an attribute with a known offset.</li>
     * </ol>
     */
    @NonNull
    public Iterator<Attribute> getAttributes(int binding) {
        var attributes = mAttributeSets[binding];
        if (mMasks != null) {
            int mask = mMasks[binding];
            return mask != 0 ? attributes.new Iter(mask) : Collections.emptyIterator();
        }
        return attributes != null ? attributes.iterator() : Collections.emptyIterator();
    }
}
