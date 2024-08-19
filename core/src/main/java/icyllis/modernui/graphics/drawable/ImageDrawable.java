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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.*;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.LayoutDirection;
import icyllis.modernui.view.Gravity;

import java.io.IOException;
import java.io.InputStream;

/**
 * A Drawable that wraps an image and can be tiled, stretched, or aligned. You can create a
 * ImageDrawable from a file path, an input stream, or from a {@link Image} object.
 */
//TODO texture tint blending, current it's MODULATE
public class ImageDrawable extends Drawable {

    // lazily init
    private Rect mSrcRect;
    private final Rect mDstRect = new Rect();

    private ImageState mImageState;
    private BlendModeColorFilter mBlendModeFilter;

    private boolean mFullImage = true;
    private boolean mDstRectAndInsetsDirty = true;
    private boolean mMutated;

    /**
     * Create drawable from an image, setting initial target density based on
     * the display metrics of the resources.
     */
    public ImageDrawable(@Nullable Image image) {
        init(new ImageState(image));
    }

    /**
     * Create a drawable by opening a given file path and decoding the image.
     */
    public ImageDrawable(@NonNull String namespace, @NonNull String path) {
        Image image = Image.create(namespace, path);
        init(new ImageState(image));
    }

    /**
     * Create a drawable by decoding an image from the given input stream.
     * <p>
     * This method silently ignores any IO exception. This method <em>does not</em> closed
     * the given stream after read operation has completed. The stream will be at end if
     * read operation succeeds.
     * <p>
     * This method may be called from either render thread or UI thread.
     */
    public ImageDrawable(@NonNull InputStream stream) {
        Image image = null;
        try (var bitmap = BitmapFactory.decodeStream(stream)) {
            image = Image.createTextureFromBitmap(bitmap);
        } catch (IOException ignored) {
        }
        init(new ImageState(image));
    }

    /**
     * Returns the paint used to render this drawable.
     */
    @NonNull
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
    protected void onBoundsChange(@NonNull Rect bounds) {
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
    public void draw(@NonNull Canvas canvas) {
        final Image image = mImageState.mImage;
        if (image == null) {
            return;
        }

        final ImageState state = mImageState;
        final Paint paint = state.mPaint;

        final boolean clearColorFilter;
        if (mBlendModeFilter != null && paint.getColorFilter() == null) {
            paint.setColorFilter(mBlendModeFilter);
            clearColorFilter = true;
        } else {
            clearColorFilter = false;
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

        if (clearColorFilter) {
            paint.setColorFilter(null);
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
            mBlendModeFilter = updateBlendModeFilter(mBlendModeFilter, tint,
                    mImageState.mBlendMode);
            invalidateSelf();
        }
    }

    @Override
    public void setTintBlendMode(@NonNull BlendMode blendMode) {
        final ImageState state = mImageState;
        if (state.mBlendMode != blendMode) {
            state.mBlendMode = blendMode;
            mBlendModeFilter = updateBlendModeFilter(mBlendModeFilter, mImageState.mTint,
                    blendMode);
            invalidateSelf();
        }
    }

    /**
     * A mutable BitmapDrawable still shares its Bitmap with any other Drawable
     * that comes from the same resource.
     *
     * @return This drawable.
     */
    @NonNull
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
    protected boolean onStateChange(@NonNull int[] stateSet) {
        final ImageState state = mImageState;
        if (state.mTint != null && state.mBlendMode != null) {
            mBlendModeFilter = updateBlendModeFilter(mBlendModeFilter, state.mTint,
                    state.mBlendMode);
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
        if (mImageState.mImage == null) {
            return super.getIntrinsicWidth();
        }
        if (mFullImage) {
            return mImageState.mImage.getWidth();
        }
        return Math.min(mSrcRect.width(), mImageState.mImage.getWidth());
    }

    @Override
    public int getIntrinsicHeight() {
        if (mImageState.mImage == null) {
            return super.getIntrinsicHeight();
        }
        if (mFullImage) {
            return mImageState.mImage.getHeight();
        }
        return Math.min(mSrcRect.height(), mImageState.mImage.getHeight());
    }

    @Override
    public final ConstantState getConstantState() {
        return mImageState;
    }

    static final class ImageState extends ConstantState {

        final Paint mPaint;

        Image mImage;
        ColorStateList mTint = null;
        BlendMode mBlendMode = DEFAULT_BLEND_MODE;

        int mGravity = Gravity.FILL;

        boolean mAutoMirrored = false;

        ImageState(Image image) {
            mImage = image;
            mPaint = new Paint();
        }

        ImageState(@NonNull ImageState imageState) {
            mImage = imageState.mImage;
            mTint = imageState.mTint;
            mGravity = imageState.mGravity;
            mPaint = new Paint(imageState.mPaint);
            mAutoMirrored = imageState.mAutoMirrored;
        }

        @NonNull
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
        mBlendModeFilter = updateBlendModeFilter(mBlendModeFilter, mImageState.mTint,
                mImageState.mBlendMode);
    }
}
