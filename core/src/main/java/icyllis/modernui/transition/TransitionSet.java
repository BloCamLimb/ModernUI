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

import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

/**
 * A TransitionSet is a parent of child transitions (including other
 * TransitionSets). Using TransitionSets enables more complex
 * choreography of transitions, where some sets play {@link #ORDERING_TOGETHER} and
 * others play {@link #ORDERING_SEQUENTIAL}. For example, {@link AutoTransition}
 * uses a TransitionSet to sequentially play a Fade(Fade.OUT), followed by
 * a {@link ChangeBounds}, followed by a Fade(Fade.OUT) transition.
 */
public class TransitionSet extends Transition {

    /**
     * Flag indicating the interpolator changed.
     */
    private static final int FLAG_CHANGE_INTERPOLATOR = 0x01;
    /**
     * Flag indicating the propagation changed.
     */
    private static final int FLAG_CHANGE_PROPAGATION = 0x02;
    /**
     * Flag indicating the path motion changed.
     */
    private static final int FLAG_CHANGE_PATH_MOTION = 0x04;
    /**
     * Flag indicating the epicenter callback changed.
     */
    static final int FLAG_CHANGE_EPICENTER = 0x08;

    private ArrayList<Transition> mTransitions = new ArrayList<>();
    private boolean mPlayTogether = true;
    int mCurrentListeners;
    boolean mStarted = false;
    // Flags to know whether the interpolator, path motion, epicenter, propagation
    // have changed
    private int mChangeFlags = 0;

    /**
     * A flag used to indicate that the child transitions of this set
     * should all start at the same time.
     */
    public static final int ORDERING_TOGETHER = 0;
    /**
     * A flag used to indicate that the child transitions of this set should
     * play in sequence; when one child transition ends, the next child
     * transition begins. Note that a transition does not end until all
     * instances of it (which are playing on all applicable targets of the
     * transition) end.
     */
    public static final int ORDERING_SEQUENTIAL = 1;

    /**
     * Constructs an empty transition set. Add child transitions to the
     * set by calling {@link #addTransition(Transition)} )}. By default,
     * child transitions will play {@link #ORDERING_TOGETHER together}.
     */
    public TransitionSet() {
    }

    /**
     * Sets the play order of this set's child transitions.
     *
     * @param ordering {@link #ORDERING_TOGETHER} to play this set's child
     *                 transitions together, {@link #ORDERING_SEQUENTIAL} to play the child
     *                 transitions in sequence.
     * @return This transitionSet object.
     */
    @Nonnull
    public TransitionSet setOrdering(int ordering) {
        switch (ordering) {
            case ORDERING_SEQUENTIAL -> mPlayTogether = false;
            case ORDERING_TOGETHER -> mPlayTogether = true;
            default -> throw new IllegalArgumentException("Invalid parameter for TransitionSet " +
                    "ordering: " + ordering);
        }
        return this;
    }

    /**
     * Returns the ordering of this TransitionSet. By default, the value is
     * {@link #ORDERING_TOGETHER}.
     *
     * @return {@link #ORDERING_TOGETHER} if child transitions will play at the same
     * time, {@link #ORDERING_SEQUENTIAL} if they will play in sequence.
     * @see #setOrdering(int)
     */
    public int getOrdering() {
        return mPlayTogether ? ORDERING_TOGETHER : ORDERING_SEQUENTIAL;
    }

    /**
     * Adds child transition to this set. The order in which this child transition
     * is added relative to other child transitions that are added, in addition to
     * the {@link #getOrdering() ordering} property, determines the
     * order in which the transitions are started.
     *
     * <p>If this transitionSet has a {@link #getDuration() duration},
     * {@link #getInterpolator() interpolator}, {@link #getPropagation() propagation delay},
     * {@link #getPathMotion() path motion}, or
     * {@link #setEpicenterCallback(EpicenterCallback) epicenter callback}
     * set on it, the child transition will inherit the values that are set.
     * Transitions are assumed to have a maximum of one transitionSet parent.</p>
     *
     * @param transition A non-null child transition to be added to this set.
     * @return This transitionSet object.
     */
    @Nonnull
    public TransitionSet addTransition(@Nonnull Transition transition) {
        addTransitionInternal(transition);
        if (mDuration >= 0) {
            transition.setDuration(mDuration);
        }
        if ((mChangeFlags & FLAG_CHANGE_INTERPOLATOR) != 0) {
            transition.setInterpolator(getInterpolator());
        }
        if ((mChangeFlags & FLAG_CHANGE_PROPAGATION) != 0) {
            transition.setPropagation(getPropagation());
        }
        if ((mChangeFlags & FLAG_CHANGE_PATH_MOTION) != 0) {
            //transition.setPathMotion(getPathMotion());
        }
        if ((mChangeFlags & FLAG_CHANGE_EPICENTER) != 0) {
            transition.setEpicenterCallback(getEpicenterCallback());
        }
        return this;
    }

    private void addTransitionInternal(@Nonnull Transition transition) {
        mTransitions.add(transition);
        transition.mParent = this;
    }

    /**
     * Returns the number of child transitions in the TransitionSet.
     *
     * @return The number of child transitions in the TransitionSet.
     * @see #addTransition(Transition)
     * @see #getTransitionAt(int)
     */
    public int getTransitionCount() {
        return mTransitions.size();
    }

    /**
     * Returns the child Transition at the specified position in the TransitionSet.
     *
     * @param index The position of the Transition to retrieve.
     * @see #addTransition(Transition)
     * @see #getTransitionCount()
     */
    @Nullable
    public Transition getTransitionAt(int index) {
        if (index < 0 || index >= mTransitions.size()) {
            return null;
        }
        return mTransitions.get(index);
    }

    /**
     * Setting a non-negative duration on a TransitionSet causes all of the child
     * transitions (current and future) to inherit this duration.
     *
     * @param duration The length of the animation, in milliseconds.
     * @return This transitionSet object.
     */
    @Nonnull
    @Override
    public TransitionSet setDuration(long duration) {
        super.setDuration(duration);
        if (mDuration >= 0 && mTransitions != null) {
            for (Transition transition : mTransitions) {
                transition.setDuration(duration);
            }
        }
        return this;
    }

    @Nonnull
    @Override
    public TransitionSet setStartDelay(long startDelay) {
        return (TransitionSet) super.setStartDelay(startDelay);
    }

    @Nonnull
    @Override
    public TransitionSet setInterpolator(@Nullable TimeInterpolator interpolator) {
        mChangeFlags |= FLAG_CHANGE_INTERPOLATOR;
        if (mTransitions != null) {
            for (Transition transition : mTransitions) {
                transition.setInterpolator(interpolator);
            }
        }
        return (TransitionSet) super.setInterpolator(interpolator);
    }

    @Nonnull
    @Override
    public TransitionSet addTarget(@Nonnull View target) {
        for (Transition transition : mTransitions) {
            transition.addTarget(target);
        }
        return (TransitionSet) super.addTarget(target);
    }

    @Nonnull
    @Override
    public TransitionSet addTarget(int targetId) {
        for (Transition transition : mTransitions) {
            transition.addTarget(targetId);
        }
        return (TransitionSet) super.addTarget(targetId);
    }

    @Nonnull
    @Override
    public TransitionSet addTarget(@Nonnull String targetName) {
        for (Transition transition : mTransitions) {
            transition.addTarget(targetName);
        }
        return (TransitionSet) super.addTarget(targetName);
    }

    @Nonnull
    @Override
    public TransitionSet addTarget(@Nonnull Class<?> targetType) {
        for (Transition transition : mTransitions) {
            transition.addTarget(targetType);
        }
        return (TransitionSet) super.addTarget(targetType);
    }

    @Nonnull
    @Override
    public TransitionSet addListener(@Nonnull TransitionListener listener) {
        return (TransitionSet) super.addListener(listener);
    }

    @Nonnull
    @Override
    public TransitionSet removeTarget(int targetId) {
        for (Transition transition : mTransitions) {
            transition.removeTarget(targetId);
        }
        return (TransitionSet) super.removeTarget(targetId);
    }

    @Nonnull
    @Override
    public TransitionSet removeTarget(@Nonnull View target) {
        for (Transition transition : mTransitions) {
            transition.removeTarget(target);
        }
        return (TransitionSet) super.removeTarget(target);
    }

    @Nonnull
    @Override
    public TransitionSet removeTarget(@Nonnull Class<?> target) {
        for (Transition transition : mTransitions) {
            transition.removeTarget(target);
        }
        return (TransitionSet) super.removeTarget(target);
    }

    @Nonnull
    @Override
    public TransitionSet removeTarget(@Nonnull String target) {
        for (Transition transition : mTransitions) {
            transition.removeTarget(target);
        }
        return (TransitionSet) super.removeTarget(target);
    }

    @Nonnull
    @Override
    public Transition excludeTarget(@Nonnull View target, boolean exclude) {
        for (Transition transition : mTransitions) {
            transition.excludeTarget(target, exclude);
        }
        return super.excludeTarget(target, exclude);
    }

    @Nonnull
    @Override
    public Transition excludeTarget(@Nonnull String targetName, boolean exclude) {
        for (Transition transition : mTransitions) {
            transition.excludeTarget(targetName, exclude);
        }
        return super.excludeTarget(targetName, exclude);
    }

    @Nonnull
    @Override
    public Transition excludeTarget(int targetId, boolean exclude) {
        for (Transition transition : mTransitions) {
            transition.excludeTarget(targetId, exclude);
        }
        return super.excludeTarget(targetId, exclude);
    }

    @Nonnull
    @Override
    public Transition excludeTarget(@Nonnull Class<?> type, boolean exclude) {
        for (Transition transition : mTransitions) {
            transition.excludeTarget(type, exclude);
        }
        return super.excludeTarget(type, exclude);
    }

    @Nonnull
    @Override
    public TransitionSet removeListener(@Nonnull TransitionListener listener) {
        return (TransitionSet) super.removeListener(listener);
    }

    /*@Override
    public void setPathMotion(PathMotion pathMotion) {
        super.setPathMotion(pathMotion);
        mChangeFlags |= FLAG_CHANGE_PATH_MOTION;
        if (mTransitions != null) {
            for (int i = 0; i < mTransitions.size(); i++) {
                mTransitions.get(i).setPathMotion(pathMotion);
            }
        }
    }*/

    /**
     * Removes the specified child transition from this set.
     *
     * @param transition The transition to be removed.
     * @return This transitionSet object.
     */
    @Nonnull
    public TransitionSet removeTransition(@Nonnull Transition transition) {
        mTransitions.remove(transition);
        transition.mParent = null;
        return this;
    }

    /**
     * Sets up listeners for each of the child transitions. This is used to
     * determine when this transition set is finished (all child transitions
     * must finish first).
     */
    private void setupStartEndListeners() {
        TransitionSetListener listener = new TransitionSetListener(this);
        for (Transition childTransition : mTransitions) {
            childTransition.addListener(listener);
        }
        mCurrentListeners = mTransitions.size();
    }

    /**
     * This listener is used to detect when all child transitions are done, at
     * which point this transition set is also done.
     */
    static class TransitionSetListener implements TransitionListener {

        final TransitionSet mTransitionSet;

        TransitionSetListener(@Nonnull TransitionSet transitionSet) {
            mTransitionSet = transitionSet;
        }

        @Override
        public void onTransitionStart(@Nonnull Transition transition) {
            if (!mTransitionSet.mStarted) {
                mTransitionSet.start();
                mTransitionSet.mStarted = true;
            }
        }

        @Override
        public void onTransitionEnd(@Nonnull Transition transition) {
            --mTransitionSet.mCurrentListeners;
            if (mTransitionSet.mCurrentListeners == 0) {
                // All child trans
                mTransitionSet.mStarted = false;
                mTransitionSet.end();
            }
            transition.removeListener(this);
        }
    }

    @Override
    protected void createAnimators(@Nonnull ViewGroup sceneRoot,
                                   @Nonnull TransitionValuesMaps startValues,
                                   @Nonnull TransitionValuesMaps endValues,
                                   @Nonnull ArrayList<TransitionValues> startValuesList,
                                   @Nonnull ArrayList<TransitionValues> endValuesList) {
        long startDelay = getStartDelay();
        int numTransitions = mTransitions.size();
        for (int i = 0; i < numTransitions; i++) {
            Transition childTransition = mTransitions.get(i);
            // We only set the start delay on the first transition if we are playing
            // the transitions sequentially.
            if (startDelay > 0 && (mPlayTogether || i == 0)) {
                long childStartDelay = childTransition.getStartDelay();
                if (childStartDelay > 0) {
                    childTransition.setStartDelay(startDelay + childStartDelay);
                } else {
                    childTransition.setStartDelay(startDelay);
                }
            }
            childTransition.createAnimators(sceneRoot, startValues, endValues, startValuesList,
                    endValuesList);
        }
    }

    @Override
    protected void runAnimators() {
        if (mTransitions.isEmpty()) {
            start();
            end();
            return;
        }
        setupStartEndListeners();
        if (!mPlayTogether) {
            // Setup sequence with listeners
            // TODO: Need to add listeners in such a way that we can remove them later if canceled
            for (int i = 1; i < mTransitions.size(); ++i) {
                Transition previousTransition = mTransitions.get(i - 1);
                final Transition nextTransition = mTransitions.get(i);
                previousTransition.addListener(new TransitionListener() {
                    @Override
                    public void onTransitionEnd(@Nonnull Transition transition) {
                        nextTransition.runAnimators();
                        transition.removeListener(this);
                    }
                });
            }
            Transition firstTransition = mTransitions.get(0);
            if (firstTransition != null) {
                firstTransition.runAnimators();
            }
        } else {
            for (Transition childTransition : mTransitions) {
                childTransition.runAnimators();
            }
        }
    }

    @Override
    public void captureStartValues(@Nonnull TransitionValues transitionValues) {
        if (isValidTarget(transitionValues.view)) {
            for (Transition childTransition : mTransitions) {
                if (childTransition.isValidTarget(transitionValues.view)) {
                    childTransition.captureStartValues(transitionValues);
                    transitionValues.mTargetedTransitions.add(childTransition);
                }
            }
        }
    }

    @Override
    public void captureEndValues(@Nonnull TransitionValues transitionValues) {
        if (isValidTarget(transitionValues.view)) {
            for (Transition childTransition : mTransitions) {
                if (childTransition.isValidTarget(transitionValues.view)) {
                    childTransition.captureEndValues(transitionValues);
                    transitionValues.mTargetedTransitions.add(childTransition);
                }
            }
        }
    }

    @Override
    void capturePropagationValues(@Nonnull TransitionValues transitionValues) {
        super.capturePropagationValues(transitionValues);
        for (Transition transition : mTransitions) {
            transition.capturePropagationValues(transitionValues);
        }
    }

    @Override
    public void pause(View sceneRoot) {
        super.pause(sceneRoot);
        for (Transition transition : mTransitions) {
            transition.pause(sceneRoot);
        }
    }

    @Override
    public void resume(@Nonnull View sceneRoot) {
        super.resume(sceneRoot);
        for (Transition transition : mTransitions) {
            transition.resume(sceneRoot);
        }
    }

    @Override
    protected void cancel() {
        super.cancel();
        for (Transition transition : mTransitions) {
            transition.cancel();
        }
    }

    @Override
    void forceToEnd(@Nonnull ViewGroup sceneRoot) {
        super.forceToEnd(sceneRoot);
        for (Transition transition : mTransitions) {
            transition.forceToEnd(sceneRoot);
        }
    }

    @Override
    void setCanRemoveViews(boolean canRemoveViews) {
        super.setCanRemoveViews(canRemoveViews);
        for (Transition transition : mTransitions) {
            transition.setCanRemoveViews(canRemoveViews);
        }
    }

    @Override
    public void setPropagation(@Nullable TransitionPropagation propagation) {
        super.setPropagation(propagation);
        mChangeFlags |= FLAG_CHANGE_PROPAGATION;
        for (Transition transition : mTransitions) {
            transition.setPropagation(propagation);
        }
    }

    @Override
    public void setEpicenterCallback(@Nullable EpicenterCallback epicenterCallback) {
        super.setEpicenterCallback(epicenterCallback);
        mChangeFlags |= FLAG_CHANGE_EPICENTER;
        for (Transition transition : mTransitions) {
            transition.setEpicenterCallback(epicenterCallback);
        }
    }

    @Override
    String toString(String indent) {
        StringBuilder sb = new StringBuilder(super.toString(indent));
        for (Transition transition : mTransitions) {
            sb.append("\n").append(transition.toString(indent + "  "));
        }
        return sb.toString();
    }

    @Override
    public Transition clone() {
        TransitionSet clone = (TransitionSet) super.clone();
        clone.mTransitions = new ArrayList<>();
        for (Transition transition : mTransitions) {
            clone.addTransitionInternal(transition.clone());
        }
        return clone;
    }
}
