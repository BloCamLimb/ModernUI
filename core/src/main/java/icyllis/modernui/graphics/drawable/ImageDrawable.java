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

package icyllis.modernui.graphics.drawable;

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.opengl.TextureManager;
import icyllis.modernui.math.Rect;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.LayoutDirection;
import icyllis.modernui.view.Gravity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;

/**
 * A Drawable that wraps an image and can be tiled, stretched, or aligned. You can create a
 * ImageDrawable from a file path, an input stream, or from a {@link Image} object.
 */
public class ImageDrawable extends Drawable {

    // lazily init
    private Rect mSrcRect;
    private final Rect mDstRect = new Rect();

    private ImageState mImageState;
    private int mBlendColor;

    private boolean mFullImage = true;
    private boolean mDstRectAndInsetsDirty = true;
    private boolean mMutated;

    /**
     * Create drawable from a image, setting initial target density based on
     * the display metrics of the resources.
     */
    public ImageDrawable(@Nullable Image image) {
        init(new ImageState(image));
    }

    /**
     * Create a drawable by opening a given file path and decoding the image.
     */
    public ImageDrawable(@Nonnull String res, @Nonnull String path) {
        Image image = Image.create(res, path);
        init(new ImageState(image));
    }

    /**
     * Create a drawable by decoding an image from the given input stream.
     */
    public ImageDrawable(@Nonnull InputStream is) {
        Image image = new Image(TextureManager.getInstance().create(is, true));
        init(new ImageState(image));
    }

    /**
     * Returns the paint used to render this drawable.
     */
    @Nonnull
    public final Paint getPaint() {
        return mImageState.mPaint;
    }

    /**
     * Returns the image used by this drawable to render. May be null.
     */
    public final Image getImage() {
        return mImageState.mImage;
    }

    /**
     * Switch to a new Image object.
     */
    public void setImage(@Nullable Image image) {
        if (mImageState.mImage != image) {
            mImageState.mImage = image;
            invalidateSelf();
        }
    }

    /**
     * Get the gravity used to position/stretch the image within its bounds.
     *
     * @return the gravity applied to the image
     */
    public int getGravity() {
        return mImageState.mGravity;
    }

    /**
     * Set the gravity used to position/stretch the image within its bounds.
     *
     * @param gravity the gravity
     */
    public void setGravity(int gravity) {
        if (mImageState.mGravity != gravity) {
            mImageState.mGravity = gravity;
            mDstRectAndInsetsDirty = true;
            invalidateSelf();
        }
    }

    /**
     * Specifies the subset of the image to draw. To draw the full image,
     * call {@link #setSrcRect(Rect)} with null.
     */
    public void setSrcRect(int left, int top, int right, int bottom) {
        if (mSrcRect == null) {
            mSrcRect = new Rect(left, top, right, bottom);
            invalidateSelf();
        } else {
            Rect oldBounds = mSrcRect;
            if (oldBounds.left != left || oldBounds.top != top ||
                    oldBounds.right != right || oldBounds.bottom != bottom) {
                mSrcRect.set(left, top, right, bottom);
                invalidateSelf();
            }
        }
        mFullImage = false;
    }

    /**
     * Specifies the subset of the image to draw. Null for the full image.
     *
     * @param srcRect the subset of the image
     */
    public void setSrcRect(@Nullable Rect srcRect) {
        if (srcRect == null) {
            mFullImage = true;
        } else {
            if (mSrcRect == null) {
                mSrcRect = srcRect.copy();
            } else {
                mSrcRect.set(srcRect);
            }
            mFullImage = false;
        }
    }

    /**
     * Enables or disables the mipmap hint for this drawable's image.
     * <p>
     * If the image is null calling this method has no effect.
     *
     * @param mipmap True if the image should use mipmaps, false otherwise.
     * @see #hasMipmap()
     */
    public void setMipmap(boolean mipmap) {
    }

    /**
     * Indicates whether the mipmap hint is enabled on this drawable's image.
     *
     * @return True if the mipmap hint is set, false otherwise. If the image
     * is null, this method always returns false.
     * @see #setMipmap(boolean)
     */
    public boolean hasMipmap() {
        return true;
    }

    @Override
    public void setAutoMirrored(boolean mirrored) {
        if (mImageState.mAutoMirrored != mirrored) {
            mImageState.mAutoMirrored = mirrored;
            invalidateSelf();
        }
    }

    @Override
    public final boolean isAutoMirrored() {
        return mImageState.mAutoMirrored;
    }

    private boolean needMirroring() {
        return isAutoMirrored() && getLayoutDirection() == LayoutDirection.RTL;
    }

    @Override
    protected void onBoundsChange(@Nonnull Rect bounds) {
        mDstRectAndInsetsDirty = true;
    }

    private void updateDstRectAndInsetsIfDirty() {
        if (mDstRectAndInsetsDirty) {
            final Image image = mImageState.mImage;
            if (image != null) {
                final int layoutDirection = getLayoutDirection();
                Gravity.apply(mImageState.mGravity, image.getWidth(), image.getHeight(),
                        getBounds(), mDstRect, layoutDirection);
            }
            mDstRectAndInsetsDirty = false;
        }
    }

    @Override
    public void draw(@Nonnull Canvas canvas) {
        final Image image = mImageState.mImage;
        if (image == null) {
            return;
        }

        final ImageState state = mImageState;
        final Paint paint = state.mPaint;

        final int restoreAlpha;
        if (mBlendColor >>> 24 != 0xFF) {
            restoreAlpha = paint.getAlpha();
            paint.setColor(mBlendColor);
            paint.setAlpha(restoreAlpha * (mBlendColor >>> 24) >>> 8);
        } else {
            restoreAlpha = -1;
        }

        updateDstRectAndInsetsIfDirty();

        final boolean needMirroring = needMirroring();
        if (needMirroring) {
            canvas.save();
            // Flip horizontal
            canvas.translate(mDstRect.width(), 0);
            canvas.scale(-1.0f, 1.0f);
        }

        canvas.drawImage(image, mFullImage ? null : mSrcRect, mDstRect, paint);

        if (needMirroring) {
            canvas.restore();
        }

        if (restoreAlpha >= 0) {
            paint.setAlpha(restoreAlpha);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        final int oldAlpha = mImageState.mPaint.getAlpha();
        if (alpha != oldAlpha) {
            mImageState.mPaint.setAlpha(alpha);
            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
        return mImageState.mPaint.getAlpha();
    }

    @Override
    public void setTintList(@Nullable ColorStateList tint) {
        final ImageState state = mImageState;
        if (state.mTint != tint) {
            state.mTint = tint;
            if (tint == null) {
                mBlendColor = ~0;
            } else {
                mBlendColor = tint.getColorForState(getState(), ~0);
            }
            invalidateSelf();
        }
    }

    /**
     * A mutable BitmapDrawable still shares its Bitmap with any other Drawable
     * that comes from the same resource.
     *
     * @return This drawable.
     */
    @Nonnull
    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mImageState = new ImageState(mImageState);
            mMutated = true;
        }
        return this;
    }

    @Override
    public void clearMutated() {
        super.clearMutated();
        mMutated = false;
    }

    @Override
    protected boolean onStateChange(@Nonnull int[] stateSet) {
        final ImageState state = mImageState;
        if (state.mTint != null) {
            mBlendColor = state.mTint.getColorForState(stateSet, ~0);
            return true;
        }
        return false;
    }

    @Override
    public boolean isStateful() {
        return (mImageState.mTint != null && mImageState.mTint.isStateful())
                || super.isStateful();
    }

    @Override
    public boolean hasFocusStateSpecified() {
        return mImageState.mTint != null && mImageState.mTint.hasFocusStateSpecified();
    }

    @Override
    public int getIntrinsicWidth() {
        return mImageState.mImage == null ? super.getIntrinsicWidth() : mImageState.mImage.getWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return mImageState.mImage == null ? super.getIntrinsicHeight() : mImageState.mImage.getHeight();
    }

    @Override
    public final ConstantState getConstantState() {
        return mImageState;
    }

    static final class ImageState extends ConstantState {

        final Paint mPaint;

        Image mImage;
        ColorStateList mTint = null;

        int mGravity = Gravity.FILL;

        boolean mAutoMirrored = false;

        ImageState(Image image) {
            mImage = image;
            mPaint = new Paint();
        }

        ImageState(@Nonnull ImageState imageState) {
            mImage = imageState.mImage;
            mTint = imageState.mTint;
            mGravity = imageState.mGravity;
            mPaint = new Paint(imageState.mPaint);
            mAutoMirrored = imageState.mAutoMirrored;
        }

        @Nonnull
        @Override
        public Drawable newDrawable() {
            return new ImageDrawable(this);
        }
    }

    private ImageDrawable(ImageState state) {
        init(state);
    }

    /**
     * The one helper to rule them all. This is called by all public & private
     * constructors to set the state and initialize local properties.
     */
    private void init(ImageState state) {
        mImageState = state;
        updateLocalState();
    }

    /**
     * Initializes local dynamic properties from state. This should be called
     * after significant state changes, e.g. from the One True Constructor and
     * after inflating or applying a theme.
     */
    private void updateLocalState() {
        if (mImageState.mTint == null) {
            mBlendColor = ~0;
        } else {
            mBlendColor = mImageState.mTint.getColorForState(getState(), ~0);
        }
    }
}
