/*
 * Modern UI.
 * Copyright (C) 2021-2025 BloCamLimb. All rights reserved.
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
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *   Copyright (C) 2006 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package icyllis.modernui.view;

import icyllis.modernui.animation.LayoutTransition;
import icyllis.modernui.annotation.*;
import icyllis.modernui.core.Context;
import icyllis.modernui.core.Core;
import icyllis.modernui.graphics.*;
import icyllis.modernui.graphics.pipeline.DrawShadowUtils;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.util.AttributeSet;
import icyllis.modernui.util.Pools;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.ApiStatus;

import java.nio.FloatBuffer;
import java.util.*;
import java.util.function.Predicate;

/**
 * <p>
 * A <code>ViewGroup</code> is a special view that can contain other views
 * (called children.) The view group is the base class for layouts and views
 * containers. This class also defines the
 * {@link LayoutParams} class which serves as the base
 * class for layouts parameters.
 * </p>
 *
 * <p>
 * Also see {@link LayoutParams} for layout attributes.
 * </p>
 */
@UiThread
@SuppressWarnings("unused")
public abstract class ViewGroup extends View implements ViewParent, ViewManager {

    private static final int ARRAY_CAPACITY_INCREMENT = 12;

    /**
     * When set, ViewGroup invalidates only the child's rectangle
     * Set by default
     */
    static final int FLAG_CLIP_CHILDREN = 0x1;

    /**
     * When set, ViewGroup excludes the padding area from the invalidate rectangle
     * Set by default
     */
    private static final int FLAG_CLIP_TO_PADDING = 0x2;

    /**
     * If set, this ViewGroup has padding; if unset there is no padding and we don't need
     * to clip it, even if FLAG_CLIP_TO_PADDING is set
     */
    private static final int FLAG_PADDING_NOT_NULL = 0x20;

    /**
     * When set, the drawing method will call {@link #getChildDrawingOrder(int, int)}
     * to get the index of the child to draw for that iteration.
     */
    static final int FLAG_USE_CHILD_DRAWING_ORDER = 0x400;

    /**
     * When set, this group will go through its list of children to notify them of
     * any drawable state change.
     */
    private static final int FLAG_NOTIFY_CHILDREN_ON_DRAWABLE_STATE_CHANGE = 0x10000;

    /**
     * When set, this ViewGroup's drawable states also include those
     * of its children.
     */
    private static final int FLAG_ADD_STATES_FROM_CHILDREN = 0x2000;

    /**
     * This view will get focus before any of its descendants.
     */
    public static final int FOCUS_BEFORE_DESCENDANTS = 0x20000;

    /**
     * This view will get focus only if none of its descendants want it.
     */
    public static final int FOCUS_AFTER_DESCENDANTS = 0x40000;

    /**
     * This view will block any of its descendants from getting focus, even
     * if they are focusable.
     */
    public static final int FOCUS_BLOCK_DESCENDANTS = 0x60000;

    private static final int FLAG_MASK_FOCUSABILITY = 0x60000;

    /**
     * When set, this ViewGroup should not intercept touch events.
     */
    @ApiStatus.Internal
    protected static final int FLAG_DISALLOW_INTERCEPT = 0x80000;

    /**
     * When set, this ViewGroup will not dispatch onAttachedToWindow calls
     * to children when adding new views. This is used to prevent multiple
     * onAttached calls when a ViewGroup adds children in its own onAttached method.
     */
    private static final int FLAG_PREVENT_DISPATCH_ATTACHED_TO_WINDOW = 0x400000;

    static final int FLAG_IS_TRANSITION_GROUP = 0x1000000;

    static final int FLAG_IS_TRANSITION_GROUP_SET = 0x2000000;

    /**
     * When set, focus will not be permitted to enter this group if a touchscreen is present.
     */
    static final int FLAG_TOUCHSCREEN_BLOCKS_FOCUS = 0x4000000;

    /**
     * Used to map between enum in attributes and flag values.
     */
    private static final int[] DESCENDANT_FOCUSABILITY_FLAGS =
            {FOCUS_BEFORE_DESCENDANTS, FOCUS_AFTER_DESCENDANTS,
                    FOCUS_BLOCK_DESCENDANTS};

    /**
     * We clip to padding when FLAG_CLIP_TO_PADDING and FLAG_PADDING_NOT_NULL
     * are set at the same time.
     */
    protected static final int CLIP_TO_PADDING_MASK = FLAG_CLIP_TO_PADDING | FLAG_PADDING_NOT_NULL;

    private int mGroupFlags;

    // child views
    private View[] mChildren;

    // number of valid children in the children array, the rest
    // should be null or not considered as children
    private int mChildrenCount;

    // The view contained within this ViewGroup that has or contains focus.
    View mFocused;
    // The view contained within this ViewGroup (excluding nested keyboard navigation clusters)
    // that is or contains a default-focus view.
    private View mDefaultFocus;
    // The last child of this ViewGroup which held focus within the current cluster
    View mFocusedInCluster;

    // Whether layout calls are currently being suppressed, controlled by calls to
    // suppressLayout()
    boolean mSuppressLayout = false;

    // Whether any layout calls have actually been suppressed while mSuppressLayout
    // has been true. This tracks whether we need to issue a requestLayout() when
    // layout is later re-enabled.
    private boolean mLayoutCalledWhileSuppressed = false;

    // Lazily-created holder for point computations.
    private float[] mTempPosition;

    // First touch target in the linked list of touch targets.
    private TouchTarget mTouchTarget;

    // First hover target in the linked list of hover targets.
    // The hover targets are children which have received ACTION_HOVER_ENTER.
    // They might not have actually handled the hover event, but we will
    // continue sending hover events to them as long as the pointer remains over
    // their bounds and the view group does not intercept hover.
    private HoverTarget mFirstHoverTarget;

    // True if the view group itself received a hover event.
    // It might not have actually handled the hover event.
    private boolean mHoveredSelf;

    // The child capable of showing a tooltip and currently under the pointer.
    private View mTooltipHoverTarget;

    // True if the view group is capable of showing a tooltip and the pointer is directly
    // over the view group but not one of its child views.
    private boolean mTooltipHoveredSelf;

    private static final ThreadLocal<FloatBuffer> sDebugDrawBuffer =
            ThreadLocal.withInitial(() -> FloatBuffer.allocate(3 * 2 * 2 * 8));

    // Used to animate add/remove changes in layout
    private LayoutTransition mTransition;

    // Views which have been hidden or removed which need to be animated on
    // their way out.
    private ArrayList<View> mDisappearingChildren;

    // The set of views that are currently being transitioned. This list is used to track views
    // being removed that should not actually be removed from the parent yet because they are
    // being animated.
    private ArrayList<View> mTransitioningViews;

    // List of children changing visibility. This is used to potentially keep rendering
    // views during a transition when they otherwise would have become gone/invisible
    private ArrayList<View> mVisibilityChangingChildren;

    // Temporary holder of presorted children, only used for
    // input/software draw dispatch for correctly Z ordering.
    private ArrayList<View> mPreSortedChildren;

    private int mChildCountWithTransientState = 0;

    private int mNestedScrollAxesTouch;
    private int mNestedScrollAxesNonTouch;

    // Used to manage the list of transient views, added by addTransientView()
    private IntArrayList mTransientIndices = null;
    private List<View> mTransientViews = null;

    public ViewGroup(Context context) {
        this(context, null);
    }

    public ViewGroup(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, null);
    }

    public ViewGroup(Context context, @Nullable AttributeSet attrs,
                     @Nullable @AttrRes ResourceId defStyleAttr) {
        this(context, attrs, defStyleAttr, null);
    }

    public ViewGroup(Context context, @Nullable AttributeSet attrs,
                     @Nullable @AttrRes ResourceId defStyleAttr,
                     @Nullable @StyleRes ResourceId defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        // ViewGroup doesn't draw by default
        setWillNotDraw(true);
        mGroupFlags |= FLAG_CLIP_CHILDREN | FLAG_CLIP_TO_PADDING;
        setDescendantFocusability(FOCUS_BEFORE_DESCENDANTS);
        mChildren = new View[ARRAY_CAPACITY_INCREMENT];
        mChildrenCount = 0;

        //TODO read attributes
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    protected void onDebugDraw(@NonNull Canvas canvas) {
        Paint paint = Paint.obtain();

        final int childrenCount = mChildrenCount;
        final View[] children = mChildren;
        FloatBuffer positions = null;

        // Draw optical bounds
        paint.setRGBA(1F, 0F, 0F, 1F);
        for (int i = 0; i < childrenCount; i++) {
            View child = children[i];
            if (child.getVisibility() != View.GONE) {
                if (positions == null)
                    positions = sDebugDrawBuffer.get();
                positions.clear();
                // draw GPU line strip primitive at pixel center
                float x1 = child.getLeft() + 0.5f;
                float y1 = child.getTop() + 0.5f;
                float x2 = child.getRight() - 0.5f;
                float y2 = child.getBottom() - 0.5f;
                positions.put(x1).put(y1)
                        .put(x2).put(y1)
                        .put(x2).put(y2)
                        .put(x1).put(y2)
                        .put(x1).put(y1);
                canvas.drawMesh(Canvas.VertexMode.LINE_STRIP,
                        positions.flip(), null, null, null, null,
                        paint);
            }
        }

        // Draw margins
        paint.setRGBA(1F, 0F, 1F, 0.25F);
        onDebugDrawMargins(canvas, paint);

        // Draw clip bounds
        paint.setRGBA(0.25F, 0.5F, 1F, 1F);
        int lineLength = dp(4F);
        int lineWidth = dp(0.5F);
        for (int i = 0; i < childrenCount; i++) {
            View child = children[i];
            if (child.getVisibility() != View.GONE) {
                if (positions == null)
                    positions = sDebugDrawBuffer.get();
                positions.clear();
                int x1 = child.getLeft();
                int y1 = child.getTop();
                int x2 = child.getRight();
                int y2 = child.getBottom();
                fillCorner(x1, y1, lineLength, lineLength, lineWidth, positions);
                fillCorner(x1, y2, lineLength, -lineLength, lineWidth, positions);
                fillCorner(x2, y1, -lineLength, lineLength, lineWidth, positions);
                fillCorner(x2, y2, -lineLength, -lineLength, lineWidth, positions);
                canvas.drawMesh(Canvas.VertexMode.TRIANGLES,
                        positions.flip(), null, null, null, null,
                        paint);
            }
        }

        paint.recycle();
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    protected void onDebugDrawMargins(@NonNull Canvas canvas, Paint paint) {
        final int childrenCount = mChildrenCount;
        final View[] children = mChildren;
        for (int i = 0; i < childrenCount; i++) {
            View child = children[i];
            child.getLayoutParams().onDebugDraw(child, canvas, paint);
        }
    }

    private static void fillRect(int x1, int y1, int x2, int y2,
                                 FloatBuffer positions) {
        if (x1 != x2 && y1 != y2) {
            if (x1 > x2) {
                int tmp = x1; x1 = x2; x2 = tmp;
            }
            if (y1 > y2) {
                int tmp = y1; y1 = y2; y2 = tmp;
            }
            positions.put(x1).put(y2)
                    .put(x2).put(y2)
                    .put(x1).put(y1)
                    .put(x1).put(y1)
                    .put(x2).put(y2)
                    .put(x2).put(y1);
        }
    }

    private static void fillCorner(int x1, int y1, int dx, int dy, int lw,
                                   FloatBuffer positions) {
        fillRect(x1, y1, x1 + dx, y1 + lw * (dy >= 0 ? 1 : -1), positions);
        fillRect(x1, y1, x1 + lw * (dx >= 0 ? 1 : -1), y1 + dy, positions);
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        int clipSaveCount = 0;
        final boolean clipToPadding = (mGroupFlags & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK;
        if (clipToPadding) {
            clipSaveCount = canvas.save();
            canvas.clipRect(mScrollX + mPaddingLeft, mScrollY + mPaddingTop,
                    mScrollX + mRight - mLeft - mPaddingRight,
                    mScrollY + mBottom - mTop - mPaddingBottom);
        }

        final int childrenCount = mChildrenCount;
        final View[] children = mChildren;

        final int transientCount = mTransientIndices == null ? 0 : mTransientIndices.size();
        int transientIndex = transientCount != 0 ? 0 : -1;
        final ArrayList<View> preorderedList = buildOrderedChildList();
        final boolean customOrder = preorderedList == null
                && isChildrenDrawingOrderEnabled();
        int shadowIndex = -1;
        float lastCasterZ = 0.0f;
        for (int i = 0; i < childrenCount; ) {
            final int childIndex = getAndVerifyPreorderedIndex(childrenCount, i, customOrder);
            final View child = getAndVerifyPreorderedView(preorderedList, children, childIndex);
            if (shadowIndex < 0 && child.getZ() > 0.001f) {
                shadowIndex = i;
            }

            if (shadowIndex >= 0 && shadowIndex < childrenCount) {
                final int casterIndex = getAndVerifyPreorderedIndex(childrenCount, shadowIndex, customOrder);
                final View caster = getAndVerifyPreorderedView(preorderedList, children, casterIndex);
                final float casterZ = caster.getZ();
                if (shadowIndex == i || casterZ - lastCasterZ < 0.1f) {
                    drawShadow(canvas, caster);
                    lastCasterZ = casterZ;
                    shadowIndex++;
                    continue;
                }
            }

            while (transientIndex >= 0 && mTransientIndices.getInt(transientIndex) == i) {
                final View transientChild = mTransientViews.get(transientIndex);
                if ((transientChild.mViewFlags & VISIBILITY_MASK) == VISIBLE) {
                    drawChild(canvas, transientChild, 0);
                }
                transientIndex++;
                if (transientIndex >= transientCount) {
                    transientIndex = -1;
                }
            }

            if ((child.mViewFlags & VISIBILITY_MASK) == VISIBLE) {
                drawChild(canvas, child, 0);
            }
            i++;
        }
        while (transientIndex >= 0) {
            // there may be additional transient views after the normal views
            final View transientChild = mTransientViews.get(transientIndex);
            if ((transientChild.mViewFlags & VISIBILITY_MASK) == VISIBLE) {
                drawChild(canvas, transientChild, 0);
            }
            transientIndex++;
            if (transientIndex >= transientCount) {
                break;
            }
        }
        if (preorderedList != null) {
            preorderedList.clear();
        }

        // Draw any disappearing views that have animations
        if (mDisappearingChildren != null) {
            final ArrayList<View> disappearingChildren = mDisappearingChildren;
            // Go backwards -- we may delete as animations finish
            for (int i = disappearingChildren.size() - 1; i >= 0; i--) {
                final View child = disappearingChildren.get(i);
                drawChild(canvas, child, 0);
            }
        }

        if (isShowingLayoutBounds()) {
            onDebugDraw(canvas);
        }

        if (clipToPadding) {
            canvas.restoreToCount(clipSaveCount);
        }
    }

    /**
     * Draw one child of this View Group. This method is responsible for getting
     * the canvas in the right state. This includes clipping, translating so
     * that the child's scrolled origin is at 0, 0, and applying any animation
     * transformations.
     *
     * @param canvas      The canvas on which to draw the child
     * @param child       Who to draw
     * @param drawingTime The time at which draw is occurring
     */
    protected void drawChild(@NonNull Canvas canvas, @NonNull View child, long drawingTime) {
        child.draw(canvas, this, (mGroupFlags & FLAG_CLIP_CHILDREN) != 0);
    }

    private void drawShadow(@NonNull Canvas canvas, @NonNull View child) {
        var casterProperties = child.mRenderNode;
        if (casterProperties.getScaleX() == 0 || casterProperties.getScaleY() == 0) {
            return;
        }
        child.setBackgroundBounds();
        var outline = casterProperties.getOutline();
        if (outline.getType() != Outline.TYPE_ROUND_RECT) {
            return;
        }
        float casterAlpha = casterProperties.getAlpha() * outline.getAlpha();
        if (casterAlpha <= 0.001f) {
            return;
        }

        if ((casterProperties.getClippingFlags() & RenderProperties.CLIP_TO_CLIP_BOUNDS) != 0) {
            // we don't know the shape of the outline after it is clipped by the clip bounds
            return;
        }

        int ambientColor = (int) (LightingInfo.getAmbientShadowAlpha() * casterAlpha) << 24;
        int spotColor = (int) (LightingInfo.getSpotShadowAlpha() * casterAlpha) << 24;

        canvas.save();
        canvas.translate(child.mLeft, child.mTop);

        var shadowMatrix = new icyllis.arc3d.core.Matrix4();

        if (casterProperties.getAnimationMatrix() != null) {
            casterProperties.getAnimationMatrix().toMatrix4(shadowMatrix);
        }
        var transform = casterProperties.getTransform();
        if (transform != null) {
            shadowMatrix.preConcat(transform);
        }
        canvas.concat(shadowMatrix);

        canvas.translate(-child.mScrollX, -child.mScrollY);

        float zPlane0, zPlane1, zPlane2;
        if (shadowMatrix.hasPerspective()) {
            zPlane0 = shadowMatrix.m13;
            zPlane1 = shadowMatrix.m23;
            zPlane2 = shadowMatrix.m43;
        } else {
            zPlane0 = 0;
            zPlane1 = 0;
            zPlane2 = casterProperties.getZ();
        }
        DrawShadowUtils.drawShadow(
                ((ArcCanvas) canvas).getCanvas(),
                outline.getBounds(), outline.getRadius(),
                zPlane0, zPlane1, zPlane2,
                LightingInfo.getLightX(), LightingInfo.getLightY(), LightingInfo.getLightZ(),
                LightingInfo.getLightRadius(), ambientColor, spotColor
        );

        canvas.restore();
    }

    @Override
    public final void layout(int l, int t, int r, int b) {
        if (!mSuppressLayout && (mTransition == null || !mTransition.isChangingLayout())) {
            if (mTransition != null) {
                mTransition.layoutChange(this);
            }
            super.layout(l, t, r, b);
        } else {
            // record the fact that we noop'd it; request layout when transition finishes
            mLayoutCalledWhileSuppressed = true;
        }
    }

    @Override
    protected abstract void onLayout(boolean changed, int left, int top, int right, int bottom);

    private int getAndVerifyPreorderedIndex(int childrenCount, int i, boolean customOrder) {
        final int childIndex;
        if (customOrder) {
            final int childIndex1 = getChildDrawingOrder(childrenCount, i);
            if (childIndex1 >= childrenCount) {
                throw new IndexOutOfBoundsException("getChildDrawingOrder() "
                        + "returned invalid index " + childIndex1
                        + " (child count is " + childrenCount + ")");
            }
            childIndex = childIndex1;
        } else {
            childIndex = i;
        }
        return childIndex;
    }

    @Override
    protected boolean dispatchHoverEvent(@NonNull MotionEvent event) {
        final int action = event.getAction();

        final boolean intercepted = onInterceptHoverEvent(event);
        event.setAction(action); // restore action in case it was changed

        boolean handled = false;

        // Send events to the hovered children and build a new list of hover targets until
        // one is found that handles the event.
        HoverTarget firstOldHoverTarget = mFirstHoverTarget;
        mFirstHoverTarget = null;
        if (!intercepted && action != MotionEvent.ACTION_HOVER_EXIT
                && mChildrenCount != 0) {
            final float x = event.getX();
            final float y = event.getY();
            final View[] children = mChildren;
            final int childrenCount = mChildrenCount;
            final ArrayList<View> preorderedList = buildOrderedChildList();
            final boolean customOrder = preorderedList == null && isChildrenDrawingOrderEnabled();
            HoverTarget lastHoverTarget = null;
            for (int i = childrenCount - 1; i >= 0; i--) {
                final int childIndex = getAndVerifyPreorderedIndex(
                        childrenCount, i, customOrder);
                final View child = getAndVerifyPreorderedView(
                        preorderedList, children, childIndex);
                if (!child.canReceivePointerEvents()
                        || !isTransformedTouchPointInView(x, y, child, null)) {
                    continue;
                }

                // Obtain a hover target for this child.  Dequeue it from the
                // old hover target list if the child was previously hovered.
                HoverTarget hoverTarget = firstOldHoverTarget;
                final boolean wasHovered;
                for (HoverTarget predecessor = null; ; ) {
                    if (hoverTarget == null) {
                        hoverTarget = HoverTarget.obtain(child);
                        wasHovered = false;
                        break;
                    }

                    if (hoverTarget.child == child) {
                        if (predecessor != null) {
                            predecessor.next = hoverTarget.next;
                        } else {
                            firstOldHoverTarget = hoverTarget.next;
                        }
                        hoverTarget.next = null;
                        wasHovered = true;
                        break;
                    }

                    predecessor = hoverTarget;
                    hoverTarget = hoverTarget.next;
                }

                // Enqueue the hover target onto the new hover target list.
                if (lastHoverTarget != null) {
                    lastHoverTarget.next = hoverTarget;
                } else {
                    mFirstHoverTarget = hoverTarget;
                }
                lastHoverTarget = hoverTarget;

                // Dispatch the event to the child.
                if (action == MotionEvent.ACTION_HOVER_ENTER) {
                    if (!wasHovered) {
                        // Send the enter as is.
                        handled = dispatchTransformedGenericPointerEvent(
                                event, child); // enter
                    }
                } else if (action == MotionEvent.ACTION_HOVER_MOVE) {
                    if (!wasHovered) {
                        event.setAction(MotionEvent.ACTION_HOVER_ENTER);
                        handled = dispatchTransformedGenericPointerEvent(
                                event, child); // enter
                        event.setAction(action);
                    }
                    // Send the move as is.
                    handled |= dispatchTransformedGenericPointerEvent(
                            event, child); // move
                }
                if (handled) {
                    break;
                }
            }
            if (preorderedList != null) {
                preorderedList.clear();
            }
        }

        // Send exit events to all previously hovered children that are no longer hovered.
        while (firstOldHoverTarget != null) {
            final View child = firstOldHoverTarget.child;

            // Exit the old hovered child.
            if (action == MotionEvent.ACTION_HOVER_EXIT) {
                // Send the exit as is.
                handled |= dispatchTransformedGenericPointerEvent(
                        event, child); // exit
            } else {
                // Synthesize an exit from a move or enter.
                // Ignore the result because hover focus has moved to a different view.
                if (action == MotionEvent.ACTION_HOVER_MOVE) {
                    final boolean hoverExitPending = event.isHoverExitPending();
                    event.setHoverExitPending(true);
                    dispatchTransformedGenericPointerEvent(
                            event, child); // move
                    event.setHoverExitPending(hoverExitPending);
                }
                event.setAction(MotionEvent.ACTION_HOVER_EXIT);
                dispatchTransformedGenericPointerEvent(
                        event, child); // exit
                event.setAction(action);
            }

            final HoverTarget nextOldHoverTarget = firstOldHoverTarget.next;
            firstOldHoverTarget.recycle();
            firstOldHoverTarget = nextOldHoverTarget;
        }

        // Send events to the view group itself if no children have handled it and the view group
        // itself is not currently being hover-exited.
        boolean newHoveredSelf = !handled &&
                (action != MotionEvent.ACTION_HOVER_EXIT) && !event.isHoverExitPending();
        if (newHoveredSelf == mHoveredSelf) {
            if (newHoveredSelf) {
                // Send event to the view group as before.
                handled = super.dispatchHoverEvent(event);
            }
        } else {
            if (mHoveredSelf) {
                // Exit the view group.
                if (action == MotionEvent.ACTION_HOVER_EXIT) {
                    // Send the exit as is.
                    handled |= super.dispatchHoverEvent(event); // exit
                } else {
                    // Synthesize an exit from a move or enter.
                    // Ignore the result because hover focus is moving to a different view.
                    if (action == MotionEvent.ACTION_HOVER_MOVE) {
                        super.dispatchHoverEvent(event); // move
                    }
                    event.setAction(MotionEvent.ACTION_HOVER_EXIT);
                    super.dispatchHoverEvent(event); // exit
                    event.setAction(action);
                }
                mHoveredSelf = false;
            }

            if (newHoveredSelf) {
                // Enter the view group.
                if (action == MotionEvent.ACTION_HOVER_ENTER) {
                    // Send the enter as is.
                    handled = super.dispatchHoverEvent(event); // enter
                    mHoveredSelf = true;
                } else if (action == MotionEvent.ACTION_HOVER_MOVE) {
                    // Synthesize an enter from a move.
                    event.setAction(MotionEvent.ACTION_HOVER_ENTER);
                    handled = super.dispatchHoverEvent(event); // enter
                    event.setAction(action);

                    handled |= super.dispatchHoverEvent(event); // move
                    mHoveredSelf = true;
                }
            }
        }

        // Done.
        return handled;
    }

    private void exitHoverTargets() {
        if (mHoveredSelf || mFirstHoverTarget != null) {
            final long now = Core.timeNanos();
            MotionEvent event = MotionEvent.obtain(now,
                    MotionEvent.ACTION_HOVER_EXIT, 0.0f, 0.0f, 0);
            dispatchHoverEvent(event);
            event.recycle();
        }
    }

    private void cancelHoverTarget(View view) {
        HoverTarget predecessor = null;
        HoverTarget target = mFirstHoverTarget;
        while (target != null) {
            final HoverTarget next = target.next;
            if (target.child == view) {
                if (predecessor == null) {
                    mFirstHoverTarget = next;
                } else {
                    predecessor.next = next;
                }
                target.recycle();

                final long now = Core.timeNanos();
                MotionEvent event = MotionEvent.obtain(now,
                        MotionEvent.ACTION_HOVER_EXIT, 0.0f, 0.0f, 0);
                view.dispatchHoverEvent(event);
                event.recycle();
                return;
            }
            predecessor = target;
            target = next;
        }
    }

    @Override
    boolean dispatchTooltipHoverEvent(@NonNull MotionEvent event) {
        final int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_HOVER_ENTER:
                break;

            case MotionEvent.ACTION_HOVER_MOVE:
                View newTarget = null;

                // Check what the child under the pointer says about the tooltip.
                final int childrenCount = mChildrenCount;
                if (childrenCount != 0) {
                    final float x = event.getX();
                    final float y = event.getY();

                    final ArrayList<View> preorderedList = buildOrderedChildList();
                    final boolean customOrder = preorderedList == null
                            && isChildrenDrawingOrderEnabled();
                    final View[] children = mChildren;
                    for (int i = childrenCount - 1; i >= 0; i--) {
                        final int childIndex =
                                getAndVerifyPreorderedIndex(childrenCount, i, customOrder);
                        final View child =
                                getAndVerifyPreorderedView(preorderedList, children, childIndex);
                        if (!child.canReceivePointerEvents()
                                || !isTransformedTouchPointInView(x, y, child, null)) {
                            continue;
                        }
                        if (dispatchTransformedTooltipHoverEvent(event, child)) {
                            newTarget = child;
                            break;
                        }
                    }
                    if (preorderedList != null) preorderedList.clear();
                }

                if (mTooltipHoverTarget != newTarget) {
                    if (mTooltipHoverTarget != null) {
                        event.setAction(MotionEvent.ACTION_HOVER_EXIT);
                        mTooltipHoverTarget.dispatchTooltipHoverEvent(event);
                        event.setAction(action);
                    }
                    mTooltipHoverTarget = newTarget;
                }

                if (mTooltipHoverTarget != null) {
                    if (mTooltipHoveredSelf) {
                        mTooltipHoveredSelf = false;
                        event.setAction(MotionEvent.ACTION_HOVER_EXIT);
                        super.dispatchTooltipHoverEvent(event);
                        event.setAction(action);
                    }
                    return true;
                }

                mTooltipHoveredSelf = super.dispatchTooltipHoverEvent(event);
                return mTooltipHoveredSelf;

            case MotionEvent.ACTION_HOVER_EXIT:
                if (mTooltipHoverTarget != null) {
                    mTooltipHoverTarget.dispatchTooltipHoverEvent(event);
                    mTooltipHoverTarget = null;
                } else if (mTooltipHoveredSelf) {
                    super.dispatchTooltipHoverEvent(event);
                    mTooltipHoveredSelf = false;
                }
                break;
        }
        return false;
    }

    boolean dispatchTransformedTooltipHoverEvent(MotionEvent event, View child) {
        final boolean result;
        if (!child.hasIdentityMatrix()) {
            MotionEvent transformedEvent = getTransformedMotionEvent(event, child);
            result = child.dispatchTooltipHoverEvent(transformedEvent);
            transformedEvent.recycle();
        } else {
            final float offsetX = mScrollX - child.mLeft;
            final float offsetY = mScrollY - child.mTop;
            event.offsetLocation(offsetX, offsetY);
            result = child.dispatchTooltipHoverEvent(event);
            event.offsetLocation(-offsetX, -offsetY);
        }
        return result;
    }

    private void exitTooltipHoverTargets() {
        if (mTooltipHoveredSelf || mTooltipHoverTarget != null) {
            final long now = Core.timeNanos();
            MotionEvent event = MotionEvent.obtain(now,
                    MotionEvent.ACTION_HOVER_EXIT, 0.0f, 0.0f, 0);
            dispatchTooltipHoverEvent(event);
            event.recycle();
        }
    }

    /**
     * Dispatches a generic pointer event to a child, taking into account
     * transformations that apply to the child.
     *
     * @param event The event to send.
     * @param child The view to send the event to.
     * @return {@code true} if the child handled the event.
     */
    boolean dispatchTransformedGenericPointerEvent(@NonNull MotionEvent event, @NonNull View child) {
        boolean handled;
        if (!child.hasIdentityMatrix()) {
            MotionEvent transformedEvent = getTransformedMotionEvent(event, child);
            handled = child.dispatchGenericMotionEvent(transformedEvent);
            transformedEvent.recycle();
        } else {
            final float offsetX = mScrollX - child.mLeft;
            final float offsetY = mScrollY - child.mTop;
            event.offsetLocation(offsetX, offsetY);
            handled = child.dispatchGenericMotionEvent(event);
            event.offsetLocation(-offsetX, -offsetY);
        }
        return handled;
    }

    /**
     * Returns a MotionEvent that's been transformed into the child's local coordinates.
     * <p>
     * It's the responsibility of the caller to recycle it once they're finished with it.
     *
     * @param event The event to transform.
     * @param child The view whose coordinate space is to be used.
     * @return A copy of the the given MotionEvent, transformed into the given View's coordinate
     * space.
     */
    @NonNull
    private MotionEvent getTransformedMotionEvent(@NonNull MotionEvent event, @NonNull View child) {
        final float offsetX = mScrollX - child.mLeft;
        final float offsetY = mScrollY - child.mTop;
        final MotionEvent transformedEvent = event.copy();
        transformedEvent.offsetLocation(offsetX, offsetY);
        if (!child.hasIdentityMatrix()) {
            transformedEvent.transform(child.getInverseMatrix());
        }
        return transformedEvent;
    }

    /**
     * Implement this method to intercept hover events before they are handled
     * by child views.
     * <p>
     * This method is called before dispatching a hover event to a child of
     * the view group or to the view group's own {@link #onHoverEvent} to allow
     * the view group a chance to intercept the hover event.
     * This method can also be used to watch all pointer motions that occur within
     * the bounds of the view group even when the pointer is hovering over
     * a child of the view group rather than over the view group itself.
     * </p><p>
     * The view group can prevent its children from receiving hover events by
     * implementing this method and returning <code>true</code> to indicate
     * that it would like to intercept hover events.  The view group must
     * continuously return <code>true</code> from this method for as long as
     * it wishes to continue intercepting hover events from its children.
     * </p><p>
     * Interception preserves the invariant that at most one view can be
     * hovered at a time by transferring hover focus from the currently hovered
     * child to the view group or vice-versa as needed.
     * </p><p>
     * If this method returns <code>true</code> and a child is already hovered, then the
     * child view will first receive a hover exit event and then the view group
     * itself will receive a hover enter event in {@link #onHoverEvent}.
     * Likewise, if this method had previously returned <code>true</code> to intercept hover
     * events and instead returns <code>false</code> while the pointer is hovering
     * within the bounds of one of a child, then the view group will first receive a
     * hover exit event in {@link #onHoverEvent} and then the hovered child will
     * receive a hover enter event.
     * </p><p>
     * The default implementation handles mouse hover on the scroll bars.
     * </p>
     *
     * @param event The motion event that describes the hover.
     * @return True if the view group would like to intercept the hover event
     * and prevent its children from receiving it.
     */
    public boolean onInterceptHoverEvent(@NonNull MotionEvent event) {
        final int action = event.getAction();
        final float x = event.getX();
        final float y = event.getY();
        return (action == MotionEvent.ACTION_HOVER_MOVE
                || action == MotionEvent.ACTION_HOVER_ENTER) && isOnScrollbar(x, y);
    }

    @Override
    protected boolean dispatchGenericPointerEvent(@NonNull MotionEvent event) {
        // Send the event to the child under the pointer.
        final int childrenCount = mChildrenCount;
        if (childrenCount != 0) {
            final float x = event.getX();
            final float y = event.getY();

            final ArrayList<View> preorderedList = buildOrderedChildList();
            final boolean customOrder = preorderedList == null
                    && isChildrenDrawingOrderEnabled();
            final View[] children = mChildren;
            for (int i = childrenCount - 1; i >= 0; i--) {
                final int childIndex = getAndVerifyPreorderedIndex(childrenCount, i, customOrder);
                final View child = getAndVerifyPreorderedView(preorderedList, children, childIndex);
                if (!child.canReceivePointerEvents()
                        || !isTransformedTouchPointInView(x, y, child, null)) {
                    continue;
                }

                if (dispatchTransformedGenericPointerEvent(event, child)) {
                    if (preorderedList != null) {
                        preorderedList.clear();
                    }
                    return true;
                }
            }
            if (preorderedList != null) {
                preorderedList.clear();
            }
        }

        // No child handled the event.  Send it to this view group.
        return super.dispatchGenericPointerEvent(event);
    }

    private static View getAndVerifyPreorderedView(@Nullable ArrayList<View> preorderedList, View[] children,
                                                   int childIndex) {
        final View child;
        if (preorderedList != null) {
            child = preorderedList.get(childIndex);
            if (child == null) {
                throw new RuntimeException("Invalid preorderedList contained null child at index "
                        + childIndex);
            }
        } else {
            child = children[childIndex];
        }
        return child;
    }

    /*@Override
    public final boolean dispatchGenericMotionEvent(@NonNull MotionEvent event) {
        final int action = event.getAction();

        float scrollX = getScrollX();
        float scrollY = getScrollY();
        event.offsetLocation(scrollX, scrollY);

        final View[] views = children;
        View child;
        boolean anyHovered = false;

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                for (int i = childrenCount - 1; i >= 0; i--) {
                    child = views[i];
                    if (!anyHovered && child.onGenericMotionEvent(event)) {
                        anyHovered = true;
                    } else {
                        child.ensureMouseHoverExit();
                    }
                }
                return anyHovered;
            *//*case MotionEvent.ACTION_PRESS:
            case MotionEvent.ACTION_RELEASE:
            case MotionEvent.ACTION_DOUBLE_CLICK:*//*
            case MotionEvent.ACTION_SCROLL:
                for (int i = childrenCount - 1; i >= 0; i--) {
                    child = views[i];
                    if (child.onGenericMotionEvent(event)) {
                        return true;
                    }
                }
                return false;
        }

        event.offsetLocation(-scrollX, -scrollY);
        return super.dispatchGenericMotionEvent(event);
    }*/

    /*@Deprecated
    @Override
    final boolean dispatchUpdateMouseHover(double mouseX, double mouseY) {
        final double mx = mouseX + getScrollX();
        final double my = mouseY + getScrollY();
        final View[] views = children;
        boolean anyHovered = false;
        View child;
        for (int i = childrenCount - 1; i >= 0; i--) {
            child = views[i];
            if (!anyHovered && child.updateMouseHover(mx, my)) {
                anyHovered = true;
            } else {
                child.ensureMouseHoverExit();
            }
        }
        return anyHovered;
    }*/

    /*@Override
    final void ensureMouseHoverExit() {
        super.ensureMouseHoverExit();
        final View[] views = children;
        final int count = childrenCount;
        for (int i = 0; i < count; i++) {
            views[i].ensureMouseHoverExit();
        }
    }*/

    /**
     * Resets the cancel next up flag.
     *
     * @return true if the flag was previously set.
     */
    private static boolean resetCancelNextUpFlag(@NonNull View view) {
        if ((view.mPrivateFlags & PFLAG_CANCEL_NEXT_UP_EVENT) != 0) {
            view.mPrivateFlags &= ~PFLAG_CANCEL_NEXT_UP_EVENT;
            return true;
        }
        return false;
    }

    /**
     * Clears all touch targets.
     */
    private void clearTouchTargets() {
        TouchTarget target = mTouchTarget;
        if (target != null) {
            target.recycle();
            mTouchTarget = null;
        }
    }

    /**
     * Resets all touch state in preparation for a new cycle.
     */
    private void resetTouchState() {
        clearTouchTargets();
        resetCancelNextUpFlag(this);
        mGroupFlags &= ~FLAG_DISALLOW_INTERCEPT;
    }

    /**
     * Cancels and clears all touch targets.
     */
    private void cancelAndClearTouchTargets(@Nullable MotionEvent event) {
        TouchTarget target = mTouchTarget;
        if (target != null) {
            boolean syntheticEvent = false;
            if (event == null) {
                final long time = Core.timeNanos();
                event = MotionEvent.obtain(time,
                        MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
                syntheticEvent = true;
            }

            resetCancelNextUpFlag(target.child);
            dispatchTransformedTouchEvent(event, target.child, true);
            clearTouchTargets();

            if (syntheticEvent) {
                event.recycle();
            }
        }
    }

    private void cancelTouchTarget(View view) {
        TouchTarget target = mTouchTarget;
        if (target != null) {
            if (target.child == view) {
                mTouchTarget = null;
                target.recycle();

                final long now = Core.timeNanos();
                MotionEvent event = MotionEvent.obtain(now,
                        MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
                view.dispatchTouchEvent(event);
                event.recycle();
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
        boolean handled = false;

        final int action = ev.getAction();

        // Handle an initial down.
        if (action == MotionEvent.ACTION_DOWN) {
            // Throw away all previous state when starting a new touch gesture.
            // The framework may have dropped the up or cancel event for the previous gesture
            // due to an app switch, ANR, or some other state change.
            cancelAndClearTouchTargets(ev);
            resetTouchState();
        }

        // Check for interception.
        final boolean intercepted;
        if (action == MotionEvent.ACTION_DOWN || mTouchTarget != null) {
            final boolean allowIntercept = (mGroupFlags & FLAG_DISALLOW_INTERCEPT) == 0;
            if (allowIntercept) {
                intercepted = onInterceptTouchEvent(ev);
                ev.setAction(action); // restore action in case it was changed
            } else {
                intercepted = false;
            }
        } else {
            // There are no touch targets and this action is not an initial down
            // so this view group continues to intercept touches.
            intercepted = true;
        }

        // Check for cancellation.
        final boolean canceled = resetCancelNextUpFlag(this)
                || action == MotionEvent.ACTION_CANCEL;

        // Update list of touch targets for pointer down, if needed.
        TouchTarget newTouchTarget = null;
        boolean dispatchedToNewTarget = false;
        if (!canceled && !intercepted && action == MotionEvent.ACTION_DOWN && mChildrenCount > 0) {
            final int childrenCount = mChildrenCount;
            final float x = ev.getX();
            final float y = ev.getY();
            // Find a child that can receive the event.
            // Scan children from front to back.
            final ArrayList<View> preorderedList = buildOrderedChildList();
            final boolean customOrder = preorderedList == null
                    && isChildrenDrawingOrderEnabled();
            final View[] children = mChildren;
            for (int i = childrenCount - 1; i >= 0; i--) {
                final int childIndex = getAndVerifyPreorderedIndex(
                        childrenCount, i, customOrder);
                final View child = getAndVerifyPreorderedView(
                        preorderedList, children, childIndex);

                if (!child.canReceivePointerEvents()
                        || !isTransformedTouchPointInView(x, y, child, null)) {
                    continue;
                }

                resetCancelNextUpFlag(child);
                if (dispatchTransformedTouchEvent(ev, child, false)) {
                    mTouchTarget = TouchTarget.obtain(child);
                    dispatchedToNewTarget = true;
                    break;
                }
            }
            if (preorderedList != null) {
                preorderedList.clear();
            }
        }

        // Dispatch to touch targets.
        if (mTouchTarget == null) {
            // No touch targets so treat this as an ordinary view.
            handled = dispatchTransformedTouchEvent(ev, null, canceled);
        } else {
            // Dispatch to touch targets, excluding the new touch target if we already
            // dispatched to it.  Cancel touch targets if necessary.
            if (dispatchedToNewTarget) {
                handled = true;
            } else {
                final TouchTarget target = mTouchTarget;
                final boolean cancelChild = resetCancelNextUpFlag(target.child)
                        || intercepted;
                if (dispatchTransformedTouchEvent(ev, target.child, cancelChild)) {
                    handled = true;
                }
                if (cancelChild) {
                    mTouchTarget = null;
                    target.recycle();
                }
            }
        }

        // Update list of touch targets for pointer up or cancel, if needed.
        if (canceled || action == MotionEvent.ACTION_UP) {
            resetTouchState();
        }
        return handled;
    }

    /**
     * Returns true if this ViewGroup should be considered as a single entity for removal
     * when executing an Activity transition. If this is false, child elements will move
     * individually during the transition.
     *
     * @return True if the ViewGroup should be acted on together during an Activity transition.
     * The default value is true when there is a non-null background or if
     * {@link #getTransitionName()} is not null and false otherwise.
     */
    public boolean isTransitionGroup() {
        if ((mGroupFlags & FLAG_IS_TRANSITION_GROUP_SET) != 0) {
            return ((mGroupFlags & FLAG_IS_TRANSITION_GROUP) != 0);
        } else {
            return getBackground() != null || getTransitionName() != null;
        }
    }

    /**
     * Changes whether or not this ViewGroup should be treated as a single entity during
     * Activity Transitions.
     *
     * @param isTransitionGroup Whether or not the ViewGroup should be treated as a unit
     *                          in Activity transitions. If false, the ViewGroup won't transition,
     *                          only its children. If true, the entire ViewGroup will transition
     *                          together.
     */
    public void setTransitionGroup(boolean isTransitionGroup) {
        mGroupFlags |= FLAG_IS_TRANSITION_GROUP_SET;
        if (isTransitionGroup) {
            mGroupFlags |= FLAG_IS_TRANSITION_GROUP;
        } else {
            mGroupFlags &= ~FLAG_IS_TRANSITION_GROUP;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept == ((mGroupFlags & FLAG_DISALLOW_INTERCEPT) != 0)) {
            // We're already in this state, assume our ancestors are too
            return;
        }

        if (disallowIntercept) {
            mGroupFlags |= FLAG_DISALLOW_INTERCEPT;
        } else {
            mGroupFlags &= ~FLAG_DISALLOW_INTERCEPT;
        }

        // Pass it up to our parent
        if (mParent != null) {
            mParent.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    /**
     * Implement this method to intercept all touch screen motion events.  This
     * allows you to watch events as they are dispatched to your children, and
     * take ownership of the current gesture at any point.
     *
     * <p>Using this function takes some care, as it has a fairly complicated
     * interaction with {@link View#onTouchEvent(MotionEvent)
     * View.onTouchEvent(MotionEvent)}, and using it requires implementing
     * that method as well as this one in the correct way.  Events will be
     * received in the following order:
     *
     * <ol>
     * <li> You will receive the down event here.
     * <li> The down event will be handled either by a child of this view
     * group, or given to your own onTouchEvent() method to handle; this means
     * you should implement onTouchEvent() to return true, so you will
     * continue to see the rest of the gesture (instead of looking for
     * a parent view to handle it).  Also, by returning true from
     * onTouchEvent(), you will not receive any following
     * events in onInterceptTouchEvent() and all touch processing must
     * happen in onTouchEvent() like normal.
     * <li> For as long as you return false from this function, each following
     * event (up to and including the final up) will be delivered first here
     * and then to the target's onTouchEvent().
     * <li> If you return true from here, you will not receive any
     * following events: the target view will receive the same event but
     * with the action {@link MotionEvent#ACTION_CANCEL}, and all further
     * events will be delivered to your onTouchEvent() method and no longer
     * appear here.
     * </ol>
     *
     * @param ev The motion event being dispatched down the hierarchy.
     * @return Return true to steal motion events from the children and have
     * them dispatched to this ViewGroup through onTouchEvent().
     * The current target will receive an ACTION_CANCEL event, and no further
     * messages will be delivered here.
     */
    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
        return ev.getAction() == MotionEvent.ACTION_DOWN
                && ev.isButtonPressed(MotionEvent.BUTTON_PRIMARY)
                && isOnScrollbarThumb(ev.getX(), ev.getY());
    }


    /**
     * {@inheritDoc}
     * <p>
     * Looks for a view to give focus to respecting the setting specified by
     * {@link #getDescendantFocusability()}.
     * <p>
     * Uses {@link #onRequestFocusInDescendants(int, Rect)} to
     * find focus within the children of this group when appropriate.
     *
     * @see #FOCUS_BEFORE_DESCENDANTS
     * @see #FOCUS_AFTER_DESCENDANTS
     * @see #FOCUS_BLOCK_DESCENDANTS
     * @see #onRequestFocusInDescendants(int, Rect)
     */
    @Override
    public boolean requestFocus(int direction, @Nullable Rect previouslyFocusedRect) {
        int descendantFocusability = getDescendantFocusability();

        boolean result;
        switch (descendantFocusability) {
            case FOCUS_BLOCK_DESCENDANTS -> result = super.requestFocus(direction, previouslyFocusedRect);
            case FOCUS_BEFORE_DESCENDANTS -> {
                final boolean took = super.requestFocus(direction, previouslyFocusedRect);
                result = took || onRequestFocusInDescendants(direction, previouslyFocusedRect);
            }
            case FOCUS_AFTER_DESCENDANTS -> {
                final boolean took = onRequestFocusInDescendants(direction, previouslyFocusedRect);
                result = took || super.requestFocus(direction, previouslyFocusedRect);
            }
            default -> throw new IllegalStateException("descendant focusability must be "
                    + "one of FOCUS_BEFORE_DESCENDANTS, FOCUS_AFTER_DESCENDANTS, FOCUS_BLOCK_DESCENDANTS "
                    + "but is " + descendantFocusability);
        }
        if (result && !isLayoutValid() && ((mPrivateFlags & PFLAG_WANTS_FOCUS) == 0)) {
            mPrivateFlags |= PFLAG_WANTS_FOCUS;
        }
        return result;
    }

    /**
     * Look for a descendant to call {@link View#requestFocus} on.
     * Called by {@link ViewGroup#requestFocus(int, Rect)}
     * when it wants to request focus within its children.  Override this to
     * customize how your {@link ViewGroup} requests focus within its children.
     *
     * @param direction             One of FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, and FOCUS_RIGHT
     * @param previouslyFocusedRect The rectangle (in this View's coordinate system)
     *                              to give a finer grained hint about where focus is coming from.  May be null
     *                              if there is no hint.
     * @return Whether focus was taken.
     */
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        int index;
        int increment;
        int end;
        int count = mChildrenCount;
        if ((direction & FOCUS_FORWARD) != 0) {
            index = 0;
            increment = 1;
            end = count;
        } else {
            index = count - 1;
            increment = -1;
            end = -1;
        }
        final View[] children = mChildren;
        for (int i = index; i != end; i += increment) {
            View child = children[i];
            if ((child.mViewFlags & VISIBILITY_MASK) == VISIBLE) {
                if (child.requestFocus(direction, previouslyFocusedRect)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean restoreDefaultFocus() {
        if (mDefaultFocus != null
                && getDescendantFocusability() != FOCUS_BLOCK_DESCENDANTS
                && (mDefaultFocus.mViewFlags & VISIBILITY_MASK) == VISIBLE
                && mDefaultFocus.restoreDefaultFocus()) {
            return true;
        }
        return super.restoreDefaultFocus();
    }

    @Override
    boolean restoreFocusInCluster(@FocusRealDirection int direction) {
        // Allow cluster-navigation to enter touchscreenBlocksFocus ViewGroups.
        if (isKeyboardNavigationCluster()) {
            final boolean blockedFocus = getTouchscreenBlocksFocus();
            try {
                setTouchscreenBlocksFocusNoRefocus(false);
                return restoreFocusInClusterInternal(direction);
            } finally {
                setTouchscreenBlocksFocusNoRefocus(blockedFocus);
            }
        } else {
            return restoreFocusInClusterInternal(direction);
        }
    }

    private boolean restoreFocusInClusterInternal(@FocusRealDirection int direction) {
        if (mFocusedInCluster != null && getDescendantFocusability() != FOCUS_BLOCK_DESCENDANTS
                && (mFocusedInCluster.mViewFlags & VISIBILITY_MASK) == VISIBLE
                && mFocusedInCluster.restoreFocusInCluster(direction)) {
            return true;
        }
        return super.restoreFocusInCluster(direction);
    }

    @Override
    boolean restoreFocusNotInCluster() {
        if (mFocusedInCluster != null) {
            // since clusters don't nest; we can assume that a non-null mFocusedInCluster
            // will refer to a view not-in a cluster.
            return restoreFocusInCluster(View.FOCUS_DOWN);
        }
        if (isKeyboardNavigationCluster() || (mViewFlags & VISIBILITY_MASK) != VISIBLE) {
            return false;
        }
        int descendentFocusability = getDescendantFocusability();
        if (descendentFocusability == FOCUS_BLOCK_DESCENDANTS) {
            return super.requestFocus(FOCUS_DOWN, null);
        }
        if (descendentFocusability == FOCUS_BEFORE_DESCENDANTS
                && super.requestFocus(FOCUS_DOWN, null)) {
            return true;
        }
        for (int i = 0; i < mChildrenCount; ++i) {
            View child = mChildren[i];
            if (!child.isKeyboardNavigationCluster()
                    && child.restoreFocusNotInCluster()) {
                return true;
            }
        }
        if (descendentFocusability == FOCUS_AFTER_DESCENDANTS && !hasFocusableChild(false)) {
            return super.requestFocus(FOCUS_DOWN, null);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @ApiStatus.Internal
    @Override
    public void dispatchStartTemporaryDetach() {
        super.dispatchStartTemporaryDetach();
        final int count = mChildrenCount;
        final View[] children = mChildren;
        for (int i = 0; i < count; i++) {
            children[i].dispatchStartTemporaryDetach();
        }
    }

    /**
     * {@inheritDoc}
     */
    @ApiStatus.Internal
    @Override
    public void dispatchFinishTemporaryDetach() {
        super.dispatchFinishTemporaryDetach();
        final int count = mChildrenCount;
        final View[] children = mChildren;
        for (int i = 0; i < count; i++) {
            children[i].dispatchFinishTemporaryDetach();
        }
    }

    @Override
    final void dispatchAttachedToWindow(AttachInfo info, int visibility) {
        mGroupFlags |= FLAG_PREVENT_DISPATCH_ATTACHED_TO_WINDOW;
        super.dispatchAttachedToWindow(info, visibility);
        mGroupFlags &= ~FLAG_PREVENT_DISPATCH_ATTACHED_TO_WINDOW;

        final int count = mChildrenCount;
        final View[] children = mChildren;
        for (int i = 0; i < count; i++) {
            final View child = children[i];
            child.dispatchAttachedToWindow(info,
                    combineVisibility(visibility, child.getVisibility()));
        }
        final int transientCount = mTransientIndices == null ? 0 : mTransientIndices.size();
        for (int i = 0; i < transientCount; ++i) {
            View view = mTransientViews.get(i);
            view.dispatchAttachedToWindow(info,
                    combineVisibility(visibility, view.getVisibility()));
        }
    }

    @Override
    final void dispatchDetachedFromWindow() {
        // If we still have a touch target, we are still in the process of
        // dispatching motion events to a child; we need to get rid of that
        // child to avoid dispatching events to it after the window is torn
        // down. To make sure we keep the child in a consistent state, we
        // first send it an ACTION_CANCEL motion event.
        cancelAndClearTouchTargets(null);

        // Similarly, set ACTION_EXIT to all hover targets and clear them.
        exitHoverTargets();

        // In case view is detached while transition is running
        mLayoutCalledWhileSuppressed = false;

        final int count = mChildrenCount;
        final View[] children = mChildren;
        for (int i = 0; i < count; i++) {
            children[i].dispatchDetachedFromWindow();
        }
        clearDisappearingChildren();
        final int transientCount = mTransientViews == null ? 0 : mTransientIndices.size();
        for (int i = 0; i < transientCount; ++i) {
            View view = mTransientViews.get(i);
            view.dispatchDetachedFromWindow();
        }
        super.dispatchDetachedFromWindow();
    }

    private float[] getTempLocationF() {
        if (mTempPosition == null) {
            mTempPosition = new float[2];
        }
        return mTempPosition;
    }

    /**
     * Transforms a motion event into the coordinate space of a particular child view,
     * filters out irrelevant pointer ids, and overrides its action if necessary.
     * If child is null, assumes the MotionEvent will be sent to this ViewGroup instead.
     */
    boolean dispatchTransformedTouchEvent(@NonNull MotionEvent event,
                                          @Nullable View child, boolean cancel) {
        final boolean handled;

        // Canceling motions is a special case.  We don't need to perform any transformations
        // or filtering.  The important part is the action, not the contents.
        final int oldAction = event.getAction();
        if (cancel || oldAction == MotionEvent.ACTION_CANCEL) {
            event.setAction(MotionEvent.ACTION_CANCEL);
            if (child == null) {
                handled = super.dispatchTouchEvent(event);
            } else {
                handled = child.dispatchTouchEvent(event);
            }
            event.setAction(oldAction);
            return handled;
        }

        if (child == null || child.hasIdentityMatrix()) {
            if (child == null) {
                handled = super.dispatchTouchEvent(event);
            } else {
                final float offsetX = mScrollX - child.mLeft;
                final float offsetY = mScrollY - child.mTop;
                event.offsetLocation(offsetX, offsetY);

                handled = child.dispatchTouchEvent(event);

                event.offsetLocation(-offsetX, -offsetY);
            }
        } else {
            final MotionEvent transformedEvent = event.copy();

            final float offsetX = mScrollX - child.mLeft;
            final float offsetY = mScrollY - child.mTop;
            transformedEvent.offsetLocation(offsetX, offsetY);
            if (!child.hasIdentityMatrix()) {
                transformedEvent.transform(child.getInverseMatrix());
            }

            handled = child.dispatchTouchEvent(transformedEvent);

            transformedEvent.recycle();
        }
        return handled;
    }

    /**
     * Returns true if a child view contains the specified point when transformed
     * into its coordinate space.
     * Child must not be null.
     */
    boolean isTransformedTouchPointInView(float x, float y, @NonNull View child,
                                          @Nullable PointF outLocalPoint) {
        final float[] point = getTempLocationF();
        point[0] = x;
        point[1] = y;
        transformPointToViewLocal(point, child);
        final boolean isInView = child.pointInView(point[0], point[1]);
        if (isInView && outLocalPoint != null) {
            outLocalPoint.set(point[0], point[1]);
        }
        return isInView;
    }

    @ApiStatus.Internal
    protected void transformPointToViewLocal(@NonNull float[] point, @NonNull View child) {
        point[0] += mScrollX - child.mLeft;
        point[1] += mScrollY - child.mTop;

        if (!child.hasIdentityMatrix()) {
            child.getInverseMatrix().mapPoint(point);
        }
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if ((mPrivateFlags & (PFLAG_FOCUSED | PFLAG_HAS_BOUNDS))
                == (PFLAG_FOCUSED | PFLAG_HAS_BOUNDS)) {
            return super.dispatchKeyEvent(event);
        } else if (mFocused != null && (mFocused.mPrivateFlags & PFLAG_HAS_BOUNDS)
                == PFLAG_HAS_BOUNDS) {
            return mFocused.dispatchKeyEvent(event);
        }
        return false;
    }

    @Override
    public boolean dispatchKeyShortcutEvent(@NonNull KeyEvent event) {
        if ((mPrivateFlags & (PFLAG_FOCUSED | PFLAG_HAS_BOUNDS))
                == (PFLAG_FOCUSED | PFLAG_HAS_BOUNDS)) {
            return super.dispatchKeyShortcutEvent(event);
        } else if (mFocused != null && (mFocused.mPrivateFlags & PFLAG_HAS_BOUNDS)
                == PFLAG_HAS_BOUNDS) {
            return mFocused.dispatchKeyShortcutEvent(event);
        }
        return false;
    }

    @Override
    public PointerIcon onResolvePointerIcon(@NonNull MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();
        if (isOnScrollbarThumb(x, y) || isDraggingScrollBar()) {
            return PointerIcon.getSystemIcon(PointerIcon.TYPE_ARROW);
        }
        // Check what the child under the pointer says about the pointer.
        final int childrenCount = mChildrenCount;
        if (childrenCount != 0) {
            final ArrayList<View> preorderedList = buildOrderedChildList();
            final boolean customOrder = preorderedList == null
                    && isChildrenDrawingOrderEnabled();
            final View[] children = mChildren;
            for (int i = childrenCount - 1; i >= 0; i--) {
                final int childIndex = getAndVerifyPreorderedIndex(childrenCount, i, customOrder);
                final View child = getAndVerifyPreorderedView(preorderedList, children, childIndex);

                if (!child.canReceivePointerEvents()
                        || !isTransformedTouchPointInView(x, y, child, null)) {
                    continue;
                }
                final PointerIcon pointerIcon = dispatchResolvePointerIcon(event, child);
                if (pointerIcon != null) {
                    if (preorderedList != null) preorderedList.clear();
                    return pointerIcon;
                }
            }
            if (preorderedList != null) preorderedList.clear();
        }

        // The pointer is not a child or the child has no preferences, returning the default
        // implementation.
        return super.onResolvePointerIcon(event);
    }

    @Nullable
    PointerIcon dispatchResolvePointerIcon(MotionEvent event, @NonNull View child) {
        final PointerIcon pointerIcon;
        if (!child.hasIdentityMatrix()) {
            MotionEvent transformedEvent = getTransformedMotionEvent(event, child);
            pointerIcon = child.onResolvePointerIcon(transformedEvent);
            transformedEvent.recycle();
        } else {
            final float offsetX = mScrollX - child.mLeft;
            final float offsetY = mScrollY - child.mTop;
            event.offsetLocation(offsetX, offsetY);
            pointerIcon = child.onResolvePointerIcon(event);
            event.offsetLocation(-offsetX, -offsetY);
        }
        return pointerIcon;
    }

    /**
     * This method adds a view to this container at the specified index purely for the
     * purposes of allowing that view to draw even though it is not a normal child of
     * the container. That is, the view does not participate in layout, focus, accessibility,
     * input, or other normal view operations; it is purely an item to be drawn during the normal
     * rendering operation of this container. The index that it is added at is the order
     * in which it will be drawn, with respect to the other views in the container.
     * For example, a transient view added at index 0 will be drawn before all other views
     * in the container because it will be drawn first (including before any real view
     * at index 0). There can be more than one transient view at any particular index;
     * these views will be drawn in the order in which they were added to the list of
     * transient views. The index of transient views can also be greater than the number
     * of normal views in the container; that just means that they will be drawn after all
     * other views are drawn.
     *
     * <p>Note that since transient views do not participate in layout, they must be sized
     * manually or, more typically, they should just use the size that they had before they
     * were removed from their container.</p>
     *
     * <p>Transient views are useful for handling animations of views that have been removed
     * from the container, but which should be animated out after the removal. Adding these
     * views as transient views allows them to participate in drawing without side-effecting
     * the layout of the container.</p>
     *
     * <p>Transient views must always be explicitly {@link #removeTransientView(View) removed}
     * from the container when they are no longer needed. For example, a transient view
     * which is added in order to fade it out in its old location should be removed
     * once the animation is complete.</p>
     *
     * @param view  The view to be added. The view must not have a parent.
     * @param index The index at which this view should be drawn, must be >= 0.
     *              This value is relative to the {@link #getChildAt(int) index} values in the normal
     *              child list of this container, where any transient view at a particular index will
     *              be drawn before any normal child at that same index.
     */
    @ApiStatus.Internal
    public void addTransientView(View view, int index) {
        if (index < 0 || view == null) {
            return;
        }
        if (view.mParent != null) {
            throw new IllegalStateException("The specified view already has a parent "
                    + view.mParent);
        }

        if (mTransientIndices == null) {
            mTransientIndices = new IntArrayList();
            mTransientViews = new ArrayList<>();
        }
        final int oldSize = mTransientIndices.size();
        if (oldSize > 0) {
            int insertionIndex;
            for (insertionIndex = 0; insertionIndex < oldSize; ++insertionIndex) {
                if (index < mTransientIndices.getInt(insertionIndex)) {
                    break;
                }
            }
            mTransientIndices.add(insertionIndex, index);
            mTransientViews.add(insertionIndex, view);
        } else {
            mTransientIndices.add(index);
            mTransientViews.add(view);
        }
        view.mParent = this;
        if (mAttachInfo != null) {
            view.dispatchAttachedToWindow(mAttachInfo, (mViewFlags & VISIBILITY_MASK));
        }
        invalidate();
    }

    /**
     * Removes a view from the list of transient views in this container. If there is no
     * such transient view, this method does nothing.
     *
     * @param view The transient view to be removed
     */
    @ApiStatus.Internal
    public void removeTransientView(View view) {
        if (mTransientViews == null) {
            return;
        }
        final int size = mTransientViews.size();
        for (int i = 0; i < size; ++i) {
            if (view == mTransientViews.get(i)) {
                mTransientViews.remove(i);
                mTransientIndices.removeInt(i);
                view.mParent = null;
                if (view.mAttachInfo != null) {
                    view.dispatchDetachedFromWindow();
                }
                invalidate();
                return;
            }
        }
    }

    /**
     * Returns the number of transient views in this container. Specific transient
     * views and the index at which they were added can be retrieved via
     * {@link #getTransientView(int)} and {@link #getTransientViewIndex(int)}.
     *
     * @return The number of transient views in this container
     * @see #addTransientView(View, int)
     */
    @ApiStatus.Internal
    public int getTransientViewCount() {
        return mTransientIndices == null ? 0 : mTransientIndices.size();
    }

    /**
     * Given a valid position within the list of transient views, returns the index of
     * the transient view at that position.
     *
     * @param position The position of the index being queried. Must be at least 0
     *                 and less than the value returned by {@link #getTransientViewCount()}.
     * @return The index of the transient view stored in the given position if the
     * position is valid, otherwise -1
     */
    @ApiStatus.Internal
    public int getTransientViewIndex(int position) {
        if (position < 0 || mTransientIndices == null || position >= mTransientIndices.size()) {
            return -1;
        }
        return mTransientIndices.getInt(position);
    }

    /**
     * Given a valid position within the list of transient views, returns the
     * transient view at that position.
     *
     * @param position The position of the view being queried. Must be at least 0
     *                 and less than the value returned by {@link #getTransientViewCount()}.
     * @return The transient view stored in the given position if the
     * position is valid, otherwise null
     */
    @ApiStatus.Internal
    public View getTransientView(int position) {
        if (mTransientViews == null || position >= mTransientViews.size()) {
            return null;
        }
        return mTransientViews.get(position);
    }

    /**
     * <p>Adds a child view. If no layout parameters are already set on the child, the
     * default parameters for this ViewGroup are set on the child.</p>
     *
     * <p><strong>Note:</strong> do not invoke this method from
     * {@link #draw(Canvas)}, {@link #onDraw(Canvas)},
     * {@link #dispatchDraw(Canvas)} or any related method.</p>
     *
     * @param child the child view to add
     * @see #generateDefaultLayoutParams()
     */
    public void addView(@NonNull View child) {
        addView(child, -1);
    }

    /**
     * Adds a child view. If no layout parameters are already set on the child, the
     * default parameters for this ViewGroup are set on the child.
     *
     * <p><strong>Note:</strong> do not invoke this method from
     * {@link #draw(Canvas)}, {@link #onDraw(Canvas)},
     * {@link #dispatchDraw(Canvas)} or any related method.</p>
     *
     * @param child the child view to add
     * @param index the position at which to add the child
     * @see #generateDefaultLayoutParams()
     */
    public void addView(@NonNull View child, int index) {
        LayoutParams params = child.getLayoutParams();
        if (params == null) {
            params = generateDefaultLayoutParams();
        }
        addView(child, index, params);
    }

    /**
     * Adds a child view with this ViewGroup's default layout parameters and the
     * specified width and height.
     *
     * <p><strong>Note:</strong> do not invoke this method from
     * {@link #draw(Canvas)}, {@link #onDraw(Canvas)},
     * {@link #dispatchDraw(Canvas)} or any related method.</p>
     *
     * @param child the child view to add
     */
    public void addView(@NonNull View child, int width, int height) {
        final LayoutParams params = generateDefaultLayoutParams();
        params.width = width;
        params.height = height;
        addView(child, -1, params);
    }

    /**
     * Adds a child view with the specified layout parameters.
     *
     * <p><strong>Note:</strong> do not invoke this method from
     * {@link #draw(Canvas)}, {@link #onDraw(Canvas)},
     * {@link #dispatchDraw(Canvas)} or any related method.</p>
     *
     * @param child  the child view to add
     * @param params the layout parameters to set on the child
     */
    @Override
    public void addView(@NonNull View child, @NonNull LayoutParams params) {
        addView(child, -1, params);
    }

    /**
     * Adds a child view with the specified layout parameters.
     *
     * <p><strong>Note:</strong> do not invoke this method from
     * {@link #draw(Canvas)}, {@link #onDraw(Canvas)},
     * {@link #dispatchDraw(Canvas)} or any related method.</p>
     *
     * @param child  the child view to add
     * @param index  the position at which to add the child or -1 to add last
     * @param params the layout parameters to set on the child
     */
    public void addView(@NonNull View child, int index, @NonNull LayoutParams params) {
        // addViewInner() will call child.requestLayout() when setting the new LayoutParams
        // therefore, we call requestLayout() on ourselves before, so that the child's request
        // will be blocked at our level
        requestLayout();
        invalidate();
        addViewInner(child, index, params, false);
    }

    @Override
    public void updateViewLayout(@NonNull View view, @NonNull ViewGroup.LayoutParams params) {
        if (!checkLayoutParams(params)) {
            throw new IllegalArgumentException("Invalid LayoutParams supplied to " + this);
        }
        if (view.mParent != this) {
            throw new IllegalArgumentException("Given view not a child of " + this);
        }
        view.setLayoutParams(params);
    }

    /**
     * Adds a view during layout. This is useful if in your onLayout() method,
     * you need to add more views (as does the list view for example).
     * <p>
     * If index is negative, it means put it at the end of the list.
     *
     * @param child  the view to add to the group
     * @param index  the index at which the child must be added or -1 to add last
     * @param params the layout parameters to associate with the child
     * @return true if the child was added, false otherwise
     */
    protected boolean addViewInLayout(@NonNull View child, int index, @NonNull LayoutParams params) {
        return addViewInLayout(child, index, params, false);
    }

    /**
     * Adds a view during layout. This is useful if in your onLayout() method,
     * you need to add more views (as does the list view for example).
     * <p>
     * If index is negative, it means put it at the end of the list.
     *
     * @param child                the view to add to the group
     * @param index                the index at which the child must be added or -1 to add last
     * @param params               the layout parameters to associate with the child
     * @param preventRequestLayout if true, calling this method will not trigger a
     *                             layout request on child
     * @return true if the child was added, false otherwise
     */
    protected boolean addViewInLayout(@NonNull View child, int index, @NonNull LayoutParams params,
                                      boolean preventRequestLayout) {
        child.mParent = null;
        addViewInner(child, index, params, preventRequestLayout);
        child.mPrivateFlags = (child.mPrivateFlags & ~PFLAG_DIRTY_MASK) | PFLAG_DRAWN;
        return true;
    }

    /**
     * Prevents the specified child to be laid out during the next layout pass.
     *
     * @param child the child on which to perform the cleanup
     */
    protected void cleanupLayoutState(@NonNull View child) {
        child.mPrivateFlags &= ~View.PFLAG_FORCE_LAYOUT;
    }

    private void addViewInner(@NonNull final View child, int index, @NonNull LayoutParams params,
                              boolean preventRequestLayout) {
        if (mTransition != null) {
            // Don't prevent other add transitions from completing, but cancel remove
            // transitions to let them complete the process before we add to the container
            mTransition.cancel(LayoutTransition.DISAPPEARING);
        }

        if (child.getParent() != null) {
            throw new IllegalStateException("The specified child already has a parent. " +
                    "You must call removeView() on the child's parent first.");
        }

        if (mTransition != null) {
            mTransition.addChild(this, child);
        }

        if (!checkLayoutParams(params)) {
            params = generateLayoutParams(params);
        }

        if (preventRequestLayout) {
            child.mLayoutParams = params;
        } else {
            child.setLayoutParams(params);
        }

        if (index < 0) {
            index = mChildrenCount;
        }

        addInArray(child, index);

        // tell our children
        if (preventRequestLayout) {
            child.assignParent(this);
        } else {
            child.mParent = this;
        }

        final boolean childHasFocus = child.hasFocus();
        if (childHasFocus) {
            requestChildFocus(child, child.findFocus());
        }

        AttachInfo attachInfo = mAttachInfo;
        if (attachInfo != null && (mGroupFlags & FLAG_PREVENT_DISPATCH_ATTACHED_TO_WINDOW) == 0) {
            child.dispatchAttachedToWindow(mAttachInfo, (mViewFlags & VISIBILITY_MASK));
        }

        if (child.isLayoutDirectionInherited()) {
            child.resetRtlProperties();
        }

        onViewAdded(child);

        if ((child.mViewFlags & DUPLICATE_PARENT_STATE) == DUPLICATE_PARENT_STATE) {
            mGroupFlags |= FLAG_NOTIFY_CHILDREN_ON_DRAWABLE_STATE_CHANGE;
        }

        if (child.hasTransientState()) {
            childHasTransientStateChanged(child, true);
        }

        if (mTransientIndices != null) {
            final int transientCount = mTransientIndices.size();
            for (int i = 0; i < transientCount; ++i) {
                final int oldIndex = mTransientIndices.getInt(i);
                if (index <= oldIndex) {
                    mTransientIndices.set(i, oldIndex + 1);
                }
            }
        }
    }

    /**
     * Called when a new child is added to this ViewGroup. Overrides should always
     * call super.onViewAdded.
     *
     * @param child the added child view
     */
    protected void onViewAdded(View child) {
    }

    /**
     * Called when a child view is removed from this ViewGroup. Overrides should always
     * call super.onViewRemoved.
     *
     * @param child the removed child view
     */
    protected void onViewRemoved(View child) {
    }

    /**
     * Returns the position in the group of the specified child view.
     *
     * @param child the view for which to get the position
     * @return a positive integer representing the position of the view in the
     * group, or -1 if the view does not exist in the group
     */
    public int indexOfChild(View child) {
        final int count = mChildrenCount;
        final View[] children = mChildren;
        for (int i = 0; i < count; i++) {
            if (children[i] == child) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the view at the specified position in the group.
     *
     * @param index the position at which to get the view from
     * @return the view at the specified position or null if the position
     * does not exist within the group
     */
    public View getChildAt(int index) {
        if (index < 0 || index >= mChildrenCount) {
            return null;
        }
        return mChildren[index];
    }

    /**
     * Returns the number of children in the group.
     *
     * @return a positive integer representing the number of children in
     * the group
     */
    public int getChildCount() {
        return mChildrenCount;
    }

    private void addInArray(View child, int index) {
        View[] children = mChildren;
        final int count = mChildrenCount;
        final int size = children.length;
        if (index == count) {
            if (size == count) {
                mChildren = new View[size + ARRAY_CAPACITY_INCREMENT];
                System.arraycopy(children, 0, mChildren, 0, size);
                children = mChildren;
            }
            children[mChildrenCount++] = child;
        } else if (index < count) {
            if (size == count) {
                mChildren = new View[size + ARRAY_CAPACITY_INCREMENT];
                System.arraycopy(children, 0, mChildren, 0, index);
                System.arraycopy(children, index, mChildren, index + 1, count - index);
                children = mChildren;
            } else {
                System.arraycopy(children, index, children, index + 1, count - index);
            }
            children[index] = child;
            mChildrenCount++;
        } else {
            throw new IndexOutOfBoundsException("index=" + index + " count=" + count);
        }
    }

    // This method also sets the child's mParent to null
    private void removeFromArray(int index) {
        final View[] children = mChildren;
        if (!(mTransitioningViews != null && mTransitioningViews.contains(children[index]))) {
            children[index].mParent = null;
        }
        final int count = mChildrenCount;
        if (index == count - 1) {
            children[--mChildrenCount] = null;
        } else if (index < count) {
            System.arraycopy(children, index + 1, children, index, count - index - 1);
            children[--mChildrenCount] = null;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    // This method also sets the children's mParent to null
    private void removeFromArray(int start, int count) {
        final View[] children = mChildren;
        final int childrenCount = mChildrenCount;

        start = Math.max(0, start);
        final int end = Math.min(childrenCount, start + count);

        if (start == end) {
            return;
        }

        if (end == childrenCount) {
            for (int i = start; i < end; i++) {
                children[i].mParent = null;
                children[i] = null;
            }
        } else {
            for (int i = start; i < end; i++) {
                children[i].mParent = null;
            }

            // Since we're looping above, we might as well do the copy, but is arraycopy()
            // faster than the extra 2 bounds checks we would do in the loop?
            System.arraycopy(children, end, children, start, childrenCount - end);

            for (int i = childrenCount - (end - start); i < childrenCount; i++) {
                children[i] = null;
            }
        }

        mChildrenCount -= (end - start);
    }

    /**
     * <p><strong>Note:</strong> do not invoke this method from
     * {@link #draw(Canvas)}, {@link #onDraw(Canvas)},
     * {@link #dispatchDraw(Canvas)} or any related method.</p>
     */
    @Override
    public void removeView(@NonNull View view) {
        if (removeViewInternal(view)) {
            requestLayout();
            invalidate();
        }
    }

    /**
     * Removes a view during layout. This is useful if in your onLayout() method,
     * you need to remove more views.
     *
     * <p><strong>Note:</strong> do not invoke this method from
     * {@link #draw(Canvas)}, {@link #onDraw(Canvas)},
     * {@link #dispatchDraw(Canvas)} or any related method.</p>
     *
     * @param view the view to remove from the group
     */
    public void removeViewInLayout(@NonNull View view) {
        removeViewInternal(view);
    }

    /**
     * Removes a range of views during layout. This is useful if in your onLayout() method,
     * you need to remove more views.
     *
     * <p><strong>Note:</strong> do not invoke this method from
     * {@link #draw(Canvas)}, {@link #onDraw(Canvas)},
     * {@link #dispatchDraw(Canvas)} or any related method.</p>
     *
     * @param start the index of the first view to remove from the group
     * @param count the number of views to remove from the group
     */
    public void removeViewsInLayout(int start, int count) {
        removeViewsInternal(start, count);
    }

    /**
     * Removes the view at the specified position in the group.
     *
     * <p><strong>Note:</strong> do not invoke this method from
     * {@link #draw(Canvas)}, {@link #onDraw(Canvas)},
     * {@link #dispatchDraw(Canvas)} or any related method.</p>
     *
     * @param index the position in the group of the view to remove
     */
    public void removeViewAt(int index) {
        removeViewInternal(index, getChildAt(index));
        requestLayout();
        invalidate();
    }

    /**
     * Removes the specified range of views from the group.
     *
     * <p><strong>Note:</strong> do not invoke this method from
     * {@link #draw(Canvas)}, {@link #onDraw(Canvas)},
     * {@link #dispatchDraw(Canvas)} or any related method.</p>
     *
     * @param start the first position in the group of the range of views to remove
     * @param count the number of views to remove
     */
    public void removeViews(int start, int count) {
        removeViewsInternal(start, count);
        requestLayout();
        invalidate();
    }

    private boolean removeViewInternal(View view) {
        final int index = indexOfChild(view);
        if (index >= 0) {
            removeViewInternal(index, view);
            return true;
        }
        return false;
    }

    private void removeViewInternal(int index, View view) {
        if (mTransition != null) {
            mTransition.removeChild(this, view);
        }

        boolean clearChildFocus = false;
        if (view == mFocused) {
            view.unFocus(null);
            clearChildFocus = true;
        }
        if (view == mFocusedInCluster) {
            clearFocusedInCluster(view);
        }

        cancelTouchTarget(view);
        cancelHoverTarget(view);

        if (mTransitioningViews != null && mTransitioningViews.contains(view)) {
            addDisappearingView(view);
        } else if (view.mAttachInfo != null) {
            view.dispatchDetachedFromWindow();
        }

        if (view.hasTransientState()) {
            childHasTransientStateChanged(view, false);
        }

        //needGlobalAttributesUpdate(false);

        removeFromArray(index);

        if (view == mDefaultFocus) {
            clearDefaultFocus(view);
        }
        if (clearChildFocus) {
            clearChildFocus(view);
            if (!rootViewRequestFocus()) {
                notifyGlobalFocusCleared(this);
            }
        }

        onViewRemoved(view);

        int transientCount = mTransientIndices == null ? 0 : mTransientIndices.size();
        for (int i = 0; i < transientCount; ++i) {
            final int oldIndex = mTransientIndices.getInt(i);
            if (index < oldIndex) {
                mTransientIndices.set(i, oldIndex - 1);
            }
        }
    }

    /**
     * Sets the LayoutTransition object for this ViewGroup. If the LayoutTransition object is
     * not null, changes in layout which occur because of children being added to or removed from
     * the ViewGroup will be animated according to the animations defined in that LayoutTransition
     * object. By default, the transition object is null (so layout changes are not animated).
     *
     * <p>Replacing a non-null transition will cause that previous transition to be
     * canceled, if it is currently running, to restore this container to
     * its correct post-transition state.</p>
     *
     * @param transition The LayoutTransition object that will animated changes in layout. A value
     *                   of <code>null</code> means no transition will run on layout changes.
     */
    public void setLayoutTransition(LayoutTransition transition) {
        if (mTransition != null) {
            LayoutTransition previousTransition = mTransition;
            previousTransition.cancel();
            previousTransition.removeTransitionListener(mLayoutTransitionListener);
        }
        mTransition = transition;
        if (mTransition != null) {
            mTransition.addTransitionListener(mLayoutTransitionListener);
        }
    }

    /**
     * Gets the LayoutTransition object for this ViewGroup. If the LayoutTransition object is
     * not null, changes in layout which occur because of children being added to or removed from
     * the ViewGroup will be animated according to the animations defined in that LayoutTransition
     * object. By default, the transition object is null (so layout changes are not animated).
     *
     * @return LayoutTranstion The LayoutTransition object that will animated changes in layout.
     * A value of <code>null</code> means no transition will run on layout changes.
     */
    public LayoutTransition getLayoutTransition() {
        return mTransition;
    }

    private void removeViewsInternal(int start, int count) {
        final int end = start + count;

        if (start < 0 || count < 0 || end > mChildrenCount) {
            throw new IndexOutOfBoundsException();
        }

        final View focused = mFocused;
        final boolean detach = mAttachInfo != null;
        boolean clearChildFocus = false;
        View clearDefaultFocus = null;

        final View[] children = mChildren;

        for (int i = start; i < end; i++) {
            final View view = children[i];

            if (mTransition != null) {
                mTransition.removeChild(this, view);
            }

            if (view == focused) {
                view.unFocus(null);
                clearChildFocus = true;
            }
            if (view == mDefaultFocus) {
                clearDefaultFocus = view;
            }
            if (view == mFocusedInCluster) {
                clearFocusedInCluster(view);
            }

            cancelTouchTarget(view);
            cancelHoverTarget(view);

            if (mTransitioningViews != null && mTransitioningViews.contains(view)) {
                addDisappearingView(view);
            } else if (detach) {
                view.dispatchDetachedFromWindow();
            }

            if (view.hasTransientState()) {
                childHasTransientStateChanged(view, false);
            }

            /*needGlobalAttributesUpdate(false);*/

            onViewRemoved(view);
        }

        removeFromArray(start, count);

        if (clearDefaultFocus != null) {
            clearDefaultFocus(clearDefaultFocus);
        }
        if (clearChildFocus) {
            clearChildFocus(focused);
            if (!rootViewRequestFocus()) {
                notifyGlobalFocusCleared(focused);
            }
        }
    }

    /**
     * Call this method to remove all child views from the
     * ViewGroup.
     *
     * <p><strong>Note:</strong> do not invoke this method from
     * {@link #draw(Canvas)}, {@link View#onDraw(icyllis.modernui.graphics.Canvas)},
     * {@link View#dispatchDraw(Canvas)} or any related method.</p>
     */
    public void removeAllViews() {
        removeAllViewsInLayout();
        requestLayout();
        invalidate();
    }

    /**
     * Called by a ViewGroup subclass to remove child views from itself,
     * when it must first know its size on screen before it can calculate how many
     * child views it will render. An example is a Gallery or a ListView, which
     * may "have" 50 children, but actually only render the number of children
     * that can currently fit inside the object on screen. Do not call
     * this method unless you are extending ViewGroup and understand the
     * view measuring and layout pipeline.
     *
     * <p><strong>Note:</strong> do not invoke this method from
     * {@link #draw(Canvas)}, {@link View#onDraw(icyllis.modernui.graphics.Canvas)},
     * {@link View#dispatchDraw(Canvas)} or any related method.</p>
     */
    public void removeAllViewsInLayout() {
        final int count = mChildrenCount;
        if (count <= 0) {
            return;
        }

        final View[] children = mChildren;
        mChildrenCount = 0;

        final View focused = mFocused;
        final boolean detach = mAttachInfo != null;
        boolean clearChildFocus = false;

        //needGlobalAttributesUpdate(false);

        for (int i = count - 1; i >= 0; i--) {
            final View view = children[i];

            if (mTransition != null) {
                mTransition.removeChild(this, view);
            }

            if (view == focused) {
                view.unFocus(null);
                clearChildFocus = true;
            }

            cancelTouchTarget(view);
            cancelHoverTarget(view);

            if ((mTransitioningViews != null && mTransitioningViews.contains(view))) {
                addDisappearingView(view);
            } else if (detach) {
                view.dispatchDetachedFromWindow();
            }

            if (view.hasTransientState()) {
                childHasTransientStateChanged(view, false);
            }

            onViewRemoved(view);

            view.mParent = null;
            children[i] = null;
        }

        if (mDefaultFocus != null) {
            clearDefaultFocus(mDefaultFocus);
        }
        if (mFocusedInCluster != null) {
            clearFocusedInCluster(mFocusedInCluster);
        }
        if (clearChildFocus) {
            clearChildFocus(focused);
            if (!rootViewRequestFocus()) {
                notifyGlobalFocusCleared(focused);
            }
        }
    }

    /**
     * Finishes the removal of a detached view. This method will dispatch the detached from
     * window event and notify the hierarchy change listener.
     * <p>
     * This method is intended to be lightweight and makes no assumptions about whether the
     * parent or child should be redrawn. Proper use of this method will include also making
     * any appropriate {@link #requestLayout()} or {@link #invalidate()} calls.
     * For example, callers can {@link #post(Runnable) post} a {@link Runnable}
     * which performs a {@link #requestLayout()} on the next frame, after all detach/remove
     * calls are finished, causing layout to be run prior to redrawing the view hierarchy.
     *
     * @param child   the child to be definitely removed from the view hierarchy
     * @param animate if true and the view has an animation, the view is placed in the
     *                disappearing views list, otherwise, it is detached from the window
     * @see #attachViewToParent(View, int, ViewGroup.LayoutParams)
     * @see #detachAllViewsFromParent()
     * @see #detachViewFromParent(View)
     * @see #detachViewFromParent(int)
     */
    protected void removeDetachedView(@NonNull View child, boolean animate) {
        if (mTransition != null) {
            mTransition.removeChild(this, child);
        }

        if (child == mFocused) {
            child.clearFocus();
        }
        if (child == mDefaultFocus) {
            clearDefaultFocus(child);
        }
        if (child == mFocusedInCluster) {
            clearFocusedInCluster(child);
        }

        cancelTouchTarget(child);
        cancelHoverTarget(child);

        if (mTransitioningViews != null && mTransitioningViews.contains(child)) {
            addDisappearingView(child);
        } else if (child.mAttachInfo != null) {
            child.dispatchDetachedFromWindow();
        }

        if (child.hasTransientState()) {
            childHasTransientStateChanged(child, false);
        }

        onViewRemoved(child);
    }

    /**
     * Attaches a view to this view group. Attaching a view assigns this group as the parent,
     * sets the layout parameters and puts the view in the list of children so that
     * it can be retrieved by calling {@link #getChildAt(int)}.
     * <p>
     * This method is intended to be lightweight and makes no assumptions about whether the
     * parent or child should be redrawn. Proper use of this method will include also making
     * any appropriate {@link #requestLayout()} or {@link #invalidate()} calls.
     * For example, callers can {@link #post(Runnable) post} a {@link Runnable}
     * which performs a {@link #requestLayout()} on the next frame, after all detach/attach
     * calls are finished, causing layout to be run prior to redrawing the view hierarchy.
     * <p>
     * This method should be called only for views which were detached from their parent.
     *
     * @param child  the child to attach
     * @param index  the index at which the child should be attached
     * @param params the layout parameters of the child
     * @see #removeDetachedView(View, boolean)
     * @see #detachAllViewsFromParent()
     * @see #detachViewFromParent(View)
     * @see #detachViewFromParent(int)
     */
    protected void attachViewToParent(View child, int index, LayoutParams params) {
        child.mLayoutParams = params;

        if (index < 0) {
            index = mChildrenCount;
        }

        addInArray(child, index);

        child.mParent = this;
        child.mPrivateFlags = (child.mPrivateFlags & ~PFLAG_DIRTY_MASK
                & ~PFLAG_DRAWING_CACHE_VALID)
                | PFLAG_DRAWN | PFLAG_INVALIDATED;
        mPrivateFlags |= PFLAG_INVALIDATED;

        if (child.hasFocus()) {
            requestChildFocus(child, child.findFocus());
        }
        dispatchVisibilityAggregated(isAttachedToWindow() && getWindowVisibility() == VISIBLE
                && isShown());
    }

    /**
     * Detaches a view from its parent. Detaching a view should be followed
     * either by a call to
     * {@link #attachViewToParent(View, int, ViewGroup.LayoutParams)}
     * or a call to {@link #removeDetachedView(View, boolean)}. Detachment should only be
     * temporary; reattachment or removal should happen within the same drawing cycle as
     * detachment. When a view is detached, its parent is null and cannot be retrieved by a
     * call to {@link #getChildAt(int)}.
     *
     * @param child the child to detach
     * @see #detachViewFromParent(int)
     * @see #detachViewsFromParent(int, int)
     * @see #detachAllViewsFromParent()
     * @see #attachViewToParent(View, int, ViewGroup.LayoutParams)
     * @see #removeDetachedView(View, boolean)
     */
    protected void detachViewFromParent(View child) {
        removeFromArray(indexOfChild(child));
    }

    /**
     * Detaches a view from its parent. Detaching a view should be followed
     * either by a call to
     * {@link #attachViewToParent(View, int, ViewGroup.LayoutParams)}
     * or a call to {@link #removeDetachedView(View, boolean)}. Detachment should only be
     * temporary; reattachment or removal should happen within the same drawing cycle as
     * detachment. When a view is detached, its parent is null and cannot be retrieved by a
     * call to {@link #getChildAt(int)}.
     *
     * @param index the index of the child to detach
     * @see #detachViewFromParent(View)
     * @see #detachAllViewsFromParent()
     * @see #detachViewsFromParent(int, int)
     * @see #attachViewToParent(View, int, ViewGroup.LayoutParams)
     * @see #removeDetachedView(View, boolean)
     */
    protected void detachViewFromParent(int index) {
        removeFromArray(index);
    }

    /**
     * Detaches a range of views from their parents. Detaching a view should be followed
     * either by a call to
     * {@link #attachViewToParent(View, int, ViewGroup.LayoutParams)}
     * or a call to {@link #removeDetachedView(View, boolean)}. Detachment should only be
     * temporary; reattachment or removal should happen within the same drawing cycle as
     * detachment. When a view is detached, its parent is null and cannot be retrieved by a
     * call to {@link #getChildAt(int)}.
     *
     * @param start the first index of the childrend range to detach
     * @param count the number of children to detach
     * @see #detachViewFromParent(View)
     * @see #detachViewFromParent(int)
     * @see #detachAllViewsFromParent()
     * @see #attachViewToParent(View, int, ViewGroup.LayoutParams)
     * @see #removeDetachedView(View, boolean)
     */
    protected void detachViewsFromParent(int start, int count) {
        removeFromArray(Math.max(0, start), Math.min(mChildrenCount, count));
    }

    /**
     * Detaches all views from the parent. Detaching a view should be followed
     * either by a call to
     * {@link #attachViewToParent(View, int, ViewGroup.LayoutParams)}
     * or a call to {@link #removeDetachedView(View, boolean)}. Detachment should only be
     * temporary; reattachment or removal should happen within the same drawing cycle as
     * detachment. When a view is detached, its parent is null and cannot be retrieved by a
     * call to {@link #getChildAt(int)}.
     *
     * @see #detachViewFromParent(View)
     * @see #detachViewFromParent(int)
     * @see #detachViewsFromParent(int, int)
     * @see #attachViewToParent(View, int, ViewGroup.LayoutParams)
     * @see #removeDetachedView(View, boolean)
     */
    protected void detachAllViewsFromParent() {
        final int count = mChildrenCount;
        if (count <= 0) {
            return;
        }

        final View[] children = mChildren;
        mChildrenCount = 0;

        for (int i = count - 1; i >= 0; i--) {
            children[i].mParent = null;
            children[i] = null;
        }
    }

    /**
     * Don't call or override this method. It is used for the implementation of
     * the view hierarchy.
     */
    @Override
    public final void invalidateChild(View child, Rect dirty) {
        final AttachInfo attachInfo = mAttachInfo;

        ViewParent parent = this;
        if (attachInfo != null) {
            // Check whether the child that requests the invalidate is fully opaque
            // Views being animated or transformed are not considered opaque because we may
            // be invalidating their old position and need the parent to paint behind them.
            Matrix childMatrix = child.getMatrix();
            // Mark the child as dirty, using the appropriate flag
            // Make sure we do not set both flags at the same time

            final int[] location = attachInfo.mInvalidateChildLocation;
            location[0] = child.mLeft;
            location[1] = child.mTop;
            if (!childMatrix.isIdentity()) {
                RectF boundingRect = attachInfo.mTmpTransformRect;
                boundingRect.set(dirty);
                Matrix transformMatrix;
                transformMatrix = childMatrix;
                transformMatrix.mapRect(boundingRect);
                dirty.set((int) Math.floor(boundingRect.left),
                        (int) Math.floor(boundingRect.top),
                        (int) Math.ceil(boundingRect.right),
                        (int) Math.ceil(boundingRect.bottom));
            }

            do {
                View view = null;
                if (parent instanceof View) {
                    view = (View) parent;
                }

                // If the parent is dirty opaque or not dirty, mark it dirty with the opaque
                // flag coming from the child that initiated the invalidate
                if (view != null) {
                    if ((view.mPrivateFlags & PFLAG_DIRTY_MASK) != PFLAG_DIRTY) {
                        view.mPrivateFlags = (view.mPrivateFlags & ~PFLAG_DIRTY_MASK) | PFLAG_DIRTY;
                    }
                }

                parent = parent.invalidateChildInParent(location, dirty);
                if (view != null) {
                    // Account for transform on current parent
                    Matrix m = view.getMatrix();
                    if (!m.isIdentity()) {
                        RectF boundingRect = attachInfo.mTmpTransformRect;
                        boundingRect.set(dirty);
                        m.mapRect(boundingRect);
                        dirty.set((int) Math.floor(boundingRect.left),
                                (int) Math.floor(boundingRect.top),
                                (int) Math.ceil(boundingRect.right),
                                (int) Math.ceil(boundingRect.bottom));
                    }
                }
            } while (parent != null);
        }
    }

    /**
     * Don't call or override this method. It is used for the implementation of
     * the view hierarchy.
     *
     * This implementation returns null if this ViewGroup does not have a parent,
     * if this ViewGroup is already fully invalidated or if the dirty rectangle
     * does not intersect with this ViewGroup's bounds.
     */
    @Override
    public ViewParent invalidateChildInParent(int[] location, Rect dirty) {
        if ((mPrivateFlags & (PFLAG_DRAWN | PFLAG_DRAWING_CACHE_VALID)) != 0) {
            dirty.offset(location[0] - mScrollX,
                    location[1] - mScrollY);
            if ((mGroupFlags & FLAG_CLIP_CHILDREN) == 0) {
                dirty.union(0, 0, mRight - mLeft, mBottom - mTop);
            }

            final int left = mLeft;
            final int top = mTop;

            if ((mGroupFlags & FLAG_CLIP_CHILDREN) == FLAG_CLIP_CHILDREN) {
                if (!dirty.intersect(0, 0, mRight - left, mBottom - top)) {
                    dirty.setEmpty();
                }
            }

            location[0] = left;
            location[1] = top;
            mPrivateFlags &= ~PFLAG_DRAWING_CACHE_VALID;

            return mParent;
        }

        return null;
    }

    /**
     * Gets the descendant focusability of this view group.  The descendant
     * focusability defines the relationship between this view group and its
     * descendants when looking for a view to take focus in
     * {@link #requestFocus(int)}.
     *
     * @return one of {@link #FOCUS_BEFORE_DESCENDANTS}, {@link #FOCUS_AFTER_DESCENDANTS},
     * {@link #FOCUS_BLOCK_DESCENDANTS}.
     */
    public int getDescendantFocusability() {
        return mGroupFlags & FLAG_MASK_FOCUSABILITY;
    }

    /**
     * Set the descendant focusability of this view group. This defines the relationship
     * between this view group and its descendants when looking for a view to
     * take focus in {@link #requestFocus(int)}.
     *
     * @param focusability one of {@link #FOCUS_BEFORE_DESCENDANTS}, {@link #FOCUS_AFTER_DESCENDANTS},
     *                     {@link #FOCUS_BLOCK_DESCENDANTS}.
     */
    public void setDescendantFocusability(int focusability) {
        switch (focusability) {
            case FOCUS_BEFORE_DESCENDANTS:
            case FOCUS_AFTER_DESCENDANTS:
            case FOCUS_BLOCK_DESCENDANTS:
                break;
            default:
                throw new IllegalArgumentException("must be one of FOCUS_BEFORE_DESCENDANTS, "
                        + "FOCUS_AFTER_DESCENDANTS, FOCUS_BLOCK_DESCENDANTS");
        }
        mGroupFlags &= ~FLAG_MASK_FOCUSABILITY;
        mGroupFlags |= (focusability & FLAG_MASK_FOCUSABILITY);
    }

    @Override
    void handleFocusGainInternal(int direction, @Nullable Rect previouslyFocusedRect) {
        if (mFocused != null) {
            mFocused.unFocus(this);
            mFocused = null;
            mFocusedInCluster = null;
        }
        super.handleFocusGainInternal(direction, previouslyFocusedRect);
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        if (getDescendantFocusability() == FOCUS_BLOCK_DESCENDANTS) {
            return;
        }

        // Unfocus us, if necessary
        super.unFocus(focused);

        // We had a previous notion of who had focus. Clear it.
        if (mFocused != child) {
            if (mFocused != null) {
                mFocused.unFocus(focused);
            }

            mFocused = child;
        }
        if (mParent != null) {
            mParent.requestChildFocus(this, focused);
        }
    }

    void setDefaultFocus(View child) {
        // Stop at any higher view which is explicitly focused-by-default
        if (mDefaultFocus != null && mDefaultFocus.isFocusedByDefault()) {
            return;
        }

        mDefaultFocus = child;

        if (mParent instanceof ViewGroup) {
            ((ViewGroup) mParent).setDefaultFocus(this);
        }
    }

    /**
     * Clears the default-focus chain from {@param child} up to the first parent which has another
     * default-focusable branch below it or until there is no default-focus chain.
     */
    void clearDefaultFocus(View child) {
        // Stop at any higher view which is explicitly focused-by-default
        if (mDefaultFocus != child && mDefaultFocus != null
                && mDefaultFocus.isFocusedByDefault()) {
            return;
        }

        mDefaultFocus = null;

        // Search child siblings for default focusables.
        for (int i = 0; i < mChildrenCount; ++i) {
            View sibling = mChildren[i];
            if (sibling.isFocusedByDefault()) {
                mDefaultFocus = sibling;
                return;
            } else if (mDefaultFocus == null && sibling.hasDefaultFocus()) {
                mDefaultFocus = sibling;
            }
        }

        if (mParent instanceof ViewGroup) {
            ((ViewGroup) mParent).clearDefaultFocus(this);
        }
    }

    @Override
    boolean hasDefaultFocus() {
        return mDefaultFocus != null || super.hasDefaultFocus();
    }

    /**
     * Removes {@code child} (and associated focusedInCluster chain) from the cluster containing
     * it.
     * <br>
     * This is intended to be run on {@code child}'s immediate parent. This is necessary because
     * the chain is sometimes cleared after {@code child} has been detached.
     */
    void clearFocusedInCluster(View child) {
        if (mFocusedInCluster != child) {
            return;
        }
        clearFocusedInCluster();
    }

    /**
     * Removes the focusedInCluster chain from this up to the cluster containing it.
     */
    void clearFocusedInCluster() {
        View top = findKeyboardNavigationCluster();
        ViewParent parent = this;
        do {
            ((ViewGroup) parent).mFocusedInCluster = null;
            if (parent == top) {
                break;
            }
            parent = parent.getParent();
        } while (parent instanceof ViewGroup);
    }

    @Override
    public void dispatchWindowFocusChanged(boolean hasFocus) {
        super.dispatchWindowFocusChanged(hasFocus);
        final int count = mChildrenCount;
        final View[] children = mChildren;
        for (int i = 0; i < count; i++) {
            children[i].dispatchWindowFocusChanged(hasFocus);
        }
    }

    @Override
    public void addFocusables(@NonNull ArrayList<View> views, int direction, int focusableMode) {
        final int focusableCount = views.size();

        final int descendantFocusability = getDescendantFocusability();

        if (descendantFocusability == FOCUS_BLOCK_DESCENDANTS) {
            super.addFocusables(views, direction, focusableMode);
            return;
        }

        if (descendantFocusability == FOCUS_BEFORE_DESCENDANTS) {
            super.addFocusables(views, direction, focusableMode);
        }

        int count = 0;
        final View[] children = new View[mChildrenCount];
        for (int i = 0; i < mChildrenCount; ++i) {
            View child = mChildren[i];
            if ((child.mViewFlags & VISIBILITY_MASK) == VISIBLE) {
                children[count++] = child;
            }
        }
        FocusFinder.sort(children, 0, count, this, isLayoutRtl());
        for (int i = 0; i < count; ++i) {
            children[i].addFocusables(views, direction, focusableMode);
        }

        // When set to FOCUS_AFTER_DESCENDANTS, we only add ourselves if
        // there aren't any focusable descendants.  this is
        // to avoid the focus search finding layouts when a more precise search
        // among the focusable children would be more interesting.
        if (descendantFocusability == FOCUS_AFTER_DESCENDANTS && focusableCount == views.size()) {
            super.addFocusables(views, direction, focusableMode);
        }
    }

    @Override
    public void addTouchables(@NonNull ArrayList<View> views) {
        super.addTouchables(views);

        final int count = mChildrenCount;
        final View[] children = mChildren;

        for (int i = 0; i < count; i++) {
            final View child = children[i];
            if ((child.mViewFlags & VISIBILITY_MASK) == VISIBLE) {
                child.addTouchables(views);
            }
        }
    }

    @Override
    public void addKeyboardNavigationClusters(@NonNull Collection<View> views, int direction) {
        final int focusableCount = views.size();

        if (isKeyboardNavigationCluster()) {
            // Cluster-navigation can enter a touchscreenBlocksFocus cluster, so temporarily
            // disable touchscreenBlocksFocus to evaluate whether it contains focusables.
            final boolean blockedFocus = getTouchscreenBlocksFocus();
            try {
                setTouchscreenBlocksFocusNoRefocus(false);
                super.addKeyboardNavigationClusters(views, direction);
            } finally {
                setTouchscreenBlocksFocusNoRefocus(blockedFocus);
            }
        } else {
            super.addKeyboardNavigationClusters(views, direction);
        }

        if (focusableCount != views.size()) {
            // No need to look for groups inside a group.
            return;
        }

        if (getDescendantFocusability() == FOCUS_BLOCK_DESCENDANTS) {
            return;
        }

        int count = 0;
        final View[] visibleChildren = new View[mChildrenCount];
        for (int i = 0; i < mChildrenCount; ++i) {
            final View child = mChildren[i];
            if ((child.mViewFlags & VISIBILITY_MASK) == VISIBLE) {
                visibleChildren[count++] = child;
            }
        }
        FocusFinder.sort(visibleChildren, 0, count, this, isLayoutRtl());
        for (int i = 0; i < count; ++i) {
            visibleChildren[i].addKeyboardNavigationClusters(views, direction);
        }
    }

    /**
     * Set whether this ViewGroup should ignore focus requests for itself and its children.
     * If this option is enabled and the ViewGroup or a descendant currently has focus, focus
     * will proceed forward.
     *
     * @param touchscreenBlocksFocus true to enable blocking focus in the presence of a touchscreen
     */
    public void setTouchscreenBlocksFocus(boolean touchscreenBlocksFocus) {
        if (touchscreenBlocksFocus) {
            mGroupFlags |= FLAG_TOUCHSCREEN_BLOCKS_FOCUS;
            if (hasFocus() && !isKeyboardNavigationCluster()) {
                final View focusedChild = getDeepestFocusedChild();
                if (!focusedChild.isFocusableInTouchMode()) {
                    final View newFocus = focusSearch(FOCUS_FORWARD);
                    if (newFocus != null) {
                        newFocus.requestFocus();
                    }
                }
            }
        } else {
            mGroupFlags &= ~FLAG_TOUCHSCREEN_BLOCKS_FOCUS;
        }
    }

    private void setTouchscreenBlocksFocusNoRefocus(boolean touchscreenBlocksFocus) {
        if (touchscreenBlocksFocus) {
            mGroupFlags |= FLAG_TOUCHSCREEN_BLOCKS_FOCUS;
        } else {
            mGroupFlags &= ~FLAG_TOUCHSCREEN_BLOCKS_FOCUS;
        }
    }

    /**
     * Check whether this ViewGroup should ignore focus requests for itself and its children.
     */
    public boolean getTouchscreenBlocksFocus() {
        return (mGroupFlags & FLAG_TOUCHSCREEN_BLOCKS_FOCUS) != 0;
    }

    boolean shouldBlockFocusForTouchscreen() {
        // There is a special case for keyboard-navigation clusters. We allow cluster navigation
        // to jump into blockFocusForTouchscreen ViewGroups which are clusters. Once in the
        // cluster, focus is free to move around within it.
        return getTouchscreenBlocksFocus()
                && !(isKeyboardNavigationCluster()
                && (hasFocus() || (findKeyboardNavigationCluster() != this)));
    }

    /**
     * Called when a child view has changed whether it is tracking transient state.
     */
    @Override
    public void childHasTransientStateChanged(View child, boolean childHasTransientState) {
        final boolean oldHasTransientState = hasTransientState();
        if (childHasTransientState) {
            mChildCountWithTransientState++;
        } else {
            mChildCountWithTransientState--;
        }

        final boolean newHasTransientState = hasTransientState();
        if (mParent != null && oldHasTransientState != newHasTransientState) {
            mParent.childHasTransientStateChanged(this, newHasTransientState);
        }
    }

    @Override
    public boolean hasTransientState() {
        return mChildCountWithTransientState > 0 || super.hasTransientState();
    }

    @Override
    public void clearChildFocus(View child) {
        mFocused = null;
        if (mParent != null) {
            mParent.clearChildFocus(this);
        }
    }

    @Override
    public void clearFocus() {
        if (mFocused == null) {
            super.clearFocus();
        } else {
            View focused = mFocused;
            mFocused = null;
            focused.clearFocus();
        }
    }

    @Override
    void unFocus(View focused) {
        if (mFocused == null) {
            super.unFocus(focused);
        } else {
            mFocused.unFocus(focused);
            mFocused = null;
        }
    }

    @Override
    boolean hasFocusable(boolean allowAutoFocus, boolean dispatchExplicit) {
        // This should probably be super.hasFocusable, but that would change
        // behavior. Historically, we have not checked the ancestor views for
        // shouldBlockFocusForTouchscreen() in ViewGroup.hasFocusable.

        // Invisible and gone views are never focusable.
        if ((mViewFlags & VISIBILITY_MASK) != VISIBLE) {
            return false;
        }

        // Only use effective focusable value when allowed.
        if ((allowAutoFocus || getFocusable() != FOCUSABLE_AUTO) && isFocusable()) {
            return true;
        }

        // Determine whether we have a focused descendant.
        final int descendantFocusability = getDescendantFocusability();
        if (descendantFocusability != FOCUS_BLOCK_DESCENDANTS) {
            return hasFocusableChild(dispatchExplicit);
        }

        return false;
    }

    boolean hasFocusableChild(boolean dispatchExplicit) {
        // Determine whether we have a focusable descendant.
        final int count = mChildrenCount;
        final View[] children = mChildren;

        for (int i = 0; i < count; i++) {
            final View child = children[i];

            // In case the subclass has overridden has[Explicit]Focusable, dispatch
            // to the expected one for each child even though we share logic here.
            if ((dispatchExplicit && child.hasExplicitFocusable())
                    || (!dispatchExplicit && child.hasFocusable())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the focused child of this view, if any. The child may have focus
     * or contain focus.
     *
     * @return the focused child or null.
     */
    public View getFocusedChild() {
        return mFocused;
    }

    View getDeepestFocusedChild() {
        View v = this;
        while (v != null) {
            if (v.isFocused()) {
                return v;
            }
            v = v instanceof ViewGroup ? ((ViewGroup) v).getFocusedChild() : null;
        }
        return null;
    }

    /**
     * Returns true if this view has or contains focus
     *
     * @return true if this view has or contains focus
     */
    @Override
    public boolean hasFocus() {
        return (mPrivateFlags & PFLAG_FOCUSED) != 0 || mFocused != null;
    }

    @Nullable
    @Override
    public View findFocus() {
        if (isFocused()) {
            return this;
        }

        if (mFocused != null) {
            return mFocused.findFocus();
        }
        return null;
    }

    /**
     * Offset a rectangle that is in a descendant's coordinate
     * space into our coordinate space.
     *
     * @param descendant A descendant of this view
     * @param rect       A rectangle defined in descendant's coordinate space.
     */
    public final void offsetDescendantRectToMyCoords(View descendant, Rect rect) {
        offsetRectBetweenParentAndChild(descendant, rect, true, false);
    }

    /**
     * Offset a rectangle that is in our coordinate space into an ancestor's
     * coordinate space.
     *
     * @param descendant A descendant of this view
     * @param rect       A rectangle defined in descendant's coordinate space.
     */
    public final void offsetRectIntoDescendantCoords(View descendant, Rect rect) {
        offsetRectBetweenParentAndChild(descendant, rect, false, false);
    }

    /**
     * Helper method that offsets a rect either from parent to descendant or
     * descendant to parent.
     */
    void offsetRectBetweenParentAndChild(View descendant, Rect rect,
                                         boolean offsetFromChildToParent, boolean clipToBounds) {

        // already in the same coord system :)
        if (descendant == this) {
            return;
        }

        ViewParent theParent = descendant.mParent;

        // search and offset up to the parent
        while (theParent instanceof View && theParent != this) {

            if (offsetFromChildToParent) {
                rect.offset(descendant.mLeft - descendant.mScrollX,
                        descendant.mTop - descendant.mScrollY);
                if (clipToBounds) {
                    View p = (View) theParent;
                    boolean intersected = rect.intersect(0, 0, p.mRight - p.mLeft,
                            p.mBottom - p.mTop);
                    if (!intersected) {
                        rect.setEmpty();
                    }
                }
            } else {
                if (clipToBounds) {
                    View p = (View) theParent;
                    boolean intersected = rect.intersect(0, 0, p.mRight - p.mLeft,
                            p.mBottom - p.mTop);
                    if (!intersected) {
                        rect.setEmpty();
                    }
                }
                rect.offset(descendant.mScrollX - descendant.mLeft,
                        descendant.mScrollY - descendant.mTop);
            }

            descendant = (View) theParent;
            theParent = descendant.mParent;
        }

        // now that we are up to this view, need to offset one more time
        // to get into our coordinate space
        if (theParent == this) {
            if (offsetFromChildToParent) {
                rect.offset(descendant.mLeft - descendant.mScrollX,
                        descendant.mTop - descendant.mScrollY);
            } else {
                rect.offset(descendant.mScrollX - descendant.mLeft,
                        descendant.mScrollY - descendant.mTop);
            }
        } else {
            throw new IllegalArgumentException("parameter must be a descendant of this view");
        }
    }

    /**
     * Offset the vertical location of all children of this view by the specified number of pixels.
     *
     * @param offset the number of pixels to offset
     */
    public void offsetChildrenTopAndBottom(int offset) {
        final int count = mChildrenCount;
        final View[] children = mChildren;
        for (int i = 0; i < count; i++) {
            children[i].offsetTopAndBottom(offset);
        }
    }

    @Override
    public boolean getChildVisibleRect(View child, Rect r, @Nullable Point offset) {
        return getChildVisibleRect(child, r, offset, false);
    }

    /**
     * @param forceParentCheck true to guarantee that this call will propagate to all ancestors,
     *                         false otherwise
     */
    @ApiStatus.Internal
    public boolean getChildVisibleRect(@NonNull View child, Rect r, @Nullable Point offset, boolean forceParentCheck) {
        // It doesn't make a whole lot of sense to call this on a view that isn't attached,
        // but for some simple tests it can be useful. If we don't have attach info this
        // will allocate memory.
        final RectF rect = mAttachInfo != null ? mAttachInfo.mTmpTransformRect : new RectF();
        rect.set(r);

        if (!child.hasIdentityMatrix()) {
            child.getMatrix().mapRect(rect);
        }

        final int dx = child.mLeft - mScrollX;
        final int dy = child.mTop - mScrollY;

        rect.offset(dx, dy);

        if (offset != null) {
            if (!child.hasIdentityMatrix()) {
                float[] position = mAttachInfo != null ? mAttachInfo.mTmpTransformLocation
                        : new float[2];
                position[0] = offset.x;
                position[1] = offset.y;
                child.getMatrix().mapPoint(position);
                offset.x = Math.round(position[0]);
                offset.y = Math.round(position[1]);
            }
            offset.x += dx;
            offset.y += dy;
        }

        final int width = mRight - mLeft;
        final int height = mBottom - mTop;

        boolean rectIsVisible = true;
        if (mParent == null ||
                (mParent instanceof ViewGroup && ((ViewGroup) mParent).getClipChildren())) {
            // Clip to bounds.
            rectIsVisible = rect.intersect(0, 0, width, height);
        }

        if ((forceParentCheck || rectIsVisible)
                && (mGroupFlags & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK) {
            // Clip to padding.
            rectIsVisible = rect.intersect(mPaddingLeft, mPaddingTop,
                    width - mPaddingRight, height - mPaddingBottom);
        }

        r.set((int) Math.floor(rect.left), (int) Math.floor(rect.top),
                (int) Math.ceil(rect.right), (int) Math.ceil(rect.bottom));

        if ((forceParentCheck || rectIsVisible) && mParent != null) {
            if (mParent instanceof ViewGroup) {
                rectIsVisible = ((ViewGroup) mParent)
                        .getChildVisibleRect(this, r, offset, forceParentCheck);
            } else {
                rectIsVisible = mParent.getChildVisibleRect(this, r, offset);
            }
        }
        return rectIsVisible;
    }

    /**
     * Find the nearest view in the specified direction that wants to take
     * focus.
     *
     * @param focused   The view that currently has focus
     * @param direction One of FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, and
     *                  FOCUS_RIGHT, or 0 for not applicable.
     */
    @Override
    public View focusSearch(View focused, int direction) {
        if (isRootNamespace()) {
            // root namespace means we should consider ourselves the top of the
            // tree for focus searching; otherwise we could be focus searching
            // into other tabs.  see LocalActivityManager and TabHost for more info.
            return FocusFinder.getInstance().findNextFocus(this, focused, direction);
        } else if (mParent != null) {
            return mParent.focusSearch(focused, direction);
        }
        return null;
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        return false;
    }

    @Override
    public void focusableViewAvailable(View v) {
        if (mParent != null
                // shortcut: don't report a new focusable view if we block our descendants from
                // getting focus or if we're not visible
                && (getDescendantFocusability() != FOCUS_BLOCK_DESCENDANTS)
                && ((mViewFlags & VISIBILITY_MASK) == VISIBLE)
                // shortcut: don't report a new focusable view if we already are focused
                // (and we don't prefer our descendants)
                //
                // note: knowing that mFocused is non-null is not a good enough reason
                // to break the traversal since in that case we'd actually have to find
                // the focused view and make sure it wasn't FOCUS_AFTER_DESCENDANTS and
                // an ancestor of v; this will get checked for at ViewAncestor
                && !(isFocused() && getDescendantFocusability() != FOCUS_AFTER_DESCENDANTS)) {
            mParent.focusableViewAvailable(v);
        }
    }

    @Override
    public boolean showContextMenuForChild(View originalView, float x, float y) {
        return mParent != null && mParent.showContextMenuForChild(originalView, x, y);
    }

    @Override
    public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback, int type) {
        if (mParent != null) {
            return mParent.startActionModeForChild(originalView, callback, type);
        }
        return null;
    }


    /**
     * Sets whether this ViewGroup will clip its children to its padding and resize (but not
     * clip) any EdgeEffect to the padded region, if padding is present.
     * <p>
     * By default, children are clipped to the padding of their parent
     * ViewGroup. This clipping behavior is only enabled if padding is non-zero.
     *
     * @param clipToPadding true to clip children to the padding of the group, and resize (but
     *                      not clip) any EdgeEffect to the padded region. False otherwise.
     */
    public void setClipToPadding(boolean clipToPadding) {
        if (hasBooleanFlag(FLAG_CLIP_TO_PADDING) != clipToPadding) {
            setBooleanFlag(FLAG_CLIP_TO_PADDING, clipToPadding);
            invalidate();
        }
    }

    /**
     * Returns whether this ViewGroup will clip its children to its padding, and resize (but
     * not clip) any EdgeEffect to the padded region, if padding is present.
     * <p>
     * By default, children are clipped to the padding of their parent
     * Viewgroup. This clipping behavior is only enabled if padding is non-zero.
     *
     * @return true if this ViewGroup clips children to its padding and resizes (but doesn't
     * clip) any EdgeEffect to the padded region, false otherwise.
     */
    public boolean getClipToPadding() {
        return hasBooleanFlag(FLAG_CLIP_TO_PADDING);
    }

    @Override
    public void dispatchSetSelected(boolean selected) {
        final View[] children = mChildren;
        final int count = mChildrenCount;
        for (int i = 0; i < count; i++) {
            children[i].setSelected(selected);
        }
    }

    @Override
    public void dispatchSetActivated(boolean activated) {
        final View[] children = mChildren;
        final int count = mChildrenCount;
        for (int i = 0; i < count; i++) {
            children[i].setActivated(activated);
        }
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        final View[] children = mChildren;
        final int count = mChildrenCount;
        for (int i = 0; i < count; i++) {
            final View child = children[i];
            // Children that are clickable on their own should not
            // show a pressed state when their parent view does.
            // Clearing a pressed state always propagates.
            if (!pressed || (!child.isClickable() && !child.isLongClickable())) {
                child.setPressed(pressed);
            }
        }
    }

    /**
     * Dispatches drawable hotspot changes to child views that meet at least
     * one of the following criteria:
     * <ul>
     *     <li>Returns {@code false} from both {@link View#isClickable()} and
     *     {@link View#isLongClickable()}</li>
     *     <li>Requests duplication of parent state via
     *     {@link View#setDuplicateParentStateEnabled(boolean)}</li>
     * </ul>
     *
     * @param x hotspot x coordinate
     * @param y hotspot y coordinate
     * @see #drawableHotspotChanged(float, float)
     */
    @Override
    public void dispatchDrawableHotspotChanged(float x, float y) {
        final int count = mChildrenCount;
        if (count == 0) {
            return;
        }

        final View[] children = mChildren;
        for (int i = 0; i < count; i++) {
            final View child = children[i];
            // Children that are clickable on their own should not
            // receive hotspots when their parent view does.
            final boolean nonActionable = !child.isClickable() && !child.isLongClickable();
            final boolean duplicatesState = (child.mViewFlags & DUPLICATE_PARENT_STATE) != 0;
            if (nonActionable || duplicatesState) {
                final float[] point = getTempLocationF();
                point[0] = x;
                point[1] = y;
                transformPointToViewLocal(point, child);
                child.drawableHotspotChanged(point[0], point[1]);
            }
        }
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    protected <T extends View> T findViewByPredicateTraversal(@NonNull Predicate<View> predicate,
                                                              @Nullable View childToSkip) {
        if (predicate.test(this)) {
            return (T) this;
        }

        final View[] where = mChildren;
        final int len = mChildrenCount;

        for (int i = 0; i < len; i++) {
            View v = where[i];

            if (v != childToSkip && (v.mPrivateFlags & PFLAG_IS_ROOT_NAMESPACE) == 0) {
                v = v.findViewByPredicate(predicate);

                if (v != null) {
                    return (T) v;
                }
            }
        }

        return null;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    protected <T extends View> T findViewTraversal(int id) {
        if (id == mID) {
            return (T) this;
        }

        final View[] views = mChildren;
        final int count = mChildrenCount;

        View v;
        for (int i = 0; i < count; i++) {
            v = views[i].findViewTraversal(id);

            if (v != null) {
                return (T) v;
            }
        }

        return null;
    }

    @Override
    public void bringChildToFront(View child) {
        final int index = indexOfChild(child);
        if (index >= 0) {
            removeFromArray(index);
            addInArray(child, mChildrenCount);
            child.mParent = this;
            requestLayout();
            invalidate();
        }
    }

    protected boolean hasBooleanFlag(int flag) {
        return (mGroupFlags & flag) == flag;
    }

    protected void setBooleanFlag(int flag, boolean value) {
        if (value) {
            mGroupFlags |= flag;
        } else {
            mGroupFlags &= ~flag;
        }
    }

    /**
     * Indicates whether the ViewGroup is drawing its children in the order defined by
     * {@link #getChildDrawingOrder(int, int)}.
     *
     * @return true if children drawing order is defined by {@link #getChildDrawingOrder(int, int)},
     * false otherwise
     * @see #setChildrenDrawingOrderEnabled(boolean)
     * @see #getChildDrawingOrder(int, int)
     */
    protected boolean isChildrenDrawingOrderEnabled() {
        return (mGroupFlags & FLAG_USE_CHILD_DRAWING_ORDER) == FLAG_USE_CHILD_DRAWING_ORDER;
    }

    /**
     * Tells the ViewGroup whether to draw its children in the order defined by the method
     * {@link #getChildDrawingOrder(int, int)}.
     * <p>
     * Note that View#getZ() reordering, done by {@link View#dispatchDraw(Canvas)},
     * will override custom child ordering done via this method.
     *
     * @param enabled true if the order of the children when drawing is determined by
     *                {@link #getChildDrawingOrder(int, int)}, false otherwise
     * @see #isChildrenDrawingOrderEnabled()
     * @see #getChildDrawingOrder(int, int)
     */
    protected void setChildrenDrawingOrderEnabled(boolean enabled) {
        setBooleanFlag(FLAG_USE_CHILD_DRAWING_ORDER, enabled);
    }

    /**
     * Converts drawing order position to container position. Override this
     * if you want to change the drawing order of children. By default, it
     * returns drawingPosition.
     * <p>
     * NOTE: In order for this method to be called, you must enable child ordering
     * first by calling {@link #setChildrenDrawingOrderEnabled(boolean)}.
     *
     * @param drawingPosition the drawing order position.
     * @return the container position of a child for this drawing order position.
     * @see #setChildrenDrawingOrderEnabled(boolean)
     * @see #isChildrenDrawingOrderEnabled()
     */
    protected int getChildDrawingOrder(int childCount, int drawingPosition) {
        return drawingPosition;
    }

    /**
     * Converts drawing order position to container position.
     * <p>
     * Children are not necessarily drawn in the order in which they appear in the container.
     * ViewGroups can enable a custom ordering via {@link #setChildrenDrawingOrderEnabled(boolean)}.
     * This method returns the container position of a child that appears in the given position
     * in the current drawing order.
     *
     * @param drawingPosition the drawing order position.
     * @return the container position of a child for this drawing order position.
     * @see #getChildDrawingOrder(int, int)}
     */
    public final int getChildDrawingOrder(int drawingPosition) {
        return getChildDrawingOrder(getChildCount(), drawingPosition);
    }

    private boolean hasChildWithZ() {
        for (int i = 0; i < mChildrenCount; i++) {
            if (mChildren[i].getZ() != 0) return true;
        }
        return false;
    }

    /**
     * Populates (and returns) mPreSortedChildren with a pre-ordered list of the View's children,
     * sorted first by Z, then by child drawing order (if applicable). This list must be cleared
     * after use to avoid leaking child Views.
     * <p>
     * Uses a stable, insertion sort which is commonly O(n) for ViewGroups with very few elevated
     * children.
     */
    @Nullable
    ArrayList<View> buildOrderedChildList() {
        final int childrenCount = mChildrenCount;
        if (childrenCount <= 1 || !hasChildWithZ()) return null;

        if (mPreSortedChildren == null) {
            mPreSortedChildren = new ArrayList<>(childrenCount);
        } else {
            // callers should clear, so clear shouldn't be necessary, but for safety...
            mPreSortedChildren.clear();
            mPreSortedChildren.ensureCapacity(childrenCount);
        }

        final boolean customOrder = isChildrenDrawingOrderEnabled();
        for (int i = 0; i < childrenCount; i++) {
            // add next child (in child order) to end of list
            final int childIndex = getAndVerifyPreorderedIndex(childrenCount, i, customOrder);
            final View nextChild = mChildren[childIndex];
            final float currentZ = nextChild.getZ();

            // insert ahead of any Views with greater Z
            int insertIndex = i;
            while (insertIndex > 0 && mPreSortedChildren.get(insertIndex - 1).getZ() > currentZ) {
                insertIndex--;
            }
            mPreSortedChildren.add(insertIndex, nextChild);
        }
        return mPreSortedChildren;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if ((mGroupFlags & FLAG_NOTIFY_CHILDREN_ON_DRAWABLE_STATE_CHANGE) != 0) {
            if ((mGroupFlags & FLAG_ADD_STATES_FROM_CHILDREN) != 0) {
                throw new IllegalStateException("addStateFromChildren cannot be enabled if a"
                        + " child has duplicateParentState set to true");
            }

            final View[] children = mChildren;
            final int count = mChildrenCount;

            for (int i = 0; i < count; i++) {
                final View child = children[i];
                if ((child.mViewFlags & DUPLICATE_PARENT_STATE) != 0) {
                    child.refreshDrawableState();
                }
            }
        }
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        final View[] children = mChildren;
        final int count = mChildrenCount;
        for (int i = 0; i < count; i++) {
            children[i].jumpDrawablesToCurrentState();
        }
    }

    @NonNull
    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        if ((mGroupFlags & FLAG_ADD_STATES_FROM_CHILDREN) == 0) {
            return super.onCreateDrawableState(extraSpace);
        }

        int need = 0;
        int n = getChildCount();
        for (int i = 0; i < n; i++) {
            int[] childState = getChildAt(i).getDrawableState();

            if (childState != null) {
                need += childState.length;
            }
        }

        int[] state = super.onCreateDrawableState(extraSpace + need);

        for (int i = 0; i < n; i++) {
            int[] childState = getChildAt(i).getDrawableState();

            if (childState != null) {
                mergeDrawableStates(state, childState);
            }
        }

        return state;
    }

    /**
     * Sets whether this ViewGroup's drawable states also include
     * its children's drawable states.  This is used, for example, to
     * make a group appear to be focused when its child EditText or button
     * is focused.
     */
    public void setAddStatesFromChildren(boolean addsStates) {
        if (addsStates) {
            mGroupFlags |= FLAG_ADD_STATES_FROM_CHILDREN;
        } else {
            mGroupFlags &= ~FLAG_ADD_STATES_FROM_CHILDREN;
        }

        refreshDrawableState();
    }

    /**
     * Returns whether this ViewGroup's drawable states also include
     * its children's drawable states.  This is used, for example, to
     * make a group appear to be focused when its child EditText or button
     * is focused.
     */
    public boolean addStatesFromChildren() {
        return (mGroupFlags & FLAG_ADD_STATES_FROM_CHILDREN) != 0;
    }

    /**
     * If {@link #addStatesFromChildren} is true, refreshes this group's
     * drawable state (to include the states from its children).
     */
    @Override
    public void childDrawableStateChanged(View child) {
        if ((mGroupFlags & FLAG_ADD_STATES_FROM_CHILDREN) != 0) {
            refreshDrawableState();
        }
    }

    /**
     * This method is called by LayoutTransition when there are 'changing' animations that need
     * to start after the layout/setup phase. The request is forwarded to the ViewAncestor, who
     * starts all pending transitions prior to the drawing phase in the current traversal.
     *
     * @param transition The LayoutTransition to be started on the next traversal.
     */
    @ApiStatus.Internal
    public void requestTransitionStart(LayoutTransition transition) {
        ViewRoot viewAncestor = mAttachInfo != null ? mAttachInfo.mViewRoot : null;
        if (viewAncestor != null) {
            viewAncestor.requestTransitionStart(transition);
        }
    }

    @ApiStatus.Internal
    @Override
    public boolean resolveRtlPropertiesIfNeeded() {
        final boolean result = super.resolveRtlPropertiesIfNeeded();
        // We don't need to resolve the children RTL properties if nothing has changed for the parent
        if (result) {
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (child.isLayoutDirectionInherited()) {
                    child.resolveRtlPropertiesIfNeeded();
                }
            }
        }
        return result;
    }

    @ApiStatus.Internal
    @Override
    public boolean resolveLayoutDirection() {
        final boolean result = super.resolveLayoutDirection();
        if (result) {
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (child.isLayoutDirectionInherited()) {
                    child.resolveLayoutDirection();
                }
            }
        }
        return result;
    }

    @ApiStatus.Internal
    @Override
    public boolean resolveTextDirection() {
        final boolean result = super.resolveTextDirection();
        if (result) {
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (child.isTextDirectionInherited()) {
                    child.resolveTextDirection();
                }
            }
        }
        return result;
    }

    @ApiStatus.Internal
    @Override
    public boolean resolveTextAlignment() {
        final boolean result = super.resolveTextAlignment();
        if (result) {
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (child.isTextAlignmentInherited()) {
                    child.resolveTextAlignment();
                }
            }
        }
        return result;
    }

    @ApiStatus.Internal
    @Override
    public void resolvePadding() {
        super.resolvePadding();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.isLayoutDirectionInherited() && !child.isPaddingResolved()) {
                child.resolvePadding();
            }
        }
    }

    @ApiStatus.Internal
    @Override
    protected void resolveDrawables() {
        super.resolveDrawables();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.isLayoutDirectionInherited() && !child.areDrawablesResolved()) {
                child.resolveDrawables();
            }
        }
    }

    @ApiStatus.Internal
    @Override
    public void resolveLayoutParams() {
        super.resolveLayoutParams();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            child.resolveLayoutParams();
        }
    }

    @ApiStatus.Internal
    @Override
    void resetResolvedLayoutDirection() {
        super.resetResolvedLayoutDirection();

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.isLayoutDirectionInherited()) {
                child.resetResolvedLayoutDirection();
            }
        }
    }

    @ApiStatus.Internal
    @Override
    void resetResolvedTextDirection() {
        super.resetResolvedTextDirection();

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.isTextDirectionInherited()) {
                child.resetResolvedTextDirection();
            }
        }
    }

    @ApiStatus.Internal
    @Override
    void resetResolvedTextAlignment() {
        super.resetResolvedTextAlignment();

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.isTextAlignmentInherited()) {
                child.resetResolvedTextAlignment();
            }
        }
    }

    @ApiStatus.Internal
    @Override
    void resetResolvedPadding() {
        super.resetResolvedPadding();

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.isLayoutDirectionInherited()) {
                child.resetResolvedPadding();
            }
        }
    }

    @ApiStatus.Internal
    @Override
    protected void resetResolvedDrawables() {
        super.resetResolvedDrawables();

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.isLayoutDirectionInherited()) {
                child.resetResolvedDrawables();
            }
        }
    }

    /**
     * Returns whether this group's children are clipped to their bounds before drawing.
     * The default value is true.
     *
     * @return True if the group's children will be clipped to their bounds,
     * false otherwise.
     * @see #setClipChildren(boolean)
     */
    public boolean getClipChildren() {
        return ((mGroupFlags & FLAG_CLIP_CHILDREN) != 0);
    }

    /**
     * By default, children are clipped to their bounds before drawing. This
     * allows view groups to override this behavior for animations, etc.
     *
     * @param clipChildren true to clip children to their bounds,
     *                     false otherwise
     */
    public void setClipChildren(boolean clipChildren) {
        boolean previousValue = (mGroupFlags & FLAG_CLIP_CHILDREN) == FLAG_CLIP_CHILDREN;
        if (clipChildren != previousValue) {
            setBooleanFlag(FLAG_CLIP_CHILDREN, clipChildren);
            invalidate();
        }
    }

    @Override
    void dispatchCancelPendingInputEvents() {
        super.dispatchCancelPendingInputEvents();

        final View[] children = mChildren;
        final int count = mChildrenCount;
        for (int i = 0; i < count; i++) {
            children[i].dispatchCancelPendingInputEvents();
        }
    }

    /*@Override
    final boolean onCursorPosEvent(LinkedList<View> route, double x, double y) {
        if (x >= mLeft && x < mRight && y >= mTop && y < mBottom) {
            if ((mViewFlags & ENABLED_MASK) == ENABLED) {
                route.add(this);
            }
            x += getScrollX();
            y += getScrollY();
            if (x >= mLeft && x < mRight && y >= mTop && y < mBottom) {
                for (int i = mChildrenCount - 1; i >= 0; i--) {
                    if (mChildren[i].onCursorPosEvent(route, x, y)) {
                        break;
                    }
                }
            }
            return true;
        }
        return false;
    }*/

    /**
     * Ask all the children of this view to measure themselves, taking into
     * account both the MeasureSpec requirements for this view and its padding.
     * We skip children that are in the GONE state The heavy lifting is done in
     * getChildMeasureSpec.
     *
     * @param widthMeasureSpec  The width requirements for this view
     * @param heightMeasureSpec The height requirements for this view
     */
    protected void measureChildren(int widthMeasureSpec, int heightMeasureSpec) {
        final int size = mChildrenCount;
        final View[] children = this.mChildren;
        for (int i = 0; i < size; i++) {
            View child = children[i];
            if (child.getVisibility() != GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
            }
        }
    }

    /**
     * Ask one of the children of this view to measure itself, taking into
     * account both the MeasureSpec requirements for this view and its padding.
     * The heavy lifting is done in getChildMeasureSpec.
     *
     * @param child            The child to measure
     * @param parentWidthSpec  The width requirements for this view
     * @param parentHeightSpec The height requirements for this view
     */
    protected void measureChild(@NonNull View child, int parentWidthSpec,
                                int parentHeightSpec) {
        final LayoutParams lp = child.getLayoutParams();

        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthSpec,
                mPaddingLeft + mPaddingRight, lp.width);
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec,
                mPaddingTop + mPaddingBottom, lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    /**
     * Ask one of the children of this view to measure itself, taking into
     * account both the MeasureSpec requirements for this view and its padding
     * and margins. The child must have MarginLayoutParams The heavy lifting is
     * done in getChildMeasureSpec.
     *
     * @param child            The child to measure
     * @param parentWidthSpec  The width requirements for this view
     * @param widthUsed        Extra space that has been used up by the parent
     *                         horizontally (possibly by other children of the parent)
     * @param parentHeightSpec The height requirements for this view
     * @param heightUsed       Extra space that has been used up by the parent
     *                         vertically (possibly by other children of the parent)
     */
    protected void measureChildWithMargins(@NonNull View child,
                                           int parentWidthSpec, int widthUsed,
                                           int parentHeightSpec, int heightUsed) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthSpec,
                mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin
                        + widthUsed, lp.width);
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec,
                mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin
                        + heightUsed, lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    /**
     * Does the hard part of measureChildren: figuring out the MeasureSpec to
     * pass to a particular child. This method figures out the right MeasureSpec
     * for one dimension (height or width) of one child view.
     * <p>
     * The goal is to combine information from our MeasureSpec with the
     * LayoutParams of the child to get the best possible results. For example,
     * if the this view knows its size (because its MeasureSpec has a mode of
     * EXACTLY), and the child has indicated in its LayoutParams that it wants
     * to be the same size as the parent, the parent should ask the child to
     * layout given an exact size.
     *
     * @param spec           The requirements for this view
     * @param padding        The padding of this view for the current dimension and
     *                       margins, if applicable
     * @param childDimension How big the child wants to be in the current
     *                       dimension
     * @return a MeasureSpec integer for the child
     */
    public static int getChildMeasureSpec(int spec, int padding, int childDimension) {
        int specSize = MeasureSpec.getSize(spec);

        int size = Math.max(0, specSize - padding);

        int resultSize = 0;
        int resultMode = MeasureSpec.UNSPECIFIED;

        switch (MeasureSpec.getMode(spec)) {
            // Parent has imposed an exact size on us
            case MeasureSpec.EXACTLY:
                if (childDimension >= 0) {
                    resultSize = childDimension;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    // Child wants to be our size. So be it.
                    resultSize = size;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    // Child wants to determine its own size. It can't be
                    // bigger than us.
                    resultSize = size;
                    resultMode = MeasureSpec.AT_MOST;
                }
                break;

            // Parent has imposed a maximum size on us
            case MeasureSpec.AT_MOST:
                if (childDimension >= 0) {
                    // Child wants a specific size... so be it
                    resultSize = childDimension;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    // Child wants to be our size, but our size is not fixed.
                    // Constrain child to not be bigger than us.
                    resultSize = size;
                    resultMode = MeasureSpec.AT_MOST;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    // Child wants to determine its own size. It can't be
                    // bigger than us.
                    resultSize = size;
                    resultMode = MeasureSpec.AT_MOST;
                }
                break;

            // Parent asked to see how big we want to be
            case MeasureSpec.UNSPECIFIED:
                if (childDimension >= 0) {
                    // Child wants a specific size... let him have it
                    resultSize = childDimension;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    // Child wants to be our size... find out how big it should
                    // be
                    resultSize = size;
                    //resultMode = MeasureSpec.UNSPECIFIED;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    // Child wants to determine its own size.... find out how
                    // big it should be
                    resultSize = size;
                    //resultMode = MeasureSpec.UNSPECIFIED;
                }
                break;
        }

        return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
    }

    /**
     * Removes any pending animations for views that have been removed. Call
     * this if you don't want animations for exiting views to stack up.
     */
    public void clearDisappearingChildren() {
        final ArrayList<View> disappearingChildren = mDisappearingChildren;
        if (disappearingChildren != null) {
            for (View view : disappearingChildren) {
                if (view.mAttachInfo != null) {
                    view.dispatchDetachedFromWindow();
                }
            }
            disappearingChildren.clear();
            invalidate();
        }
    }

    /**
     * Add a view which is removed from mChildren but still needs animation
     *
     * @param v View to add
     */
    private void addDisappearingView(View v) {
        ArrayList<View> disappearingChildren = mDisappearingChildren;

        if (disappearingChildren == null) {
            disappearingChildren = mDisappearingChildren = new ArrayList<>();
        }

        disappearingChildren.add(v);
    }

    /**
     * Utility function called by View during invalidation to determine whether a view that
     * is invisible or gone should still be invalidated because it is being transitioned (and
     * therefore still needs to be drawn).
     */
    boolean isViewTransitioning(View view) {
        return (mTransitioningViews != null && mTransitioningViews.contains(view));
    }

    /**
     * This method tells the ViewGroup that the given View object, which should have this
     * ViewGroup as its parent,
     * should be kept around  (re-displayed when the ViewGroup draws its children) even if it
     * is removed from its parent. This allows animations, such as those used by
     * {@link icyllis.modernui.fragment.Fragment} and {@link LayoutTransition} to animate
     * the removal of views. A call to this method should always be accompanied by a later call
     * to {@link #endViewTransition(View)}, such as after an animation on the View has finished,
     * so that the View finally gets removed.
     *
     * @param view The View object to be kept visible even if it gets removed from its parent.
     */
    public void startViewTransition(@NonNull View view) {
        if (view.mParent == this) {
            if (mTransitioningViews == null) {
                mTransitioningViews = new ArrayList<>();
            }
            mTransitioningViews.add(view);
        }
    }

    /**
     * This method should always be called following an earlier call to
     * {@link #startViewTransition(View)}. The given View is finally removed from its parent
     * and will no longer be displayed. Note that this method does not perform the functionality
     * of removing a view from its parent; it just discontinues the display of a View that
     * has previously been removed.
     *
     * @param view The View object that has been removed but is being kept around in the visible
     *             hierarchy by an earlier call to {@link #startViewTransition(View)}.
     */
    public void endViewTransition(View view) {
        if (mTransitioningViews != null) {
            mTransitioningViews.remove(view);
            final ArrayList<View> disappearingChildren = mDisappearingChildren;
            if (disappearingChildren != null && disappearingChildren.contains(view)) {
                disappearingChildren.remove(view);
                if (mVisibilityChangingChildren != null &&
                        mVisibilityChangingChildren.contains(view)) {
                    mVisibilityChangingChildren.remove(view);
                } else {
                    if (view.mAttachInfo != null) {
                        view.dispatchDetachedFromWindow();
                    }
                    if (view.mParent != null) {
                        view.mParent = null;
                    }
                }
                invalidate();
            }
        }
    }

    private final LayoutTransitionListener mLayoutTransitionListener = new LayoutTransitionListener();

    private class LayoutTransitionListener implements LayoutTransition.TransitionListener {

        @Override
        public void startTransition(LayoutTransition transition, ViewGroup container,
                                    View view, int transitionType) {
            // We only care about disappearing items, since we need special logic to keep
            // those items visible after they've been 'removed'
            if (transitionType == LayoutTransition.DISAPPEARING) {
                startViewTransition(view);
            }
        }

        @Override
        public void endTransition(LayoutTransition transition, ViewGroup container,
                                  View view, int transitionType) {
            if (mLayoutCalledWhileSuppressed && !transition.isChangingLayout()) {
                requestLayout();
                mLayoutCalledWhileSuppressed = false;
            }
            if (transitionType == LayoutTransition.DISAPPEARING && mTransitioningViews != null) {
                endViewTransition(view);
            }
        }
    }

    /**
     * Tells this ViewGroup to suppress all layout() calls until layout
     * suppression is disabled with a later call to suppressLayout(false).
     * When layout suppression is disabled, a requestLayout() call is sent
     * if layout() was attempted while layout was being suppressed.
     */
    public void suppressLayout(boolean suppress) {
        mSuppressLayout = suppress;
        if (!suppress) {
            if (mLayoutCalledWhileSuppressed) {
                requestLayout();
                mLayoutCalledWhileSuppressed = false;
            }
        }
    }

    /**
     * Returns whether layout calls on this container are currently being
     * suppressed, due to an earlier call to {@link #suppressLayout(boolean)}.
     *
     * @return true if layout calls are currently suppressed, false otherwise.
     */
    public boolean isLayoutSuppressed() {
        return mSuppressLayout;
    }

    @ApiStatus.Internal
    @Override
    protected void internalSetPadding(int left, int top, int right, int bottom) {
        super.internalSetPadding(left, top, right, bottom);

        if ((mPaddingLeft | mPaddingTop | mPaddingRight | mPaddingBottom) != 0) {
            mGroupFlags |= FLAG_PADDING_NOT_NULL;
        } else {
            mGroupFlags &= ~FLAG_PADDING_NOT_NULL;
        }
    }

    /**
     * Called when a view's visibility has changed. Notify the parent to take any appropriate
     * action.
     *
     * @param child         The view whose visibility has changed
     * @param oldVisibility The previous visibility value (GONE, INVISIBLE, or VISIBLE).
     * @param newVisibility The new visibility value (GONE, INVISIBLE, or VISIBLE).
     */
    @ApiStatus.Internal
    protected void onChildVisibilityChanged(View child, int oldVisibility, int newVisibility) {
        if (mTransition != null) {
            if (newVisibility == VISIBLE) {
                mTransition.showChild(this, child, oldVisibility);
            } else {
                mTransition.hideChild(this, child, newVisibility);
                if (mTransitioningViews != null && mTransitioningViews.contains(child)) {
                    // Only track this on disappearing views - appearing views are already visible
                    // and don't need special handling during drawChild()
                    if (mVisibilityChangingChildren == null) {
                        mVisibilityChangingChildren = new ArrayList<>();
                    }
                    mVisibilityChangingChildren.add(child);
                    addDisappearingView(child);
                }
            }
        }
    }

    @Override
    protected void dispatchVisibilityChanged(@NonNull View changedView, int visibility) {
        super.dispatchVisibilityChanged(changedView, visibility);
        final int count = mChildrenCount;
        final View[] children = mChildren;
        for (int i = 0; i < count; i++) {
            children[i].dispatchVisibilityChanged(changedView, visibility);
        }
    }

    @Override
    public void dispatchWindowVisibilityChanged(int visibility) {
        super.dispatchWindowVisibilityChanged(visibility);
        final int count = mChildrenCount;
        final View[] children = mChildren;
        for (int i = 0; i < count; i++) {
            children[i].dispatchWindowVisibilityChanged(visibility);
        }
    }

    @Override
    boolean dispatchVisibilityAggregated(boolean isVisible) {
        isVisible = super.dispatchVisibilityAggregated(isVisible);
        final int count = mChildrenCount;
        final View[] children = mChildren;
        for (int i = 0; i < count; i++) {
            // Only dispatch to visible children. Not visible children and their subtrees already
            // know that they aren't visible and that's not going to change as a result of
            // whatever triggered this dispatch.
            if (children[i].getVisibility() == VISIBLE) {
                children[i].dispatchVisibilityAggregated(isVisible);
            }
        }
        return isVisible;
    }

    /**
     * Return true if the pressed state should be delayed for children or descendants of this
     * ViewGroup. Generally, this should be done for containers that can scroll, such as a List.
     * This prevents the pressed state from appearing when the user is actually trying to scroll
     * the content.
     * <p>
     * The default implementation returns true for compatibility reasons. Subclasses that do
     * not scroll should generally override this method and return false.
     */
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
        return false;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes, int type) {
        if (type == TYPE_NON_TOUCH) {
            mNestedScrollAxesNonTouch = axes;
        } else {
            mNestedScrollAxesTouch = axes;
        }
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        // Stop any recursive nested scrolling.
        stopNestedScroll(type);
        if (type == TYPE_NON_TOUCH) {
            mNestedScrollAxesNonTouch = SCROLL_AXIS_NONE;
        } else {
            mNestedScrollAxesTouch = SCROLL_AXIS_NONE;
        }
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
                               int dyUnconsumed, int type, @NonNull int[] consumed) {
        // Re-dispatch up the tree by default
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, null, type, consumed);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        // Re-dispatch up the tree by default
        dispatchNestedPreScroll(dx, dy, consumed, null, type);
    }

    @Override
    public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY, boolean consumed) {
        // Re-dispatch up the tree by default
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
        // Re-dispatch up the tree by default
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollAxesTouch | mNestedScrollAxesNonTouch;
    }

    @ApiStatus.Internal
    protected void onSetLayoutParams(View child, LayoutParams layoutParams) {
        requestLayout();
    }

    /**
     * Returns a safe set of layout parameters based on the supplied layout params.
     * When a ViewGroup is passed a View whose layout params do not pass the test of
     * {@link #checkLayoutParams(LayoutParams)}, this method
     * is invoked. This method should return a new set of layout params suitable for
     * this ViewGroup, possibly by copying the appropriate attributes from the
     * specified set of layout params.
     *
     * @param p The layout parameters to convert into a suitable set of layout parameters
     *          for this ViewGroup.
     * @return an instance of {@link LayoutParams} or one of its descendants
     */
    @NonNull
    protected LayoutParams generateLayoutParams(@NonNull LayoutParams p) {
        return p;
    }

    /**
     * Returns a set of default layout parameters. These parameters are requested
     * when the View passed to {@link #addView(View)} has no layout parameters
     * already set. If null is returned, an exception is thrown from addView.
     *
     * @return a set of default layout parameters
     */
    @NonNull
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    /**
     * Check whether given params fit to this view group.
     * <p>
     * See also {@link #generateLayoutParams(LayoutParams)}
     * See also {@link #generateDefaultLayoutParams()}
     *
     * @param p layout params to check
     * @return if params matched to this view group
     */
    protected boolean checkLayoutParams(@Nullable LayoutParams p) {
        return p != null;
    }

    /**
     * LayoutParams are used by views to tell their parents how they want to
     * be laid out.
     * <p>
     * The base LayoutParams class just describes how big the view wants to be
     * for both width and height.
     * <p>
     * There are subclasses of LayoutParams for different subclasses of
     * ViewGroup.
     */
    public static class LayoutParams {

        /**
         * Special value for the height or width requested by a View.
         * MATCH_PARENT means that the view wants to be as big as its parent,
         * minus the parent's padding, if any. Introduced in API Level 8.
         */
        public static final int MATCH_PARENT = -1;

        /**
         * Special value for the height or width requested by a View.
         * WRAP_CONTENT means that the view wants to be just large enough to fit
         * its own internal content, taking its own padding into account.
         */
        public static final int WRAP_CONTENT = -2;

        /**
         * Information about how wide the view wants to be. Can be one of the
         * constants MATCH_PARENT or WRAP_CONTENT, or an exact size.
         */
        public int width;

        /**
         * Information about how tall the view wants to be. Can be one of the
         * constants MATCH_PARENT or WRAP_CONTENT, or an exact size.
         */
        public int height;

        /**
         * Creates a new set of layout parameters with the specified width
         * and height.
         *
         * @param width  the width, either {@link #WRAP_CONTENT},
         *               {@link #MATCH_PARENT}, or a size in pixels
         * @param height the height, either {@link #WRAP_CONTENT},
         *               {@link #MATCH_PARENT}, or a size in pixels
         */
        public LayoutParams(int width, int height) {
            this.width = width;
            this.height = height;
        }

        /**
         * Copy constructor. Clones the width and height values of the source.
         *
         * @param source the layout params to copy from.
         */
        public LayoutParams(@NonNull LayoutParams source) {
            width = source.width;
            height = source.height;
        }

        /**
         * Used internally by MarginLayoutParams.
         */
        @ApiStatus.Internal
        LayoutParams() {
        }

        /**
         * Resolve layout parameters depending on the layout direction. Subclasses that care about
         * layoutDirection changes should override this method. The default implementation does
         * nothing.
         *
         * @param layoutDirection the direction of the layout
         * @see View#LAYOUT_DIRECTION_LTR
         * @see View#LAYOUT_DIRECTION_RTL
         */
        public void resolveLayoutDirection(int layoutDirection) {
        }

        /**
         * Use {@code canvas} to draw suitable debugging annotations for these LayoutParameters.
         *
         * @param view the view that contains these layout parameters
         * @param canvas the canvas on which to draw
         *
         * @hidden
         */
        @ApiStatus.Internal
        public void onDebugDraw(View view, Canvas canvas, Paint paint) {
        }
    }

    /**
     * Per-child layout information for layouts that support margins.
     */
    public static class MarginLayoutParams extends LayoutParams {

        /**
         * The left margin in pixels of the child. Margin values should be positive.
         * Call {@link ViewGroup#setLayoutParams(LayoutParams)} after reassigning a new value
         * to this field.
         */
        public int leftMargin;

        /**
         * The top margin in pixels of the child. Margin values should be positive.
         * Call {@link ViewGroup#setLayoutParams(LayoutParams)} after reassigning a new value
         * to this field.
         */
        public int topMargin;

        /**
         * The right margin in pixels of the child. Margin values should be positive.
         * Call {@link ViewGroup#setLayoutParams(LayoutParams)} after reassigning a new value
         * to this field.
         */
        public int rightMargin;

        /**
         * The bottom margin in pixels of the child. Margin values should be positive.
         * Call {@link ViewGroup#setLayoutParams(LayoutParams)} after reassigning a new value
         * to this field.
         */
        public int bottomMargin;

        /**
         * The start margin in pixels of the child. Margin values should be positive.
         * Call {@link ViewGroup#setLayoutParams(LayoutParams)} after reassigning a new value
         * to this field.
         */
        private int startMargin = DEFAULT_MARGIN_RELATIVE;

        /**
         * The end margin in pixels of the child. Margin values should be positive.
         * Call {@link ViewGroup#setLayoutParams(LayoutParams)} after reassigning a new value
         * to this field.
         */
        private int endMargin = DEFAULT_MARGIN_RELATIVE;

        /**
         * The default start and end margin.
         */
        @ApiStatus.Internal
        public static final int DEFAULT_MARGIN_RELATIVE = Integer.MIN_VALUE;

        /**
         * Bit  0: layout direction
         * Bit  1: layout direction
         * Bit  2: left margin undefined
         * Bit  3: right margin undefined
         * Bit  4: is RTL compatibility mode
         * Bit  5: need resolution
         * <p>
         * Bit 6 to 7 not used
         */
        @ApiStatus.Internal
        byte mMarginFlags;

        private static final int LAYOUT_DIRECTION_MASK = 0x00000003;
        private static final int LEFT_MARGIN_UNDEFINED_MASK = 0x00000004;
        private static final int RIGHT_MARGIN_UNDEFINED_MASK = 0x00000008;
        private static final int RTL_COMPATIBILITY_MODE_MASK = 0x00000010;
        private static final int NEED_RESOLUTION_MASK = 0x00000020;

        private static final int DEFAULT_MARGIN_RESOLVED = 0;
        private static final int UNDEFINED_MARGIN = DEFAULT_MARGIN_RELATIVE;

        /**
         * Creates a new set of layout parameters with the specified width
         * and height.
         *
         * @param width  the width, either {@link #WRAP_CONTENT},
         *               {@link #MATCH_PARENT}, or a size in pixels
         * @param height the height, either {@link #WRAP_CONTENT},
         *               {@link #MATCH_PARENT}, or a size in pixels
         */
        public MarginLayoutParams(int width, int height) {
            super(width, height);

            mMarginFlags |= LEFT_MARGIN_UNDEFINED_MASK;
            mMarginFlags |= RIGHT_MARGIN_UNDEFINED_MASK;
        }

        /**
         * Copy constructor. Clones the width, height and margin values of the source.
         *
         * @param source The layout params to copy from.
         */
        public MarginLayoutParams(@NonNull MarginLayoutParams source) {
            super(source);

            this.leftMargin = source.leftMargin;
            this.topMargin = source.topMargin;
            this.rightMargin = source.rightMargin;
            this.bottomMargin = source.bottomMargin;
            this.startMargin = source.startMargin;
            this.endMargin = source.endMargin;

            this.mMarginFlags = source.mMarginFlags;
        }

        public MarginLayoutParams(LayoutParams source) {
            super(source);

            mMarginFlags |= LEFT_MARGIN_UNDEFINED_MASK;
            mMarginFlags |= RIGHT_MARGIN_UNDEFINED_MASK;
        }

        @ApiStatus.Internal
        public final void copyMarginsFrom(@NonNull MarginLayoutParams source) {
            this.leftMargin = source.leftMargin;
            this.topMargin = source.topMargin;
            this.rightMargin = source.rightMargin;
            this.bottomMargin = source.bottomMargin;
            this.startMargin = source.startMargin;
            this.endMargin = source.endMargin;

            this.mMarginFlags = source.mMarginFlags;
        }

        /**
         * Sets the margins, in pixels. A call to {@link View#requestLayout()} needs
         * to be done so that the new margins are taken into account. Left and right margins may be
         * overridden by {@link View#requestLayout()} depending on layout direction.
         * Margin values should be positive.
         *
         * @param left   the left margin size
         * @param top    the top margin size
         * @param right  the right margin size
         * @param bottom the bottom margin size
         */
        public void setMargins(int left, int top, int right, int bottom) {
            leftMargin = left;
            topMargin = top;
            rightMargin = right;
            bottomMargin = bottom;
            mMarginFlags &= ~LEFT_MARGIN_UNDEFINED_MASK;
            mMarginFlags &= ~RIGHT_MARGIN_UNDEFINED_MASK;
            if (isMarginRelative()) {
                mMarginFlags |= NEED_RESOLUTION_MASK;
            } else {
                mMarginFlags &= ~NEED_RESOLUTION_MASK;
            }
        }

        /**
         * Sets the relative margins, in pixels. A call to {@link View#requestLayout()}
         * needs to be done so that the new relative margins are taken into account. Left and right
         * margins may be overridden by {@link View#requestLayout()} depending on
         * layout direction. Margin values should be positive.
         *
         * @param start  the start margin size
         * @param top    the top margin size
         * @param end    the right margin size
         * @param bottom the bottom margin size
         */
        public void setMarginsRelative(int start, int top, int end, int bottom) {
            startMargin = start;
            topMargin = top;
            endMargin = end;
            bottomMargin = bottom;
            mMarginFlags |= NEED_RESOLUTION_MASK;
        }

        /**
         * Sets the relative start margin. Margin values should be positive.
         *
         * @param start the start margin size
         */
        public void setMarginStart(int start) {
            startMargin = start;
            mMarginFlags |= NEED_RESOLUTION_MASK;
        }

        /**
         * Returns the start margin in pixels.
         *
         * @return the start margin in pixels.
         */
        public int getMarginStart() {
            if (startMargin != DEFAULT_MARGIN_RELATIVE)
                return startMargin;
            if ((mMarginFlags & NEED_RESOLUTION_MASK) == NEED_RESOLUTION_MASK) {
                doResolveMargins();
            }
            if (isLayoutRtl()) {
                return rightMargin;
            } else {
                return leftMargin;
            }
        }

        /**
         * Sets the relative end margin. Margin values should be positive.
         *
         * @param end the end margin size
         */
        public void setMarginEnd(int end) {
            endMargin = end;
            mMarginFlags |= NEED_RESOLUTION_MASK;
        }

        /**
         * Returns the end margin in pixels.
         *
         * @return the end margin in pixels.
         */
        public int getMarginEnd() {
            if (endMargin != DEFAULT_MARGIN_RELATIVE)
                return endMargin;
            if ((mMarginFlags & NEED_RESOLUTION_MASK) == NEED_RESOLUTION_MASK) {
                doResolveMargins();
            }
            if (isLayoutRtl()) {
                return leftMargin;
            } else {
                return rightMargin;
            }
        }

        /**
         * Check if margins are relative.
         *
         * @return true if either marginStart or marginEnd has been set.
         */
        public boolean isMarginRelative() {
            return (startMargin != DEFAULT_MARGIN_RELATIVE || endMargin != DEFAULT_MARGIN_RELATIVE);
        }

        /**
         * Set the layout direction
         *
         * @param layoutDirection the layout direction.
         *                        Should be either {@link View#LAYOUT_DIRECTION_LTR}
         *                        or {@link View#LAYOUT_DIRECTION_RTL}.
         */
        public void setLayoutDirection(int layoutDirection) {
            if (layoutDirection != View.LAYOUT_DIRECTION_LTR &&
                    layoutDirection != View.LAYOUT_DIRECTION_RTL) return;
            if (layoutDirection != (mMarginFlags & LAYOUT_DIRECTION_MASK)) {
                mMarginFlags &= ~LAYOUT_DIRECTION_MASK;
                mMarginFlags |= (layoutDirection & LAYOUT_DIRECTION_MASK);
                if (isMarginRelative()) {
                    mMarginFlags |= NEED_RESOLUTION_MASK;
                } else {
                    mMarginFlags &= ~NEED_RESOLUTION_MASK;
                }
            }
        }

        /**
         * Returns the layout direction. Can be either {@link View#LAYOUT_DIRECTION_LTR} or
         * {@link View#LAYOUT_DIRECTION_RTL}.
         *
         * @return the layout direction.
         */
        public int getLayoutDirection() {
            return (mMarginFlags & LAYOUT_DIRECTION_MASK);
        }

        /**
         * This will be called by {@link View#requestLayout()}. Left and Right margins
         * may be overridden depending on layout direction.
         */
        @Override
        public void resolveLayoutDirection(int layoutDirection) {
            setLayoutDirection(layoutDirection);

            // No relative margin or pre JB-MR1 case or no need to resolve, just dont do anything
            // Will use the left and right margins if no relative margin is defined.
            if (!isMarginRelative() ||
                    (mMarginFlags & NEED_RESOLUTION_MASK) != NEED_RESOLUTION_MASK) return;

            // Proceed with resolution
            doResolveMargins();
        }

        private void doResolveMargins() {
            if ((mMarginFlags & RTL_COMPATIBILITY_MODE_MASK) == RTL_COMPATIBILITY_MODE_MASK) {
                // if left or right margins are not defined and if we have some start or end margin
                // defined then use those start and end margins.
                if ((mMarginFlags & LEFT_MARGIN_UNDEFINED_MASK) == LEFT_MARGIN_UNDEFINED_MASK
                        && startMargin > DEFAULT_MARGIN_RELATIVE) {
                    leftMargin = startMargin;
                }
                if ((mMarginFlags & RIGHT_MARGIN_UNDEFINED_MASK) == RIGHT_MARGIN_UNDEFINED_MASK
                        && endMargin > DEFAULT_MARGIN_RELATIVE) {
                    rightMargin = endMargin;
                }
            } else {
                // We have some relative margins (either the start one or the end one or both). So use
                // them and override what has been defined for left and right margins. If either start
                // or end margin is not defined, just set it to default "0".
                if (isLayoutRtl()) {
                    leftMargin = (endMargin > DEFAULT_MARGIN_RELATIVE) ?
                            endMargin : DEFAULT_MARGIN_RESOLVED;
                    rightMargin = (startMargin > DEFAULT_MARGIN_RELATIVE) ?
                            startMargin : DEFAULT_MARGIN_RESOLVED;
                } else {
                    leftMargin = (startMargin > DEFAULT_MARGIN_RELATIVE) ?
                            startMargin : DEFAULT_MARGIN_RESOLVED;
                    rightMargin = (endMargin > DEFAULT_MARGIN_RELATIVE) ?
                            endMargin : DEFAULT_MARGIN_RESOLVED;
                }
            }
            mMarginFlags &= ~NEED_RESOLUTION_MASK;
        }

        @ApiStatus.Internal
        public boolean isLayoutRtl() {
            return ((mMarginFlags & LAYOUT_DIRECTION_MASK) == View.LAYOUT_DIRECTION_RTL);
        }

        @ApiStatus.Internal
        @Override
        public void onDebugDraw(View view, Canvas canvas, Paint paint) {
            FloatBuffer positions = sDebugDrawBuffer.get()
                    .clear();
            int x2 = view.getLeft();
            int y2 = view.getTop();
            int x3 = view.getRight();
            int y3 = view.getBottom();
            int x1 = x2 - leftMargin;
            int y1 = y2 - topMargin;
            int x4 = x3 + rightMargin;
            int y4 = y3 + bottomMargin;

            fillRect(x1, y1, x4, y2, positions);
            fillRect(x1, y2, x2, y3, positions);
            fillRect(x3, y2, x4, y3, positions);
            fillRect(x1, y3, x4, y4, positions);

            canvas.drawMesh(Canvas.VertexMode.TRIANGLES,
                    positions.flip(), null, null, null, null,
                    paint);
        }
    }

    /**
     * Describes a hovered view.
     */
    private static final class HoverTarget {

        private static final int MAX_RECYCLED = 32;
        private static final Object sRecyclerLock = new Object();
        private static HoverTarget sRecyclerTop;
        private static int sRecyclerUsed;

        // The hovered view, one of the child of this ViewGroup
        public View child;

        // The next target in the linked list.
        public HoverTarget next;

        private HoverTarget() {
        }

        @NonNull
        public static HoverTarget obtain(@NonNull View child) {
            final HoverTarget target;
            synchronized (sRecyclerLock) {
                if (sRecyclerTop == null) {
                    target = new HoverTarget();
                } else {
                    target = sRecyclerTop;
                    sRecyclerTop = target.next;
                    sRecyclerUsed--;
                    target.next = null;
                }
            }
            target.child = child;
            return target;
        }

        public void recycle() {
            if (child == null) {
                throw new IllegalStateException(this + " already recycled");
            }
            synchronized (sRecyclerLock) {
                if (sRecyclerUsed < MAX_RECYCLED) {
                    sRecyclerUsed++;
                    next = sRecyclerTop;
                    sRecyclerTop = this;
                } else {
                    next = null;
                }
                child = null;
            }
        }
    }

    /**
     * Describes a touched view and the ids of the pointers that it has captured.
     * <p>
     * This code assumes that pointer ids are always in the range 0..31 such that
     * it can use a bitfield to track which pointer ids are present.
     * As it happens, the lower layers of the input dispatch pipeline also use the
     * same trick so the assumption should be safe here...
     */
    private static final class TouchTarget {

        private static final Pools.Pool<TouchTarget> sPool = Pools.newSynchronizedPool(12);

        // The touched view, one of the child of this ViewGroup
        public View child;

        private TouchTarget() {
        }

        @NonNull
        public static TouchTarget obtain(@NonNull View child) {
            TouchTarget target = sPool.acquire();
            if (target == null) {
                target = new TouchTarget();
            }
            target.child = child;
            return target;
        }

        public void recycle() {
            if (child == null) {
                throw new IllegalStateException(this + " already recycled");
            }
            sPool.release(this);
            child = null;
        }
    }
}
