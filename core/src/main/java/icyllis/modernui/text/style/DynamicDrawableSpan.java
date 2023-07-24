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

package icyllis.modernui.text.style;

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.text.FontMetricsInt;
import icyllis.modernui.graphics.text.FontPaint;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.text.TextPaint;
import org.intellij.lang.annotations.MagicConstant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.ref.WeakReference;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Span that replaces the text it's attached to with a {@link Drawable} that can be aligned with
 * the bottom or with the baseline of the surrounding text.
 * <p>
 * For an implementation that constructs the drawable from various sources (<code>Bitmap</code>,
 * <code>Drawable</code>, resource id or <code>Uri</code>) use {@link ImageSpan}.
 * <p>
 * A simple implementation of <code>DynamicDrawableSpan</code> that uses drawables from resources
 * looks like this:
 * <pre>
 * class MyDynamicDrawableSpan extends DynamicDrawableSpan {
 *
 * private final Context mContext;
 * private final int mResourceId;
 *
 * public MyDynamicDrawableSpan(Context context, @DrawableRes int resourceId) {
 *     mContext = context;
 *     mResourceId = resourceId;
 * }
 *
 * {@literal @}Override
 * public Drawable getDrawable() {
 *      Drawable drawable = mContext.getDrawable(mResourceId);
 *      drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
 *      return drawable;
 * }
 * }</pre>
 * The class can be used like this:
 * <pre>
 * SpannableString string = new SpannableString("Text with a drawable span");
 * string.setSpan(new MyDynamicDrawableSpan(context, R.mipmap.ic_launcher), 12, 20, Spanned
 * .SPAN_EXCLUSIVE_EXCLUSIVE);</pre>
 * <img src="https://developer.android.com/reference/android/images/text/style/dynamicdrawablespan.png" />
 * <figcaption>Replacing text with a drawable.</figcaption>
 */
public abstract class DynamicDrawableSpan extends ReplacementSpan {

    /**
     * A constant indicating that the bottom of this span should be aligned
     * with the bottom of the surrounding text, i.e., at the same level as the
     * lowest descender in the text.
     */
    public static final int ALIGN_BOTTOM = 0;

    /**
     * A constant indicating that the bottom of this span should be aligned
     * with the baseline of the surrounding text.
     */
    public static final int ALIGN_BASELINE = 1;

    /**
     * A constant indicating that this span should be vertically centered between
     * the top and the lowest descender.
     */
    public static final int ALIGN_CENTER = 2;

    /**
     * Defines acceptable alignment types.
     */
    @Retention(SOURCE)
    @MagicConstant(intValues = {
            ALIGN_BOTTOM,
            ALIGN_BASELINE,
            ALIGN_CENTER
    })
    public @interface AlignmentType {
    }

    protected final int mVerticalAlignment;

    private WeakReference<Drawable> mDrawableRef;

    /**
     * Creates a {@link DynamicDrawableSpan}. The default vertical alignment is
     * {@link #ALIGN_BOTTOM}
     */
    public DynamicDrawableSpan() {
        mVerticalAlignment = ALIGN_BOTTOM;
    }

    /**
     * Creates a {@link DynamicDrawableSpan} based on a vertical alignment.\
     *
     * @param verticalAlignment one of {@link #ALIGN_BOTTOM}, {@link #ALIGN_BASELINE} or
     *                          {@link #ALIGN_CENTER}
     */
    protected DynamicDrawableSpan(@AlignmentType int verticalAlignment) {
        mVerticalAlignment = verticalAlignment;
    }

    /**
     * Returns the vertical alignment of this span, one of {@link #ALIGN_BOTTOM},
     * {@link #ALIGN_BASELINE} or {@link #ALIGN_CENTER}.
     */
    @AlignmentType
    public int getVerticalAlignment() {
        return mVerticalAlignment;
    }

    /**
     * Your subclass must implement this method to provide the bitmap
     * to be drawn.  The dimensions of the bitmap must be the same
     * from each call to the next.
     */
    public abstract Drawable getDrawable();

    @Override
    public int getSize(@Nonnull TextPaint paint, CharSequence text,
                       int start, int end, @Nullable FontMetricsInt fm) {
        Drawable d = getCachedDrawable();
        Rect rect = d.getBounds();

        if (fm != null) {
            fm.ascent = rect.bottom;
            fm.descent = 0;
        }

        return rect.right;
    }

    @Override
    public void draw(@Nonnull Canvas canvas, CharSequence text,
                     int start, int end, float x, int top, int y, int bottom,
                     @Nonnull TextPaint paint) {
        Drawable b = getCachedDrawable();
        canvas.save();

        int transY = bottom - b.getBounds().bottom;
        if (mVerticalAlignment == ALIGN_BASELINE) {
            transY -= paint.getFontMetricsInt().descent;
        } else if (mVerticalAlignment == ALIGN_CENTER) {
            transY = top + (bottom - top) / 2 - b.getBounds().height() / 2;
        }

        canvas.translate(x, transY);
        b.draw(canvas);
        canvas.restore();
    }

    private Drawable getCachedDrawable() {
        WeakReference<Drawable> wr = mDrawableRef;
        Drawable d = null;

        if (wr != null) {
            d = wr.get();
        }

        if (d == null) {
            d = getDrawable();
            mDrawableRef = new WeakReference<>(d);
        }

        return d;
    }
}
