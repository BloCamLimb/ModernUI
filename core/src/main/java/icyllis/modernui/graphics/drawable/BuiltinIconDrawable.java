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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Matrix;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.resources.TypedValue;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.LayoutDirection;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;

/*
  This file incorporates <https://github.com/google/material-design-icons>
  which is licensed under Apache License Version 2.0.
 */

/**
 * An efficient way to draw simple scalable icons, without triggering heavy
 * path rendering. These icons are designed to be 24x24dp with perfect
 * pixel-grid alignment, colors may be changed via
 * {@link #setTintList(ColorStateList)}, icons may be auto mirrored in
 * RTL (right-to-left) layout direction via {@link #setAutoMirrored(boolean)}.
 *
 * @since 3.12
 */
@ApiStatus.Experimental
public class BuiltinIconDrawable extends Drawable {

    public static final int KEYBOARD_ARROW_RIGHT = 0;
    public static final int CHEVRON_RIGHT = KEYBOARD_ARROW_RIGHT;
    public static final int KEYBOARD_ARROW_LEFT = 1;
    public static final int CHEVRON_LEFT = KEYBOARD_ARROW_LEFT;
    public static final int KEYBOARD_ARROW_DOWN = 2;
    public static final int KEYBOARD_ARROW_UP = 3;
    public static final int CHECK = 4;
    public static final int CHECK_SMALL = 5;
    public static final int CHECK_INDETERMINATE_SMALL = 6;
    public static final int RADIO_SMALL = 7; // Modern UI added

    public static final float SIZE = 24;
    private final int mSize;
    private final int mIconType;

    private ColorStateList mTint;
    private int mColor = ~0;
    private int mAlpha = 255;

    private boolean mAutoMirrored = false;

    private static final float VIEWPORT_SIZE = 960;

    private final Matrix mTmpMatrix = new Matrix();
    private final float[] mTmpClip = new float[8];

    public BuiltinIconDrawable(@NonNull Resources res,
                               @MagicConstant(valuesFromClass = BuiltinIconDrawable.class) int iconType) {
        this(res, iconType, SIZE);
    }

    public BuiltinIconDrawable(@NonNull Resources res,
                               @MagicConstant(valuesFromClass = BuiltinIconDrawable.class) int iconType,
                               float sizeInDp) {
        mSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DP,
                sizeInDp, res.getDisplayMetrics());
        mIconType = iconType;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        var paint = Paint.obtain();
        paint.setColor(mColor);
        paint.setAlpha(ShapeDrawable.modulateAlpha(paint.getAlpha(), mAlpha));
        if (paint.getAlpha() != 0) {
            var b = getBounds();
            // map (0, -960, 960, 0) to dst bounds, and mirror if needed
            float sx = b.width() / VIEWPORT_SIZE;
            float sy = b.height() / VIEWPORT_SIZE;
            mTmpMatrix.setScaleTranslate(sx, sy, b.left, b.top + VIEWPORT_SIZE * sy);
            if (mAutoMirrored && getLayoutDirection() == LayoutDirection.RTL) {
                mTmpMatrix.postScale(-1.0f, 1.0f);
                mTmpMatrix.postTranslate(b.width(), 0);
            }
            canvas.save();
            canvas.concat(mTmpMatrix);
            switch (mIconType) {
                case KEYBOARD_ARROW_RIGHT -> keyboard_arrow_right(canvas, paint);
                case KEYBOARD_ARROW_LEFT -> keyboard_arrow_left(canvas, paint);
                case KEYBOARD_ARROW_DOWN -> keyboard_arrow_down(canvas, paint);
                case KEYBOARD_ARROW_UP -> keyboard_arrow_up(canvas, paint);
                case CHECK -> check(canvas, paint);
                case CHECK_SMALL -> check_small(canvas, paint);
                case CHECK_INDETERMINATE_SMALL -> check_indeterminate_small(canvas, paint);
                case RADIO_SMALL -> radio_small(canvas, paint);
            }
            canvas.restore();
        }
        paint.recycle();
    }

    void keyboard_arrow_right(@NonNull Canvas c, @NonNull Paint p) {
        var clip = mTmpClip;
        // clockwise
        clip[0] = 504; clip[1] = -480;
        clip[2] = 320; clip[3] = -664;
        clip[4] = 376; clip[5] = -720;
        clip[6] = 616; clip[7] = -480;
        c.drawEdgeAAQuad(null, clip, Canvas.QUAD_AA_FLAG_TOP|Canvas.QUAD_AA_FLAG_RIGHT|Canvas.QUAD_AA_FLAG_BOTTOM, p);
        // counter-clockwise
        clip[3] = -296;
        clip[5] = -240;
        c.drawEdgeAAQuad(null, clip, Canvas.QUAD_AA_FLAG_TOP|Canvas.QUAD_AA_FLAG_RIGHT|Canvas.QUAD_AA_FLAG_BOTTOM, p);
    }

    void keyboard_arrow_left(@NonNull Canvas c, @NonNull Paint p) {
        var clip = mTmpClip;
        // clockwise
        clip[0] = 320; clip[1] = -480;
        clip[2] = 560; clip[3] = -720;
        clip[4] = 616; clip[5] = -664;
        clip[6] = 432; clip[7] = -480;
        c.drawEdgeAAQuad(null, clip, Canvas.QUAD_AA_FLAG_TOP|Canvas.QUAD_AA_FLAG_RIGHT|Canvas.QUAD_AA_FLAG_BOTTOM, p);
        // counter-clockwise
        clip[3] = -240;
        clip[5] = -296;
        c.drawEdgeAAQuad(null, clip, Canvas.QUAD_AA_FLAG_TOP|Canvas.QUAD_AA_FLAG_RIGHT|Canvas.QUAD_AA_FLAG_BOTTOM, p);
    }

    void keyboard_arrow_down(@NonNull Canvas c, @NonNull Paint p) {
        var clip = mTmpClip;
        // clockwise
        clip[0] = 480; clip[1] = -334;
        clip[2] = 240; clip[3] = -584;
        clip[4] = 296; clip[5] = -640;
        clip[6] = 480; clip[7] = -456;
        c.drawEdgeAAQuad(null, clip, Canvas.QUAD_AA_FLAG_TOP|Canvas.QUAD_AA_FLAG_RIGHT|Canvas.QUAD_AA_FLAG_BOTTOM, p);
        // counter-clockwise
        clip[2] = 720;
        clip[4] = 664;
        c.drawEdgeAAQuad(null, clip, Canvas.QUAD_AA_FLAG_TOP|Canvas.QUAD_AA_FLAG_RIGHT|Canvas.QUAD_AA_FLAG_BOTTOM, p);
    }

    void keyboard_arrow_up(@NonNull Canvas c, @NonNull Paint p) {
        var clip = mTmpClip;
        // clockwise
        clip[0] = 480; clip[1] = -528;
        clip[2] = 296; clip[3] = -344;
        clip[4] = 240; clip[5] = -400;
        clip[6] = 480; clip[7] = -640;
        c.drawEdgeAAQuad(null, clip, Canvas.QUAD_AA_FLAG_TOP|Canvas.QUAD_AA_FLAG_RIGHT|Canvas.QUAD_AA_FLAG_BOTTOM, p);
        // counter-clockwise
        clip[2] = 664;
        clip[4] = 720;
        c.drawEdgeAAQuad(null, clip, Canvas.QUAD_AA_FLAG_TOP|Canvas.QUAD_AA_FLAG_RIGHT|Canvas.QUAD_AA_FLAG_BOTTOM, p);
    }

    void check(@NonNull Canvas c, @NonNull Paint p) {
        var clip = mTmpClip;
        // clockwise
        clip[0] = 382; clip[1] = -240;
        clip[2] = 154; clip[3] = -468;
        clip[4] = 211; clip[5] = -525;
        clip[6] = 382; clip[7] = -354;
        c.drawEdgeAAQuad(null, clip, Canvas.QUAD_AA_FLAG_TOP|Canvas.QUAD_AA_FLAG_RIGHT|Canvas.QUAD_AA_FLAG_BOTTOM, p);
        // counter-clockwise
        clip[2] = 806; clip[3] = -664;
        clip[4] = 749; clip[5] = -721;
        c.drawEdgeAAQuad(null, clip, Canvas.QUAD_AA_FLAG_TOP|Canvas.QUAD_AA_FLAG_RIGHT|Canvas.QUAD_AA_FLAG_BOTTOM, p);
    }

    void check_small(@NonNull Canvas c, @NonNull Paint p) {
        var clip = mTmpClip;
        // clockwise
        clip[0] = 400; clip[1] = -304;
        clip[2] = 240; clip[3] = -464;
        clip[4] = 296; clip[5] = -520;
        clip[6] = 400; clip[7] = -416;
        c.drawEdgeAAQuad(null, clip, Canvas.QUAD_AA_FLAG_TOP|Canvas.QUAD_AA_FLAG_RIGHT|Canvas.QUAD_AA_FLAG_BOTTOM, p);
        // counter-clockwise
        clip[2] = 720; clip[3] = -624;
        clip[4] = 664; clip[5] = -680;
        c.drawEdgeAAQuad(null, clip, Canvas.QUAD_AA_FLAG_TOP|Canvas.QUAD_AA_FLAG_RIGHT|Canvas.QUAD_AA_FLAG_BOTTOM, p);
    }

    void check_indeterminate_small(@NonNull Canvas c, @NonNull Paint p) {
        c.drawRect(240, -520, 720, -440, p);
    }

    void radio_small(@NonNull Canvas c, @NonNull Paint p) {
        c.drawCircle(480, -480, 200, p);
    }

    @Override
    public void setTintList(@Nullable ColorStateList tint) {
        if (mTint != tint) {
            mTint = tint;
            if (tint != null) {
                mColor = tint.getColorForState(getState(), ~0);
            } else {
                mColor = ~0;
            }
            invalidateSelf();
        }
    }

    @Override
    protected boolean onStateChange(@NonNull int[] stateSet) {
        if (mTint != null) {
            mColor = mTint.getColorForState(stateSet, ~0);
            return true;
        }
        return false;
    }

    @Override
    public boolean isStateful() {
        return super.isStateful() || (mTint != null && mTint.isStateful());
    }

    @Override
    public boolean hasFocusStateSpecified() {
        return mTint != null && mTint.hasFocusStateSpecified();
    }

    @Override
    public void setAlpha(int alpha) {
        if (mAlpha != alpha) {
            mAlpha = alpha;
            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
        return mAlpha;
    }

    @Override
    public void setAutoMirrored(boolean mirrored) {
        if (mAutoMirrored != mirrored) {
            mAutoMirrored = mirrored;
            invalidateSelf();
        }
    }

    @Override
    public boolean isAutoMirrored() {
        return mAutoMirrored;
    }

    @Override
    public int getIntrinsicWidth() {
        return mSize;
    }

    @Override
    public int getIntrinsicHeight() {
        return mSize;
    }
}
