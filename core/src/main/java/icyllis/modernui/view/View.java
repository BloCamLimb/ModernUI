/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.view;

import icyllis.modernui.ModernUI;
import icyllis.modernui.animation.AnimationHandler;
import icyllis.modernui.annotation.CallSuper;
import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.math.*;
import icyllis.modernui.text.TextUtils;
import icyllis.modernui.util.LayoutDirection;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

import static java.lang.Math.max;

/**
 * View is the basic component of UI. View has its own rectangular area on screen,
 * which is also responsible for drawing and event handling.
 * <p>
 * Measure ...
 * <p>
 * Layout ...
 * <p>
 * Attaching ...
 * <p>
 * Hierarchy ...
 * <p>
 * Drawing Contents ...
 * <p>
 * Text Input & Drawing ...
 * <p>
 * Scrolling ...
 * <p>
 * Event Handling ... Native Events & Derivative Events
 * <p>
 * Listeners ...
 * <p>
 * Custom View ...
 *
 * @since 2.0
 */
@UiThread
@SuppressWarnings("unused")
public class View implements Drawable.Callback {

    /**
     * Log marker.
     */
    public static final Marker VIEW_MARKER = MarkerManager.getMarker("View");

    /**
     * Used to mark a View that has no ID.
     */
    public static final int NO_ID = -1;

    /**
     * Temporary Rect currently for use in setBackground().  This will probably
     * be extended in the future to hold our own class with more than just
     * a Rect. :)
     */
    static final ThreadLocal<Rect> sThreadLocal = ThreadLocal.withInitial(Rect::new);

    /*
     * Private masks
     *
     * |--------|--------|--------|--------|
     *                             1         PFLAG_SKIP_DRAW
     * |--------|--------|--------|--------|
     *                         1             PFLAG_DRAWABLE_STATE_DIRTY
     *                        1              PFLAG_MEASURED_DIMENSION_SET
     *                       1               PFLAG_FORCE_LAYOUT
     *                      1                PFLAG_LAYOUT_REQUIRED
     *                     1                 PFLAG_PRESSED
     * |--------|--------|--------|--------|
     *       1                               PFLAG_CANCEL_NEXT_UP_EVENT
     *     1                                 PFLAG_HOVERED
     * |--------|--------|--------|--------|
     */
    static final int PFLAG_SKIP_DRAW = 0x00000080;
    static final int PFLAG_DRAWABLE_STATE_DIRTY = 0x00000400;
    static final int PFLAG_MEASURED_DIMENSION_SET = 0x00000800;
    static final int PFLAG_FORCE_LAYOUT = 0x00001000;
    static final int PFLAG_LAYOUT_REQUIRED = 0x00002000;
    private static final int PFLAG_PRESSED = 0x00004000;

    /**
     * Indicates whether the view is temporarily detached.
     */
    static final int PFLAG_CANCEL_NEXT_UP_EVENT = 0x04000000;

    /**
     * Indicates that the view has received HOVER_ENTER.  Cleared on HOVER_EXIT.
     */
    private static final int PFLAG_HOVERED = 0x10000000;

    private static final int PFLAG2_BACKGROUND_SIZE_CHANGED = 0x00000001;

    /**
     * Bits of {@link #getMeasuredWidthAndState()} and
     * {@link #getMeasuredWidthAndState()} that provide the actual measured size.
     */
    public static final int MEASURED_SIZE_MASK = 0x00ffffff;

    /**
     * Bits of {@link #getMeasuredWidthAndState()} and
     * {@link #getMeasuredWidthAndState()} that provide the additional state bits.
     */
    public static final int MEASURED_STATE_MASK = 0xff000000;

    /**
     * Bit shift of {@link #MEASURED_STATE_MASK} to get to the height bits
     * for functions that combine both width and height into a single int,
     * such as {@link #getMeasuredState()} and the childState argument of
     * {@link #resolveSizeAndState(int, int, int)}.
     */
    public static final int MEASURED_HEIGHT_STATE_SHIFT = 16;

    /**
     * Bit of {@link #getMeasuredWidthAndState()} and
     * {@link #getMeasuredWidthAndState()} that indicates the measured size
     * is smaller that the space the view would like to have.
     */
    public static final int MEASURED_STATE_TOO_SMALL = 0x01000000;

    /**
     * A flag to indicate that the layout direction of this view has not been defined yet.
     *
     * @hide
     */
    public static final int LAYOUT_DIRECTION_UNDEFINED = LayoutDirection.UNDEFINED;

    /**
     * Horizontal layout direction of this view is from Left to Right.
     * Use with {@link #setLayoutDirection}.
     */
    public static final int LAYOUT_DIRECTION_LTR = LayoutDirection.LTR;

    /**
     * Horizontal layout direction of this view is from Right to Left.
     * Use with {@link #setLayoutDirection}.
     */
    public static final int LAYOUT_DIRECTION_RTL = LayoutDirection.RTL;

    /**
     * Horizontal layout direction of this view is inherited from its parent.
     * Use with {@link #setLayoutDirection}.
     */
    public static final int LAYOUT_DIRECTION_INHERIT = LayoutDirection.INHERIT;

    /**
     * Horizontal layout direction of this view is from deduced from the default language
     * script for the locale. Use with {@link #setLayoutDirection}.
     */
    public static final int LAYOUT_DIRECTION_LOCALE = LayoutDirection.LOCALE;

    /**
     * Bit shift to get the horizontal layout direction. (bits after DRAG_HOVERED)
     *
     * @hide
     */
    static final int PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT = 2;

    /**
     * Mask for use with private flags indicating bits used for horizontal layout direction.
     *
     * @hide
     */
    static final int PFLAG2_LAYOUT_DIRECTION_MASK = 0x00000003 << PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT;

    /**
     * Indicates whether the view horizontal layout direction has been resolved and drawn to the
     * right-to-left direction.
     *
     * @hide
     */
    static final int PFLAG2_LAYOUT_DIRECTION_RESOLVED_RTL = 4 << PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT;

    /**
     * Indicates whether the view horizontal layout direction has been resolved.
     *
     * @hide
     */
    static final int PFLAG2_LAYOUT_DIRECTION_RESOLVED = 8 << PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT;

    /**
     * Mask for use with private flags indicating bits used for resolved horizontal layout direction.
     *
     * @hide
     */
    static final int PFLAG2_LAYOUT_DIRECTION_RESOLVED_MASK = 0x0000000C
            << PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT;

    /*
     * Array of horizontal layout direction flags for mapping attribute "layoutDirection" to correct
     * flag value.
     * @hide
     */
    private static final int[] LAYOUT_DIRECTION_FLAGS = {
            LAYOUT_DIRECTION_LTR,
            LAYOUT_DIRECTION_RTL,
            LAYOUT_DIRECTION_INHERIT,
            LAYOUT_DIRECTION_LOCALE
    };

    /**
     * Default horizontal layout direction.
     */
    private static final int LAYOUT_DIRECTION_DEFAULT = LAYOUT_DIRECTION_INHERIT;

    /**
     * Default horizontal layout direction.
     *
     * @hide
     */
    static final int LAYOUT_DIRECTION_RESOLVED_DEFAULT = LAYOUT_DIRECTION_LTR;

    /**
     * Text direction is inherited through {@link ViewGroup}
     */
    public static final int TEXT_DIRECTION_INHERIT = 0;

    /**
     * Text direction is using "first strong algorithm". The first strong directional character
     * determines the paragraph direction. If there is no strong directional character, the
     * paragraph direction is the view's resolved layout direction.
     */
    public static final int TEXT_DIRECTION_FIRST_STRONG = 1;

    /**
     * Text direction is using "any-RTL" algorithm. The paragraph direction is RTL if it contains
     * any strong RTL character, otherwise it is LTR if it contains any strong LTR characters.
     * If there are neither, the paragraph direction is the view's resolved layout direction.
     */
    public static final int TEXT_DIRECTION_ANY_RTL = 2;

    /**
     * Text direction is forced to LTR.
     */
    public static final int TEXT_DIRECTION_LTR = 3;

    /**
     * Text direction is forced to RTL.
     */
    public static final int TEXT_DIRECTION_RTL = 4;

    /**
     * Text direction is coming from the system Locale.
     */
    public static final int TEXT_DIRECTION_LOCALE = 5;

    /**
     * Text direction is using "first strong algorithm". The first strong directional character
     * determines the paragraph direction. If there is no strong directional character, the
     * paragraph direction is LTR.
     */
    public static final int TEXT_DIRECTION_FIRST_STRONG_LTR = 6;

    /**
     * Text direction is using "first strong algorithm". The first strong directional character
     * determines the paragraph direction. If there is no strong directional character, the
     * paragraph direction is RTL.
     */
    public static final int TEXT_DIRECTION_FIRST_STRONG_RTL = 7;

    /**
     * Default text direction is inherited
     */
    private static final int TEXT_DIRECTION_DEFAULT = TEXT_DIRECTION_INHERIT;

    /**
     * Default resolved text direction
     *
     * @hide
     */
    static final int TEXT_DIRECTION_RESOLVED_DEFAULT = TEXT_DIRECTION_FIRST_STRONG;

    /**
     * Bit shift to get the horizontal layout direction. (bits after LAYOUT_DIRECTION_RESOLVED)
     *
     * @hide
     */
    static final int PFLAG2_TEXT_DIRECTION_MASK_SHIFT = 6;

    /**
     * Mask for use with private flags indicating bits used for text direction.
     *
     * @hide
     */
    static final int PFLAG2_TEXT_DIRECTION_MASK = 0x00000007
            << PFLAG2_TEXT_DIRECTION_MASK_SHIFT;

    /**
     * Array of text direction flags for mapping attribute "textDirection" to correct
     * flag value.
     *
     * @hide
     */
    private static final int[] PFLAG2_TEXT_DIRECTION_FLAGS = {
            TEXT_DIRECTION_INHERIT << PFLAG2_TEXT_DIRECTION_MASK_SHIFT,
            TEXT_DIRECTION_FIRST_STRONG << PFLAG2_TEXT_DIRECTION_MASK_SHIFT,
            TEXT_DIRECTION_ANY_RTL << PFLAG2_TEXT_DIRECTION_MASK_SHIFT,
            TEXT_DIRECTION_LTR << PFLAG2_TEXT_DIRECTION_MASK_SHIFT,
            TEXT_DIRECTION_RTL << PFLAG2_TEXT_DIRECTION_MASK_SHIFT,
            TEXT_DIRECTION_LOCALE << PFLAG2_TEXT_DIRECTION_MASK_SHIFT,
            TEXT_DIRECTION_FIRST_STRONG_LTR << PFLAG2_TEXT_DIRECTION_MASK_SHIFT,
            TEXT_DIRECTION_FIRST_STRONG_RTL << PFLAG2_TEXT_DIRECTION_MASK_SHIFT
    };

    /**
     * Indicates whether the view text direction has been resolved.
     *
     * @hide
     */
    static final int PFLAG2_TEXT_DIRECTION_RESOLVED = 0x00000008
            << PFLAG2_TEXT_DIRECTION_MASK_SHIFT;

    /**
     * Bit shift to get the horizontal layout direction. (bits after DRAG_HOVERED)
     *
     * @hide
     */
    static final int PFLAG2_TEXT_DIRECTION_RESOLVED_MASK_SHIFT = 10;

    /**
     * Mask for use with private flags indicating bits used for resolved text direction.
     *
     * @hide
     */
    static final int PFLAG2_TEXT_DIRECTION_RESOLVED_MASK = 0x00000007
            << PFLAG2_TEXT_DIRECTION_RESOLVED_MASK_SHIFT;

    /**
     * Indicates whether the view text direction has been resolved to the "first strong" heuristic.
     *
     * @hide
     */
    static final int PFLAG2_TEXT_DIRECTION_RESOLVED_DEFAULT =
            TEXT_DIRECTION_RESOLVED_DEFAULT << PFLAG2_TEXT_DIRECTION_RESOLVED_MASK_SHIFT;

    /**
     * Default text alignment. The text alignment of this View is inherited from its parent.
     * Use with {@link #setTextAlignment(int)}
     */
    public static final int TEXT_ALIGNMENT_INHERIT = 0;

    /**
     * Default for the root view. The gravity determines the text alignment, ALIGN_NORMAL,
     * ALIGN_CENTER, or ALIGN_OPPOSITE, which are relative to each paragraph's text direction.
     * <p>
     * Use with {@link #setTextAlignment(int)}
     */
    public static final int TEXT_ALIGNMENT_GRAVITY = 1;

    /**
     * Align to the start of the paragraph, e.g. ALIGN_NORMAL.
     * <p>
     * Use with {@link #setTextAlignment(int)}
     */
    public static final int TEXT_ALIGNMENT_TEXT_START = 2;

    /**
     * Align to the end of the paragraph, e.g. ALIGN_OPPOSITE.
     * <p>
     * Use with {@link #setTextAlignment(int)}
     */
    public static final int TEXT_ALIGNMENT_TEXT_END = 3;

    /**
     * Center the paragraph, e.g. ALIGN_CENTER.
     * <p>
     * Use with {@link #setTextAlignment(int)}
     */
    public static final int TEXT_ALIGNMENT_CENTER = 4;

    /**
     * Align to the start of the view, which is ALIGN_LEFT if the view's resolved
     * layoutDirection is LTR, and ALIGN_RIGHT otherwise.
     * <p>
     * Use with {@link #setTextAlignment(int)}
     */
    public static final int TEXT_ALIGNMENT_VIEW_START = 5;

    /**
     * Align to the end of the view, which is ALIGN_RIGHT if the view's resolved
     * layoutDirection is LTR, and ALIGN_LEFT otherwise.
     * <p>
     * Use with {@link #setTextAlignment(int)}
     */
    public static final int TEXT_ALIGNMENT_VIEW_END = 6;

    /**
     * Default text alignment is inherited
     */
    private static final int TEXT_ALIGNMENT_DEFAULT = TEXT_ALIGNMENT_GRAVITY;

    /**
     * Default resolved text alignment
     *
     * @hide
     */
    static final int TEXT_ALIGNMENT_RESOLVED_DEFAULT = TEXT_ALIGNMENT_GRAVITY;

    /**
     * Bit shift to get the horizontal layout direction. (bits after DRAG_HOVERED)
     *
     * @hide
     */
    static final int PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT = 13;

    /**
     * Mask for use with private flags indicating bits used for text alignment.
     *
     * @hide
     */
    static final int PFLAG2_TEXT_ALIGNMENT_MASK = 0x00000007 << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT;

    /**
     * Array of text direction flags for mapping attribute "textAlignment" to correct
     * flag value.
     *
     * @hide
     */
    private static final int[] PFLAG2_TEXT_ALIGNMENT_FLAGS = {
            TEXT_ALIGNMENT_INHERIT << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT,
            TEXT_ALIGNMENT_GRAVITY << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT,
            TEXT_ALIGNMENT_TEXT_START << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT,
            TEXT_ALIGNMENT_TEXT_END << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT,
            TEXT_ALIGNMENT_CENTER << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT,
            TEXT_ALIGNMENT_VIEW_START << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT,
            TEXT_ALIGNMENT_VIEW_END << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT
    };

    /**
     * Indicates whether the view text alignment has been resolved.
     *
     * @hide
     */
    static final int PFLAG2_TEXT_ALIGNMENT_RESOLVED = 0x00000008 << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT;

    /**
     * Bit shift to get the resolved text alignment.
     *
     * @hide
     */
    static final int PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK_SHIFT = 17;

    /**
     * Mask for use with private flags indicating bits used for text alignment.
     *
     * @hide
     */
    static final int PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK = 0x00000007
            << PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK_SHIFT;

    /**
     * Indicates whether if the view text alignment has been resolved to gravity
     */
    private static final int PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT =
            TEXT_ALIGNMENT_RESOLVED_DEFAULT << PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK_SHIFT;

    /**
     * Flag indicating that start/end padding has been resolved into left/right padding
     * for use in measurement, layout, drawing, etc. This is set by {@link #resolvePadding()}
     * and checked by {@link #measure(int, int)} to determine if padding needs to be resolved
     * during measurement. In some special cases this is required such as when an adapter-based
     * view measures prospective children without attaching them to a window.
     */
    static final int PFLAG2_PADDING_RESOLVED = 0x20000000;

    /**
     * Flag indicating that the start/end drawables has been resolved into left/right ones.
     */
    static final int PFLAG2_DRAWABLE_RESOLVED = 0x40000000;

    /**
     * Indicates that the view is tracking some sort of transient state
     * that the app should not need to be aware of, but that the framework
     * should take special care to preserve.
     */
    static final int PFLAG2_HAS_TRANSIENT_STATE = 0x80000000;

    /**
     * Group of bits indicating that RTL properties resolution is done.
     */
    static final int ALL_RTL_PROPERTIES_RESOLVED = PFLAG2_LAYOUT_DIRECTION_RESOLVED |
            PFLAG2_TEXT_DIRECTION_RESOLVED |
            PFLAG2_TEXT_ALIGNMENT_RESOLVED |
            PFLAG2_PADDING_RESOLVED |
            PFLAG2_DRAWABLE_RESOLVED;

    // private flags
    int mPrivateFlags;
    int mPrivateFlags2;

    /**
     * View visibility.
     * {@link #setVisibility(int)}
     * {@link #getVisibility()}
     * <p>
     * This view is visible, as view's default value
     */
    public static final int VISIBLE = 0x00000000;

    /**
     * This view is invisible, but it still takes up space for layout.
     */
    public static final int INVISIBLE = 0x00000001;

    /**
     * This view is invisible, and it doesn't take any space for layout.
     */
    public static final int GONE = 0x00000002;

    /**
     * View visibility mask
     */
    static final int VISIBILITY_MASK = 0x00000003;

    /**
     * This view can receive keyboard events
     */
    static final int FOCUSABLE = 0x00000004;

    /**
     * This view is enabled. Interpretation varies by subclass.
     * Use with ENABLED_MASK when calling setFlags.
     */
    static final int ENABLED = 0x00000000;

    /**
     * This view is disabled. Interpretation varies by subclass.
     * Use with ENABLED_MASK when calling setFlags.
     */
    static final int DISABLED = 0x00000008;

    /**
     * Mask indicating bits used for indicating whether this view is enabled
     */
    static final int ENABLED_MASK = 0x00000008;

    /**
     * This view won't draw. {@link #onDraw(Canvas)} won't be
     * called and further optimizations will be performed. It is okay to have
     * this flag set and a background. Use with DRAW_MASK when calling setFlags.
     * {@hide}
     */
    static final int WILL_NOT_DRAW = 0x00000080;

    /**
     * Mask for use with setFlags indicating bits used for indicating whether
     * this view is will draw
     * {@hide}
     */
    static final int DRAW_MASK = 0x00000080;

    /**
     * This view doesn't show scrollbars.
     */
    static final int SCROLLBARS_NONE = 0x00000000;

    /**
     * This view shows horizontal scrollbar.
     */
    static final int SCROLLBARS_HORIZONTAL = 0x00000020;

    /**
     * This view shows vertical scrollbar.
     */
    static final int SCROLLBARS_VERTICAL = 0x00000040;

    /**
     * Mask for use with setFlags indicating bits used for indicating which
     * scrollbars are enabled.
     */
    static final int SCROLLBARS_MASK = 0x00000060;

    /**
     * Indicates this view can be clicked. When clickable, a View reacts
     * to clicks by notifying the OnClickListener.
     */
    static final int CLICKABLE = 0x00004000;

    /**
     * <p>
     * Indicates this view can be context clicked. When context clickable, a View reacts to a
     * context click (e.g. a primary stylus button press or right mouse click) by notifying the
     * OnContextClickListener.
     * </p>
     */
    static final int CONTEXT_CLICKABLE = 0x00800000;

    /*
     * View masks
     * |--------|--------|--------|--------|
     *                                   11  VISIBILITY
     *                                  1    FOCUSABLE
     *                                 1     ENABLED
     *                              11       SCROLLBARS
     * |--------|--------|--------|--------|
     *                     1                 CLICKABLE
     * |--------|--------|--------|--------|
     *           1                           CONTEXT_CLICKABLE
     * |--------|--------|--------|--------|
     */
    /**
     * The view flags hold various views states.
     * <p>
     * {@link #setStateFlag(int, int)}
     */
    int mViewFlags;

    /**
     * Parent view of this view
     * {@link #assignParent(ViewParent)}
     */
    private ViewParent mParent;

    /**
     * Internal use
     */
    AttachInfo mAttachInfo;

    /**
     * View id to identify this view in the hierarchy.
     * <p>
     * {@link #getId()}
     * {@link #setId(int)}
     */
    int mId = NO_ID;

    /**
     * The distance in pixels from the left edge of this view's parent
     * to the left edge of this view.
     */
    int mLeft;
    /**
     * The distance in pixels from the left edge of this view's parent
     * to the right edge of this view.
     */
    int mRight;
    /**
     * The distance in pixels from the top edge of this view's parent
     * to the top edge of this view.
     */
    int mTop;
    /**
     * The distance in pixels from the top edge of this view's parent
     * to the bottom edge of this view.
     */
    int mBottom;

    /**
     * The offset, in pixels, by which the content of this view is scrolled
     * horizontally.
     * Please use {@link View#getScrollX()} and {@link View#setScrollX(int)} instead of
     * accessing these directly.
     * {@hide}
     */
    protected int mScrollX;
    /**
     * The offset, in pixels, by which the content of this view is scrolled
     * vertically.
     * Please use {@link View#getScrollY()} and {@link View#setScrollY(int)} instead of
     * accessing these directly.
     * {@hide}
     */
    protected int mScrollY;

    /**
     * The final computed left padding in pixels that is used for drawing. This is the distance in
     * pixels between the left edge of this view and the left edge of its content.
     * {@hide}
     */
    protected int mPaddingLeft = 0;
    /**
     * The final computed right padding in pixels that is used for drawing. This is the distance in
     * pixels between the right edge of this view and the right edge of its content.
     * {@hide}
     */
    protected int mPaddingRight = 0;
    /**
     * The final computed top padding in pixels that is used for drawing. This is the distance in
     * pixels between the top edge of this view and the top edge of its content.
     * {@hide}
     */
    protected int mPaddingTop;
    /**
     * The final computed bottom padding in pixels that is used for drawing. This is the distance in
     * pixels between the bottom edge of this view and the bottom edge of its content.
     * {@hide}
     */
    protected int mPaddingBottom;

    /**
     * The transform matrix for the View. This transform is calculated internally
     * based on the translation, rotation, and scale properties. This matrix
     * affects the view both logically (such as for events) and visually.
     */
    private Transformation mTransformation;

    /**
     * The transition matrix for the View only used for transition animations. This
     * is a separate object because it does not change the properties of the view,
     * only visual effects are affected. This can be null if there is no animation.
     */
    @Nullable
    private Matrix4 mTransitionMatrix;

    /**
     * The opacity of the view as manipulated by the Fade transition. This is a
     * property only used by transitions, which is composited with the other alpha
     * values to calculate the final visual alpha value (offscreen rendering).
     * <p>
     * Note that a View has no alpha property, because alpha only affects visual
     * effects, and will have an impact on performance.
     */
    private float mTransitionAlpha = 1f;

    private int mMinWidth;
    private int mMinHeight;

    /**
     * The right padding after RTL resolution, but before taking account of scroll bars.
     *
     * @hide
     */
    protected int mUserPaddingRight;

    /**
     * The resolved bottom padding before taking account of scroll bars.
     *
     * @hide
     */
    protected int mUserPaddingBottom;

    /**
     * The left padding after RTL resolution, but before taking account of scroll bars.
     *
     * @hide
     */
    protected int mUserPaddingLeft;

    /**
     * Cache the paddingStart set by the user to append to the scrollbar's size.
     */
    int mUserPaddingStart;

    /**
     * Cache the paddingEnd set by the user to append to the scrollbar's size.
     */
    int mUserPaddingEnd;

    /**
     * The left padding as set by a setter method, a background's padding, or via XML property
     * resolution. This value is the padding before LTR resolution or taking account of scrollbars.
     *
     * @hide
     */
    int mUserPaddingLeftInitial;

    /**
     * The right padding as set by a setter method, a background's padding, or via XML property
     * resolution. This value is the padding before LTR resolution or taking account of scrollbars.
     *
     * @hide
     */
    int mUserPaddingRightInitial;

    /**
     * Default undefined padding
     */
    private static final int UNDEFINED_PADDING = Integer.MIN_VALUE;

    /**
     * Cache if a left padding has been defined explicitly via padding, horizontal padding,
     * or leftPadding in XML, or by setPadding(...) or setRelativePadding(...)
     */
    private boolean mLeftPaddingDefined = false;

    /**
     * Cache if a right padding has been defined explicitly via padding, horizontal padding,
     * or rightPadding in XML, or by setPadding(...) or setRelativePadding(...)
     */
    private boolean mRightPaddingDefined = false;

    /**
     * Cached previous measure spec to avoid unnecessary measurements
     */
    private int mOldWidthMeasureSpec = Integer.MIN_VALUE;
    private int mOldHeightMeasureSpec = Integer.MIN_VALUE;

    /**
     * The measurement result in onMeasure(), used to layout
     */
    private int mMeasuredWidth;
    private int mMeasuredHeight;

    private Drawable mBackground;

    private ScrollCache mScrollCache;

    private int[] mDrawableState = null;

    /**
     * The layout parameters associated with this view and used by the parent
     * {@link ViewGroup} to determine how this view should be laid out.
     */
    private ViewGroup.LayoutParams mLayoutParams;

    @Nullable
    ListenerInfo mListenerInfo;

    public View() {
    }

    /**
     * This method is called by ViewGroup.drawChild() to have each child view draw itself.
     */
    final void draw(@Nonnull Canvas canvas, @Nonnull ViewGroup group, boolean clip) {
        final boolean identity = hasIdentityMatrix();
        if (clip && identity &&
                canvas.quickReject(mLeft, mTop, mRight, mBottom)) {
            // quick rejected
            return;
        }

        float alpha = mTransitionAlpha;
        if (alpha <= 0) {
            // completely transparent
            return;
        }

        computeScroll();
        int sx = mScrollX;
        int sy = mScrollY;

        int saveCount = canvas.save();
        canvas.translate(mLeft, mTop);

        if (mTransitionMatrix != null) {
            canvas.multiply(mTransitionMatrix);
        }
        if (!identity) {
            canvas.multiply(getMatrix());
        }

        // true if clip region is not empty, or quick rejected
        boolean hasSpace = true;
        if (clip) {
            hasSpace = canvas.clipRect(0, 0, mRight - mLeft, mBottom - mTop);
        }

        canvas.translate(-sx, -sy);

        if (hasSpace) {
            //TODO stacked offscreen rendering
            /*if (alpha < 1) {

            }*/

            if ((mPrivateFlags & PFLAG_SKIP_DRAW) == PFLAG_SKIP_DRAW) {
                dispatchDraw(canvas);
            } else {
                draw(canvas);
            }
        }
        canvas.restoreToCount(saveCount);
    }

    /**
     * Base method that directly draws this view and its background, foreground,
     * overlay and all children to the given canvas. When implementing a view,
     * override {@link #onDraw(Canvas)} instead of this.
     * <p>
     * This is not the entry point for the view system to draw.
     *
     * @param canvas the canvas to draw content
     */
    @CallSuper
    public void draw(@Nonnull Canvas canvas) {
        drawBackground(canvas);

        onDraw(canvas);

        dispatchDraw(canvas);

        onDrawForeground(canvas);
    }

    private void drawBackground(@Nonnull Canvas canvas) {
        final Drawable background = mBackground;
        if (background == null) {
            return;
        }
        if ((mPrivateFlags2 & PFLAG2_BACKGROUND_SIZE_CHANGED) != 0) {
            background.setBounds(0, 0, mRight - mLeft, mBottom - mTop);
            mPrivateFlags2 &= ~PFLAG2_BACKGROUND_SIZE_CHANGED;
        }

        final int scrollX = mScrollX;
        final int scrollY = mScrollY;
        if ((scrollX | scrollY) == 0) {
            background.draw(canvas);
        } else {
            canvas.translate(scrollX, scrollY);
            background.draw(canvas);
            canvas.translate(-scrollX, -scrollY);
        }
    }

    /**
     * Draw the content of this view, implement this to do your drawing.
     * <p>
     * Note that (0, 0) will be the top left of the bounds, and (width, height)
     * will be the bottom right of the bounds.
     *
     * @param canvas the canvas to draw content
     */
    protected void onDraw(@Nonnull Canvas canvas) {
    }

    /**
     * Draw the child views.
     *
     * @param canvas the canvas to draw content
     */
    protected void dispatchDraw(@Nonnull Canvas canvas) {
    }

    /**
     * Draw any foreground content for this view.
     * <p>
     * Foreground content may consist of scroll bars, a {@link #setForeground foreground}
     * drawable or other view-specific decorations. The foreground is drawn on top of the
     * primary view content.
     *
     * @param canvas the canvas to draw content
     */
    public void onDrawForeground(@Nonnull Canvas canvas) {
        onDrawScrollBars(canvas);
    }

    /**
     * <p>Request the drawing of the horizontal and the vertical scrollbar. The
     * scrollbars are painted only if they have been awakened first.</p>
     *
     * @param canvas the canvas on which to draw the scrollbars
     * @see #awakenScrollBars(int)
     */
    protected final void onDrawScrollBars(@Nonnull Canvas canvas) {
        final ScrollCache cache = mScrollCache;
        if (cache == null || cache.mState == ScrollCache.OFF) {
            return;
        }
        boolean invalidate = false;
        if (cache.mState == ScrollCache.FADING) {
            long currentTime = AnimationHandler.currentTimeMillis();
            float fraction = (float) (currentTime - cache.mFadeStartTime) / cache.mFadeDuration;
            if (fraction >= 1.0f) {
                cache.mState = ScrollCache.OFF;
                return;
            } else {
                int alpha = 255 - (int) (fraction * 255);
                cache.mScrollBar.setAlpha(alpha);
            }
            invalidate = true;
        } else {
            cache.mScrollBar.setAlpha(255);
        }

        boolean drawHorizontalScrollBar = isHorizontalScrollBarEnabled();
        boolean drawVerticalScrollBar = isVerticalScrollBarEnabled();
        if (drawVerticalScrollBar || drawHorizontalScrollBar) {
            final ScrollBar scrollBar = cache.mScrollBar;

            if (drawHorizontalScrollBar) {
                scrollBar.setParameters(computeHorizontalScrollRange(),
                        computeHorizontalScrollOffset(),
                        computeHorizontalScrollExtent(), false);
                final Rect bounds = cache.mScrollBarBounds;
                getHorizontalScrollBarBounds(bounds, null);
                scrollBar.setBounds(bounds.left, bounds.top,
                        bounds.right, bounds.bottom);
                scrollBar.draw(canvas);
                if (invalidate) {
                    invalidate();
                }
            }

            if (drawVerticalScrollBar) {
                scrollBar.setParameters(computeVerticalScrollRange(),
                        computeVerticalScrollOffset(),
                        computeVerticalScrollExtent(), true);
                final Rect bounds = cache.mScrollBarBounds;
                getVerticalScrollBarBounds(bounds, null);
                scrollBar.setBounds(bounds.left, bounds.top,
                        bounds.right, bounds.bottom);
                scrollBar.draw(canvas);
                if (invalidate) {
                    invalidate();
                }
            }
        }
    }

    private void getHorizontalScrollBarBounds(@Nullable Rect drawBounds,
                                              @Nullable Rect touchBounds) {
        final Rect bounds = drawBounds != null ? drawBounds : touchBounds;
        if (bounds == null) {
            return;
        }
        final boolean drawVerticalScrollBar = isVerticalScrollBarEnabled();
        final int size = getHorizontalScrollbarHeight();
        final int verticalScrollBarGap = drawVerticalScrollBar ?
                getVerticalScrollbarWidth() : 0;
        final int width = mRight - mLeft;
        final int height = mBottom - mTop;
        bounds.top = mScrollY + height - size;
        bounds.left = mScrollX;
        bounds.right = mScrollX + width - verticalScrollBarGap;
        bounds.bottom = bounds.top + size;

        if (touchBounds == null) {
            return;
        }
        if (touchBounds != bounds) {
            touchBounds.set(bounds);
        }
    }

    private void getVerticalScrollBarBounds(@Nullable Rect drawBounds, @Nullable Rect touchBounds) {
        final Rect bounds = drawBounds != null ? drawBounds : touchBounds;
        if (bounds == null) {
            return;
        }
        final int size = getVerticalScrollbarWidth();
        final int width = mRight - mLeft;
        final int height = mBottom - mTop;
        bounds.left = mScrollX + width - size;
        bounds.top = mScrollY;
        bounds.right = bounds.left + size;
        bounds.bottom = mScrollY + height;

        if (touchBounds == null) {
            return;
        }
        if (touchBounds != bounds) {
            touchBounds.set(bounds);
        }
    }

    /**
     * Returns the width of the vertical scrollbar.
     *
     * @return The width in pixels of the vertical scrollbar or 0 if there
     * is no vertical scrollbar.
     */
    public int getVerticalScrollbarWidth() {
        ScrollCache cache = mScrollCache;
        if (cache != null) {
            ScrollBar scrollBar = cache.mScrollBar;
            if (scrollBar != null) {
                int size = scrollBar.getSize(true);
                if (size <= 0) {
                    size = cache.mScrollBarSize;
                }
                return size;
            }
            return 0;
        }
        return 0;
    }

    /**
     * Returns the height of the horizontal scrollbar.
     *
     * @return The height in pixels of the horizontal scrollbar or 0 if
     * there is no horizontal scrollbar.
     */
    protected int getHorizontalScrollbarHeight() {
        ScrollCache cache = mScrollCache;
        if (cache != null) {
            ScrollBar scrollBar = cache.mScrollBar;
            if (scrollBar != null) {
                int size = scrollBar.getSize(false);
                if (size <= 0) {
                    size = cache.mScrollBarSize;
                }
                return size;
            }
            return 0;
        }
        return 0;
    }

    /**
     * Called from client thread every tick on pre-tick, to update or cache something
     */
    @Deprecated
    protected void tick() {

    }

    /**
     * Specifies the rectangle area of this view and all its descendants.
     * <p>
     * Derived classes should NOT override this method for any reason.
     * Derived classes with children should override {@link #onLayout(boolean, int, int, int, int)}.
     * In that method, they should call layout() on each of their children
     *
     * @param left   left position, relative to parent
     * @param top    top position, relative to parent
     * @param right  right position, relative to parent
     * @param bottom bottom position, relative to parent
     */
    public void layout(int left, int top, int right, int bottom) {
        boolean changed = setFrame(left, top, right, bottom);

        if (changed || (mPrivateFlags & PFLAG_LAYOUT_REQUIRED) != 0) {
            onLayout(changed, left, top, right, bottom);

            mPrivateFlags &= ~PFLAG_LAYOUT_REQUIRED;
        }

        mPrivateFlags &= ~PFLAG_FORCE_LAYOUT;
    }

    /**
     * Layout child views, call from {@link #layout(int, int, int, int)}
     *
     * @param changed whether the size or position of this view was changed
     * @param left    left position, relative to parent
     * @param top     top position, relative to parent
     * @param right   right position, relative to parent
     * @param bottom  bottom position, relative to parent
     */
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    }

    /**
     * Assign the rectangle area of this view, called from layout()
     *
     * @param left   left position, relative to parent
     * @param top    top position, relative to parent
     * @param right  right position, relative to parent
     * @param bottom bottom position, relative to parent
     * @return whether the area of this view was changed
     */
    protected boolean setFrame(int left, int top, int right, int bottom) {
        if (mLeft != left || mRight != right || mTop != top || mBottom != bottom) {
            int oldWidth = mRight - mLeft;
            int oldHeight = mBottom - mTop;
            int newWidth = right - left;
            int newHeight = bottom - top;
            boolean sizeChanged = (newWidth != oldWidth) || (newHeight != oldHeight);

            mLeft = left;
            mTop = top;
            mRight = right;
            mBottom = bottom;

            if (sizeChanged) {
                onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
            }

            invalidate();
            mPrivateFlags2 |= PFLAG2_BACKGROUND_SIZE_CHANGED;
            return true;
        }
        return false;
    }

    /**
     * Called when width or height changed
     *
     * @param width      new width
     * @param height     new height
     * @param prevWidth  previous width
     * @param prevHeight previous height
     */
    protected void onSizeChanged(int width, int height, int prevWidth, int prevHeight) {

    }

    /**
     * This is called to find out how big a view should be. The parent
     * supplies constraint information in the width and height parameters.
     * <p>
     * This method is used to post the onMeasure() event, and the actual
     * measurement work is performed in {@link #onMeasure(int, int)}.
     *
     * @param widthMeasureSpec  width measure specification imposed by the parent
     * @param heightMeasureSpec height measure specification imposed by the parent
     * @throws IllegalStateException measured dimension is not set in
     *                               {@link #onMeasure(int, int)}
     * @see #onMeasure(int, int)
     */
    public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean needsLayout = (mPrivateFlags & PFLAG_FORCE_LAYOUT) != 0;

        if (!needsLayout) {
            boolean specChanged =
                    widthMeasureSpec != mOldWidthMeasureSpec
                            || heightMeasureSpec != mOldHeightMeasureSpec;
            boolean isSpecExactly =
                    MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY
                            && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY;
            boolean matchesSpecSize =
                    getMeasuredWidth() == MeasureSpec.getSize(widthMeasureSpec)
                            && getMeasuredHeight() == MeasureSpec.getSize(heightMeasureSpec);
            needsLayout = specChanged
                    && (!isSpecExactly || !matchesSpecSize);
        }

        if (needsLayout) {
            // remove the flag first anyway
            mPrivateFlags &= ~PFLAG_MEASURED_DIMENSION_SET;

            // measure ourselves, this should set the measured dimension flag back
            onMeasure(widthMeasureSpec, heightMeasureSpec);

            // the flag should be added in onMeasure() by calling setMeasuredDimension()
            if ((mPrivateFlags & PFLAG_MEASURED_DIMENSION_SET) == 0) {
                throw new IllegalStateException(getClass().getName() +
                        "#onMeasure() did not set the measured dimension" +
                        "by calling setMeasuredDimension()");
            }

            mPrivateFlags |= PFLAG_LAYOUT_REQUIRED;
        }

        mOldWidthMeasureSpec = widthMeasureSpec;
        mOldHeightMeasureSpec = heightMeasureSpec;
    }

    /**
     * Measure the view and its content to determine the measured width and the
     * measured height. This method is invoked by {@link #measure(int, int)} and
     * should be overridden by subclasses to provide accurate and efficient
     * measurement of their contents.
     * <p>
     * <strong>CONTRACT:</strong> When overriding this method, you
     * <em>must</em> call {@link #setMeasuredDimension(int, int)} to store the
     * measured width and height of this view. Failure to do so will trigger an
     * <code>IllegalStateException</code>, thrown by {@link #measure(int, int)}.
     * Calling super.onMeasure() is a valid use.
     * <p>
     * The base class implementation of measure defaults to the background size,
     * unless a larger size is allowed by the MeasureSpec. Subclasses should
     * override the base one to provide better measurements of their content.
     *
     * @param widthMeasureSpec  width measure specification imposed by the parent
     *                          {@link MeasureSpec}
     * @param heightMeasureSpec height measure specification imposed by the parent
     *                          {@link MeasureSpec}
     */
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getDefaultSize(mMinWidth, widthMeasureSpec),
                getDefaultSize(mMinHeight, heightMeasureSpec));
    }

    /**
     * Set the measured dimension, must be called by {@link #onMeasure(int, int)}
     *
     * @param measuredWidth  the measured width of this view
     * @param measuredHeight the measured height of this view
     */
    protected final void setMeasuredDimension(int measuredWidth, int measuredHeight) {
        mMeasuredWidth = measuredWidth;
        mMeasuredHeight = measuredHeight;

        mPrivateFlags |= PFLAG_MEASURED_DIMENSION_SET;
    }

    /**
     * Like {@link #getMeasuredWidthAndState()}, but only returns the
     * raw width component (that is the result is masked by
     * {@link #MEASURED_SIZE_MASK}).
     *
     * @return The raw measured width of this view.
     */
    public final int getMeasuredWidth() {
        return mMeasuredWidth & MEASURED_SIZE_MASK;
    }

    /**
     * Return the full width measurement information for this view as computed
     * by the most recent call to {@link #measure(int, int)}.  This result is a bit mask
     * as defined by {@link #MEASURED_SIZE_MASK} and {@link #MEASURED_STATE_TOO_SMALL}.
     * This should be used during measurement and layout calculations only. Use
     * {@link #getWidth()} to see how wide a view is after layout.
     *
     * @return The measured width of this view as a bit mask.
     */
    public final int getMeasuredWidthAndState() {
        return mMeasuredWidth;
    }

    /**
     * Like {@link #getMeasuredHeightAndState()}, but only returns the
     * raw height component (that is the result is masked by
     * {@link #MEASURED_SIZE_MASK}).
     *
     * @return The raw measured height of this view.
     */
    public final int getMeasuredHeight() {
        return mMeasuredHeight & MEASURED_SIZE_MASK;
    }

    /**
     * Return the full height measurement information for this view as computed
     * by the most recent call to {@link #measure(int, int)}.  This result is a bit mask
     * as defined by {@link #MEASURED_SIZE_MASK} and {@link #MEASURED_STATE_TOO_SMALL}.
     * This should be used during measurement and layout calculations only. Use
     * {@link #getHeight()} to see how high a view is after layout.
     *
     * @return The measured height of this view as a bit mask.
     */
    public final int getMeasuredHeightAndState() {
        return mMeasuredHeight;
    }

    /**
     * Return only the state bits of {@link #getMeasuredWidthAndState()}
     * and {@link #getMeasuredHeightAndState()}, combined into one integer.
     * The width component is in the regular bits {@link #MEASURED_STATE_MASK}
     * and the height component is at the shifted bits
     * {@link #MEASURED_HEIGHT_STATE_SHIFT}>>{@link #MEASURED_STATE_MASK}.
     */
    public final int getMeasuredState() {
        return (mMeasuredWidth & MEASURED_STATE_MASK)
                | ((mMeasuredHeight >> MEASURED_HEIGHT_STATE_SHIFT)
                & (MEASURED_STATE_MASK >> MEASURED_HEIGHT_STATE_SHIFT));
    }

    /**
     * Get the LayoutParams associated with this view. All views should have
     * layout parameters. These supply parameters to the <i>parent</i> of this
     * view specifying how it should be arranged. There are many subclasses of
     * ViewGroup.LayoutParams, and these correspond to the different subclasses
     * of ViewGroup that are responsible for arranging their children.
     * <p>
     * This method may return null if this View is not attached to a parent
     * ViewGroup or {@link #setLayoutParams(ViewGroup.LayoutParams)}
     * was not invoked successfully. When a View is attached to a parent
     * ViewGroup, this method must not return null.
     *
     * @return the LayoutParams associated with this view, or null if no
     * parameters have been set yet
     */
    public ViewGroup.LayoutParams getLayoutParams() {
        return mLayoutParams;
    }

    /**
     * Set the layout parameters associated with this view. These supply
     * parameters to the <i>parent</i> of this view specifying how it should be
     * arranged. There are many subclasses of ViewGroup.LayoutParams, and these
     * correspond to the different subclasses of ViewGroup that are responsible
     * for arranging their children.
     *
     * @param params layout parameters for this view
     */
    public void setLayoutParams(@Nonnull ViewGroup.LayoutParams params) {
        mLayoutParams = params;
        requestLayout();
    }

    /**
     * Resolve the layout parameters depending on the resolved layout direction
     *
     * @hide
     */
    public void resolveLayoutParams() {
        if (mLayoutParams != null) {
            mLayoutParams.resolveLayoutDirection(getLayoutDirection());
        }
    }

    /**
     * Merge two states as returned by {@link #getMeasuredState()}.
     *
     * @param curState The current state as returned from a view or the result
     *                 of combining multiple views.
     * @param newState The new view state to combine.
     * @return Returns a new integer reflecting the combination of the two
     * states.
     */
    public static int combineMeasuredStates(int curState, int newState) {
        return curState | newState;
    }

    /**
     * Version of {@link #resolveSizeAndState(int, int, int)}
     * returning only the {@link #MEASURED_SIZE_MASK} bits of the result.
     */
    public static int resolveSize(int size, int measureSpec) {
        return resolveSizeAndState(size, measureSpec, 0) & MEASURED_SIZE_MASK;
    }

    /**
     * Utility to reconcile a desired size and state, with constraints imposed
     * by a MeasureSpec. Will take the desired size, unless a different size
     * is imposed by the constraints. The returned value is a compound integer,
     * with the resolved size in the {@link #MEASURED_SIZE_MASK} bits and
     * optionally the bit {@link #MEASURED_STATE_TOO_SMALL} set if the
     * resulting size is smaller than the size the view wants to be.
     *
     * @param size               How big the view wants to be.
     * @param measureSpec        Constraints imposed by the parent.
     * @param childMeasuredState Size information bit mask for the view's
     *                           children.
     * @return Size information bit mask as defined by
     * {@link #MEASURED_SIZE_MASK} and
     * {@link #MEASURED_STATE_TOO_SMALL}.
     */
    public static int resolveSizeAndState(int size, int measureSpec, int childMeasuredState) {
        final int specMode = MeasureSpec.getMode(measureSpec);
        final int specSize = MeasureSpec.getSize(measureSpec);
        final int result;
        switch (specMode) {
            case MeasureSpec.AT_MOST:
                if (specSize < size) {
                    result = specSize | MEASURED_STATE_TOO_SMALL;
                } else {
                    result = size;
                }
                break;
            case MeasureSpec.EXACTLY:
                result = specSize;
                break;
            case MeasureSpec.UNSPECIFIED:
            default:
                result = size;
        }
        return result | (childMeasuredState & MEASURED_STATE_MASK);
    }

    /**
     * Utility to return a default size. Uses the supplied size if the
     * MeasureSpec imposed no constraints. Will get larger if allowed
     * by the MeasureSpec.
     *
     * @param size        Default size for this view
     * @param measureSpec Constraints imposed by the parent
     * @return The size this view should be.
     */
    public static int getDefaultSize(int size, int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        return switch (specMode) {
            case MeasureSpec.AT_MOST, MeasureSpec.EXACTLY -> specSize;
            default -> size;
        };
    }

    /**
     * Returns the suggested minimum height that the view should use. This
     * returns the maximum of the view's minimum height
     * and the background's minimum height
     * ({@link Drawable#getMinimumHeight()}).
     * <p>
     * When being used in {@link #onMeasure(int, int)}, the caller should still
     * ensure the returned height is within the requirements of the parent.
     *
     * @return The suggested minimum height of the view.
     */
    protected int getSuggestedMinimumHeight() {
        return (mBackground == null) ? mMinHeight : max(mMinHeight, mBackground.getMinimumHeight());
    }

    /**
     * Returns the suggested minimum width that the view should use. This
     * returns the maximum of the view's minimum width
     * and the background's minimum width
     * ({@link Drawable#getMinimumWidth()}).
     * <p>
     * When being used in {@link #onMeasure(int, int)}, the caller should still
     * ensure the returned width is within the requirements of the parent.
     *
     * @return The suggested minimum width of the view.
     */
    protected int getSuggestedMinimumWidth() {
        return (mBackground == null) ? mMinWidth : max(mMinWidth, mBackground.getMinimumWidth());
    }

    /**
     * Returns the minimum width of the view.
     *
     * @return the minimum width the view will try to be, in pixels
     * @see #setMinimumWidth(int)
     */
    public int getMinimumWidth() {
        return mMinWidth;
    }

    /**
     * Sets the minimum width of the view. It is not guaranteed the view will
     * be able to achieve this minimum width (for example, if its parent layout
     * constrains it with less available width).
     *
     * @param minWidth The minimum width the view will try to be, in pixels
     * @see #getMinimumWidth()
     */
    public void setMinimumWidth(int minWidth) {
        mMinWidth = minWidth;
        requestLayout();
    }

    /**
     * Returns the minimum height of the view.
     *
     * @return the minimum height the view will try to be, in pixels
     * @see #setMinimumHeight(int)
     */
    public int getMinimumHeight() {
        return mMinHeight;
    }

    /**
     * Sets the minimum height of the view. It is not guaranteed the view will
     * be able to achieve this minimum height (for example, if its parent layout
     * constrains it with less available height).
     *
     * @param minHeight The minimum height the view will try to be, in pixels
     * @see #getMinimumHeight()
     */
    public void setMinimumHeight(int minHeight) {
        mMinHeight = minHeight;
        requestLayout();
    }

    /**
     * Gets the parent of this view. Note the parent is not necessarily a View.
     *
     * @return the parent of this view
     */
    @Nullable
    public final ViewParent getParent() {
        return mParent;
    }

    /**
     * Sets the parent view during layout.
     *
     * @param parent the parent of this view
     */
    final void assignParent(@Nullable ViewParent parent) {
        if (mParent == null) {
            mParent = parent;
        } else if (parent == null) {
            mParent = null;
        } else {
            throw new IllegalStateException("The parent of view " + this + " has been assigned");
        }
    }

    /**
     * Returns this view's identifier.
     *
     * @return a positive integer used to identify the view or {@link #NO_ID}
     * if the view has no ID
     * @see #setId(int)
     * @see #findViewById(int)
     */
    public int getId() {
        return mId;
    }

    /**
     * Sets the identifier for this view. The identifier does not have to be
     * unique in this view's hierarchy. The identifier should be a positive
     * integer.
     *
     * @param id a number used to identify the view
     * @see #NO_ID
     * @see #getId()
     * @see #findViewById(int)
     */
    public void setId(int id) {
        mId = id;
    }

    //TODO state switching events
    void setStateFlag(int flag, int mask) {
        final int old = mViewFlags;

        mViewFlags = (mViewFlags & ~mask) | (flag & mask);

        final int change = mViewFlags ^ old;
    }

    /**
     * Set visibility of this view
     * See {@link #VISIBLE} or {@link #INVISIBLE} or {@link #GONE}
     *
     * @param visibility visibility to set
     */
    public void setVisibility(int visibility) {
        setStateFlag(visibility, VISIBILITY_MASK);
    }

    /**
     * Get visibility of this view.
     * See {@link #VISIBLE} or {@link #INVISIBLE} or {@link #GONE}
     *
     * @return visibility
     */
    public int getVisibility() {
        return mViewFlags & VISIBILITY_MASK;
    }

    /**
     * Returns the enabled status for this view. The interpretation of the
     * enabled state varies by subclass.
     *
     * @return True if this view is enabled, false otherwise.
     */
    public boolean isEnabled() {
        return (mViewFlags & ENABLED_MASK) == ENABLED;
    }

    /**
     * Set the enabled state of this view. The interpretation of the enabled
     * state varies by subclass.
     *
     * @param enabled True if this view is enabled, false otherwise.
     */
    public void setEnabled(boolean enabled) {
        if (enabled == isEnabled()) return;

        setFlags(enabled ? ENABLED : DISABLED, ENABLED_MASK);

        /*
         * The View most likely has to change its appearance, so refresh
         * the drawable state.
         */
        refreshDrawableState();

        // Invalidate too, since the default behavior for views is to be
        // be drawn at 50% alpha rather than to change the drawable.
        //invalidate(true);

        if (!enabled) {
            //cancelPendingInputEvents();
        }
    }

    /**
     * Indicates whether this view reacts to click events or not.
     *
     * @return true if the view is clickable, false otherwise
     * @see #setClickable(boolean)
     */
    public boolean isClickable() {
        return (mViewFlags & CLICKABLE) == CLICKABLE;
    }

    /**
     * Enables or disables click events for this view. When a view
     * is clickable it will change its state to "pressed" on every click.
     * Subclasses should set the view clickable to visually react to
     * user's clicks.
     *
     * @param clickable true to make the view clickable, false otherwise
     * @see #isClickable()
     */
    public void setClickable(boolean clickable) {
        setFlags(clickable ? CLICKABLE : 0, CLICKABLE);
    }

    /**
     * Indicates whether this view reacts to context clicks or not.
     *
     * @return true if the view is context clickable, false otherwise
     * @see #setContextClickable(boolean)
     */
    public boolean isContextClickable() {
        return (mViewFlags & CONTEXT_CLICKABLE) == CONTEXT_CLICKABLE;
    }

    /**
     * Enables or disables context clicking for this view. This event can launch the listener.
     *
     * @param contextClickable true to make the view react to a context click, false otherwise
     * @see #isContextClickable()
     */
    public void setContextClickable(boolean contextClickable) {
        setFlags(contextClickable ? CONTEXT_CLICKABLE : 0, CONTEXT_CLICKABLE);
    }

    /**
     * Sets the pressed state for this view.
     *
     * @param pressed Pass true to set the View's internal state to "pressed", or false to reverts
     *                the View's internal state from a previously set "pressed" state.
     * @see #isClickable()
     * @see #setClickable(boolean)
     */
    public void setPressed(boolean pressed) {
        final boolean needsRefresh = pressed != ((mPrivateFlags & PFLAG_PRESSED) == PFLAG_PRESSED);

        if (pressed) {
            mPrivateFlags |= PFLAG_PRESSED;
        } else {
            mPrivateFlags &= ~PFLAG_PRESSED;
        }

        if (needsRefresh) {
            refreshDrawableState();
        }
        dispatchSetPressed(pressed);
    }

    /**
     * Dispatch setPressed to all of this View's children.
     *
     * @param pressed The new pressed state
     * @see #setPressed(boolean)
     */
    protected void dispatchSetPressed(boolean pressed) {
    }

    /**
     * Indicates whether the view is currently in pressed state. Unless
     * {@link #setPressed(boolean)} is explicitly called, only clickable views can enter
     * the pressed state.
     *
     * @return true if the view is currently pressed, false otherwise
     * @see #setPressed(boolean)
     * @see #isClickable()
     * @see #setClickable(boolean)
     */
    public boolean isPressed() {
        return (mPrivateFlags & PFLAG_PRESSED) == PFLAG_PRESSED;
    }

    /**
     * Set flags controlling behavior of this view.
     *
     * @param flags Constant indicating the value which should be set
     * @param mask  Constant indicating the bit range that should be changed
     */
    void setFlags(int flags, int mask) {
        int old = mViewFlags;
        mViewFlags = (mViewFlags & ~mask) | (flags & mask);

        int changed = mViewFlags ^ old;
        if (changed == 0) {
            return;
        }

        if ((changed & DRAW_MASK) != 0) {
            if ((mViewFlags & WILL_NOT_DRAW) != 0) {
                if (mBackground != null) {
                    mPrivateFlags &= ~PFLAG_SKIP_DRAW;
                } else {
                    mPrivateFlags |= PFLAG_SKIP_DRAW;
                }
            } else {
                mPrivateFlags &= ~PFLAG_SKIP_DRAW;
            }
            requestLayout();
            invalidate();
        }
    }

    /**
     * Invalidate the whole view hierarchy. All views will be redrawn
     * in the future.
     */
    public final void invalidate() {
        if (mAttachInfo != null) {
            mAttachInfo.mViewRootImpl.invalidate();
        }
    }

    /**
     * Invalidates the specified Drawable.
     *
     * @param drawable the drawable to invalidate
     */
    @Override
    public void invalidateDrawable(@Nonnull Drawable drawable) {
        if (verifyDrawable(drawable)) {
            invalidate();
        }
    }

    /**
     * Define whether the horizontal scrollbar should have or not.
     *
     * @param enabled true if the horizontal scrollbar should be enabled
     * @see #isHorizontalScrollBarEnabled()
     */
    public void setHorizontalScrollBarEnabled(boolean enabled) {
        if (isHorizontalScrollBarEnabled() != enabled) {
            mViewFlags ^= SCROLLBARS_HORIZONTAL;
        }
    }

    /**
     * Define whether the vertical scrollbar should have or not.
     *
     * @param enabled true if the vertical scrollbar should be enabled
     * @see #isVerticalScrollBarEnabled()
     */
    public void setVerticalScrollBarEnabled(boolean enabled) {
        if (isVerticalScrollBarEnabled() != enabled) {
            mViewFlags ^= SCROLLBARS_VERTICAL;
        }
    }

    /**
     * Indicate whether the horizontal scrollbar should have or not.
     *
     * @return true if the horizontal scrollbar already created, false otherwise
     * @see #setHorizontalScrollBarEnabled(boolean)
     */
    public boolean isHorizontalScrollBarEnabled() {
        return (mViewFlags & SCROLLBARS_HORIZONTAL) != 0;
    }

    /**
     * Indicate whether the vertical scrollbar should have or not.
     *
     * @return true if the vertical scrollbar already created, false otherwise
     * @see #setVerticalScrollBarEnabled(boolean)
     */
    public boolean isVerticalScrollBarEnabled() {
        return (mViewFlags & SCROLLBARS_VERTICAL) != 0;
    }

    private void initScrollCache() {
        if (mScrollCache == null) {
            mScrollCache = new ScrollCache(this);
        }
    }

    private void initializeScrollBarDrawable() {
        initScrollCache();

        if (mScrollCache.mScrollBar == null) {
            mScrollCache.mScrollBar = new ScrollBar();
            mScrollCache.mScrollBar.setCallback(this);
        }
    }

    /**
     * Defines the vertical scrollbar thumb drawable
     *
     * @see #isVerticalScrollBarEnabled()
     * @see #setVerticalScrollBarEnabled(boolean)
     */
    public void setVerticalScrollbarThumbDrawable(@Nullable Drawable drawable) {
        initializeScrollBarDrawable();
        mScrollCache.mScrollBar.setVerticalThumbDrawable(drawable);
    }

    /**
     * Defines the vertical scrollbar track drawable
     *
     * @see #isVerticalScrollBarEnabled()
     * @see #setVerticalScrollBarEnabled(boolean)
     */
    public void setVerticalScrollbarTrackDrawable(@Nullable Drawable drawable) {
        initializeScrollBarDrawable();
        mScrollCache.mScrollBar.setVerticalTrackDrawable(drawable);
    }

    /**
     * Defines the horizontal thumb drawable
     *
     * @see #isHorizontalScrollBarEnabled()
     * @see #setHorizontalScrollBarEnabled(boolean)
     */
    public void setHorizontalScrollbarThumbDrawable(@Nullable Drawable drawable) {
        initializeScrollBarDrawable();
        mScrollCache.mScrollBar.setHorizontalThumbDrawable(drawable);
    }

    /**
     * Defines the horizontal track drawable
     *
     * @see #isHorizontalScrollBarEnabled()
     * @see #setHorizontalScrollBarEnabled(boolean)
     */
    public void setHorizontalScrollbarTrackDrawable(@Nullable Drawable drawable) {
        initializeScrollBarDrawable();
        mScrollCache.mScrollBar.setHorizontalTrackDrawable(drawable);
    }

    /**
     * Returns the currently configured Drawable for the thumb of the vertical scroll bar if it
     * exists, null otherwise.
     *
     * @see #isVerticalScrollBarEnabled()
     * @see #setVerticalScrollBarEnabled(boolean)
     */
    @Nullable
    public Drawable getVerticalScrollbarThumbDrawable() {
        return mScrollCache != null && mScrollCache.mScrollBar != null
                ? mScrollCache.mScrollBar.getVerticalThumbDrawable() : null;
    }

    /**
     * Returns the currently configured Drawable for the track of the vertical scroll bar if it
     * exists, null otherwise.
     *
     * @see #isVerticalScrollBarEnabled()
     * @see #setVerticalScrollBarEnabled(boolean)
     */
    @Nullable
    public Drawable getVerticalScrollbarTrackDrawable() {
        return mScrollCache != null && mScrollCache.mScrollBar != null
                ? mScrollCache.mScrollBar.getVerticalTrackDrawable() : null;
    }

    /**
     * Returns the currently configured Drawable for the thumb of the horizontal scroll bar if it
     * exists, null otherwise.
     *
     * @see #isHorizontalScrollBarEnabled()
     * @see #setHorizontalScrollBarEnabled(boolean)
     */
    @Nullable
    public Drawable getHorizontalScrollbarThumbDrawable() {
        return mScrollCache != null && mScrollCache.mScrollBar != null
                ? mScrollCache.mScrollBar.getHorizontalThumbDrawable() : null;
    }

    /**
     * Returns the currently configured Drawable for the track of the horizontal scroll bar if it
     * exists, null otherwise.
     *
     * @see #isHorizontalScrollBarEnabled()
     * @see #setHorizontalScrollBarEnabled(boolean)
     */
    @Nullable
    public Drawable getHorizontalScrollbarTrackDrawable() {
        return mScrollCache != null && mScrollCache.mScrollBar != null ?
                mScrollCache.mScrollBar.getHorizontalTrackDrawable() : null;
    }

    /**
     * <p>Trigger the scrollbars to draw. When invoked this method starts an
     * animation to fade the scrollbars out after a default delay. If a subclass
     * provides animated scrolling, the start delay should equal the duration
     * of the scrolling animation.</p>
     *
     * <p>The animation starts only if at least one of the scrollbars is
     * enabled, as specified by {@link #isHorizontalScrollBarEnabled()} and
     * {@link #isVerticalScrollBarEnabled()}. When the animation is started,
     * this method returns true, and false otherwise. If the animation is
     * started, this method calls {@link #invalidate()}; in that case the
     * caller should not call {@link #invalidate()}.</p>
     *
     * <p>This method should be invoked every time a subclass directly updates
     * the scroll parameters.</p>
     *
     * <p>This method is automatically invoked by {@link #scrollBy(int, int)}
     * and {@link #scrollTo(int, int)}.</p>
     *
     * @return true if the animation is played, false otherwise
     * @see #awakenScrollBars(int)
     * @see #scrollBy(int, int)
     * @see #scrollTo(int, int)
     * @see #isHorizontalScrollBarEnabled()
     * @see #isVerticalScrollBarEnabled()
     * @see #setHorizontalScrollBarEnabled(boolean)
     * @see #setVerticalScrollBarEnabled(boolean)
     */
    protected final boolean awakenScrollBars() {
        return mScrollCache != null && awakenScrollBars(500);
    }

    /**
     * <p>
     * Trigger the scrollbars to draw. When invoked this method starts an
     * animation to fade the scrollbars out after a fixed delay. If a subclass
     * provides animated scrolling, the start delay should equal the duration of
     * the scrolling animation.
     * </p>
     *
     * <p>
     * The animation starts only if at least one of the scrollbars is enabled,
     * as specified by {@link #isHorizontalScrollBarEnabled()} and
     * {@link #isVerticalScrollBarEnabled()}. When the animation is started,
     * this method returns true, and false otherwise. If the animation is
     * started, this method calls {@link #invalidate()}; in that case the caller
     * should not call {@link #invalidate()}.
     * </p>
     *
     * <p>
     * This method should be invoked every time a subclass directly updates the
     * scroll parameters.
     * </p>
     *
     * @param startDelay the delay, in milliseconds, after which the animation
     *                   should start; when the delay is 0, the animation starts
     *                   immediately
     * @return true if the animation is played, false otherwise
     * @see #scrollBy(int, int)
     * @see #scrollTo(int, int)
     * @see #isHorizontalScrollBarEnabled()
     * @see #isVerticalScrollBarEnabled()
     * @see #setHorizontalScrollBarEnabled(boolean)
     * @see #setVerticalScrollBarEnabled(boolean)
     */
    protected boolean awakenScrollBars(int startDelay) {
        final ScrollCache scrollCache = mScrollCache;
        if (scrollCache == null || !scrollCache.mFadeScrollBars) {
            return false;
        }
        initializeScrollBarDrawable();
        if (isHorizontalScrollBarEnabled() || isVerticalScrollBarEnabled()) {
            if (scrollCache.mState == ScrollCache.OFF) {
                // first takes longer
                startDelay = Math.max(1250, startDelay);
            } else {
                startDelay = Math.max(0, startDelay);
            }
            scrollCache.mFadeStartTime = AnimationHandler.currentTimeMillis() + startDelay;
            scrollCache.mState = ScrollCache.ON;
            if (startDelay <= 0) {
                scrollCache.mState = ScrollCache.FADING;
            } else if (mAttachInfo != null) {
                AnimationHandler.getInstance().register(scrollCache, startDelay);
            }
            invalidate();
            return true;
        }
        return false;
    }

    /**
     * Return the scrolled left position of this view. This is the left edge of
     * the displayed part of your view. You do not need to draw any pixels
     * farther left, since those are outside of the frame of your view on
     * screen.
     *
     * @return The left edge of the displayed part of your view, in pixels.
     */
    public final int getScrollX() {
        return mScrollX;
    }

    /**
     * Return the scrolled top position of this view. This is the top edge of
     * the displayed part of your view. You do not need to draw any pixels above
     * it, since those are outside of the frame of your view on screen.
     *
     * @return The top edge of the displayed part of your view, in pixels.
     */
    public final int getScrollY() {
        return mScrollY;
    }

    /**
     * <p>Compute the horizontal range that the horizontal scrollbar
     * represents.</p>
     *
     * <p>The range is expressed in arbitrary units that must be the same as the
     * units used by {@link #computeHorizontalScrollExtent()} and
     * {@link #computeHorizontalScrollOffset()}.</p>
     *
     * <p>The default range is the drawing width of this view.</p>
     *
     * @return the total horizontal range represented by the horizontal
     * scrollbar
     * @see #computeHorizontalScrollExtent()
     * @see #computeHorizontalScrollOffset()
     */
    protected int computeHorizontalScrollRange() {
        return getWidth();
    }

    /**
     * <p>Compute the horizontal offset of the horizontal scrollbar's thumb
     * within the horizontal range. This value is used to compute the position
     * of the thumb within the scrollbar's track.</p>
     *
     * <p>The range is expressed in arbitrary units that must be the same as the
     * units used by {@link #computeHorizontalScrollRange()} and
     * {@link #computeHorizontalScrollExtent()}.</p>
     *
     * <p>The default offset is the scroll offset of this view.</p>
     *
     * @return the horizontal offset of the scrollbar's thumb
     * @see #computeHorizontalScrollRange()
     * @see #computeHorizontalScrollExtent()
     */
    protected int computeHorizontalScrollOffset() {
        return mScrollX;
    }

    /**
     * <p>Compute the horizontal extent of the horizontal scrollbar's thumb
     * within the horizontal range. This value is used to compute the length
     * of the thumb within the scrollbar's track.</p>
     *
     * <p>The range is expressed in arbitrary units that must be the same as the
     * units used by {@link #computeHorizontalScrollRange()} and
     * {@link #computeHorizontalScrollOffset()}.</p>
     *
     * <p>The default extent is the drawing width of this view.</p>
     *
     * @return the horizontal extent of the scrollbar's thumb
     * @see #computeHorizontalScrollRange()
     * @see #computeHorizontalScrollOffset()
     */
    protected int computeHorizontalScrollExtent() {
        return getWidth();
    }

    /**
     * <p>Compute the vertical range that the vertical scrollbar represents.</p>
     *
     * <p>The range is expressed in arbitrary units that must be the same as the
     * units used by {@link #computeVerticalScrollExtent()} and
     * {@link #computeVerticalScrollOffset()}.</p>
     *
     * @return the total vertical range represented by the vertical scrollbar
     *
     * <p>The default range is the drawing height of this view.</p>
     * @see #computeVerticalScrollExtent()
     * @see #computeVerticalScrollOffset()
     */
    protected int computeVerticalScrollRange() {
        return getHeight();
    }

    /**
     * <p>Compute the vertical offset of the vertical scrollbar's thumb
     * within the horizontal range. This value is used to compute the position
     * of the thumb within the scrollbar's track.</p>
     *
     * <p>The range is expressed in arbitrary units that must be the same as the
     * units used by {@link #computeVerticalScrollRange()} and
     * {@link #computeVerticalScrollExtent()}.</p>
     *
     * <p>The default offset is the scroll offset of this view.</p>
     *
     * @return the vertical offset of the scrollbar's thumb
     * @see #computeVerticalScrollRange()
     * @see #computeVerticalScrollExtent()
     */
    protected int computeVerticalScrollOffset() {
        return mScrollY;
    }

    /**
     * <p>Compute the vertical extent of the vertical scrollbar's thumb
     * within the vertical range. This value is used to compute the length
     * of the thumb within the scrollbar's track.</p>
     *
     * <p>The range is expressed in arbitrary units that must be the same as the
     * units used by {@link #computeVerticalScrollRange()} and
     * {@link #computeVerticalScrollOffset()}.</p>
     *
     * <p>The default extent is the drawing height of this view.</p>
     *
     * @return the vertical extent of the scrollbar's thumb
     * @see #computeVerticalScrollRange()
     * @see #computeVerticalScrollOffset()
     */
    protected int computeVerticalScrollExtent() {
        return getHeight();
    }

    /**
     * Get the width of the view.
     *
     * @return the width in pixels
     */
    public final int getWidth() {
        return mRight - mLeft;
    }

    /**
     * Get the height of the view.
     *
     * @return the height in pixels
     */
    public final int getHeight() {
        return mBottom - mTop;
    }

    /**
     * Get view logic left position on screen.
     * The sum of scroll amounts of all parent views are not counted.
     *
     * @return left
     */
    public final int getLeft() {
        return mLeft;
    }

    /**
     * Get view logic top position on screen.
     * The sum of scroll amounts of all parent views are not counted.
     *
     * @return top
     */
    public final int getTop() {
        return mTop;
    }

    /**
     * Get view logic right position on screen.
     * The sum of scroll amounts of all parent views are not counted.
     *
     * @return right
     */
    public final int getRight() {
        return mRight;
    }

    /**
     * Get view logic bottom position on screen.
     * The sum of scroll amounts of all parent views are not counted.
     *
     * @return bottom
     */
    public final int getBottom() {
        return mBottom;
    }

    /**
     * The transform matrix of this view, which is calculated based on the current
     * rotation, scale, and pivot properties. This will affect this view logically
     * (such as for events) and visually.
     * <p>
     * Note that the matrix should be only used as read-only (like transforming
     * coordinates). In that case, you should call {@link #hasIdentityMatrix()}
     * first to check if the operation can be skipped, because this method will
     * create the matrix if not available. However, if you want to change the
     * transform matrix, you should use methods such as {@link #setTranslationX(float)}.
     *
     * @return the current transform matrix for the view
     * @see #hasIdentityMatrix()
     * @see #getRotation()
     * @see #getScaleX()
     * @see #getScaleY()
     * @see #getPivotX()
     * @see #getPivotY()
     */
    @Nonnull
    public final Matrix4 getMatrix() {
        ensureTransformation();
        return mTransformation.getMatrix();
    }

    /**
     * Returns true if the transform matrix is the identity matrix.
     *
     * @return true if the transform matrix is the identity matrix, false otherwise.
     * @see #getMatrix()
     */
    public final boolean hasIdentityMatrix() {
        if (mTransformation == null) {
            return true;
        }
        return mTransformation.getMatrix().isIdentity();
    }

    private void ensureTransformation() {
        if (mTransformation == null) {
            mTransformation = new Transformation();
        }
    }

    /**
     * Sets the horizontal location of this view relative to its {@link #getLeft() left} position.
     * This effectively positions the object post-layout, in addition to wherever the object's
     * layout placed it. This is not used for transition animation.
     *
     * @param translationX The horizontal position of this view relative to its left position,
     *                     in pixels.
     */
    public void setTranslationX(float translationX) {
        ensureTransformation();
        mTransformation.setTranslationX(translationX);
    }

    /**
     * Changes the transition matrix on the view. This is only used in the transition animation
     * framework, {@link Transition}. When the animation finishes, the matrix
     * should be cleared by calling this method with <code>null</code> as the matrix parameter.
     * <p>
     * Note that this matrix only affects the visual effect of this view, you should never
     * call this method. You should use methods such as {@link #setTranslationX(float)}} instead
     * to change the transformation logically.
     *
     * @param matrix the matrix, null indicates that the matrix should be cleared.
     * @see #getTransitionMatrix()
     */
    public final void setTransitionMatrix(@Nullable Matrix4 matrix) {
        mTransitionMatrix = matrix;
    }

    /**
     * Changes the transition matrix on the view. This is only used in the transition animation
     * framework, {@link Transition}. Returns <code>null</code> when there is no
     * transformation provided by {@link #setTransitionMatrix(Matrix4)}.
     * <p>
     * Note that this matrix only affects the visual effect of this view, you should never
     * call this method. You should use methods such as {@link #setTranslationX(float)}} instead
     * to change the transformation logically.
     *
     * @return the matrix, null indicates that the matrix should be cleared.
     * @see #setTransitionMatrix(Matrix4)
     */
    @Nullable
    public final Matrix4 getTransitionMatrix() {
        return mTransitionMatrix;
    }

    void dispatchAttachedToWindow(AttachInfo info) {
        mAttachInfo = info;
    }

    /**
     * If this view doesn't do any drawing on its own, set this flag to
     * allow further optimizations. By default, this flag is not set on
     * View, but could be set on some View subclasses such as ViewGroup.
     * <p>
     * Typically, if you override {@link #onDraw(Canvas)}
     * you should clear this flag.
     *
     * @param willNotDraw whether or not this View draw on its own
     */
    public void setWillNotDraw(boolean willNotDraw) {
        setFlags(willNotDraw ? WILL_NOT_DRAW : 0, DRAW_MASK);
    }

    /**
     * Returns whether or not this View draws on its own.
     *
     * @return true if this view has nothing to draw, false otherwise
     */
    public boolean willNotDraw() {
        return (mViewFlags & DRAW_MASK) == WILL_NOT_DRAW;
    }

    /// SECTION START - Direction, RTL \\\

    /**
     * Returns the layout direction for this view.
     *
     * @return One of {@link #LAYOUT_DIRECTION_LTR},
     * {@link #LAYOUT_DIRECTION_RTL},
     * {@link #LAYOUT_DIRECTION_INHERIT} or
     * {@link #LAYOUT_DIRECTION_LOCALE}.
     * @hide
     */
    public final int getRawLayoutDirection() {
        return (mPrivateFlags2 & PFLAG2_LAYOUT_DIRECTION_MASK) >> PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT;
    }

    /**
     * Set the layout direction for this view. This will propagate a reset of layout direction
     * resolution to the view's children and resolve layout direction for this view.
     * <p>
     * Should be one of:
     * <p>
     * {@link #LAYOUT_DIRECTION_LTR},
     * {@link #LAYOUT_DIRECTION_RTL},
     * {@link #LAYOUT_DIRECTION_INHERIT},
     * {@link #LAYOUT_DIRECTION_LOCALE}.
     * <p>
     * Resolution will be done if the value is set to LAYOUT_DIRECTION_INHERIT. The resolution
     * proceeds up the parent chain of the view to get the value. If there is no parent, then it
     * will return the default {@link #LAYOUT_DIRECTION_LTR}.
     *
     * @param layoutDirection the layout direction to set
     */
    public void setLayoutDirection(int layoutDirection) {
        if (getRawLayoutDirection() != layoutDirection) {
            // Reset the current layout direction and the resolved one
            mPrivateFlags2 &= ~PFLAG2_LAYOUT_DIRECTION_MASK;
            resetRtlProperties();
            // Set the new layout direction (filtered)
            mPrivateFlags2 |=
                    ((layoutDirection << PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT) & PFLAG2_LAYOUT_DIRECTION_MASK);
            // We need to resolve all RTL properties as they all depend on layout direction
            resolveRtlProperties();
            requestLayout();
            invalidate();
        }
    }

    /**
     * Returns the resolved layout direction for this view.
     *
     * @return {@link #LAYOUT_DIRECTION_RTL} if the layout direction is RTL or returns
     * {@link #LAYOUT_DIRECTION_LTR} if the layout direction is not RTL.
     */
    public final int getLayoutDirection() {
        return ((mPrivateFlags2 & PFLAG2_LAYOUT_DIRECTION_RESOLVED_RTL) ==
                PFLAG2_LAYOUT_DIRECTION_RESOLVED_RTL) ? LAYOUT_DIRECTION_RTL : LAYOUT_DIRECTION_LTR;
    }

    /**
     * Indicates whether or not this view's layout is right-to-left. This is resolved from
     * layout attribute and/or the inherited value from the parent
     *
     * @return true if the layout is right-to-left.
     * @hide
     */
    public final boolean isLayoutRtl() {
        return (getLayoutDirection() == LAYOUT_DIRECTION_RTL);
    }

    /**
     * Resolve all RTL related properties.
     *
     * @hide
     */
    private void resolveRtlProperties() {
        // Overall check
        if ((mPrivateFlags2 & ALL_RTL_PROPERTIES_RESOLVED) == ALL_RTL_PROPERTIES_RESOLVED) {
            return;
        }

        // Order is important here: LayoutDirection MUST be resolved first
        if (!isLayoutDirectionResolved()) {
            resolveLayoutDirection();
            resolveLayoutParams();
        }
        // ... then we can resolve the others properties depending on the resolved LayoutDirection.
        if (!isTextDirectionResolved()) {
            resolveTextDirection();
        }
        if (!isTextAlignmentResolved()) {
            resolveTextAlignment();
        }
        // Should resolve Drawables before Padding because we need the layout direction of the
        // Drawable to correctly resolve Padding.
        if (!areDrawablesResolved()) {
            resolveDrawables();
        }
        if (!isPaddingResolved()) {
            resolvePadding();
        }
        onRtlPropertiesChanged(getLayoutDirection());
    }

    /**
     * Reset resolution of all RTL related properties.
     *
     * @hide
     */
    private void resetRtlProperties() {
        resetResolvedLayoutDirection();
        resetResolvedTextDirection();
        resetResolvedTextAlignment();
        resetResolvedPadding();
        resetResolvedDrawables();
    }

    /**
     * Called when any RTL property (layout direction or text direction or text alignment) has
     * been changed.
     * <p>
     * Subclasses need to override this method to take care of cached information that depends on the
     * resolved layout direction, or to inform child views that inherit their layout direction.
     * <p>
     * The default implementation does nothing.
     *
     * @param layoutDirection the direction of the layout
     * @see #LAYOUT_DIRECTION_LTR
     * @see #LAYOUT_DIRECTION_RTL
     */
    protected void onRtlPropertiesChanged(int layoutDirection) {
    }

    /**
     * Resolve and cache the layout direction. LTR is set initially. This is implicitly supposing
     * that the parent directionality can and will be resolved before its children.
     *
     * @return true if resolution has been done, false otherwise.
     * @hide
     */
    public boolean resolveLayoutDirection() {
        // Clear any previous layout direction resolution
        mPrivateFlags2 &= ~PFLAG2_LAYOUT_DIRECTION_RESOLVED_MASK;

        if (ModernUI.get().hasRtlSupport()) {
            // Set resolved depending on layout direction
            switch (getRawLayoutDirection()) {
                case LAYOUT_DIRECTION_INHERIT:
                    // We cannot resolve yet. LTR is by default and let the resolution happen again
                    // later to get the correct resolved value
                    if (!canResolveLayoutDirection()) return false;

                    // Parent has not yet resolved, LTR is still the default
                    try {
                        if (!mParent.isLayoutDirectionResolved()) return false;

                        if (mParent.getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
                            mPrivateFlags2 |= PFLAG2_LAYOUT_DIRECTION_RESOLVED_RTL;
                        }
                    } catch (AbstractMethodError e) {
                        ModernUI.LOGGER.error(VIEW_MARKER, mParent.getClass().getSimpleName() +
                                " does not fully implement ViewParent", e);
                    }
                    break;
                case LAYOUT_DIRECTION_RTL:
                    mPrivateFlags2 |= PFLAG2_LAYOUT_DIRECTION_RESOLVED_RTL;
                    break;
                case LAYOUT_DIRECTION_LOCALE:
                    if (TextUtils.getLayoutDirectionFromLocale(ModernUI.get().getSelectedLocale()) ==
                            LAYOUT_DIRECTION_RTL) {
                        mPrivateFlags2 |= PFLAG2_LAYOUT_DIRECTION_RESOLVED_RTL;
                    }
                    break;
                default:
                    // Nothing to do, LTR by default
            }
        }

        // Set to resolved
        mPrivateFlags2 |= PFLAG2_LAYOUT_DIRECTION_RESOLVED;
        return true;
    }

    /**
     * Check if layout direction resolution can be done.
     *
     * @return true if layout direction resolution can be done otherwise return false.
     */
    public final boolean canResolveLayoutDirection() {
        if (isLayoutDirectionInherited()) {
            if (mParent != null) {
                try {
                    return mParent.canResolveLayoutDirection();
                } catch (AbstractMethodError e) {
                    ModernUI.LOGGER.error(VIEW_MARKER, mParent.getClass().getSimpleName() +
                            " does not fully implement ViewParent", e);
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Reset the resolved layout direction. Layout direction will be resolved during a call to
     * {@link #onMeasure(int, int)}.
     *
     * @hide
     */
    void resetResolvedLayoutDirection() {
        // Reset the current resolved bits
        mPrivateFlags2 &= ~PFLAG2_LAYOUT_DIRECTION_RESOLVED_MASK;
    }

    /**
     * @return true if the layout direction is inherited.
     * @hide
     */
    public final boolean isLayoutDirectionInherited() {
        return (getRawLayoutDirection() == LAYOUT_DIRECTION_INHERIT);
    }

    /**
     * @return true if layout direction has been resolved.
     */
    public final boolean isLayoutDirectionResolved() {
        return (mPrivateFlags2 & PFLAG2_LAYOUT_DIRECTION_RESOLVED) == PFLAG2_LAYOUT_DIRECTION_RESOLVED;
    }

    /**
     * Return the value specifying the text direction or policy that was set with
     * {@link #setTextDirection(int)}.
     *
     * @return the defined text direction. It can be one of:
     * <p>
     * {@link #TEXT_DIRECTION_INHERIT},
     * {@link #TEXT_DIRECTION_FIRST_STRONG},
     * {@link #TEXT_DIRECTION_ANY_RTL},
     * {@link #TEXT_DIRECTION_LTR},
     * {@link #TEXT_DIRECTION_RTL},
     * {@link #TEXT_DIRECTION_LOCALE},
     * {@link #TEXT_DIRECTION_FIRST_STRONG_LTR},
     * {@link #TEXT_DIRECTION_FIRST_STRONG_RTL}
     * @hide
     */
    public final int getRawTextDirection() {
        return (mPrivateFlags2 & PFLAG2_TEXT_DIRECTION_MASK) >> PFLAG2_TEXT_DIRECTION_MASK_SHIFT;
    }

    /**
     * Set the text direction.
     * <p>
     * Should be one of:
     * <p>
     * {@link #TEXT_DIRECTION_INHERIT},
     * {@link #TEXT_DIRECTION_FIRST_STRONG},
     * {@link #TEXT_DIRECTION_ANY_RTL},
     * {@link #TEXT_DIRECTION_LTR},
     * {@link #TEXT_DIRECTION_RTL},
     * {@link #TEXT_DIRECTION_LOCALE}
     * {@link #TEXT_DIRECTION_FIRST_STRONG_LTR},
     * {@link #TEXT_DIRECTION_FIRST_STRONG_RTL},
     * <p>
     * Resolution will be done if the value is set to TEXT_DIRECTION_INHERIT. The resolution
     * proceeds up the parent chain of the view to get the value. If there is no parent, then it
     * will
     * return the default {@link #TEXT_DIRECTION_FIRST_STRONG}.
     *
     * @param textDirection the direction to set.
     */
    public void setTextDirection(int textDirection) {
        if (getRawTextDirection() != textDirection) {
            // Reset the current text direction and the resolved one
            mPrivateFlags2 &= ~PFLAG2_TEXT_DIRECTION_MASK;
            resetResolvedTextDirection();
            // Set the new text direction
            mPrivateFlags2 |= ((textDirection << PFLAG2_TEXT_DIRECTION_MASK_SHIFT) & PFLAG2_TEXT_DIRECTION_MASK);
            // Do resolution
            resolveTextDirection();
            // Notify change
            onRtlPropertiesChanged(getLayoutDirection());
            // Refresh
            requestLayout();
            invalidate();
        }
    }

    /**
     * Return the resolved text direction.
     *
     * @return the resolved text direction. Returns one of:
     * <p>
     * {@link #TEXT_DIRECTION_FIRST_STRONG},
     * {@link #TEXT_DIRECTION_ANY_RTL},
     * {@link #TEXT_DIRECTION_LTR},
     * {@link #TEXT_DIRECTION_RTL},
     * {@link #TEXT_DIRECTION_LOCALE},
     * {@link #TEXT_DIRECTION_FIRST_STRONG_LTR},
     * {@link #TEXT_DIRECTION_FIRST_STRONG_RTL}
     */
    public final int getTextDirection() {
        return (mPrivateFlags2 & PFLAG2_TEXT_DIRECTION_RESOLVED_MASK) >> PFLAG2_TEXT_DIRECTION_RESOLVED_MASK_SHIFT;
    }

    /**
     * Resolve the text direction.
     *
     * @return true if resolution has been done, false otherwise.
     * @hide
     */
    public boolean resolveTextDirection() {
        // Reset any previous text direction resolution
        mPrivateFlags2 &= ~(PFLAG2_TEXT_DIRECTION_RESOLVED | PFLAG2_TEXT_DIRECTION_RESOLVED_MASK);

        if (ModernUI.get().hasRtlSupport()) {
            // Set resolved text direction flag depending on text direction flag
            final int textDirection = getRawTextDirection();
            switch (textDirection) {
                case TEXT_DIRECTION_INHERIT -> {
                    if (!canResolveTextDirection()) {
                        // We cannot do the resolution if there is no parent, so use the default one
                        mPrivateFlags2 |= PFLAG2_TEXT_DIRECTION_RESOLVED_DEFAULT;
                        // Resolution will need to happen again later
                        return false;
                    }

                    // Parent has not yet resolved, so we still return the default
                    try {
                        if (!mParent.isTextDirectionResolved()) {
                            mPrivateFlags2 |= PFLAG2_TEXT_DIRECTION_RESOLVED_DEFAULT;
                            // Resolution will need to happen again later
                            return false;
                        }
                    } catch (AbstractMethodError e) {
                        ModernUI.LOGGER.error(VIEW_MARKER, mParent.getClass().getSimpleName() +
                                " does not fully implement ViewParent", e);
                        mPrivateFlags2 |= PFLAG2_TEXT_DIRECTION_RESOLVED |
                                PFLAG2_TEXT_DIRECTION_RESOLVED_DEFAULT;
                        return true;
                    }

                    // Set current resolved direction to the same value as the parent's one
                    int parentResolvedDirection;
                    try {
                        parentResolvedDirection = mParent.getTextDirection();
                    } catch (AbstractMethodError e) {
                        ModernUI.LOGGER.error(VIEW_MARKER, mParent.getClass().getSimpleName() +
                                " does not fully implement ViewParent", e);
                        parentResolvedDirection = TEXT_DIRECTION_LTR;
                    }
                    switch (parentResolvedDirection) {
                        case TEXT_DIRECTION_FIRST_STRONG, TEXT_DIRECTION_ANY_RTL, TEXT_DIRECTION_LTR,
                                TEXT_DIRECTION_RTL, TEXT_DIRECTION_LOCALE, TEXT_DIRECTION_FIRST_STRONG_LTR,
                                TEXT_DIRECTION_FIRST_STRONG_RTL -> mPrivateFlags2 |=
                                (parentResolvedDirection << PFLAG2_TEXT_DIRECTION_RESOLVED_MASK_SHIFT);
                        default ->
                                // Default resolved direction is "first strong" heuristic
                                mPrivateFlags2 |= PFLAG2_TEXT_DIRECTION_RESOLVED_DEFAULT;
                    }
                }
                case TEXT_DIRECTION_FIRST_STRONG, TEXT_DIRECTION_ANY_RTL, TEXT_DIRECTION_LTR, TEXT_DIRECTION_RTL,
                        TEXT_DIRECTION_LOCALE, TEXT_DIRECTION_FIRST_STRONG_LTR, TEXT_DIRECTION_FIRST_STRONG_RTL ->
                        // Resolved direction is the same as text direction
                        mPrivateFlags2 |= (textDirection << PFLAG2_TEXT_DIRECTION_RESOLVED_MASK_SHIFT);
                default ->
                        // Default resolved direction is "first strong" heuristic
                        mPrivateFlags2 |= PFLAG2_TEXT_DIRECTION_RESOLVED_DEFAULT;
            }
        } else {
            // Default resolved direction is "first strong" heuristic
            mPrivateFlags2 |= PFLAG2_TEXT_DIRECTION_RESOLVED_DEFAULT;
        }

        // Set to resolved
        mPrivateFlags2 |= PFLAG2_TEXT_DIRECTION_RESOLVED;
        return true;
    }

    /**
     * Check if text direction resolution can be done.
     *
     * @return true if text direction resolution can be done otherwise return false.
     */
    public final boolean canResolveTextDirection() {
        if (isTextDirectionInherited()) {
            if (mParent != null) {
                try {
                    return mParent.canResolveTextDirection();
                } catch (AbstractMethodError e) {
                    ModernUI.LOGGER.error(VIEW_MARKER, mParent.getClass().getSimpleName() +
                            " does not fully implement ViewParent", e);
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Reset resolved text direction. Text direction will be resolved during a call to
     * {@link #onMeasure(int, int)}.
     *
     * @hide
     */
    void resetResolvedTextDirection() {
        // Reset any previous text direction resolution
        mPrivateFlags2 &= ~(PFLAG2_TEXT_DIRECTION_RESOLVED | PFLAG2_TEXT_DIRECTION_RESOLVED_MASK);
        // Set to default value
        mPrivateFlags2 |= PFLAG2_TEXT_DIRECTION_RESOLVED_DEFAULT;
    }

    /**
     * @return true if text direction is inherited.
     * @hide
     */
    public final boolean isTextDirectionInherited() {
        return (getRawTextDirection() == TEXT_DIRECTION_INHERIT);
    }

    /**
     * @return true if text direction is resolved.
     */
    public final boolean isTextDirectionResolved() {
        return (mPrivateFlags2 & PFLAG2_TEXT_DIRECTION_RESOLVED) == PFLAG2_TEXT_DIRECTION_RESOLVED;
    }

    /**
     * Return the value specifying the text alignment or policy that was set with
     * {@link #setTextAlignment(int)}.
     *
     * @return the defined text alignment. It can be one of:
     * <p>
     * {@link #TEXT_ALIGNMENT_INHERIT},
     * {@link #TEXT_ALIGNMENT_GRAVITY},
     * {@link #TEXT_ALIGNMENT_CENTER},
     * {@link #TEXT_ALIGNMENT_TEXT_START},
     * {@link #TEXT_ALIGNMENT_TEXT_END},
     * {@link #TEXT_ALIGNMENT_VIEW_START},
     * {@link #TEXT_ALIGNMENT_VIEW_END}
     * @hide
     */
    public final int getRawTextAlignment() {
        return (mPrivateFlags2 & PFLAG2_TEXT_ALIGNMENT_MASK) >> PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT;
    }

    /**
     * Set the text alignment.
     * <p>
     * Should be one of
     * <p>
     * {@link #TEXT_ALIGNMENT_INHERIT},
     * {@link #TEXT_ALIGNMENT_GRAVITY},
     * {@link #TEXT_ALIGNMENT_CENTER},
     * {@link #TEXT_ALIGNMENT_TEXT_START},
     * {@link #TEXT_ALIGNMENT_TEXT_END},
     * {@link #TEXT_ALIGNMENT_VIEW_START},
     * {@link #TEXT_ALIGNMENT_VIEW_END}
     * <p>
     * Resolution will be done if the value is set to TEXT_ALIGNMENT_INHERIT. The resolution
     * proceeds up the parent chain of the view to get the value. If there is no parent, then it
     * will return the default {@link #TEXT_ALIGNMENT_GRAVITY}.
     *
     * @param textAlignment The text alignment to set.
     */
    public void setTextAlignment(int textAlignment) {
        if (textAlignment != getRawTextAlignment()) {
            // Reset the current and resolved text alignment
            mPrivateFlags2 &= ~PFLAG2_TEXT_ALIGNMENT_MASK;
            resetResolvedTextAlignment();
            // Set the new text alignment
            mPrivateFlags2 |=
                    ((textAlignment << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT) & PFLAG2_TEXT_ALIGNMENT_MASK);
            // Do resolution
            resolveTextAlignment();
            // Notify change
            onRtlPropertiesChanged(getLayoutDirection());
            // Refresh
            requestLayout();
            invalidate();
        }
    }

    /**
     * Return the resolved text alignment.
     *
     * @return the resolved text alignment. Returns one of:
     * <p>
     * {@link #TEXT_ALIGNMENT_GRAVITY},
     * {@link #TEXT_ALIGNMENT_CENTER},
     * {@link #TEXT_ALIGNMENT_TEXT_START},
     * {@link #TEXT_ALIGNMENT_TEXT_END},
     * {@link #TEXT_ALIGNMENT_VIEW_START},
     * {@link #TEXT_ALIGNMENT_VIEW_END}
     */
    public final int getTextAlignment() {
        return (mPrivateFlags2 & PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK) >>
                PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK_SHIFT;
    }

    /**
     * Resolve the text alignment.
     *
     * @return true if resolution has been done, false otherwise.
     * @hide
     */
    public boolean resolveTextAlignment() {
        // Reset any previous text alignment resolution
        mPrivateFlags2 &= ~(PFLAG2_TEXT_ALIGNMENT_RESOLVED | PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK);

        if (ModernUI.get().hasRtlSupport()) {
            // Set resolved text alignment flag depending on text alignment flag
            final int textAlignment = getRawTextAlignment();
            switch (textAlignment) {
                case TEXT_ALIGNMENT_INHERIT -> {
                    // Check if we can resolve the text alignment
                    if (!canResolveTextAlignment()) {
                        // We cannot do the resolution if there is no parent so use the default
                        mPrivateFlags2 |= PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT;
                        // Resolution will need to happen again later
                        return false;
                    }

                    // Parent has not yet resolved, so we still return the default
                    try {
                        if (!mParent.isTextAlignmentResolved()) {
                            mPrivateFlags2 |= PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT;
                            // Resolution will need to happen again later
                            return false;
                        }
                    } catch (AbstractMethodError e) {
                        ModernUI.LOGGER.error(VIEW_MARKER, mParent.getClass().getSimpleName() +
                                " does not fully implement ViewParent", e);
                        mPrivateFlags2 |= PFLAG2_TEXT_ALIGNMENT_RESOLVED |
                                PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT;
                        return true;
                    }
                    int parentResolvedTextAlignment;
                    try {
                        parentResolvedTextAlignment = mParent.getTextAlignment();
                    } catch (AbstractMethodError e) {
                        ModernUI.LOGGER.error(VIEW_MARKER, mParent.getClass().getSimpleName() +
                                " does not fully implement ViewParent", e);
                        parentResolvedTextAlignment = TEXT_ALIGNMENT_GRAVITY;
                    }
                    switch (parentResolvedTextAlignment) {
                        case TEXT_ALIGNMENT_GRAVITY, TEXT_ALIGNMENT_TEXT_START, TEXT_ALIGNMENT_TEXT_END,
                                TEXT_ALIGNMENT_CENTER, TEXT_ALIGNMENT_VIEW_START, TEXT_ALIGNMENT_VIEW_END ->
                                // Resolved text alignment is the same as the parent resolved
                                // text alignment
                                mPrivateFlags2 |=
                                        (parentResolvedTextAlignment << PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK_SHIFT);
                        default ->
                                // Use default resolved text alignment
                                mPrivateFlags2 |= PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT;
                    }
                }
                case TEXT_ALIGNMENT_GRAVITY, TEXT_ALIGNMENT_TEXT_START, TEXT_ALIGNMENT_TEXT_END,
                        TEXT_ALIGNMENT_CENTER, TEXT_ALIGNMENT_VIEW_START, TEXT_ALIGNMENT_VIEW_END ->
                        // Resolved text alignment is the same as text alignment
                        mPrivateFlags2 |= (textAlignment << PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK_SHIFT);
                default ->
                        // Use default resolved text alignment
                        mPrivateFlags2 |= PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT;
            }
        } else {
            // Use default resolved text alignment
            mPrivateFlags2 |= PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT;
        }

        // Set the resolved
        mPrivateFlags2 |= PFLAG2_TEXT_ALIGNMENT_RESOLVED;
        return true;
    }

    /**
     * Check if text alignment resolution can be done.
     *
     * @return true if text alignment resolution can be done otherwise return false.
     */
    public final boolean canResolveTextAlignment() {
        if (isTextAlignmentInherited()) {
            if (mParent != null) {
                try {
                    return mParent.canResolveTextAlignment();
                } catch (AbstractMethodError e) {
                    ModernUI.LOGGER.error(VIEW_MARKER, mParent.getClass().getSimpleName() +
                            " does not fully implement ViewParent", e);
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Reset resolved text alignment. Text alignment will be resolved during a call to
     * {@link #onMeasure(int, int)}.
     *
     * @hide
     */
    void resetResolvedTextAlignment() {
        // Reset any previous text alignment resolution
        mPrivateFlags2 &= ~(PFLAG2_TEXT_ALIGNMENT_RESOLVED | PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK);
        // Set to default
        mPrivateFlags2 |= PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT;
    }

    /**
     * @return true if text alignment is inherited.
     * @hide
     */
    public final boolean isTextAlignmentInherited() {
        return (getRawTextAlignment() == TEXT_ALIGNMENT_INHERIT);
    }

    /**
     * @return true if text alignment is resolved.
     */
    public final boolean isTextAlignmentResolved() {
        return (mPrivateFlags2 & PFLAG2_TEXT_ALIGNMENT_RESOLVED) == PFLAG2_TEXT_ALIGNMENT_RESOLVED;
    }

    /**
     * Return if padding has been resolved
     *
     * @hide
     */
    boolean isPaddingResolved() {
        return (mPrivateFlags2 & PFLAG2_PADDING_RESOLVED) == PFLAG2_PADDING_RESOLVED;
    }

    /**
     * Resolves padding depending on layout direction, if applicable, and
     * recomputes internal padding values to adjust for scroll bars.
     *
     * @hide
     */
    public void resolvePadding() {
        final int resolvedLayoutDirection = getLayoutDirection();

        if (ModernUI.get().hasRtlSupport()) {
            // Post Jelly Bean MR1 case: we need to take the resolved layout direction into account.
            // If start / end padding are defined, they will be resolved (hence overriding) to
            // left / right or right / left depending on the resolved layout direction.
            // If start / end padding are not defined, use the left / right ones.
            if (mBackground != null && (!mLeftPaddingDefined || !mRightPaddingDefined)) {
                Rect padding = sThreadLocal.get();
                if (padding == null) {
                    padding = new Rect();
                    sThreadLocal.set(padding);
                }
                mBackground.getPadding(padding);
                if (!mLeftPaddingDefined) {
                    mUserPaddingLeftInitial = padding.left;
                }
                if (!mRightPaddingDefined) {
                    mUserPaddingRightInitial = padding.right;
                }
            }
            if (resolvedLayoutDirection == LAYOUT_DIRECTION_RTL) {
                if (mUserPaddingStart != UNDEFINED_PADDING) {
                    mUserPaddingRight = mUserPaddingStart;
                } else {
                    mUserPaddingRight = mUserPaddingRightInitial;
                }
                if (mUserPaddingEnd != UNDEFINED_PADDING) {
                    mUserPaddingLeft = mUserPaddingEnd;
                } else {
                    mUserPaddingLeft = mUserPaddingLeftInitial;
                }
            } else {
                if (mUserPaddingStart != UNDEFINED_PADDING) {
                    mUserPaddingLeft = mUserPaddingStart;
                } else {
                    mUserPaddingLeft = mUserPaddingLeftInitial;
                }
                if (mUserPaddingEnd != UNDEFINED_PADDING) {
                    mUserPaddingRight = mUserPaddingEnd;
                } else {
                    mUserPaddingRight = mUserPaddingRightInitial;
                }
            }

            mUserPaddingBottom = (mUserPaddingBottom >= 0) ? mUserPaddingBottom : mPaddingBottom;
        }

        internalSetPadding(mUserPaddingLeft, mPaddingTop, mUserPaddingRight, mUserPaddingBottom);
        onRtlPropertiesChanged(resolvedLayoutDirection);

        mPrivateFlags2 |= PFLAG2_PADDING_RESOLVED;
    }

    /**
     * Reset the resolved layout direction.
     * <p>
     * Used when we only want to reset *this* view's padding and not trigger overrides
     * in ViewGroup that reset children too.
     *
     * @hide
     */
    void resetResolvedPadding() {
        mPrivateFlags2 &= ~PFLAG2_PADDING_RESOLVED;
    }

    /// SECTION END - Direction, RTL \\\

    /**
     * <p>Return the offset of the widget's text baseline from the widget's top
     * boundary. If this widget does not support baseline alignment, this
     * method returns -1. </p>
     *
     * @return the offset of the baseline within the widget's bounds or -1
     * if baseline alignment is not supported
     */
    public int getBaseline() {
        return -1;
    }

    /**
     * Request layout if layout information changed.
     * This will schedule a layout pass of the view tree.
     */
    public void requestLayout() {
        boolean requestParent = (mPrivateFlags & PFLAG_FORCE_LAYOUT) == 0;

        mPrivateFlags |= PFLAG_FORCE_LAYOUT;

        if (requestParent && mParent != null) {
            mParent.requestLayout();
        }
    }

    /**
     * Add a mark to force this view to be laid out during the next
     * layout pass.
     */
    public void forceLayout() {
        mPrivateFlags |= PFLAG_FORCE_LAYOUT;
    }

    /**
     * Call this to force a view to update its drawable state. This will cause
     * drawableStateChanged to be called on this view. Views that are interested
     * in the new state should call getDrawableState.
     *
     * @see #drawableStateChanged
     * @see #getDrawableState
     */
    public void refreshDrawableState() {
        mPrivateFlags |= PFLAG_DRAWABLE_STATE_DIRTY;
        drawableStateChanged();

        ViewParent parent = mParent;
        if (parent != null) {
            parent.childDrawableStateChanged(this);
        }
    }


    /**
     * Resolve the Drawables depending on the layout direction. This is implicitly supposing
     * that the View directionality can and will be resolved before its Drawables.
     * <p>
     * Will call {@link View#onResolveDrawables} when resolution is done.
     *
     * @hide
     */
    protected void resolveDrawables() {
        // Drawables resolution may need to happen before resolving the layout direction (which is
        // done only during the measure() call).
        // If the layout direction is not resolved yet, we cannot resolve the Drawables except in
        // one case: when the raw layout direction has not been defined as LAYOUT_DIRECTION_INHERIT.
        // So, if the raw layout direction is LAYOUT_DIRECTION_LTR or LAYOUT_DIRECTION_RTL or
        // LAYOUT_DIRECTION_LOCALE, we can "cheat" and we don't need to wait for the layout
        // direction to be resolved as its resolved value will be the same as its raw value.
        if (!isLayoutDirectionResolved() &&
                getRawLayoutDirection() == View.LAYOUT_DIRECTION_INHERIT) {
            return;
        }

        final int layoutDirection = isLayoutDirectionResolved() ?
                getLayoutDirection() : getRawLayoutDirection();

        if (mBackground != null) {
            mBackground.setLayoutDirection(layoutDirection);
        }
        /*if (mForegroundInfo != null && mForegroundInfo.mDrawable != null) {
            mForegroundInfo.mDrawable.setLayoutDirection(layoutDirection);
        }
        if (mDefaultFocusHighlight != null) {
            mDefaultFocusHighlight.setLayoutDirection(layoutDirection);
        }*/
        mPrivateFlags2 |= PFLAG2_DRAWABLE_RESOLVED;
        onResolveDrawables(layoutDirection);
    }

    boolean areDrawablesResolved() {
        return (mPrivateFlags2 & PFLAG2_DRAWABLE_RESOLVED) == PFLAG2_DRAWABLE_RESOLVED;
    }

    /**
     * Called when layout direction has been resolved.
     * <p>
     * The default implementation does nothing.
     *
     * @param layoutDirection The resolved layout direction.
     * @hide
     * @see #LAYOUT_DIRECTION_LTR
     * @see #LAYOUT_DIRECTION_RTL
     */
    public void onResolveDrawables(int layoutDirection) {
    }

    /**
     * @hide
     */
    protected void resetResolvedDrawables() {
        resetResolvedDrawablesInternal();
    }

    void resetResolvedDrawablesInternal() {
        mPrivateFlags2 &= ~PFLAG2_DRAWABLE_RESOLVED;
    }

    /**
     * Sets the padding. The view may add on the space required to display
     * the scrollbars, depending on the style and visibility of the scrollbars.
     * So the values returned from {@link #getPaddingLeft}, {@link #getPaddingTop},
     * {@link #getPaddingRight} and {@link #getPaddingBottom} may be different
     * from the values set in this call.
     *
     * @param left   the left padding in pixels
     * @param top    the top padding in pixels
     * @param right  the right padding in pixels
     * @param bottom the bottom padding in pixels
     */
    public void setPadding(int left, int top, int right, int bottom) {
        resetResolvedPadding();

        mUserPaddingStart = UNDEFINED_PADDING;
        mUserPaddingEnd = UNDEFINED_PADDING;

        mUserPaddingLeftInitial = left;
        mUserPaddingRightInitial = right;

        mLeftPaddingDefined = true;
        mRightPaddingDefined = true;

        internalSetPadding(left, top, right, bottom);
    }

    /**
     * @hide
     */
    protected void internalSetPadding(int left, int top, int right, int bottom) {
        mUserPaddingLeft = left;
        mUserPaddingRight = right;
        mUserPaddingBottom = bottom;

        final int viewFlags = mViewFlags;
        boolean changed = false;

        // Common case is there are no scroll bars.
        /*if ((viewFlags & (SCROLLBARS_VERTICAL|SCROLLBARS_HORIZONTAL)) != 0) {
            if ((viewFlags & SCROLLBARS_VERTICAL) != 0) {
                final int offset = (viewFlags & SCROLLBARS_INSET_MASK) == 0
                        ? 0 : getVerticalScrollbarWidth();
                switch (mVerticalScrollbarPosition) {
                    case SCROLLBAR_POSITION_DEFAULT:
                        if (isLayoutRtl()) {
                            left += offset;
                        } else {
                            right += offset;
                        }
                        break;
                    case SCROLLBAR_POSITION_RIGHT:
                        right += offset;
                        break;
                    case SCROLLBAR_POSITION_LEFT:
                        left += offset;
                        break;
                }
            }
            if ((viewFlags & SCROLLBARS_HORIZONTAL) != 0) {
                bottom += (viewFlags & SCROLLBARS_INSET_MASK) == 0
                        ? 0 : getHorizontalScrollbarHeight();
            }
        }*/

        if (mPaddingLeft != left) {
            changed = true;
            mPaddingLeft = left;
        }
        if (mPaddingTop != top) {
            changed = true;
            mPaddingTop = top;
        }
        if (mPaddingRight != right) {
            changed = true;
            mPaddingRight = right;
        }
        if (mPaddingBottom != bottom) {
            changed = true;
            mPaddingBottom = bottom;
        }

        if (changed) {
            requestLayout();
        }
    }

    /**
     * Sets the relative padding. The view may add on the space required to display
     * the scrollbars, depending on the style and visibility of the scrollbars.
     * So the values returned from {@link #getPaddingStart}, {@link #getPaddingTop},
     * {@link #getPaddingEnd} and {@link #getPaddingBottom} may be different
     * from the values set in this call.
     *
     * @param start  the start padding in pixels
     * @param top    the top padding in pixels
     * @param end    the end padding in pixels
     * @param bottom the bottom padding in pixels
     */
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        resetResolvedPadding();

        mUserPaddingStart = start;
        mUserPaddingEnd = end;
        mLeftPaddingDefined = true;
        mRightPaddingDefined = true;

        if (isLayoutRtl()) {
            mUserPaddingLeftInitial = end;
            mUserPaddingRightInitial = start;
            internalSetPadding(end, top, start, bottom);
        } else {
            mUserPaddingLeftInitial = start;
            mUserPaddingRightInitial = end;
            internalSetPadding(start, top, end, bottom);
        }
    }

    /**
     * Returns the top padding of this view.
     *
     * @return the top padding in pixels
     */
    public int getPaddingTop() {
        return mPaddingTop;
    }

    /**
     * Returns the bottom padding of this view. If there are inset and enabled
     * scrollbars, this value may include the space required to display the
     * scrollbars as well.
     *
     * @return the bottom padding in pixels
     */
    public int getPaddingBottom() {
        return mPaddingBottom;
    }

    /**
     * Returns the left padding of this view. If there are inset and enabled
     * scrollbars, this value may include the space required to display the
     * scrollbars as well.
     *
     * @return the left padding in pixels
     */
    public int getPaddingLeft() {
        if (!isPaddingResolved()) {
            resolvePadding();
        }
        return mPaddingLeft;
    }

    /**
     * Returns the start padding of this view depending on its resolved layout direction.
     * If there are inset and enabled scrollbars, this value may include the space
     * required to display the scrollbars as well.
     *
     * @return the start padding in pixels
     */
    public int getPaddingStart() {
        if (!isPaddingResolved()) {
            resolvePadding();
        }
        return (getLayoutDirection() == LAYOUT_DIRECTION_RTL) ?
                mPaddingRight : mPaddingLeft;
    }

    /**
     * Returns the right padding of this view. If there are inset and enabled
     * scrollbars, this value may include the space required to display the
     * scrollbars as well.
     *
     * @return the right padding in pixels
     */
    public int getPaddingRight() {
        if (!isPaddingResolved()) {
            resolvePadding();
        }
        return mPaddingRight;
    }

    /**
     * Returns the end padding of this view depending on its resolved layout direction.
     * If there are inset and enabled scrollbars, this value may include the space
     * required to display the scrollbars as well.
     *
     * @return the end padding in pixels
     */
    public int getPaddingEnd() {
        if (!isPaddingResolved()) {
            resolvePadding();
        }
        return (getLayoutDirection() == LAYOUT_DIRECTION_RTL) ?
                mPaddingLeft : mPaddingRight;
    }

    /**
     * Return if the padding has been set through relative values
     * {@link #setPaddingRelative(int, int, int, int)} or through
     *
     * @return true if the padding is relative or false if it is not.
     */
    public boolean isPaddingRelative() {
        return (mUserPaddingStart != UNDEFINED_PADDING || mUserPaddingEnd != UNDEFINED_PADDING);
    }

    /**
     * If your view subclass is displaying its own Drawable objects, it should
     * override this function and return true for any Drawable it is
     * displaying.  This allows animations for those drawables to be
     * scheduled.
     *
     * <p>Be sure to call through to the super class when overriding this
     * function.
     *
     * @param drawable The Drawable to verify.  Return true if it is one you are
     *                 displaying, else return the result of calling through to the
     *                 super class.
     * @return boolean If true than the Drawable is being displayed in the
     * view; else false and it is not allowed to animate.
     * @see #drawableStateChanged()
     */
    @CallSuper
    protected boolean verifyDrawable(@Nonnull Drawable drawable) {
        // Avoid verifying the scroll bar drawable so that we don't end up in
        // an invalidation loop. This effectively prevents the scroll bar
        // drawable from triggering invalidations and scheduling runnables.
        return drawable == mBackground;
    }

    /**
     * This function is called whenever the state of the view changes in such
     * a way that it impacts the state of drawables being shown.
     * <p>
     * If the View has a StateListAnimator, it will also be called to run necessary state
     * change animations.
     * <p>
     * Be sure to call through to the superclass when overriding this function.
     *
     * @see Drawable#setState(int[])
     */
    protected void drawableStateChanged() {

    }

    /**
     * Return an array of resource IDs of the drawable states representing the
     * current state of the view.
     *
     * @return The current drawable state
     * @see Drawable#setState(int[])
     * @see #drawableStateChanged()
     * @see #onCreateDrawableState(int)
     */
    public final int[] getDrawableState() {
        if (mDrawableState == null || (mPrivateFlags & PFLAG_DRAWABLE_STATE_DIRTY) != 0) {
            mDrawableState = onCreateDrawableState(0);
            mPrivateFlags &= ~PFLAG_DRAWABLE_STATE_DIRTY;
        }
        return mDrawableState;
    }

    /**
     * Generate the new {@link Drawable} state for
     * this view. This is called by the view
     * system when the cached Drawable state is determined to be invalid.  To
     * retrieve the current state, you should use {@link #getDrawableState}.
     *
     * @param extraSpace if non-zero, this is the number of extra entries you
     *                   would like in the returned array in which you can place your own
     *                   states.
     * @return an array holding the current {@link Drawable} state of
     * the view.
     * @see #mergeDrawableStates(int[], int[])
     */
    protected int[] onCreateDrawableState(int extraSpace) {
        return new int[0];
    }

    /**
     * Set the background to a given Drawable, or remove the background. If the
     * background has padding, this View's padding is set to the background's
     * padding. However, when a background is removed, this View's padding isn't
     * touched.
     *
     * @param background The Drawable to use as the background, or null to remove the
     *                   background
     */
    public void setBackground(@Nullable Drawable background) {
        if (background == mBackground) {
            return;
        }
        mBackground = background;
        if (background != null) {
            background.setCallback(this);
        }
    }

    /**
     * Gets the background drawable
     *
     * @return The drawable used as the background for this view, if any.
     * @see #setBackground(Drawable)
     */
    public Drawable getBackground() {
        return mBackground;
    }

    /**
     * Returns the drawable used as the foreground of this View. The
     * foreground drawable, if non-null, is always drawn on top of the view's content.
     *
     * @return a Drawable or null if no foreground was set
     * @see #onDrawForeground(Canvas)
     */
    public Drawable getForeground() {
        return null;
    }

    /**
     * Magic bit used to support features of framework-internal window decor implementation details.
     * This used to live exclusively in FrameLayout.
     *
     * @return true if the foreground should draw inside the padding region or false
     * if it should draw inset by the view's padding
     * @hide internal use only; only used by FrameLayout and internal screen layouts.
     */
    public boolean isForegroundInsidePadding() {
        return true;
    }

    /**
     * Finds the topmost view in the current view hierarchy.
     *
     * @return the topmost view containing this view
     */
    public final View getRootView() {
        if (mAttachInfo != null) {
            View v = mAttachInfo.mRootView;
            if (v != null) {
                return v;
            }
        }

        View v = this;

        while (v.mParent instanceof View) {
            v = (View) v.mParent;
        }

        return v;
    }

    /**
     * Computes the coordinates of this view in its window.
     *
     * @param out the point in which to hold the coordinates
     */
    public void getLocationInWindow(@Nonnull Point out) {
        if (mAttachInfo == null) {
            out.set(0, 0);
            return;
        }

        PointF p = mAttachInfo.mTmpPointF;
        p.set(0, 0);

        if (!hasIdentityMatrix()) {
            getMatrix().transform(p);
        }

        p.offset(mLeft, mTop);

        ViewParent parent = mParent;
        while (parent instanceof View) {
            View view = (View) parent;
            p.offset(-view.mScrollX, -view.mScrollY);

            if (!view.hasIdentityMatrix()) {
                view.getMatrix().transform(p);
            }

            p.offset(view.mLeft, view.mTop);
            parent = view.mParent;
        }

        p.round(out);
    }

    /**
     * Finds the first descendant view with the given ID, the view itself if
     * the ID matches {@link #getId()}, or {@code null} if the ID is invalid
     * (< 0) or there is no matching view in the hierarchy.
     *
     * @param id the id to search for
     * @return a view with given id if found, or {@code null} otherwise
     */
    @Nullable
    public final <T extends View> T findViewById(int id) {
        if (id == NO_ID) {
            return null;
        }
        return findViewTraversal(id);
    }

    /**
     * Finds the first descendant view with the given ID, the view itself if the ID
     * matches {@link #getId()}, or results in an error.
     *
     * @param id the ID to search for
     * @return a view with given ID
     * @throws IllegalArgumentException the ID is invalid or there is no matching view
     *                                  in the hierarchy
     * @see View#findViewById(int)
     */
    @Nonnull
    public final <T extends View> T getViewById(int id) {
        T view = findViewById(id);
        if (view == null) {
            throw new IllegalArgumentException("ID does not reference a View inside this View");
        }
        return view;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    <T extends View> T findViewTraversal(int id) {
        if (id == mId) {
            return (T) this;
        }
        return null;
    }

    /*boolean onCursorPosEvent(LinkedList<View> route, double x, double y) {
        if ((mViewFlags & ENABLED_MASK) == DISABLED) {
            return false;
        }
        if (x >= mLeft && x < mRight && y >= mTop && y < mBottom) {
            route.add(this);
            return true;
        }
        return false;
    }*/

    /**
     * Called by a parent to request that a child update its values for mScrollX
     * and mScrollY if necessary. This will typically be done if the child is
     * animating a scroll using a Scroller.
     */
    public void computeScroll() {
    }

    /**
     * Starts a drag and drop operation. This method passes a {@link DragShadow} object to
     * the window system. The system calls {@link DragShadow#onDrawShadow(Canvas)}
     * to draw the drag shadow itself at proper level.
     * <p>
     * Once the system has the drag shadow, it begins the drag and drop operation by sending
     * drag events to all the View objects in all windows that are currently visible. It does
     * this either by calling the View object's drag listener (an implementation of
     * {@link View.OnDragListener#onDrag(View, DragEvent)} or by calling the
     * View object's {@link #onDragEvent(DragEvent)} method.
     * Both are passed a {@link DragEvent} object that has a
     * {@link DragEvent#getAction()} value of {@link DragEvent#ACTION_DRAG_STARTED}.
     *
     * @param localState The data to be transferred by the drag and drop operation. It can be in any form,
     *                   which can not only store the required data, but also let the view identify whether
     *                   it can accept the followed DragEvent.
     * @param shadow     The shadow object to draw the shadow, {@code null} will generate a default shadow.
     * @param flags      Flags, 0 for no flags.
     * @return {@code true} if operation successfully started, {@code false} means the system was
     * unable to start the operation because of another ongoing operation or some other reasons.
     */
    public final boolean startDragAndDrop(@Nullable Object localState, @Nullable DragShadow shadow, int flags) {
        if (mAttachInfo == null) {
            ModernUI.LOGGER.error(VIEW_MARKER, "startDragAndDrop called out of a window");
            return false;
        }
        return mAttachInfo.mViewRootImpl.startDragAndDrop(this, localState, shadow, flags);
    }

    /**
     * Handle primitive drag event of this view
     *
     * @param event the event to be handled
     * @return {@code true} if the event was consumed by the view, {@code false} otherwise
     */
    //TODO
    public boolean onDragEvent(DragEvent event) {
        return true;
    }

    /**
     * Dispatch a pointer event.
     * <p>
     * Dispatches touch related pointer events to {@link #onTouchEvent(MotionEvent)} and all
     * other events to {@link #onGenericMotionEvent(MotionEvent)}.  This separation of concerns
     * reinforces the invariant that {@link #onTouchEvent(MotionEvent)} is really about touches
     * and should not be expected to handle other pointing device features.
     * </p>
     *
     * @param event the motion event to be dispatched.
     * @return true if the event was handled by the view, false otherwise.
     */
    public final boolean dispatchPointerEvent(@Nonnull MotionEvent event) {
        if (event.isTouchEvent()) {
            return dispatchTouchEvent(event);
        } else {
            return dispatchGenericMotionEvent(event);
        }
    }

    /**
     * Dispatch a generic motion event.
     * <p>
     * Generic motion events with source class pointer are delivered to the view under the
     * pointer.  All other generic motion events are delivered to the focused view.
     * Hover events are handled specially and are delivered to {@link #onHoverEvent(MotionEvent)}.
     * <p>
     * Generally, this method should not be overridden.
     *
     * @param event the event to be dispatched
     * @return {@code true} if the event was consumed by the view, {@code false} otherwise
     */
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        final int action = event.getAction();
        if (action == MotionEvent.ACTION_HOVER_ENTER
                || action == MotionEvent.ACTION_HOVER_MOVE
                || action == MotionEvent.ACTION_HOVER_EXIT) {
            if (dispatchHoverEvent(event)) {
                return true;
            }
        }
        if (dispatchGenericPointerEvent(event)) {
            return true;
        }
        return dispatchGenericMotionEventInternal(event);
    }

    private boolean dispatchGenericMotionEventInternal(MotionEvent event) {
        if (onGenericMotionEvent(event)) {
            return true;
        }
        if ((mViewFlags & ENABLED_MASK) == DISABLED) {
            return false;
        }
        final int action = event.getAction();
        return false;
    }

    /**
     * Dispatch a hover event.
     * <p>
     * Do not call this method directly.
     * Call {@link #dispatchGenericMotionEvent(MotionEvent)} instead.
     * </p>
     *
     * @param event The motion event to be dispatched.
     * @return True if the event was handled by the view, false otherwise.
     */
    protected boolean dispatchHoverEvent(MotionEvent event) {
        return onHoverEvent(event);
    }

    /**
     * Dispatch a generic motion event to the view under the first pointer.
     * <p>
     * Do not call this method directly.
     * Call {@link #dispatchGenericMotionEvent(MotionEvent)} instead.
     * </p>
     *
     * @param event The motion event to be dispatched.
     * @return True if the event was handled by the view, false otherwise.
     */
    protected boolean dispatchGenericPointerEvent(MotionEvent event) {
        return false;
    }

    /**
     * Implement this method to handle generic motion events.
     * <p>
     * Implementations of this method should check if this view ENABLED and CLICKABLE.
     *
     * @param event the generic motion event being processed.
     * @return {@code true} if the event was consumed by the view, {@code false} otherwise
     */
    public boolean onGenericMotionEvent(MotionEvent event) {
        /*final double mouseX = event.getX();
        final double mouseY = event.getY();
        final int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                final boolean prevHovered = (mPrivateFlags & PFLAG_HOVERED) != 0;
                if (mouseX >= mLeft && mouseX < mRight && mouseY >= mTop && mouseY < mBottom) {
                    if (!prevHovered) {
                        mPrivateFlags |= PFLAG_HOVERED;
                        onMouseHoverEnter(mouseX, mouseY);
                    }
                    onMouseHoverMoved(mouseX, mouseY);
                    dispatchGenericMotionEvent(event);
                    return true;
                } else {
                    if (prevHovered) {
                        mPrivateFlags &= ~PFLAG_HOVERED;
                        onMouseHoverExit();
                    }

                }
                return false;
            case MotionEvent.ACTION_PRESS:
                if ((mPrivateFlags & PFLAG_HOVERED) != 0) {
                    if (dispatchGenericMotionEvent(event)) {
                        return true;
                    }
                    event.pressMap.putIfAbsent(event.mActionButton, this);
                    return onMousePressed(mouseX, mouseY, event.mActionButton);
                }
                return false;
            case MotionEvent.ACTION_RELEASE:
                if ((mPrivateFlags & PFLAG_HOVERED) != 0) {
                    if (dispatchGenericMotionEvent(event)) {
                        return true;
                    }
                    boolean handled;
                    handled = onMouseReleased(mouseX, mouseY, event.mActionButton);
                    if (event.pressMap.remove(event.mActionButton) == this) {
                        handled = onMouseClicked(mouseX, mouseY, event.mActionButton);
                        event.clicked = this;
                    }
                    return handled;
                }
                return false;
            case MotionEvent.ACTION_DOUBLE_CLICK:
                if ((mPrivateFlags & PFLAG_HOVERED) != 0) {
                    *//*if (dispatchMouseEvent(event)) {
                        return true;
                    }*//*
                    return onMouseDoubleClicked(mouseX, mouseY);
                }
                return false;
            case MotionEvent.ACTION_SCROLL:
                if ((mPrivateFlags & PFLAG_HOVERED) != 0) {
                    if (dispatchGenericMotionEvent(event)) {
                        return true;
                    }
                    return onMouseScrolled(mouseX, mouseY, event.scrollDelta);
                }
                return false;
        }*/
        return false;
    }

    /**
     * Implement this method to handle hover events.
     * <p>
     * This method is called whenever a pointer is hovering into, over, or out of the
     * bounds of a view and the view is not currently being touched.
     * Hover events are represented as pointer events with action
     * {@link MotionEvent#ACTION_HOVER_ENTER}, {@link MotionEvent#ACTION_HOVER_MOVE},
     * or {@link MotionEvent#ACTION_HOVER_EXIT}.
     * </p>
     * <ul>
     * <li>The view receives a hover event with action {@link MotionEvent#ACTION_HOVER_ENTER}
     * when the pointer enters the bounds of the view.</li>
     * <li>The view receives a hover event with action {@link MotionEvent#ACTION_HOVER_MOVE}
     * when the pointer has already entered the bounds of the view and has moved.</li>
     * <li>The view receives a hover event with action {@link MotionEvent#ACTION_HOVER_EXIT}
     * when the pointer has exited the bounds of the view or when the pointer is
     * about to go down due to a button click, tap, or similar user action that
     * causes the view to be touched.</li>
     * </ul>
     * <p>
     * The view should implement this method to return true to indicate that it is
     * handling the hover event, such as by changing its drawable state.
     * </p><p>
     * The default implementation calls {@link #setHovered} to update the hovered state
     * of the view when a hover enter or hover exit event is received, if the view
     * is enabled and is clickable.  The default implementation also sends hover
     * accessibility events.
     * </p>
     *
     * @param event The motion event that describes the hover.
     * @return True if the view handled the hover event.
     * @see #isHovered
     * @see #setHovered
     * @see #onHoverChanged
     */
    public boolean onHoverEvent(MotionEvent event) {
        final int action = event.getAction();
        // If we consider ourself hoverable, or if we we're already hovered,
        // handle changing state in response to ENTER and EXIT events.
        if (isHoverable() || isHovered()) {
            switch (action) {
                case MotionEvent.ACTION_HOVER_ENTER:
                    setHovered(true);
                    break;
                case MotionEvent.ACTION_HOVER_EXIT:
                    setHovered(false);
                    break;
            }

            // Dispatch the event to onGenericMotionEvent before returning true.
            // This is to provide compatibility with existing applications that
            // handled HOVER_MOVE events in onGenericMotionEvent and that would
            // break because of the new default handling for hoverable views
            // in onHoverEvent.
            // Note that onGenericMotionEvent will be called by default when
            // onHoverEvent returns false (refer to dispatchGenericMotionEvent).
            dispatchGenericMotionEventInternal(event);
            // The event was already handled by calling setHovered(), so always
            // return true.
            return true;
        }
        return false;
    }

    /**
     * Returns true if the view should handle {@link #onHoverEvent}
     * by calling {@link #setHovered} to change its hovered state.
     *
     * @return True if the view is hoverable.
     */
    private boolean isHoverable() {
        final int viewFlags = mViewFlags;
        if ((viewFlags & ENABLED_MASK) == DISABLED) {
            return false;
        }
        return (viewFlags & CLICKABLE) == CLICKABLE;
    }

    /**
     * Returns true if the view is currently hovered.
     *
     * @return True if the view is currently hovered.
     * @see #setHovered
     * @see #onHoverChanged
     */
    public boolean isHovered() {
        return (mPrivateFlags & PFLAG_HOVERED) != 0;
    }

    /**
     * Sets whether the view is currently hovered.
     * <p>
     * Calling this method also changes the drawable state of the view.  This
     * enables the view to react to hover by using different drawable resources
     * to change its appearance.
     * </p><p>
     * The {@link #onHoverChanged} method is called when the hovered state changes.
     * </p>
     *
     * @param hovered True if the view is hovered.
     * @see #isHovered
     * @see #onHoverChanged
     */
    public void setHovered(boolean hovered) {
        if (hovered) {
            if ((mPrivateFlags & PFLAG_HOVERED) == 0) {
                mPrivateFlags |= PFLAG_HOVERED;
                refreshDrawableState();
                onHoverChanged(true);
            }
        } else {
            if ((mPrivateFlags & PFLAG_HOVERED) != 0) {
                mPrivateFlags &= ~PFLAG_HOVERED;
                refreshDrawableState();
                onHoverChanged(false);
            }
        }
    }

    /**
     * Implement this method to handle hover state changes.
     * <p>
     * This method is called whenever the hover state changes as a result of a
     * call to {@link #setHovered}.
     * </p>
     *
     * @param hovered The current hover state, as returned by {@link #isHovered}.
     * @see #isHovered
     * @see #setHovered
     */
    public void onHoverChanged(boolean hovered) {

    }

    @Nonnull
    ListenerInfo getListenerInfo() {
        if (mListenerInfo == null) {
            mListenerInfo = new ListenerInfo();
        }
        return mListenerInfo;
    }

    /**
     * Register a callback to be invoked when this view is clicked. If this view is not
     * clickable, it becomes clickable.
     *
     * @param l The callback that will run
     * @see #setClickable(boolean)
     */
    public void setOnClickListener(@Nullable OnClickListener l) {
        if (!isClickable()) {
            setClickable(true);
        }
        getListenerInfo().mOnClickListener = l;
    }

    /**
     * Return whether this view has an attached OnClickListener.  Returns
     * true if there is a listener, false if there is none.
     */
    public boolean hasOnClickListeners() {
        ListenerInfo li = mListenerInfo;
        return (li != null && li.mOnClickListener != null);
    }

    /*
     * Internal method. Check if mouse hover this view.
     *
     * @param mouseX relative mouse X pos
     * @param mouseY relative mouse Y pos
     * @return return {@code true} if certain view hovered
     */
    /*@Deprecated
    final boolean updateMouseHover(double mouseX, double mouseY) {
        final boolean prevHovered = (privateFlags & PFLAG_HOVERED) != 0;
        if (mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom) {
            if (handleScrollBarsHover(mouseX, mouseY)) {
                UIManager.getInstance().setHovered(this);
                return true;
            }
            if (!prevHovered) {
                privateFlags |= PFLAG_HOVERED;
                onMouseHoverEnter(mouseX, mouseY);
            }
            onMouseHoverMoved(mouseX, mouseY);
            if (!dispatchUpdateMouseHover(mouseX, mouseY)) {
                UIManager.getInstance().setHovered(this);
            }
            return true;
        } else {
            if (prevHovered) {
                privateFlags &= ~PFLAG_HOVERED;
                onMouseHoverExit();
            }
        }
        return false;
    }

    private boolean handleScrollBarsHover(double mouseX, double mouseY) {
        ScrollBar scrollBar = verticalScrollBar;
        if (scrollBar != null && scrollBar.updateMouseHover(mouseX, mouseY)) {
            return true;
        }
        scrollBar = horizontalScrollBar;
        return scrollBar != null && scrollBar.updateMouseHover(mouseX, mouseY);
    }*/

    /*
     * Internal method. Dispatch events to child views if present,
     * check if mouse hovered a child view.
     *
     * @param mouseX relative mouse X pos
     * @param mouseY relative mouse Y pos
     * @return return {@code true} if certain child view hovered
     */
    /*boolean dispatchUpdateMouseHover(double mouseX, double mouseY) {
        return false;
    }*/

    /*void ensureMouseHoverExit() {
        if ((mPrivateFlags & PFLAG_HOVERED) != 0) {
            mPrivateFlags &= ~PFLAG_HOVERED;
            onMouseHoverExit();
        }
    }*/

    /**
     * Returns whether this view can receive pointer events.
     *
     * @return {@code true} if this view can receive pointer events.
     */
    protected boolean canReceivePointerEvents() {
        return (mViewFlags & VISIBILITY_MASK) == VISIBLE;
    }

    /**
     * Pass the touch screen motion event down to the target view, or this view if
     * it is the target.
     *
     * @param event The motion event to be dispatched.
     * @return {@code true} if the event was handled by the view, {@code false} otherwise
     */
    public boolean dispatchTouchEvent(@Nonnull MotionEvent event) {
        return onTouchEvent(event);
    }

    /**
     * Implement this method to handle touch screen motion events.
     * <p>
     * If this method is used to detect click actions, it is recommended that
     * the actions be performed by implementing and calling
     * {@link #performClick()}. This will ensure consistent system behavior.
     *
     * @param event the touch event
     * @return {@code true} if the event was handled by the view, {@code false} otherwise
     */
    public boolean onTouchEvent(@Nonnull MotionEvent event) {
        final int action = event.getAction();
        final int viewFlags = mViewFlags;
        final boolean clickable = (viewFlags & CLICKABLE) == CLICKABLE
                || (viewFlags & CONTEXT_CLICKABLE) == CONTEXT_CLICKABLE;

        if ((viewFlags & ENABLED_MASK) == DISABLED) {
            if (action == MotionEvent.ACTION_UP && (mPrivateFlags & PFLAG_PRESSED) != 0) {
                setPressed(false);
            }
            // A disabled view that is clickable still consumes the touch
            // events, it just doesn't respond to them.
            return clickable;
        }

        if (clickable) {
            switch (action) {
                case MotionEvent.ACTION_UP:
                    if ((mPrivateFlags & PFLAG_PRESSED) != 0) {
                        performClick();
                        setPressed(false);
                    }
                    break;
                case MotionEvent.ACTION_DOWN:
                    setPressed(true);
                    break;

                case MotionEvent.ACTION_CANCEL:
                    setPressed(false);
                    break;
            }

            return true;
        }

        return false;
    }

    boolean isOnScrollbarThumb(float x, float y) {
        return false;
    }

    /**
     * Call this view's OnClickListener, if it is defined.  Performs all normal
     * actions associated with clicking: reporting accessibility event, playing
     * a sound, etc.
     *
     * @return True there was an assigned OnClickListener that was called, false
     * otherwise is returned.
     */
    public boolean performClick() {
        final ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnClickListener != null) {
            //playSoundEffect(SoundEffectConstants.CLICK);
            li.mOnClickListener.onClick(this);
            return true;
        }
        return false;
    }

    /*
     * Returns true if the view is currently mouse hovered
     *
     * @return {@code true} if the view is currently mouse hovered
     */
    /*public final boolean isMouseHovered() {
        return (mPrivateFlags & PFLAG_HOVERED) != 0;
    }*/

    /*
     * Called when mouse start to hover on this view
     *
     * @param mouseX relative mouse x pos
     * @param mouseY relative mouse y pos
     */
    /*protected void onMouseHoverEnter(double mouseX, double mouseY) {

    }*/

    /*
     * Called when mouse hovered on this view and moved
     * Also called at the same time as onMouseHoverEnter()
     *
     * @param mouseX relative mouse x pos
     * @param mouseY relative mouse y pos
     */
    /*protected void onMouseHoverMoved(double mouseX, double mouseY) {

    }*/

    /*
     * Called when mouse no longer hover on this view
     */
    /*protected void onMouseHoverExit() {

    }*/

    /*
     * Internal method. Called when mouse hovered on this view and a mouse button clicked.
     *
     * @param mouseX      relative mouse x pos
     * @param mouseY      relative mouse y pos
     * @param mouseButton mouse button, for example {@link GLFW#GLFW_MOUSE_BUTTON_LEFT}
     * @return {@code true} if action performed
     */
    /*final boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        ScrollBar scrollBar = verticalScrollBar;
        if (scrollBar != null && scrollBar.onMouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        scrollBar = horizontalScrollBar;
        if (scrollBar != null && scrollBar.onMouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        return onMouseClicked(mouseX, mouseY, mouseButton);
    }*/

    /**
     * Determines whether the given point, in local coordinates is inside the view.
     */
    final boolean pointInView(float localX, float localY) {
        return pointInView(localX, localY, 0);
    }

    /**
     * Utility method to determine whether the given point, in local coordinates,
     * is inside the view, where the area of the view is expanded by the slop factor.
     * This method is called while processing touch-move events to determine if the event
     * is still within the view.
     */
    boolean pointInView(float localX, float localY, float slop) {
        return localX >= -slop && localY >= -slop && localX < ((mRight - mLeft) + slop) &&
                localY < ((mBottom - mTop) + slop);
    }

    /**
     * Called when mouse hovered on this view and a mouse button pressed.
     *
     * @param mouseX      relative mouse x pos
     * @param mouseY      relative mouse y pos
     * @param mouseButton mouse button, for example {@link GLFW#GLFW_MOUSE_BUTTON_LEFT}
     * @return {@code true} if action performed
     * @see GLFW
     */
    protected boolean onMousePressed(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    /**
     * Called when mouse hovered on this view and a mouse button released.
     *
     * @param mouseX      relative mouse x pos
     * @param mouseY      relative mouse y pos
     * @param mouseButton mouse button, for example {@link GLFW#GLFW_MOUSE_BUTTON_LEFT}
     * @return {@code true} if action performed
     * @see GLFW
     */
    protected boolean onMouseReleased(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    /**
     * Called when mouse pressed and released on this view with the mouse button.
     * Called after onMouseReleased no matter whether it's consumed or not.
     *
     * @param mouseX      relative mouse x pos
     * @param mouseY      relative mouse y pos
     * @param mouseButton mouse button, for example {@link GLFW#GLFW_MOUSE_BUTTON_LEFT}
     * @return {@code true} if action performed
     * @see GLFW
     */
    protected boolean onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    /**
     * Called when mouse hovered on this view and left button double clicked.
     * If no action performed in this method, onMousePressed will be called.
     *
     * @param mouseX relative mouse x pos
     * @param mouseY relative mouse y pos
     * @return {@code true} if action performed
     */
    protected boolean onMouseDoubleClicked(double mouseX, double mouseY) {
        return false;
    }

    /**
     * Called when mouse hovered on this view and mouse scrolled.
     *
     * @param mouseX relative mouse x pos
     * @param mouseY relative mouse y pos
     * @param amount scroll amount
     * @return return {@code true} if action performed
     */
    protected boolean onMouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }

    /**
     * Call when this view start to be listened as a draggable.
     */
    protected void onStartDragging() {

    }

    /**
     * Call when this view is no longer listened as a draggable.
     */
    protected void onStopDragging() {

    }

    /**
     * Called when mouse moved and dragged.
     *
     * @param mouseX relative mouse x pos
     * @param mouseY relative mouse y pos
     * @param deltaX mouse x pos change
     * @param deltaY mouse y pos change
     * @return {@code true} if action performed
     */
    protected boolean onMouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        return false;
    }

    /**
     * Call when this view start to be listened as a keyboard listener.
     */
    protected void onStartKeyboard() {

    }

    /**
     * Call when this view is no longer listened as a keyboard listener.
     */
    protected void onStopKeyboard() {

    }

    /**
     * Called when a key pressed.
     *
     * @param keyCode   see {@link GLFW}, for example {@link GLFW#GLFW_KEY_W}
     * @param scanCode  keyboard scan code, seldom used
     * @param modifiers modifier key
     * @return return {@code true} if action performed
     */
    protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /**
     * Called when a key released.
     *
     * @param keyCode   see {@link GLFW}, for example {@link GLFW#GLFW_KEY_W}
     * @param scanCode  keyboard scan code, seldom used
     * @param modifiers modifier key
     * @return return {@code true} if action performed
     */
    protected boolean onKeyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /**
     * Called when a unicode character typed.
     *
     * @param codePoint char code
     * @param modifiers modifier key
     * @return return {@code true} if action performed
     */
    protected boolean onCharTyped(char codePoint, int modifiers) {
        return false;
    }

    /**
     * Called when click on scrollbar track and not on the thumb
     * and there was an scroll amount change
     *
     * @param vertical    {@code true} if scrollbar is vertical, horizontal otherwise
     * @param scrollDelta scroll amount change calculated by scrollbar
     */
    protected void onScrollBarClicked(boolean vertical, float scrollDelta) {

    }

    /**
     * Called when drag the scroll thumb and there was an scroll
     * amount change
     *
     * @param vertical    {@code true} if scrollbar is vertical, horizontal otherwise
     * @param scrollDelta scroll amount change calculated by scrollbar
     */
    protected void onScrollBarDragged(boolean vertical, float scrollDelta) {

    }

    /*
     * Layout scroll bars if enabled
     */
    /*private void layoutScrollBars() {
        ScrollBar scrollBar = verticalScrollBar;
        if (scrollBar != null) {
            int thickness = scrollBar.getSize();
            int r = mRight - scrollBar.getRightPadding();
            int l = Math.max(r - thickness, mLeft);
            int t = mTop + scrollBar.getTopPadding();
            int b = mBottom - scrollBar.getBottomPadding();
            scrollBar.setFrame(l, t, r, b);
        }
        scrollBar = horizontalScrollBar;
        if (scrollBar != null) {
            int thickness = scrollBar.getSize();
            int b = mBottom - scrollBar.getBottomPadding();
            int t = Math.max(b - thickness, mTop);
            int l = mLeft + scrollBar.getLeftPadding();
            int r = mRight - scrollBar.getRightPadding();
            if (isVerticalScrollBarEnabled()) {
                r -= verticalScrollBar.getWidth();
            }
            scrollBar.setFrame(l, t, r, b);
        }
    }*/

    /**
     * Creates an image that the system displays during the drag and drop operation.
     */
    public static class DragShadow {

        private final WeakReference<View> viewRef;

        public DragShadow(View view) {
            viewRef = new WeakReference<>(view);
        }

        /**
         * Construct a shadow builder object with no associated View. This
         * constructor variant is only useful when the {@link #onProvideShadowCenter(Point)}}
         * and {@link #onDrawShadow(Canvas)} methods are also overridden in order
         * to supply the drag shadow's dimensions and appearance without
         * reference to any View object.
         */
        public DragShadow() {
            viewRef = new WeakReference<>(null);
        }

        @Nullable
        public final View getView() {
            return viewRef.get();
        }

        /**
         * Called when the view is not mouse hovered or shadow is nonnull, to determine
         * where the mouse cursor position is in the shadow.
         *
         * @param outShadowCenter the center point in the shadow
         */
        public void onProvideShadowCenter(@Nonnull Point outShadowCenter) {

        }

        /**
         * Draw the shadow.
         *
         * @param canvas canvas to draw content
         */
        public void onDrawShadow(@Nonnull Canvas canvas) {
            View view = viewRef.get();
            if (view != null) {
                view.onDraw(canvas);
            } else {
                ModernUI.LOGGER.error(VIEW_MARKER, "No view found on draw shadow");
            }
        }
    }

    static class ListenerInfo {

        private OnClickListener mOnClickListener;

        private OnHoverListener mOnHoverListener;
    }

    /**
     * Interface definition for a callback to be invoked when a view is clicked.
     */
    @FunctionalInterface
    public interface OnClickListener {

        /**
         * Called when a view has been clicked.
         *
         * @param v The view that was clicked.
         */
        void onClick(View v);
    }

    /**
     * Interface definition for a callback to be invoked when a hover event is
     * dispatched to this view. The callback will be invoked before the hover
     * event is given to the view.
     */
    public interface OnHoverListener {

        /**
         * Called when a hover event is dispatched to a view. This allows listeners to
         * get a chance to respond before the target view.
         *
         * @param v     The view the hover event has been dispatched to.
         * @param event The MotionEvent object containing full information about
         *              the event.
         * @return True if the listener has consumed the event, false otherwise.
         */
        boolean onHover(View v, MotionEvent event);
    }
}
