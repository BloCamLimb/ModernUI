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

import icyllis.modernui.animation.Animator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class FragmentAnim {
    /**
     * Static util classes shouldn't be instantiated.
     */
    private FragmentAnim() {
    }

    @Nullable
    static Animator loadAnimation(@Nonnull Fragment fragment, boolean enter, boolean isPop) {
        int transit = fragment.getNextTransition();
        int nextAnim;
        if (isPop) {
            if (enter) {
                nextAnim = fragment.getPopEnterAnim();
            } else {
                nextAnim = fragment.getPopExitAnim();
            }
        } else {
            if (enter) {
                nextAnim = fragment.getEnterAnim();
            } else {
                nextAnim = fragment.getExitAnim();
            }
        }
        // Clear the Fragment animations
        fragment.setAnimations(0, 0, 0, 0);
        // We do not need to keep up with the removing Fragment after we get its next animation.
        // If transactions do not allow reordering, this will always be true and the visible
        // removing fragment will be cleared. If reordering is allowed, this will only be true
        // after all records in a transaction have been executed and the visible removing
        // fragment has the correct animation, so it is time to clear it.
        if (fragment.mContainer != null
                && fragment.mContainer.getTag(FragmentManager.visible_removing_fragment_view_tag) != null) {
            fragment.mContainer.setTag(FragmentManager.visible_removing_fragment_view_tag, null);
        }
        // If there is a transition on the container, clear those set on the fragment
        if (fragment.mContainer != null && fragment.mContainer.getLayoutTransition() != null) {
            return null;
        }

        Animator animator = fragment.onCreateAnimator(transit, enter, nextAnim);
        if (animator != null) {
            return animator;
        }

        if (nextAnim == 0 && transit != FragmentTransaction.TRANSIT_NONE) {
            //TODO animators
        }
        return null;
    }
}
