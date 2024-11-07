/*
 * Modern UI.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.text.style;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.drawable.ImageDrawable;

/**
 * Span that replaces the text it's attached to with a {@link Drawable} that can be aligned with
 * the bottom or with the baseline of the surrounding text. The drawable can be constructed from
 * varied sources:
 * <ul>
 * <li>{@link Image} - see {@link #ImageSpan(Image)} and
 * {@link #ImageSpan(Image, int)}
 * </li>
 * <li>{@link Drawable} - see {@link #ImageSpan(Drawable, int)}</li>
 * </ul>
 * The default value for the vertical alignment is {@link DynamicDrawableSpan#ALIGN_BOTTOM}
 * <p>
 * For example, an <code>ImagedSpan</code> can be used like this:
 * <pre>
 * SpannableString string = new SpannableString("Bottom: span.\nBaseline: span.");
 * // using the default alignment: ALIGN_BOTTOM
 * string.setSpan(new ImageSpan(this, R.mipmap.ic_launcher), 7, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
 * string.setSpan(new ImageSpan(this, R.mipmap.ic_launcher, DynamicDrawableSpan.ALIGN_BASELINE),
 * 22, 23, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
 * </pre>
 * <img src="https://developer.android.com/reference/android/images/text/style/imagespan.png" />
 * <figcaption>Text with <code>ImageSpan</code>s aligned bottom and baseline.</figcaption>
 */
public class ImageSpan extends DynamicDrawableSpan {

    @NonNull
    private final Drawable mDrawable;

    /**
     * Constructs an {@link ImageSpan} from an {@link Image} with the default
     * alignment {@link DynamicDrawableSpan#ALIGN_BOTTOM}
     *
     * @param image image to be rendered
     */
    @Deprecated
    @SuppressWarnings("DataFlowIssue")
    public ImageSpan(@NonNull Image image) {
        this(null, image, ALIGN_BOTTOM);
    }

    /**
     * Constructs an {@link ImageSpan} from an {@link Image} and a vertical
     * alignment.
     *
     * @param image             image to be rendered
     * @param verticalAlignment one of {@link DynamicDrawableSpan#ALIGN_BOTTOM} or
     *                          {@link DynamicDrawableSpan#ALIGN_BASELINE}
     */
    @Deprecated
    @SuppressWarnings("DataFlowIssue")
    public ImageSpan(@NonNull Image image, int verticalAlignment) {
        this(null, image, verticalAlignment);
    }

    /**
     * Constructs an {@link ImageSpan} from a {@link Context} and an {@link Image} with the default
     * alignment {@link DynamicDrawableSpan#ALIGN_BOTTOM}
     *
     * @param context context used to create a drawable from {@param image} based on the display
     *                metrics of the resources
     * @param image   image to be rendered
     */
    public ImageSpan(@NonNull Context context, @NonNull Image image) {
        this(context, image, ALIGN_BOTTOM);
    }

    /**
     * Constructs an {@link ImageSpan} from a {@link Context}, an {@link Image} and a vertical
     * alignment.
     *
     * @param context           context used to create a drawable from image based on
     *                          the display metrics of the resources
     * @param image             image to be rendered
     * @param verticalAlignment one of {@link DynamicDrawableSpan#ALIGN_BOTTOM} or
     *                          {@link DynamicDrawableSpan#ALIGN_BASELINE}
     */
    @SuppressWarnings({"ConstantValue", "deprecation"})
    public ImageSpan(@NonNull Context context, @NonNull Image image, int verticalAlignment) {
        super(verticalAlignment);
        mDrawable = context != null
                ? new ImageDrawable(context.getResources(), image)
                : new ImageDrawable(image);
        int width = mDrawable.getIntrinsicWidth();
        int height = mDrawable.getIntrinsicHeight();
        mDrawable.setBounds(0, 0, Math.max(width, 0), Math.max(height, 0));
    }

    /**
     * Constructs an {@link ImageSpan} from a drawable with the default
     * alignment {@link DynamicDrawableSpan#ALIGN_BOTTOM}.
     *
     * @param drawable drawable to be rendered
     */
    public ImageSpan(@NonNull Drawable drawable) {
        this(drawable, ALIGN_BOTTOM);
    }

    /**
     * Constructs an {@link ImageSpan} from a drawable and a vertical alignment.
     *
     * @param drawable          drawable to be rendered
     * @param verticalAlignment one of {@link DynamicDrawableSpan#ALIGN_BOTTOM} or
     *                          {@link DynamicDrawableSpan#ALIGN_BASELINE}
     */
    public ImageSpan(@NonNull Drawable drawable, int verticalAlignment) {
        super(verticalAlignment);
        mDrawable = drawable;
    }

    @NonNull
    @Override
    public Drawable getDrawable() {
        return mDrawable;
    }
}
