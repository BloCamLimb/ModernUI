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

import icyllis.modernui.animation.Animator;
import icyllis.modernui.math.Rect;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

//TODO transition
public abstract class Transition implements Cloneable {

    ArrayList<View> mTargets = new ArrayList<>();

    // The set of listeners to be sent transition lifecycle events.
    private ArrayList<TransitionListener> mListeners = null;

    // The rectangular region for Transitions like Explode and TransitionPropagations
    // like CircularPropagation
    private EpicenterCallback mEpicenterCallback;

    public Transition() {
    }

    /**
     * This method creates an animation that will be run for this transition
     * given the information in the startValues and endValues structures captured
     * earlier for the start and end scenes. Subclasses of Transition should override
     * this method. The method should only be called by the transition system; it is
     * not intended to be called from external classes.
     *
     * <p>This method is called by the transition's parent (all the way up to the
     * topmost Transition in the hierarchy) with the sceneRoot and start/end
     * values that the transition may need to set up initial target values
     * and construct an appropriate animation. For example, if an overall
     * Transition is a {@link TransitionSet} consisting of several
     * child transitions in sequence, then some of the child transitions may
     * want to set initial values on target views prior to the overall
     * Transition commencing, to put them in an appropriate state for the
     * delay between that start and the child Transition start time. For
     * example, a transition that fades an item in may wish to set the starting
     * alpha value to 0, to avoid it blinking in prior to the transition
     * actually starting the animation. This is necessary because the scene
     * change that triggers the Transition will automatically set the end-scene
     * on all target views, so a Transition that wants to animate from a
     * different value should set that value prior to returning from this method.</p>
     *
     * <p>Additionally, a Transition can perform logic to determine whether
     * the transition needs to run on the given target and start/end values.
     * For example, a transition that resizes objects on the screen may wish
     * to avoid running for views which are not present in either the start
     * or end scenes.</p>
     *
     * <p>If there is an animator created and returned from this method, the
     * transition mechanism will apply any applicable duration, startDelay,
     * and interpolator to that animation and start it. A return value of
     * <code>null</code> indicates that no animation should run. The default
     * implementation returns null.</p>
     *
     * <p>The method is called for every applicable target object, which is
     * stored in the {@link TransitionValues#view} field.</p>
     *
     * @param sceneRoot   The root of the transition hierarchy.
     * @param startValues The values for a specific target in the start scene.
     * @param endValues   The values for the target in the end scene.
     * @return A Animator to be started at the appropriate time in the
     * overall transition for this scene change. A null value means no animation
     * should be run.
     */
    @Nullable
    public Animator createAnimator(@Nonnull ViewGroup sceneRoot,
                                   @Nullable TransitionValues startValues, @Nullable TransitionValues endValues) {
        return null;
    }

    /**
     * Sets the target view instances that this Transition is interested in
     * animating. By default, there are no targets, and a Transition will
     * listen for changes on every view in the hierarchy below the sceneRoot
     * of the Scene being transitioned into. Setting targets constrains
     * the Transition to only listen for, and act on, these views.
     * All other views will be ignored.
     *
     * <p>The target list is like the {@link #addTarget(int) targetId}
     * list except this list specifies the actual View instances, not the ids
     * of the views. This is an important distinction when scene changes involve
     * view hierarchies which have been inflated separately; different views may
     * share the same id but not actually be the same instance. If the transition
     * should treat those views as the same, then {@link #addTarget(int)} should be used
     * instead of this method. If, on the other hand, scene changes involve
     * changes all within the same view hierarchy, among views which do not
     * necessarily have ids set on them, then the target list of views may be more
     * convenient.</p>
     *
     * @param target A View on which the Transition will act, must be non-null.
     * @return The Transition to which the target is added.
     * Returning the same object makes it easier to chain calls during
     * construction, such as
     * <code>transitionSet.addTransitions(new Fade()).addTarget(someView);</code>
     * @see #addTarget(int)
     */
    @Nonnull
    public Transition addTarget(@Nonnull View target) {
        mTargets.add(target);
        return this;
    }

    /**
     * Adds a listener to the set of listeners that are sent events through the
     * life of an animation, such as start, repeat, and end.
     *
     * @param listener the listener to be added to the current set of listeners
     *                 for this animation.
     * @return This transition object.
     */
    @Nonnull
    public Transition addListener(@Nonnull TransitionListener listener) {
        if (mListeners == null) {
            mListeners = new ArrayList<>();
        }
        mListeners.add(listener);
        return this;
    }

    /**
     * Removes a listener from the set listening to this animation.
     *
     * @param listener the listener to be removed from the current set of
     *                 listeners for this transition.
     * @return This transition object.
     */
    @Nonnull
    public Transition removeListener(@Nonnull TransitionListener listener) {
        if (mListeners == null) {
            return this;
        }
        mListeners.remove(listener);
        if (mListeners.size() == 0) {
            mListeners = null;
        }
        return this;
    }

    /**
     * Sets the callback to use to find the epicenter of a Transition. A null value indicates
     * that there is no epicenter in the Transition and onGetEpicenter() will return null.
     * Transitions like {@link Explode} use a point or Rect to orient
     * the direction of travel. This is called the epicenter of the Transition and is
     * typically centered on a touched View. The
     * {@link EpicenterCallback} allows a Transition to
     * dynamically retrieve the epicenter during a Transition.
     *
     * @param epicenterCallback The callback to use to find the epicenter of the Transition.
     */
    public void setEpicenterCallback(@Nullable EpicenterCallback epicenterCallback) {
        mEpicenterCallback = epicenterCallback;
    }

    /**
     * Returns the callback used to find the epicenter of the Transition.
     * Transitions like {@link Explode} use a point or Rect to orient
     * the direction of travel. This is called the epicenter of the Transition and is
     * typically centered on a touched View. The
     * {@link EpicenterCallback} allows a Transition to
     * dynamically retrieve the epicenter during a Transition.
     *
     * @return the callback used to find the epicenter of the Transition.
     */
    @Nullable
    public EpicenterCallback getEpicenterCallback() {
        return mEpicenterCallback;
    }

    /**
     * Returns the epicenter as specified by the
     * {@link EpicenterCallback} or null if no callback exists.
     *
     * @return the epicenter as specified by the
     * {@link EpicenterCallback} or null if no callback exists.
     * @see #setEpicenterCallback(EpicenterCallback)
     */
    @Nullable
    public Rect getEpicenter() {
        if (mEpicenterCallback == null) {
            return null;
        }
        return mEpicenterCallback.onGetEpicenter(this);
    }

    @Override
    public Transition clone() {
        try {
            Transition clone = (Transition) super.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * Interface to get the epicenter of Transition. Use
     * {@link #setEpicenterCallback(EpicenterCallback)} to set the callback used to calculate the
     * epicenter of the Transition. Override {@link #getEpicenter()} to return the rectangular
     * region in screen coordinates of the epicenter of the transition.
     *
     * @see #setEpicenterCallback(EpicenterCallback)
     */
    @FunctionalInterface
    public interface EpicenterCallback {

        /**
         * Implementers must override to return the epicenter of the Transition in screen
         * coordinates. Transitions like {@link Explode} depend upon
         * an epicenter for the Transition. In Explode, Views move toward or away from the
         * center of the epicenter Rect along the vector between the epicenter and the center
         * of the View appearing and disappearing. Some Transitions, such as
         * {@link Fade} pay no attention to the epicenter.
         *
         * @param transition The transition for which the epicenter applies.
         * @return The Rect region of the epicenter of <code>transition</code> or null if
         * there is no epicenter.
         */
        Rect onGetEpicenter(@Nonnull Transition transition);
    }
}
