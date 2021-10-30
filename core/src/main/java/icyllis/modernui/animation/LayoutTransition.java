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

package icyllis.modernui.animation;

import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class LayoutTransition {

    /**
     * A flag indicating the animation that runs on those items that are changing
     * due to a new item appearing in the container.
     */
    public static final int CHANGE_APPEARING = 0;

    /**
     * A flag indicating the animation that runs on those items that are changing
     * due to an item disappearing from the container.
     */
    public static final int CHANGE_DISAPPEARING = 1;

    /**
     * A flag indicating the animation that runs on those items that are appearing
     * in the container.
     */
    public static final int APPEARING = 2;

    /**
     * A flag indicating the animation that runs on those items that are disappearing
     * from the container.
     */
    public static final int DISAPPEARING = 3;

    /**
     * A flag indicating the animation that runs on those items that are changing
     * due to a layout change not caused by items being added to or removed
     * from the container. This transition type is not enabled by default; it can be
     * enabled via {@link #enableTransitionType(int)}.
     */
    public static final int CHANGING = 4;

    /**
     * Private bit fields used to set the collection of enabled transition types for
     * mTransitionTypes.
     */
    private static final int FLAG_APPEARING = 0x01;
    private static final int FLAG_DISAPPEARING = 0x02;
    private static final int FLAG_CHANGE_APPEARING = 0x04;
    private static final int FLAG_CHANGE_DISAPPEARING = 0x08;
    private static final int FLAG_CHANGING = 0x10;

    /**
     * These are the default animations, defined in the constructor, that will be used
     * unless the user specifies custom animations.
     */
    private static volatile ObjectAnimator defaultChange;
    private static ObjectAnimator defaultChangeIn;
    private static ObjectAnimator defaultChangeOut;
    private static ObjectAnimator defaultFadeIn;
    private static ObjectAnimator defaultFadeOut;

    /**
     * The default duration used by all animations.
     */
    private static final long DEFAULT_DURATION = 300;

    private static final TimeInterpolator sAppearingInterpolator = TimeInterpolator.ACCELERATE_DECELERATE;
    private static final TimeInterpolator sDisappearingInterpolator = TimeInterpolator.ACCELERATE_DECELERATE;
    private static final TimeInterpolator sChangingAppearingInterpolator = TimeInterpolator.DECELERATE;
    private static final TimeInterpolator sChangingDisappearingInterpolator = TimeInterpolator.DECELERATE;
    private static final TimeInterpolator sChangingInterpolator = TimeInterpolator.DECELERATE;

    /**
     * These variables hold the animations that are currently used to run the transition effects.
     * These animations are set to default values, but can be changed to custom animations by
     * calls to setAnimator().
     */
    private Animator mDisappearingAnim;
    private Animator mAppearingAnim;
    private Animator mChangingAppearingAnim;
    private Animator mChangingDisappearingAnim;
    private Animator mChangingAnim;

    /**
     * The durations of the different animations
     */
    private long mChangingAppearingDuration = DEFAULT_DURATION;
    private long mChangingDisappearingDuration = DEFAULT_DURATION;
    private long mChangingDuration = DEFAULT_DURATION;
    private long mAppearingDuration = DEFAULT_DURATION;
    private long mDisappearingDuration = DEFAULT_DURATION;

    /**
     * The start delays of the different animations. Note that the default behavior of
     * the appearing item is the default duration, since it should wait for the items to move
     * before fading it. Same for the changing animation when disappearing; it waits for the item
     * to fade out before moving the other items.
     */
    private long mAppearingDelay = DEFAULT_DURATION;
    private long mDisappearingDelay = 0;
    private long mChangingAppearingDelay = 0;
    private long mChangingDisappearingDelay = DEFAULT_DURATION;
    private long mChangingDelay = 0;

    /**
     * The inter-animation delays used on the changing animations
     */
    private long mChangingAppearingStagger = 0;
    private long mChangingDisappearingStagger = 0;
    private long mChangingStagger = 0;

    /**
     * The default interpolators used for the animations
     */
    private TimeInterpolator mAppearingInterpolator = sAppearingInterpolator;
    private TimeInterpolator mDisappearingInterpolator = sDisappearingInterpolator;
    private TimeInterpolator mChangingAppearingInterpolator = sChangingAppearingInterpolator;
    private TimeInterpolator mChangingDisappearingInterpolator = sChangingDisappearingInterpolator;
    private TimeInterpolator mChangingInterpolator = sChangingInterpolator;

    /**
     * These hashmaps are used to store the animations that are currently running as part of
     * the transition. The reason for this is that a further layout event should cause
     * existing animations to stop where they are prior to starting new animations. So
     * we cache all the current animations in this map for possible cancellation on
     * another layout event. LinkedHashMaps are used to preserve the order in which animations
     * are inserted, so that we process events (such as setting up start values) in the same order.
     */
    private final HashMap<View, Animator> pendingAnimations = new HashMap<>();
    private final LinkedHashMap<View, Animator> currentChangingAnimations = new LinkedHashMap<>();
    private final LinkedHashMap<View, Animator> currentAppearingAnimations = new LinkedHashMap<>();
    private final LinkedHashMap<View, Animator> currentDisappearingAnimations = new LinkedHashMap<>();

    /**
     * This hashmap is used to track the listeners that have been added to the children of
     * a container. When a layout change occurs, an animation is created for each View, so that
     * the pre-layout values can be cached in that animation. Then a listener is added to the
     * view to see whether the layout changes the bounds of that view. If so, the animation
     * is set with the final values and then run. If not, the animation is not started. When
     * the process of setting up and running all appropriate animations is done, we need to
     * remove these listeners and clear out the map.
     */
    private final HashMap<View, View.OnLayoutChangeListener> layoutChangeListenerMap = new HashMap<>();

    /**
     * Used to track the current delay being assigned to successive animations as they are
     * started. This value is incremented for each new animation, then zeroed before the next
     * transition begins.
     */
    private long staggerDelay;

    /**
     * These are the types of transition animations that the LayoutTransition is reacting
     * to. By default, appearing/disappearing and the change animations related to them are
     * enabled (not CHANGING).
     */
    private int mTransitionTypes = FLAG_CHANGE_APPEARING | FLAG_CHANGE_DISAPPEARING |
            FLAG_APPEARING | FLAG_DISAPPEARING;
    /**
     * The set of listeners that should be notified when APPEARING/DISAPPEARING transitions
     * start and end.
     */
    private ArrayList<TransitionListener> mListeners;

    /**
     * Controls whether changing animations automatically animate the parent hierarchy as well.
     * This behavior prevents artifacts when wrap_content layouts snap to the end state as the
     * transition begins, causing visual glitches and clipping.
     * Default value is true.
     */
    private boolean mAnimateParentHierarchy = true;

    /**
     * Constructs a LayoutTransition object. By default, the object will listen to layout
     * events on any ViewGroup that it is set on and will run default animations for each
     * type of layout event.
     */
    public LayoutTransition() {
        if (defaultChange == null) {
            synchronized (LayoutTransition.class) {
                if (defaultChange == null) {
                    initDefaultAnimators();
                }
            }
        }
        mChangingAppearingAnim = defaultChangeIn;
        mChangingDisappearingAnim = defaultChangeOut;
        mChangingAnim = defaultChange;
        mAppearingAnim = defaultFadeIn;
        mDisappearingAnim = defaultFadeOut;
    }

    private void initDefaultAnimators() {
        // "left" is just a placeholder; we'll put real properties/values in when needed
        PropertyValuesHolder<View, ?, ?> pvhLeft = PropertyValuesHolder.ofInt(new IntProperty<>() {
            @Override
            public void setValue(@Nonnull View target, int value) {
                target.setLeft(value);
            }

            @Override
            public Integer get(@Nonnull View target) {
                return target.getLeft();
            }
        }, 0, 1);
        PropertyValuesHolder<View, ?, ?> pvhTop = PropertyValuesHolder.ofInt(new IntProperty<>() {
            @Override
            public void setValue(@Nonnull View target, int value) {
                target.setTop(value);
            }

            @Override
            public Integer get(@Nonnull View target) {
                return target.getTop();
            }
        }, 0, 1);
        PropertyValuesHolder<View, ?, ?> pvhRight = PropertyValuesHolder.ofInt(new IntProperty<>() {
            @Override
            public void setValue(@Nonnull View target, int value) {
                target.setRight(value);
            }

            @Override
            public Integer get(@Nonnull View target) {
                return target.getRight();
            }
        }, 0, 1);
        PropertyValuesHolder<View, ?, ?> pvhBottom = PropertyValuesHolder.ofInt(new IntProperty<>() {
            @Override
            public void setValue(@Nonnull View target, int value) {
                target.setBottom(value);
            }

            @Override
            public Integer get(@Nonnull View target) {
                return target.getBottom();
            }
        }, 0, 1);
        /*PropertyValuesHolder pvhScrollX = PropertyValuesHolder.ofInt("scrollX", 0, 1);
        PropertyValuesHolder pvhScrollY = PropertyValuesHolder.ofInt("scrollY", 0, 1);*/
        defaultChangeIn = ObjectAnimator.ofPropertyValuesHolder(null,
                pvhLeft, pvhTop, pvhRight, pvhBottom);
        defaultChangeIn.setDuration(DEFAULT_DURATION);
        defaultChangeIn.setStartDelay(mChangingAppearingDelay);
        defaultChangeIn.setInterpolator(mChangingAppearingInterpolator);
        defaultChangeOut = defaultChangeIn.clone();
        defaultChangeOut.setStartDelay(mChangingDisappearingDelay);
        defaultChangeOut.setInterpolator(mChangingDisappearingInterpolator);
        defaultChange = defaultChangeIn.clone();
        defaultChange.setStartDelay(mChangingDelay);
        defaultChange.setInterpolator(mChangingInterpolator);

        FloatProperty<View> alpha = new FloatProperty<>() {
            @Override
            public void setValue(@Nonnull View target, float value) {
                target.setTransitionAlpha(value);
            }

            @Override
            public Float get(@Nonnull View target) {
                return target.getTransitionAlpha();
            }
        };

        defaultFadeIn = ObjectAnimator.ofFloat(null, alpha, 0f, 1f);
        defaultFadeIn.setDuration(DEFAULT_DURATION);
        defaultFadeIn.setStartDelay(mAppearingDelay);
        defaultFadeIn.setInterpolator(mAppearingInterpolator);
        defaultFadeOut = ObjectAnimator.ofFloat(null, alpha, 1f, 0f);
        defaultFadeOut.setDuration(DEFAULT_DURATION);
        defaultFadeOut.setStartDelay(mDisappearingDelay);
        defaultFadeOut.setInterpolator(mDisappearingInterpolator);
    }

    /**
     * Sets the duration to be used by all animations of this transition object. If you want to
     * set the duration of just one of the animations in particular, use the
     * {@link #setDuration(int, long)} method.
     *
     * @param duration The length of time, in milliseconds, that the transition animations
     *                 should last.
     */
    public void setDuration(long duration) {
        mChangingAppearingDuration = duration;
        mChangingDisappearingDuration = duration;
        mChangingDuration = duration;
        mAppearingDuration = duration;
        mDisappearingDuration = duration;
    }

    /**
     * Enables the specified transitionType for this LayoutTransition object.
     * By default, a LayoutTransition listens for changes in children being
     * added/remove/hidden/shown in the container, and runs the animations associated with
     * those events. That is, all transition types besides {@link #CHANGING} are enabled by default.
     * You can also enable {@link #CHANGING} animations by calling this method with the
     * {@link #CHANGING} transitionType.
     *
     * @param transitionType One of {@link #CHANGE_APPEARING}, {@link #CHANGE_DISAPPEARING},
     *                       {@link #CHANGING}, {@link #APPEARING}, or {@link #DISAPPEARING}.
     */
    public void enableTransitionType(int transitionType) {
        switch (transitionType) {
            case APPEARING -> mTransitionTypes |= FLAG_APPEARING;
            case DISAPPEARING -> mTransitionTypes |= FLAG_DISAPPEARING;
            case CHANGE_APPEARING -> mTransitionTypes |= FLAG_CHANGE_APPEARING;
            case CHANGE_DISAPPEARING -> mTransitionTypes |= FLAG_CHANGE_DISAPPEARING;
            case CHANGING -> mTransitionTypes |= FLAG_CHANGING;
        }
    }

    /**
     * Disables the specified transitionType for this LayoutTransition object.
     * By default, all transition types except {@link #CHANGING} are enabled.
     *
     * @param transitionType One of {@link #CHANGE_APPEARING}, {@link #CHANGE_DISAPPEARING},
     *                       {@link #CHANGING}, {@link #APPEARING}, or {@link #DISAPPEARING}.
     */
    public void disableTransitionType(int transitionType) {
        switch (transitionType) {
            case APPEARING -> mTransitionTypes &= ~FLAG_APPEARING;
            case DISAPPEARING -> mTransitionTypes &= ~FLAG_DISAPPEARING;
            case CHANGE_APPEARING -> mTransitionTypes &= ~FLAG_CHANGE_APPEARING;
            case CHANGE_DISAPPEARING -> mTransitionTypes &= ~FLAG_CHANGE_DISAPPEARING;
            case CHANGING -> mTransitionTypes &= ~FLAG_CHANGING;
        }
    }

    /**
     * Returns whether the specified transitionType is enabled for this LayoutTransition object.
     * By default, all transition types except {@link #CHANGING} are enabled.
     *
     * @param transitionType One of {@link #CHANGE_APPEARING}, {@link #CHANGE_DISAPPEARING},
     *                       {@link #CHANGING}, {@link #APPEARING}, or {@link #DISAPPEARING}.
     * @return true if the specified transitionType is currently enabled, false otherwise.
     */
    public boolean isTransitionTypeEnabled(int transitionType) {
        return switch (transitionType) {
            case APPEARING -> (mTransitionTypes & FLAG_APPEARING) == FLAG_APPEARING;
            case DISAPPEARING -> (mTransitionTypes & FLAG_DISAPPEARING) == FLAG_DISAPPEARING;
            case CHANGE_APPEARING -> (mTransitionTypes & FLAG_CHANGE_APPEARING) == FLAG_CHANGE_APPEARING;
            case CHANGE_DISAPPEARING -> (mTransitionTypes & FLAG_CHANGE_DISAPPEARING) == FLAG_CHANGE_DISAPPEARING;
            case CHANGING -> (mTransitionTypes & FLAG_CHANGING) == FLAG_CHANGING;
            default -> false;
        };
    }

    /**
     * Sets the start delay on one of the animation objects used by this transition. The
     * <code>transitionType</code> parameter determines the animation whose start delay
     * is being set.
     *
     * @param transitionType One of {@link #CHANGE_APPEARING}, {@link #CHANGE_DISAPPEARING},
     *                       {@link #CHANGING}, {@link #APPEARING}, or {@link #DISAPPEARING}, which determines
     *                       the animation whose start delay is being set.
     * @param delay          The length of time, in milliseconds, to delay before starting the animation.
     * @see Animator#setStartDelay(long)
     */
    public void setStartDelay(int transitionType, long delay) {
        switch (transitionType) {
            case CHANGE_APPEARING -> mChangingAppearingDelay = delay;
            case CHANGE_DISAPPEARING -> mChangingDisappearingDelay = delay;
            case CHANGING -> mChangingDelay = delay;
            case APPEARING -> mAppearingDelay = delay;
            case DISAPPEARING -> mDisappearingDelay = delay;
        }
    }

    /**
     * Gets the start delay on one of the animation objects used by this transition. The
     * <code>transitionType</code> parameter determines the animation whose start delay
     * is returned.
     *
     * @param transitionType One of {@link #CHANGE_APPEARING}, {@link #CHANGE_DISAPPEARING},
     *                       {@link #CHANGING}, {@link #APPEARING}, or {@link #DISAPPEARING}, which determines
     *                       the animation whose start delay is returned.
     * @return long The start delay of the specified animation.
     * @see Animator#getStartDelay()
     */
    public long getStartDelay(int transitionType) {
        return switch (transitionType) {
            case CHANGE_APPEARING -> mChangingAppearingDelay;
            case CHANGE_DISAPPEARING -> mChangingDisappearingDelay;
            case CHANGING -> mChangingDelay;
            case APPEARING -> mAppearingDelay;
            case DISAPPEARING -> mDisappearingDelay;
            default -> 0;
        };
    }

    /**
     * Sets the duration on one of the animation objects used by this transition. The
     * <code>transitionType</code> parameter determines the animation whose duration
     * is being set.
     *
     * @param transitionType One of {@link #CHANGE_APPEARING}, {@link #CHANGE_DISAPPEARING},
     *                       {@link #CHANGING}, {@link #APPEARING}, or {@link #DISAPPEARING}, which determines
     *                       the animation whose duration is being set.
     * @param duration       The length of time, in milliseconds, that the specified animation should run.
     * @see Animator#setDuration(long)
     */
    public void setDuration(int transitionType, long duration) {
        switch (transitionType) {
            case CHANGE_APPEARING -> mChangingAppearingDuration = duration;
            case CHANGE_DISAPPEARING -> mChangingDisappearingDuration = duration;
            case CHANGING -> mChangingDuration = duration;
            case APPEARING -> mAppearingDuration = duration;
            case DISAPPEARING -> mDisappearingDuration = duration;
        }
    }

    /**
     * Gets the duration on one of the animation objects used by this transition. The
     * <code>transitionType</code> parameter determines the animation whose duration
     * is returned.
     *
     * @param transitionType One of {@link #CHANGE_APPEARING}, {@link #CHANGE_DISAPPEARING},
     *                       {@link #CHANGING}, {@link #APPEARING}, or {@link #DISAPPEARING}, which determines
     *                       the animation whose duration is returned.
     * @return long The duration of the specified animation.
     * @see Animator#getDuration()
     */
    public long getDuration(int transitionType) {
        return switch (transitionType) {
            case CHANGE_APPEARING -> mChangingAppearingDuration;
            case CHANGE_DISAPPEARING -> mChangingDisappearingDuration;
            case CHANGING -> mChangingDuration;
            case APPEARING -> mAppearingDuration;
            case DISAPPEARING -> mDisappearingDuration;
            default -> 0;
        };
    }

    /**
     * Sets the length of time to delay between starting each animation during one of the
     * change animations.
     *
     * @param transitionType A value of {@link #CHANGE_APPEARING}, {@link #CHANGE_DISAPPEARING}, or
     *                       {@link #CHANGING}.
     * @param duration       The length of time, in milliseconds, to delay before launching the next
     *                       animation in the sequence.
     */
    public void setStagger(int transitionType, long duration) {
        switch (transitionType) {
            case CHANGE_APPEARING -> mChangingAppearingStagger = duration;
            case CHANGE_DISAPPEARING -> mChangingDisappearingStagger = duration;
            case CHANGING -> mChangingStagger = duration;
            // noop other cases
        }
    }

    /**
     * Gets the length of time to delay between starting each animation during one of the
     * change animations.
     *
     * @param transitionType A value of {@link #CHANGE_APPEARING}, {@link #CHANGE_DISAPPEARING}, or
     *                       {@link #CHANGING}.
     * @return long The length of time, in milliseconds, to delay before launching the next
     * animation in the sequence.
     */
    public long getStagger(int transitionType) {
        return switch (transitionType) {
            case CHANGE_APPEARING -> mChangingAppearingStagger;
            case CHANGE_DISAPPEARING -> mChangingDisappearingStagger;
            case CHANGING -> mChangingStagger;
            default -> 0;
        };
    }

    /**
     * Sets the interpolator on one of the animation objects used by this transition. The
     * <code>transitionType</code> parameter determines the animation whose interpolator
     * is being set.
     *
     * @param transitionType One of {@link #CHANGE_APPEARING}, {@link #CHANGE_DISAPPEARING},
     *                       {@link #CHANGING}, {@link #APPEARING}, or {@link #DISAPPEARING}, which determines
     *                       the animation whose interpolator is being set.
     * @param interpolator   The interpolator that the specified animation should use.
     * @see Animator#setInterpolator(TimeInterpolator)
     */
    public void setInterpolator(int transitionType, TimeInterpolator interpolator) {
        switch (transitionType) {
            case CHANGE_APPEARING -> mChangingAppearingInterpolator = interpolator;
            case CHANGE_DISAPPEARING -> mChangingDisappearingInterpolator = interpolator;
            case CHANGING -> mChangingInterpolator = interpolator;
            case APPEARING -> mAppearingInterpolator = interpolator;
            case DISAPPEARING -> mDisappearingInterpolator = interpolator;
        }
    }

    /**
     * Gets the interpolator on one of the animation objects used by this transition. The
     * <code>transitionType</code> parameter determines the animation whose interpolator
     * is returned.
     *
     * @param transitionType One of {@link #CHANGE_APPEARING}, {@link #CHANGE_DISAPPEARING},
     *                       {@link #CHANGING}, {@link #APPEARING}, or {@link #DISAPPEARING}, which determines
     *                       the animation whose interpolator is being returned.
     * @return TimeInterpolator The interpolator that the specified animation uses.
     * @see Animator#setInterpolator(TimeInterpolator)
     */
    public TimeInterpolator getInterpolator(int transitionType) {
        return switch (transitionType) {
            case CHANGE_APPEARING -> mChangingAppearingInterpolator;
            case CHANGE_DISAPPEARING -> mChangingDisappearingInterpolator;
            case CHANGING -> mChangingInterpolator;
            case APPEARING -> mAppearingInterpolator;
            case DISAPPEARING -> mDisappearingInterpolator;
            default -> null;
        };
    }

    /**
     * Sets the animation used during one of the transition types that may run. Any
     * Animator object can be used, but to be most useful in the context of layout
     * transitions, the animation should either be a ObjectAnimator or a AnimatorSet
     * of animations including PropertyAnimators. Also, these ObjectAnimator objects
     * should be able to get and set values on their target objects automatically. For
     * example, a ObjectAnimator that animates the property "left" is able to set and get the
     * <code>left</code> property from the View objects being animated by the layout
     * transition. The transition works by setting target objects and properties
     * dynamically, according to the pre- and post-layout values of those objects, so
     * having animations that can handle those properties appropriately will work best
     * for custom animation. The dynamic setting of values is only the case for the
     * CHANGE animations; the APPEARING and DISAPPEARING animations are simply run with
     * the values they have.
     *
     * <p>It is also worth noting that any and all animations (and their underlying
     * PropertyValuesHolder objects) will have their start and end values set according
     * to the pre- and post-layout values. So, for example, a custom animation on "alpha"
     * as the CHANGE_APPEARING animation will inherit the real value of alpha on the target
     * object (presumably 1) as its starting and ending value when the animation begins.
     * Animations which need to use values at the beginning and end that may not match the
     * values queried when the transition begins may need to use a different mechanism
     * than a standard ObjectAnimator object.</p>
     *
     * @param transitionType One of {@link #CHANGE_APPEARING}, {@link #CHANGE_DISAPPEARING},
     *                       {@link #CHANGING}, {@link #APPEARING}, or {@link #DISAPPEARING}, which determines the
     *                       animation whose animator is being set.
     * @param animator       The animation being assigned. A value of <code>null</code> means that no
     *                       animation will be run for the specified transitionType.
     */
    public void setAnimator(int transitionType, Animator animator) {
        switch (transitionType) {
            case CHANGE_APPEARING -> mChangingAppearingAnim = animator;
            case CHANGE_DISAPPEARING -> mChangingDisappearingAnim = animator;
            case CHANGING -> mChangingAnim = animator;
            case APPEARING -> mAppearingAnim = animator;
            case DISAPPEARING -> mDisappearingAnim = animator;
        }
    }

    /**
     * Gets the animation used during one of the transition types that may run.
     *
     * @param transitionType One of {@link #CHANGE_APPEARING}, {@link #CHANGE_DISAPPEARING},
     *                       {@link #CHANGING}, {@link #APPEARING}, or {@link #DISAPPEARING}, which determines
     *                       the animation whose animator is being returned.
     * @return Animator The animation being used for the given transition type.
     * @see #setAnimator(int, Animator)
     */
    public Animator getAnimator(int transitionType) {
        return switch (transitionType) {
            case CHANGE_APPEARING -> mChangingAppearingAnim;
            case CHANGE_DISAPPEARING -> mChangingDisappearingAnim;
            case CHANGING -> mChangingAnim;
            case APPEARING -> mAppearingAnim;
            case DISAPPEARING -> mDisappearingAnim;
            default -> null;
        };
    }

    /**
     * Utility function called by runChangingTransition for both the children and the parent
     * hierarchy.
     */
    private void setupChangeAnimation(final ViewGroup parent, final int changeReason,
                                      Animator baseAnimator, final long duration, final View child) {

        // If we already have a listener for this child, then we've already set up the
        // changing animation we need. Multiple calls for a child may occur when several
        // add/remove operations are run at once on a container; each one will trigger
        // changes for the existing children in the container.
        if (layoutChangeListenerMap.get(child) != null) {
            return;
        }

        // Don't animate items up from size(0,0); this is likely because the objects
        // were offscreen/invisible or otherwise measured to be infinitely small. We don't
        // want to see them animate into their real size; just ignore animation requests
        // on these views
        if (child.getWidth() == 0 && child.getHeight() == 0) {
            return;
        }

        // Make a copy of the appropriate animation
        final Animator anim = baseAnimator.clone();

        // Set the target object for the animation
        anim.setTarget(child);

        // A ObjectAnimator (or AnimatorSet of them) can extract start values from
        // its target object
        anim.setupStartValues();

        // If there's an animation running on this view already, cancel it
        Animator currentAnimation = pendingAnimations.get(child);
        if (currentAnimation != null) {
            currentAnimation.cancel();
            pendingAnimations.remove(child);
        }
        // Cache the animation in case we need to cancel it later
        pendingAnimations.put(child, anim);

        // For the animations which don't get started, we have to have a means of
        // removing them from the cache, lest we leak them and their target objects.
        // We run an animator for the default duration+100 (an arbitrary time, but one
        // which should far surpass the delay between setting them up here and
        // handling layout events which start them.
        /*ValueAnimator pendingAnimRemover = ValueAnimator.ofFloat(0f, 1f).
                setDuration(duration + 100);
        pendingAnimRemover.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                pendingAnimations.remove(child);
            }
        });
        pendingAnimRemover.start();*/
        child.postOnAnimationDelayed(() -> pendingAnimations.remove(child), duration + 100);

        // Add a listener to track layout changes on this view. If we don't get a callback,
        // then there's nothing to animate.
        final View.OnLayoutChangeListener listener = new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {

                // Tell the animation to extract end values from the changed object
                anim.setupEndValues();
                if (anim instanceof ObjectAnimator valueAnim) {
                    boolean valuesDiffer = false;
                    PropertyValuesHolder<Object, ?, ?>[] oldValues = valueAnim.getValues();
                    for (PropertyValuesHolder<Object, ?, ?> pvh : oldValues) {
                        if (pvh.mKeyframes instanceof KeyframeSet) {
                            Keyframe[] keyframes = ((KeyframeSet<?>) pvh.mKeyframes).mKeyframes;
                            if (!keyframes[0].getValue().equals(
                                    keyframes[keyframes.length - 1].getValue())) {
                                valuesDiffer = true;
                            }
                        } else if (!pvh.mKeyframes.getValue(0).equals(pvh.mKeyframes.getValue(1))) {
                            valuesDiffer = true;
                        }
                    }
                    if (!valuesDiffer) {
                        return;
                    }
                }

                long startDelay = 0;
                switch (changeReason) {
                    case APPEARING -> {
                        startDelay = mChangingAppearingDelay + staggerDelay;
                        staggerDelay += mChangingAppearingStagger;
                        if (mChangingAppearingInterpolator != sChangingAppearingInterpolator) {
                            anim.setInterpolator(mChangingAppearingInterpolator);
                        }
                    }
                    case DISAPPEARING -> {
                        startDelay = mChangingDisappearingDelay + staggerDelay;
                        staggerDelay += mChangingDisappearingStagger;
                        if (mChangingDisappearingInterpolator !=
                                sChangingDisappearingInterpolator) {
                            anim.setInterpolator(mChangingDisappearingInterpolator);
                        }
                    }
                    case CHANGING -> {
                        startDelay = mChangingDelay + staggerDelay;
                        staggerDelay += mChangingStagger;
                        if (mChangingInterpolator != sChangingInterpolator) {
                            anim.setInterpolator(mChangingInterpolator);
                        }
                    }
                }
                anim.setStartDelay(startDelay);
                anim.setDuration(duration);

                Animator prevAnimation = currentChangingAnimations.get(child);
                if (prevAnimation != null) {
                    prevAnimation.cancel();
                }
                Animator pendingAnimation = pendingAnimations.get(child);
                if (pendingAnimation != null) {
                    pendingAnimations.remove(child);
                }
                // Cache the animation in case we need to cancel it later
                currentChangingAnimations.put(child, anim);

                //FIXME?
                //parent.requestTransitionStart(LayoutTransition.this);

                // this only removes listeners whose views changed - must clear the
                // other listeners later
                child.removeOnLayoutChangeListener(this);
                layoutChangeListenerMap.remove(child);
            }
        };
        // Remove the animation from the cache when it ends
        anim.addListener(new Animator.AnimatorListener() {

            @SuppressWarnings("unchecked")
            @Override
            public void onAnimationStart(@Nonnull Animator animator, boolean reverse) {
                if (hasListeners()) {
                    ArrayList<TransitionListener> listeners =
                            (ArrayList<TransitionListener>) mListeners.clone();
                    for (TransitionListener listener : listeners) {
                        listener.startTransition(LayoutTransition.this, parent, child,
                                changeReason == APPEARING ?
                                        CHANGE_APPEARING : changeReason == DISAPPEARING ?
                                        CHANGE_DISAPPEARING : CHANGING);
                    }
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            public void onAnimationEnd(@Nonnull Animator animator, boolean reverse) {
                currentChangingAnimations.remove(child);
                if (hasListeners()) {
                    ArrayList<TransitionListener> listeners =
                            (ArrayList<TransitionListener>) mListeners.clone();
                    for (TransitionListener listener : listeners) {
                        listener.endTransition(LayoutTransition.this, parent, child,
                                changeReason == APPEARING ?
                                        CHANGE_APPEARING : changeReason == DISAPPEARING ?
                                        CHANGE_DISAPPEARING : CHANGING);
                    }
                }
            }

            @Override
            public void onAnimationCancel(@Nonnull Animator animator) {
                child.removeOnLayoutChangeListener(listener);
                layoutChangeListenerMap.remove(child);
            }
        });

        child.addOnLayoutChangeListener(listener);
        // cache the listener for later removal
        layoutChangeListenerMap.put(child, listener);
    }

    /**
     * Returns true if animations are running which animate layout-related properties. This
     * essentially means that either CHANGE_APPEARING or CHANGE_DISAPPEARING animations
     * are running, since these animations operate on layout-related properties.
     *
     * @return true if CHANGE_APPEARING or CHANGE_DISAPPEARING animations are currently
     * running.
     */
    public boolean isChangingLayout() {
        return (currentChangingAnimations.size() > 0);
    }

    /**
     * Returns true if any of the animations in this transition are currently running.
     *
     * @return true if any animations in the transition are running.
     */
    public boolean isRunning() {
        return (currentChangingAnimations.size() > 0 || currentAppearingAnimations.size() > 0 ||
                currentDisappearingAnimations.size() > 0);
    }

    private boolean hasListeners() {
        return mListeners != null && mListeners.size() > 0;
    }

    /**
     * Add a listener that will be called when the bounds of the view change due to
     * layout processing.
     *
     * @param listener The listener that will be called when layout bounds change.
     */
    public void addTransitionListener(TransitionListener listener) {
        if (mListeners == null) {
            mListeners = new ArrayList<>();
        }
        mListeners.add(listener);
    }

    /**
     * Remove a listener for layout changes.
     *
     * @param listener The listener for layout bounds change.
     */
    public void removeTransitionListener(TransitionListener listener) {
        if (mListeners == null) {
            return;
        }
        mListeners.remove(listener);
    }

    /**
     * Gets the current list of listeners for layout changes.
     */
    @Nullable
    public List<TransitionListener> getTransitionListeners() {
        return mListeners;
    }

    /**
     * This interface is used for listening to starting and ending events for transitions.
     */
    public interface TransitionListener {

        /**
         * This event is sent to listeners when any type of transition animation begins.
         *
         * @param transition     The LayoutTransition sending out the event.
         * @param container      The ViewGroup on which the transition is playing.
         * @param view           The View object being affected by the transition animation.
         * @param transitionType The type of transition that is beginning,
         *                       {@link LayoutTransition#APPEARING},
         *                       {@link LayoutTransition#DISAPPEARING},
         *                       {@link LayoutTransition#CHANGE_APPEARING}, or
         *                       {@link LayoutTransition#CHANGE_DISAPPEARING}.
         */
        void startTransition(LayoutTransition transition, ViewGroup container,
                             View view, int transitionType);

        /**
         * This event is sent to listeners when any type of transition animation ends.
         *
         * @param transition     The LayoutTransition sending out the event.
         * @param container      The ViewGroup on which the transition is playing.
         * @param view           The View object being affected by the transition animation.
         * @param transitionType The type of transition that is ending,
         *                       {@link LayoutTransition#APPEARING},
         *                       {@link LayoutTransition#DISAPPEARING},
         *                       {@link LayoutTransition#CHANGE_APPEARING}, or
         *                       {@link LayoutTransition#CHANGE_DISAPPEARING}.
         */
        void endTransition(LayoutTransition transition, ViewGroup container,
                           View view, int transitionType);
    }
}
