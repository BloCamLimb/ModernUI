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

package icyllis.arc3d.core;

import org.jspecify.annotations.NonNull;
import sun.misc.Unsafe;

/**
 * Represents a CPU glyph + digest information for GPU drawing,
 * managed by {@link Strike}.
 * <p>
 * GlyphID is represented as an int value. For TrueType/OpenType fonts,
 * glyph ID (or glyph index) is a 16-bit unsigned int. For composite fonts
 * (mapping a logical font to a set of physical fonts) in JDK, the slot index
 * is stored in the higher 8 bits, then the 16-24 bits is reserved.
 * <p>
 * Another form of GlyphID is PackedGlyphID, which is also represented by an
 * int value. It stores sub-pixel information based on the GlyphID. We use
 * bit 22 and bit 23 to represent the sub-pixel X positions of glyph rendering at
 * 0.0, 0.25, 0.5, and 0.75 in the horizontal direction.
 * <p>
 * If sub-pixel positioning is not required, then GlyphID and PackedGlyphID are
 * interchangeable.
 *
 * <ul>
 * <li>0-16 bits: glyph index</li>
 * <li>16-22 bits: reserved</li>
 * <li>22-24 bits: sub-pixel X position</li>
 * <li>24-32 bits: slot index</li>
 * </ul>
 */
public final class Glyph {

    // we have 4 sub-pixel positions, use two bits
    // 0.0~0.25, 0.25~0.5, 0.5~0.75, 0.75~1.0
    public static final int kSubPixelPosLen = 2;
    public static final int kSubPixelPosMask = (1 << kSubPixelPosLen) - 1;
    public static final int kSubPixelXShift = 22;
    public static final int kSubPixelXMask = kSubPixelPosMask << kSubPixelXShift;

    public static int packGlyphID(int glyphID, float xPos, int mask) {
        final float magic = 1.0f * (1 << (kSubPixelPosLen + kSubPixelXShift));

        xPos = (xPos - (float) Math.floor(xPos)) + 1.0f;
        int subX = (int) (xPos * magic) & mask;

        assert (glyphID & kSubPixelXMask) == 0;
        assert subX / (1 << kSubPixelXShift) < (1 << kSubPixelPosLen);
        return glyphID | subX;
    }

    public static int getGlyphID(int packedID) {
        return packedID & ~kSubPixelXMask;
    }

    public static float getSubX(int packedID) {
        final float magic = 1.0f / (1 << kSubPixelPosLen);

        int subX = (packedID >> kSubPixelXShift) & kSubPixelPosMask;
        return subX * magic;
    }

    /**
     * Action for methods.
     */
    public static final int
            kUnset_Action = 0,  // must be 0
            kAccept_Action = 1,
            kReject_Action = 2,
            kDrop_Action = 3;
    static final int
            kActionMask = 0x3;

    /**
     * Text rendering methods, values are used as bit shift to obtain action.
     *
     * @see #actionFor(int)
     */
    // action mask is two bits
    public static final int
            kDirectMask = 0,        // nearest sampling
            kTransformedMask = 2,   // bilinear sampling
            kPath = 4,              // draw path
            kDrawable = 6,          // custom draw TODO not implement yet
            kSDF = 8,               // single-channel SDF TODO not implement yet
            kMSDF = 10;             // multi-channel SDF TODO not implement yet

    private static final int kMaxGlyphWidth = 1 << 13;

    static {
        //noinspection ConstantValue
        assert (kMaxGlyphWidth - 1) * ((1 << 16) - 1) * 4 <= Integer.MAX_VALUE;
        // the max glyph image width is 8191
        // the max glyph image height is 65535
        // the max bpp is 4
        // assert (max width * max height * max bpp) <= int max
    }

    static final int kSizeOf = 48;

    // The padding needed for bilinear interpolating the glyph when drawing.
    public static final int BILERP_GLYPH_BORDER = 1;
    // An atlas consists of plots, and plots hold glyphs. The minimum a plot can be is 256x256.
    // This means that the maximum size a glyph can be is 256x256.
    public static final int MAX_ATLAS_DIMENSION = 256;
    public static final int MAX_BILERP_ATLAS_DIMENSION = MAX_ATLAS_DIMENSION - (2 * BILERP_GLYPH_BORDER);

    // The glyph ID
    private final int mID;

    // The offset from the glyphs origin on the baseline to the top left of the glyph mask.
    // Also known as bearing X and bearing Y.
    short mTop = 0;
    short mLeft = 0;

    // The width and height of the glyph mask.
    short mWidth = 0;   // unsigned
    short mHeight = 0;  // unsigned

    // This array contains raw memory of image data.
    // mImage must remain null if the glyph is empty or if width > kMaxGlyphWidth.
    Object mImage;

    Path mPath;
    boolean mPathIsRequested;

    // Mask::Format
    byte mMaskFormat;

    // 12 bits in total
    private short mActions;

    // Created by ScalerContext
    public Glyph(int id) {
        mID = id;
    }

    public int getGlyphID() {
        return getGlyphID(mID);
    }

    public int getPackedID() {
        return mID;
    }

    public float getSubX() {
        return getSubX(mID);
    }

    /**
     * The bearing X.
     */
    public int getLeft() {
        return mLeft;
    }

    /**
     * The bearing Y.
     */
    public int getTop() {
        return mTop;
    }

    public int getWidth() {
        return mWidth & 0xFFFF;
    }

    public int getHeight() {
        return mHeight & 0xFFFF;
    }

    /**
     * Returns the largest dimension ( max(width, height) ).
     */
    public int getMaxDimension() {
        return Math.max(getWidth(), getHeight());
    }

    /**
     * Returns the bounding box for this glyph.
     * <p>
     * This may an exact bounding box or just a bounds of all control points,
     * also known as pixel bounds.
     */
    public void getBounds(@NonNull Rect2i dst) {
        dst.set(mLeft, mTop, mLeft + getWidth(), mTop + getHeight());
    }

    /**
     * Returns the bounding box for this glyph, it holds rounded integer values.
     * <p>
     * This may an exact bounding box or just a bounds of all control points,
     * also known as pixel bounds.
     */
    public void getBounds(@NonNull Rect2f dst) {
        dst.set(mLeft, mTop, mLeft + getWidth(), mTop + getHeight());
    }

    public boolean isEmpty() {
        return mWidth == 0 || mHeight == 0;
    }

    // Mask::Format
    public byte getMaskFormat() {
        return mMaskFormat;
    }

    public boolean isColor() {
        return mMaskFormat == Mask.kARGB32_Format;
    }

    public boolean imageIsTooLarge() {
        return getWidth() >= kMaxGlyphWidth;
    }

    // Image
    // If we haven't already tried to associate an image with this glyph
    // (i.e. setImageHasBeenCalled() returns false), then use the
    // ScalerContext to set the image.
    public boolean setImage(ScalerContext scalerContext) {
        if (!setImageHasBeenCalled()) {
            int size = getImageSize();
            assert size > 0;
            // use corresponding array type to ensure the alignment
            if (mMaskFormat == Mask.kARGB32_Format) {
                assert MathUtil.isAlign4(size);
                mImage = new int[size >> 2];
            } else {
                assert mMaskFormat == Mask.kBW_Format ||
                        mMaskFormat == Mask.kA8_Format;
                mImage = new byte[size];
            }
            scalerContext.getImage(this);
            return true;
        }
        return false;
    }

    // Returns true if the image has been set.
    public boolean setImageHasBeenCalled() {
        return mImage != null || isEmpty() || imageIsTooLarge();
    }

    // Return a pointer to the path if the image exists, otherwise return null.
    public Object getImageBase() {
        assert setImageHasBeenCalled();
        return mImage;
    }

    // Return a pointer to the path if the image exists, otherwise return null.
    public long getImageAddress() {
        assert setImageHasBeenCalled();
        if (mImage instanceof byte[]) {
            return Unsafe.ARRAY_BYTE_BASE_OFFSET;
        } else if (mImage instanceof int[]) {
            return Unsafe.ARRAY_INT_BASE_OFFSET;
        } else {
            return 0;
        }
    }

    public int getRowBytes() {
        int width = getWidth();
        return switch (mMaskFormat) {
            case Mask.kBW_Format -> (width + 7) >> 3;
            case Mask.kA8_Format -> width;
            case Mask.kARGB32_Format -> width << 2;
            default -> {
                assert false;
                yield 0;
            }
        };
    }

    // Return the size of the image in bytes.
    public int getImageSize() {
        if (isEmpty() || imageIsTooLarge()) {
            return 0;
        }

        // this fits in 32 bits
        return getRowBytes() * getHeight();
    }

    // Path
    // If we haven't already tried to associate a path to this glyph
    // (i.e. setPathHasBeenCalled() returns false), then use the
    // ScalerContext or Path argument to try to do so.  N.B. this
    // may still result in no path being associated with this glyph,
    // e.g. if you pass a null Path or the typeface is bitmap-only.
    //
    // This setPath() call is sticky... once you call it, the glyph
    // stays in its state permanently, ignoring any future calls.
    //
    // Returns true if this is the first time you called setPath()
    // and there actually is a path; call getPath() to get it.
    public boolean setPath(ScalerContext scalerContext) {
        if (!setPathHasBeenCalled()) {
            scalerContext.getPath(this);
            assert setPathHasBeenCalled();
            return getPath() != null;
        }
        return false;
    }

    public boolean setPath(Path path) {
        if (!setPathHasBeenCalled()) {
            mPathIsRequested = true;
            if (path != null) {
                mPath = new Path(path);
                mPath.updateBoundsCache();
                //TODO getGenerationID()
            }
            return getPath() != null;
        }
        return false;
    }

    // Returns true if that path has been set.
    public boolean setPathHasBeenCalled() {
        return mPathIsRequested;
    }

    // Return a pointer to the path if it exists, otherwise return null. Only works if the
    // path was previously set. Return value is read only!!
    public Path getPath() {
        assert setPathHasBeenCalled();
        return mPath;
    }

    /**
     * Returns the action result for the given text rendering method.
     *
     * @param actionType e.g. {@link #kDirectMask}
     */
    public int actionFor(int actionType) {
        return (mActions >>> actionType) & kActionMask;
    }

    // called by Strike
    void setActionFor(int actionType,
                      Strike strike) {
        // We don't have to do any more if the glyph is marked as kDrop because it was isEmpty().
        if (actionFor(actionType) == kUnset_Action) {
            int action = kReject_Action;
            switch (actionType) {
                case kDirectMask -> {
                    // fits in atlas
                    if (getMaxDimension() <= MAX_ATLAS_DIMENSION) {
                        action = kAccept_Action;
                    }
                }
                case kTransformedMask -> {
                    // fits in atlas
                    if (getMaxDimension() <= MAX_BILERP_ATLAS_DIMENSION) {
                        action = kAccept_Action;
                    }
                }
                case kPath -> {
                    if (strike.prepareForPath(this)) {
                        action = kAccept_Action;
                    }
                }
            }
            // unset is 0, so just OR it
            mActions |= (short) (action << actionType);
        }
    }

    // called after getGlyphMetrics
    void initActions() {
        // drop all if empty, or unset all
        mActions = (short) (isEmpty()
                ? (kDrop_Action << kDirectMask) |
                (kDrop_Action << kTransformedMask) |
                (kDrop_Action << kPath) |
                (kDrop_Action << kDrawable) |
                (kDrop_Action << kSDF) |
                (kDrop_Action << kMSDF)
                : 0);
    }
}
