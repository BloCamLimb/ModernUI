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
import icyllis.modernui.animation.AnimationUtils;
import icyllis.modernui.animation.StateListAnimator;
import icyllis.modernui.annotation.CallSuper;
import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.core.ArchCore;
import icyllis.modernui.core.Choreographer;
import icyllis.modernui.core.Handler;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.RenderNode;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.math.Matrix4;
import icyllis.modernui.math.Point;
import icyllis.modernui.math.Rect;
import icyllis.modernui.math.RectF;
import icyllis.modernui.text.TextUtils;
import icyllis.modernui.transition.Fade;
import icyllis.modernui.transition.Transition;
import icyllis.modernui.util.*;
import icyllis.modernui.view.menu.MenuBuilder;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.intellij.lang.annotations.MagicConstant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import static icyllis.modernui.ModernUI.LOGGER;

/**
 * <p>
 * This class represents the basic building block for user interface components. A View
 * occupies a rectangular area on the screen and is responsible for drawing and
 * event handling. View is the base class for <em>widgets</em>, which are
 * used to create interactive UI components (buttons, text fields, etc.). The
 * {@link ViewGroup} subclass is the base class for <em>layouts</em>, which
 * are invisible containers that hold other Views (or other ViewGroups) and define
 * their layout properties.
 * </p>
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For information about using this class to develop your application's user interface,
 * read the <a href="https://developer.android.com/guide/topics/ui/index.html">User Interface</a>
 * developer guide.
 * </div>
 *
 * <a name="Using"></a>
 * <h3>Using Views</h3>
 * <p>
 * All of the views in a window are arranged in a single tree. You can add views
 * from code. There are many specialized subclasses of views that act as controls or
 * are capable of displaying text, images, or other content.
 * </p>
 * <p>
 * Once you have created a tree of views, there are typically a few types of
 * common operations you may wish to perform:
 * <ul>
 * <li><strong>Set properties:</strong> for example setting the text of a
 * {@link icyllis.modernui.widget.TextView}. The available properties and the methods
 * that set them will vary among the different subclasses of views.</li>
 * <li><strong>Set focus:</strong> The framework will handle moving focus in
 * response to user input. To force focus to a specific view, call
 * {@link #requestFocus()}.</li>
 * <li><strong>Set up listeners:</strong> Views allow clients to set listeners
 * that will be notified when something interesting happens to the view. For
 * example, all views will let you set a listener to be notified when the view
 * gains or loses focus. You can register such a listener using
 * {@link #setOnFocusChangeListener(OnFocusChangeListener)}.
 * Other view subclasses offer more specialized listeners. For example, a Button
 * exposes a listener to notify clients when the button is clicked.</li>
 * <li><strong>Set visibility:</strong> You can hide or show views using
 * {@link #setVisibility(int)}.</li>
 * </ul>
 * </p>
 * <p><em>
 * Note: Modern UI view system is responsible for measuring, laying out and
 * drawing views. You should not call methods that perform these actions on
 * views yourself unless you are actually implementing a {@link ViewGroup}.
 * </em></p>
 *
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
 * @see ViewGroup
 * @since 2.0
 */
@SuppressWarnings({"unused", "MagicConstant"})
@UiThread
public class View implements Drawable.Callback {

    /**
     * The logging marker used by this class with {@link org.apache.logging.log4j.Logger}.
     */
    protected static final Marker VIEW_MARKER = MarkerManager.getMarker("View");

    /**
     * Used to mark a View that has no ID.
     */
    public static final int NO_ID = -1;

    @MagicConstant(intValues = {NOT_FOCUSABLE, FOCUSABLE, FOCUSABLE_AUTO})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Focusable {
    }

    /**
     * This view does not want keystrokes.
     * <p>
     * Use with {@link #setFocusable(int)}.
     */
    public static final int NOT_FOCUSABLE = 0x00000000;

    /**
     * This view wants keystrokes.
     * <p>
     * Use with {@link #setFocusable(int)}.
     */
    public static final int FOCUSABLE = 0x00000001;

    /**
     * This view determines focusability automatically. This is the default.
     * <p>
     * Use with {@link #setFocusable(int)}.
     */
    public static final int FOCUSABLE_AUTO = 0x00000010;

    /**
     * Mask for use with setFlags indicating bits used for focus.
     */
    private static final int FOCUSABLE_MASK = 0x00000011;

    @MagicConstant(intValues = {VISIBLE, INVISIBLE, GONE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Visibility {
    }

    /**
     * This view is visible.
     * Use with {@link #setVisibility}.
     */
    public static final int VISIBLE = 0x00000000;

    /**
     * This view is invisible, but it still takes up space for layout purposes.
     * Use with {@link #setVisibility}.
     */
    public static final int INVISIBLE = 0x00000004;

    /**
     * This view is invisible, and it doesn't take any space for layout
     * purposes. Use with {@link #setVisibility}.
     */
    public static final int GONE = 0x00000008;

    /**
     * Mask for use with setFlags indicating bits used for visibility.
     * {@hide}
     */
    static final int VISIBILITY_MASK = 0x0000000C;

    /**
     * This view is enabled. Interpretation varies by subclass.
     * Use with ENABLED_MASK when calling setFlags.
     * {@hide}
     */
    static final int ENABLED = 0x00000000;

    /**
     * This view is disabled. Interpretation varies by subclass.
     * Use with ENABLED_MASK when calling setFlags.
     * {@hide}
     */
    static final int DISABLED = 0x00000020;

    /**
     * Mask for use with setFlags indicating bits used for indicating whether
     * this view is enabled
     * {@hide}
     */
    static final int ENABLED_MASK = 0x00000020;

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
     * <p>This view doesn't show scrollbars.</p>
     * {@hide}
     */
    static final int SCROLLBARS_NONE = 0x00000000;

    /**
     * <p>This view shows horizontal scrollbars.</p>
     * {@hide}
     */
    static final int SCROLLBARS_HORIZONTAL = 0x00000100;

    /**
     * <p>This view shows vertical scrollbars.</p>
     * {@hide}
     */
    static final int SCROLLBARS_VERTICAL = 0x00000200;

    /**
     * <p>Mask for use with setFlags indicating bits used for indicating which
     * scrollbars are enabled.</p>
     * {@hide}
     */
    static final int SCROLLBARS_MASK = 0x00000300;

    /**
     * <p>This view doesn't show fading edges.</p>
     * {@hide}
     */
    static final int FADING_EDGE_NONE = 0x00000000;

    /**
     * <p>This view shows horizontal fading edges.</p>
     * {@hide}
     */
    static final int FADING_EDGE_HORIZONTAL = 0x00001000;

    /**
     * <p>This view shows vertical fading edges.</p>
     * {@hide}
     */
    static final int FADING_EDGE_VERTICAL = 0x00002000;

    /**
     * <p>Mask for use with setFlags indicating bits used for indicating which
     * fading edges are enabled.</p>
     * {@hide}
     */
    static final int FADING_EDGE_MASK = 0x00003000;

    /**
     * <p>Indicates this view can be clicked. When clickable, a View reacts
     * to clicks by notifying the OnClickListener.<p>
     * {@hide}
     */
    static final int CLICKABLE = 0x00004000;

    /**
     * <p>Indicates this view can take / keep focus when int touch mode.</p>
     * {@hide}
     */
    static final int FOCUSABLE_IN_TOUCH_MODE = 0x00040000;

    /**
     * <p>
     * Indicates this view can be long clicked. When long clickable, a View
     * reacts to long clicks by notifying the OnLongClickListener or showing a
     * context menu.
     * </p>
     * {@hide}
     */
    static final int LONG_CLICKABLE = 0x00200000;

    /**
     * <p>Indicates that this view gets its drawable states from its direct parent
     * and ignores its original internal states.</p>
     *
     * @hide
     */
    static final int DUPLICATE_PARENT_STATE = 0x00400000;

    /**
     * <p>
     * Indicates this view can be context clicked. When context clickable, a View reacts to a
     * context click (e.g. a primary stylus button press or right mouse click) by notifying the
     * OnContextClickListener.
     * </p>
     * {@hide}
     */
    static final int CONTEXT_CLICKABLE = 0x00800000;

    @MagicConstant(intValues = {
            SCROLLBARS_INSIDE_OVERLAY,
            SCROLLBARS_INSIDE_INSET,
            SCROLLBARS_OUTSIDE_OVERLAY,
            SCROLLBARS_OUTSIDE_INSET
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScrollBarStyle {
    }

    /**
     * The scrollbar style to display the scrollbars inside the content area,
     * without increasing the padding. The scrollbars will be overlaid with
     * translucency on the view's content.
     */
    public static final int SCROLLBARS_INSIDE_OVERLAY = 0;

    /**
     * The scrollbar style to display the scrollbars inside the padded area,
     * increasing the padding of the view. The scrollbars will not overlap the
     * content area of the view.
     */
    public static final int SCROLLBARS_INSIDE_INSET = 0x01000000;

    /**
     * The scrollbar style to display the scrollbars at the edge of the view,
     * without increasing the padding. The scrollbars will be overlaid with
     * translucency.
     */
    public static final int SCROLLBARS_OUTSIDE_OVERLAY = 0x02000000;

    /**
     * The scrollbar style to display the scrollbars at the edge of the view,
     * increasing the padding of the view. The scrollbars will only overlap the
     * background, if any.
     */
    public static final int SCROLLBARS_OUTSIDE_INSET = 0x03000000;

    /**
     * Mask to check if the scrollbar style is overlay or inset.
     * {@hide}
     */
    static final int SCROLLBARS_INSET_MASK = 0x01000000;

    /**
     * Mask to check if the scrollbar style is inside or outside.
     * {@hide}
     */
    static final int SCROLLBARS_OUTSIDE_MASK = 0x02000000;

    /**
     * Mask for scrollbar style.
     * {@hide}
     */
    static final int SCROLLBARS_STYLE_MASK = 0x03000000;

    /**
     * View flag indicating whether this view should have sound effects enabled
     * for events such as clicking and touching.
     */
    public static final int SOUND_EFFECTS_ENABLED = 0x08000000;

    /**
     * View flag indicating whether this view should have haptic feedback
     * enabled for events such as long presses.
     */
    public static final int HAPTIC_FEEDBACK_ENABLED = 0x10000000;

    /**
     * <p>Indicates this view can display a tooltip on hover or long press.</p>
     * {@hide}
     */
    static final int TOOLTIP = 0x40000000;

    @MagicConstant(flags = {
            FOCUSABLES_ALL,
            FOCUSABLES_TOUCH_MODE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FocusableMode {
    }

    /**
     * View flag indicating whether {@link #addFocusables(ArrayList, int, int)}
     * should add all focusable Views regardless if they are focusable in touch mode.
     */
    public static final int FOCUSABLES_ALL = 0x00000000;

    /**
     * View flag indicating whether {@link #addFocusables(ArrayList, int, int)}
     * should add only Views focusable in touch mode.
     */
    public static final int FOCUSABLES_TOUCH_MODE = 0x00000001;

    @MagicConstant(intValues = {
            FOCUS_BACKWARD,
            FOCUS_FORWARD,
            FOCUS_LEFT,
            FOCUS_UP,
            FOCUS_RIGHT,
            FOCUS_DOWN
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FocusDirection {
    }

    @MagicConstant(intValues = {
            FOCUS_LEFT,
            FOCUS_UP,
            FOCUS_RIGHT,
            FOCUS_DOWN
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FocusRealDirection {
        // Like @FocusDirection, but without forward/backward
    }

    /**
     * Use with {@link #focusSearch(int)}. Move focus to the previous selectable
     * item.
     */
    public static final int FOCUS_BACKWARD = 0x00000001;

    /**
     * Use with {@link #focusSearch(int)}. Move focus to the next selectable
     * item.
     */
    public static final int FOCUS_FORWARD = 0x00000002;

    /**
     * Use with {@link #focusSearch(int)}. Move focus to the left.
     */
    public static final int FOCUS_LEFT = 0x00000011;

    /**
     * Use with {@link #focusSearch(int)}. Move focus up.
     */
    public static final int FOCUS_UP = 0x00000021;

    /**
     * Use with {@link #focusSearch(int)}. Move focus to the right.
     */
    public static final int FOCUS_RIGHT = 0x00000042;

    /**
     * Use with {@link #focusSearch(int)}. Move focus down.
     */
    public static final int FOCUS_DOWN = 0x00000082;

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

    // Base View state sets
    // Singles
    /**
     * Indicates the view has no states set. States are used with
     * {@link Drawable} to change the drawing of the
     * view depending on its state.
     *
     * @see Drawable
     * @see #getDrawableState()
     */
    protected static final int[] EMPTY_STATE_SET;
    /**
     * Indicates the view is enabled. States are used with
     * {@link Drawable} to change the drawing of the
     * view depending on its state.
     *
     * @see Drawable
     * @see #getDrawableState()
     */
    protected static final int[] ENABLED_STATE_SET;
    /**
     * Indicates the view is focused. States are used with
     * {@link Drawable} to change the drawing of the
     * view depending on its state.
     *
     * @see Drawable
     * @see #getDrawableState()
     */
    protected static final int[] FOCUSED_STATE_SET;
    /**
     * Indicates the view is selected. States are used with
     * {@link Drawable} to change the drawing of the
     * view depending on its state.
     *
     * @see Drawable
     * @see #getDrawableState()
     */
    protected static final int[] SELECTED_STATE_SET;
    /**
     * Indicates the view is pressed. States are used with
     * {@link Drawable} to change the drawing of the
     * view depending on its state.
     *
     * @see Drawable
     * @see #getDrawableState()
     */
    protected static final int[] PRESSED_STATE_SET;
    /**
     * Indicates the view's window has focus. States are used with
     * {@link Drawable} to change the drawing of the
     * view depending on its state.
     *
     * @see Drawable
     * @see #getDrawableState()
     */
    protected static final int[] WINDOW_FOCUSED_STATE_SET;
    // Doubles
    /**
     * Indicates the view is enabled and has the focus.
     *
     * @see #ENABLED_STATE_SET
     * @see #FOCUSED_STATE_SET
     */
    protected static final int[] ENABLED_FOCUSED_STATE_SET;
    /**
     * Indicates the view is enabled and selected.
     *
     * @see #ENABLED_STATE_SET
     * @see #SELECTED_STATE_SET
     */
    protected static final int[] ENABLED_SELECTED_STATE_SET;
    /**
     * Indicates the view is enabled and that its window has focus.
     *
     * @see #ENABLED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] ENABLED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is focused and selected.
     *
     * @see #FOCUSED_STATE_SET
     * @see #SELECTED_STATE_SET
     */
    protected static final int[] FOCUSED_SELECTED_STATE_SET;
    /**
     * Indicates the view has the focus and that its window has the focus.
     *
     * @see #FOCUSED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] FOCUSED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is selected and that its window has the focus.
     *
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] SELECTED_WINDOW_FOCUSED_STATE_SET;
    // Triples
    /**
     * Indicates the view is enabled, focused and selected.
     *
     * @see #ENABLED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #SELECTED_STATE_SET
     */
    protected static final int[] ENABLED_FOCUSED_SELECTED_STATE_SET;
    /**
     * Indicates the view is enabled, focused and its window has the focus.
     *
     * @see #ENABLED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is enabled, selected and its window has the focus.
     *
     * @see #ENABLED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is focused, selected and its window has the focus.
     *
     * @see #FOCUSED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is enabled, focused, selected and its window
     * has the focus.
     *
     * @see #ENABLED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed and its window has the focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed and selected.
     *
     * @see #PRESSED_STATE_SET
     * @see #SELECTED_STATE_SET
     */
    protected static final int[] PRESSED_SELECTED_STATE_SET;
    /**
     * Indicates the view is pressed, selected and its window has the focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed and focused.
     *
     * @see #PRESSED_STATE_SET
     * @see #FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed, focused and its window has the focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_FOCUSED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed, focused and selected.
     *
     * @see #PRESSED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_FOCUSED_SELECTED_STATE_SET;
    /**
     * Indicates the view is pressed, focused, selected and its window has the focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed and enabled.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled and its window has the focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled and selected.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #SELECTED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_SELECTED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled, selected and its window has the
     * focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled and focused.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled, focused and its window has the
     * focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled, focused and selected.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_FOCUSED_SELECTED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled, focused, selected and its window
     * has the focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET;

    static {
        EMPTY_STATE_SET = StateSet.WILD_CARD;

        WINDOW_FOCUSED_STATE_SET = StateSet.get(StateSet.VIEW_STATE_WINDOW_FOCUSED);

        SELECTED_STATE_SET = StateSet.get(StateSet.VIEW_STATE_SELECTED);
        SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_SELECTED);

        FOCUSED_STATE_SET = StateSet.get(StateSet.VIEW_STATE_FOCUSED);
        FOCUSED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_FOCUSED);
        FOCUSED_SELECTED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_SELECTED | StateSet.VIEW_STATE_FOCUSED);
        FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_SELECTED
                        | StateSet.VIEW_STATE_FOCUSED);

        ENABLED_STATE_SET = StateSet.get(StateSet.VIEW_STATE_ENABLED);
        ENABLED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_ENABLED);
        ENABLED_SELECTED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_SELECTED | StateSet.VIEW_STATE_ENABLED);
        ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_SELECTED
                        | StateSet.VIEW_STATE_ENABLED);
        ENABLED_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_FOCUSED | StateSet.VIEW_STATE_ENABLED);
        ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_FOCUSED
                        | StateSet.VIEW_STATE_ENABLED);
        ENABLED_FOCUSED_SELECTED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_SELECTED | StateSet.VIEW_STATE_FOCUSED
                        | StateSet.VIEW_STATE_ENABLED);
        ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_SELECTED
                        | StateSet.VIEW_STATE_FOCUSED | StateSet.VIEW_STATE_ENABLED);

        PRESSED_STATE_SET = StateSet.get(StateSet.VIEW_STATE_PRESSED);
        PRESSED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_PRESSED);
        PRESSED_SELECTED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_SELECTED | StateSet.VIEW_STATE_PRESSED);
        PRESSED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_SELECTED
                        | StateSet.VIEW_STATE_PRESSED);
        PRESSED_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_FOCUSED | StateSet.VIEW_STATE_PRESSED);
        PRESSED_FOCUSED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_FOCUSED
                        | StateSet.VIEW_STATE_PRESSED);
        PRESSED_FOCUSED_SELECTED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_SELECTED | StateSet.VIEW_STATE_FOCUSED
                        | StateSet.VIEW_STATE_PRESSED);
        PRESSED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_SELECTED
                        | StateSet.VIEW_STATE_FOCUSED | StateSet.VIEW_STATE_PRESSED);
        PRESSED_ENABLED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_ENABLED | StateSet.VIEW_STATE_PRESSED);
        PRESSED_ENABLED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_ENABLED
                        | StateSet.VIEW_STATE_PRESSED);
        PRESSED_ENABLED_SELECTED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_SELECTED | StateSet.VIEW_STATE_ENABLED
                        | StateSet.VIEW_STATE_PRESSED);
        PRESSED_ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_SELECTED
                        | StateSet.VIEW_STATE_ENABLED | StateSet.VIEW_STATE_PRESSED);
        PRESSED_ENABLED_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_FOCUSED | StateSet.VIEW_STATE_ENABLED
                        | StateSet.VIEW_STATE_PRESSED);
        PRESSED_ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_FOCUSED
                        | StateSet.VIEW_STATE_ENABLED | StateSet.VIEW_STATE_PRESSED);
        PRESSED_ENABLED_FOCUSED_SELECTED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_SELECTED | StateSet.VIEW_STATE_FOCUSED
                        | StateSet.VIEW_STATE_ENABLED | StateSet.VIEW_STATE_PRESSED);
        PRESSED_ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_SELECTED
                        | StateSet.VIEW_STATE_FOCUSED | StateSet.VIEW_STATE_ENABLED
                        | StateSet.VIEW_STATE_PRESSED);
    }

    /**
     * Temporary Rect currently for use in setBackground().  This will probably
     * be extended in the future to hold our own class with more than just
     * a Rect. :)
     */
    static final ThreadLocal<Rect> sThreadLocal = ThreadLocal.withInitial(Rect::new);

    /*
     * Masks for mPrivateFlags, as generated by dumpFlags():
     *
     * |-------|-------|-------|-------|
     *                                 1 PFLAG_WANTS_FOCUS
     *                                1  PFLAG_FOCUSED
     *                               1   PFLAG_SELECTED
     *                              1    PFLAG_IS_ROOT_NAMESPACE
     *                             1     PFLAG_HAS_BOUNDS
     *                            1      PFLAG_DRAWN
     *                           1       PFLAG_DRAW_ANIMATION
     *                          1        PFLAG_SKIP_DRAW
     *                        1          PFLAG_REQUEST_TRANSPARENT_REGIONS
     *                       1           PFLAG_DRAWABLE_STATE_DIRTY
     *                      1            PFLAG_MEASURED_DIMENSION_SET
     *                     1             PFLAG_FORCE_LAYOUT
     *                    1              PFLAG_LAYOUT_REQUIRED
     *                   1               PFLAG_PRESSED
     *                  1                PFLAG_DRAWING_CACHE_VALID
     *                 1                 PFLAG_ANIMATION_STARTED
     *                1                  PFLAG_SAVE_STATE_CALLED
     *               1                   PFLAG_ALPHA_SET
     *              1                    PFLAG_SCROLL_CONTAINER
     *             1                     PFLAG_SCROLL_CONTAINER_ADDED
     *            1                      PFLAG_DIRTY
     *            1                      PFLAG_DIRTY_MASK
     *          1                        PFLAG_OPAQUE_BACKGROUND
     *         1                         PFLAG_OPAQUE_SCROLLBARS
     *         11                        PFLAG_OPAQUE_MASK
     *        1                          PFLAG_PREPRESSED
     *       1                           PFLAG_CANCEL_NEXT_UP_EVENT
     *      1                            PFLAG_AWAKEN_SCROLL_BARS_ON_ATTACH
     *     1                             PFLAG_HOVERED
     *   1                               PFLAG_ACTIVATED
     *  1                                PFLAG_INVALIDATED
     * |-------|-------|-------|-------|
     */
    /**
     * {@hide}
     */
    static final int PFLAG_WANTS_FOCUS = 0x00000001;
    /**
     * {@hide}
     */
    static final int PFLAG_FOCUSED = 0x00000002;
    /**
     * {@hide}
     */
    static final int PFLAG_SELECTED = 0x00000004;
    /**
     * {@hide}
     */
    static final int PFLAG_IS_ROOT_NAMESPACE = 0x00000008;
    /**
     * {@hide}
     */
    static final int PFLAG_HAS_BOUNDS = 0x00000010;
    /**
     * {@hide}
     */
    static final int PFLAG_DRAWN = 0x00000020;
    /**
     * When this flag is set, this view is running an animation on behalf of its
     * children and should therefore not cancel invalidate requests, even if they
     * lie outside of this view's bounds.
     * <p>
     * {@hide}
     */
    static final int PFLAG_DRAW_ANIMATION = 0x00000040;
    /**
     * {@hide}
     */
    static final int PFLAG_SKIP_DRAW = 0x00000080;
    /**
     * {@hide}
     */
    static final int PFLAG_REQUEST_TRANSPARENT_REGIONS = 0x00000200;
    /**
     * {@hide}
     */
    static final int PFLAG_DRAWABLE_STATE_DIRTY = 0x00000400;
    /**
     * {@hide}
     */
    static final int PFLAG_MEASURED_DIMENSION_SET = 0x00000800;
    /**
     * {@hide}
     */
    static final int PFLAG_FORCE_LAYOUT = 0x00001000;
    /**
     * {@hide}
     */
    static final int PFLAG_LAYOUT_REQUIRED = 0x00002000;

    private static final int PFLAG_PRESSED = 0x00004000;

    /**
     * {@hide}
     */
    static final int PFLAG_DRAWING_CACHE_VALID = 0x00008000;
    /**
     * Flag used to indicate that this view should be drawn once more (and only once
     * more) after its animation has completed.
     * {@hide}
     */
    static final int PFLAG_ANIMATION_STARTED = 0x00010000;

    private static final int PFLAG_SAVE_STATE_CALLED = 0x00020000;

    /**
     * Indicates that the View returned true when onSetAlpha() was called and that
     * the alpha must be restored.
     * {@hide}
     */
    static final int PFLAG_ALPHA_SET = 0x00040000;

    /**
     * Set by {@link #setScrollContainer(boolean)}.
     */
    static final int PFLAG_SCROLL_CONTAINER = 0x00080000;

    /**
     * Set by {@link #setScrollContainer(boolean)}.
     */
    static final int PFLAG_SCROLL_CONTAINER_ADDED = 0x00100000;

    /**
     * View flag indicating whether this view was invalidated (fully or partially.)
     *
     * @hide
     */
    static final int PFLAG_DIRTY = 0x00200000;

    /**
     * Mask for {@link #PFLAG_DIRTY}.
     *
     * @hide
     */
    static final int PFLAG_DIRTY_MASK = 0x00200000;

    /**
     * Indicates whether the background is opaque.
     *
     * @hide
     */
    static final int PFLAG_OPAQUE_BACKGROUND = 0x00800000;

    /**
     * Indicates whether the scrollbars are opaque.
     *
     * @hide
     */
    static final int PFLAG_OPAQUE_SCROLLBARS = 0x01000000;

    /**
     * Indicates whether the view is opaque.
     *
     * @hide
     */
    static final int PFLAG_OPAQUE_MASK = 0x01800000;

    /**
     * Indicates a prepressed state;
     * the short time between ACTION_DOWN and recognizing
     * a 'real' press. Prepressed is used to recognize quick taps
     * even when they are shorter than ViewConfiguration.getTapTimeout().
     *
     * @hide
     */
    private static final int PFLAG_PREPRESSED = 0x02000000;

    /**
     * Indicates whether the view is temporarily detached.
     *
     * @hide
     */
    static final int PFLAG_CANCEL_NEXT_UP_EVENT = 0x04000000;

    /**
     * Indicates that we should awaken scroll bars once attached
     * <p>
     * PLEASE NOTE: This flag is now unused as we now send onVisibilityChanged
     * during window attachment and it is no longer needed. Feel free to repurpose it.
     *
     * @hide
     */
    private static final int PFLAG_AWAKEN_SCROLL_BARS_ON_ATTACH = 0x08000000;

    /**
     * Indicates that the view has received HOVER_ENTER.  Cleared on HOVER_EXIT.
     *
     * @hide
     */
    private static final int PFLAG_HOVERED = 0x10000000;

    /**
     * {@hide}
     */
    static final int PFLAG_ACTIVATED = 0x40000000;

    /**
     * Indicates that this view was specifically invalidated, not just dirtied because some
     * child view was invalidated. The flag is used to determine when we need to recreate
     * a view's display list (as opposed to just returning a reference to its existing
     * display list).
     *
     * @hide
     */
    static final int PFLAG_INVALIDATED = 0x80000000;

    /* End of masks for mPrivateFlags */

    /*
     * Masks for mPrivateFlags2, as generated by dumpFlags():
     *
     * |-------|-------|-------|-------|
     *                                 1 PFLAG2_DRAG_CAN_ACCEPT
     *                                1  PFLAG2_DRAG_HOVERED
     *                              11   PFLAG2_LAYOUT_DIRECTION_MASK
     *                             1     PFLAG2_LAYOUT_DIRECTION_RESOLVED_RTL
     *                            1      PFLAG2_LAYOUT_DIRECTION_RESOLVED
     *                            11     PFLAG2_LAYOUT_DIRECTION_RESOLVED_MASK
     *                           1       PFLAG2_TEXT_DIRECTION_FLAGS[1]
     *                          1        PFLAG2_TEXT_DIRECTION_FLAGS[2]
     *                          11       PFLAG2_TEXT_DIRECTION_FLAGS[3]
     *                         1         PFLAG2_TEXT_DIRECTION_FLAGS[4]
     *                         1 1       PFLAG2_TEXT_DIRECTION_FLAGS[5]
     *                         11        PFLAG2_TEXT_DIRECTION_FLAGS[6]
     *                         111       PFLAG2_TEXT_DIRECTION_FLAGS[7]
     *                         111       PFLAG2_TEXT_DIRECTION_MASK
     *                        1          PFLAG2_TEXT_DIRECTION_RESOLVED
     *                       1           PFLAG2_TEXT_DIRECTION_RESOLVED_DEFAULT
     *                     111           PFLAG2_TEXT_DIRECTION_RESOLVED_MASK
     *                    1              PFLAG2_TEXT_ALIGNMENT_FLAGS[1]
     *                   1               PFLAG2_TEXT_ALIGNMENT_FLAGS[2]
     *                   11              PFLAG2_TEXT_ALIGNMENT_FLAGS[3]
     *                  1                PFLAG2_TEXT_ALIGNMENT_FLAGS[4]
     *                  1 1              PFLAG2_TEXT_ALIGNMENT_FLAGS[5]
     *                  11               PFLAG2_TEXT_ALIGNMENT_FLAGS[6]
     *                  111              PFLAG2_TEXT_ALIGNMENT_MASK
     *                 1                 PFLAG2_TEXT_ALIGNMENT_RESOLVED
     *                1                  PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT
     *              111                  PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK
     *     1                             PFLAG2_VIEW_QUICK_REJECTED
     *    1                              PFLAG2_PADDING_RESOLVED
     *   1                               PFLAG2_DRAWABLE_RESOLVED
     *  1                                PFLAG2_HAS_TRANSIENT_STATE
     * |-------|-------|-------|-------|
     */

    /**
     * Indicates that this view has reported that it can accept the current drag's content.
     * Cleared when the drag operation concludes.
     *
     * @hide
     */
    static final int PFLAG2_DRAG_CAN_ACCEPT = 0x00000001;

    /**
     * Indicates that this view is currently directly under the drag location in a
     * drag-and-drop operation involving content that it can accept.  Cleared when
     * the drag exits the view, or when the drag operation concludes.
     *
     * @hide
     */
    static final int PFLAG2_DRAG_HOVERED = 0x00000002;

    @MagicConstant(intValues = {
            LAYOUT_DIRECTION_LTR,
            LAYOUT_DIRECTION_RTL,
            LAYOUT_DIRECTION_INHERIT,
            LAYOUT_DIRECTION_LOCALE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface LayoutDir {
        // Not called LayoutDirection to avoid conflict with class util.LayoutDirection
    }

    @MagicConstant(intValues = {
            LAYOUT_DIRECTION_LTR,
            LAYOUT_DIRECTION_RTL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ResolvedLayoutDir {
    }

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

    @MagicConstant(intValues = {
            TEXT_ALIGNMENT_INHERIT,
            TEXT_ALIGNMENT_GRAVITY,
            TEXT_ALIGNMENT_CENTER,
            TEXT_ALIGNMENT_TEXT_START,
            TEXT_ALIGNMENT_TEXT_END,
            TEXT_ALIGNMENT_VIEW_START,
            TEXT_ALIGNMENT_VIEW_END
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TextAlignment {
    }

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
     * Flag indicating whether a view failed the quickReject() check in draw(). This condition
     * is used to check whether later changes to the view's transform should invalidate the
     * view to force the quickReject test to run again.
     */
    static final int PFLAG2_VIEW_QUICK_REJECTED = 0x10000000;

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

    // There are a couple of flags left in mPrivateFlags2

    /* End of masks for mPrivateFlags2 */

    /*
     * Masks for mPrivateFlags3, as generated by dumpFlags():
     *
     * |-------|-------|-------|-------|
     *                                 1 PFLAG3_VIEW_IS_ANIMATING_TRANSFORM
     *                                1  PFLAG3_VIEW_IS_ANIMATING_ALPHA
     *                               1   PFLAG3_IS_LAID_OUT
     *                              1    PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT
     *                             1     PFLAG3_CALLED_SUPER
     *                            1      PFLAG3_APPLYING_INSETS
     *                           1       PFLAG3_FITTING_SYSTEM_WINDOWS
     *                          1        PFLAG3_NESTED_SCROLLING_ENABLED
     *                         1         PFLAG3_SCROLL_INDICATOR_TOP
     *                        1          PFLAG3_SCROLL_INDICATOR_BOTTOM
     *                       1           PFLAG3_SCROLL_INDICATOR_LEFT
     *                      1            PFLAG3_SCROLL_INDICATOR_RIGHT
     *                     1             PFLAG3_SCROLL_INDICATOR_START
     *                    1              PFLAG3_SCROLL_INDICATOR_END
     *                   1               PFLAG3_ASSIST_BLOCKED
     *                  1                PFLAG3_CLUSTER
     *                1                  PFLAG3_FINGER_DOWN
     *               1                   PFLAG3_FOCUSED_BY_DEFAULT
     *          1                        PFLAG3_OVERLAPPING_RENDERING_FORCED_VALUE
     *         1                         PFLAG3_HAS_OVERLAPPING_RENDERING_FORCED
     *        1                          PFLAG3_TEMPORARY_DETACH
     *       1                           PFLAG3_NO_REVEAL_ON_FOCUS
     *     1                             PFLAG3_SCREEN_READER_FOCUSABLE
     *    1                              PFLAG3_AGGREGATED_VISIBLE
     * |-------|-------|-------|-------|
     */

    /**
     * Flag indicating that view has a transform animation set on it. This is used to track whether
     * an animation is cleared between successive frames, in order to tell the associated
     * DisplayList to clear its animation matrix.
     */
    static final int PFLAG3_VIEW_IS_ANIMATING_TRANSFORM = 0x1;

    /**
     * Flag indicating that view has an alpha animation set on it. This is used to track whether an
     * animation is cleared between successive frames, in order to tell the associated
     * DisplayList to restore its alpha value.
     */
    static final int PFLAG3_VIEW_IS_ANIMATING_ALPHA = 0x2;

    /**
     * Flag indicating that the view has been through at least one layout since it
     * was last attached to a window.
     */
    static final int PFLAG3_IS_LAID_OUT = 0x4;

    /**
     * Flag indicating that a call to measure() was skipped and should be done
     * instead when layout() is invoked.
     */
    static final int PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT = 0x8;

    /**
     * Flag indicating that an overridden method correctly called down to
     * the superclass implementation as required by the API spec.
     */
    static final int PFLAG3_CALLED_SUPER = 0x10;

    /**
     * Flag indicating that we're in the process of applying window insets.
     */
    static final int PFLAG3_APPLYING_INSETS = 0x20;

    /**
     * Flag indicating that we're in the process of fitting system windows using the old method.
     */
    static final int PFLAG3_FITTING_SYSTEM_WINDOWS = 0x40;

    /**
     * Flag indicating that nested scrolling is enabled for this view.
     * The view will optionally cooperate with views up its parent chain to allow for
     * integrated nested scrolling along the same axis.
     */
    static final int PFLAG3_NESTED_SCROLLING_ENABLED = 0x80;

    /**
     * Flag indicating that the bottom scroll indicator should be displayed
     * when this view can scroll up.
     */
    static final int PFLAG3_SCROLL_INDICATOR_TOP = 0x0100;

    /**
     * Flag indicating that the bottom scroll indicator should be displayed
     * when this view can scroll down.
     */
    static final int PFLAG3_SCROLL_INDICATOR_BOTTOM = 0x0200;

    /**
     * Flag indicating that the left scroll indicator should be displayed
     * when this view can scroll left.
     */
    static final int PFLAG3_SCROLL_INDICATOR_LEFT = 0x0400;

    /**
     * Flag indicating that the right scroll indicator should be displayed
     * when this view can scroll right.
     */
    static final int PFLAG3_SCROLL_INDICATOR_RIGHT = 0x0800;

    /**
     * Flag indicating that the start scroll indicator should be displayed
     * when this view can scroll in the start direction.
     */
    static final int PFLAG3_SCROLL_INDICATOR_START = 0x1000;

    /**
     * Flag indicating that the end scroll indicator should be displayed
     * when this view can scroll in the end direction.
     */
    static final int PFLAG3_SCROLL_INDICATOR_END = 0x2000;

    static final int DRAG_MASK = PFLAG2_DRAG_CAN_ACCEPT | PFLAG2_DRAG_HOVERED;

    static final int SCROLL_INDICATORS_NONE = 0x0000;

    /**
     * Mask for use with setFlags indicating bits used for indicating which
     * scroll indicators are enabled.
     */
    static final int SCROLL_INDICATORS_PFLAG3_MASK = PFLAG3_SCROLL_INDICATOR_TOP
            | PFLAG3_SCROLL_INDICATOR_BOTTOM | PFLAG3_SCROLL_INDICATOR_LEFT
            | PFLAG3_SCROLL_INDICATOR_RIGHT | PFLAG3_SCROLL_INDICATOR_START
            | PFLAG3_SCROLL_INDICATOR_END;

    /**
     * Left-shift required to translate between public scroll indicator flags
     * and internal PFLAGS3 flags. When used as a right-shift, translates
     * PFLAGS3 flags to public flags.
     */
    static final int SCROLL_INDICATORS_TO_PFLAGS3_LSHIFT = 8;

    @Retention(RetentionPolicy.SOURCE)
    @MagicConstant(flags = {
            SCROLL_INDICATOR_TOP,
            SCROLL_INDICATOR_BOTTOM,
            SCROLL_INDICATOR_LEFT,
            SCROLL_INDICATOR_RIGHT,
            SCROLL_INDICATOR_START,
            SCROLL_INDICATOR_END,
    })
    public @interface ScrollIndicators {
    }

    /**
     * Scroll indicator direction for the top edge of the view.
     *
     * @see #setScrollIndicators(int)
     * @see #setScrollIndicators(int, int)
     * @see #getScrollIndicators()
     */
    public static final int SCROLL_INDICATOR_TOP =
            PFLAG3_SCROLL_INDICATOR_TOP >> SCROLL_INDICATORS_TO_PFLAGS3_LSHIFT;

    /**
     * Scroll indicator direction for the bottom edge of the view.
     *
     * @see #setScrollIndicators(int)
     * @see #setScrollIndicators(int, int)
     * @see #getScrollIndicators()
     */
    public static final int SCROLL_INDICATOR_BOTTOM =
            PFLAG3_SCROLL_INDICATOR_BOTTOM >> SCROLL_INDICATORS_TO_PFLAGS3_LSHIFT;

    /**
     * Scroll indicator direction for the left edge of the view.
     *
     * @see #setScrollIndicators(int)
     * @see #setScrollIndicators(int, int)
     * @see #getScrollIndicators()
     */
    public static final int SCROLL_INDICATOR_LEFT =
            PFLAG3_SCROLL_INDICATOR_LEFT >> SCROLL_INDICATORS_TO_PFLAGS3_LSHIFT;

    /**
     * Scroll indicator direction for the right edge of the view.
     *
     * @see #setScrollIndicators(int)
     * @see #setScrollIndicators(int, int)
     * @see #getScrollIndicators()
     */
    public static final int SCROLL_INDICATOR_RIGHT =
            PFLAG3_SCROLL_INDICATOR_RIGHT >> SCROLL_INDICATORS_TO_PFLAGS3_LSHIFT;

    /**
     * Scroll indicator direction for the starting edge of the view.
     * <p>
     * Resolved according to the view's layout direction, see
     * {@link #getLayoutDirection()} for more information.
     *
     * @see #setScrollIndicators(int)
     * @see #setScrollIndicators(int, int)
     * @see #getScrollIndicators()
     */
    public static final int SCROLL_INDICATOR_START =
            PFLAG3_SCROLL_INDICATOR_START >> SCROLL_INDICATORS_TO_PFLAGS3_LSHIFT;

    /**
     * Scroll indicator direction for the ending edge of the view.
     * <p>
     * Resolved according to the view's layout direction, see
     * {@link #getLayoutDirection()} for more information.
     *
     * @see #setScrollIndicators(int)
     * @see #setScrollIndicators(int, int)
     * @see #getScrollIndicators()
     */
    public static final int SCROLL_INDICATOR_END =
            PFLAG3_SCROLL_INDICATOR_END >> SCROLL_INDICATORS_TO_PFLAGS3_LSHIFT;

    /**
     * Flag indicating that the view is a root of a keyboard navigation cluster.
     *
     * @see #isKeyboardNavigationCluster()
     * @see #setKeyboardNavigationCluster(boolean)
     */
    private static final int PFLAG3_CLUSTER = 0x8000;

    /**
     * Indicates that the user is currently touching the screen.
     * Currently used for the tooltip positioning only.
     */
    private static final int PFLAG3_FINGER_DOWN = 0x20000;

    /**
     * Flag indicating that this view is the default-focus view.
     *
     * @see #isFocusedByDefault()
     * @see #setFocusedByDefault(boolean)
     */
    private static final int PFLAG3_FOCUSED_BY_DEFAULT = 0x40000;

    /**
     * Whether this view has rendered elements that overlap (see {@link
     * #hasOverlappingRendering()}, {@link #forceHasOverlappingRendering(boolean)}, and
     * {@link #getHasOverlappingRendering()} ). The value in this bit is only valid when
     * PFLAG3_HAS_OVERLAPPING_RENDERING_FORCED has been set. Otherwise, the value is
     * determined by whatever {@link #hasOverlappingRendering()} returns.
     */
    private static final int PFLAG3_OVERLAPPING_RENDERING_FORCED_VALUE = 0x800000;

    /**
     * Whether {@link #forceHasOverlappingRendering(boolean)} has been called. When true, value
     * in PFLAG3_OVERLAPPING_RENDERING_FORCED_VALUE is valid.
     */
    private static final int PFLAG3_HAS_OVERLAPPING_RENDERING_FORCED = 0x1000000;

    /**
     * Flag indicating that the view is temporarily detached from the parent view.
     *
     * @see #onStartTemporaryDetach()
     * @see #onFinishTemporaryDetach()
     */
    static final int PFLAG3_TEMPORARY_DETACH = 0x2000000;

    /**
     * Flag indicating that the view does not wish to be revealed within its parent
     * hierarchy when it gains focus. Expressed in the negative since the historical
     * default behavior is to reveal on focus; this flag suppresses that behavior.
     *
     * @see #setRevealOnFocusHint(boolean)
     * @see #getRevealOnFocusHint()
     */
    private static final int PFLAG3_NO_REVEAL_ON_FOCUS = 0x4000000;

    /**
     * Works like focusable for screen readers, but without the side effects on input focus.
     *
     * @see #setScreenReaderFocusable(boolean)
     */
    private static final int PFLAG3_SCREEN_READER_FOCUSABLE = 0x10000000;

    /**
     * The last aggregated visibility. Used to detect when it truly changes.
     */
    private static final int PFLAG3_AGGREGATED_VISIBLE = 0x20000000;

    /* End of masks for mPrivateFlags3 */

    /*
     * Masks for mPrivateFlags4, as generated by dumpFlags():
     *
     * |-------|-------|-------|-------|
     *                    1             PFLAG4_ALLOW_CLICK_WHEN_DISABLED
     *                   1              PFLAG4_DETACHED
     *                  1               PFLAG4_HAS_TRANSLATION_TRANSIENT_STATE
     * |-------|-------|-------|-------|
     */

    /**
     * Indicates if the view can receive click events when disabled.
     */
    private static final int PFLAG4_ALLOW_CLICK_WHEN_DISABLED = 0x000001000;

    /**
     * Indicates if the view is just detached.
     */
    private static final int PFLAG4_DETACHED = 0x000002000;

    /**
     * Indicates that the view has transient state because the system is translating it.
     */
    private static final int PFLAG4_HAS_TRANSLATION_TRANSIENT_STATE = 0x000004000;

    /* End of masks for mPrivateFlags4 */

    /**
     * Always allow a user to over-scroll this view, provided it is a
     * view that can scroll.
     *
     * @see #getOverScrollMode()
     * @see #setOverScrollMode(int)
     */
    public static final int OVER_SCROLL_ALWAYS = 0;

    /**
     * Allow a user to over-scroll this view only if the content is large
     * enough to meaningfully scroll, provided it is a view that can scroll.
     *
     * @see #getOverScrollMode()
     * @see #setOverScrollMode(int)
     */
    public static final int OVER_SCROLL_IF_CONTENT_SCROLLS = 1;

    /**
     * Never allow a user to over-scroll this view.
     *
     * @see #getOverScrollMode()
     * @see #setOverScrollMode(int)
     */
    public static final int OVER_SCROLL_NEVER = 2;

    @MagicConstant(flags = {SCROLL_AXIS_NONE, SCROLL_AXIS_HORIZONTAL, SCROLL_AXIS_VERTICAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScrollAxis {
    }

    /**
     * Indicates no axis of view scrolling.
     */
    public static final int SCROLL_AXIS_NONE = 0;

    /**
     * Indicates scrolling along the horizontal axis.
     */
    public static final int SCROLL_AXIS_HORIZONTAL = 1;

    /**
     * Indicates scrolling along the vertical axis.
     */
    public static final int SCROLL_AXIS_VERTICAL = 1 << 1;

    @MagicConstant(intValues = {TYPE_TOUCH, TYPE_NON_TOUCH})
    @Retention(RetentionPolicy.SOURCE)
    public @interface NestedScrollType {
    }

    /**
     * Indicates that the input type for the gesture is from a user touching the screen.
     */
    public static final int TYPE_TOUCH = 0;

    /**
     * Indicates that the input type for the gesture is caused by something which is not a user
     * touching a screen. This is usually from a fling which is settling.
     */
    public static final int TYPE_NON_TOUCH = 1;

    /**
     * Controls the over-scroll mode for this view.
     * See {@link #overScrollBy(int, int, int, int, int, int, int, int, boolean)},
     * {@link #OVER_SCROLL_ALWAYS}, {@link #OVER_SCROLL_IF_CONTENT_SCROLLS},
     * and {@link #OVER_SCROLL_NEVER}.
     */
    private int mOverScrollMode;

    int mPrivateFlags;
    int mPrivateFlags2;
    int mPrivateFlags3;
    int mPrivateFlags4;

    /**
     * The parent this view is attached to.
     * {@hide}
     *
     * @see #getParent()
     */
    ViewParent mParent;

    /**
     * {@hide}
     * <p>
     * Not available for general use. If you need help, hang up and then dial one of the following
     * public APIs:
     *
     * @see #isAttachedToWindow() for current attach state
     * @see #onAttachedToWindow() for subclasses performing work when becoming attached
     * @see #onDetachedFromWindow() for subclasses performing work when becoming detached
     * @see OnAttachStateChangeListener for other code performing work on attach/detach
     * @see #getHandler() for posting messages to this view's UI thread/looper
     * @see #getParent() for interacting with the parent chain
     * @see #getRootView() for the view at the root of the attached hierarchy
     * @see #hasWindowFocus() for whether the attached window is currently focused
     * @see #getWindowVisibility() for checking the visibility of the attached window
     */
    AttachInfo mAttachInfo;

    /**
     * Reference count for transient state.
     *
     * @see #setHasTransientState(boolean)
     */
    int mTransientStateCount = 0;

    /**
     * Count of how many windows this view has been attached to.
     */
    int mWindowAttachCount;

    /**
     * The layout parameters associated with this view and used by the parent
     * {@link ViewGroup} to determine how this view should be
     * laid out.
     * <p>
     * The field should not be used directly. Instead {@link #getLayoutParams()} and {@link
     * #setLayoutParams(ViewGroup.LayoutParams)} should be used. The setter guarantees internal
     * state correctness of the class.
     * {@hide}
     */
    ViewGroup.LayoutParams mLayoutParams;

    /**
     * The view flags hold various views states.
     * <p>
     * Use {@link #setTransitionVisibility(int)} to change the visibility of this view without
     * triggering updates.
     * {@hide}
     */
    int mViewFlags;

    /**
     * Map used to store views' tags.
     */
    private SparseArray<Object> mKeyedTags;

    /**
     * The view's identifier.
     * {@hide}
     *
     * @see #setId(int)
     * @see #getId()
     */
    int mID = NO_ID;

    /**
     * RenderNode holding View properties, potentially holding a DisplayList of View content.
     * <p>
     * When non-null and valid, this is expected to contain an up-to-date copy
     * of the View content. Its DisplayList content is cleared on temporary detach and reset on
     * cleanup.
     */
    final RenderNode mRenderNode = new RenderNode();

    /**
     * The opacity of the View. This is a value from 0 to 1, where 0 means
     * completely transparent and 1 means completely opaque.
     */
    private float mAlpha = 1f;

    /**
     * The opacity of the view as manipulated by the Fade transition. This is a
     * property only used by transitions, which is composited with the other alpha
     * values to calculate the final visual alpha value (offscreen rendering).
     */
    private float mTransitionAlpha = 1f;

    /**
     * The distance in pixels from the left edge of this view's parent
     * to the left edge of this view.
     * {@hide}
     */
    int mLeft;
    /**
     * The distance in pixels from the left edge of this view's parent
     * to the right edge of this view.
     * {@hide}
     */
    int mRight;
    /**
     * The distance in pixels from the top edge of this view's parent
     * to the top edge of this view.
     * {@hide}
     */
    int mTop;
    /**
     * The distance in pixels from the top edge of this view's parent
     * to the bottom edge of this view.
     * {@hide}
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
     * The right padding after RTL resolution, but before taking account of scroll bars.
     *
     * @hide
     */
    int mUserPaddingRight;

    /**
     * The resolved bottom padding before taking account of scroll bars.
     *
     * @hide
     */
    int mUserPaddingBottom;

    /**
     * The left padding after RTL resolution, but before taking account of scroll bars.
     *
     * @hide
     */
    int mUserPaddingLeft;

    /**
     * Cache the paddingStart set by the user to append to the scrollbar's size.
     */
    int mUserPaddingStart;

    /**
     * Cache the paddingEnd set by the user to append to the scrollbar's size.
     */
    int mUserPaddingEnd;

    /**
     * The left padding as set by a setter method, a background's padding.
     * This value is the padding before LTR resolution or taking account of scrollbars.
     *
     * @hide
     */
    int mUserPaddingLeftInitial;

    /**
     * The right padding as set by a setter method, a background's padding.
     * This value is the padding before LTR resolution or taking account of scrollbars.
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
     * or by setPadding(...) or setRelativePadding(...)
     */
    private boolean mLeftPaddingDefined = false;

    /**
     * Cache if a right padding has been defined explicitly via padding, horizontal padding,
     * or by setPadding(...) or setRelativePadding(...)
     */
    private boolean mRightPaddingDefined = false;

    /**
     * Width as measured during measure pass.
     * {@hide}
     */
    int mMeasuredWidth;

    /**
     * Height as measured during measure pass.
     * {@hide}
     */
    int mMeasuredHeight;

    /**
     * @hide
     */
    int mOldWidthMeasureSpec = Integer.MIN_VALUE;
    /**
     * @hide
     */
    int mOldHeightMeasureSpec = Integer.MIN_VALUE;

    private Drawable mBackground;
    private boolean mBackgroundSizeChanged;

    private ForegroundInfo mForegroundInfo;

    private Drawable mScrollIndicatorDrawable;

    /**
     * Whether this View should use a default focus highlight when it gets focused but doesn't
     * have {@link icyllis.modernui.R.attr#state_focused} defined in its background.
     */
    boolean mDefaultFocusHighlightEnabled = true;

    /**
     * The default focus highlight.
     *
     * @see #mDefaultFocusHighlightEnabled
     * @see Drawable#hasFocusStateSpecified()
     */
    private Drawable mDefaultFocusHighlight;
    private Drawable mDefaultFocusHighlightCache;
    private boolean mDefaultFocusHighlightSizeChanged;

    private String mTransitionName;

    ListenerInfo mListenerInfo;

    // Temporary values used to hold (x,y) coordinates when delegating from the
    // two-arg performLongClick() method to the legacy no-arg version.
    private float mLongClickX = Float.NaN;
    private float mLongClickY = Float.NaN;

    private ScrollCache mScrollCache;

    private int[] mDrawableState = null;

    /**
     * Animator that automatically runs based on state changes.
     */
    private StateListAnimator mStateListAnimator;

    /**
     * When this view has focus and the next focus is {@link #FOCUS_LEFT},
     * the user may specify which view to go to next.
     */
    private int mNextFocusLeftId = View.NO_ID;

    /**
     * When this view has focus and the next focus is {@link #FOCUS_RIGHT},
     * the user may specify which view to go to next.
     */
    private int mNextFocusRightId = View.NO_ID;

    /**
     * When this view has focus and the next focus is {@link #FOCUS_UP},
     * the user may specify which view to go to next.
     */
    private int mNextFocusUpId = View.NO_ID;

    /**
     * When this view has focus and the next focus is {@link #FOCUS_DOWN},
     * the user may specify which view to go to next.
     */
    private int mNextFocusDownId = View.NO_ID;

    /**
     * When this view has focus and the next focus is {@link #FOCUS_FORWARD},
     * the user may specify which view to go to next.
     */
    int mNextFocusForwardId = View.NO_ID;

    /**
     * User-specified next keyboard navigation cluster in the {@link #FOCUS_FORWARD} direction.
     *
     * @see #findUserSetNextKeyboardNavigationCluster(View, int)
     */
    int mNextClusterForwardId = View.NO_ID;

    private CheckForLongPress mPendingCheckForLongPress;
    private CheckForTap mPendingCheckForTap;
    private PerformClick mPerformClick;
    private UnsetPressedState mUnsetPressedState;

    /**
     * Whether the long press's action has been invoked.  The tap's action is invoked on the
     * up event while a long press is invoked as soon as the long press duration is reached, so
     * a long press could be performed before the tap is checked, in which case the tap's action
     * should not be invoked.
     */
    private boolean mHasPerformedLongPress;

    /**
     * Whether a context click button is currently pressed down. This is true when the stylus is
     * touching the screen and the primary button has been pressed, or if a mouse's right button is
     * pressed. This is false once the button is released or if the stylus has been lifted.
     */
    private boolean mInContextButtonPress;

    /**
     * Whether the next up event should be ignored for the purposes of gesture recognition. This is
     * true after a stylus button press has occurred, when the next up event should not be recognized
     * as a tap.
     */
    private boolean mIgnoreNextUpEvent;

    /**
     * The minimum height of the view. We'll try our best to have the height
     * of this view to at least this amount.
     */
    private int mMinHeight;

    /**
     * The minimum width of the view. We'll try our best to have the width
     * of this view to at least this amount.
     */
    private int mMinWidth;

    /**
     * Special tree observer used when mAttachInfo is null.
     */
    private ViewTreeObserver mFloatingTreeObserver;

    /**
     * The currently active parent view for receiving delegated nested scrolling events.
     * This is set by {@link #startNestedScroll(int, int)} during a touch interaction and cleared
     * by {@link #stopNestedScroll(int)} at the same point where we clear
     * requestDisallowInterceptTouchEvent.
     */
    private ViewParent mNestedScrollingParentTouch;
    private ViewParent mNestedScrollingParentNonTouch;

    private int[] mTempNestedScrollConsumed;

    /**
     * Queue of pending runnables. Used to postpone calls to post() until this
     * view is attached and has a handler.
     */
    private HandlerActionQueue mRunQueue;

    /**
     * Simple constructor to use when creating a view from code.
     */
    public View() {
        mViewFlags = SOUND_EFFECTS_ENABLED | HAPTIC_FEEDBACK_ENABLED | FOCUSABLE_AUTO;
        // Set some flags defaults
        mPrivateFlags2 =
                (LAYOUT_DIRECTION_DEFAULT << PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT) |
                        (TEXT_DIRECTION_DEFAULT << PFLAG2_TEXT_DIRECTION_MASK_SHIFT) |
                        (PFLAG2_TEXT_DIRECTION_RESOLVED_DEFAULT) |
                        (TEXT_ALIGNMENT_DEFAULT << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT) |
                        (PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT);

        setOverScrollMode(OVER_SCROLL_IF_CONTENT_SCROLLS);
        mUserPaddingStart = UNDEFINED_PADDING;
        mUserPaddingEnd = UNDEFINED_PADDING;
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

        float alpha = mAlpha * mTransitionAlpha;
        if (alpha <= 0.001f) {
            // completely transparent
            return;
        }

        computeScroll();
        int sx = mScrollX;
        int sy = mScrollY;

        int saveCount = canvas.save();
        canvas.translate(mLeft, mTop);

        if (mRenderNode.getAnimationMatrix() != null) {
            canvas.concat(mRenderNode.getAnimationMatrix());
        }
        if (!identity) {
            canvas.concat(getMatrix());
        }

        // true if clip region is not empty, or quick rejected
        boolean hasSpace = true;
        if (clip) {
            hasSpace = canvas.clipRect(0, 0, mRight - mLeft, mBottom - mTop);
        }

        canvas.translate(-sx, -sy);

        if (hasSpace) {
            if (alpha < 0.999f) {
                canvas.saveLayer(sx, sy, sx + mRight - mLeft, sy + mBottom - mTop, (int) (alpha * 255));
            }

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
        if (mBackgroundSizeChanged) {
            background.setBounds(0, 0, mRight - mLeft, mBottom - mTop);
            mBackgroundSizeChanged = false;
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
        onDrawScrollIndicators(canvas);
        onDrawScrollBars(canvas);

        final Drawable foreground = mForegroundInfo != null ? mForegroundInfo.mDrawable : null;
        if (foreground != null) {
            if (mForegroundInfo.mBoundsChanged) {
                mForegroundInfo.mBoundsChanged = false;
                final Rect selfBounds = mForegroundInfo.mSelfBounds;
                final Rect overlayBounds = mForegroundInfo.mOverlayBounds;

                if (mForegroundInfo.mInsidePadding) {
                    selfBounds.set(0, 0, getWidth(), getHeight());
                } else {
                    selfBounds.set(getPaddingLeft(), getPaddingTop(),
                            getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
                }

                final int ld = getLayoutDirection();
                Gravity.apply(mForegroundInfo.mGravity, foreground.getIntrinsicWidth(),
                        foreground.getIntrinsicHeight(), selfBounds, overlayBounds, ld);
                foreground.setBounds(overlayBounds);
            }

            foreground.draw(canvas);
        }
    }

    private void onDrawScrollIndicators(@Nonnull Canvas c) {
        if ((mPrivateFlags3 & SCROLL_INDICATORS_PFLAG3_MASK) == 0) {
            // No scroll indicators enabled.
            return;
        }

        final Drawable dr = mScrollIndicatorDrawable;
        if (dr == null) {
            // Scroll indicators aren't supported here.
            return;
        }

        if (mAttachInfo == null) {
            // View is not attached.
            return;
        }

        final int h = dr.getIntrinsicHeight();
        final int w = dr.getIntrinsicWidth();
        final Rect rect = mAttachInfo.mTmpInvalRect;
        getScrollIndicatorBounds(rect);

        if ((mPrivateFlags3 & PFLAG3_SCROLL_INDICATOR_TOP) != 0) {
            final boolean canScrollUp = canScrollVertically(-1);
            if (canScrollUp) {
                dr.setBounds(rect.left, rect.top, rect.right, rect.top + h);
                dr.draw(c);
            }
        }

        if ((mPrivateFlags3 & PFLAG3_SCROLL_INDICATOR_BOTTOM) != 0) {
            final boolean canScrollDown = canScrollVertically(1);
            if (canScrollDown) {
                dr.setBounds(rect.left, rect.bottom - h, rect.right, rect.bottom);
                dr.draw(c);
            }
        }

        final int leftRtl;
        final int rightRtl;
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
            leftRtl = PFLAG3_SCROLL_INDICATOR_END;
            rightRtl = PFLAG3_SCROLL_INDICATOR_START;
        } else {
            leftRtl = PFLAG3_SCROLL_INDICATOR_START;
            rightRtl = PFLAG3_SCROLL_INDICATOR_END;
        }

        final int leftMask = PFLAG3_SCROLL_INDICATOR_LEFT | leftRtl;
        if ((mPrivateFlags3 & leftMask) != 0) {
            final boolean canScrollLeft = canScrollHorizontally(-1);
            if (canScrollLeft) {
                dr.setBounds(rect.left, rect.top, rect.left + w, rect.bottom);
                dr.draw(c);
            }
        }

        final int rightMask = PFLAG3_SCROLL_INDICATOR_RIGHT | rightRtl;
        if ((mPrivateFlags3 & rightMask) != 0) {
            final boolean canScrollRight = canScrollHorizontally(1);
            if (canScrollRight) {
                dr.setBounds(rect.right - w, rect.top, rect.right, rect.bottom);
                dr.draw(c);
            }
        }
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
            long currentTime = AnimationUtils.currentAnimationTimeMillis();
            float fraction = (float) (currentTime - cache.mFadeStartTime) / cache.mFadeDuration;
            if (fraction >= 1.0f) {
                cache.mState = ScrollCache.OFF;
                return;
            } else {
                int alpha = 255 - (int) (fraction * 255);
                cache.mScrollBar.mutate().setAlpha(alpha);
            }
            invalidate = true;
        } else {
            cache.mScrollBar.mutate().setAlpha(255);
        }

        final boolean drawHorizontalScrollBar = isHorizontalScrollBarEnabled();
        final boolean drawVerticalScrollBar = isVerticalScrollBarEnabled()
                && !isVerticalScrollBarHidden();

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

    /**
     * Specifies the rectangle area of this view and all its descendants.
     * <p>
     * Derived classes should NOT override this method for any reason.
     * Derived classes with children should override {@link #onLayout(boolean, int, int, int, int)}.
     * In that method, they should call layout() on each of their children.
     *
     * @param l left position, relative to parent
     * @param t top position, relative to parent
     * @param r right position, relative to parent
     * @param b bottom position, relative to parent
     */
    @SuppressWarnings("unchecked")
    public void layout(int l, int t, int r, int b) {
        int oldL = mLeft;
        int oldT = mTop;
        int oldB = mBottom;
        int oldR = mRight;

        boolean changed = setFrame(l, t, r, b);

        if (changed || (mPrivateFlags & PFLAG_LAYOUT_REQUIRED) != 0) {
            onLayout(changed, l, t, r, b);

            mPrivateFlags &= ~PFLAG_LAYOUT_REQUIRED;

            ListenerInfo li = mListenerInfo;
            if (li != null && li.mOnLayoutChangeListeners != null) {
                for (var listener : (ArrayList<OnLayoutChangeListener>) li.mOnLayoutChangeListeners.clone()) {
                    listener.onLayoutChange(this, l, t, r, b, oldL, oldT, oldR, oldB);
                }
            }
        }

        mPrivateFlags &= ~PFLAG_FORCE_LAYOUT;
        mPrivateFlags3 |= PFLAG3_IS_LAID_OUT;
    }

    /**
     * Called from {@link #layout(int, int, int, int)} when this view should
     * assign a size and position to each of its children.
     * <p>
     * Derived classes with children should override this method and call layout
     * on each of their children.
     *
     * @param changed This is a new size or position for this view
     * @param left    Left position, relative to parent
     * @param top     Top position, relative to parent
     * @param right   Right position, relative to parent
     * @param bottom  Bottom position, relative to parent
     */
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    }

    /**
     * Assign a size and position to this view.
     * <p>
     * Called from {@link #layout(int, int, int, int)}.
     *
     * @param left   left position, relative to parent
     * @param top    top position, relative to parent
     * @param right  right position, relative to parent
     * @param bottom bottom position, relative to parent
     * @return whether the area of this view was changed
     */
    protected boolean setFrame(int left, int top, int right, int bottom) {
        if (mLeft != left || mRight != right || mTop != top || mBottom != bottom) {
            // Remember our drawn bit
            int drawn = mPrivateFlags & PFLAG_DRAWN;

            int oldWidth = mRight - mLeft;
            int oldHeight = mBottom - mTop;
            int newWidth = right - left;
            int newHeight = bottom - top;
            boolean sizeChanged = (newWidth != oldWidth) || (newHeight != oldHeight);

            invalidate();

            mLeft = left;
            mTop = top;
            mRight = right;
            mBottom = bottom;
            mRenderNode.setPosition(mLeft, mTop, mRight, mBottom);

            mPrivateFlags |= PFLAG_HAS_BOUNDS;

            if (sizeChanged) {
                sizeChange(newWidth, newHeight, oldWidth, oldHeight);
            }

            // If we are visible, force the DRAWN bit to on so that
            // this invalidate will go through (at least to our parent).
            // This is because someone may have invalidated this view
            // before this call to setFrame came in, thereby clearing
            // the DRAWN bit.
            mPrivateFlags |= PFLAG_DRAWN;

            // Reset drawn bit to original value (invalidate turns it off)
            mPrivateFlags |= drawn;

            mDefaultFocusHighlightSizeChanged = true;
            mBackgroundSizeChanged = true;
            if (mForegroundInfo != null) {
                mForegroundInfo.mBoundsChanged = true;
            }
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
        boolean needsLayout = (mPrivateFlags & PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT;

        if (!needsLayout) {
            // Optimize layout by avoiding an extra EXACTLY pass when the view is
            // already measured as the correct size. In API 23 and below, this
            // extra pass is required to make LinearLayout re-distribute weight.
            final boolean specChanged = widthMeasureSpec != mOldWidthMeasureSpec
                    || heightMeasureSpec != mOldHeightMeasureSpec;
            final boolean isSpecExactly = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY
                    && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY;
            final boolean matchesSpecSize = getMeasuredWidth() == MeasureSpec.getSize(widthMeasureSpec)
                    && getMeasuredHeight() == MeasureSpec.getSize(heightMeasureSpec);
            needsLayout |= specChanged && (!isSpecExactly || !matchesSpecSize);
        }

        if (needsLayout) {
            // remove the flag first anyway
            mPrivateFlags &= ~PFLAG_MEASURED_DIMENSION_SET;

            resolveRtlPropertiesIfNeeded();

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
     * Utility to get the size in pixels that matches the view layout standards.
     *
     * @param v scaling-independent pixel, relative to other views
     * @return converted size in pixels
     */
    public static int dp(float v) {
        return ViewConfiguration.get().dp(v);
    }

    /**
     * Utility to get the size in pixels that matches the text layout standards.
     *
     * @param v scaling-independent pixel, relative to other texts
     * @return converted size in pixels
     */
    public static int sp(float v) {
        return ViewConfiguration.get().sp(v);
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
        return (mBackground == null) ? mMinHeight : Math.max(mMinHeight, mBackground.getMinimumHeight());
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
        return (mBackground == null) ? mMinWidth : Math.max(mMinWidth, mBackground.getMinimumWidth());
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
     * {@hide}
     *
     * @param isRoot true if the view belongs to the root namespace, false
     *               otherwise
     */
    public void setIsRootNamespace(boolean isRoot) {
        if (isRoot) {
            mPrivateFlags |= PFLAG_IS_ROOT_NAMESPACE;
        } else {
            mPrivateFlags &= ~PFLAG_IS_ROOT_NAMESPACE;
        }
    }

    /**
     * {@hide}
     *
     * @return true if the view belongs to the root namespace, false otherwise
     */
    public boolean isRootNamespace() {
        return (mPrivateFlags & PFLAG_IS_ROOT_NAMESPACE) != 0;
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
        return mID;
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
        mID = id;
    }

    /**
     * Returns the tag associated with this view and the specified key.
     *
     * @param key The key identifying the tag
     * @return the Object stored in this view as a tag, or {@code null} if not set
     * @see #setTag(int, Object)
     */
    @Nullable
    public Object getTag(int key) {
        if (mKeyedTags != null) return mKeyedTags.get(key);
        return null;
    }

    /**
     * Sets a tag associated with this view and a key. A tag can be used
     * to mark a view in its hierarchy and does not have to be unique within
     * the hierarchy. Tags can also be used to store data within a view
     * without resorting to another data structure.
     *
     * @param key The key identifying the tag
     * @param tag An Object to tag the view with
     * @throws IllegalArgumentException If they specified key is not valid
     * @see #getTag(int)
     */
    public void setTag(int key, Object tag) {
        if ((key & 0xFF000000) == 0) {
            throw new IllegalArgumentException("The key must be a valid resource id.");
        }
        if (mKeyedTags == null) {
            mKeyedTags = new SparseArray<>(2);
        }
        mKeyedTags.put(key, tag);
    }

    /**
     * Returns the visibility status for this view.
     *
     * @return One of {@link #VISIBLE}, {@link #INVISIBLE}, or {@link #GONE}.
     */
    @Visibility
    public int getVisibility() {
        return mViewFlags & VISIBILITY_MASK;
    }

    /**
     * Set the visibility state of this view.
     *
     * @param visibility One of {@link #VISIBLE}, {@link #INVISIBLE}, or {@link #GONE}.
     */
    public void setVisibility(@Visibility int visibility) {
        setFlags(visibility, VISIBILITY_MASK);
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

        // Invalidate too, since the default behavior for views is to
        // be drawn at 50% alpha rather than to change the drawable.
        invalidate();

        if (!enabled) {
            cancelPendingInputEvents();
        }
    }

    /**
     * Returns whether this View is currently able to take focus.
     *
     * @return True if this view can take focus, or false otherwise.
     */
    public final boolean isFocusable() {
        return (mViewFlags & FOCUSABLE) == FOCUSABLE;
    }

    /**
     * Returns the focusable setting for this view.
     *
     * @return One of {@link #NOT_FOCUSABLE}, {@link #FOCUSABLE}, or {@link #FOCUSABLE_AUTO}.
     */
    @Focusable
    public int getFocusable() {
        return (mViewFlags & FOCUSABLE_AUTO) > 0 ? FOCUSABLE_AUTO : mViewFlags & FOCUSABLE;
    }

    /**
     * Set whether this view can receive the focus.
     * <p>
     * Setting this to false will also ensure that this view is not focusable
     * in touch mode.
     *
     * @param focusable If true, this view can receive the focus.
     * @see #setFocusable(int)
     */
    public void setFocusable(boolean focusable) {
        setFocusable(focusable ? FOCUSABLE : NOT_FOCUSABLE);
    }

    /**
     * Sets whether this view can receive focus.
     * <p>
     * Setting this to {@link #FOCUSABLE_AUTO} tells the framework to determine focusability
     * automatically based on the view's interactivity. This is the default.
     * <p>
     * Setting this to NOT_FOCUSABLE will ensure that this view is also not focusable
     * in touch mode.
     *
     * @param focusable One of {@link #NOT_FOCUSABLE}, {@link #FOCUSABLE},
     *                  or {@link #FOCUSABLE_AUTO}.
     * @see #setFocusableInTouchMode(boolean)
     */
    public void setFocusable(@Focusable int focusable) {
        if ((focusable & (FOCUSABLE_AUTO | FOCUSABLE)) == 0) {
            setFlags(0, FOCUSABLE_IN_TOUCH_MODE);
        }
        setFlags(focusable, FOCUSABLE_MASK);
    }

    /**
     * When a view is focusable, it may not want to take focus when in touch mode.
     * For example, a button would like focus when the user is navigating via a D-pad
     * so that the user can click on it, but once the user starts touching the screen,
     * the button shouldn't take focus
     *
     * @return Whether the view is focusable in touch mode.
     */
    public final boolean isFocusableInTouchMode() {
        return (mViewFlags & FOCUSABLE_IN_TOUCH_MODE) == FOCUSABLE_IN_TOUCH_MODE;
    }

    /**
     * Set whether this view can receive focus while in touch mode.
     * <p>
     * Setting this to true will also ensure that this view is focusable.
     *
     * @param focusableInTouchMode If true, this view can receive the focus while
     *                             in touch mode.
     * @see #setFocusable(boolean)
     */
    public void setFocusableInTouchMode(boolean focusableInTouchMode) {
        // Focusable in touch mode should always be set before the focusable flag
        // otherwise, setting the focusable flag will trigger a focusableViewAvailable()
        // which, in touch mode, will not successfully request focus on this view
        // because the focusable in touch mode flag is not set
        setFlags(focusableInTouchMode ? FOCUSABLE_IN_TOUCH_MODE : 0, FOCUSABLE_IN_TOUCH_MODE);

        // Clear FOCUSABLE_AUTO if set.
        if (focusableInTouchMode) {
            // Clears FOCUSABLE_AUTO if set.
            setFlags(FOCUSABLE, FOCUSABLE_MASK);
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
     * Indicates whether this view reacts to long click events or not.
     *
     * @return true if the view is long clickable, false otherwise
     * @see #setLongClickable(boolean)
     */
    public boolean isLongClickable() {
        return (mViewFlags & LONG_CLICKABLE) == LONG_CLICKABLE;
    }

    /**
     * Enables or disables long click events for this view. When a view is long
     * clickable it reacts to the user holding down the button for a longer
     * duration than a tap. This event can either launch the listener or a
     * context menu.
     *
     * @param longClickable true to make the view long clickable, false otherwise
     * @see #isLongClickable()
     */
    public void setLongClickable(boolean longClickable) {
        setFlags(longClickable ? LONG_CLICKABLE : 0, LONG_CLICKABLE);
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
     * Sets the pressed state for this view and provides a touch coordinate for
     * animation hinting.
     *
     * @param x The x coordinate of the touch that caused the press
     * @param y The y coordinate of the touch that caused the press
     */
    private void setPressed(float x, float y) {
        drawableHotspotChanged(x, y);

        setPressed(true);
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
     * Changes the selection state of this view. A view can be selected or not.
     * Note that selection is not the same as focus. Views are typically
     * selected in the context of an AdapterView like ListView or GridView;
     * the selected view is the view that is highlighted.
     *
     * @param selected true if the view must be selected, false otherwise
     */
    public void setSelected(boolean selected) {
        //noinspection DoubleNegation
        if (((mPrivateFlags & PFLAG_SELECTED) != 0) != selected) {
            mPrivateFlags = (mPrivateFlags & ~PFLAG_SELECTED) | (selected ? PFLAG_SELECTED : 0);
            if (!selected) resetPressedState();
            invalidate();
            refreshDrawableState();
            dispatchSetSelected(selected);
        }
    }

    /**
     * Dispatch setSelected to all of this View's children.
     *
     * @param selected The new selected state
     * @see #setSelected(boolean)
     */
    protected void dispatchSetSelected(boolean selected) {
    }

    /**
     * Indicates the selection state of this view.
     *
     * @return true if the view is selected, false otherwise
     */
    public boolean isSelected() {
        return (mPrivateFlags & PFLAG_SELECTED) != 0;
    }

    /**
     * Changes the activated state of this view. A view can be activated or not.
     * Note that activation is not the same as selection.  Selection is
     * a transient property, representing the view (hierarchy) the user is
     * currently interacting with.  Activation is a longer-term state that the
     * user can move views in and out of.  For example, in a list view with
     * single or multiple selection enabled, the views in the current selection
     * set are activated.  (Um, yeah, we are deeply sorry about the terminology
     * here.)  The activated state is propagated down to children of the view it
     * is set on.
     *
     * @param activated true if the view must be activated, false otherwise
     */
    public void setActivated(boolean activated) {
        //noinspection DoubleNegation
        if (((mPrivateFlags & PFLAG_ACTIVATED) != 0) != activated) {
            mPrivateFlags = (mPrivateFlags & ~PFLAG_ACTIVATED) | (activated ? PFLAG_ACTIVATED : 0);
            invalidate();
            refreshDrawableState();
            dispatchSetActivated(activated);
        }
    }

    /**
     * Dispatch setActivated to all of this View's children.
     *
     * @param activated The new activated state
     * @see #setActivated(boolean)
     */
    protected void dispatchSetActivated(boolean activated) {
    }

    /**
     * Indicates the activation state of this view.
     *
     * @return true if the view is activated, false otherwise
     */
    public boolean isActivated() {
        return (mPrivateFlags & PFLAG_ACTIVATED) != 0;
    }

    private boolean hasSize() {
        return (mBottom > mTop) && (mRight > mLeft);
    }

    private boolean canTakeFocus() {
        return ((mViewFlags & VISIBILITY_MASK) == VISIBLE)
                && ((mViewFlags & FOCUSABLE) == FOCUSABLE)
                && ((mViewFlags & ENABLED_MASK) == ENABLED)
                && (!isLayoutValid() || hasSize());
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
        int privateFlags = mPrivateFlags;
        boolean shouldNotifyFocusableAvailable = false;

        // If focusable is auto, update the FOCUSABLE bit.
        int focusableChangedByAuto;
        if (((mViewFlags & FOCUSABLE_AUTO) != 0)
                && (changed & (FOCUSABLE_MASK | CLICKABLE)) != 0) {
            // Heuristic only takes into account whether view is clickable.
            final int newFocus;
            if ((mViewFlags & CLICKABLE) != 0) {
                newFocus = FOCUSABLE;
            } else {
                newFocus = NOT_FOCUSABLE;
            }
            mViewFlags = (mViewFlags & ~FOCUSABLE) | newFocus;
            focusableChangedByAuto = (old & FOCUSABLE) ^ (newFocus & FOCUSABLE);
            changed = (changed & ~FOCUSABLE) | focusableChangedByAuto;
        }

        /* Check if the FOCUSABLE bit has changed */
        if (((changed & FOCUSABLE) != 0) && ((privateFlags & PFLAG_HAS_BOUNDS) != 0)) {
            if (((old & FOCUSABLE) == FOCUSABLE)
                    && ((privateFlags & PFLAG_FOCUSED) != 0)) {
                /* Give up focus if we are no longer focusable */
                clearFocus();
                /*if (mParent instanceof ViewGroup) {
                    ((ViewGroup) mParent).clearFocusedInCluster();
                }*/
            } else if (((old & FOCUSABLE) == NOT_FOCUSABLE)
                    && ((privateFlags & PFLAG_FOCUSED) == 0)) {
                /*
                 * Tell the view system that we are now available to take focus
                 * if no one else already has it.
                 */
                if (mParent != null) {
                    shouldNotifyFocusableAvailable = canTakeFocus();
                }
            }
        }

        final int newVisibility = flags & VISIBILITY_MASK;
        if (newVisibility == VISIBLE) {
            if ((changed & VISIBILITY_MASK) != 0) {
                /*
                 * If this view is becoming visible, invalidate it in case it changed while
                 * it was not visible. Marking it drawn ensures that the invalidation will
                 * go through.
                 */
                mPrivateFlags |= PFLAG_DRAWN;
                invalidate();

                // a view becoming visible is worth notifying the parent about in case nothing has
                // focus. Even if this specific view isn't focusable, it may contain something that
                // is, so let the root view try to give this focus if nothing else does.
                shouldNotifyFocusableAvailable = hasSize();
            }
        }

        if ((changed & ENABLED_MASK) != 0) {
            if ((mViewFlags & ENABLED_MASK) == ENABLED) {
                // a view becoming enabled should notify the parent as long as the view is also
                // visible and the parent wasn't already notified by becoming visible during this
                // setFlags invocation.
                shouldNotifyFocusableAvailable = canTakeFocus();
            } else {
                if (isFocused()) {
                    clearFocus();
                }
            }
        }

        if (shouldNotifyFocusableAvailable && mParent != null) {
            mParent.focusableViewAvailable(this);
        }

        /* Check if the GONE bit has changed */
        if ((changed & GONE) != 0) {
            requestLayout();

            if (((mViewFlags & VISIBILITY_MASK) == GONE)) {
                if (hasFocus()) {
                    clearFocus();
                    /*if (mParent instanceof ViewGroup) {
                        ((ViewGroup) mParent).clearFocusedInCluster();
                    }*/
                }
                if (mParent instanceof View) {
                    // GONE views noop invalidation, so invalidate the parent
                    ((View) mParent).invalidate();
                }
                // Mark the view drawn to ensure that it gets invalidated properly the next
                // time it is visible and gets invalidated
                mPrivateFlags |= PFLAG_DRAWN;
            }
            if (mAttachInfo != null) {
                mAttachInfo.mViewVisibilityChanged = true;
            }
        }

        /* Check if the VISIBLE bit has changed */
        if ((changed & INVISIBLE) != 0) {
            /*
             * If this view is becoming invisible, set the DRAWN flag so that
             * the next invalidate() will not be skipped.
             */
            mPrivateFlags |= PFLAG_DRAWN;

            if (((mViewFlags & VISIBILITY_MASK) == INVISIBLE)) {
                // root view becoming invisible shouldn't clear focus and accessibility focus
                if (getRootView() != this) {
                    if (hasFocus()) {
                        clearFocus();
                        /*if (mParent instanceof ViewGroup) {
                            ((ViewGroup) mParent).clearFocusedInCluster();
                        }*/
                    }
                }
            }
            if (mAttachInfo != null) {
                mAttachInfo.mViewVisibilityChanged = true;
            }
        }

        if ((changed & VISIBILITY_MASK) != 0) {
            // If the view is invisible, cleanup its display list to free up resources
            if (newVisibility != VISIBLE && mAttachInfo != null) {
                //cleanupDraw();
            }

            if (mParent instanceof ViewGroup parent) {
                parent.onChildVisibilityChanged(this, (changed & VISIBILITY_MASK),
                        newVisibility);
                parent.invalidate();
            }

            if (mAttachInfo != null) {
                dispatchVisibilityChanged(this, newVisibility);

                // Aggregated visibility changes are dispatched to attached views
                // in visible windows where the parent is currently shown/drawn
                // or the parent is not a ViewGroup (and therefore assumed to be a ViewRoot),
                // discounting clipping or overlapping. This makes it a good place
                // to change animation states.
                if (mParent != null && getWindowVisibility() == VISIBLE &&
                        ((!(mParent instanceof ViewGroup)) || ((ViewGroup) mParent).isShown())) {
                    dispatchVisibilityAggregated(newVisibility == VISIBLE);
                }
            }
        }

        if ((changed & DRAW_MASK) != 0) {
            if ((mViewFlags & WILL_NOT_DRAW) != 0) {
                if (mBackground != null
                        || mDefaultFocusHighlight != null
                        || (mForegroundInfo != null && mForegroundInfo.mDrawable != null)) {
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
     * Invalidate the whole view. If the view is visible, {@link #onDraw(Canvas)}
     * will be called at some point in the future.
     * <p>
     * This must be called from a UI thread. To call from a non-UI thread, call
     * {@link #postInvalidate()}.
     */
    public final void invalidate() {
        if ((mViewFlags & VISIBILITY_MASK) != VISIBLE &&
                (!(mParent instanceof ViewGroup) ||
                        !((ViewGroup) mParent).isViewTransitioning(this))) {
            return;
        }

        if (mAttachInfo != null) {
            mAttachInfo.mViewRoot.invalidate();
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
     * Schedules an action on a drawable to occur at a specified time.
     *
     * @param who  the recipient of the action
     * @param what the action to run on the drawable
     * @param when the time at which the action must occur. Uses the
     *             {@link ArchCore#timeMillis()} timebase.
     */
    @Override
    public final void scheduleDrawable(@Nonnull Drawable who, @Nonnull Runnable what, long when) {
        if (verifyDrawable(who)) {
            // Postpone the runnable until we know
            // on which thread it needs to run.
            final long delay = when - ArchCore.timeMillis();
            if (mAttachInfo != null) {
                mAttachInfo.mViewRoot.mChoreographer.postCallbackDelayed(
                        Choreographer.CALLBACK_ANIMATION, what, who, delay);
            } else {
                getRunQueue().postDelayed(what, delay);
            }
        }
    }

    /**
     * Cancels a scheduled action on a drawable.
     *
     * @param who  the recipient of the action
     * @param what the action to cancel
     */
    @Override
    public final void unscheduleDrawable(@Nonnull Drawable who, @Nonnull Runnable what) {
        if (verifyDrawable(who)) {
            if (mAttachInfo != null) {
                mAttachInfo.mViewRoot.mChoreographer.removeCallbacks(
                        Choreographer.CALLBACK_ANIMATION, what, who);
            }
            if (mRunQueue != null) {
                mRunQueue.removeCallbacks(what);
            }
        }
    }

    /**
     * Unschedule any events associated with the given Drawable.  This can be
     * used when selecting a new Drawable into a view, so that the previous
     * one is completely unscheduled.
     *
     * @param who The Drawable to unschedule.
     * @see #drawableStateChanged
     */
    public final void unscheduleDrawable(@Nullable Drawable who) {
        if (mAttachInfo != null && who != null) {
            mAttachInfo.mViewRoot.mChoreographer.removeCallbacks(
                    Choreographer.CALLBACK_ANIMATION, null, who);
        }
    }

    /**
     * Find and return all focusable views that are descendants of this view,
     * possibly including this view if it is focusable itself.
     *
     * @param direction The direction of the focus
     * @return A list of focusable views
     */
    @Nonnull
    public final ArrayList<View> getFocusables(@FocusDirection int direction) {
        ArrayList<View> result = new ArrayList<>();
        addFocusables(result, direction);
        return result;
    }

    /**
     * Add any focusable views that are descendants of this view (possibly
     * including this view if it is focusable itself) to views.  If we are in touch mode,
     * only add views that are also focusable in touch mode.
     *
     * @param views     Focusable views found so far
     * @param direction The direction of the focus
     */
    public final void addFocusables(ArrayList<View> views, @FocusDirection int direction) {
        addFocusables(views, direction, isInTouchMode() ? FOCUSABLES_TOUCH_MODE : FOCUSABLES_ALL);
    }

    /**
     * Adds any focusable views that are descendants of this view (possibly
     * including this view if it is focusable itself) to views. This method
     * adds all focusable views regardless if we are in touch mode or
     * only views focusable in touch mode if we are in touch mode or
     * only views that can take accessibility focus if accessibility is enabled
     * depending on the focusable mode parameter.
     *
     * @param views     Focusable views found so far or null if all we are interested is
     *                  the number of focusables.
     * @param direction The direction of the focus.
     */
    public void addFocusables(@Nonnull ArrayList<View> views, @FocusDirection int direction,
                              @FocusableMode int focusableMode) {
        if (!canTakeFocus()) {
            return;
        }
        if ((focusableMode & FOCUSABLES_TOUCH_MODE) == FOCUSABLES_TOUCH_MODE
                && !isFocusableInTouchMode()) {
            return;
        }
        views.add(this);
    }

    /**
     * Find and return all touchable views that are descendants of this view,
     * possibly including this view if it is touchable itself.
     *
     * @return A list of touchable views
     */
    @Nonnull
    public final ArrayList<View> getTouchables() {
        ArrayList<View> result = new ArrayList<>();
        addTouchables(result);
        return result;
    }

    /**
     * Add any touchable views that are descendants of this view (possibly
     * including this view if it is touchable itself) to views.
     *
     * @param views Touchable views found so far
     */
    public void addTouchables(@Nonnull ArrayList<View> views) {
        final int viewFlags = mViewFlags;

        if (((viewFlags & CLICKABLE) == CLICKABLE || (viewFlags & LONG_CLICKABLE) == LONG_CLICKABLE
                || (viewFlags & CONTEXT_CLICKABLE) == CONTEXT_CLICKABLE)
                && (viewFlags & ENABLED_MASK) == ENABLED) {
            views.add(this);
        }
    }

    /**
     * Call this to try to give focus to a specific view or to one of its
     * descendants.
     * <p>
     * A view will not actually take focus if it is not focusable ({@link #isFocusable} returns
     * false), or if it can't be focused due to other conditions (not visible, not
     * enabled, or has no size).
     * <p>
     * See also {@link #focusSearch(int)}, which is what you call to say that you
     * have focus, and you want your parent to look for the next one.
     * <p>
     * This is equivalent to calling {@link #requestFocus(int)} with arguments
     * {@link #FOCUS_DOWN}.
     *
     * @return Whether this view or one of its descendants actually took focus.
     */
    public final boolean requestFocus() {
        return requestFocus(FOCUS_DOWN);
    }

    /**
     * Gives focus to the default-focus view in the view hierarchy that has this view as a root.
     * If the default-focus view cannot be found, falls back to calling {@link #requestFocus(int)}.
     *
     * @return Whether this view or one of its descendants actually took focus
     */
    public boolean restoreDefaultFocus() {
        return requestFocus(View.FOCUS_DOWN);
    }

    /**
     * Call this to try to give focus to a specific view or to one of its
     * descendants and give it a hint about what direction focus is heading.
     * <p>
     * A view will not actually take focus if it is not focusable
     * ({@link #isFocusable} returns false).
     * <p>
     * See also {@link #focusSearch(int)}, which is what you call to say that you
     * have focus, and you want your parent to look for the next one.
     * <p>
     * This is equivalent to calling this with
     * <code>null</code> set for the previously focused rectangle.
     *
     * @param direction One of FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, and FOCUS_RIGHT
     * @return Whether this view or one of its descendants actually took focus.
     */
    public boolean requestFocus(@FocusRealDirection int direction) {
        return requestFocusNoSearch(direction);
    }

    private boolean requestFocusNoSearch(@FocusRealDirection int direction) {
        // need to be focusable
        if (!canTakeFocus()) {
            return false;
        }

        // need to be focusable in touch mode if in touch mode
        if (!isFocusableInTouchMode()) {
            return false;
        }

        // need to not have any parents blocking us
        ViewParent p = mParent;
        while (p instanceof ViewGroup vg) {
            if (vg.getDescendantFocusability() == ViewGroup.FOCUS_BLOCK_DESCENDANTS) {
                return false;
            } else {
                p = vg.getParent();
            }
        }

        if (!isLayoutValid()) {
            mPrivateFlags |= PFLAG_WANTS_FOCUS;
        } else {
            clearParentsWantFocus();
        }

        handleFocusGainInternal(direction);
        return true;
    }

    /**
     * Give this view focus. This will cause
     * {@link #onFocusChanged(boolean, int)} to be called.
     * <p>
     * Note: this does not check whether this {@link View} should get focus, it just
     * gives it focus no matter what.  It should only be called internally by framework
     * code that knows what it is doing, namely {@link #requestFocus(int)}.
     *
     * @param direction values are {@link View#FOCUS_UP}, {@link View#FOCUS_DOWN},
     *                  {@link View#FOCUS_LEFT} or {@link View#FOCUS_RIGHT}. This is the direction which
     *                  focus moved when requestFocus() is called. It may not always
     *                  apply, in which case use the default View.FOCUS_DOWN.
     */
    void handleFocusGainInternal(@FocusRealDirection int direction) {
        if ((mPrivateFlags & PFLAG_FOCUSED) == 0) {
            mPrivateFlags |= PFLAG_FOCUSED;

            View oldFocus = (mAttachInfo != null) ? getRootView().findFocus() : null;

            if (mParent != null) {
                mParent.requestChildFocus(this, this);
                //updateFocusedInCluster(oldFocus, direction);
            }

            /*if (mAttachInfo != null) {
                mAttachInfo.mTreeObserver.dispatchOnGlobalFocusChange(oldFocus, this);
            }*/

            onFocusChanged(true, direction);
            refreshDrawableState();
        }
    }

    /**
     * Sets this view's preference for reveal behavior when it gains focus.
     *
     * <p>When set to true, this is a signal to ancestor views in the hierarchy that
     * this view would prefer to be brought fully into view when it gains focus.
     * For example, a text field that a user is meant to type into. Other views such
     * as scrolling containers may prefer to opt-out of this behavior.</p>
     *
     * <p>The default value for views is true, though subclasses may change this
     * based on their preferred behavior.</p>
     *
     * @param revealOnFocus true to request reveal on focus in ancestors, false otherwise
     * @see #getRevealOnFocusHint()
     */
    public final void setRevealOnFocusHint(boolean revealOnFocus) {
        if (revealOnFocus) {
            mPrivateFlags3 &= ~PFLAG3_NO_REVEAL_ON_FOCUS;
        } else {
            mPrivateFlags3 |= PFLAG3_NO_REVEAL_ON_FOCUS;
        }
    }

    /**
     * Returns this view's preference for reveal behavior when it gains focus.
     *
     * <p>When this method returns true for a child view requesting focus, ancestor
     * views responding to a focus change in {@link ViewParent#requestChildFocus(View, View)}
     * should make a best effort to make the newly focused child fully visible to the user.
     * When it returns false, ancestor views should preferably not disrupt scroll positioning or
     * other properties affecting visibility to the user as part of the focus change.</p>
     *
     * @return true if this view would prefer to become fully visible when it gains focus,
     * false if it would prefer not to disrupt scroll positioning
     * @see #setRevealOnFocusHint(boolean)
     */
    public final boolean getRevealOnFocusHint() {
        return (mPrivateFlags3 & PFLAG3_NO_REVEAL_ON_FOCUS) == 0;
    }

    /**
     * Request that a rectangle of this view be visible on the screen,
     * scrolling if necessary just enough.
     *
     * <p>A View should call this if it maintains some notion of which part
     * of its content is interesting.  For example, a text editing view
     * should call this when its cursor moves.
     * <p>The Rectangle passed into this method should be in the View's content coordinate space.
     * It should not be affected by which part of the View is currently visible or its scroll
     * position.
     *
     * @param rectangle The rectangle in the View's content coordinate space
     * @return Whether any parent scrolled.
     */
    public boolean requestRectangleOnScreen(Rect rectangle) {
        return requestRectangleOnScreen(rectangle, false);
    }

    /**
     * Request that a rectangle of this view be visible on the screen,
     * scrolling if necessary just enough.
     *
     * <p>A View should call this if it maintains some notion of which part
     * of its content is interesting.  For example, a text editing view
     * should call this when its cursor moves.
     * <p>The Rectangle passed into this method should be in the View's content coordinate space.
     * It should not be affected by which part of the View is currently visible or its scroll
     * position.
     * <p>When <code>immediate</code> is set to true, scrolling will not be
     * animated.
     *
     * @param rectangle The rectangle in the View's content coordinate space
     * @param immediate True to forbid animated scrolling, false otherwise
     * @return Whether any parent scrolled.
     */
    public boolean requestRectangleOnScreen(Rect rectangle, boolean immediate) {
        if (mParent == null) {
            return false;
        }

        View child = this;

        RectF position = (mAttachInfo != null) ? mAttachInfo.mTmpTransformRect : new RectF();
        position.set(rectangle);

        ViewParent parent = mParent;
        boolean scrolled = false;
        while (parent != null) {
            rectangle.set((int) position.left, (int) position.top,
                    (int) position.right, (int) position.bottom);

            scrolled |= parent.requestChildRectangleOnScreen(child, rectangle, immediate);

            if (!(parent instanceof View)) {
                break;
            }

            // move it from child's content coordinate space to parent's content coordinate space
            position.offset(child.mLeft - child.getScrollX(), child.mTop - child.getScrollY());

            child = (View) parent;
            parent = child.getParent();
        }

        return scrolled;
    }

    /**
     * Find the nearest keyboard navigation cluster in the specified direction.
     * This does not actually give focus to that cluster.
     *
     * @param currentCluster The starting point of the search. Null means the current cluster is not
     *                       found yet
     * @param direction      Direction to look
     * @return The nearest keyboard navigation cluster in the specified direction, or null if none
     * can be found
     */
    public View keyboardNavigationClusterSearch(View currentCluster, @FocusDirection int direction) {
        /*if (isKeyboardNavigationCluster()) {
            currentCluster = this;
        }
        if (isRootNamespace()) {
            // Root namespace means we should consider ourselves the top of the
            // tree for group searching; otherwise we could be group searching
            // into other tabs.  see LocalActivityManager and TabHost for more info.
            return FocusFinder.getInstance().findNextKeyboardNavigationCluster(
                    this, currentCluster, direction);
        } else if (mParent != null) {
            return mParent.keyboardNavigationClusterSearch(currentCluster, direction);
        }*/
        return null;
    }

    /**
     * Called when this view wants to give up focus. If focus is cleared
     * {@link #onFocusChanged(boolean, int)} is called.
     * <p>
     * <strong>Note:</strong> When not in touch-mode, the framework will try to give focus
     * to the first focusable View from the top after focus is cleared. Hence, if this
     * View is the first from the top that can take focus, then all callbacks
     * related to clearing focus will be invoked after which the framework will
     * give focus to this view.
     * </p>
     */
    public void clearFocus() {
        clearFocusInternal(true);
    }

    /**
     * Clears focus from the view, optionally broadcasting the change up through
     * the parent hierarchy and requesting that the root view place new focus.
     *
     * @param broadcast whether to broadcast the change up through the parent
     *                  hierarchy
     */
    void clearFocusInternal(boolean broadcast) {
        if ((mPrivateFlags & PFLAG_FOCUSED) != 0) {
            mPrivateFlags &= ~PFLAG_FOCUSED;
            clearParentsWantFocus();

            if (broadcast && mParent != null) {
                mParent.clearChildFocus(this);
            }

            onFocusChanged(false, 0);
            refreshDrawableState();

            if (broadcast) {
                notifyGlobalFocusCleared(this);
            }
        }
    }

    void notifyGlobalFocusCleared(View oldFocus) {
        /*if (oldFocus != null && mAttachInfo != null) {
            mAttachInfo.mTreeObserver.dispatchOnGlobalFocusChange(oldFocus, null);
        }*/
    }

    boolean rootViewRequestFocus() {
        final View root = getRootView();
        return root != null && root.requestFocus();
    }

    void clearParentsWantFocus() {
        if (mParent instanceof View v) {
            v.mPrivateFlags &= ~PFLAG_WANTS_FOCUS;
            v.clearParentsWantFocus();
        }
    }

    /**
     * Called internally by the view system when a new view is getting focus.
     * This is what clears the old focus.
     * <p>
     * <b>NOTE:</b> The parent view's focused child must be updated manually
     * after calling this method. Otherwise, the view hierarchy may be left in
     * an inconstant state.
     */
    void unFocus(View focused) {
        clearFocusInternal(false);
    }

    /**
     * Returns true if this view has focus itself, or is the ancestor of the
     * view that has focus.
     *
     * @return True if this view has or contains focus, false otherwise.
     */
    public boolean hasFocus() {
        return (mPrivateFlags & PFLAG_FOCUSED) != 0;
    }

    /**
     * Returns true if this view is focusable or if it contains a reachable View
     * for which this method returns {@code true}. A "reachable hasFocusable()"
     * is a view whose parents do not block descendants focus.
     * Only {@link #VISIBLE} views are considered focusable.
     *
     * @return {@code true} if the view is focusable or if the view contains a focusable
     * view, {@code false} otherwise
     * @see ViewGroup#FOCUS_BLOCK_DESCENDANTS
     * @see ViewGroup#getTouchscreenBlocksFocus()
     * @see #hasExplicitFocusable()
     */
    public boolean hasFocusable() {
        return hasFocusable(true, false);
    }

    /**
     * Returns true if this view is focusable or if it contains a reachable View
     * for which {@link #hasExplicitFocusable()} returns {@code true}.
     * A "reachable hasExplicitFocusable()" is a view whose parents do not block descendants focus.
     * Only {@link #VISIBLE} views for which {@link #getFocusable()} would return
     * {@link #FOCUSABLE} are considered focusable.
     *
     * @return {@code true} if the view is focusable or if the view contains a focusable
     * view, {@code false} otherwise
     * @see #hasFocusable()
     */
    public boolean hasExplicitFocusable() {
        return hasFocusable(false, true);
    }

    boolean hasFocusable(boolean allowAutoFocus, boolean dispatchExplicit) {
        if (!isFocusableInTouchMode()) {
            for (ViewParent p = mParent; p instanceof ViewGroup; p = p.getParent()) {
                final ViewGroup g = (ViewGroup) p;
                if (g.shouldBlockFocusForTouchscreen()) {
                    return false;
                }
            }
        }

        // Invisible, gone, or disabled views are never focusable.
        if ((mViewFlags & VISIBILITY_MASK) != VISIBLE
                || (mViewFlags & ENABLED_MASK) != ENABLED) {
            return false;
        }

        // Only use effective focusable value when allowed.
        return (allowAutoFocus || getFocusable() != FOCUSABLE_AUTO) && isFocusable();
    }

    /**
     * Called by the view system when the focus state of this view changes.
     * When the focus change event is caused by directional navigation, direction
     * and previouslyFocusedRect provide insight into where the focus is coming from.
     * When overriding, be sure to call up through to the super class so that
     * the standard focus handling will occur.
     *
     * @param gainFocus True if the View has focus; false otherwise.
     * @param direction The direction focus has moved when requestFocus()
     *                  is called to give this view focus. Values are
     *                  {@link #FOCUS_UP}, {@link #FOCUS_DOWN}, {@link #FOCUS_LEFT},
     *                  {@link #FOCUS_RIGHT}, {@link #FOCUS_FORWARD}, or {@link #FOCUS_BACKWARD}.
     *                  It may not always apply, in which case use the default.
     */
    @CallSuper
    protected void onFocusChanged(boolean gainFocus, int direction) {
        if (!gainFocus) {
            if (isPressed()) {
                setPressed(false);
            }
            resetPressedState();
        }

        invalidate();
        ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnFocusChangeListener != null) {
            li.mOnFocusChangeListener.onFocusChange(this, gainFocus);
        }
    }

    private void resetPressedState() {
        if ((mViewFlags & ENABLED_MASK) == DISABLED) {
            return;
        }

        if (isPressed()) {
            setPressed(false);

            if (!mHasPerformedLongPress) {
                removeLongPressCallback();
            }
        }
    }

    /**
     * Returns true if this view has focus
     *
     * @return True if this view has focus, false otherwise.
     */
    public boolean isFocused() {
        return (mPrivateFlags & PFLAG_FOCUSED) != 0;
    }

    /**
     * Find the view in the hierarchy rooted at this view that currently has
     * focus.
     *
     * @return The view that currently has focus, or null if no focused view can
     * be found.
     */
    @Nullable
    public View findFocus() {
        return isFocused() ? this : null;
    }

    /**
     * Find the nearest view in the specified direction that can take focus.
     * This does not actually give focus to that view.
     *
     * @param direction One of FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, and FOCUS_RIGHT
     * @return The nearest focusable in the specified direction, or null if none
     * can be found.
     */
    @Nullable
    public View focusSearch(@FocusDirection int direction) {
        if (mParent != null) {
            return mParent.focusSearch(this, direction);
        } else {
            return null;
        }
    }

    /**
     * Returns whether this View is a root of a keyboard navigation cluster.
     *
     * @return True if this view is a root of a cluster, or false otherwise.
     */
    public final boolean isKeyboardNavigationCluster() {
        return (mPrivateFlags3 & PFLAG3_CLUSTER) != 0;
    }

    /**
     * Searches up the view hierarchy to find the top-most cluster. All deeper/nested clusters
     * will be ignored.
     *
     * @return the keyboard navigation cluster that this view is in (can be this view)
     * or {@code null} if not in one
     */
    @Nullable
    View findKeyboardNavigationCluster() {
        if (mParent instanceof View) {
            View cluster = ((View) mParent).findKeyboardNavigationCluster();
            if (cluster != null) {
                return cluster;
            } else if (isKeyboardNavigationCluster()) {
                return this;
            }
        }
        return null;
    }

    /**
     * Set whether this view is a root of a keyboard navigation cluster.
     *
     * @param isCluster If true, this view is a root of a cluster.
     */
    public void setKeyboardNavigationCluster(boolean isCluster) {
        if (isCluster) {
            mPrivateFlags3 |= PFLAG3_CLUSTER;
        } else {
            mPrivateFlags3 &= ~PFLAG3_CLUSTER;
        }
    }

    /**
     * Gets the id of the view to use when the next focus is {@link #FOCUS_LEFT}.
     *
     * @return The next focus ID, or {@link #NO_ID} if the framework should decide automatically.
     */
    public int getNextFocusLeftId() {
        return mNextFocusLeftId;
    }

    /**
     * Sets the id of the view to use when the next focus is {@link #FOCUS_LEFT}.
     *
     * @param nextFocusLeftId The next focus ID, or {@link #NO_ID} if the framework should
     *                        decide automatically.
     */
    public void setNextFocusLeftId(int nextFocusLeftId) {
        mNextFocusLeftId = nextFocusLeftId;
    }

    /**
     * Gets the id of the view to use when the next focus is {@link #FOCUS_RIGHT}.
     *
     * @return The next focus ID, or {@link #NO_ID} if the framework should decide automatically.
     */
    public int getNextFocusRightId() {
        return mNextFocusRightId;
    }

    /**
     * Sets the id of the view to use when the next focus is {@link #FOCUS_RIGHT}.
     *
     * @param nextFocusRightId The next focus ID, or {@link #NO_ID} if the framework should
     *                         decide automatically.
     */
    public void setNextFocusRightId(int nextFocusRightId) {
        mNextFocusRightId = nextFocusRightId;
    }

    /**
     * Gets the id of the view to use when the next focus is {@link #FOCUS_UP}.
     *
     * @return The next focus ID, or {@link #NO_ID} if the framework should decide automatically.
     */
    public int getNextFocusUpId() {
        return mNextFocusUpId;
    }

    /**
     * Sets the id of the view to use when the next focus is {@link #FOCUS_UP}.
     *
     * @param nextFocusUpId The next focus ID, or {@link #NO_ID} if the framework should
     *                      decide automatically.
     */
    public void setNextFocusUpId(int nextFocusUpId) {
        mNextFocusUpId = nextFocusUpId;
    }

    /**
     * Gets the id of the view to use when the next focus is {@link #FOCUS_DOWN}.
     *
     * @return The next focus ID, or {@link #NO_ID} if the framework should decide automatically.
     */
    public int getNextFocusDownId() {
        return mNextFocusDownId;
    }

    /**
     * Sets the id of the view to use when the next focus is {@link #FOCUS_DOWN}.
     *
     * @param nextFocusDownId The next focus ID, or {@link #NO_ID} if the framework should
     *                        decide automatically.
     */
    public void setNextFocusDownId(int nextFocusDownId) {
        mNextFocusDownId = nextFocusDownId;
    }

    /**
     * Gets the id of the view to use when the next focus is {@link #FOCUS_FORWARD}.
     *
     * @return The next focus ID, or {@link #NO_ID} if the framework should decide automatically.
     */
    public int getNextFocusForwardId() {
        return mNextFocusForwardId;
    }

    /**
     * Sets the id of the view to use when the next focus is {@link #FOCUS_FORWARD}.
     *
     * @param nextFocusForwardId The next focus ID, or {@link #NO_ID} if the framework should
     *                           decide automatically.
     */
    public void setNextFocusForwardId(int nextFocusForwardId) {
        mNextFocusForwardId = nextFocusForwardId;
    }

    /**
     * Gets the id of the root of the next keyboard navigation cluster.
     *
     * @return The next keyboard navigation cluster ID, or {@link #NO_ID} if the framework should
     * decide automatically.
     */
    public int getNextClusterForwardId() {
        return mNextClusterForwardId;
    }

    /**
     * Sets the id of the view to use as the root of the next keyboard navigation cluster.
     *
     * @param nextClusterForwardId The next cluster ID, or {@link #NO_ID} if the framework should
     *                             decide automatically.
     */
    public void setNextClusterForwardId(int nextClusterForwardId) {
        mNextClusterForwardId = nextClusterForwardId;
    }

    /**
     * Returns the visibility of this view and all of its ancestors
     *
     * @return True if this view and all of its ancestors are {@link #VISIBLE}
     */
    public boolean isShown() {
        View current = this;
        //noinspection ConstantConditions
        do {
            if ((current.mViewFlags & VISIBILITY_MASK) != VISIBLE) {
                return false;
            }
            ViewParent parent = current.mParent;
            if (parent == null) {
                return false; // We are not attached to the view root
            }
            if (!(parent instanceof View)) {
                return true;
            }
            current = (View) parent;
        } while (current != null);

        return false;
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
            resolvePadding();
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
            resolvePadding();
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

    private void initializeScrollIndicatorsInternal() {
        // Some day maybe we'll break this into top/left/start/etc. and let the
        // client control it. Until then, you can have any scroll indicator you
        // want as long as it's a 1dp foreground-colored rectangle.
        if (mScrollIndicatorDrawable == null) {
            mScrollIndicatorDrawable = new Drawable() {
                @Override
                public void draw(@Nonnull Canvas canvas) {
                    Paint paint = Paint.take();
                    paint.setRGBA(0, 0, 0, 0x1f);
                    canvas.drawRect(getBounds(), paint);
                }
            };
        }
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
            mScrollCache.mScrollBar.setState(getDrawableState());
            mScrollCache.mScrollBar.setCallback(this);
        }
    }

    /**
     * Define whether scrollbars will fade when the view is not scrolling.
     *
     * @param fadeScrollbars whether to enable fading
     */
    public void setScrollbarFadingEnabled(boolean fadeScrollbars) {
        initScrollCache();
        final ScrollCache scrollCache = mScrollCache;
        scrollCache.mFadeScrollBars = fadeScrollbars;
        if (fadeScrollbars) {
            scrollCache.mState = ScrollCache.OFF;
        } else {
            scrollCache.mState = ScrollCache.ON;
        }
    }

    /**
     * Returns true if scrollbars will fade when this view is not scrolling
     *
     * @return true if scrollbar fading is enabled
     */
    public boolean isScrollbarFadingEnabled() {
        return mScrollCache != null && mScrollCache.mFadeScrollBars;
    }

    private ScrollCache getScrollCache() {
        initScrollCache();
        return mScrollCache;
    }

    /**
     * Returns the delay before scrollbars fade.
     *
     * @return the delay before scrollbars fade
     */
    public int getScrollBarDefaultDelayBeforeFade() {
        return mScrollCache == null ? ViewConfiguration.getScrollDefaultDelay() :
                mScrollCache.mDefaultDelayBeforeFade;
    }

    /**
     * Define the delay before scrollbars fade.
     *
     * @param scrollBarDefaultDelayBeforeFade - the delay before scrollbars fade
     */
    public void setScrollBarDefaultDelayBeforeFade(int scrollBarDefaultDelayBeforeFade) {
        getScrollCache().mDefaultDelayBeforeFade = scrollBarDefaultDelayBeforeFade;
    }

    /**
     * Returns the scrollbar fade duration.
     *
     * @return the scrollbar fade duration, in milliseconds
     */
    public int getScrollBarFadeDuration() {
        return mScrollCache == null ? ViewConfiguration.getScrollBarFadeDuration() :
                mScrollCache.mFadeDuration;
    }

    /**
     * Define the scrollbar fade duration.
     *
     * @param scrollBarFadeDuration - the scrollbar fade duration, in milliseconds
     */
    public void setScrollBarFadeDuration(int scrollBarFadeDuration) {
        getScrollCache().mFadeDuration = scrollBarFadeDuration;
    }

    /**
     * Returns the scrollbar size.
     *
     * @return the scrollbar size
     */
    public int getScrollBarSize() {
        return mScrollCache == null ? ViewConfiguration.get().getScaledScrollbarSize() :
                mScrollCache.mScrollBarSize;
    }

    /**
     * Define the scrollbar size.
     *
     * @param scrollBarSize - the scrollbar size
     */
    public void setScrollBarSize(int scrollBarSize) {
        getScrollCache().mScrollBarSize = scrollBarSize;
    }

    /**
     * <p>Specify the style of the scrollbars. The scrollbars can be overlaid or
     * inset. When inset, they add to the padding of the view. And the scrollbars
     * can be drawn inside the padding area or on the edge of the view. For example,
     * if a view has a background drawable and you want to draw the scrollbars
     * inside the padding specified by the drawable, you can use
     * SCROLLBARS_INSIDE_OVERLAY or SCROLLBARS_INSIDE_INSET. If you want them to
     * appear at the edge of the view, ignoring the padding, then you can use
     * SCROLLBARS_OUTSIDE_OVERLAY or SCROLLBARS_OUTSIDE_INSET.</p>
     *
     * @param style the style of the scrollbars. Should be one of
     *              SCROLLBARS_INSIDE_OVERLAY, SCROLLBARS_INSIDE_INSET,
     *              SCROLLBARS_OUTSIDE_OVERLAY or SCROLLBARS_OUTSIDE_INSET.
     * @see #SCROLLBARS_INSIDE_OVERLAY
     * @see #SCROLLBARS_INSIDE_INSET
     * @see #SCROLLBARS_OUTSIDE_OVERLAY
     * @see #SCROLLBARS_OUTSIDE_INSET
     */
    public void setScrollBarStyle(int style) {
        if (style != (mViewFlags & SCROLLBARS_STYLE_MASK)) {
            mViewFlags = (mViewFlags & ~SCROLLBARS_STYLE_MASK) | (style & SCROLLBARS_STYLE_MASK);
            resolvePadding();
        }
    }

    /**
     * <p>Returns the current scrollbar style.</p>
     *
     * @return the current scrollbar style
     * @see #SCROLLBARS_INSIDE_OVERLAY
     * @see #SCROLLBARS_INSIDE_INSET
     * @see #SCROLLBARS_OUTSIDE_OVERLAY
     * @see #SCROLLBARS_OUTSIDE_INSET
     */
    public int getScrollBarStyle() {
        return mViewFlags & SCROLLBARS_STYLE_MASK;
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
     * Set the scrolled position of your view. This will cause a call to
     * {@link #onScrollChanged(int, int, int, int)} and the view will be
     * invalidated.
     *
     * @param x the x position to scroll to
     * @param y the y position to scroll to
     */
    public void scrollTo(int x, int y) {
        if (mScrollX != x || mScrollY != y) {
            int oldX = mScrollX;
            int oldY = mScrollY;
            mScrollX = x;
            mScrollY = y;
            onScrollChanged(mScrollX, mScrollY, oldX, oldY);
            if (!awakenScrollBars()) {
                postInvalidateOnAnimation();
            }
        }
    }

    /**
     * Move the scrolled position of your view. This will cause a call to
     * {@link #onScrollChanged(int, int, int, int)} and the view will be
     * invalidated.
     *
     * @param x the amount of pixels to scroll by horizontally
     * @param y the amount of pixels to scroll by vertically
     */
    public void scrollBy(int x, int y) {
        scrollTo(mScrollX + x, mScrollY + y);
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
        return mScrollCache != null && awakenScrollBars(mScrollCache.mDefaultDelayBeforeFade);
    }

    /**
     * Trigger the scrollbars to draw.
     * This method differs from awakenScrollBars() only in its default duration.
     * initialAwakenScrollBars() will show the scroll bars for longer than
     * usual to give the user more of a chance to notice them.
     */
    private void initialAwakenScrollBars() {
        if (mScrollCache != null) {
            awakenScrollBars(mScrollCache.mDefaultDelayBeforeFade * 4);
        }
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

            // Invalidate to show the scrollbars
            postInvalidateOnAnimation();

            if (scrollCache.mState == ScrollCache.OFF) {
                // first takes longer
                startDelay = Math.max(1500, startDelay);
            } else {
                startDelay = Math.max(0, startDelay);
            }

            // Tell mScrollCache when we should start fading. This may
            // extend the fade start time if one was already scheduled
            long fadeStartTime = AnimationUtils.currentAnimationTimeMillis() + startDelay;
            scrollCache.mFadeStartTime = fadeStartTime;
            scrollCache.mState = ScrollCache.ON;

            if (mAttachInfo != null) {
                mAttachInfo.mHandler.removeCallbacks(scrollCache);
                mAttachInfo.mHandler.postAtTime(scrollCache, fadeStartTime);
            }

            return true;
        }

        return false;
    }

    /**
     * Set the horizontal scrolled position of your view. This will cause a call to
     * {@link #onScrollChanged(int, int, int, int)} and the view will be
     * invalidated.
     *
     * @param value the x position to scroll to
     */
    public void setScrollX(int value) {
        scrollTo(value, mScrollY);
    }

    /**
     * Set the vertical scrolled position of your view. This will cause a call to
     * {@link #onScrollChanged(int, int, int, int)} and the view will be
     * invalidated.
     *
     * @param value the y position to scroll to
     */
    public void setScrollY(int value) {
        scrollTo(mScrollX, value);
    }

    /**
     * Return the scrolled left position of this view. This is the left edge of
     * the displayed part of your view. You do not need to draw any pixels
     * farther left, since those are outside the frame of your view on
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
     * it, since those are outside the frame of your view on screen.
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
     * Check if this view can be scrolled horizontally in a certain direction.
     *
     * @param direction Negative to check scrolling left, positive to check scrolling right.
     * @return true if this view can be scrolled in the specified direction, false otherwise.
     */
    public boolean canScrollHorizontally(int direction) {
        final int offset = computeHorizontalScrollOffset();
        final int range = computeHorizontalScrollRange() - computeHorizontalScrollExtent();
        if (range == 0) return false;
        if (direction < 0) {
            return offset > 0;
        } else {
            return offset < range - 1;
        }
    }

    /**
     * Check if this view can be scrolled vertically in a certain direction.
     *
     * @param direction Negative to check scrolling up, positive to check scrolling down.
     * @return true if this view can be scrolled in the specified direction, false otherwise.
     */
    public boolean canScrollVertically(int direction) {
        final int offset = computeVerticalScrollOffset();
        final int range = computeVerticalScrollRange() - computeVerticalScrollExtent();
        if (range == 0) return false;
        if (direction < 0) {
            return offset > 0;
        } else {
            return offset < range - 1;
        }
    }

    void getScrollIndicatorBounds(@Nonnull Rect out) {
        out.left = mScrollX;
        out.right = mScrollX + mRight - mLeft;
        out.top = mScrollY;
        out.bottom = mScrollY + mBottom - mTop;
    }

    private void getHorizontalScrollBarBounds(@Nullable Rect drawBounds,
                                              @Nullable Rect touchBounds) {
        final Rect bounds = drawBounds != null ? drawBounds : touchBounds;
        if (bounds == null) {
            return;
        }
        final int inside = (mViewFlags & SCROLLBARS_OUTSIDE_MASK) == 0 ? ~0 : 0;
        final boolean drawVerticalScrollBar = isVerticalScrollBarEnabled()
                && !isVerticalScrollBarHidden();
        final int size = getHorizontalScrollbarHeight();
        final int verticalScrollBarGap = drawVerticalScrollBar ?
                getVerticalScrollbarWidth() : 0;
        final int width = mRight - mLeft;
        final int height = mBottom - mTop;
        bounds.top = mScrollY + height - size - (mUserPaddingBottom & inside);
        bounds.left = mScrollX + (mPaddingLeft & inside);
        bounds.right = mScrollX + width - (mUserPaddingRight & inside) - verticalScrollBarGap;
        bounds.bottom = bounds.top + size;

        if (touchBounds == null) {
            return;
        }
        if (touchBounds != bounds) {
            touchBounds.set(bounds);
        }
        final int minTouchTarget = mScrollCache.mScrollBarMinTouchTarget;
        if (touchBounds.height() < minTouchTarget) {
            final int adjust = (minTouchTarget - touchBounds.height()) / 2;
            touchBounds.bottom = Math.min(touchBounds.bottom + adjust, mScrollY + height);
            touchBounds.top = touchBounds.bottom - minTouchTarget;
        }
        if (touchBounds.width() < minTouchTarget) {
            final int adjust = (minTouchTarget - touchBounds.width()) / 2;
            touchBounds.left -= adjust;
            touchBounds.right = touchBounds.left + minTouchTarget;
        }
    }

    private void getVerticalScrollBarBounds(@Nullable Rect drawBounds, @Nullable Rect touchBounds) {
        final Rect bounds = drawBounds != null ? drawBounds : touchBounds;
        if (bounds == null) {
            return;
        }
        final int inside = (mViewFlags & SCROLLBARS_OUTSIDE_MASK) == 0 ? ~0 : 0;
        final int size = getVerticalScrollbarWidth();
        final boolean layoutRtl = isLayoutRtl();
        final int width = mRight - mLeft;
        final int height = mBottom - mTop;
        if (layoutRtl) {
            bounds.left = mScrollX + (mUserPaddingLeft & inside);
        } else {
            bounds.left = mScrollX + width - size - (mUserPaddingRight & inside);
        }
        bounds.top = mScrollY + (mPaddingTop & inside);
        bounds.right = bounds.left + size;
        bounds.bottom = mScrollY + height - (mUserPaddingBottom & inside);

        if (touchBounds == null) {
            return;
        }
        if (touchBounds != bounds) {
            touchBounds.set(bounds);
        }
        final int minTouchTarget = mScrollCache.mScrollBarMinTouchTarget;
        if (touchBounds.width() < minTouchTarget) {
            final int adjust = (minTouchTarget - touchBounds.width()) / 2;
            if (layoutRtl) {
                touchBounds.left = Math.max(touchBounds.left + adjust, mScrollX);
                touchBounds.right = touchBounds.left + minTouchTarget;
            } else {
                touchBounds.right = Math.min(touchBounds.right + adjust, mScrollX + width);
                touchBounds.left = touchBounds.right - minTouchTarget;
            }
        }
        if (touchBounds.height() < minTouchTarget) {
            final int adjust = (minTouchTarget - touchBounds.height()) / 2;
            touchBounds.top -= adjust;
            touchBounds.bottom = touchBounds.top + minTouchTarget;
        }
    }

    /**
     * Override this if the vertical scrollbar needs to be hidden in a subclass, like when
     * FastScroller is visible.
     *
     * @return whether to temporarily hide the vertical scrollbar
     * @hide
     */
    protected boolean isVerticalScrollBarHidden() {
        return false;
    }

    boolean isOnScrollbar(float x, float y) {
        if (mScrollCache == null) {
            return false;
        }
        x += getScrollX();
        y += getScrollY();
        final boolean canScrollVertically =
                computeVerticalScrollRange() > computeVerticalScrollExtent();
        if (isVerticalScrollBarEnabled() && !isVerticalScrollBarHidden() && canScrollVertically) {
            final Rect touchBounds = mScrollCache.mScrollBarTouchBounds;
            getVerticalScrollBarBounds(null, touchBounds);
            if (touchBounds.contains((int) x, (int) y)) {
                return true;
            }
        }
        final boolean canScrollHorizontally =
                computeHorizontalScrollRange() > computeHorizontalScrollExtent();
        if (isHorizontalScrollBarEnabled() && canScrollHorizontally) {
            final Rect touchBounds = mScrollCache.mScrollBarTouchBounds;
            getHorizontalScrollBarBounds(null, touchBounds);
            return touchBounds.contains((int) x, (int) y);
        }
        return false;
    }

    boolean isOnScrollbarThumb(float x, float y) {
        return isOnVerticalScrollbarThumb(x, y) || isOnHorizontalScrollbarThumb(x, y);
    }

    private boolean isOnVerticalScrollbarThumb(float x, float y) {
        if (mScrollCache == null || !isVerticalScrollBarEnabled() || isVerticalScrollBarHidden()) {
            return false;
        }
        final int range = computeVerticalScrollRange();
        final int extent = computeVerticalScrollExtent();
        if (range > extent) {
            x += getScrollX();
            y += getScrollY();
            final Rect bounds = mScrollCache.mScrollBarBounds;
            final Rect touchBounds = mScrollCache.mScrollBarTouchBounds;
            getVerticalScrollBarBounds(bounds, touchBounds);
            final int offset = computeVerticalScrollOffset();
            final int thumbLength = ScrollCache.getThumbLength(bounds.height(), bounds.width(),
                    extent, range);
            final int thumbOffset = ScrollCache.getThumbOffset(bounds.height(), thumbLength,
                    extent, range, offset);
            final int thumbTop = bounds.top + thumbOffset;
            final int adjust = Math.max(mScrollCache.mScrollBarMinTouchTarget - thumbLength, 0) / 2;
            return x >= touchBounds.left && x <= touchBounds.right
                    && y >= thumbTop - adjust && y <= thumbTop + thumbLength + adjust;
        }
        return false;
    }

    private boolean isOnHorizontalScrollbarThumb(float x, float y) {
        if (mScrollCache == null || !isHorizontalScrollBarEnabled()) {
            return false;
        }
        final int range = computeHorizontalScrollRange();
        final int extent = computeHorizontalScrollExtent();
        if (range > extent) {
            x += getScrollX();
            y += getScrollY();
            final Rect bounds = mScrollCache.mScrollBarBounds;
            final Rect touchBounds = mScrollCache.mScrollBarTouchBounds;
            getHorizontalScrollBarBounds(bounds, touchBounds);
            final int offset = computeHorizontalScrollOffset();

            final int thumbLength = ScrollCache.getThumbLength(bounds.width(), bounds.height(),
                    extent, range);
            final int thumbOffset = ScrollCache.getThumbOffset(bounds.width(), thumbLength,
                    extent, range, offset);
            final int thumbLeft = bounds.left + thumbOffset;
            final int adjust = Math.max(mScrollCache.mScrollBarMinTouchTarget - thumbLength, 0) / 2;
            return x >= thumbLeft - adjust && x <= thumbLeft + thumbLength + adjust
                    && y >= touchBounds.top && y <= touchBounds.bottom;
        }
        return false;
    }

    boolean isDraggingScrollBar() {
        return mScrollCache != null
                && mScrollCache.mScrollBarDraggingState != ScrollCache.NOT_DRAGGING;
    }

    /**
     * Sets the state of all scroll indicators.
     * <p>
     * See {@link #setScrollIndicators(int, int)} for usage information.
     *
     * @param indicators a bitmask of indicators that should be enabled, or
     *                   {@code 0} to disable all indicators
     * @see #setScrollIndicators(int, int)
     * @see #getScrollIndicators()
     */
    public void setScrollIndicators(@ScrollIndicators int indicators) {
        setScrollIndicators(indicators,
                SCROLL_INDICATORS_PFLAG3_MASK >>> SCROLL_INDICATORS_TO_PFLAGS3_LSHIFT);
    }

    /**
     * Sets the state of the scroll indicators specified by the mask. To change
     * all scroll indicators at once, see {@link #setScrollIndicators(int)}.
     * <p>
     * When a scroll indicator is enabled, it will be displayed if the view
     * can scroll in the direction of the indicator.
     * <p>
     * Multiple indicator types may be enabled or disabled by passing the
     * logical OR of the desired types. If multiple types are specified, they
     * will all be set to the same enabled state.
     * <p>
     * For example, to enable the top scroll indicatorExample: {@code setScrollIndicators
     *
     * @param indicators the indicator direction, or the logical OR of multiple
     *                   indicator directions. One or more of:
     *                   <ul>
     *                     <li>{@link #SCROLL_INDICATOR_TOP}</li>
     *                     <li>{@link #SCROLL_INDICATOR_BOTTOM}</li>
     *                     <li>{@link #SCROLL_INDICATOR_LEFT}</li>
     *                     <li>{@link #SCROLL_INDICATOR_RIGHT}</li>
     *                     <li>{@link #SCROLL_INDICATOR_START}</li>
     *                     <li>{@link #SCROLL_INDICATOR_END}</li>
     *                   </ul>
     * @see #setScrollIndicators(int)
     * @see #getScrollIndicators()
     */
    public void setScrollIndicators(@ScrollIndicators int indicators, @ScrollIndicators int mask) {
        // Shift and sanitize mask.
        mask <<= SCROLL_INDICATORS_TO_PFLAGS3_LSHIFT;
        mask &= SCROLL_INDICATORS_PFLAG3_MASK;

        // Shift and mask indicators.
        indicators <<= SCROLL_INDICATORS_TO_PFLAGS3_LSHIFT;
        indicators &= mask;

        // Merge with non-masked flags.
        final int updatedFlags = indicators | (mPrivateFlags3 & ~mask);

        if (mPrivateFlags3 != updatedFlags) {
            mPrivateFlags3 = updatedFlags;

            if (indicators != 0) {
                initializeScrollIndicatorsInternal();
            }
            invalidate();
        }
    }

    /**
     * Returns a bitmask representing the enabled scroll indicators.
     * <p>
     * For example, if the top and left scroll indicators are enabled and all
     * other indicators are disabled, the return value will be
     * {@code View.SCROLL_INDICATOR_TOP | View.SCROLL_INDICATOR_LEFT}.
     * <p>
     * To check whether the bottom scroll indicator is enabled, use the value
     * of {@code (getScrollIndicators() & View.SCROLL_INDICATOR_BOTTOM) != 0}.
     *
     * @return a bitmask representing the enabled scroll indicators
     */
    public int getScrollIndicators() {
        return (mPrivateFlags3 & SCROLL_INDICATORS_PFLAG3_MASK)
                >>> SCROLL_INDICATORS_TO_PFLAGS3_LSHIFT;
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
     * Called by a parent to request that a child update its values for mScrollX
     * and mScrollY if necessary. This will typically be done if the child is
     * animating a scroll using a Scroller.
     */
    public void computeScroll() {
    }

    /**
     * This is called in response to an internal scroll in this view (i.e., the
     * view scrolled its own contents). This is typically as a result of
     * {@link #scrollBy(int, int)} or {@link #scrollTo(int, int)} having been
     * called.
     *
     * @param l    Current horizontal scroll origin.
     * @param t    Current vertical scroll origin.
     * @param oldl Previous horizontal scroll origin.
     * @param oldt Previous vertical scroll origin.
     */
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        mBackgroundSizeChanged = true;
        mDefaultFocusHighlightSizeChanged = true;
        if (mForegroundInfo != null) {
            mForegroundInfo.mBoundsChanged = true;
        }

        final AttachInfo ai = mAttachInfo;
        if (ai != null) {
            ai.mViewScrollChanged = true;
        }

        if (mListenerInfo != null && mListenerInfo.mOnScrollChangeListener != null) {
            mListenerInfo.mOnScrollChangeListener.onScrollChange(this, l, t, oldl, oldt);
        }
    }

    /**
     * Scroll the view with standard behavior for scrolling beyond the normal
     * content boundaries. Views that call this method should override
     * {@link #onOverScrolled(int, int, boolean, boolean)} to respond to the
     * results of an over-scroll operation.
     * <p>
     * Views can use this method to handle any touch or fling-based scrolling.
     *
     * @param deltaX         Change in X in pixels
     * @param deltaY         Change in Y in pixels
     * @param scrollX        Current X scroll value in pixels before applying deltaX
     * @param scrollY        Current Y scroll value in pixels before applying deltaY
     * @param scrollRangeX   Maximum content scroll range along the X axis
     * @param scrollRangeY   Maximum content scroll range along the Y axis
     * @param maxOverScrollX Number of pixels to overscroll by in either direction
     *                       along the X axis.
     * @param maxOverScrollY Number of pixels to overscroll by in either direction
     *                       along the Y axis.
     * @param isTouchEvent   true if this scroll operation is the result of a touch event.
     * @return true if scrolling was clamped to an over-scroll boundary along either
     * axis, false otherwise.
     */
    protected boolean overScrollBy(int deltaX, int deltaY,
                                   int scrollX, int scrollY,
                                   int scrollRangeX, int scrollRangeY,
                                   int maxOverScrollX, int maxOverScrollY,
                                   boolean isTouchEvent) {
        final int overScrollMode = mOverScrollMode;
        final boolean canScrollHorizontal =
                computeHorizontalScrollRange() > computeHorizontalScrollExtent();
        final boolean canScrollVertical =
                computeVerticalScrollRange() > computeVerticalScrollExtent();
        final boolean overScrollHorizontal = overScrollMode == OVER_SCROLL_ALWAYS ||
                (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollHorizontal);
        final boolean overScrollVertical = overScrollMode == OVER_SCROLL_ALWAYS ||
                (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollVertical);

        int newScrollX = scrollX + deltaX;
        if (!overScrollHorizontal) {
            maxOverScrollX = 0;
        }

        int newScrollY = scrollY + deltaY;
        if (!overScrollVertical) {
            maxOverScrollY = 0;
        }

        // Clamp values if at the limits and record
        final int left = -maxOverScrollX;
        final int right = maxOverScrollX + scrollRangeX;
        final int top = -maxOverScrollY;
        final int bottom = maxOverScrollY + scrollRangeY;

        boolean clampedX = false;
        if (newScrollX > right) {
            newScrollX = right;
            clampedX = true;
        } else if (newScrollX < left) {
            newScrollX = left;
            clampedX = true;
        }

        boolean clampedY = false;
        if (newScrollY > bottom) {
            newScrollY = bottom;
            clampedY = true;
        } else if (newScrollY < top) {
            newScrollY = top;
            clampedY = true;
        }

        onOverScrolled(newScrollX, newScrollY, clampedX, clampedY);

        return clampedX || clampedY;
    }

    /**
     * Called by {@link #overScrollBy(int, int, int, int, int, int, int, int, boolean)} to
     * respond to the results of an over-scroll operation.
     *
     * @param scrollX  New X scroll value in pixels
     * @param scrollY  New Y scroll value in pixels
     * @param clampedX True if scrollX was clamped to an over-scroll boundary
     * @param clampedY True if scrollY was clamped to an over-scroll boundary
     */
    protected void onOverScrolled(int scrollX, int scrollY,
                                  boolean clampedX, boolean clampedY) {
        // Intentionally empty.
    }

    /**
     * Returns the over-scroll mode for this view. The result will be
     * one of {@link #OVER_SCROLL_ALWAYS}, {@link #OVER_SCROLL_IF_CONTENT_SCROLLS}
     * (allow over-scrolling only if the view content is larger than the container),
     * or {@link #OVER_SCROLL_NEVER}.
     *
     * @return This view's over-scroll mode.
     */
    public int getOverScrollMode() {
        return mOverScrollMode;
    }

    /**
     * Set the over-scroll mode for this view. Valid over-scroll modes are
     * {@link #OVER_SCROLL_ALWAYS}, {@link #OVER_SCROLL_IF_CONTENT_SCROLLS}
     * (allow over-scrolling only if the view content is larger than the container),
     * or {@link #OVER_SCROLL_NEVER}.
     * <p>
     * Setting the over-scroll mode of a view will have an effect only if the
     * view is capable of scrolling.
     *
     * @param overScrollMode The new over-scroll mode for this view.
     */
    public void setOverScrollMode(int overScrollMode) {
        if (overScrollMode != OVER_SCROLL_ALWAYS &&
                overScrollMode != OVER_SCROLL_IF_CONTENT_SCROLLS &&
                overScrollMode != OVER_SCROLL_NEVER) {
            throw new IllegalArgumentException("Invalid overscroll mode " + overScrollMode);
        }
        mOverScrollMode = overScrollMode;
    }

    /// Start - Nested Scrolling \\\

    /**
     * Enable or disable nested scrolling for this view.
     *
     * <p>If this property is set to true the view will be permitted to initiate nested
     * scrolling operations with a compatible parent view in the current hierarchy. If this
     * view does not implement nested scrolling this will have no effect. Disabling nested scrolling
     * while a nested scroll is in progress has the effect of {@link #stopNestedScroll(int) stopping}
     * the nested scroll.</p>
     *
     * @param enabled true to enable nested scrolling, false to disable
     * @see #isNestedScrollingEnabled()
     */
    public void setNestedScrollingEnabled(boolean enabled) {
        if (enabled) {
            mPrivateFlags3 |= PFLAG3_NESTED_SCROLLING_ENABLED;
        } else if (isNestedScrollingEnabled()) {
            stopNestedScroll(TYPE_TOUCH);
            mPrivateFlags3 &= ~PFLAG3_NESTED_SCROLLING_ENABLED;
        }
    }

    /**
     * Returns true if nested scrolling is enabled for this view.
     *
     * <p>If nested scrolling is enabled and this View class implementation supports it,
     * this view will act as a nested scrolling child view when applicable, forwarding data
     * about the scroll operation in progress to a compatible and cooperating nested scrolling
     * parent.</p>
     *
     * @return true if nested scrolling is enabled
     * @see #setNestedScrollingEnabled(boolean)
     */
    public boolean isNestedScrollingEnabled() {
        return (mPrivateFlags3 & PFLAG3_NESTED_SCROLLING_ENABLED) ==
                PFLAG3_NESTED_SCROLLING_ENABLED;
    }

    /**
     * Begin a nestable scroll operation along the given axes, for the given input type.
     *
     * <p>A view starting a nested scroll promises to abide by the following contract:</p>
     *
     * <p>The view will call startNestedScroll upon initiating a scroll operation. In the case
     * of a touch scroll type this corresponds to the initial {@link MotionEvent#ACTION_DOWN}.
     * In the case of touch scrolling the nested scroll will be terminated automatically in
     * the same manner as {@link ViewParent#requestDisallowInterceptTouchEvent(boolean)}.
     * In the event of programmatic scrolling the caller must explicitly call
     * {@link #stopNestedScroll(int)} to indicate the end of the nested scroll.</p>
     *
     * <p>If <code>startNestedScroll</code> returns true, a cooperative parent was found.
     * If it returns false the caller may ignore the rest of this contract until the next scroll.
     * Calling startNestedScroll while a nested scroll is already in progress will return true.</p>
     *
     * <p>At each incremental step of the scroll the caller should invoke
     * {@link #dispatchNestedPreScroll(int, int, int[], int[], int) dispatchNestedPreScroll}
     * once it has calculated the requested scrolling delta. If it returns true the nested scrolling
     * parent at least partially consumed the scroll and the caller should adjust the amount it
     * scrolls by.</p>
     *
     * <p>After applying the remainder of the scroll delta the caller should invoke
     * {@link #dispatchNestedScroll(int, int, int, int, int[], int, int[]) dispatchNestedScroll}, passing
     * both the delta consumed and the delta unconsumed. A nested scrolling parent may treat
     * these values differently. See
     * {@link ViewParent#onNestedScroll(View, int, int, int, int, int, int[])}.
     * </p>
     *
     * @param axes Flags consisting of a combination of {@link View#SCROLL_AXIS_HORIZONTAL}
     *             and/or {@link View#SCROLL_AXIS_VERTICAL}.
     * @param type the type of input which cause this scroll event
     * @return true if a cooperative parent was found and nested scrolling has been enabled for
     * the current gesture.
     * @see #stopNestedScroll(int)
     * @see #dispatchNestedPreScroll(int, int, int[], int[], int)
     * @see #dispatchNestedScroll(int, int, int, int, int[], int, int[])
     */
    public boolean startNestedScroll(@ScrollAxis int axes, @NestedScrollType int type) {
        if (hasNestedScrollingParent(type)) {
            // Already in progress
            return true;
        }
        if (isNestedScrollingEnabled()) {
            ViewParent p = getParent();
            View child = this;
            while (p != null) {
                if (p.onStartNestedScroll(child, this, axes, type)) {
                    if (type == TYPE_NON_TOUCH) {
                        mNestedScrollingParentNonTouch = p;
                    } else {
                        mNestedScrollingParentTouch = p;
                    }
                    p.onNestedScrollAccepted(child, this, axes, type);
                    return true;
                }
                if (p instanceof View) {
                    child = (View) p;
                }
                p = p.getParent();
            }
        }
        return false;
    }

    /**
     * Stop a nested scroll in progress for the given input type.
     *
     * <p>Calling this method when a nested scroll is not currently in progress is harmless.</p>
     *
     * @param type the type of input which cause this scroll event
     * @see #startNestedScroll(int, int)
     */
    public void stopNestedScroll(@NestedScrollType int type) {
        if (type == TYPE_NON_TOUCH) {
            if (mNestedScrollingParentNonTouch != null) {
                mNestedScrollingParentNonTouch.onStopNestedScroll(this, type);
                mNestedScrollingParentNonTouch = null;
            }
        } else {
            if (mNestedScrollingParentTouch != null) {
                mNestedScrollingParentTouch.onStopNestedScroll(this, type);
                mNestedScrollingParentTouch = null;
            }
        }
    }

    /**
     * Returns true if this view has a nested scrolling parent for the given input type.
     *
     * <p>The presence of a nested scrolling parent indicates that this view has initiated
     * a nested scroll and it was accepted by an ancestor view further up the view hierarchy.</p>
     *
     * @param type the type of input which cause this scroll event
     * @return whether this view has a nested scrolling parent
     */
    public boolean hasNestedScrollingParent(@NestedScrollType int type) {
        if (type == TYPE_NON_TOUCH) {
            return mNestedScrollingParentNonTouch != null;
        } else {
            return mNestedScrollingParentTouch != null;
        }
    }

    /**
     * Dispatch one step of a nested scroll in progress.
     *
     * <p>Implementations of views that support nested scrolling should call this to report
     * info about a scroll in progress to the current nested scrolling parent. If a nested scroll
     * is not currently in progress or nested scrolling is not
     * {@link #isNestedScrollingEnabled() enabled} for this view this method does nothing.
     *
     * <p>Compatible View implementations should also call
     * {@link #dispatchNestedPreScroll(int, int, int[], int[], int) dispatchNestedPreScroll} before
     * consuming a component of the scroll event themselves.
     *
     * <p>The original nested scrolling child (where the input events were received to start the
     * scroll) must provide a non-null <code>consumed</code> parameter with values {0, 0}.
     *
     * @param dxConsumed     Horizontal distance in pixels consumed by this view during this scroll step
     * @param dyConsumed     Vertical distance in pixels consumed by this view during this scroll step
     * @param dxUnconsumed   Horizontal scroll distance in pixels not consumed by this view
     * @param dyUnconsumed   Horizontal scroll distance in pixels not consumed by this view
     * @param offsetInWindow Optional. If not null, on return this will contain the offset
     *                       in local view coordinates of this view from before this operation
     *                       to after it completes. View implementations may use this to adjust
     *                       expected input coordinate tracking.
     * @param type           the type of input which cause this scroll event
     * @param consumed       Output. Upon this method returning, will contain the original values plus any
     *                       scroll distances consumed by all of this view's nested scrolling parents up
     *                       the view hierarchy. Index 0 for the x dimension, and index 1 for the y
     *                       dimension
     * @return true if the event was dispatched, false if it could not be dispatched.
     * @see ViewParent#onNestedScroll(View, int, int, int, int, int, int[])
     */
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,
                                        @Nullable int[] offsetInWindow, @NestedScrollType int type,
                                        @Nonnull int[] consumed) {
        if (isNestedScrollingEnabled()) {
            final ViewParent parent = type == TYPE_NON_TOUCH ? mNestedScrollingParentNonTouch :
                    mNestedScrollingParentTouch;
            if (parent == null) {
                return false;
            }

            if (dxConsumed != 0 || dyConsumed != 0 || dxUnconsumed != 0 || dyUnconsumed != 0) {
                int startX = 0;
                int startY = 0;
                if (offsetInWindow != null) {
                    getLocationInWindow(offsetInWindow);
                    startX = offsetInWindow[0];
                    startY = offsetInWindow[1];
                }

                parent.onNestedScroll(this, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed);

                if (offsetInWindow != null) {
                    getLocationInWindow(offsetInWindow);
                    offsetInWindow[0] -= startX;
                    offsetInWindow[1] -= startY;
                }
                return true;
            } else if (offsetInWindow != null) {
                // No motion, no dispatch. Keep offsetInWindow up to date.
                offsetInWindow[0] = 0;
                offsetInWindow[1] = 0;
            }
        }
        return false;
    }

    /**
     * Dispatch one step of a nested scroll in progress before this view consumes any portion of it.
     *
     * <p>Nested pre-scroll events are to nested scroll events what touch intercept is to touch.
     * <code>dispatchNestedPreScroll</code> offers an opportunity for the parent view in a nested
     * scrolling operation to consume some or all of the scroll operation before the child view
     * consumes it.</p>
     *
     * @param dx             Horizontal scroll distance in pixels
     * @param dy             Vertical scroll distance in pixels
     * @param consumed       Output. If not null, consumed[0] will contain the consumed component of dx
     *                       and consumed[1] the consumed dy.
     * @param offsetInWindow Optional. If not null, on return this will contain the offset
     *                       in local view coordinates of this view from before this operation
     *                       to after it completes. View implementations may use this to adjust
     *                       expected input coordinate tracking.
     * @param type           the type of input which cause this scroll event
     * @return true if the parent consumed some or all of the scroll delta
     * @see #dispatchNestedScroll(int, int, int, int, int[], int, int[])
     */
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed,
                                           @Nullable int[] offsetInWindow, @NestedScrollType int type) {
        if (isNestedScrollingEnabled()) {
            final ViewParent parent = type == TYPE_NON_TOUCH ? mNestedScrollingParentNonTouch :
                    mNestedScrollingParentTouch;
            if (parent == null) {
                return false;
            }

            if (dx != 0 || dy != 0) {
                int startX = 0;
                int startY = 0;
                if (offsetInWindow != null) {
                    getLocationInWindow(offsetInWindow);
                    startX = offsetInWindow[0];
                    startY = offsetInWindow[1];
                }

                if (consumed == null) {
                    if (mTempNestedScrollConsumed == null) {
                        mTempNestedScrollConsumed = new int[2];
                    }
                    consumed = mTempNestedScrollConsumed;
                }
                consumed[0] = 0;
                consumed[1] = 0;
                parent.onNestedPreScroll(this, dx, dy, consumed, type);

                if (offsetInWindow != null) {
                    getLocationInWindow(offsetInWindow);
                    offsetInWindow[0] -= startX;
                    offsetInWindow[1] -= startY;
                }
                return consumed[0] != 0 || consumed[1] != 0;
            } else if (offsetInWindow != null) {
                offsetInWindow[0] = 0;
                offsetInWindow[1] = 0;
            }
        }
        return false;
    }

    /**
     * Dispatch a fling to a nested scrolling parent.
     *
     * <p>This method should be used to indicate that a nested scrolling child has detected
     * suitable conditions for a fling. Generally this means that a touch scroll has ended with a
     * {@link VelocityTracker velocity} in the direction of scrolling that meets or exceeds
     * the {@link ViewConfiguration#getScaledMinimumFlingVelocity() minimum fling velocity}
     * along a scrollable axis.</p>
     *
     * <p>If a nested scrolling child view would normally fling but it is at the edge of
     * its own content, it can use this method to delegate the fling to its nested scrolling
     * parent instead. The parent may optionally consume the fling or observe a child fling.</p>
     *
     * @param velocityX Horizontal fling velocity in pixels per second
     * @param velocityY Vertical fling velocity in pixels per second
     * @param consumed  true if the child consumed the fling, false otherwise
     * @return true if the nested scrolling parent consumed or otherwise reacted to the fling
     */
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        if (isNestedScrollingEnabled() && mNestedScrollingParentTouch != null) {
            return mNestedScrollingParentTouch.onNestedFling(this, velocityX, velocityY, consumed);
        }
        return false;
    }

    /**
     * Dispatch a fling to a nested scrolling parent before it is processed by this view.
     *
     * <p>Nested pre-fling events are to nested fling events what touch intercept is to touch
     * and what nested pre-scroll is to nested scroll. <code>dispatchNestedPreFling</code>
     * offsets an opportunity for the parent view in a nested fling to fully consume the fling
     * before the child view consumes it. If this method returns <code>true</code>, a nested
     * parent view consumed the fling and this view should not scroll as a result.</p>
     *
     * <p>For a better user experience, only one view in a nested scrolling chain should consume
     * the fling at a time. If a parent view consumed the fling this method will return false.
     * Custom view implementations should account for this in two ways:</p>
     *
     * <ul>
     *     <li>If a custom view is paged and needs to settle to a fixed page-point, do not
     *     call <code>dispatchNestedPreFling</code>; consume the fling and settle to a valid
     *     position regardless.</li>
     *     <li>If a nested parent does consume the fling, this view should not scroll at all,
     *     even to settle back to a valid idle position.</li>
     * </ul>
     *
     * <p>Views should also not offer fling velocities to nested parent views along an axis
     * where scrolling is not currently supported; a {@link icyllis.modernui.widget.ScrollView ScrollView}
     * should not offer a horizontal fling velocity to its parents since scrolling along that
     * axis is not permitted and carrying velocity along that motion does not make sense.</p>
     *
     * @param velocityX Horizontal fling velocity in pixels per second
     * @param velocityY Vertical fling velocity in pixels per second
     * @return true if a nested scrolling parent consumed the fling
     */
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        if (isNestedScrollingEnabled() && mNestedScrollingParentTouch != null) {
            return mNestedScrollingParentTouch.onNestedPreFling(this, velocityX, velocityY);
        }
        return false;
    }

    /// End - Nested Scrolling \\\

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
     * Return the visible drawing bounds of your view. Fills in the output
     * rectangle with the values from getScrollX(), getScrollY(),
     * getWidth(), and getHeight(). These bounds do not account for any
     * transformation properties currently set on the view, such as
     * {@link #setScaleX(float)} or {@link #setRotation(float)}.
     *
     * @param outRect The (scrolled) drawing bounds of the view.
     */
    public void getDrawingRect(@Nonnull Rect outRect) {
        outRect.left = mScrollX;
        outRect.top = mScrollY;
        outRect.right = mScrollX + (mRight - mLeft);
        outRect.bottom = mScrollY + (mBottom - mTop);
    }

    /**
     * Left position of this view relative to its parent.
     *
     * @return The left edge of this view, in pixels.
     */
    public final int getLeft() {
        return mLeft;
    }

    /**
     * Sets the left position of this view relative to its parent. This method is meant to be called
     * by the layout system and should not generally be called otherwise, because the property
     * may be changed at any time by the layout.
     *
     * @param left The left of this view, in pixels.
     */
    public final void setLeft(int left) {
        if (left != mLeft) {
            invalidate();

            int oldWidth = mRight - mLeft;
            int height = mBottom - mTop;

            mLeft = left;
            mRenderNode.setLeft(left);

            sizeChange(mRight - mLeft, height, oldWidth, height);

            mBackgroundSizeChanged = true;
            mDefaultFocusHighlightSizeChanged = true;
            if (mForegroundInfo != null) {
                mForegroundInfo.mBoundsChanged = true;
            }
        }
    }

    /**
     * Top position of this view relative to its parent.
     *
     * @return The top of this view, in pixels.
     */
    public final int getTop() {
        return mTop;
    }

    /**
     * Sets the top position of this view relative to its parent. This method is meant to be called
     * by the layout system and should not generally be called otherwise, because the property
     * may be changed at any time by the layout.
     *
     * @param top The top of this view, in pixels.
     */
    public final void setTop(int top) {
        if (top != mTop) {
            invalidate();

            int width = mRight - mLeft;
            int oldHeight = mBottom - mTop;

            mTop = top;
            mRenderNode.setTop(mTop);

            sizeChange(width, mBottom - mTop, width, oldHeight);

            mBackgroundSizeChanged = true;
            mDefaultFocusHighlightSizeChanged = true;
            if (mForegroundInfo != null) {
                mForegroundInfo.mBoundsChanged = true;
            }
        }
    }

    /**
     * Right position of this view relative to its parent.
     *
     * @return The right edge of this view, in pixels.
     */
    public final int getRight() {
        return mRight;
    }

    /**
     * Sets the right position of this view relative to its parent. This method is meant to be called
     * by the layout system and should not generally be called otherwise, because the property
     * may be changed at any time by the layout.
     *
     * @param right The right of this view, in pixels.
     */
    public final void setRight(int right) {
        if (right != mRight) {
            invalidate();

            int oldWidth = mRight - mLeft;
            int height = mBottom - mTop;

            mRight = right;
            mRenderNode.setRight(mRight);

            sizeChange(mRight - mLeft, height, oldWidth, height);

            mBackgroundSizeChanged = true;
            mDefaultFocusHighlightSizeChanged = true;
            if (mForegroundInfo != null) {
                mForegroundInfo.mBoundsChanged = true;
            }
        }
    }

    /**
     * Bottom position of this view relative to its parent.
     *
     * @return The bottom of this view, in pixels.
     */
    public final int getBottom() {
        return mBottom;
    }

    /**
     * Sets the bottom position of this view relative to its parent. This method is meant to be
     * called by the layout system and should not generally be called otherwise, because the
     * property may be changed at any time by the layout.
     *
     * @param bottom The bottom of this view, in pixels.
     */
    public final void setBottom(int bottom) {
        if (bottom != mBottom) {
            invalidate();

            int width = mRight - mLeft;
            int oldHeight = mBottom - mTop;

            mBottom = bottom;
            mRenderNode.setBottom(mBottom);

            sizeChange(width, mBottom - mTop, width, oldHeight);

            mBackgroundSizeChanged = true;
            mDefaultFocusHighlightSizeChanged = true;
            if (mForegroundInfo != null) {
                mForegroundInfo.mBoundsChanged = true;
            }
        }
    }

    private void sizeChange(int newWidth, int newHeight, int oldWidth, int oldHeight) {
        onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
        // If this isn't laid out yet, focus assignment will be handled during the "deferment/
        // backtracking" of requestFocus during layout, so don't touch focus here.
        if (isLayoutValid()
                // Don't touch focus if animating
                && !(mParent instanceof ViewGroup && ((ViewGroup) mParent).isLayoutSuppressed())) {
            if (newWidth <= 0 || newHeight <= 0) {
                if (hasFocus()) {
                    clearFocus();
                    //TODO
                    /*if (mParent instanceof ViewGroup) {
                        ((ViewGroup) mParent).clearFocusedInCluster();
                    }*/
                }
            } else if (oldWidth <= 0 || oldHeight <= 0) {
                if (mParent != null && canTakeFocus()) {
                    mParent.focusableViewAvailable(this);
                }
            }
        }
    }

    /**
     * The transform matrix of this view, which is calculated based on the current
     * rotation, scale, and pivot properties. This will affect this view logically
     * (such as for events) and visually.
     * <p>
     * Note that the matrix should be only used as read-only (like transforming
     * coordinates). In that case, you should call {@link #hasIdentityMatrix()}
     * first to check if the operation can be skipped, because this method will
     * return null if not available. However, if you want to change the
     * transform matrix, you should use methods such as {@link #setTranslationX(float)}.
     * The return value may be null, indicating that it is identity.
     *
     * @return the current transform matrix for the view, may be null if identity
     * @see #hasIdentityMatrix()
     * @see #getRotation()
     * @see #getScaleX()
     * @see #getScaleY()
     * @see #getPivotX()
     * @see #getPivotY()
     */
    public final Matrix4 getMatrix() {
        return mRenderNode.getMatrix();
    }

    /**
     * Utility method to retrieve the inverse of the current mMatrix property.
     * We cache the matrix to avoid recalculating it when transform properties
     * have not changed.
     *
     * @return The inverse of the current matrix of this view, may be null if identity
     * @see #hasIdentityMatrix()
     */
    public final Matrix4 getInverseMatrix() {
        return mRenderNode.getInverseMatrix();
    }

    /**
     * Returns true if the transform matrix is the identity matrix.
     *
     * @return true if the transform matrix is the identity matrix, false otherwise.
     * @see #getMatrix()
     */
    public final boolean hasIdentityMatrix() {
        Matrix4 matrix = mRenderNode.getMatrix();
        return matrix == null || matrix.isIdentity();
    }

    /**
     * The visual x position of this view, in pixels. This is equivalent to the
     * {@link #setTranslationX(float) translationX} property plus the current
     * {@link #getLeft() left} property.
     *
     * @return The visual x position of this view, in pixels.
     */
    public float getX() {
        return mLeft + getTranslationX();
    }

    /**
     * Sets the visual x position of this view, in pixels. This is equivalent to setting the
     * {@link #setTranslationX(float) translationX} property to be the difference between
     * the x value passed in and the current {@link #getLeft() left} property.
     *
     * @param x The visual x position of this view, in pixels.
     */
    public void setX(float x) {
        setTranslationX(x - mLeft);
    }

    /**
     * The visual y position of this view, in pixels. This is equivalent to the
     * {@link #setTranslationY(float) translationY} property plus the current
     * {@link #getTop() top} property.
     *
     * @return The visual y position of this view, in pixels.
     */
    public float getY() {
        return mTop + getTranslationY();
    }

    /**
     * Sets the visual y position of this view, in pixels. This is equivalent to setting the
     * {@link #setTranslationY(float) translationY} property to be the difference between
     * the y value passed in and the current {@link #getTop() top} property.
     *
     * @param y The visual y position of this view, in pixels.
     */
    public void setY(float y) {
        setTranslationY(y - mTop);
    }

    /**
     * The visual z position of this view, in pixels. This is equivalent to the
     * {@link #setTranslationZ(float) translationZ} property plus the current
     * {@link #getElevation() elevation} property.
     *
     * @return The visual z position of this view, in pixels.
     */
    public float getZ() {
        return getElevation() + getTranslationZ();
    }

    /**
     * Sets the visual z position of this view, in pixels. This is equivalent to setting the
     * {@link #setTranslationZ(float) translationZ} property to be the difference between
     * the z value passed in and the current {@link #getElevation() elevation} property.
     *
     * @param z The visual z position of this view, in pixels.
     */
    public void setZ(float z) {
        setTranslationZ(z - getElevation());
    }

    /**
     * The base elevation of this view relative to its parent, in pixels.
     *
     * @return The base depth position of the view, in pixels.
     */
    public float getElevation() {
        return mRenderNode.getElevation();
    }

    /**
     * Sets the base elevation of this view, in pixels.
     */
    public void setElevation(float elevation) {
        if (mRenderNode.setElevation(elevation)) {
            invalidate();
        }
    }

    /**
     * The horizontal location of this view relative to its {@link #getLeft() left} position.
     * This position is post-layout, in addition to wherever the object's
     * layout placed it.
     *
     * @return The horizontal position of this view relative to its left position, in pixels.
     */
    public float getTranslationX() {
        return mRenderNode.getTranslationX();
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
        if (mRenderNode.setTranslationX(translationX)) {
            invalidate();
        }
    }

    /**
     * The vertical location of this view relative to its {@link #getTop() top} position.
     * This position is post-layout, in addition to wherever the object's
     * layout placed it.
     *
     * @return The vertical position of this view relative to its top position,
     * in pixels.
     */
    public float getTranslationY() {
        return mRenderNode.getTranslationY();
    }

    /**
     * Sets the vertical location of this view relative to its {@link #getTop() top} position.
     * This effectively positions the object post-layout, in addition to wherever the object's
     * layout placed it.
     *
     * @param translationY The vertical position of this view relative to its top position,
     *                     in pixels.
     */
    public void setTranslationY(float translationY) {
        if (mRenderNode.setTranslationY(translationY)) {
            invalidate();
        }
    }

    /**
     * The depth location of this view relative to its {@link #getElevation() elevation}.
     *
     * @return The depth of this view relative to its elevation.
     */
    public float getTranslationZ() {
        return mRenderNode.getTranslationZ();
    }

    /**
     * Sets the depth location of this view relative to its {@link #getElevation() elevation}.
     */
    public void setTranslationZ(float translationZ) {
        if (mRenderNode.setTranslationZ(translationZ)) {
            invalidate();
        }
    }

    /**
     * The degrees that the view is rotated around the pivot point.
     *
     * @return The degrees of rotation.
     * @see #setRotation(float)
     * @see #getPivotX()
     * @see #getPivotY()
     */
    public float getRotation() {
        return mRenderNode.getRotationZ();
    }

    /**
     * Sets the degrees that the view is rotated around the pivot point. Increasing values
     * result in clockwise rotation.
     *
     * @param rotation The degrees of rotation.
     * @see #getRotation()
     * @see #getPivotX()
     * @see #getPivotY()
     * @see #setRotationX(float)
     * @see #setRotationY(float)
     */
    public void setRotation(float rotation) {
        if (mRenderNode.setRotationZ(rotation)) {
            invalidate();
        }
    }

    /**
     * The degrees that the view is rotated around the vertical axis through the pivot point.
     *
     * @return The degrees of Y rotation.
     * @see #getPivotX()
     * @see #getPivotY()
     * @see #setRotationY(float)
     */
    public float getRotationY() {
        return mRenderNode.getRotationY();
    }

    /**
     * Sets the degrees that the view is rotated around the vertical axis through the pivot point.
     * Increasing values result in counter-clockwise rotation from the viewpoint of looking
     * down the y-axis.
     *
     * @param rotationY The degrees of Y rotation.
     * @see #getRotationY()
     * @see #getPivotX()
     * @see #getPivotY()
     * @see #setRotation(float)
     * @see #setRotationX(float)
     */
    public void setRotationY(float rotationY) {
        if (mRenderNode.setRotationY(rotationY)) {
            invalidate();
        }
    }

    /**
     * The degrees that the view is rotated around the horizontal axis through the pivot point.
     *
     * @return The degrees of X rotation.
     * @see #getPivotX()
     * @see #getPivotY()
     * @see #setRotationX(float)
     */
    public float getRotationX() {
        return mRenderNode.getRotationX();
    }

    /**
     * Sets the degrees that the view is rotated around the horizontal axis through the pivot point.
     * Increasing values result in clockwise rotation from the viewpoint of looking down the
     * x-axis.
     *
     * @param rotationX The degrees of X rotation.
     * @see #getRotationX()
     * @see #getPivotX()
     * @see #getPivotY()
     * @see #setRotation(float)
     * @see #setRotationY(float)
     */
    public void setRotationX(float rotationX) {
        if (mRenderNode.setRotationX(rotationX)) {
            invalidate();
        }
    }

    /**
     * The amount that the view is scaled in x around the pivot point, as a proportion of
     * the view's unscaled width. A value of 1, the default, means that no scaling is applied.
     *
     * <p>By default, this is 1.0f.
     *
     * @return The scaling factor.
     * @see #getPivotX()
     * @see #getPivotY()
     */
    public float getScaleX() {
        return mRenderNode.getScaleX();
    }

    /**
     * Sets the amount that the view is scaled in x around the pivot point, as a proportion of
     * the view's unscaled width. A value of 1 means that no scaling is applied.
     *
     * @param scaleX The scaling factor.
     * @see #getPivotX()
     * @see #getPivotY()
     */
    public void setScaleX(float scaleX) {
        if (mRenderNode.setScaleX(scaleX)) {
            invalidate();
        }
    }

    /**
     * The amount that the view is scaled in y around the pivot point, as a proportion of
     * the view's unscaled height. A value of 1, the default, means that no scaling is applied.
     *
     * <p>By default, this is 1.0f.
     *
     * @return The scaling factor.
     * @see #getPivotX()
     * @see #getPivotY()
     */
    public float getScaleY() {
        return mRenderNode.getScaleY();
    }

    /**
     * Sets the amount that the view is scaled in Y around the pivot point, as a proportion of
     * the view's unscaled width. A value of 1 means that no scaling is applied.
     *
     * @param scaleY The scaling factor.
     * @see #getPivotX()
     * @see #getPivotY()
     */
    public void setScaleY(float scaleY) {
        if (mRenderNode.setScaleY(scaleY)) {
            invalidate();
        }
    }

    /**
     * The x location of the point around which the view is {@link #setRotation(float) rotated}
     * and {@link #setScaleX(float) scaled}.
     *
     * @return The x location of the pivot point.
     * @see #getRotation()
     * @see #getScaleX()
     * @see #getScaleY()
     * @see #getPivotY()
     */
    public float getPivotX() {
        return mRenderNode.getPivotX();
    }

    /**
     * Sets the x location of the point around which the view is
     * {@link #setRotation(float) rotated} and {@link #setScaleX(float) scaled}.
     * By default, the pivot point is centered on the object.
     * Setting this property disables this behavior and causes the view to use only the
     * explicitly set pivotX and pivotY values.
     *
     * @param pivotX The x location of the pivot point.
     * @see #getRotation()
     * @see #getScaleX()
     * @see #getScaleY()
     * @see #getPivotY()
     */
    public void setPivotX(float pivotX) {
        if (mRenderNode.setPivotX(pivotX)) {
            invalidate();
        }
    }

    /**
     * The y location of the point around which the view is {@link #setRotation(float) rotated}
     * and {@link #setScaleY(float) scaled}.
     *
     * @return The y location of the pivot point.
     * @see #getRotation()
     * @see #getScaleX()
     * @see #getScaleY()
     * @see #getPivotY()
     */
    public float getPivotY() {
        return mRenderNode.getPivotY();
    }

    /**
     * Sets the y location of the point around which the view is {@link #setRotation(float) rotated}
     * and {@link #setScaleY(float) scaled}. By default, the pivot point is centered on the object.
     * Setting this property disables this behavior and causes the view to use only the
     * explicitly set pivotX and pivotY values.
     *
     * @param pivotY The y location of the pivot point.
     * @see #getRotation()
     * @see #getScaleX()
     * @see #getScaleY()
     * @see #getPivotY()
     */
    public void setPivotY(float pivotY) {
        if (mRenderNode.setPivotY(pivotY)) {
            invalidate();
        }
    }

    /**
     * Returns whether a pivot has been set by a call to {@link #setPivotX(float)} or
     * {@link #setPivotY(float)}. If no pivot has been set then the pivot will be the center
     * of the view.
     *
     * @return True if a pivot has been set, false if the default pivot is being used
     */
    public boolean isPivotSet() {
        return mRenderNode.isPivotExplicitlySet();
    }

    /**
     * Clears any pivot previously set by a call to  {@link #setPivotX(float)} or
     * {@link #setPivotY(float)}. After calling this {@link #isPivotSet()} will be false
     * and the pivot used for rotation will return to default of being centered on the view.
     */
    public void resetPivot() {
        if (mRenderNode.resetPivot()) {
            invalidate();
        }
    }

    //TODO WIP
    public void setAlpha(float alpha) {
        if (mAlpha != alpha) {
            mAlpha = alpha;
            invalidate();
        }
    }

    /**
     * The opacity of the view. This is a value from 0 to 1, where 0 means the view is
     * completely transparent and 1 means the view is completely opaque.
     *
     * <p>By default this is 1.0f.
     *
     * @return The opacity of the view.
     */
    public float getAlpha() {
        return mAlpha;
    }

    /**
     * This property is intended only for use by the {@link Fade} transition, which animates it
     * to produce a visual translucency that does not side effect (or get affected by)
     * the real alpha property. This value is composited with the other alpha value
     * (and the AlphaAnimation value, when that is present) to produce a final visual
     * translucency result, which is what is passed into the DisplayList.
     */
    public final void setTransitionAlpha(float alpha) {
        if (mTransitionAlpha != alpha) {
            mTransitionAlpha = alpha;
            invalidate();
        }
    }

    /**
     * This property is intended only for use by the {@link Fade} transition, which animates
     * it to produce a visual translucency that does not side effect (or get affected
     * by) the real alpha property. This value is composited with the other alpha
     * value (and the AlphaAnimation value, when that is present) to produce a final
     * visual translucency result, which is what is passed into the DisplayList.
     */
    public final float getTransitionAlpha() {
        return mTransitionAlpha;
    }

    /**
     * Changes the transformation matrix on the view. This is used in animation frameworks,
     * such as {@link Transition}. When the animation finishes, the matrix
     * should be cleared by calling this method with <code>null</code> as the matrix parameter.
     * Application developers should use transformation methods like {@link #setRotation(float)},
     * {@link #setScaleX(float)}, {@link #setScaleX(float)}, {@link #setTranslationX(float)}}
     * and {@link #setTranslationY(float)} instead.
     *
     * @param matrix The matrix, null indicates that the matrix should be cleared.
     * @see #getAnimationMatrix()
     */
    public final void setAnimationMatrix(@Nullable Matrix4 matrix) {
        mRenderNode.setAnimationMatrix(matrix);
    }

    /**
     * Changes the transition matrix on the view. This is only used in the transition animation
     * framework, {@link Transition}. Returns <code>null</code> when there is no
     * transformation provided by {@link #setAnimationMatrix(Matrix4)}.
     * <p>
     * Note that this matrix only affects the visual effect of this view, you should never
     * call this method. You should use methods such as {@link #setTranslationX(float)}} instead
     * to change the transformation logically.
     *
     * @return the matrix, null indicates that the matrix should be cleared.
     * @see #setAnimationMatrix(Matrix4)
     */
    @Nullable
    public final Matrix4 getAnimationMatrix() {
        return mRenderNode.getAnimationMatrix();
    }

    /**
     * Returns the current StateListAnimator if exists.
     *
     * @return StateListAnimator or null if it does not exist
     * @see #setStateListAnimator(StateListAnimator)
     */
    public StateListAnimator getStateListAnimator() {
        return mStateListAnimator;
    }

    /**
     * Attaches the provided StateListAnimator to this View.
     * <p>
     * Any previously attached StateListAnimator will be detached.
     *
     * @param stateListAnimator The StateListAnimator to update the view
     * @see StateListAnimator
     */
    public void setStateListAnimator(@Nullable StateListAnimator stateListAnimator) {
        if (mStateListAnimator == stateListAnimator) {
            return;
        }
        if (mStateListAnimator != null) {
            mStateListAnimator.setTarget(null);
        }
        mStateListAnimator = stateListAnimator;
        if (stateListAnimator != null) {
            stateListAnimator.setTarget(this);
            if (isAttachedToWindow()) {
                stateListAnimator.setState(getDrawableState());
            }
        }
    }

    /**
     * Return the visibility value of the least visible component passed.
     */
    int combineVisibility(int vis1, int vis2) {
        // This works because VISIBLE < INVISIBLE < GONE.
        return Math.max(vis1, vis2);
    }

    void dispatchAttachedToWindow(AttachInfo info, int visibility) {
        mAttachInfo = info;
        mWindowAttachCount++;
        // We will need to evaluate the drawable state at least once.
        mPrivateFlags |= PFLAG_DRAWABLE_STATE_DIRTY;
        if (mFloatingTreeObserver != null) {
            info.mTreeObserver.merge(mFloatingTreeObserver);
            mFloatingTreeObserver = null;
        }
        // transfer all pending tasks
        if (mRunQueue != null) {
            mRunQueue.executeActions(info.mHandler);
            mRunQueue = null;
        }
        onAttachedToWindow();

        ListenerInfo li = mListenerInfo;
        final CopyOnWriteArrayList<OnAttachStateChangeListener> listeners =
                li != null ? li.mOnAttachStateChangeListeners : null;
        if (listeners != null && listeners.size() > 0) {
            // NOTE: because of the use of CopyOnWriteArrayList, we *must* use an iterator to
            // perform the dispatching. The iterator is a safeguard against listeners that
            // could mutate the list by calling the various add/remove methods. This prevents
            // the array from being modified while we iterate it.
            for (OnAttachStateChangeListener listener : listeners) {
                listener.onViewAttachedToWindow(this);
            }
        }

        int vis = info.mWindowVisibility;
        if (vis != GONE) {
            onWindowVisibilityChanged(vis);
            if (isShown()) {
                // Calling onVisibilityAggregated directly here since the subtree will also
                // receive dispatchAttachedToWindow and this same call
                onVisibilityAggregated(vis == VISIBLE);
            }
        }

        // Send onVisibilityChanged directly instead of dispatchVisibilityChanged.
        // As all views in the subtree will already receive dispatchAttachedToWindow
        // traversing the subtree again here is not desired.
        onVisibilityChanged(this, visibility);

        if ((mPrivateFlags & PFLAG_DRAWABLE_STATE_DIRTY) != 0) {
            // If nobody has evaluated the drawable state yet, then do it now.
            refreshDrawableState();
        }
    }

    void dispatchDetachedFromWindow() {
        AttachInfo info = mAttachInfo;
        if (info != null) {
            int vis = info.mWindowVisibility;
            if (vis != GONE) {
                onWindowVisibilityChanged(GONE);
                if (isShown()) {
                    // Invoking onVisibilityAggregated directly here since the subtree
                    // will also receive detached from window
                    onVisibilityAggregated(false);
                }
            }
        }

        onDetachedFromWindow();

        mPrivateFlags &= ~PFLAG_CANCEL_NEXT_UP_EVENT;
        mPrivateFlags3 &= ~PFLAG3_IS_LAID_OUT;
        mPrivateFlags3 &= ~PFLAG3_TEMPORARY_DETACH;

        removeUnsetPressCallback();
        removeLongPressCallback();
        removePerformClickCallback();
        stopNestedScroll(TYPE_TOUCH);

        // Anything that started animating right before detach should already
        // be in its final state when re-attached.
        jumpDrawablesToCurrentState();

        /*cleanupDraw();*/

        ListenerInfo li = mListenerInfo;
        final CopyOnWriteArrayList<OnAttachStateChangeListener> listeners =
                li != null ? li.mOnAttachStateChangeListeners : null;
        if (listeners != null && listeners.size() > 0) {
            // NOTE: because of the use of CopyOnWriteArrayList, we *must* use an iterator to
            // perform the dispatching. The iterator is a safe-guard against listeners that
            // could mutate the list by calling the various add/remove methods. This prevents
            // the array from being modified while we iterate it.
            for (OnAttachStateChangeListener listener : listeners) {
                listener.onViewDetachedFromWindow(this);
            }
        }

        mAttachInfo = null;
    }

    /**
     * This is called when the view is attached to a window.  At this point it
     * has a Surface and will start drawing.  Note that this function is
     * guaranteed to be called before {@link #onDraw(Canvas)},
     * however it may be called any time before the first onDraw -- including
     * before or after {@link #onMeasure(int, int)}.
     *
     * @see #onDetachedFromWindow()
     */
    @CallSuper
    protected void onAttachedToWindow() {
        mPrivateFlags3 &= ~PFLAG3_IS_LAID_OUT;

        jumpDrawablesToCurrentState();
    }

    /**
     * This is called when the view is detached from a window.  At this point it
     * no longer has a surface for drawing.
     *
     * @see #onAttachedToWindow()
     */
    protected void onDetachedFromWindow() {
    }

    /**
     * Returns true if this view is currently attached to a window.
     */
    public boolean isAttachedToWindow() {
        return mAttachInfo != null;
    }

    /**
     * @return The number of times this view has been attached to a window
     */
    protected int getWindowAttachCount() {
        return mWindowAttachCount;
    }

    /**
     * Indicates whether the view is currently tracking transient state that the
     * app should not need to concern itself with saving and restoring, but that
     * the framework should take special note to preserve when possible.
     *
     * <p>A view with transient state cannot be trivially rebound from an external
     * data source, such as an adapter binding item views in a list. This may be
     * because the view is performing an animation, tracking user selection
     * of content, or similar.</p>
     *
     * @return true if the view has transient state
     */
    public boolean hasTransientState() {
        return (mPrivateFlags2 & PFLAG2_HAS_TRANSIENT_STATE) == PFLAG2_HAS_TRANSIENT_STATE;
    }

    /**
     * Set whether this view is currently tracking transient state that the
     * framework should attempt to preserve when possible. This flag is reference counted,
     * so every call to setHasTransientState(true) should be paired with a later call
     * to setHasTransientState(false).
     *
     * <p>A view with transient state cannot be trivially rebound from an external
     * data source, such as an adapter binding item views in a list. This may be
     * because the view is performing an animation, tracking user selection
     * of content, or similar.</p>
     *
     * @param hasTransientState true if this view has transient state
     */
    public void setHasTransientState(boolean hasTransientState) {
        final boolean oldHasTransientState = hasTransientState();
        mTransientStateCount = hasTransientState ? mTransientStateCount + 1 :
                mTransientStateCount - 1;
        if (mTransientStateCount < 0) {
            mTransientStateCount = 0;
        } else if ((hasTransientState && mTransientStateCount == 1) ||
                (!hasTransientState && mTransientStateCount == 0)) {
            // update flag if we've just incremented up from 0 or decremented down to 0
            mPrivateFlags2 = (mPrivateFlags2 & ~PFLAG2_HAS_TRANSIENT_STATE) |
                    (hasTransientState ? PFLAG2_HAS_TRANSIENT_STATE : 0);
            final boolean newHasTransientState = hasTransientState();
            if (mParent != null && newHasTransientState != oldHasTransientState) {
                mParent.childHasTransientStateChanged(this, newHasTransientState);
            }
        }
    }

    /**
     * Set the view is tracking translation transient state. This flag is used to check if the view
     * need to call setHasTransientState(false) to reset transient state that set when starting
     * translation.
     *
     * @param hasTranslationTransientState true if this view has translation transient state
     * @hide
     */
    public void setHasTranslationTransientState(boolean hasTranslationTransientState) {
        if (hasTranslationTransientState) {
            mPrivateFlags4 |= PFLAG4_HAS_TRANSLATION_TRANSIENT_STATE;
        } else {
            mPrivateFlags4 &= ~PFLAG4_HAS_TRANSLATION_TRANSIENT_STATE;
        }
    }

    /**
     * @hide
     */
    public boolean hasTranslationTransientState() {
        return (mPrivateFlags4 & PFLAG4_HAS_TRANSLATION_TRANSIENT_STATE)
                == PFLAG4_HAS_TRANSLATION_TRANSIENT_STATE;
    }

    /**
     * Cancel any deferred high-level input events that were previously posted to the event queue.
     *
     * <p>Many views post high-level events such as click handlers to the event queue
     * to run deferred in order to preserve a desired user experience - clearing visible
     * pressed states before executing, etc. This method will abort any events of this nature
     * that are currently in flight.</p>
     *
     * <p>Custom views that generate their own high-level deferred input events should override
     * {@link #onCancelPendingInputEvents()} and remove those pending events from the queue.</p>
     *
     * <p>This will also cancel pending input events for any child views.</p>
     *
     * <p>Note that this may not be sufficient as a debouncing strategy for clicks in all cases.
     * This will not impact newer events posted after this call that may occur as a result of
     * lower-level input events still waiting in the queue. If you are trying to prevent
     * double-submitted  events for the duration of some sort of asynchronous transaction
     * you should also take other steps to protect against unexpected double inputs e.g. calling
     * {@link #setEnabled(boolean) setEnabled(false)} and re-enabling the view when
     * the transaction completes, tracking already submitted transaction IDs, etc.</p>
     */
    public final void cancelPendingInputEvents() {
        dispatchCancelPendingInputEvents();
    }

    /**
     * Called by {@link #cancelPendingInputEvents()} to cancel input events in flight.
     * Overridden by ViewGroup to dispatch. Package scoped to prevent app-side meddling.
     */
    void dispatchCancelPendingInputEvents() {
        mPrivateFlags3 &= ~PFLAG3_CALLED_SUPER;
        onCancelPendingInputEvents();
        if ((mPrivateFlags3 & PFLAG3_CALLED_SUPER) != PFLAG3_CALLED_SUPER) {
            throw new IllegalStateException("View " + getClass().getSimpleName() +
                    " did not call through to super.onCancelPendingInputEvents()");
        }
    }

    /**
     * Called as the result of a call to {@link #cancelPendingInputEvents()} on this view or
     * a parent view.
     *
     * <p>This method is responsible for removing any pending high-level input events that were
     * posted to the event queue to run later. Custom view classes that post their own deferred
     * high-level events via {@link #post(Runnable)}, {@link #postDelayed(Runnable, long)} or
     * {@link icyllis.modernui.core.Handler} should override this method, call
     * <code>super.onCancelPendingInputEvents()</code> and remove those callbacks as appropriate.
     * </p>
     */
    public void onCancelPendingInputEvents() {
        removePerformClickCallback();
        cancelLongPress();
        mPrivateFlags3 |= PFLAG3_CALLED_SUPER;
    }

    /**
     * <p>Enables or disables the duplication of the parent's state into this view. When
     * duplication is enabled, this view gets its drawable state from its parent rather
     * than from its own internal properties.</p>
     *
     * <p>Note: in the current implementation, setting this property to true after the
     * view was added to a ViewGroup might have no effect at all. This property should
     * always be used from XML or set to true before adding this view to a ViewGroup.</p>
     *
     * <p>Note: if this view's parent addStateFromChildren property is enabled and this
     * property is enabled, an exception will be thrown.</p>
     *
     * <p>Note: if the child view uses and updates additional states which are unknown to the
     * parent, these states should not be affected by this method.</p>
     *
     * @param enabled True to enable duplication of the parent's drawable state, false
     *                to disable it.
     * @see #getDrawableState()
     * @see #isDuplicateParentStateEnabled()
     */
    public void setDuplicateParentStateEnabled(boolean enabled) {
        setFlags(enabled ? DUPLICATE_PARENT_STATE : 0, DUPLICATE_PARENT_STATE);
    }

    /**
     * <p>Indicates whether this duplicates its drawable state from its parent.</p>
     *
     * @return True if this view's drawable state is duplicated from the parent,
     * false otherwise
     * @see #getDrawableState()
     * @see #setDuplicateParentStateEnabled(boolean)
     */
    public boolean isDuplicateParentStateEnabled() {
        return (mViewFlags & DUPLICATE_PARENT_STATE) == DUPLICATE_PARENT_STATE;
    }

    /**
     * Returns true if this view has been through at least one layout since it
     * was last attached to or detached from a window.
     */
    public boolean isLaidOut() {
        return (mPrivateFlags3 & PFLAG3_IS_LAID_OUT) == PFLAG3_IS_LAID_OUT;
    }

    /**
     * @return {@code true} if laid-out and not about to do another layout.
     */
    boolean isLayoutValid() {
        return isLaidOut() && ((mPrivateFlags & PFLAG_FORCE_LAYOUT) == 0);
    }

    /**
     * If this view doesn't do any drawing on its own, set this flag to
     * allow further optimizations. By default, this flag is not set on
     * View, but could be set on some View subclasses such as ViewGroup.
     * <p>
     * Typically, if you override {@link #onDraw(Canvas)}
     * you should clear this flag.
     *
     * @param willNotDraw whether this View draw on its own
     */
    public void setWillNotDraw(boolean willNotDraw) {
        setFlags(willNotDraw ? WILL_NOT_DRAW : 0, DRAW_MASK);
    }

    /**
     * Returns whether this View draws on its own.
     *
     * @return true if this view has nothing to draw, false otherwise
     */
    public boolean willNotDraw() {
        return (mViewFlags & DRAW_MASK) == WILL_NOT_DRAW;
    }

    /**
     * Set whether this view should have sound effects enabled for events such as
     * clicking and touching.
     *
     * <p>You may wish to disable sound effects for a view if you already play sounds,
     * for instance, a dial key that plays dtmf tones.
     *
     * @param soundEffectsEnabled whether sound effects are enabled for this view.
     * @see #isSoundEffectsEnabled()
     * @see #playSoundEffect(int)
     */
    public void setSoundEffectsEnabled(boolean soundEffectsEnabled) {
        setFlags(soundEffectsEnabled ? SOUND_EFFECTS_ENABLED : 0, SOUND_EFFECTS_ENABLED);
    }

    /**
     * @return whether this view should have sound effects enabled for events such as
     * clicking and touching.
     * @see #setSoundEffectsEnabled(boolean)
     * @see #playSoundEffect(int)
     */
    public boolean isSoundEffectsEnabled() {
        return SOUND_EFFECTS_ENABLED == (mViewFlags & SOUND_EFFECTS_ENABLED);
    }

    /**
     * Set whether this view should have haptic feedback for events such as
     * long presses.
     *
     * <p>You may wish to disable haptic feedback if your view already controls
     * its own haptic feedback.
     *
     * @param hapticFeedbackEnabled whether haptic feedback enabled for this view.
     * @see #isHapticFeedbackEnabled()
     * @see #performHapticFeedback(int)
     */
    public void setHapticFeedbackEnabled(boolean hapticFeedbackEnabled) {
        setFlags(hapticFeedbackEnabled ? HAPTIC_FEEDBACK_ENABLED : 0, HAPTIC_FEEDBACK_ENABLED);
    }

    /**
     * @return whether this view should have haptic feedback enabled for events
     * long presses.
     * @see #setHapticFeedbackEnabled(boolean)
     * @see #performHapticFeedback(int)
     */
    public boolean isHapticFeedbackEnabled() {
        return HAPTIC_FEEDBACK_ENABLED == (mViewFlags & HAPTIC_FEEDBACK_ENABLED);
    }

    /**
     * Play a sound effect for this view.
     *
     * <p>The framework will play sound effects for some built in actions, such as
     * clicking, but you may wish to play these effects in your widget,
     * for instance, for internal navigation.
     *
     * <p>The sound effect will only be played if sound effects are enabled by the user, and
     * {@link #isSoundEffectsEnabled()} is true.
     *
     * @param soundConstant One of the constants defined in {@link SoundEffectConstants}.
     */
    public void playSoundEffect(int soundConstant) {
        if (mAttachInfo == null || mAttachInfo.mRootCallbacks == null || !isSoundEffectsEnabled()) {
            return;
        }
        mAttachInfo.mRootCallbacks.playSoundEffect(soundConstant);
    }

    /**
     * BZZZTT!!1!
     *
     * <p>Provide haptic feedback to the user for this view.
     *
     * <p>The framework will provide haptic feedback for some built in actions,
     * such as long presses, but you may wish to provide feedback for your
     * own widget.
     *
     * <p>The feedback will only be performed if
     * {@link #isHapticFeedbackEnabled()} is true.
     *
     * @param feedbackConstant One of the constants defined in
     *                         {@link HapticFeedbackConstants}
     */
    public final boolean performHapticFeedback(int feedbackConstant) {
        return performHapticFeedback(feedbackConstant, 0);
    }

    /**
     * BZZZTT!!1!
     *
     * <p>Like {@link #performHapticFeedback(int)}, with additional options.
     *
     * @param feedbackConstant One of the constants defined in
     *                         {@link HapticFeedbackConstants}
     * @param flags            Additional flags as per {@link HapticFeedbackConstants}.
     */
    public boolean performHapticFeedback(int feedbackConstant, int flags) {
        if (mAttachInfo == null) {
            return false;
        }
        if ((flags & HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING) == 0
                && !isHapticFeedbackEnabled()) {
            return false;
        }
        return mAttachInfo.mRootCallbacks.performHapticFeedback(feedbackConstant,
                (flags & HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING) != 0);
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
            resolveRtlPropertiesIfNeeded();
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
     * Indicates whether this view's layout is right-to-left. This is resolved from
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
     * @return true if resolution of RTL properties has been done
     * @hide
     */
    public boolean resolveRtlPropertiesIfNeeded() {
        // Overall check
        if ((mPrivateFlags2 & ALL_RTL_PROPERTIES_RESOLVED) == ALL_RTL_PROPERTIES_RESOLVED) {
            return false;
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
        return true;
    }

    /**
     * Reset resolution of all RTL related properties.
     *
     * @hide
     */
    void resetRtlProperties() {
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

        if (ModernUI.getInstance().hasRtlSupport()) {
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
                        LOGGER.error(VIEW_MARKER, mParent.getClass().getSimpleName() +
                                " does not fully implement ViewParent", e);
                    }
                    break;
                case LAYOUT_DIRECTION_RTL:
                    mPrivateFlags2 |= PFLAG2_LAYOUT_DIRECTION_RESOLVED_RTL;
                    break;
                case LAYOUT_DIRECTION_LOCALE:
                    if (TextUtils.getLayoutDirectionFromLocale(ModernUI.getInstance().getSelectedLocale()) ==
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
                    LOGGER.error(VIEW_MARKER, mParent.getClass().getSimpleName() +
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

        if (ModernUI.getInstance().hasRtlSupport()) {
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
                        LOGGER.error(VIEW_MARKER, mParent.getClass().getSimpleName() +
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
                        LOGGER.error(VIEW_MARKER, mParent.getClass().getSimpleName() +
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
                    LOGGER.error(VIEW_MARKER, mParent.getClass().getSimpleName() +
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

        if (ModernUI.getInstance().hasRtlSupport()) {
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
                        LOGGER.error(VIEW_MARKER, mParent.getClass().getSimpleName() +
                                " does not fully implement ViewParent", e);
                        mPrivateFlags2 |= PFLAG2_TEXT_ALIGNMENT_RESOLVED |
                                PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT;
                        return true;
                    }
                    int parentResolvedTextAlignment;
                    try {
                        parentResolvedTextAlignment = mParent.getTextAlignment();
                    } catch (AbstractMethodError e) {
                        LOGGER.error(VIEW_MARKER, mParent.getClass().getSimpleName() +
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
                    LOGGER.error(VIEW_MARKER, mParent.getClass().getSimpleName() +
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

        if (ModernUI.getInstance().hasRtlSupport()) {
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
     * Returns whether the view hierarchy is currently undergoing a layout pass. This
     * information is useful to avoid situations such as calling {@link #requestLayout()} during
     * a layout pass.
     *
     * @return whether the view hierarchy is currently undergoing a layout pass
     */
    public final boolean isInLayout() {
        ViewRoot viewRoot = getViewRoot();
        return viewRoot != null && viewRoot.isInLayout();
    }

    /**
     * Call this when something has changed which has invalidated the
     * layout of this view. This will schedule a layout pass of the view
     * tree. This should not be called while the view hierarchy is currently in a layout
     * pass ({@link #isInLayout()}. If layout is happening, the request may be honored at the
     * end of the current layout pass (and then layout will run again) or after the current
     * frame is drawn and the next layout occurs.
     *
     * <p>Subclasses which override this method should call the superclass method to
     * handle possible request-during-layout errors correctly.</p>
     */
    @CallSuper
    public void requestLayout() {
        if (mAttachInfo != null && mAttachInfo.mViewRequestingLayout == null) {
            // Only trigger request-during-layout logic if this is the view requesting it,
            // not the views in its parent hierarchy
            ViewRoot viewRoot = getViewRoot();
            if (viewRoot != null && viewRoot.isInLayout()) {
                if (!viewRoot.requestLayoutDuringLayout(this)) {
                    return;
                }
            }
            mAttachInfo.mViewRequestingLayout = this;
        }

        mPrivateFlags |= PFLAG_FORCE_LAYOUT;
        mPrivateFlags |= PFLAG_INVALIDATED;

        if (mParent != null && !mParent.isLayoutRequested()) {
            mParent.requestLayout();
        }
        if (mAttachInfo != null && mAttachInfo.mViewRequestingLayout == this) {
            mAttachInfo.mViewRequestingLayout = null;
        }
    }

    /**
     * <p>Indicates whether or not this view's layout will be requested during
     * the next hierarchy layout pass.</p>
     *
     * @return true if the layout will be forced during next layout pass
     */
    public boolean isLayoutRequested() {
        return (mPrivateFlags & PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT;
    }

    /**
     * Forces this view to be laid out during the next layout pass.
     * This method does not call requestLayout() or forceLayout()
     * on the parent.
     */
    public void forceLayout() {
        mPrivateFlags |= PFLAG_FORCE_LAYOUT;
        mPrivateFlags |= PFLAG_INVALIDATED;
    }

    /**
     * This function is called whenever the view hotspot changes and needs to
     * be propagated to drawables or child views managed by the view.
     * <p>
     * Dispatching to child views is handled by
     * {@link #dispatchDrawableHotspotChanged(float, float)}.
     * <p>
     * Be sure to call through to the superclass when overriding this function.
     *
     * @param x hotspot x coordinate
     * @param y hotspot y coordinate
     */
    @CallSuper
    public void drawableHotspotChanged(float x, float y) {
        if (mBackground != null) {
            mBackground.setHotspot(x, y);
        }
        if (mDefaultFocusHighlight != null) {
            mDefaultFocusHighlight.setHotspot(x, y);
        }
        if (mForegroundInfo != null && mForegroundInfo.mDrawable != null) {
            mForegroundInfo.mDrawable.setHotspot(x, y);
        }

        dispatchDrawableHotspotChanged(x, y);
    }

    /**
     * Dispatches drawableHotspotChanged to all of this View's children.
     *
     * @param x hotspot x coordinate
     * @param y hotspot y coordinate
     * @see #drawableHotspotChanged(float, float)
     */
    public void dispatchDrawableHotspotChanged(float x, float y) {
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
        if (mForegroundInfo != null && mForegroundInfo.mDrawable != null) {
            mForegroundInfo.mDrawable.setLayoutDirection(layoutDirection);
        }
        if (mDefaultFocusHighlight != null) {
            mDefaultFocusHighlight.setLayoutDirection(layoutDirection);
        }
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
    public void onResolveDrawables(@ResolvedLayoutDir int layoutDirection) {
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
        if ((viewFlags & (SCROLLBARS_VERTICAL | SCROLLBARS_HORIZONTAL)) != 0) {
            if ((viewFlags & SCROLLBARS_VERTICAL) != 0) {
                final int offset = (viewFlags & SCROLLBARS_INSET_MASK) == 0
                        ? 0 : getVerticalScrollbarWidth();
                if (isLayoutRtl()) {
                    left += offset;
                } else {
                    right += offset;
                }
            }
            if ((viewFlags & SCROLLBARS_HORIZONTAL) != 0) {
                bottom += (viewFlags & SCROLLBARS_INSET_MASK) == 0
                        ? 0 : getHorizontalScrollbarHeight();
            }
        }

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
     * @return boolean If true then the Drawable is being displayed in the
     * view; else false and it is not allowed to animate.
     * @see #unscheduleDrawable(Drawable)
     * @see #drawableStateChanged()
     */
    @CallSuper
    protected boolean verifyDrawable(@Nonnull Drawable drawable) {
        // Avoid verifying the scroll bar drawable so that we don't end up in
        // an invalidation loop. This effectively prevents the scroll bar
        // drawable from triggering invalidations and scheduling runnables.
        return drawable == mBackground || (mForegroundInfo != null && mForegroundInfo.mDrawable == drawable)
                || (mDefaultFocusHighlight == drawable);
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
    @CallSuper
    protected void drawableStateChanged() {
        final int[] state = getDrawableState();
        boolean changed = false;

        final Drawable bg = mBackground;
        if (bg != null && bg.isStateful()) {
            changed |= bg.setState(state);
        }

        final Drawable hl = mDefaultFocusHighlight;
        if (hl != null && hl.isStateful()) {
            changed |= hl.setState(state);
        }

        final Drawable fg = mForegroundInfo != null ? mForegroundInfo.mDrawable : null;
        if (fg != null && fg.isStateful()) {
            changed |= fg.setState(state);
        }

        if (mScrollCache != null) {
            final Drawable scrollBar = mScrollCache.mScrollBar;
            if (scrollBar != null && scrollBar.isStateful()) {
                changed |= scrollBar.setState(state)
                        && mScrollCache.mState != ScrollCache.OFF;
            }
        }

        if (mStateListAnimator != null) {
            mStateListAnimator.setState(state);
        }

        if (!isAggregatedVisible()) {
            // If we're not visible, skip any animated changes
            jumpDrawablesToCurrentState();
        }

        if (changed) {
            invalidate();
        }
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
     * @return an array holding the current {@link Drawable} state of the view.
     * @see #mergeDrawableStates(int[], int[])
     */
    @Nonnull
    protected int[] onCreateDrawableState(int extraSpace) {
        if ((mViewFlags & DUPLICATE_PARENT_STATE) == DUPLICATE_PARENT_STATE &&
                mParent instanceof View) {
            return ((View) mParent).onCreateDrawableState(extraSpace);
        }

        int stateMask = 0;

        final int privateFlags = mPrivateFlags;
        if ((privateFlags & PFLAG_PRESSED) != 0) {
            stateMask |= StateSet.VIEW_STATE_PRESSED;
        }
        if ((mViewFlags & ENABLED_MASK) == ENABLED) {
            stateMask |= StateSet.VIEW_STATE_ENABLED;
        }
        if (isFocused()) {
            stateMask |= StateSet.VIEW_STATE_FOCUSED;
        }
        if ((privateFlags & PFLAG_SELECTED) != 0) {
            stateMask |= StateSet.VIEW_STATE_SELECTED;
        }
        if (hasWindowFocus()) {
            stateMask |= StateSet.VIEW_STATE_WINDOW_FOCUSED;
        }
        if ((privateFlags & PFLAG_ACTIVATED) != 0) {
            stateMask |= StateSet.VIEW_STATE_ACTIVATED;
        }
        if ((privateFlags & PFLAG_HOVERED) != 0) {
            stateMask |= StateSet.VIEW_STATE_HOVERED;
        }
        final int privateFlags2 = mPrivateFlags2;
        if ((privateFlags2 & PFLAG2_DRAG_CAN_ACCEPT) != 0) {
            stateMask |= StateSet.VIEW_STATE_DRAG_CAN_ACCEPT;
        }
        if ((privateFlags2 & PFLAG2_DRAG_HOVERED) != 0) {
            stateMask |= StateSet.VIEW_STATE_DRAG_HOVERED;
        }

        final int[] drawableState = StateSet.get(stateMask);

        if (extraSpace == 0) {
            return drawableState;
        }

        final int[] fullState;
        if (stateMask != 0) {
            fullState = new int[drawableState.length + extraSpace];
            System.arraycopy(drawableState, 0, fullState, 0, drawableState.length);
        } else {
            fullState = new int[extraSpace];
        }

        return fullState;
    }

    /**
     * Merge your own state values in <var>additionalState</var> into the base
     * state values <var>baseState</var> that were returned by
     * {@link #onCreateDrawableState(int)}.
     *
     * @param baseState       The base state values returned by
     *                        {@link #onCreateDrawableState(int)}, which will be modified to also hold your
     *                        own additional state values.
     * @param additionalState The additional state values you would like
     *                        added to <var>baseState</var>; this array is not modified.
     * @return As a convenience, the <var>baseState</var> array you originally
     * passed into the function is returned.
     * @see #onCreateDrawableState(int)
     */
    @Nonnull
    protected static int[] mergeDrawableStates(@Nonnull int[] baseState, @Nonnull int[] additionalState) {
        int i = baseState.length - 1;
        while (i >= 0 && baseState[i] == 0) {
            i--;
        }
        System.arraycopy(additionalState, 0, baseState, i + 1, additionalState.length);
        return baseState;
    }

    /**
     * Call {@link Drawable#jumpToCurrentState() Drawable.jumpToCurrentState()}
     * on all Drawable objects associated with this view.
     * <p>
     * Also calls {@link StateListAnimator#jumpToCurrentState()} if there is a StateListAnimator
     * attached to this view.
     */
    @CallSuper
    public void jumpDrawablesToCurrentState() {
        if (mBackground != null) {
            mBackground.jumpToCurrentState();
        }
        if (mStateListAnimator != null) {
            mStateListAnimator.jumpToCurrentState();
        }
        if (mDefaultFocusHighlight != null) {
            mDefaultFocusHighlight.jumpToCurrentState();
        }
        if (mForegroundInfo != null && mForegroundInfo.mDrawable != null) {
            mForegroundInfo.mDrawable.jumpToCurrentState();
        }
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

        boolean requestLayout = false;

        /*
         * Regardless of whether we're setting a new background or not, we want
         * to clear the previous drawable. setVisible first while we still have the callback set.
         */
        if (mBackground != null) {
            if (isAttachedToWindow()) {
                mBackground.setVisible(false, false);
            }
            mBackground.setCallback(null);
            unscheduleDrawable(mBackground);
        }

        if (background != null) {
            Rect padding = sThreadLocal.get();
            if (padding == null) {
                padding = new Rect();
                sThreadLocal.set(padding);
            }
            resetResolvedDrawablesInternal();
            background.setLayoutDirection(getLayoutDirection());
            if (background.getPadding(padding)) {
                resetResolvedPadding();
                if (background.getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
                    mUserPaddingLeftInitial = padding.right;
                    mUserPaddingRightInitial = padding.left;
                    internalSetPadding(padding.right, padding.top, padding.left, padding.bottom);
                } else {
                    mUserPaddingLeftInitial = padding.left;
                    mUserPaddingRightInitial = padding.right;
                    internalSetPadding(padding.left, padding.top, padding.right, padding.bottom);
                }
                mLeftPaddingDefined = false;
                mRightPaddingDefined = false;
            }

            // Compare the minimum sizes of the old Drawable and the new.  If there isn't an old or
            // if it has a different minimum size, we should layout again
            if (mBackground == null
                    || mBackground.getMinimumHeight() != background.getMinimumHeight()
                    || mBackground.getMinimumWidth() != background.getMinimumWidth()) {
                requestLayout = true;
            }

            // Set mBackground before we set this as the callback and start making other
            // background drawable state change calls. In particular, the setVisible call below
            // can result in drawables attempting to start animations or otherwise invalidate,
            // which requires the view set as the callback (us) to recognize the drawable as
            // belonging to it as per verifyDrawable.
            mBackground = background;

            // Set callback last, since the view may still be initializing.
            background.setCallback(this);

            if ((mPrivateFlags & PFLAG_SKIP_DRAW) != 0) {
                mPrivateFlags &= ~PFLAG_SKIP_DRAW;
                requestLayout = true;
            }
        } else {
            /* Remove the background */
            mBackground = null;
            if ((mViewFlags & WILL_NOT_DRAW) != 0
                    && (mDefaultFocusHighlight == null)
                    && (mForegroundInfo == null || mForegroundInfo.mDrawable == null)) {
                mPrivateFlags |= PFLAG_SKIP_DRAW;
            }

            /*
             * When the background is set, we try to apply its padding to this
             * View. When the background is removed, we don't touch this View's
             * padding. This is noted in the Javadocs. Hence, we don't need to
             * requestLayout(), the invalidate() below is sufficient.
             */

            // The old background's minimum size could have affected this
            // View's layout, so let's requestLayout
            requestLayout = true;
        }

        if (requestLayout) {
            requestLayout();
        }

        mBackgroundSizeChanged = true;
        invalidate();
    }

    /**
     * Gets the background drawable
     *
     * @return The drawable used as the background for this view, if any.
     * @see #setBackground(Drawable)
     */
    @Nullable
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
    @Nullable
    public Drawable getForeground() {
        return mForegroundInfo != null ? mForegroundInfo.mDrawable : null;
    }

    /**
     * Supply a Drawable that is to be rendered on top of all of the content in the view.
     *
     * @param foreground the Drawable to be drawn on top of the children
     */
    public void setForeground(@Nullable Drawable foreground) {
        if (mForegroundInfo == null) {
            if (foreground == null) {
                // Nothing to do.
                return;
            }
            mForegroundInfo = new ForegroundInfo();
        }

        if (foreground == mForegroundInfo.mDrawable) {
            // Nothing to do
            return;
        }

        if (mForegroundInfo.mDrawable != null) {
            if (isAttachedToWindow()) {
                mForegroundInfo.mDrawable.setVisible(false, false);
            }
            mForegroundInfo.mDrawable.setCallback(null);
            unscheduleDrawable(mForegroundInfo.mDrawable);
        }

        mForegroundInfo.mDrawable = foreground;
        mForegroundInfo.mBoundsChanged = true;
        if (foreground != null) {
            if ((mPrivateFlags & PFLAG_SKIP_DRAW) != 0) {
                mPrivateFlags &= ~PFLAG_SKIP_DRAW;
            }
            foreground.setLayoutDirection(getLayoutDirection());
            if (foreground.isStateful()) {
                foreground.setState(getDrawableState());
            }
            if (isAttachedToWindow()) {
                foreground.setVisible(getWindowVisibility() == VISIBLE && isShown(), false);
            }
            // Set callback last, since the view may still be initializing.
            foreground.setCallback(this);
        } else if ((mViewFlags & WILL_NOT_DRAW) != 0 && mBackground == null
                && (mDefaultFocusHighlight == null)) {
            mPrivateFlags |= PFLAG_SKIP_DRAW;
        }
        requestLayout();
        invalidate();
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
        return mForegroundInfo == null || mForegroundInfo.mInsidePadding;
    }

    /**
     * Describes how the foreground is positioned.
     *
     * @return foreground gravity.
     * @see #setForegroundGravity(int)
     */
    public int getForegroundGravity() {
        return mForegroundInfo != null ? mForegroundInfo.mGravity
                : Gravity.START | Gravity.TOP;
    }

    /**
     * Describes how the foreground is positioned. Defaults to START and TOP.
     *
     * @param gravity see {@link Gravity}
     * @see #getForegroundGravity()
     */
    public void setForegroundGravity(int gravity) {
        if (mForegroundInfo == null) {
            mForegroundInfo = new ForegroundInfo();
        }

        if (mForegroundInfo.mGravity != gravity) {
            if ((gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
                gravity |= Gravity.START;
            }

            if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
                gravity |= Gravity.TOP;
            }

            mForegroundInfo.mGravity = gravity;
            requestLayout();
        }
    }

    /**
     * Returns the ViewTreeObserver for this view's hierarchy. The view tree
     * observer can be used to get notifications when global events, like
     * layout, happen.
     * <p>
     * The returned ViewTreeObserver observer is not guaranteed to remain
     * valid for the lifetime of this View. If the caller of this method keeps
     * a long-lived reference to ViewTreeObserver, it should always check for
     * the return value of {@link ViewTreeObserver#isAlive()}.
     *
     * @return The ViewTreeObserver for this view's hierarchy.
     */
    public final ViewTreeObserver getViewTreeObserver() {
        if (mAttachInfo != null) {
            return mAttachInfo.mTreeObserver;
        }
        if (mFloatingTreeObserver == null) {
            mFloatingTreeObserver = new ViewTreeObserver();
        }
        return mFloatingTreeObserver;
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
     * Transforms a motion event from view-local coordinates to on-screen
     * coordinates.
     *
     * @param ev the view-local motion event
     * @return false if the transformation could not be applied
     * @hide
     */
    public boolean toGlobalMotionEvent(@Nonnull MotionEvent ev) {
        final AttachInfo info = mAttachInfo;
        if (info == null) {
            return false;
        }

        final Matrix4 m = info.mTmpMatrix;
        m.setIdentity();
        transformMatrixToGlobal(m);
        ev.transform(m);
        return true;
    }

    /**
     * Transforms a motion event from on-screen coordinates to view-local
     * coordinates.
     *
     * @param ev the on-screen motion event
     * @return false if the transformation could not be applied
     * @hide
     */
    public boolean toLocalMotionEvent(@Nonnull MotionEvent ev) {
        final AttachInfo info = mAttachInfo;
        if (info == null) {
            return false;
        }

        final Matrix4 m = info.mTmpMatrix;
        m.setIdentity();
        transformMatrixToLocal(m);
        ev.transform(m);
        return true;
    }

    /**
     * Modifies the input matrix such that it maps view-local coordinates to
     * on-screen coordinates.
     *
     * @param matrix input matrix to modify
     */
    public void transformMatrixToGlobal(@Nonnull Matrix4 matrix) {
        final ViewParent parent = mParent;
        if (parent instanceof final View vp) {
            vp.transformMatrixToGlobal(matrix);
            matrix.translate(-vp.mScrollX, -vp.mScrollY);
        }

        matrix.translate(mLeft, mTop);

        if (!hasIdentityMatrix()) {
            matrix.multiply(getMatrix());
        }
    }

    /**
     * Modifies the input matrix such that it maps on-screen coordinates to
     * view-local coordinates.
     *
     * @param matrix input matrix to modify
     */
    public void transformMatrixToLocal(@Nonnull Matrix4 matrix) {
        final ViewParent parent = mParent;
        if (parent instanceof final View vp) {
            vp.transformMatrixToLocal(matrix);
            matrix.postTranslate(vp.mScrollX, vp.mScrollY);
        }

        matrix.postTranslate(-mLeft, -mTop);

        if (!hasIdentityMatrix()) {
            matrix.postMultiply(getInverseMatrix());
        }
    }

    /**
     * <p>Computes the coordinates of this view in its window. The argument
     * must be an array of two integers. After the method returns, the array
     * contains the x and y location in that order.</p>
     *
     * @param outLocation an array of two integers in which to hold the coordinates
     */
    public void getLocationInWindow(@Nonnull int[] outLocation) {
        if (outLocation.length < 2) {
            throw new IllegalArgumentException("outLocation must be an array of two integers");
        }

        outLocation[0] = 0;
        outLocation[1] = 0;

        transformFromViewToWindowSpace(outLocation);
    }

    /**
     * @hide
     */
    public void transformFromViewToWindowSpace(@Nonnull int[] inOutLocation) {
        if (inOutLocation.length < 2) {
            throw new IllegalArgumentException("inOutLocation must be an array of two integers");
        }

        if (mAttachInfo == null) {
            // When the view is not attached to a window, this method does not make sense
            inOutLocation[0] = inOutLocation[1] = 0;
            return;
        }

        float[] position = mAttachInfo.mTmpTransformLocation;
        position[0] = inOutLocation[0];
        position[1] = inOutLocation[1];

        if (!hasIdentityMatrix()) {
            getMatrix().transformPoint(position);
        }

        position[0] += mLeft;
        position[1] += mTop;

        ViewParent viewParent = mParent;
        while (viewParent instanceof final View view) {

            position[0] -= view.mScrollX;
            position[1] -= view.mScrollY;

            if (!view.hasIdentityMatrix()) {
                view.getMatrix().transformPoint(position);
            }

            position[0] += view.mLeft;
            position[1] += view.mTop;

            viewParent = view.mParent;
        }

        inOutLocation[0] = Math.round(position[0]);
        inOutLocation[1] = Math.round(position[1]);
    }

    /**
     * Look for a child view that matches the specified predicate.
     * If this view matches the predicate, return this view.
     *
     * @param predicate The predicate to evaluate.
     * @return The first view that matches the predicate or null.
     * @hide
     */
    public final <T extends View> T findViewByPredicate(@Nonnull Predicate<View> predicate) {
        return findViewByPredicateTraversal(predicate, null);
    }

    /**
     * @param predicate   The predicate to evaluate.
     * @param childToSkip If not null, ignores this child during the recursive traversal.
     * @return The first view that matches the predicate or null.
     * @hide
     */
    @Nullable
    @SuppressWarnings("unchecked")
    protected <T extends View> T findViewByPredicateTraversal(@Nonnull Predicate<View> predicate,
                                                              @Nullable View childToSkip) {
        if (predicate.test(this)) {
            return (T) this;
        }
        return null;
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
    public final <T extends View> T requireViewById(int id) {
        T view = findViewById(id);
        if (view == null) {
            throw new IllegalArgumentException("ID does not reference a View inside this View");
        }
        return view;
    }

    /**
     * @param id the id of the view to be found
     * @return the view of the specified id, null if cannot be found
     * @hide
     */
    @Nullable
    @SuppressWarnings("unchecked")
    protected <T extends View> T findViewTraversal(int id) {
        if (id == mID) {
            return (T) this;
        }
        return null;
    }

    /**
     * @return A handler associated with the thread running the View. This
     * handler can be used to pump events in the UI events queue.
     */
    @Nullable
    public final Handler getHandler() {
        final AttachInfo attachInfo = mAttachInfo;
        if (attachInfo != null) {
            return attachInfo.mHandler;
        }
        return null;
    }

    /**
     * Returns the queue of runnable for this view.
     *
     * @return the queue of runnable for this view
     */
    @Nonnull
    private HandlerActionQueue getRunQueue() {
        if (mRunQueue == null) {
            mRunQueue = new HandlerActionQueue();
        }
        return mRunQueue;
    }

    /**
     * Gets the view root associated with the View.
     *
     * @return The view root, or null if none.
     */
    @Nullable
    public final ViewRoot getViewRoot() {
        if (mAttachInfo != null) {
            return mAttachInfo.mViewRoot;
        }
        return null;
    }

    /**
     * <p>Causes the Runnable to be added to the task queue.
     * The runnable will be run on the UI thread.</p>
     *
     * @param action the runnable that will be executed.
     * @return true if the runnable was successfully placed in to the queue
     * @see #postDelayed
     * @see #removeCallbacks
     */
    public final boolean post(@Nonnull Runnable action) {
        final AttachInfo attachInfo = mAttachInfo;
        if (attachInfo != null) {
            return attachInfo.mHandler.post(action);
        }

        // Postpone the runnable until we know on which thread it needs to run.
        // Assume that the runnable will be successfully placed after attach.
        getRunQueue().post(action);
        return true;
    }

    /**
     * <p>Causes the Runnable to be added to the task queue, to be run
     * after the specified amount of time elapses.
     * The runnable will be run on the UI thread.</p>
     * <p>
     * Note that a result of true does not mean the Runnable will be processed --
     * if the application is shutdown before the delivery time of the message
     * occurs then the message will be dropped.
     *
     * @param action      the runnable that will be executed.
     * @param delayMillis the delay (in milliseconds) until the runnable will be executed.
     * @return true if the runnable was successfully placed in to the queue
     * @see #post
     * @see #removeCallbacks
     */
    public final boolean postDelayed(@Nonnull Runnable action, long delayMillis) {
        final AttachInfo attachInfo = mAttachInfo;
        if (attachInfo != null) {
            return attachInfo.mHandler.postDelayed(action, delayMillis);
        }

        // Postpone the runnable until we know on which thread it needs to run.
        // Assume that the runnable will be successfully placed after attach.
        getRunQueue().postDelayed(action, delayMillis);
        return true;
    }

    /**
     * <p>Causes the runnable to execute on the next animation time step.
     * The runnable will be run on the UI thread.</p>
     *
     * @param action the runnable that will be executed
     * @see #postOnAnimationDelayed
     * @see #removeCallbacks
     */
    public final void postOnAnimation(@Nonnull Runnable action) {
        final AttachInfo attachInfo = mAttachInfo;
        if (attachInfo != null) {
            attachInfo.mViewRoot.mChoreographer.postCallback(
                    Choreographer.CALLBACK_ANIMATION, action, null);
        } else {
            // Postpone the runnable until we know
            // on which thread it needs to run.
            getRunQueue().post(action);
        }
    }

    /**
     * <p>Causes the Runnable to execute on the next animation time step,
     * after the specified amount of time elapses.
     * The runnable will be run on the UI thread.</p>
     *
     * @param action      the runnable that will be executed
     * @param delayMillis the delay (in milliseconds) until the runnable will be executed
     * @see #postOnAnimation
     * @see #removeCallbacks
     */
    public final void postOnAnimationDelayed(@Nonnull Runnable action, long delayMillis) {
        final AttachInfo attachInfo = mAttachInfo;
        if (attachInfo != null) {
            attachInfo.mViewRoot.mChoreographer.postCallbackDelayed(
                    Choreographer.CALLBACK_ANIMATION, action, null, delayMillis);
        } else {
            // Postpone the runnable until we know
            // on which thread it needs to run.
            getRunQueue().postDelayed(action, delayMillis);
        }
    }

    /**
     * <p>Removes the specified Runnable from the message queue.</p>
     *
     * @param action the runnable to remove from the task handling queue
     * @see #post
     * @see #postDelayed
     * @see #postOnAnimation
     * @see #postOnAnimationDelayed
     */
    public final void removeCallbacks(@Nullable Runnable action) {
        if (action != null) {
            final AttachInfo attachInfo = mAttachInfo;
            if (attachInfo != null) {
                attachInfo.mHandler.removeCallbacks(action);
                attachInfo.mViewRoot.mChoreographer.removeCallbacks(
                        Choreographer.CALLBACK_ANIMATION, action, null);
            }
            if (mRunQueue != null) {
                mRunQueue.removeCallbacks(action);
            }
        }
    }

    /**
     * <p>Cause an invalidate to happen on a subsequent cycle through the event loop.
     * Use this to invalidate the View from a non-UI thread.</p>
     *
     * <p>This method can be invoked from outside of the UI thread
     * only when this View is attached to a window.</p>
     *
     * @see #invalidate()
     * @see #postInvalidateDelayed(long)
     */
    public final void postInvalidate() {
        postInvalidateDelayed(0);
    }

    /**
     * <p>Cause an invalidate to happen on a subsequent cycle through the event
     * loop. Waits for the specified amount of time.</p>
     *
     * <p>This method can be invoked from outside of the UI thread
     * only when this View is attached to a window.</p>
     *
     * @param delayMillis the duration in milliseconds to delay the
     *                    invalidation by
     * @see #invalidate()
     * @see #postInvalidate()
     */
    public final void postInvalidateDelayed(long delayMilliseconds) {
        // We try only with the AttachInfo because there's no point in invalidating
        // if we are not attached to our window
        final AttachInfo attachInfo = mAttachInfo;
        if (attachInfo != null) {
            attachInfo.mViewRoot.dispatchInvalidateDelayed(this, delayMilliseconds);
        }
    }

    /**
     * <p>Cause an invalidate to happen on the next animation time step, typically the
     * next display frame.</p>
     *
     * <p>This method can be invoked from outside of the UI thread
     * only when this View is attached to a window.</p>
     *
     * @see #invalidate()
     */
    public final void postInvalidateOnAnimation() {
        // We try only with the AttachInfo because there's no point in invalidating
        // if we are not attached to our window
        final AttachInfo attachInfo = mAttachInfo;
        if (attachInfo != null) {
            attachInfo.mViewRoot.dispatchInvalidateOnAnimation(this);
        }
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
            LOGGER.error(VIEW_MARKER, "startDragAndDrop called out of a window");
            return false;
        }
        return mAttachInfo.mViewRoot.startDragAndDrop(this, localState, shadow, flags);
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
     * Dispatch a view visibility change down the view hierarchy.
     * ViewGroups should override to route to their children.
     *
     * @param changedView The view whose visibility changed. Could be 'this' or
     *                    an ancestor view.
     * @param visibility  The new visibility of changedView: {@link #VISIBLE},
     *                    {@link #INVISIBLE} or {@link #GONE}.
     */
    protected void dispatchVisibilityChanged(@Nonnull View changedView,
                                             int visibility) {
        onVisibilityChanged(changedView, visibility);
    }

    /**
     * Called when the visibility of the view or an ancestor of the view has
     * changed.
     *
     * @param changedView The view whose visibility changed. May be
     *                    {@code this} or an ancestor view.
     * @param visibility  The new visibility, one of {@link #VISIBLE},
     *                    {@link #INVISIBLE} or {@link #GONE}.
     */
    protected void onVisibilityChanged(@Nonnull View changedView, int visibility) {
    }

    /**
     * Returns the current visibility of the window this view is attached to
     * (either {@link #GONE}, {@link #INVISIBLE}, or {@link #VISIBLE}).
     *
     * @return Returns the current visibility of the view's window.
     */
    public final int getWindowVisibility() {
        return mAttachInfo != null ? mAttachInfo.mWindowVisibility : GONE;
    }

    /**
     * Dispatch a window visibility change down the view hierarchy.
     * ViewGroups should override to route to their children.
     *
     * @param visibility The new visibility of the window.
     * @see #onWindowVisibilityChanged(int)
     */
    public void dispatchWindowVisibilityChanged(@Visibility int visibility) {
        onWindowVisibilityChanged(visibility);
    }

    /**
     * Called when the window containing has change its visibility
     * (between {@link #GONE}, {@link #INVISIBLE}, and {@link #VISIBLE}).  Note
     * that this tells you whether or not your window is being made visible
     * to the window manager; this does <em>not</em> tell you whether or not
     * your window is obscured by other windows on the screen, even if it
     * is itself visible.
     *
     * @param visibility The new visibility of the window.
     */
    protected void onWindowVisibilityChanged(@Visibility int visibility) {
        if (visibility == VISIBLE) {
            initialAwakenScrollBars();
        }
    }

    /**
     * @return true if this view and all ancestors are visible as of the last
     * {@link #onVisibilityAggregated(boolean)} call.
     */
    boolean isAggregatedVisible() {
        return (mPrivateFlags3 & PFLAG3_AGGREGATED_VISIBLE) != 0;
    }

    /**
     * Internal dispatching method for {@link #onVisibilityAggregated}. Overridden by
     * ViewGroup. Intended to only be called when {@link #isAttachedToWindow()},
     * {@link #getWindowVisibility()} is {@link #VISIBLE} and this view's parent {@link #isShown()}.
     *
     * @param isVisible true if this view's visibility to the user is uninterrupted by its
     *                  ancestors or by window visibility
     * @return true if this view is visible to the user, not counting clipping or overlapping
     */
    boolean dispatchVisibilityAggregated(boolean isVisible) {
        final boolean thisVisible = getVisibility() == VISIBLE;
        // If we're not visible but something is telling us we are, ignore it.
        if (thisVisible || !isVisible) {
            onVisibilityAggregated(isVisible);
        }
        return thisVisible && isVisible;
    }

    /**
     * Called when the user-visibility of this View is potentially affected by a change
     * to this view itself, an ancestor view or the window this view is attached to.
     *
     * @param isVisible true if this view and all of its ancestors are {@link #VISIBLE}
     *                  and this view's window is also visible
     */
    @CallSuper
    public void onVisibilityAggregated(boolean isVisible) {
        // Update our internal visibility tracking so we can detect changes
        boolean oldVisible = isAggregatedVisible();
        mPrivateFlags3 = isVisible ? (mPrivateFlags3 | PFLAG3_AGGREGATED_VISIBLE)
                : (mPrivateFlags3 & ~PFLAG3_AGGREGATED_VISIBLE);
        if (isVisible && mAttachInfo != null) {
            initialAwakenScrollBars();
        }

        final Drawable dr = mBackground;
        if (dr != null && isVisible != dr.isVisible()) {
            dr.setVisible(isVisible, false);
        }
        final Drawable hl = mDefaultFocusHighlight;
        if (hl != null && isVisible != hl.isVisible()) {
            hl.setVisible(isVisible, false);
        }
        final Drawable fg = mForegroundInfo != null ? mForegroundInfo.mDrawable : null;
        if (fg != null && isVisible != fg.isVisible()) {
            fg.setVisible(isVisible, false);
        }
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
     * Called when the window containing this view gains or loses window focus.
     * ViewGroups should override to route to their children.
     *
     * @param hasFocus True if the window containing this view now has focus,
     *                 false otherwise.
     */
    public void dispatchWindowFocusChanged(boolean hasFocus) {
        onWindowFocusChanged(hasFocus);
    }

    /**
     * Called when the window containing this view gains or loses focus.  Note
     * that this is separate from view focus: to receive key events, both
     * your view and its window must have focus.  If a window is displayed
     * on top of yours that takes input focus, then your own window will lose
     * focus but the view focus will remain unchanged.
     *
     * @param hasWindowFocus True if the window containing this view now has
     *                       focus, false otherwise.
     */
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) {
            if (isPressed()) {
                setPressed(false);
            }
            mPrivateFlags3 &= ~PFLAG3_FINGER_DOWN;
            removeLongPressCallback();
            removeTapCallback();
            resetPressedState();
        }

        refreshDrawableState();
    }

    /**
     * Returns true if this view is in a window that currently has window focus.
     * Note that this is not the same as the view itself having focus.
     *
     * @return True if this view is in a window that currently has window focus.
     */
    public boolean hasWindowFocus() {
        return mAttachInfo != null && mAttachInfo.mHasWindowFocus;
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
    public boolean dispatchGenericMotionEvent(@Nonnull MotionEvent event) {
        final int action = event.getAction();
        if (action == MotionEvent.ACTION_HOVER_ENTER
                || action == MotionEvent.ACTION_HOVER_MOVE
                || action == MotionEvent.ACTION_HOVER_EXIT) {
            if (dispatchHoverEvent(event)) {
                return true;
            }
        } else if (dispatchGenericPointerEvent(event)) {
            return true;
        }
        return dispatchGenericMotionEventInternal(event);
    }

    private boolean dispatchGenericMotionEventInternal(MotionEvent event) {
        //noinspection SimplifiableIfStatement
        ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnGenericMotionListener != null
                && (mViewFlags & ENABLED_MASK) == ENABLED
                && li.mOnGenericMotionListener.onGenericMotion(this, event)) {
            return true;
        }

        if (onGenericMotionEvent(event)) {
            return true;
        }

        final int actionButton = event.getActionButton();
        switch (event.getAction()) {
            case MotionEvent.ACTION_BUTTON_PRESS:
                if (isContextClickable() && !mInContextButtonPress && !mHasPerformedLongPress
                        && (actionButton == MotionEvent.BUTTON_SECONDARY)) {
                    if (performContextClick(event.getX(), event.getY())) {
                        mInContextButtonPress = true;
                        setPressed(event.getX(), event.getY());
                        removeTapCallback();
                        removeLongPressCallback();
                        return true;
                    }
                }
                break;

            case MotionEvent.ACTION_BUTTON_RELEASE:
                if (mInContextButtonPress && (actionButton == MotionEvent.BUTTON_SECONDARY)) {
                    mInContextButtonPress = false;
                    mIgnoreNextUpEvent = true;
                }
                break;
        }
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
    protected boolean dispatchHoverEvent(@Nonnull MotionEvent event) {
        ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnHoverListener != null
                && (mViewFlags & ENABLED_MASK) == ENABLED
                && li.mOnHoverListener.onHover(this, event)) {
            return true;
        }

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
    protected boolean dispatchGenericPointerEvent(@Nonnull MotionEvent event) {
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
    public boolean onGenericMotionEvent(@Nonnull MotionEvent event) {
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
    public boolean onHoverEvent(@Nonnull MotionEvent event) {
        final int action = event.getAction();

        if ((action == MotionEvent.ACTION_HOVER_ENTER || action == MotionEvent.ACTION_HOVER_MOVE)
                && isOnScrollbar(event.getX(), event.getY())) {
            awakenScrollBars();
        }

        // If we consider ourselves hoverable, or if we're already hovered,
        // handle changing state in response to ENTER and EXIT events.
        if (isHoverable() || isHovered()) {
            switch (action) {
                case MotionEvent.ACTION_HOVER_ENTER -> setHovered(true);
                case MotionEvent.ACTION_HOVER_EXIT -> setHovered(false);
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
     * Register a callback to be invoked when the scroll X or Y positions of
     * this view change.
     * <p>
     * <b>Note:</b> Some views handle scrolling independently of View and may
     * have their own separate listeners for scroll-type events.
     *
     * @param l The listener to notify when the scroll X or Y position changes.
     * @see #getScrollX()
     * @see #getScrollY()
     */
    public void setOnScrollChangeListener(@Nullable OnScrollChangeListener l) {
        if (l == null && mListenerInfo == null) {
            return;
        }
        getListenerInfo().mOnScrollChangeListener = l;
    }

    /**
     * Register a callback to be invoked when focus of this view changed.
     *
     * @param l The callback that will run.
     */
    public void setOnFocusChangeListener(@Nullable OnFocusChangeListener l) {
        if (l == null && mListenerInfo == null) {
            return;
        }
        getListenerInfo().mOnFocusChangeListener = l;
    }

    /**
     * Add a listener that will be called when the bounds of the view change due to
     * layout processing.
     *
     * @param listener The listener that will be called when layout bounds change.
     */
    public void addOnLayoutChangeListener(@Nonnull OnLayoutChangeListener listener) {
        ListenerInfo li = getListenerInfo();
        if (li.mOnLayoutChangeListeners == null) {
            li.mOnLayoutChangeListeners = new ArrayList<>();
        }
        if (!li.mOnLayoutChangeListeners.contains(listener)) {
            li.mOnLayoutChangeListeners.add(listener);
        }
    }

    /**
     * Remove a listener for layout changes.
     *
     * @param listener The listener for layout bounds change.
     */
    public void removeOnLayoutChangeListener(@Nonnull OnLayoutChangeListener listener) {
        ListenerInfo li = mListenerInfo;
        if (li == null || li.mOnLayoutChangeListeners == null) {
            return;
        }
        li.mOnLayoutChangeListeners.remove(listener);
    }

    /**
     * Add a listener for attach state changes.
     * <p>
     * This listener will be called whenever this view is attached or detached
     * from a window. Remove the listener using
     * {@link #removeOnAttachStateChangeListener(OnAttachStateChangeListener)}.
     *
     * @param listener Listener to attach
     * @see #removeOnAttachStateChangeListener(OnAttachStateChangeListener)
     */
    public void addOnAttachStateChangeListener(@Nonnull OnAttachStateChangeListener listener) {
        ListenerInfo li = getListenerInfo();
        if (li.mOnAttachStateChangeListeners == null) {
            li.mOnAttachStateChangeListeners = new CopyOnWriteArrayList<>();
        }
        li.mOnAttachStateChangeListeners.add(listener);
    }

    /**
     * Remove a listener for attach state changes. The listener will receive no further
     * notification of window attach/detach events.
     *
     * @param listener Listener to remove
     * @see #addOnAttachStateChangeListener(OnAttachStateChangeListener)
     */
    public void removeOnAttachStateChangeListener(@Nonnull OnAttachStateChangeListener listener) {
        ListenerInfo li = mListenerInfo;
        if (li == null || li.mOnAttachStateChangeListeners == null) {
            return;
        }
        li.mOnAttachStateChangeListeners.remove(listener);
    }

    /**
     * Returns the focus-change callback registered for this view.
     *
     * @return The callback, or null if one is not registered.
     */
    @Nullable
    public OnFocusChangeListener getOnFocusChangeListener() {
        ListenerInfo li = mListenerInfo;
        return li != null ? li.mOnFocusChangeListener : null;
    }

    /**
     * Register a callback to be invoked when this view is clicked. If this view is not
     * clickable, it becomes clickable.
     *
     * @param l The callback that will run
     * @see #setClickable(boolean)
     */
    public void setOnClickListener(@Nullable OnClickListener l) {
        if (l == null && mListenerInfo == null) {
            return;
        }
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

    /**
     * Register a callback to be invoked when this view is clicked and held. If this view is not
     * long clickable, it becomes long clickable.
     *
     * @param l The callback that will run
     * @see #setLongClickable(boolean)
     */
    public void setOnLongClickListener(@Nullable OnLongClickListener l) {
        if (l == null && mListenerInfo == null) {
            return;
        }
        if (!isLongClickable()) {
            setLongClickable(true);
        }
        getListenerInfo().mOnLongClickListener = l;
    }

    /**
     * Return whether this view has an attached OnLongClickListener.  Returns
     * true if there is a listener, false if there is none.
     */
    public boolean hasOnLongClickListeners() {
        ListenerInfo li = mListenerInfo;
        return (li != null && li.mOnLongClickListener != null);
    }

    /**
     * @return the registered {@link OnLongClickListener} if there is one, {@code null} otherwise.
     * @hide
     */
    @Nullable
    public OnLongClickListener getOnLongClickListener() {
        ListenerInfo li = mListenerInfo;
        return (li != null) ? li.mOnLongClickListener : null;
    }

    /**
     * Register a callback to be invoked when this view is context clicked. If the view is not
     * context clickable, it becomes context clickable.
     *
     * @param l The callback that will run
     * @see #setContextClickable(boolean)
     */
    public void setOnContextClickListener(@Nullable OnContextClickListener l) {
        if (l == null && mListenerInfo == null) {
            return;
        }
        if (!isContextClickable()) {
            setContextClickable(true);
        }
        getListenerInfo().mOnContextClickListener = l;
    }

    /**
     * Register a callback to be invoked when the context menu for this view is
     * being built. If this view is not long clickable, it becomes long clickable.
     *
     * @param l The callback that will run
     */
    public void setOnCreateContextMenuListener(@Nullable OnCreateContextMenuListener l) {
        if (l == null && mListenerInfo == null) {
            return;
        }
        if (!isLongClickable()) {
            setLongClickable(true);
        }
        getListenerInfo().mOnCreateContextMenuListener = l;
    }

    /**
     * Register a callback to be invoked when a hardware key is pressed in this view.
     * Key presses in software input methods will generally not trigger the methods of
     * this listener.
     *
     * @param l the key listener to attach to this view
     */
    public void setOnKeyListener(@Nullable OnKeyListener l) {
        if (l == null && mListenerInfo == null) {
            return;
        }
        getListenerInfo().mOnKeyListener = l;
    }

    /**
     * Register a callback to be invoked when a touch event is sent to this view.
     *
     * @param l the touch listener to attach to this view
     */
    public void setOnTouchListener(@Nullable OnTouchListener l) {
        if (l == null && mListenerInfo == null) {
            return;
        }
        getListenerInfo().mOnTouchListener = l;
    }

    /**
     * Register a callback to be invoked when a generic motion event is sent to this view.
     *
     * @param l the generic motion listener to attach to this view
     */
    public void setOnGenericMotionListener(@Nullable OnGenericMotionListener l) {
        if (l == null && mListenerInfo == null) {
            return;
        }
        getListenerInfo().mOnGenericMotionListener = l;
    }

    /**
     * Register a callback to be invoked when a hover event is sent to this view.
     *
     * @param l the hover listener to attach to this view
     */
    public void setOnHoverListener(@Nullable OnHoverListener l) {
        if (l == null && mListenerInfo == null) {
            return;
        }
        getListenerInfo().mOnHoverListener = l;
    }

    /**
     * Register a drag event listener callback object for this View. The parameter is
     * an implementation of {@link OnDragListener}. To send a drag event to a
     * View, the system calls the
     * {@link OnDragListener#onDrag(View, DragEvent)} method.
     *
     * @param l An implementation of {@link OnDragListener}.
     */
    public void setOnDragListener(@Nullable OnDragListener l) {
        if (l == null && mListenerInfo == null) {
            return;
        }
        getListenerInfo().mOnDragListener = l;
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
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            // Defensive cleanup for new gesture
            stopNestedScroll(TYPE_TOUCH);
        }

        boolean result = (mViewFlags & ENABLED_MASK) == ENABLED && handleScrollBarDragging(event);

        ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnTouchListener != null
                && (mViewFlags & ENABLED_MASK) == ENABLED
                && li.mOnTouchListener.onTouch(this, event)) {
            result = true;
        }

        if (!result && onTouchEvent(event)) {
            result = true;
        }

        // Clean up after nested scrolls if this is the end of a gesture;
        // also cancel it if we tried an ACTION_DOWN but we didn't want the rest
        // of the gesture.
        if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_CANCEL ||
                (action == MotionEvent.ACTION_DOWN && !result)) {
            stopNestedScroll(TYPE_TOUCH);
        }

        return result;
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
        final float x = event.getX();
        final float y = event.getY();
        final int viewFlags = mViewFlags;
        final int action = event.getAction();

        final boolean clickable = ((viewFlags & CLICKABLE) == CLICKABLE
                || (viewFlags & LONG_CLICKABLE) == LONG_CLICKABLE)
                || (viewFlags & CONTEXT_CLICKABLE) == CONTEXT_CLICKABLE;

        if ((viewFlags & ENABLED_MASK) == DISABLED) {
            if (action == MotionEvent.ACTION_UP && (mPrivateFlags & PFLAG_PRESSED) != 0) {
                setPressed(false);
            }
            mPrivateFlags3 &= ~PFLAG3_FINGER_DOWN;
            // A disabled view that is clickable still consumes the touch
            // events, it just doesn't respond to them.
            return clickable;
        }

        if (!clickable && (viewFlags & TOOLTIP) != TOOLTIP) {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_UP -> {
                mPrivateFlags3 &= ~PFLAG3_FINGER_DOWN;
                if (!clickable) {
                    removeTapCallback();
                    removeLongPressCallback();
                    mInContextButtonPress = false;
                    mHasPerformedLongPress = false;
                    mIgnoreNextUpEvent = false;
                    break;
                }
                boolean prepressed = (mPrivateFlags & PFLAG_PREPRESSED) != 0;
                if ((mPrivateFlags & PFLAG_PRESSED) != 0 || prepressed) {
                    // take focus if we don't have it already and we should in
                    // touch mode.
                    boolean focusTaken = false;
                    if (isFocusable() && isFocusableInTouchMode() && !isFocused()) {
                        focusTaken = requestFocus();
                    }

                    if (prepressed) {
                        // The button is being released before we actually
                        // showed it as pressed.  Make it show the pressed
                        // state now (before scheduling the click) to ensure
                        // the user sees it.
                        setPressed(x, y);
                    }

                    if (!mHasPerformedLongPress && !mIgnoreNextUpEvent) {
                        // This is a tap, so remove the longpress check
                        removeLongPressCallback();

                        // Only perform take click actions if we were in the pressed state
                        if (!focusTaken) {
                            // Use a Runnable and post this rather than calling
                            // performClick directly. This lets other visual state
                            // of the view update before click actions start.
                            if (mPerformClick == null) {
                                mPerformClick = new PerformClick();
                            }
                            if (!post(mPerformClick)) {
                                performClick();
                            }
                        }
                    }

                    if (mUnsetPressedState == null) {
                        mUnsetPressedState = new UnsetPressedState();
                    }

                    if (prepressed) {
                        postDelayed(mUnsetPressedState,
                                ViewConfiguration.getPressedStateDuration());
                    } else if (!post(mUnsetPressedState)) {
                        // If the post failed, unpress right now
                        mUnsetPressedState.run();
                    }

                    removeTapCallback();
                }
                mIgnoreNextUpEvent = false;
            }
            case MotionEvent.ACTION_DOWN -> {
                mHasPerformedLongPress = false;

                if (!clickable) {
                    checkForLongClick(
                            ViewConfiguration.getLongPressTimeout(),
                            x,
                            y);
                    break;
                }

                if (performButtonActionOnTouchDown(event)) {
                    break;
                }

                // Walk up the hierarchy to determine if we're inside a scrolling container.
                boolean isInScrollingContainer = isInScrollingContainer();

                // For views inside a scrolling container, delay the pressed feedback for
                // a short period in case this is a scroll.
                if (isInScrollingContainer) {
                    mPrivateFlags |= PFLAG_PREPRESSED;
                    if (mPendingCheckForTap == null) {
                        mPendingCheckForTap = new CheckForTap();
                    }
                    mPendingCheckForTap.x = event.getX();
                    mPendingCheckForTap.y = event.getY();
                    postDelayed(mPendingCheckForTap, ViewConfiguration.getTapTimeout());
                } else {
                    // Not inside a scrolling container, so show the feedback right away
                    setPressed(x, y);
                    checkForLongClick(
                            ViewConfiguration.getLongPressTimeout(),
                            x,
                            y);
                }
            }
            case MotionEvent.ACTION_CANCEL -> {
                if (clickable) {
                    setPressed(false);
                }
                removeTapCallback();
                removeLongPressCallback();
                mInContextButtonPress = false;
                mHasPerformedLongPress = false;
                mIgnoreNextUpEvent = false;
                mPrivateFlags3 &= ~PFLAG3_FINGER_DOWN;
            }
            case MotionEvent.ACTION_MOVE -> {
                if (clickable) {
                    drawableHotspotChanged(x, y);
                }

                // Be lenient about moving outside of buttons
                if (!pointInView(x, y, ViewConfiguration.get().getScaledTouchSlop())) {
                    // Outside button
                    // Remove any future long press/tap checks
                    removeTapCallback();
                    removeLongPressCallback();
                    if ((mPrivateFlags & PFLAG_PRESSED) != 0) {
                        setPressed(false);
                    }
                    mPrivateFlags3 &= ~PFLAG3_FINGER_DOWN;
                }
            }
        }

        return true;
    }

    /**
     * Handles scroll bar dragging by mouse input.
     *
     * @param event The motion event.
     * @return true if the event was handled as a scroll bar dragging, false otherwise.
     * @hide
     */
    protected boolean handleScrollBarDragging(MotionEvent event) {
        if (mScrollCache == null) {
            return false;
        }
        final float x = event.getX();
        final float y = event.getY();
        final int action = event.getAction();
        if ((mScrollCache.mScrollBarDraggingState == ScrollCache.NOT_DRAGGING
                && action != MotionEvent.ACTION_DOWN)
                || !event.isButtonPressed(MotionEvent.BUTTON_PRIMARY)) {
            mScrollCache.mScrollBarDraggingState = ScrollCache.NOT_DRAGGING;
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                if (mScrollCache.mScrollBarDraggingState
                        == ScrollCache.DRAGGING_VERTICAL_SCROLL_BAR) {
                    final Rect bounds = mScrollCache.mScrollBarBounds;
                    getVerticalScrollBarBounds(bounds, null);
                    final int range = computeVerticalScrollRange();
                    final int offset = computeVerticalScrollOffset();
                    final int extent = computeVerticalScrollExtent();

                    final int thumbLength = ScrollCache.getThumbLength(
                            bounds.height(), bounds.width(), extent, range);
                    final int thumbOffset = ScrollCache.getThumbOffset(
                            bounds.height(), thumbLength, extent, range, offset);

                    final float diff = y - mScrollCache.mScrollBarDraggingPos;
                    final float maxThumbOffset = bounds.height() - thumbLength;
                    final float newThumbOffset =
                            Math.min(Math.max(thumbOffset + diff, 0.0f), maxThumbOffset);
                    final int height = getHeight();
                    if (Math.round(newThumbOffset) != thumbOffset && maxThumbOffset > 0
                            && height > 0 && extent > 0) {
                        final int newY = Math.round((range - extent)
                                / ((float) extent / height) * (newThumbOffset / maxThumbOffset));
                        if (newY != getScrollY()) {
                            mScrollCache.mScrollBarDraggingPos = y;
                            setScrollY(newY);
                        }
                    }
                    return true;
                }
                if (mScrollCache.mScrollBarDraggingState
                        == ScrollCache.DRAGGING_HORIZONTAL_SCROLL_BAR) {
                    final Rect bounds = mScrollCache.mScrollBarBounds;
                    getHorizontalScrollBarBounds(bounds, null);
                    final int range = computeHorizontalScrollRange();
                    final int offset = computeHorizontalScrollOffset();
                    final int extent = computeHorizontalScrollExtent();

                    final int thumbLength = ScrollCache.getThumbLength(
                            bounds.width(), bounds.height(), extent, range);
                    final int thumbOffset = ScrollCache.getThumbOffset(
                            bounds.width(), thumbLength, extent, range, offset);

                    final float diff = x - mScrollCache.mScrollBarDraggingPos;
                    final float maxThumbOffset = bounds.width() - thumbLength;
                    final float newThumbOffset =
                            Math.min(Math.max(thumbOffset + diff, 0.0f), maxThumbOffset);
                    final int width = getWidth();
                    if (Math.round(newThumbOffset) != thumbOffset && maxThumbOffset > 0
                            && width > 0 && extent > 0) {
                        final int newX = Math.round((range - extent)
                                / ((float) extent / width) * (newThumbOffset / maxThumbOffset));
                        if (newX != getScrollX()) {
                            mScrollCache.mScrollBarDraggingPos = x;
                            setScrollX(newX);
                        }
                    }
                    return true;
                }
            case MotionEvent.ACTION_DOWN:
                if (mScrollCache.mState == ScrollCache.OFF) {
                    return false;
                }
                if (isOnVerticalScrollbarThumb(x, y)) {
                    mScrollCache.mScrollBarDraggingState =
                            ScrollCache.DRAGGING_VERTICAL_SCROLL_BAR;
                    mScrollCache.mScrollBarDraggingPos = y;
                    return true;
                }
                if (isOnHorizontalScrollbarThumb(x, y)) {
                    mScrollCache.mScrollBarDraggingState =
                            ScrollCache.DRAGGING_HORIZONTAL_SCROLL_BAR;
                    mScrollCache.mScrollBarDraggingPos = x;
                    return true;
                }
        }
        mScrollCache.mScrollBarDraggingState = ScrollCache.NOT_DRAGGING;
        return false;
    }

    /**
     * @hide
     */
    public boolean isInScrollingContainer() {
        ViewParent p = getParent();
        while (p instanceof ViewGroup) {
            if (((ViewGroup) p).shouldDelayChildPressedState()) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }

    /**
     * Remove the longpress detection timer.
     */
    private void removeLongPressCallback() {
        if (mPendingCheckForLongPress != null) {
            removeCallbacks(mPendingCheckForLongPress);
        }
    }

    /**
     * Return true if the long press callback is scheduled to run sometime in the future.
     * Return false if there is no scheduled long press callback at the moment.
     */
    private boolean hasPendingLongPressCallback() {
        if (mPendingCheckForLongPress == null) {
            return false;
        }
        final AttachInfo attachInfo = mAttachInfo;
        if (attachInfo == null) {
            return false;
        }
        return attachInfo.mHandler.hasCallbacks(mPendingCheckForLongPress);
    }

    /**
     * Remove the pending click action
     */
    private void removePerformClickCallback() {
        if (mPerformClick != null) {
            removeCallbacks(mPerformClick);
        }
    }

    /**
     * Remove the prepress detection timer.
     */
    private void removeUnsetPressCallback() {
        if ((mPrivateFlags & PFLAG_PRESSED) != 0 && mUnsetPressedState != null) {
            setPressed(false);
            removeCallbacks(mUnsetPressedState);
        }
    }

    /**
     * Remove the tap detection timer.
     */
    private void removeTapCallback() {
        if (mPendingCheckForTap != null) {
            mPrivateFlags &= ~PFLAG_PREPRESSED;
            removeCallbacks(mPendingCheckForTap);
        }
    }

    /**
     * Cancels a pending long press.  Your subclass can use this if you
     * want the context menu to come up if the user presses and holds
     * at the same place, but you don't want it to come up if they press
     * and then move around enough to cause scrolling.
     */
    public void cancelLongPress() {
        removeLongPressCallback();

        /*
         * The prepressed state handled by the tap callback is a display
         * construct, but the tap callback will post a long press callback
         * less its own timeout. Remove it here.
         */
        removeTapCallback();
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
        final boolean result;
        final ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnClickListener != null) {
            playSoundEffect(SoundEffectConstants.CLICK);
            li.mOnClickListener.onClick(this);
            result = true;
        } else {
            result = false;
        }
        return result;
    }

    /**
     * Directly call any attached OnClickListener.  Unlike {@link #performClick()},
     * this only calls the listener, and does not do any associated clicking
     * actions like reporting an accessibility event.
     *
     * @return True there was an assigned OnClickListener that was called, false
     * otherwise is returned.
     */
    public boolean callOnClick() {
        ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnClickListener != null) {
            li.mOnClickListener.onClick(this);
            return true;
        }
        return false;
    }

    private void checkForLongClick(long delay, float x, float y) {
        if ((mViewFlags & LONG_CLICKABLE) == LONG_CLICKABLE || (mViewFlags & TOOLTIP) == TOOLTIP) {
            mHasPerformedLongPress = false;

            if (mPendingCheckForLongPress == null) {
                mPendingCheckForLongPress = new CheckForLongPress();
            }
            mPendingCheckForLongPress.setAnchor(x, y);
            mPendingCheckForLongPress.rememberWindowAttachCount();
            mPendingCheckForLongPress.rememberPressedState();
            postDelayed(mPendingCheckForLongPress, delay);
        }
    }

    /**
     * Calls this view's OnLongClickListener, if it is defined. Invokes the
     * context menu if the OnLongClickListener did not consume the event.
     *
     * @return {@code true} if one of the above receivers consumed the event,
     * {@code false} otherwise
     */
    public boolean performLongClick() {
        return performLongClickInternal(mLongClickX, mLongClickY);
    }

    /**
     * Calls this view's OnLongClickListener, if it is defined. Invokes the
     * context menu if the OnLongClickListener did not consume the event,
     * anchoring it to an (x,y) coordinate.
     *
     * @param x x coordinate of the anchoring touch event, or {@link Float#NaN}
     *          to disable anchoring
     * @param y y coordinate of the anchoring touch event, or {@link Float#NaN}
     *          to disable anchoring
     * @return {@code true} if one of the above receivers consumed the event,
     * {@code false} otherwise
     */
    public boolean performLongClick(float x, float y) {
        mLongClickX = x;
        mLongClickY = y;
        final boolean handled = performLongClick();
        mLongClickX = Float.NaN;
        mLongClickY = Float.NaN;
        return handled;
    }

    /**
     * Calls this view's OnLongClickListener, if it is defined. Invokes the
     * context menu if the OnLongClickListener did not consume the event,
     * optionally anchoring it to an (x,y) coordinate.
     *
     * @param x x coordinate of the anchoring touch event, or {@link Float#NaN}
     *          to disable anchoring
     * @param y y coordinate of the anchoring touch event, or {@link Float#NaN}
     *          to disable anchoring
     * @return {@code true} if one of the above receivers consumed the event,
     * {@code false} otherwise
     */
    private boolean performLongClickInternal(float x, float y) {
        boolean handled = false;
        final ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnLongClickListener != null) {
            handled = li.mOnLongClickListener.onLongClick(View.this);
        }
        if (!handled) {
            final boolean isAnchored = !Float.isNaN(x) && !Float.isNaN(y);
            handled = isAnchored ? showContextMenu(x, y) : showContextMenu();
        }
        /*if ((mViewFlags & TOOLTIP) == TOOLTIP) {
            if (!handled) {
                handled = showLongClickTooltip((int) x, (int) y);
            }
        }*/
        if (handled) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
        return handled;
    }

    /**
     * Call this view's OnContextClickListener, if it is defined.
     *
     * @param x the x coordinate of the context click
     * @param y the y coordinate of the context click
     * @return True if there was an assigned OnContextClickListener that consumed the event, false
     * otherwise.
     */
    public boolean performContextClick(float x, float y) {
        return performContextClick();
    }

    /**
     * Call this view's OnContextClickListener, if it is defined.
     *
     * @return True if there was an assigned OnContextClickListener that consumed the event, false
     * otherwise.
     */
    public boolean performContextClick() {
        boolean handled = false;
        ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnContextClickListener != null) {
            handled = li.mOnContextClickListener.onContextClick(this);
        }
        if (handled) {
            performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
        }
        return handled;
    }

    /**
     * Performs button-related actions during a touch down event.
     *
     * @param event The event.
     * @return True if the down was consumed.
     * @hide
     */
    protected boolean performButtonActionOnTouchDown(@Nonnull MotionEvent event) {
        // Added: check if the view is long clickable
        if (isLongClickable() && (event.getButtonState() & MotionEvent.BUTTON_SECONDARY) != 0) {
            showContextMenu(event.getX(), event.getY());
            mPrivateFlags |= PFLAG_CANCEL_NEXT_UP_EVENT;
            return true;
        }
        return false;
    }

    /**
     * Shows the context menu for this view.
     *
     * @return {@code true} if the context menu was shown, {@code false}
     * otherwise
     * @see #showContextMenu(float, float)
     */
    public final boolean showContextMenu() {
        return showContextMenu(Float.NaN, Float.NaN);
    }

    /**
     * Shows the context menu for this view anchored to the specified
     * view-relative coordinate.
     *
     * @param x the X coordinate in pixels relative to the view to which the
     *          menu should be anchored, or {@link Float#NaN} to disable anchoring
     * @param y the Y coordinate in pixels relative to the view to which the
     *          menu should be anchored, or {@link Float#NaN} to disable anchoring
     * @return {@code true} if the context menu was shown, {@code false}
     * otherwise
     */
    public boolean showContextMenu(float x, float y) {
        ViewParent parent = getParent();
        if (parent == null) return false;
        return parent.showContextMenuForChild(this, x, y);
    }

    /**
     * Start an action mode with the default type {@link ActionMode#TYPE_PRIMARY}.
     *
     * @param callback Callback that will control the lifecycle of the action mode
     * @return The new action mode if it is started, null otherwise
     * @see ActionMode
     * @see #startActionMode(ActionMode.Callback, int)
     */
    public final ActionMode startActionMode(ActionMode.Callback callback) {
        return startActionMode(callback, ActionMode.TYPE_PRIMARY);
    }

    /**
     * Start an action mode with the given type.
     *
     * @param callback Callback that will control the lifecycle of the action mode
     * @param type     One of {@link ActionMode#TYPE_PRIMARY} or {@link ActionMode#TYPE_FLOATING}.
     * @return The new action mode if it is started, null otherwise
     * @see ActionMode
     */
    public ActionMode startActionMode(ActionMode.Callback callback, int type) {
        ViewParent parent = getParent();
        if (parent == null) return null;
        return parent.startActionModeForChild(this, callback, type);
    }

    /**
     * Show the context menu for this view. It is not safe to hold on to the
     * menu after returning from this method.
     * <p>
     * You should normally not overload this method. Overload
     * {@link #onCreateContextMenu(ContextMenu)} or define an
     * {@link OnCreateContextMenuListener} to add items to the context menu.
     *
     * @param menu The context menu to populate
     */
    public final void createContextMenu(@Nonnull ContextMenu menu) {
        Object menuInfo = getContextMenuInfo();

        // Sets the current menu info so all items added to menu will have
        // my extra info set.
        ((MenuBuilder) menu).setCurrentMenuInfo(menuInfo);

        onCreateContextMenu(menu);
        ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnCreateContextMenuListener != null) {
            li.mOnCreateContextMenuListener.onCreateContextMenu(menu, this, menuInfo);
        }

        // Clear the extra information so subsequent items that aren't mine don't
        // have my extra info.
        ((MenuBuilder) menu).setCurrentMenuInfo(null);

        if (mParent != null) {
            mParent.createContextMenu(menu);
        }
    }

    /**
     * Views should implement this if they have extra information to associate
     * with the context menu. The return result is supplied as a parameter to
     * the {@link OnCreateContextMenuListener#onCreateContextMenu(ContextMenu, View, Object)}
     * callback.
     *
     * @return Extra information about the item for which the context menu
     * should be shown. This information will vary across different
     * subclasses of View.
     */
    protected Object getContextMenuInfo() {
        return null;
    }

    /**
     * Views should implement this if the view itself is going to add items to
     * the context menu.
     *
     * @param menu the context menu to populate
     */
    protected void onCreateContextMenu(@Nonnull ContextMenu menu) {
    }

    /**
     * Tells whether the {@link View} is in the state between {@link #onStartTemporaryDetach()}
     * and {@link #onFinishTemporaryDetach()}.
     *
     * <p>This method always returns {@code true} when called directly or indirectly from
     * {@link #onStartTemporaryDetach()}. The return value when called directly or indirectly from
     * {@link #onFinishTemporaryDetach()}.
     *
     * @return {@code true} when the View is in the state between {@link #onStartTemporaryDetach()}
     * and {@link #onFinishTemporaryDetach()}.
     */
    public final boolean isTemporarilyDetached() {
        return (mPrivateFlags3 & PFLAG3_TEMPORARY_DETACH) != 0;
    }

    /**
     * Dispatch {@link #onStartTemporaryDetach()} to this View and its direct children if this is
     * a container View.
     */
    @CallSuper
    public void dispatchStartTemporaryDetach() {
        mPrivateFlags3 |= PFLAG3_TEMPORARY_DETACH;
        onStartTemporaryDetach();
    }

    /**
     * This is called when a container is going to temporarily detach a child, with
     * {@link ViewGroup#detachViewFromParent(View)}.
     * It will either be followed by {@link #onFinishTemporaryDetach()} or
     * {@link #onDetachedFromWindow()} when the container is done.
     */
    public void onStartTemporaryDetach() {
        removeUnsetPressCallback();
        mPrivateFlags |= PFLAG_CANCEL_NEXT_UP_EVENT;
    }

    /**
     * Dispatch {@link #onFinishTemporaryDetach()} to this View and its direct children if this is
     * a container View.
     */
    @CallSuper
    public void dispatchFinishTemporaryDetach() {
        mPrivateFlags3 &= ~PFLAG3_TEMPORARY_DETACH;
        onFinishTemporaryDetach();
    }

    /**
     * Called after {@link #onStartTemporaryDetach} when the container is done
     * changing the view.
     */
    public void onFinishTemporaryDetach() {
    }

    /**
     * Return the global {@link KeyEvent.DispatcherState KeyEvent.DispatcherState}
     * for this view's window.  Returns null if the view is not currently attached
     * to the window.  Normally you will not need to use this directly, but
     * just use the standard high-level event callbacks like
     * {@link #onKeyDown(int, KeyEvent)}.
     */
    @Nullable
    public final KeyEvent.DispatcherState getKeyDispatcherState() {
        return mAttachInfo != null ? mAttachInfo.mKeyDispatchState : null;
    }

    /**
     * Dispatch a key event to the next view on the focus path. This path runs
     * from the top of the view tree down to the currently focused view. If this
     * view has focus, it will dispatch to itself. Otherwise it will dispatch
     * the next node down the focus path. This method also fires any key
     * listeners.
     *
     * @param event The key event to be dispatched.
     * @return True if the event was handled, false otherwise.
     */
    public boolean dispatchKeyEvent(@Nonnull KeyEvent event) {
        // Give any attached key listener a first crack at the event.
        ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnKeyListener != null && (mViewFlags & ENABLED_MASK) == ENABLED
                && li.mOnKeyListener.onKey(this, event.getKeyCode(), event)) {
            return true;
        }

        return switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN -> onKeyDown(event.getKeyCode(), event);
            case KeyEvent.ACTION_UP -> onKeyUp(event.getKeyCode(), event);
            default -> false;
        };
    }

    /**
     * Dispatches a key shortcut event.
     *
     * @param event The key event to be dispatched.
     * @return True if the event was handled by the view, false otherwise.
     */
    public boolean dispatchKeyShortcutEvent(@Nonnull KeyEvent event) {
        return onKeyShortcut(event.getKeyCode(), event);
    }

    /**
     * Default implementation: perform press of the view
     * when {@link KeyEvent#KEY_ENTER}, {@link KeyEvent#KEY_KP_ENTER}
     * or {@link KeyEvent#KEY_SPACE} is released, if the view is enabled and clickable.
     *
     * @param keyCode a key code that represents the button pressed, from
     *                {@link KeyEvent}
     * @param event   the KeyEvent object that defines the button action
     */
    public boolean onKeyDown(int keyCode, @Nonnull KeyEvent event) {
        if (keyCode == KeyEvent.KEY_ENTER || keyCode == KeyEvent.KEY_KP_ENTER) {
            if ((mViewFlags & ENABLED_MASK) == DISABLED) {
                return true;
            }
            if (event.getRepeatCount() == 0) {
                if ((mViewFlags & CLICKABLE) == CLICKABLE) {
                    setPressed(true);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Default implementation: perform clicking of the view
     * when {@link KeyEvent#KEY_ENTER}, {@link KeyEvent#KEY_KP_ENTER}
     * or {@link KeyEvent#KEY_SPACE} is released.
     *
     * @param keyCode A key code that represents the button pressed, from
     *                {@link KeyEvent}.
     * @param event   The KeyEvent object that defines the button action.
     */
    public boolean onKeyUp(int keyCode, @Nonnull KeyEvent event) {
        if (keyCode == KeyEvent.KEY_ENTER || keyCode == KeyEvent.KEY_KP_ENTER) {
            if ((mViewFlags & ENABLED_MASK) == DISABLED) {
                return true;
            }
            if ((mViewFlags & CLICKABLE) == CLICKABLE && isPressed()) {
                setPressed(false);

                if (!mHasPerformedLongPress) {
                    // This is a tap, so remove the longpress check
                    removeLongPressCallback();
                    return performClick();
                }
            }
        }
        return false;
    }

    /**
     * Called on the focused view when a key shortcut event is not handled.
     * Override this method to implement local key shortcuts for the View.
     * Key shortcuts can also be implemented by setting the
     * {@link MenuItem#setShortcut(char, char) shortcut} property of menu items.
     *
     * @param keyCode The value in event.getKeyCode().
     * @param event   Description of the key event.
     * @return If you handled the event, return true. If you want to allow the
     * event to be handled by the next receiver, return false.
     */
    public boolean onKeyShortcut(int keyCode, @Nonnull KeyEvent event) {
        return false;
    }

    /**
     * Hit rectangle in parent's coordinates
     *
     * @param outRect The hit rectangle of the view.
     */
    public void getHitRect(Rect outRect) {
        if (hasIdentityMatrix() || mAttachInfo == null) {
            outRect.set(mLeft, mTop, mRight, mBottom);
        } else {
            final RectF tmpRect = mAttachInfo.mTmpTransformRect;
            tmpRect.set(0, 0, getWidth(), getHeight());
            getMatrix().transform(tmpRect);
            outRect.set((int) tmpRect.left + mLeft, (int) tmpRect.top + mTop,
                    (int) tmpRect.right + mLeft, (int) tmpRect.bottom + mTop);
        }
    }

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
     *
     * @hide
     */
    public boolean pointInView(float localX, float localY, float slop) {
        return localX >= -slop && localY >= -slop && localX < ((mRight - mLeft) + slop) &&
                localY < ((mBottom - mTop) + slop);
    }

    /**
     * When a view has focus and the user navigates away from it, the next view is searched for
     * starting from the rectangle filled in by this method.
     * <p>
     * By default, the rectangle is the {@link #getDrawingRect(Rect)})
     * of the view.  However, if your view maintains some idea of internal selection,
     * such as a cursor, or a selected row or column, you should override this method and
     * fill in a more specific rectangle.
     *
     * @param r The rectangle to fill in, in this view's coordinates.
     */
    public void getFocusedRect(@Nonnull Rect r) {
        getDrawingRect(r);
    }

    /**
     * If some part of this view is not clipped by any of its parents, then
     * return that area in r in global (root) coordinates. To convert r to local
     * coordinates (without taking possible View rotations into account), offset
     * it by -globalOffset (e.g. r.offset(-globalOffset.x, -globalOffset.y)).
     * If the view is completely clipped or translated out, return false.
     *
     * @param r            If true is returned, r holds the global coordinates of the
     *                     visible portion of this view.
     * @param globalOffset If true is returned, globalOffset holds the dx,dy
     *                     between this view and its root. globalOffset may be null.
     * @return true if r is non-empty (i.e. part of the view is visible at the
     * root level.
     */
    public boolean getGlobalVisibleRect(@Nonnull Rect r, @Nullable Point globalOffset) {
        int width = mRight - mLeft;
        int height = mBottom - mTop;
        if (width > 0 && height > 0) {
            r.set(0, 0, width, height);
            if (globalOffset != null) {
                globalOffset.set(-mScrollX, -mScrollY);
            }
            return mParent == null || mParent.getChildVisibleRect(this, r, globalOffset);
        }
        return false;
    }

    public final boolean getGlobalVisibleRect(@Nonnull Rect r) {
        return getGlobalVisibleRect(r, null);
    }

    public final boolean getLocalVisibleRect(@Nonnull Rect r) {
        final Point offset = mAttachInfo != null ? mAttachInfo.mPoint : new Point();
        if (getGlobalVisibleRect(r, offset)) {
            r.offset(-offset.x, -offset.y); // make r local
            return true;
        }
        return false;
    }

    /**
     * Offset this view's vertical location by the specified number of pixels.
     *
     * @param offset the number of pixels to offset the view by
     */
    public void offsetTopAndBottom(int offset) {
        if (offset != 0) {
            mTop += offset;
            mBottom += offset;
            mRenderNode.offsetTopAndBottom(offset);
            invalidate();
        }
    }

    /**
     * Offset this view's horizontal location by the specified amount of pixels.
     *
     * @param offset the number of pixels to offset the view by
     */
    public void offsetLeftAndRight(int offset) {
        if (offset != 0) {
            mLeft += offset;
            mRight += offset;
            mRenderNode.offsetLeftAndRight(offset);
            invalidate();
        }
    }

    /**
     * Returns whether the device is currently in touch mode.  Touch mode is entered
     * once the user begins interacting with the device by touch, and affects various
     * things like whether focus is always visible to the user.
     *
     * @return Whether the device is in touch mode.
     */
    public boolean isInTouchMode() {
        if (mAttachInfo != null) {
            return mAttachInfo.mInTouchMode;
        }
        return false;
    }

    /**
     * Set the pointer icon for the current view.
     * Passing {@code null} will restore the pointer icon to its default value.
     *
     * @param pointerIcon A PointerIcon instance which will be shown when the mouse hovers.
     */
    public void setPointerIcon(@Nullable PointerIcon pointerIcon) {
        mAttachInfo.mViewRoot.updatePointerIcon(pointerIcon);
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

    /*
     * Called when mouse hovered on this view and a mouse button pressed.
     *
     * @param mouseX      relative mouse x pos
     * @param mouseY      relative mouse y pos
     * @param mouseButton mouse button, for example {@link GLFW#GLFW_MOUSE_BUTTON_LEFT}
     * @return {@code true} if action performed
     * @see GLFW
     */
    /*protected boolean onMousePressed(double mouseX, double mouseY, int mouseButton) {
        return false;
    }*/

    /*
     * Called when mouse hovered on this view and a mouse button released.
     *
     * @param mouseX      relative mouse x pos
     * @param mouseY      relative mouse y pos
     * @param mouseButton mouse button, for example {@link GLFW#GLFW_MOUSE_BUTTON_LEFT}
     * @return {@code true} if action performed
     * @see GLFW
     */
    /*protected boolean onMouseReleased(double mouseX, double mouseY, int mouseButton) {
        return false;
    }*/

    /*
     * Called when mouse pressed and released on this view with the mouse button.
     * Called after onMouseReleased no matter whether it's consumed or not.
     *
     * @param mouseX      relative mouse x pos
     * @param mouseY      relative mouse y pos
     * @param mouseButton mouse button, for example {@link GLFW#GLFW_MOUSE_BUTTON_LEFT}
     * @return {@code true} if action performed
     * @see GLFW
     */
    /*protected boolean onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        return false;
    }*/

    /*
     * Called when mouse hovered on this view and left button double clicked.
     * If no action performed in this method, onMousePressed will be called.
     *
     * @param mouseX relative mouse x pos
     * @param mouseY relative mouse y pos
     * @return {@code true} if action performed
     */
    /*protected boolean onMouseDoubleClicked(double mouseX, double mouseY) {
        return false;
    }*/

    /*
     * Called when mouse hovered on this view and mouse scrolled.
     *
     * @param mouseX relative mouse x pos
     * @param mouseY relative mouse y pos
     * @param amount scroll amount
     * @return return {@code true} if action performed
     */
    /*protected boolean onMouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }*/

    /*
     * Call when this view start to be listened as a draggable.
     */
    /*protected void onStartDragging() {

    }*/

    /*
     * Call when this view is no longer listened as a draggable.
     */
    /*protected void onStopDragging() {

    }*/

    /*
     * Called when mouse moved and dragged.
     *
     * @param mouseX relative mouse x pos
     * @param mouseY relative mouse y pos
     * @param deltaX mouse x pos change
     * @param deltaY mouse y pos change
     * @return {@code true} if action performed
     */
    /*protected boolean onMouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        return false;
    }*/

    /*
     * Call when this view start to be listened as a keyboard listener.
     */
    /*protected void onStartKeyboard() {

    }*/

    /*
     * Call when this view is no longer listened as a keyboard listener.
     */
    /*protected void onStopKeyboard() {

    }*/

    /*
     * Called when a key pressed.
     *
     * @param keyCode   see {@link GLFW}, for example {@link GLFW#GLFW_KEY_W}
     * @param scanCode  keyboard scan code, seldom used
     * @param modifiers modifier key
     * @return return {@code true} if action performed
     */
    /*protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }*/

    /*
     * Called when a key released.
     *
     * @param keyCode   see {@link GLFW}, for example {@link GLFW#GLFW_KEY_W}
     * @param scanCode  keyboard scan code, seldom used
     * @param modifiers modifier key
     * @return return {@code true} if action performed
     */
    /*protected boolean onKeyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }*/

    /*
     * Called when a unicode character typed.
     *
     * @param codePoint char code
     * @param modifiers modifier key
     * @return return {@code true} if action performed
     */
    /*protected boolean onCharTyped(char codePoint, int modifiers) {
        return false;
    }*/

    /*
     * Called when click on scrollbar track and not on the thumb
     * and there was an scroll amount change
     *
     * @param vertical    {@code true} if scrollbar is vertical, horizontal otherwise
     * @param scrollDelta scroll amount change calculated by scrollbar
     */
    /*protected void onScrollBarClicked(boolean vertical, float scrollDelta) {

    }*/

    /*
     * Called when drag the scroll thumb and there was an scroll
     * amount change
     *
     * @param vertical    {@code true} if scrollbar is vertical, horizontal otherwise
     * @param scrollDelta scroll amount change calculated by scrollbar
     */
    /*protected void onScrollBarDragged(boolean vertical, float scrollDelta) {

    }*/

    /**
     * Changes the visibility of this View without triggering any other changes. This should only
     * be used by animation frameworks, such as {@link Transition}, where
     * visibility changes should not adjust focus or trigger a new layout. Application developers
     * should use {@link #setVisibility} instead to ensure that the hierarchy is correctly updated.
     *
     * <p>Only call this method when a temporary visibility must be applied during an
     * animation and the original visibility value is guaranteed to be reset after the
     * animation completes. Use {@link #setVisibility} in all other cases.</p>
     *
     * @param visibility One of {@link #VISIBLE}, {@link #INVISIBLE}, or {@link #GONE}.
     * @see #setVisibility(int)
     */
    public final void setTransitionVisibility(int visibility) {
        mViewFlags = (mViewFlags & ~View.VISIBILITY_MASK) | visibility;
    }

    /**
     * Sets the name of the View to be used to identify Views in Transitions.
     * Names should be unique in the View hierarchy.
     *
     * @param transitionName The name of the View to uniquely identify it for Transitions.
     */
    public final void setTransitionName(String transitionName) {
        mTransitionName = transitionName;
    }

    /**
     * Returns the name of the View to be used to identify Views in Transitions.
     * Names should be unique in the View hierarchy.
     *
     * <p>This returns null if the View has not been given a name.</p>
     *
     * @return The name used of the View to be used to identify Views in Transitions or null
     * if no name has been given.
     */
    public String getTransitionName() {
        return mTransitionName;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder(128);
        out.append(getClass().getName());
        out.append('{');
        out.append(Integer.toHexString(System.identityHashCode(this)));
        out.append(' ');
        switch (mViewFlags & VISIBILITY_MASK) {
            case VISIBLE -> out.append('V');
            case INVISIBLE -> out.append('I');
            case GONE -> out.append('G');
            default -> out.append('.');
        }
        out.append((mViewFlags & FOCUSABLE) == FOCUSABLE ? 'F' : '.');
        out.append((mViewFlags & ENABLED_MASK) == ENABLED ? 'E' : '.');
        out.append((mViewFlags & DRAW_MASK) == WILL_NOT_DRAW ? '.' : 'D');
        out.append((mViewFlags & SCROLLBARS_HORIZONTAL) != 0 ? 'H' : '.');
        out.append((mViewFlags & SCROLLBARS_VERTICAL) != 0 ? 'V' : '.');
        out.append((mViewFlags & CLICKABLE) != 0 ? 'C' : '.');
        out.append((mViewFlags & LONG_CLICKABLE) != 0 ? 'L' : '.');
        out.append((mViewFlags & CONTEXT_CLICKABLE) != 0 ? 'X' : '.');
        out.append(' ');
        out.append((mPrivateFlags & PFLAG_IS_ROOT_NAMESPACE) != 0 ? 'R' : '.');
        out.append((mPrivateFlags & PFLAG_FOCUSED) != 0 ? 'F' : '.');
        out.append((mPrivateFlags & PFLAG_SELECTED) != 0 ? 'S' : '.');
        if ((mPrivateFlags & PFLAG_PREPRESSED) != 0) {
            out.append('p');
        } else {
            out.append((mPrivateFlags & PFLAG_PRESSED) != 0 ? 'P' : '.');
        }
        out.append((mPrivateFlags & PFLAG_HOVERED) != 0 ? 'H' : '.');
        out.append((mPrivateFlags & PFLAG_ACTIVATED) != 0 ? 'A' : '.');
        out.append((mPrivateFlags & PFLAG_INVALIDATED) != 0 ? 'I' : '.');
        out.append((mPrivateFlags & PFLAG_DIRTY_MASK) != 0 ? 'D' : '.');
        out.append(' ');
        out.append(mLeft);
        out.append(',');
        out.append(mTop);
        out.append('-');
        out.append(mRight);
        out.append(',');
        out.append(mBottom);
        final int id = getId();
        if (id != NO_ID) {
            out.append(" #");
            out.append(Integer.toHexString(id));
        }
        out.append("}");
        return out.toString();
    }

    // Properties
    //
    /**
     * A Property wrapper around the <code>alpha</code> functionality handled by the
     * {@link View#setAlpha(float)} and {@link View#getAlpha()} methods.
     */
    public static final FloatProperty<View> ALPHA = new FloatProperty<>() {
        @Override
        public void setValue(@Nonnull View object, float value) {
            object.setAlpha(value);
        }

        @Nonnull
        @Override
        public Float get(@Nonnull View object) {
            return object.getAlpha();
        }
    };

    /**
     * A Property wrapper around the <code>translationX</code> functionality handled by the
     * {@link View#setTranslationX(float)} and {@link View#getTranslationX()} methods.
     */
    public static final FloatProperty<View> TRANSLATION_X = new FloatProperty<>() {
        @Override
        public void setValue(@Nonnull View object, float value) {
            object.setTranslationX(value);
        }

        @Nonnull
        @Override
        public Float get(@Nonnull View object) {
            return object.getTranslationX();
        }
    };

    /**
     * A Property wrapper around the <code>translationY</code> functionality handled by the
     * {@link View#setTranslationY(float)} and {@link View#getTranslationY()} methods.
     */
    public static final FloatProperty<View> TRANSLATION_Y = new FloatProperty<>() {
        @Override
        public void setValue(@Nonnull View object, float value) {
            object.setTranslationY(value);
        }

        @Nonnull
        @Override
        public Float get(@Nonnull View object) {
            return object.getTranslationY();
        }
    };

    /**
     * A Property wrapper around the <code>translationZ</code> functionality handled by the
     * {@link View#setTranslationZ(float)} and {@link View#getTranslationZ()} methods.
     */
    public static final FloatProperty<View> TRANSLATION_Z = new FloatProperty<>() {
        @Override
        public void setValue(@Nonnull View object, float value) {
            object.setTranslationZ(value);
        }

        @Nonnull
        @Override
        public Float get(@Nonnull View object) {
            return object.getTranslationZ();
        }
    };

    /**
     * A Property wrapper around the <code>x</code> functionality handled by the
     * {@link View#setX(float)} and {@link View#getX()} methods.
     */
    public static final FloatProperty<View> X = new FloatProperty<>() {
        @Override
        public void setValue(@Nonnull View object, float value) {
            object.setX(value);
        }

        @Nonnull
        @Override
        public Float get(@Nonnull View object) {
            return object.getX();
        }
    };

    /**
     * A Property wrapper around the <code>y</code> functionality handled by the
     * {@link View#setY(float)} and {@link View#getY()} methods.
     */
    public static final FloatProperty<View> Y = new FloatProperty<>() {
        @Override
        public void setValue(@Nonnull View object, float value) {
            object.setY(value);
        }

        @Nonnull
        @Override
        public Float get(@Nonnull View object) {
            return object.getY();
        }
    };

    /**
     * A Property wrapper around the <code>z</code> functionality handled by the
     * {@link View#setZ(float)} and {@link View#getZ()} methods.
     */
    public static final FloatProperty<View> Z = new FloatProperty<>() {
        @Override
        public void setValue(@Nonnull View object, float value) {
            object.setZ(value);
        }

        @Nonnull
        @Override
        public Float get(@Nonnull View object) {
            return object.getZ();
        }
    };

    /**
     * A Property wrapper around the <code>rotation</code> functionality handled by the
     * {@link View#setRotation(float)} and {@link View#getRotation()} methods.
     */
    public static final FloatProperty<View> ROTATION = new FloatProperty<>() {
        @Override
        public void setValue(@Nonnull View object, float value) {
            object.setRotation(value);
        }

        @Nonnull
        @Override
        public Float get(@Nonnull View object) {
            return object.getRotation();
        }
    };

    /**
     * A Property wrapper around the <code>rotationX</code> functionality handled by the
     * {@link View#setRotationX(float)} and {@link View#getRotationX()} methods.
     */
    public static final FloatProperty<View> ROTATION_X = new FloatProperty<>() {
        @Override
        public void setValue(@Nonnull View object, float value) {
            object.setRotationX(value);
        }

        @Nonnull
        @Override
        public Float get(@Nonnull View object) {
            return object.getRotationX();
        }
    };

    /**
     * A Property wrapper around the <code>rotationY</code> functionality handled by the
     * {@link View#setRotationY(float)} and {@link View#getRotationY()} methods.
     */
    public static final FloatProperty<View> ROTATION_Y = new FloatProperty<>() {
        @Override
        public void setValue(@Nonnull View object, float value) {
            object.setRotationY(value);
        }

        @Nonnull
        @Override
        public Float get(@Nonnull View object) {
            return object.getRotationY();
        }
    };

    /**
     * A Property wrapper around the <code>scaleX</code> functionality handled by the
     * {@link View#setScaleX(float)} and {@link View#getScaleX()} methods.
     */
    public static final FloatProperty<View> SCALE_X = new FloatProperty<>() {
        @Override
        public void setValue(@Nonnull View object, float value) {
            object.setScaleX(value);
        }

        @Nonnull
        @Override
        public Float get(@Nonnull View object) {
            return object.getScaleX();
        }
    };

    /**
     * A Property wrapper around the <code>scaleY</code> functionality handled by the
     * {@link View#setScaleY(float)} and {@link View#getScaleY()} methods.
     */
    public static final FloatProperty<View> SCALE_Y = new FloatProperty<>() {
        @Override
        public void setValue(@Nonnull View object, float value) {
            object.setScaleY(value);
        }

        @Nonnull
        @Override
        public Float get(@Nonnull View object) {
            return object.getScaleY();
        }
    };

    /**
     * A Property wrapper around the <code>left</code> functionality handled by the
     * {@link View#setLeft(int)} and {@link View#getLeft()} methods.
     * <p>
     * This is not encouraged and should be regarded as internal use only.
     */
    public static final IntProperty<View> LEFT = new IntProperty<>() {
        @Override
        public void setValue(@Nonnull View object, int value) {
            object.setLeft(value);
        }

        @Nonnull
        @Override
        public Integer get(@Nonnull View object) {
            return object.getLeft();
        }
    };

    /**
     * A Property wrapper around the <code>top</code> functionality handled by the
     * {@link View#setTop(int)} and {@link View#getTop()} methods.
     * <p>
     * This is not encouraged and should be regarded as internal use only.
     */
    public static final IntProperty<View> TOP = new IntProperty<>() {
        @Override
        public void setValue(@Nonnull View object, int value) {
            object.setTop(value);
        }

        @Nonnull
        @Override
        public Integer get(@Nonnull View object) {
            return object.getTop();
        }
    };

    /**
     * A Property wrapper around the <code>right</code> functionality handled by the
     * {@link View#setRight(int)} and {@link View#getRight()} methods.
     * <p>
     * This is not encouraged and should be regarded as internal use only.
     */
    public static final IntProperty<View> RIGHT = new IntProperty<>() {
        @Override
        public void setValue(@Nonnull View object, int value) {
            object.setRight(value);
        }

        @Nonnull
        @Override
        public Integer get(@Nonnull View object) {
            return object.getRight();
        }
    };

    /**
     * A Property wrapper around the <code>bottom</code> functionality handled by the
     * {@link View#setBottom(int)} and {@link View#getBottom()} methods.
     * <p>
     * This is not encouraged and should be regarded as internal use only.
     */
    public static final IntProperty<View> BOTTOM = new IntProperty<>() {
        @Override
        public void setValue(@Nonnull View object, int value) {
            object.setBottom(value);
        }

        @Nonnull
        @Override
        public Integer get(@Nonnull View object) {
            return object.getBottom();
        }
    };

    /**
     * A Property wrapper around the <code>scrollX</code> functionality handled by the
     * {@link View#setScrollX(int)} and {@link View#getScrollX()} methods.
     * <p>
     * This is not encouraged and should be regarded as internal use only.
     */
    public static final IntProperty<View> SCROLL_X = new IntProperty<>() {
        @Override
        public void setValue(@Nonnull View object, int value) {
            object.setScrollX(value);
        }

        @Nonnull
        @Override
        public Integer get(@Nonnull View object) {
            return object.getScrollX();
        }
    };

    /**
     * A Property wrapper around the <code>scrollY</code> functionality handled by the
     * {@link View#setScrollY(int)} and {@link View#getScrollY()} methods.
     * <p>
     * This is not encouraged and should be regarded as internal use only.
     */
    public static final IntProperty<View> SCROLL_Y = new IntProperty<>() {
        @Override
        public void setValue(@Nonnull View object, int value) {
            object.setScrollY(value);
        }

        @Nonnull
        @Override
        public Integer get(@Nonnull View object) {
            return object.getScrollY();
        }
    };

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
                LOGGER.error(VIEW_MARKER, "No view found on draw shadow");
            }
        }
    }

    private final class CheckForLongPress implements Runnable {

        private int mOriginalWindowAttachCount;
        private float mX;
        private float mY;
        private boolean mOriginalPressedState;

        private CheckForLongPress() {
        }

        @Override
        public void run() {
            if ((mOriginalPressedState == isPressed()) && (mParent != null)
                    && mOriginalWindowAttachCount == mWindowAttachCount) {
                if (performLongClick(mX, mY)) {
                    mHasPerformedLongPress = true;
                }
            }
        }

        public void setAnchor(float x, float y) {
            mX = x;
            mY = y;
        }

        public void rememberWindowAttachCount() {
            mOriginalWindowAttachCount = mWindowAttachCount;
        }

        public void rememberPressedState() {
            mOriginalPressedState = isPressed();
        }
    }

    private final class CheckForTap implements Runnable {

        public float x;
        public float y;

        @Override
        public void run() {
            mPrivateFlags &= ~PFLAG_PREPRESSED;
            setPressed(x, y);
            final long delay = ViewConfiguration.getLongPressTimeout() - ViewConfiguration.getTapTimeout();
            checkForLongClick(delay, x, y);
        }
    }

    private final class PerformClick implements Runnable {

        @Override
        public void run() {
            performClick();
        }
    }

    private final class UnsetPressedState implements Runnable {

        @Override
        public void run() {
            setPressed(false);
        }
    }

    private static class ForegroundInfo {

        private Drawable mDrawable;
        private int mGravity = Gravity.FILL;
        private boolean mInsidePadding = true;
        private boolean mBoundsChanged = true;
        private final Rect mSelfBounds = new Rect();
        private final Rect mOverlayBounds = new Rect();
    }

    static class ListenerInfo {

        ListenerInfo() {
        }

        private OnTouchListener mOnTouchListener;

        private OnHoverListener mOnHoverListener;

        private OnGenericMotionListener mOnGenericMotionListener;

        private OnKeyListener mOnKeyListener;

        /**
         * Listener used to dispatch click events.
         */
        private OnClickListener mOnClickListener;

        /**
         * Listener used to dispatch long click events.
         */
        private OnLongClickListener mOnLongClickListener;

        private OnDragListener mOnDragListener;

        /**
         * Listener used to dispatch context click events.
         */
        private OnContextClickListener mOnContextClickListener;

        /**
         * Listener used to build the context menu.
         */
        private OnCreateContextMenuListener mOnCreateContextMenuListener;

        protected OnScrollChangeListener mOnScrollChangeListener;

        /**
         * Listener used to dispatch focus change events.
         */
        private OnFocusChangeListener mOnFocusChangeListener;

        /**
         * Listeners for layout change events.
         */
        private ArrayList<OnLayoutChangeListener> mOnLayoutChangeListeners;

        /**
         * Listeners for attach events.
         */
        private CopyOnWriteArrayList<OnAttachStateChangeListener> mOnAttachStateChangeListeners;
    }

    /**
     * Interface definition for a callback to be invoked when a touch event is
     * dispatched to this view. The callback will be invoked before the touch
     * event is given to the view.
     */
    @FunctionalInterface
    public interface OnTouchListener {

        /**
         * Called when a touch event is dispatched to a view. This allows listeners to
         * get a chance to respond before the target view.
         *
         * @param v     The view the touch event has been dispatched to.
         * @param event The MotionEvent object containing full information about
         *              the event.
         * @return True if the listener has consumed the event, false otherwise.
         */
        boolean onTouch(View v, MotionEvent event);
    }

    /**
     * Interface definition for a callback to be invoked when a hover event is
     * dispatched to this view. The callback will be invoked before the hover
     * event is given to the view.
     */
    @FunctionalInterface
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

    /**
     * Interface definition for a callback to be invoked when a generic motion event is
     * dispatched to this view. The callback will be invoked before the generic motion
     * event is given to the view.
     */
    @FunctionalInterface
    public interface OnGenericMotionListener {

        /**
         * Called when a generic motion event is dispatched to a view. This allows listeners to
         * get a chance to respond before the target view.
         *
         * @param v     The view the generic motion event has been dispatched to.
         * @param event The MotionEvent object containing full information about
         *              the event.
         * @return True if the listener has consumed the event, false otherwise.
         */
        boolean onGenericMotion(View v, MotionEvent event);
    }

    /**
     * Interface definition for a callback to be invoked when a hardware key event is
     * dispatched to this view. The callback will be invoked before the key event is
     * given to the view. This is only useful for hardware keyboards; a software input
     * method has no obligation to trigger this listener.
     */
    @FunctionalInterface
    public interface OnKeyListener {

        /**
         * Called when a hardware key is dispatched to a view. This allows listeners to
         * get a chance to respond before the target view.
         * <p>Key presses in software keyboards will generally NOT trigger this method,
         * although some may elect to do so in some situations. Do not assume a
         * software input method has to be key-based; even if it is, it may use key presses
         * in a different way than you expect, so there is no way to reliably catch soft
         * input key presses.
         *
         * @param v       The view the key has been dispatched to.
         * @param keyCode The code for the physical key that was pressed
         * @param event   The KeyEvent object containing full information about
         *                the event.
         * @return True if the listener has consumed the event, false otherwise.
         */
        boolean onKey(View v, int keyCode, KeyEvent event);
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
     * Interface definition for a callback to be invoked when a view has been clicked and held.
     */
    @FunctionalInterface
    public interface OnLongClickListener {

        /**
         * Called when a view has been clicked and held.
         *
         * @param v The view that was clicked and held.
         * @return true if the callback consumed the long click, false otherwise.
         */
        boolean onLongClick(View v);
    }

    /**
     * Interface definition for a callback to be invoked when a drag is being dispatched
     * to this view.  The callback will be invoked before the hosting view's own
     * onDrag(event) method.  If the listener wants to fall back to the hosting view's
     * onDrag(event) behavior, it should return 'false' from this callback.
     *
     * <div class="special reference">
     * <h3>Developer Guides</h3>
     * <p>For a guide to implementing drag and drop features, read the
     * <a href="https://developer.android.com/guide/topics/ui/drag-drop.html">Drag and Drop</a> developer guide.</p>
     * </div>
     */
    @FunctionalInterface
    public interface OnDragListener {

        /**
         * Called when a drag event is dispatched to a view. This allows listeners
         * to get a chance to override base View behavior.
         *
         * @param v     The View that received the drag event.
         * @param event The {@link DragEvent} object for the drag event.
         * @return {@code true} if the drag event was handled successfully, or {@code false}
         * if the drag event was not handled. Note that {@code false} will trigger the View
         * to call its {@link #onDragEvent(DragEvent) onDragEvent()} handler.
         */
        boolean onDrag(View v, DragEvent event);
    }

    /**
     * Interface definition for a callback to be invoked when a view is context clicked.
     */
    @FunctionalInterface
    public interface OnContextClickListener {

        /**
         * Called when a view is context clicked.
         *
         * @param v The view that has been context clicked.
         * @return true if the callback consumed the context click, false otherwise.
         */
        boolean onContextClick(View v);
    }

    /**
     * Interface definition for a callback to be invoked when the context menu
     * for this view is being built.
     */
    @FunctionalInterface
    public interface OnCreateContextMenuListener {

        /**
         * Called when the context menu for this view is being built. It is not
         * safe to hold onto the menu after this method returns.
         *
         * @param menu     The context menu that is being built
         * @param v        The view for which the context menu is being built
         * @param menuInfo Extra information about the item for which the
         *                 context menu should be shown. This information will vary
         *                 depending on the class of v.
         */
        void onCreateContextMenu(ContextMenu menu, View v, Object menuInfo);
    }

    /**
     * Interface definition for a callback to be invoked when the scroll
     * X or Y positions of a view change.
     * <p>
     * <b>Note:</b> Some views handle scrolling independently of View and may
     * have their own separate listeners for scroll-type events.
     *
     * @see #setOnScrollChangeListener(OnScrollChangeListener)
     */
    @FunctionalInterface
    public interface OnScrollChangeListener {

        /**
         * Called when the scroll position of a view changes.
         *
         * @param v          The view whose scroll position has changed.
         * @param scrollX    Current horizontal scroll origin.
         * @param scrollY    Current vertical scroll origin.
         * @param oldScrollX Previous horizontal scroll origin.
         * @param oldScrollY Previous vertical scroll origin.
         */
        void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY);
    }

    /**
     * Interface definition for a callback to be invoked when the focus state of
     * a view changed.
     */
    @FunctionalInterface
    public interface OnFocusChangeListener {

        /**
         * Called when the focus state of a view has changed.
         *
         * @param v        The view whose state has changed.
         * @param hasFocus The new focus state of v.
         */
        void onFocusChange(View v, boolean hasFocus);
    }

    /**
     * Interface definition for a callback to be invoked when the layout bounds of a view
     * changes due to layout processing.
     */
    @FunctionalInterface
    public interface OnLayoutChangeListener {

        /**
         * Called when the layout bounds of a view changes due to layout processing.
         *
         * @param v         The view whose bounds have changed.
         * @param left      The new value of the view's left property.
         * @param top       The new value of the view's top property.
         * @param right     The new value of the view's right property.
         * @param bottom    The new value of the view's bottom property.
         * @param oldLeft   The previous value of the view's left property.
         * @param oldTop    The previous value of the view's top property.
         * @param oldRight  The previous value of the view's right property.
         * @param oldBottom The previous value of the view's bottom property.
         */
        void onLayoutChange(View v, int left, int top, int right, int bottom,
                            int oldLeft, int oldTop, int oldRight, int oldBottom);
    }

    /**
     * Interface definition for a callback to be invoked when this view is attached
     * or detached from its window.
     */
    public interface OnAttachStateChangeListener {

        /**
         * Called when the view is attached to a window.
         *
         * @param v The view that was attached
         */
        void onViewAttachedToWindow(View v);

        /**
         * Called when the view is detached from a window.
         *
         * @param v The view that was detached
         */
        void onViewDetachedFromWindow(View v);
    }
}
