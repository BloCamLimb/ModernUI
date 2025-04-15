/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.DisplayMetrics;
import icyllis.modernui.view.Gravity;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus;

import java.io.InputStream;

/**
 * A Drawable that wraps an image and can be drawn with rounded corners. You can create a
 * RoundedImageDrawable from a file path, an input stream, or from a {@link Image} object.
 *
 * @see ImageDrawable
 * @since 3.12
 */
public class RoundedImageDrawable extends Drawable {

    private final Paint mPaint = new Paint();

    private Image mImage;
    private ColorStateList mTint = null;
    private BlendMode mBlendMode = DEFAULT_BLEND_MODE;

    private int mGravity = Gravity.FILL;
    private int mTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
    private float mCornerRadius;

    private final Rect mSrcRect = new Rect();
    private final Rect mDstRect = new Rect();

    private BlendModeColorFilter mBlendModeFilter;

    private boolean mDstRectDirty = true;
    private boolean mIsCircular;

    /**
     * Create drawable from an image, setting initial target density based on
     * the display metrics of the resources.
     */
    public RoundedImageDrawable(Resources res, Image image) {
        mImage = image;
        mTargetDensity = resolveDensity(res, mTargetDensity);
        if (image != null) {
            mSrcRect.set(0, 0, image.getWidth(), image.getWidth());
        }
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
    public RoundedImageDrawable(Resources res, @NonNull InputStream stream) {
        Image image = null;
        try (var bitmap = BitmapFactory.decodeStream(stream)) {
            image = Image.createTextureFromBitmap(bitmap);
        } catch (Exception e) {
            ModernUI.LOGGER.warn(MarkerManager.getMarker("RoundedImageDrawable"),
                    "Cannot create RoundedImageDrawable from {}", stream, e);
        } finally {
            mImage = image;
            mTargetDensity = resolveDensity(res, mTargetDensity);
            if (image != null) {
                mSrcRect.set(0, 0, image.getWidth(), image.getWidth());
            }
        }
    }

    /**
     * Returns the paint used to render this drawable.
     */
    @NonNull
    public final Paint getPaint() {
        return mPaint;
    }

    /**
     * Returns the image used by this drawable to render. May be null.
     */
    @Nullable
    public final Image getImage() {
        return mImage;
    }

    /**
     * Switch to a new Image object. Calling this method will also reset
     * the subset to the full image, see {@link #setSrcRect(Rect)}.
     */
    public void setImage(@Nullable Image image) {
        if (mImage != image) {
            mImage = image;
            if (mIsCircular) {
                updateCircularSubset();
            } else {
                if (image != null) {
                    mSrcRect.set(0, 0, image.getWidth(), image.getWidth());
                }
            }
            mDstRectDirty = true;
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
            mDstRectDirty = true;
            invalidateSelf();
        }
    }

    /**
     * Get the gravity used to position/stretch the image within its bounds.
     *
     * @return the gravity applied to the image
     * @see Gravity
     */
    public int getGravity() {
        return mGravity;
    }

    /**
     * Set the gravity used to position/stretch the image within its bounds.
     *
     * @param gravity the gravity
     * @see Gravity
     */
    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            mGravity = gravity;
            mDstRectDirty = true;
            invalidateSelf();
        }
    }

    /**
     * Specifies the subset of the image to draw. Null for the full image.
     * <p>
     * Calling this method when there's no image has no effect. Next call
     * to {@link #setImage(Image)} will reset the subset to the full image.
     *
     * @param subset the subset of the image
     */
    public void setSrcRect(@Nullable Rect subset) {
        final Image image = mImage;
        if (image == null) {
            return;
        }
        if (subset == null) {
            mSrcRect.set(0, 0, image.getWidth(), image.getWidth());
            mDstRectDirty = true;
            invalidateSelf();
        } else if (!mSrcRect.equals(subset)) {
            mSrcRect.set(0, 0, image.getWidth(), image.getWidth());
            if (!mSrcRect.intersect(subset)) {
                mSrcRect.setEmpty();
            }
            mDstRectDirty = true;
            invalidateSelf();
        }
    }

    private int computeSrcWidth() {
        Image image = mImage;
        if (image == null) {
            return -1;
        }
        int width = Math.min(mSrcRect.width(), image.getWidth());
        return Image.scaleFromDensity(width, image.getDensity(), mTargetDensity);
    }

    private int computeSrcHeight() {
        Image image = mImage;
        if (image == null) {
            return -1;
        }
        int height = Math.min(mSrcRect.height(), image.getHeight());
        return Image.scaleFromDensity(height, image.getDensity(), mTargetDensity);
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
        mPaint.setAntiAlias(aa);
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
        return mPaint.isAntiAlias();
    }

    /**
     * Sets a hint that indicates if color error may be distributed to smooth color transition.
     * For example, drawing 16-bit per channel image onto an 8-bit per channel device.
     * The default value is false.
     */
    public void setDither(boolean dither) {
        mPaint.setDither(dither);
        invalidateSelf();
    }

    /**
     * Returns true if color error may be distributed to smooth color transition.
     * The default value is false.
     */
    public boolean isDither() {
        return mPaint.isDither();
    }

    /**
     * Set to true to have the drawable filter texture images with bilinear
     * sampling when they are scaled or rotated. The default is true.
     *
     * @param filter true to use bilinear sampling, false to use nearest neighbor sampling
     */
    public void setFilter(boolean filter) {
        if (mPaint.isFilter() != filter) {
            mPaint.setFilter(filter);
            mDstRectDirty = true; // rebuild shader
            invalidateSelf();
        }
    }

    /**
     * Returns the current filter. The default is true.
     */
    public boolean isFilter() {
        return mPaint.isFilter();
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        mDstRectDirty = true;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        final Image image = mImage;
        if (image == null) {
            return;
        }

        final Paint paint = mPaint;

        final boolean useShader;
        boolean rebuildShader;
        if (updateDstRect()) {
            if (!(mCornerRadius > 0.01f)) {
                paint.setShader(null);
                rebuildShader = false;
                useShader = false;
            } else {
                rebuildShader = true;
                useShader = true;
            }
        } else {
            rebuildShader = false;
            useShader = paint.hasShader();
        }

        final boolean clearColorFilter;
        if (mBlendModeFilter != null && paint.getColorFilter() == null) {
            paint.setColorFilter(mBlendModeFilter);
            clearColorFilter = true;
        } else {
            clearColorFilter = false;
        }

        if (!useShader) {
            canvas.drawImage(image, mSrcRect, mDstRect, paint);
        } else {
            if (rebuildShader) {
                paint.setShader(new ImageShader(image,
                        Shader.TileMode.CLAMP,
                        Shader.TileMode.CLAMP,
                        paint.getFilterMode(),
                        updateShaderMatrix(image)
                ));
            }
            canvas.drawRoundRect(mDstRect.left, mDstRect.top, mDstRect.right, mDstRect.bottom,
                    mCornerRadius, paint);
        }

        if (clearColorFilter) {
            paint.setColorFilter(null);
        }
    }

    @NonNull
    private Matrix updateShaderMatrix(@NonNull Image image) {
        Matrix matrix = new Matrix();

        // src to dst
        float srcLeft = mSrcRect.left;
        float srcTop = mSrcRect.top;
        float srcWidth = Math.min(image.getWidth(), mSrcRect.width());
        float srcHeight = Math.min(image.getHeight(), mSrcRect.height());
        float sx = mDstRect.width() / srcWidth;
        float sy = mDstRect.height() / srcHeight;
        float tx = mDstRect.left - srcLeft * sx;
        float ty = mDstRect.top - srcTop * sy;
        matrix.setScaleTranslate(sx, sy, tx, ty);

        return matrix;
    }

    private boolean updateDstRect() {
        if (mDstRectDirty) {
            if (mIsCircular) {
                final int minDimen = Math.min(computeSrcWidth(), computeSrcHeight());
                final int layoutDirection = getLayoutDirection();
                Gravity.apply(mGravity, minDimen, minDimen,
                        getBounds(), mDstRect, layoutDirection);

                // inset the drawing rectangle to the largest contained square,
                // so that a circle will be drawn
                final int minDrawDimen = Math.min(mDstRect.width(), mDstRect.height());
                final int insetX = Math.max(0, (mDstRect.width() - minDrawDimen) / 2);
                final int insetY = Math.max(0, (mDstRect.height() - minDrawDimen) / 2);
                mDstRect.inset(insetX, insetY);
                mCornerRadius = 0.5f * minDrawDimen;
            } else {
                final int layoutDirection = getLayoutDirection();
                Gravity.apply(mGravity, computeSrcWidth(), computeSrcHeight(),
                        getBounds(), mDstRect, layoutDirection);
            }

            mDstRectDirty = false;
            return true;
        }
        return false;
    }

    @Override
    public void getOutline(@NonNull Outline outline) {
        updateDstRect();
        outline.setRoundRect(mDstRect, getCornerRadius());
        outline.setAlpha(getAlpha() / 255.0f);
    }

    @Override
    public void setAlpha(int alpha) {
        final int oldAlpha = mPaint.getAlpha();
        if (alpha != oldAlpha) {
            mPaint.setAlpha(alpha);
            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
        return mPaint.getAlpha();
    }

    @Override
    public void setTintList(@Nullable ColorStateList tint) {
        if (mTint != tint) {
            mTint = tint;
            mBlendModeFilter = updateBlendModeFilter(mBlendModeFilter, tint,
                    mBlendMode);
            invalidateSelf();
        }
    }

    @Override
    public void setTintBlendMode(@NonNull BlendMode blendMode) {
        if (mBlendMode != blendMode) {
            mBlendMode = blendMode;
            mBlendModeFilter = updateBlendModeFilter(mBlendModeFilter, mTint,
                    blendMode);
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Nullable
    @Override
    public ColorFilter getColorFilter() {
        return mPaint.getColorFilter();
    }

    @Override
    protected boolean onStateChange(@NonNull int[] stateSet) {
        if (mTint != null && mBlendMode != null) {
            mBlendModeFilter = updateBlendModeFilter(mBlendModeFilter, mTint,
                    mBlendMode);
            return true;
        }
        return false;
    }

    @Override
    public boolean isStateful() {
        return (mTint != null && mTint.isStateful())
                || super.isStateful();
    }

    @Override
    public boolean hasFocusStateSpecified() {
        return mTint != null && mTint.hasFocusStateSpecified();
    }

    private void updateCircularSubset() {
        final Image image = mImage;
        if (image == null) {
            return;
        }
        final int minDrawDimen = Math.min(mSrcRect.width(), mSrcRect.height());
        final int insetX = Math.max(0, (mSrcRect.width() - minDrawDimen) / 2);
        final int insetY = Math.max(0, (mSrcRect.height() - minDrawDimen) / 2);
        mSrcRect.inset(insetX, insetY);
    }

    /**
     * Sets the image shape to circular.
     * <p>This overwrites any calls made to {@link #setCornerRadius(float)} so far.</p>
     * <p>If true, this overwrites any calls made to {@link #setSrcRect(Rect)} so far.</p>
     */
    public void setCircular(boolean circular) {
        mIsCircular = circular;
        if (circular) {
            updateCircularSubset();
            mDstRectDirty = true;
            invalidateSelf();
        } else {
            setCornerRadius(0);
        }
    }

    /**
     * @return <code>true</code> if the image is circular, else <code>false</code>.
     */
    public boolean isCircular() {
        return mIsCircular;
    }

    /**
     * Sets the corner radius to be applied when drawing the bitmap.
     */
    public void setCornerRadius(float cornerRadius) {
        if (mCornerRadius == cornerRadius) return;

        mIsCircular = false;
        mDstRectDirty = true;

        mCornerRadius = cornerRadius;
        invalidateSelf();
    }

    /**
     * @return The corner radius applied when drawing the bitmap.
     */
    public float getCornerRadius() {
        return mCornerRadius;
    }

    @Override
    public int getIntrinsicWidth() {
        return computeSrcWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return computeSrcHeight();
    }

    //TODO add constant state
}
