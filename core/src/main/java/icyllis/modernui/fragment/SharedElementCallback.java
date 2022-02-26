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

package icyllis.modernui.fragment;

import icyllis.modernui.view.View;

import java.util.List;
import java.util.Map;

/**
 * Listener provided in
 * {@link Fragment#setEnterSharedElementCallback(SharedElementCallback)} and
 * {@link Fragment#setExitSharedElementCallback(SharedElementCallback)}
 * to monitor the Fragment transitions. The events can be used to customize Fragment
 * Transition behavior.
 */
public abstract class SharedElementCallback {

    /**
     * In Activity Transitions, onSharedElementStart is called immediately before
     * capturing the start of the shared element state on enter and reenter transitions and
     * immediately before capturing the end of the shared element state for exit and return
     * transitions.
     * <p>
     * In Fragment Transitions, onSharedElementStart is called immediately before capturing the
     * start state of all shared element transitions.
     * <p>
     * This call can be used to adjust the transition start state by modifying the shared
     * element Views. Note that no layout step will be executed between onSharedElementStart
     * and the transition state capture.
     * <p>
     * For Activity Transitions, any changes made in {@link #onSharedElementEnd(List, List, List)}
     * that are not updated during layout should be corrected in onSharedElementStart for exit and
     * return transitions. For example, rotation or scale will not be affected by layout and
     * if changed in {@link #onSharedElementEnd(List, List, List)}, it will also have to be reset
     * in onSharedElementStart again to correct the end state.
     *
     * @param sharedElementNames     The names of the shared elements that were accepted into
     *                               the View hierarchy.
     * @param sharedElements         The shared elements that are part of the View hierarchy.
     * @param sharedElementSnapshots The Views containing snapshots of the shared element
     *                               from the launching Window. These elements will not
     *                               be part of the scene, but will be positioned relative
     *                               to the Window decor View. This list is null for Fragment
     *                               Transitions.
     */
    public void onSharedElementStart(List<String> sharedElementNames,
                                     List<View> sharedElements, List<View> sharedElementSnapshots) {
    }

    /**
     * In Activity Transitions, onSharedElementEnd is called immediately before
     * capturing the end of the shared element state on enter and reenter transitions and
     * immediately before capturing the start of the shared element state for exit and return
     * transitions.
     * <p>
     * In Fragment Transitions, onSharedElementEnd is called immediately before capturing the
     * end state of all shared element transitions.
     * <p>
     * This call can be used to adjust the transition end state by modifying the shared
     * element Views. Note that no layout step will be executed between onSharedElementEnd
     * and the transition state capture.
     * <p>
     * Any changes made in {@link #onSharedElementStart(List, List, List)} that are not updated
     * during layout should be corrected in onSharedElementEnd. For example, rotation or scale
     * will not be affected by layout and if changed in
     * {@link #onSharedElementStart(List, List, List)}, it will also have to be reset in
     * onSharedElementEnd again to correct the end state.
     *
     * @param sharedElementNames     The names of the shared elements that were accepted into
     *                               the View hierarchy.
     * @param sharedElements         The shared elements that are part of the View hierarchy.
     * @param sharedElementSnapshots The Views containing snapshots of the shared element
     *                               from the launching Window. These elements will not
     *                               be part of the scene, but will be positioned relative
     *                               to the Window decor View. This list will be null for
     *                               Fragment Transitions.
     */
    public void onSharedElementEnd(List<String> sharedElementNames,
                                   List<View> sharedElements, List<View> sharedElementSnapshots) {
    }

    /**
     * Called after {@link #onMapSharedElements(java.util.List, java.util.Map)} when
     * transferring shared elements in. Any shared elements that have no mapping will be in
     * <var>rejectedSharedElements</var>. The elements remaining in
     * <var>rejectedSharedElements</var> will be transitioned out of the Scene. If a
     * View is removed from <var>rejectedSharedElements</var>, it must be handled by the
     * <code>SharedElementListener</code>.
     * <p>
     * Views in rejectedSharedElements will have their position and size set to the
     * position of the calling shared element, relative to the Window decor View and contain
     * snapshots of the View from the calling Activity or Fragment. This
     * view may be safely added to the decor View's overlay to remain in position.
     * </p>
     * <p>This method is not called for Fragment Transitions. All rejected shared elements
     * will be handled by the exit transition.</p>
     *
     * @param rejectedSharedElements Views containing visual information of shared elements
     *                               that are not part of the entering scene. These Views
     *                               are positioned relative to the Window decor View. A
     *                               View removed from this list will not be transitioned
     *                               automatically.
     */
    public void onRejectSharedElements(List<View> rejectedSharedElements) {
    }

    /**
     * Lets the SharedElementCallback adjust the mapping of shared element names to
     * Views.
     *
     * @param names          The names of all shared elements transferred from the calling Activity
     *                       or Fragment in the order they were provided.
     * @param sharedElements The mapping of shared element names to Views. The best guess
     *                       will be filled into sharedElements based on the transitionNames.
     */
    public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
    }
}
