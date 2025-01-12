/*
 * Modern UI.
 * Copyright (C) 2019-2025 BloCamLimb. All rights reserved.
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

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.*;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.util.*;
import icyllis.modernui.view.Gravity;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus;

import java.io.InputStream;

/**
 * A Drawable that wraps an image and can be tiled, stretched, or aligned. You can create a
 * ImageDrawable from a file path, an input stream, or from a {@link Image} object.
 *
 * @see RoundedImageDrawable
 */
public class ImageDrawable extends Drawable {

    // lazily init
    private Rect mSrcRect;
    private final Rect mDstRect = new Rect();

    private ImageState mImageState;
    private BlendModeColorFilter mBlendModeFilter;

    private int mTargetDensity = DisplayMetrics.DENSITY_DEFAULT;

    private boolean mFullImage = true;
    private boolean mDstRectAndInsetsDirty = true;
    private boolean mMutated;

    /**
     * Create drawable from an image, setting target density to
     * {@link DisplayMetrics#DENSITY_DEFAULT}.
     *
     * @deprecated use {@link #ImageDrawable(Resources, Image)} instead
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public ImageDrawable(Image image) {
        init(new ImageState(image), null);
    }

    /**
     * Create drawable from an image, setting initial target density based on
     * the display metrics of the resources.
     */
    public ImageDrawable(Resources res, Image image) {
        init(new ImageState(image), res);
    }

    /**
     * Create a drawable by opening a given file path and decoding the image.
     * <p>
     * This method may only be called from UI thread.
     */
    public ImageDrawable(@NonNull String namespace, @NonNull String path) {
        Image image = Image.create(namespace, path);
        init(new ImageState(image), null);
    }

    /**
     * Create a drawable by decoding an image from the given input stream.
     *
     * @deprecated use {@link #ImageDrawable(Resources, InputStream)} instead
     */
    @Deprecated
    public ImageDrawable(@NonNull InputStream stream) {
        this(null, stream);
    }

    /**
     * Create a drawable by decoding an image from the given input stream.
     * <p>
     * This method silently ignores any IO exception. This method <em>does not</em> closed
     * the given stream after read operation has completed. The stream will be at end if
     * read operation succeeds.
     * <p>
     * This method may only be called from UI thread.
     */
    public ImageDrawable(Resources res, @NonNull InputStream stream) {
        Image image = null;
        try (var bitmap = BitmapFactory.decodeStream(stream)) {
            image = Image.createTextureFromBitmap(bitmap);
        } catch (Exception e) {
            ModernUI.LOGGER.warn(MarkerManager.getMarker("ImageDrawable"),
                    "Cannot create ImageDrawable from {}", stream, e);
        } finally {
            init(new ImageState(image), res);
        }
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
    @Nullable
    public final Image getImage() {
        return mImageState.mImage;
    }

    /**
     * Switch to a new Image object. Calling this method will also reset
     * the subset to the full image, see {@link #setSubset(Rect)}.
     */
    public void setImage(@Nullable Image image) {
        if (mImageState.mImage != image) {
            mImageState.mImage = image;
            // Added by Modern UI
            if (mSrcRect != null && image != null) {
                mSrcRect.set(0, 0, image.getWidth(), image.getWidth());
            }
            mFullImage = true;
            // Added by Modern UI
            mDstRectAndInsetsDirty = true;
            invalidateSelf();
        }
    }

    /**
     * Set the density at which this drawable will be rendered.
     *
     * @param density The density scale for this drawable.
     * @see Image#setDensity(int)
     * @see Image#getDensity()
     */
    public void setTargetDensity(int density) {
        if (density == 0) {
            density = DisplayMetrics.DENSITY_DEFAULT;
        }
        if (mTargetDensity != density) {
            mTargetDensity = density;
            // Added by Modern UI
            mDstRectAndInsetsDirty = true;
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
     * call {@link #setSubset(Rect)} with null.
     * <p>
     * Calling this method when there's no image has no effect. Next call
     * to {@link #setImage(Image)} will reset the subset to the full image.
     */
    @Deprecated
    public void setSrcRect(int left, int top, int right, int bottom) {
        final Image image = mImageState.mImage;
        if (image == null) {
            return;
        }
        setSubset(new Rect(left, top, right, bottom));
    }

    /**
     * Specifies the subset of the image to draw. Null for the full image.
     * <p>
     * Calling this method when there's no image has no effect. Next call
     * to {@link #setImage(Image)} will reset the subset to the full image.
     *
     * @param srcRect the subset of the image
     */
    @Deprecated
    public void setSrcRect(@Nullable Rect srcRect) {
        setSubset(srcRect);
    }

    /**
     * Specifies the subset of the image to draw. Null for the full image.
     * <p>
     * Calling this method when there's no image has no effect. Next call
     * to {@link #setImage(Image)} will reset the subset to the full image.
     *
     * @param subset the subset of the image
     */
    public void setSubset(@Nullable Rect subset) {
        final Image image = mImageState.mImage;
        if (image == null) {
            return;
        }
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        if (subset == null || subset.contains(0, 0, imageWidth, imageHeight)) {
            if (!mFullImage) {
                invalidateSelf();
            }
            mFullImage = true;
        } else {
            if (mSrcRect == null) {
                mSrcRect = new Rect(0, 0, imageWidth, imageHeight);
                if (!mSrcRect.intersect(subset)) {
                    mSrcRect.setEmpty();
                }
                invalidateSelf();
            } else if (!mSrcRect.equals(subset)) {
                mSrcRect.set(0, 0, imageWidth, imageHeight);
                if (!mSrcRect.intersect(subset)) {
                    mSrcRect.setEmpty();
                }
                invalidateSelf();
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
    @ApiStatus.Experimental
    public void setMipmap(boolean mipmap) {
    }

    /**
     * Indicates whether the mipmap hint is enabled on this drawable's image.
     *
     * @return True if the mipmap hint is set, false otherwise. If the image
     * is null, this method always returns false.
     * @see #setMipmap(boolean)
     */
    @ApiStatus.Experimental
    public boolean hasMipmap() {
        return true;
    }

    /**
     * Enables or disables antialiasing for this drawable. Antialiasing affects
     * the edges of the image only so it applies only when the drawable is rotated.
     * The default is true.
     *
     * @param aa True if the image should be anti-aliased, false otherwise.
     * @see #isAntiAlias()
     */
    public void setAntiAlias(boolean aa) {
        //TODO set to false by default once Arc3D is updated with a faster pipeline
        mImageState.mPaint.setAntiAlias(aa);
        invalidateSelf();
    }

    /**
     * Indicates whether antialiasing is enabled for this drawable.
     * The default is true.
     *
     * @return True if antialiasing is enabled, false otherwise.
     * @see #setAntiAlias(boolean)
     */
    public boolean isAntiAlias() {
        return mImageState.mPaint.isAntiAlias();
    }

    /**
     * Sets a hint that indicates if color error may be distributed to smooth color transition.
     * For example, drawing 16-bit per channel image onto an 8-bit per channel device.
     * The default value is false.
     */
    public void setDither(boolean dither) {
        mImageState.mPaint.setDither(dither);
        invalidateSelf();
    }

    /**
     * Returns true if color error may be distributed to smooth color transition.
     * The default value is false.
     */
    public boolean isDither() {
        return mImageState.mPaint.isDither();
    }

    /**
     * Set to true to have the drawable filter texture images with bilinear
     * sampling when they are scaled or rotated. The default is true.
     *
     * @param filter true to use bilinear sampling, false to use nearest neighbor sampling
     */
    public void setFilter(boolean filter) {
        if (mImageState.mPaint.isFilter() != filter) {
            mImageState.mPaint.setFilter(filter);
            mImageState.mRebuildShader = true;
            invalidateSelf();
        }
    }

    /**
     * Returns the current filter. The default is true.
     */
    public boolean isFilter() {
        return mImageState.mPaint.isFilter();
    }

    /**
     * Indicates the repeat behavior of this drawable on the X axis.
     *
     * @return {@link Shader.TileMode#CLAMP} if the image does not repeat,
     * {@link Shader.TileMode#REPEAT} or
     * {@link Shader.TileMode#MIRROR} otherwise.
     */
    @Nullable
    public Shader.TileMode getTileModeX() {
        return mImageState.mTileModeX;
    }

    /**
     * Indicates the repeat behavior of this drawable on the Y axis.
     *
     * @return {@link Shader.TileMode#CLAMP} if the image does not repeat,
     * {@link Shader.TileMode#REPEAT} or
     * {@link Shader.TileMode#MIRROR} otherwise.
     */
    @Nullable
    public Shader.TileMode getTileModeY() {
        return mImageState.mTileModeY;
    }

    /**
     * Sets the repeat behavior of this drawable on the X axis. By default, the drawable
     * does not repeat its image. Using {@link Shader.TileMode#REPEAT} or
     * {@link Shader.TileMode#MIRROR} the image can be repeated (or tiled)
     * if the image is smaller than this drawable.
     *
     * @param mode The repeat mode for this drawable.
     * @see #setTileModeY(Shader.TileMode)
     * @see #setTileModeXY(Shader.TileMode, Shader.TileMode)
     */
    public void setTileModeX(@Nullable Shader.TileMode mode) {
        setTileModeXY(mode, mImageState.mTileModeY);
    }

    /**
     * Sets the repeat behavior of this drawable on the Y axis. By default, the drawable
     * does not repeat its image. Using {@link Shader.TileMode#REPEAT} or
     * {@link Shader.TileMode#MIRROR} the image can be repeated (or tiled)
     * if the image is smaller than this drawable.
     *
     * @param mode The repeat mode for this drawable.
     * @see #setTileModeX(Shader.TileMode)
     * @see #setTileModeXY(Shader.TileMode, Shader.TileMode)
     */
    public final void setTileModeY(@Nullable Shader.TileMode mode) {
        setTileModeXY(mImageState.mTileModeX, mode);
    }

    /**
     * Sets the repeat behavior of this drawable on both axis. By default, the drawable
     * does not repeat its image. Using {@link Shader.TileMode#REPEAT} or
     * {@link Shader.TileMode#MIRROR} the image can be repeated (or tiled)
     * if the image is smaller than this drawable.
     *
     * @param tileModeX The X tile mode for this drawable.
     * @param tileModeY The Y tile mode for this drawable.
     * @see #setTileModeX(Shader.TileMode)
     * @see #setTileModeY(Shader.TileMode)
     */
    public void setTileModeXY(@Nullable Shader.TileMode tileModeX,
                              @Nullable Shader.TileMode tileModeY) {
        final ImageState state = mImageState;
        if (state.mTileModeX != tileModeX || state.mTileModeY != tileModeY) {
            state.mTileModeX = tileModeX;
            state.mTileModeY = tileModeY;
            state.mRebuildShader = true;
            mDstRectAndInsetsDirty = true;
            invalidateSelf();
        }
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
        mImageState.mRebuildShader = true;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        final Image image = mImageState.mImage;
        if (image == null) {
            return;
        }

        final ImageState state = mImageState;
        final Paint paint = state.mPaint;

        final boolean useShader;
        boolean rebuildShader;
        final Shader.TileMode tileModeX = state.mTileModeX;
        final Shader.TileMode tileModeY = state.mTileModeY;
        if (state.mRebuildShader) {
            if (tileModeX == null && tileModeY == null) {
                paint.setShader(null);
                rebuildShader = false;
                useShader = false;
            } else {
                rebuildShader = true;
                useShader = true;
            }
            state.mRebuildShader = false;
        } else {
            rebuildShader = false;
            useShader = paint.hasShader();
        }

        final int restoreAlpha;
        if (state.mBaseAlpha != 1.0f) {
            final Paint p = getPaint();
            restoreAlpha = p.getAlpha();
            p.setAlpha((int) (restoreAlpha * state.mBaseAlpha + 0.5f));
        } else {
            restoreAlpha = -1;
        }

        final boolean clearColorFilter;
        if (mBlendModeFilter != null && paint.getColorFilter() == null) {
            paint.setColorFilter(mBlendModeFilter);
            clearColorFilter = true;
        } else {
            clearColorFilter = false;
        }

        updateDstRectAndInsetsIfDirty();

        final boolean needMirroring = needMirroring();
        if (!useShader) {
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
        } else {
            if (rebuildShader) {
                paint.setShader(new ImageShader(image,
                        tileModeX == null ? Shader.TileMode.CLAMP : tileModeX,
                        tileModeY == null ? Shader.TileMode.CLAMP : tileModeY,
                        paint.getFilterMode(),
                        updateShaderMatrix(image, needMirroring)
                ));
            }
            canvas.drawRect(mDstRect, paint);
        }

        if (clearColorFilter) {
            paint.setColorFilter(null);
        }

        if (restoreAlpha >= 0) {
            paint.setAlpha(restoreAlpha);
        }
    }

    @Nullable
    private Matrix updateShaderMatrix(@NonNull Image image, boolean needMirroring) {
        final int sourceDensity = image.getDensity();
        final int targetDensity = mTargetDensity;
        final boolean needScaling = sourceDensity != 0 && sourceDensity != targetDensity;
        if (needScaling || needMirroring) {
            Matrix matrix = new Matrix();

            if (needMirroring) {
                // fixed by Modern UI
                matrix.setScaleTranslate(-1.0f, 1.0f, mDstRect.width(), 0);
            }

            if (needScaling) {
                final float densityScale = targetDensity / (float) sourceDensity;
                matrix.postScale(densityScale, densityScale);
            }

            return matrix;
        } else {
            return null;
        }
    }

    private void updateDstRectAndInsetsIfDirty() {
        if (mDstRectAndInsetsDirty) {
            if (mImageState.mTileModeX == null && mImageState.mTileModeY == null) {
                final int layoutDirection = getLayoutDirection();
                Gravity.apply(mImageState.mGravity, getIntrinsicWidth(), getIntrinsicHeight(),
                        getBounds(), mDstRect, layoutDirection);
            } else {
                copyBounds(mDstRect);
            }
            mDstRectAndInsetsDirty = false;
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

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mImageState.mPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Nullable
    @Override
    public ColorFilter getColorFilter() {
        return mImageState.mPaint.getColorFilter();
    }

    /**
     * A mutable ImageDrawable still shares its Image with any other Drawable
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
        Image image = mImageState.mImage;
        if (image == null) {
            return super.getIntrinsicWidth();
        }
        int width;
        if (mFullImage) {
            width = image.getWidth();
        } else {
            width = Math.min(mSrcRect.width(), image.getWidth());
        }
        return Image.scaleFromDensity(width, image.getDensity(), mTargetDensity);
    }

    @Override
    public int getIntrinsicHeight() {
        Image image = mImageState.mImage;
        if (image == null) {
            return super.getIntrinsicHeight();
        }
        int height;
        if (mFullImage) {
            height = image.getHeight();
        } else {
            height = Math.min(mSrcRect.height(), image.getHeight());
        }
        return Image.scaleFromDensity(height, image.getDensity(), mTargetDensity);
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
        float mBaseAlpha = 1.0f;
        Shader.TileMode mTileModeX = null;
        Shader.TileMode mTileModeY = null;

        int mTargetDensity = DisplayMetrics.DENSITY_DEFAULT;

        boolean mAutoMirrored = false;

        boolean mRebuildShader;

        ImageState(Image image) {
            mImage = image;
            mPaint = new Paint();
        }

        @SuppressWarnings("IncompleteCopyConstructor")
        ImageState(@NonNull ImageState imageState) {
            mImage = imageState.mImage;
            mTint = imageState.mTint;
            mBlendMode = imageState.mBlendMode;
            mGravity = imageState.mGravity;
            mTileModeX = imageState.mTileModeX;
            mTileModeY = imageState.mTileModeY;
            mTargetDensity = imageState.mTargetDensity;
            mBaseAlpha = imageState.mBaseAlpha;
            mPaint = new Paint(imageState.mPaint);
            mRebuildShader = imageState.mRebuildShader;
            mAutoMirrored = imageState.mAutoMirrored;
        }

        @NonNull
        @Override
        public Drawable newDrawable() {
            return new ImageDrawable(this, null);
        }

        @NonNull
        @Override
        public Drawable newDrawable(Resources res) {
            return new ImageDrawable(this, res);
        }
    }

    private ImageDrawable(@NonNull ImageState state, Resources res) {
        init(state, res);
    }

    /**
     * The one helper to rule them all. This is called by all public & private
     * constructors to set the state and initialize local properties.
     */
    private void init(@NonNull ImageState state, Resources res) {
        mImageState = state;
        updateLocalState(res);

        if (res != null) {
            mImageState.mTargetDensity = mTargetDensity;
        }
    }

    /**
     * Initializes local dynamic properties from state. This should be called
     * after significant state changes, e.g. from the One True Constructor and
     * after inflating or applying a theme.
     */
    private void updateLocalState(Resources res) {
        mTargetDensity = resolveDensity(res, mImageState.mTargetDensity);
        mBlendModeFilter = updateBlendModeFilter(mBlendModeFilter, mImageState.mTint,
                mImageState.mBlendMode);
    }
}
