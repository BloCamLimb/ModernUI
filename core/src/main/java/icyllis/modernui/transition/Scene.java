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
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A scene represents the collection of values that various properties in the
 * View hierarchy will have when the scene is applied. A Scene can be
 * configured to automatically run a Transition when it is applied, which will
 * animate the various property changes that take place during the
 * scene change.
 */
public class Scene {

    private final ViewGroup mSceneRoot;
    private View mLayout; // alternative to layoutId
    private Runnable mEnterAction, mExitAction;

    /**
     * Constructs a Scene with no information about how values will change
     * when this scene is applied. This constructor might be used when
     * a Scene is created with the intention of being dynamically configured,
     * through setting {@link #setEnterAction(Runnable)} and possibly
     * {@link #setExitAction(Runnable)}.
     *
     * @param sceneRoot The root of the hierarchy in which scene changes
     *                  and transitions will take place.
     */
    public Scene(@Nonnull ViewGroup sceneRoot) {
        mSceneRoot = sceneRoot;
    }

    /**
     * Constructs a Scene which, when entered, will remove any
     * children from the sceneRoot container and add the layout
     * object as a new child of that container.
     *
     * @param sceneRoot The root of the hierarchy in which scene changes
     *                  and transitions will take place.
     * @param layout    The view hierarchy of this scene, added as a child
     *                  of sceneRoot when this scene is entered.
     */
    public Scene(@Nonnull ViewGroup sceneRoot, @Nonnull View layout) {
        mSceneRoot = sceneRoot;
        mLayout = layout;
    }

    /**
     * Gets the root of the scene, which is the root of the view hierarchy
     * affected by changes due to this scene, and which will be animated
     * when this scene is entered.
     *
     * @return The root of the view hierarchy affected by this scene.
     */
    @Nonnull
    public ViewGroup getSceneRoot() {
        return mSceneRoot;
    }

    /**
     * Exits this scene, if it is the current scene
     * on the scene's {@link #getSceneRoot() scene root}. The current scene is
     * set when {@link #enter() entering} a scene.
     * Exiting a scene runs the {@link #setExitAction(Runnable) exit action}
     * if there is one.
     */
    public void exit() {
        if (getCurrentScene(mSceneRoot) == this) {
            if (mExitAction != null) {
                mExitAction.run();
            }
        }
    }

    /**
     * Enters this scene, which entails changing all values that
     * are specified by this scene. These may be values associated
     * with a layout view group or layout resource file which will
     * now be added to the scene root, or it may be values changed by
     * an {@link #setEnterAction(Runnable)} enter action}, or a
     * combination of the these. No transition will be run when the
     * scene is entered. To get transition behavior in scene changes,
     * use one of the methods in {@link TransitionManager} instead.
     */
    public void enter() {
        // Apply layout change, if any
        if (mLayout != null) {
            // empty out parent container before adding to it
            getSceneRoot().removeAllViews();

            mSceneRoot.addView(mLayout);
        }

        // Notify next scene that it is entering. Subclasses may override to configure scene.
        if (mEnterAction != null) {
            mEnterAction.run();
        }

        setCurrentScene(mSceneRoot, this);
    }

    /**
     * Set the scene that the given ViewGroup is in. The current scene is set only
     * on the root ViewGroup of a scene, not for every view in that hierarchy. This
     * information is used by Scene to determine whether there is a previous
     * scene which should be exited before the new scene is entered.
     *
     * @param sceneRoot The ViewGroup on which the current scene is being set
     */
    static void setCurrentScene(@Nonnull ViewGroup sceneRoot, @Nullable Scene scene) {
        sceneRoot.setTag(R.id.transition_current_scene, scene);
    }

    /**
     * Gets the current {@link Scene} set on the given ViewGroup. A scene is set on a ViewGroup
     * only if that ViewGroup is the scene root.
     *
     * @param sceneRoot The ViewGroup on which the current scene will be returned
     * @return The current Scene set on this ViewGroup. A value of null indicates that
     * no Scene is currently set.
     */
    @Nullable
    public static Scene getCurrentScene(@Nonnull ViewGroup sceneRoot) {
        return (Scene) sceneRoot.getTag(R.id.transition_current_scene);
    }

    /**
     * Scenes that are not defined with layout resources or
     * hierarchies, or which need to perform additional steps
     * after those hierarchies are changed to, should set an enter
     * action, and possibly an exit action as well. An enter action
     * will cause Scene to call back into application code to do
     * anything else the application needs after transitions have
     * captured pre-change values and after any other scene changes
     * have been applied, such as the layout (if any) being added to
     * the view hierarchy. After this method is called, Transitions will
     * be played.
     *
     * @param action The runnable whose {@link Runnable#run() run()} method will
     *               be called when this scene is entered
     * @see #setExitAction(Runnable)
     * @see Scene#Scene(ViewGroup)
     */
    public void setEnterAction(@Nullable Runnable action) {
        mEnterAction = action;
    }

    /**
     * Scenes that are not defined with layout resources or
     * hierarchies, or which need to perform additional steps
     * after those hierarchies are changed to, should set an enter
     * action, and possibly an exit action as well. An exit action
     * will cause Scene to call back into application code to do
     * anything the application needs to do after applicable transitions have
     * captured pre-change values, but before any other scene changes
     * have been applied, such as the new layout (if any) being added to
     * the view hierarchy. After this method is called, the next scene
     * will be entered, including a call to {@link #setEnterAction(Runnable)}
     * if an enter action is set.
     *
     * @see #setEnterAction(Runnable)
     * @see Scene#Scene(ViewGroup)
     */
    public void setExitAction(@Nullable Runnable action) {
        mExitAction = action;
    }
}
