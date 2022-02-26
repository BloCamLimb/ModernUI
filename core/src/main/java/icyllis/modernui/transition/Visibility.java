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

package icyllis.modernui.transition;

import icyllis.modernui.R;
import icyllis.modernui.animation.Animator;
import icyllis.modernui.animation.AnimatorListener;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import org.intellij.lang.annotations.MagicConstant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This transition tracks changes to the visibility of target views in the
 * start and end scenes. Visibility is determined not just by the
 * {@link View#setVisibility(int)} state of views, but also whether
 * views exist in the current view hierarchy. The class is intended to be a
 * utility for subclasses such as {@link Fade}, which use this visibility
 * information to determine the specific animations to run when visibility
 * changes occur. Subclasses should implement one or both of the methods
 * {@link #onAppear(ViewGroup, TransitionValues, int, TransitionValues, int)},
 * {@link #onDisappear(ViewGroup, TransitionValues, int, TransitionValues, int)} or
 * {@link #onAppear(ViewGroup, View, TransitionValues, TransitionValues)},
 * {@link #onDisappear(ViewGroup, View, TransitionValues, TransitionValues)}.
 */
public abstract class Visibility extends Transition {

    static final String PROPNAME_VISIBILITY = "modernui:visibility:visibility";
    private static final String PROPNAME_PARENT = "modernui:visibility:parent";
    private static final String PROPNAME_SCREEN_LOCATION = "modernui:visibility:screenLocation";

    /**
     * Mode used in {@link #setMode(int)} to make the transition
     * operate on targets that are appearing. Maybe be combined with
     * {@link #MODE_OUT} to target Visibility changes both in and out.
     */
    public static final int MODE_IN = 0x1;

    /**
     * Mode used in {@link #setMode(int)} to make the transition
     * operate on targets that are disappearing. Maybe be combined with
     * {@link #MODE_IN} to target Visibility changes both in and out.
     */
    public static final int MODE_OUT = 0x2;

    @MagicConstant(flags = {MODE_IN, MODE_OUT, Fade.IN, Fade.OUT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Mode {
    }

    private static final String[] sTransitionProperties = {
            PROPNAME_VISIBILITY,
            PROPNAME_PARENT,
    };

    private static class VisibilityInfo {

        boolean mVisibilityChange;
        boolean mFadeIn;
        int mStartVisibility;
        int mEndVisibility;
        ViewGroup mStartParent;
        ViewGroup mEndParent;
    }

    private int mMode = MODE_IN | MODE_OUT;

    public Visibility() {
    }

    /**
     * Changes the transition to support appearing and/or disappearing Views, depending
     * on <code>mode</code>.
     *
     * @param mode The behavior supported by this transition, a combination of
     *             {@link #MODE_IN} and {@link #MODE_OUT}.
     */
    public void setMode(@Mode int mode) {
        if ((mode & ~(MODE_IN | MODE_OUT)) != 0) {
            throw new IllegalArgumentException("Only MODE_IN and MODE_OUT flags are allowed");
        }
        mMode = mode;
    }

    /**
     * Returns whether appearing and/or disappearing Views are supported.
     *
     * @return whether appearing and/or disappearing Views are supported. A combination of
     * {@link #MODE_IN} and {@link #MODE_OUT}.
     */
    @Mode
    public int getMode() {
        return mMode;
    }

    @Nullable
    @Override
    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    private void captureValues(@Nonnull TransitionValues transitionValues) {
        int visibility = transitionValues.view.getVisibility();
        transitionValues.values.put(PROPNAME_VISIBILITY, visibility);
        transitionValues.values.put(PROPNAME_PARENT, transitionValues.view.getParent());
        int[] loc = new int[2];
        transitionValues.view.getLocationInWindow(loc);
        transitionValues.values.put(PROPNAME_SCREEN_LOCATION, loc);
    }

    @Override
    public void captureStartValues(@Nonnull TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(@Nonnull TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    /**
     * Returns whether the view is 'visible' according to the given values
     * object. This is determined by testing the same properties in the values
     * object that are used to determine whether the object is appearing or
     * disappearing in the {@link
     * Transition#createAnimator(ViewGroup, TransitionValues, TransitionValues)}
     * method. This method can be called by, for example, subclasses that want
     * to know whether the object is visible in the same way that Visibility
     * determines it for the actual animation.
     *
     * @param values The TransitionValues object that holds the information by
     *               which visibility is determined.
     * @return True if the view reference by <code>values</code> is visible,
     * false otherwise.
     */
    public boolean isVisible(TransitionValues values) {
        if (values == null) {
            return false;
        }
        int visibility = (Integer) values.values.get(PROPNAME_VISIBILITY);
        View parent = (View) values.values.get(PROPNAME_PARENT);

        return visibility == View.VISIBLE && parent != null;
    }

    private VisibilityInfo getVisibilityChangeInfo(TransitionValues startValues,
                                                   TransitionValues endValues) {
        final VisibilityInfo visInfo = new VisibilityInfo();
        visInfo.mVisibilityChange = false;
        visInfo.mFadeIn = false;
        if (startValues != null && startValues.values.containsKey(PROPNAME_VISIBILITY)) {
            visInfo.mStartVisibility = (Integer) startValues.values.get(PROPNAME_VISIBILITY);
            visInfo.mStartParent = (ViewGroup) startValues.values.get(PROPNAME_PARENT);
        } else {
            visInfo.mStartVisibility = -1;
            visInfo.mStartParent = null;
        }
        if (endValues != null && endValues.values.containsKey(PROPNAME_VISIBILITY)) {
            visInfo.mEndVisibility = (Integer) endValues.values.get(PROPNAME_VISIBILITY);
            visInfo.mEndParent = (ViewGroup) endValues.values.get(PROPNAME_PARENT);
        } else {
            visInfo.mEndVisibility = -1;
            visInfo.mEndParent = null;
        }
        if (startValues != null && endValues != null) {
            if (visInfo.mStartVisibility == visInfo.mEndVisibility
                    && visInfo.mStartParent == visInfo.mEndParent) {
                return visInfo;
            } else {
                if (visInfo.mStartVisibility != visInfo.mEndVisibility) {
                    if (visInfo.mStartVisibility == View.VISIBLE) {
                        visInfo.mFadeIn = false;
                        visInfo.mVisibilityChange = true;
                    } else if (visInfo.mEndVisibility == View.VISIBLE) {
                        visInfo.mFadeIn = true;
                        visInfo.mVisibilityChange = true;
                    }
                    // no visibilityChange if going between INVISIBLE and GONE
                } else /* if (visInfo.mStartParent != visInfo.mEndParent) */ {
                    if (visInfo.mEndParent == null) {
                        visInfo.mFadeIn = false;
                        visInfo.mVisibilityChange = true;
                    } else if (visInfo.mStartParent == null) {
                        visInfo.mFadeIn = true;
                        visInfo.mVisibilityChange = true;
                    }
                }
            }
        } else if (startValues == null && visInfo.mEndVisibility == View.VISIBLE) {
            visInfo.mFadeIn = true;
            visInfo.mVisibilityChange = true;
        } else if (endValues == null && visInfo.mStartVisibility == View.VISIBLE) {
            visInfo.mFadeIn = false;
            visInfo.mVisibilityChange = true;
        }
        return visInfo;
    }

    @Nullable
    @Override
    public Animator createAnimator(@Nonnull ViewGroup sceneRoot,
                                   @Nullable TransitionValues startValues, @Nullable TransitionValues endValues) {
        VisibilityInfo visInfo = getVisibilityChangeInfo(startValues, endValues);
        if (visInfo.mVisibilityChange
                && (visInfo.mStartParent != null || visInfo.mEndParent != null)) {
            if (visInfo.mFadeIn) {
                return onAppear(sceneRoot, startValues, visInfo.mStartVisibility,
                        endValues, visInfo.mEndVisibility);
            } else {
                return onDisappear(sceneRoot, startValues, visInfo.mStartVisibility,
                        endValues, visInfo.mEndVisibility
                );
            }
        }
        return null;
    }

    /**
     * The default implementation of this method does nothing. Subclasses
     * should override if they need to create an Animator when targets appear.
     * The method should only be called by the Visibility class; it is
     * not intended to be called from external classes.
     *
     * @param sceneRoot       The root of the transition hierarchy
     * @param startValues     The target values in the start scene
     * @param startVisibility The target visibility in the start scene
     * @param endValues       The target values in the end scene
     * @param endVisibility   The target visibility in the end scene
     * @return An Animator to be started at the appropriate time in the
     * overall transition for this scene change. A null value means no animation
     * should be run.
     */
    @Nullable
    @SuppressWarnings("UnusedParameters")
    public Animator onAppear(ViewGroup sceneRoot, TransitionValues startValues, int startVisibility,
                             TransitionValues endValues, int endVisibility) {
        if ((mMode & MODE_IN) != MODE_IN || endValues == null) {
            return null;
        }
        if (startValues == null && endValues.view.getParent() instanceof View endParent) {
            TransitionValues startParentValues = getMatchedTransitionValues(endParent, false);
            TransitionValues endParentValues = getTransitionValues(endParent, false);
            VisibilityInfo parentVisibilityInfo =
                    getVisibilityChangeInfo(startParentValues, endParentValues);
            if (parentVisibilityInfo.mVisibilityChange) {
                return null;
            }
        }
        return onAppear(sceneRoot, endValues.view, startValues, endValues);
    }

    /**
     * The default implementation of this method returns a null Animator. Subclasses should
     * override this method to make targets appear with the desired transition. The
     * method should only be called from
     * {@link #onAppear(ViewGroup, TransitionValues, int, TransitionValues, int)}.
     *
     * @param sceneRoot   The root of the transition hierarchy
     * @param view        The View to make appear. This will be in the target scene's View
     *                    hierarchy
     *                    and
     *                    will be VISIBLE.
     * @param startValues The target values in the start scene
     * @param endValues   The target values in the end scene
     * @return An Animator to be started at the appropriate time in the
     * overall transition for this scene change. A null value means no animation
     * should be run.
     */
    @Nullable
    public Animator onAppear(ViewGroup sceneRoot, View view, TransitionValues startValues,
                             TransitionValues endValues) {
        return null;
    }

    /**
     * The default implementation of this method does nothing. Subclasses
     * should override if they need to create an Animator when targets disappear.
     * The method should only be called by the Visibility class; it is
     * not intended to be called from external classes.
     *
     * @param sceneRoot       The root of the transition hierarchy
     * @param startValues     The target values in the start scene
     * @param startVisibility The target visibility in the start scene
     * @param endValues       The target values in the end scene
     * @param endVisibility   The target visibility in the end scene
     * @return An Animator to be started at the appropriate time in the
     * overall transition for this scene change. A null value means no animation
     * should be run.
     */
    @Nullable
    @SuppressWarnings("UnusedParameters")
    public Animator onDisappear(ViewGroup sceneRoot, TransitionValues startValues,
                                int startVisibility, TransitionValues endValues, int endVisibility) {
        if ((mMode & MODE_OUT) != MODE_OUT) {
            return null;
        }

        if (startValues == null) {
            // startValues(and startView) will never be null for disappear transition.
            return null;
        }

        final View startView = startValues.view;
        final View endView = (endValues != null) ? endValues.view : null;
        View overlayView = null;
        View viewToKeep = null;
        boolean reusingOverlayView = false;

        View savedOverlayView = (View) startView.getTag(R.id.save_overlay_view);
        if (savedOverlayView != null) {
            // we've already created overlay for the start view.
            // it means that we are applying two visibility
            // transitions for the same view
            overlayView = savedOverlayView;
            reusingOverlayView = true;
        } else {
            boolean needOverlayForStartView = false;

            if (endView == null || endView.getParent() == null) {
                if (endView != null) {
                    // endView was removed from its parent - add it to the overlay
                    overlayView = endView;
                } else {
                    needOverlayForStartView = true;
                }
            } else {
                // visibility change
                if (endVisibility == View.INVISIBLE) {
                    viewToKeep = endView;
                } else {
                    // Becoming GONE
                    if (startView == endView) {
                        viewToKeep = endView;
                    } else {
                        needOverlayForStartView = true;
                    }
                }
            }

            if (needOverlayForStartView) {
                // endView does not exist. Use startView only under certain
                // conditions, because placing a view in an overlay necessitates
                // it being removed from its current parent
                if (startView.getParent() == null) {
                    // no parent - safe to use
                    overlayView = startView;
                } else if (startView.getParent() instanceof View startParent) {
                    TransitionValues startParentValues = getTransitionValues(startParent, true);
                    TransitionValues endParentValues = getMatchedTransitionValues(startParent, true);
                    VisibilityInfo parentVisibilityInfo =
                            getVisibilityChangeInfo(startParentValues, endParentValues);
                    //TODO overlay?? same below
                    if (!parentVisibilityInfo.mVisibilityChange) {
                        /*overlayView = TransitionUtils.copyViewImage(sceneRoot, startView,
                                startParent);*/
                    } else {
                        /*int id = startParent.getId();
                        if (startParent.getParent() == null && id != View.NO_ID
                                && sceneRoot.findViewById(id) != null && mCanRemoveViews) {
                            // no parent, but its parent is unparented  but the parent
                            // hierarchy has been replaced by a new hierarchy with the same id
                            // and it is safe to un-parent startView
                            overlayView = startView;
                        } else {

                        }*/
                    }
                    overlayView = startView;
                }
            }
        }

        if (overlayView != null) {
            if (!reusingOverlayView) {
                int[] screenLoc = (int[]) startValues.values.get(PROPNAME_SCREEN_LOCATION);
                int screenX = screenLoc[0];
                int screenY = screenLoc[1];
                int[] loc = new int[2];
                sceneRoot.getLocationInWindow(loc);
                overlayView.offsetLeftAndRight((screenX - loc[0]) - overlayView.getLeft());
                overlayView.offsetTopAndBottom((screenY - loc[1]) - overlayView.getTop());
                //ViewGroupUtils.getOverlay(sceneRoot).add(overlayView);
                sceneRoot.startViewTransition(overlayView);
            }
            Animator animator = onDisappear(sceneRoot, overlayView, startValues, endValues);
            if (!reusingOverlayView) {
                if (animator == null) {
                    //ViewGroupUtils.getOverlay(sceneRoot).remove(overlayView);
                    sceneRoot.endViewTransition(overlayView);
                } else {
                    startView.setTag(R.id.save_overlay_view, overlayView);
                    final View finalOverlayView = overlayView;
                    final ViewGroup overlayHost = sceneRoot;
                    addListener(new TransitionListener() {

                        @Override
                        public void onTransitionPause(@Nonnull Transition transition) {
                            //ViewGroupUtils.getOverlay(overlayHost).remove(finalOverlayView);
                            overlayHost.endViewTransition(finalOverlayView);
                        }

                        @Override
                        public void onTransitionResume(@Nonnull Transition transition) {
                            if (finalOverlayView.getParent() == null) {
                                //ViewGroupUtils.getOverlay(overlayHost).add(finalOverlayView);
                                overlayHost.startViewTransition(finalOverlayView);
                            } else {
                                cancel();
                            }
                        }

                        @Override
                        public void onTransitionEnd(@Nonnull Transition transition) {
                            startView.setTag(R.id.save_overlay_view, null);
                            //ViewGroupUtils.getOverlay(overlayHost).remove(finalOverlayView);
                            overlayHost.endViewTransition(finalOverlayView);
                            transition.removeListener(this);
                        }
                    });
                }
            }
            return animator;
        }

        if (viewToKeep != null) {
            int originalVisibility = viewToKeep.getVisibility();
            viewToKeep.setTransitionVisibility(View.VISIBLE);
            Animator animator = onDisappear(sceneRoot, viewToKeep, startValues, endValues);
            if (animator != null) {
                DisappearListener disappearListener = new DisappearListener(viewToKeep,
                        endVisibility, true);
                animator.addListener(disappearListener);
                addListener(disappearListener);
            } else {
                viewToKeep.setTransitionVisibility(originalVisibility);
            }
            return animator;
        }
        return null;
    }

    /**
     * The default implementation of this method returns a null Animator. Subclasses should
     * override this method to make targets disappear with the desired transition. The
     * method should only be called from
     * {@link #onDisappear(ViewGroup, TransitionValues, int, TransitionValues, int)}.
     *
     * @param sceneRoot   The root of the transition hierarchy
     * @param view        The View to make disappear. This will be in the target scene's View
     *                    hierarchy and will be VISIBLE.
     * @param startValues The target values in the start scene
     * @param endValues   The target values in the end scene
     * @return An Animator to be started at the appropriate time in the
     * overall transition for this scene change. A null value means no animation
     * should be run.
     */
    @Nullable
    public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues,
                                TransitionValues endValues) {
        return null;
    }

    @Override
    public boolean isTransitionRequired(@Nullable TransitionValues startValues,
                                        @Nullable TransitionValues newValues) {
        if (startValues == null && newValues == null) {
            return false;
        }
        if (startValues != null && newValues != null
                && newValues.values.containsKey(PROPNAME_VISIBILITY)
                != startValues.values.containsKey(PROPNAME_VISIBILITY)) {
            // The transition wasn't targeted in either the start or end, so it couldn't
            // have changed.
            return false;
        }
        VisibilityInfo changeInfo = getVisibilityChangeInfo(startValues, newValues);
        return changeInfo.mVisibilityChange && (changeInfo.mStartVisibility == View.VISIBLE
                || changeInfo.mEndVisibility == View.VISIBLE);
    }

    private static class DisappearListener implements AnimatorListener, TransitionListener {

        private final View mView;
        private final int mFinalVisibility;
        private final ViewGroup mParent;
        private final boolean mSuppressLayout;

        private boolean mLayoutSuppressed;
        boolean mCanceled = false;

        DisappearListener(@Nonnull View view, int finalVisibility, boolean suppressLayout) {
            mView = view;
            mFinalVisibility = finalVisibility;
            mParent = (ViewGroup) view.getParent();
            mSuppressLayout = suppressLayout;
            // Prevent a layout from including mView in its calculation.
            suppressLayout(true);
        }

        // This overrides both AnimatorListenerAdapter and
        // AnimatorUtilsApi14.AnimatorPauseListenerCompat
        @Override
        public void onAnimationPause(@Nonnull Animator animation) {
            if (!mCanceled) {
                mView.setTransitionVisibility(mFinalVisibility);
            }
        }

        // This overrides both AnimatorListenerAdapter and
        // AnimatorUtilsApi14.AnimatorPauseListenerCompat
        @Override
        public void onAnimationResume(@Nonnull Animator animation) {
            if (!mCanceled) {
                mView.setTransitionVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onAnimationCancel(@Nonnull Animator animation) {
            mCanceled = true;
        }

        @Override
        public void onAnimationRepeat(@Nonnull Animator animation) {
        }

        @Override
        public void onAnimationStart(@Nonnull Animator animation) {
        }

        @Override
        public void onAnimationEnd(@Nonnull Animator animation) {
            hideViewWhenNotCanceled();
        }

        @Override
        public void onTransitionStart(@Nonnull Transition transition) {
            // Do nothing
        }

        @Override
        public void onTransitionEnd(@Nonnull Transition transition) {
            hideViewWhenNotCanceled();
            transition.removeListener(this);
        }

        @Override
        public void onTransitionCancel(@Nonnull Transition transition) {
        }

        @Override
        public void onTransitionPause(@Nonnull Transition transition) {
            suppressLayout(false);
        }

        @Override
        public void onTransitionResume(@Nonnull Transition transition) {
            suppressLayout(true);
        }

        private void hideViewWhenNotCanceled() {
            if (!mCanceled) {
                // Recreate the parent's display list in case it includes mView.
                mView.setTransitionVisibility(mFinalVisibility);
                if (mParent != null) {
                    mParent.invalidate();
                }
            }
            // Layout is allowed now that the View is in its final state
            suppressLayout(false);
        }

        private void suppressLayout(boolean suppress) {
            if (mSuppressLayout && mLayoutSuppressed != suppress && mParent != null) {
                mLayoutSuppressed = suppress;
                mParent.suppressLayout(suppress);
            }
        }
    }
}
