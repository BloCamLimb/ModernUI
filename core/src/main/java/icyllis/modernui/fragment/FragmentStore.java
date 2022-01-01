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

package icyllis.modernui.fragment;

import icyllis.modernui.view.ViewGroup;
import org.apache.logging.log4j.Marker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.*;

import static icyllis.modernui.ModernUI.LOGGER;

final class FragmentStore {

    private static final Marker MARKER = FragmentManager.MARKER;

    private final ArrayList<Fragment> mAdded = new ArrayList<>();
    private final HashMap<String, FragmentStateManager> mActive = new HashMap<>();

    private FragmentManagerViewModel mViewModel;

    FragmentStore() {
    }

    void setViewModel(@Nonnull FragmentManagerViewModel viewModel) {
        mViewModel = viewModel;
    }

    FragmentManagerViewModel getViewModel() {
        return mViewModel;
    }

    void resetActiveFragments() {
        mActive.clear();
    }

    void restoreAddedFragments(@Nullable List<String> added) {
        mAdded.clear();
        if (added != null) {
            for (String who : added) {
                Fragment f = findActiveFragment(who);
                if (f == null) {
                    throw new IllegalStateException("No instantiated fragment for (" + who + ")");
                }
                if (FragmentManager.DEBUG) {
                    LOGGER.debug(MARKER, "restoreSaveState: added (" + who + "): " + f);
                }
                addFragment(f);
            }
        }
    }

    void makeActive(@Nonnull FragmentStateManager active) {
        Fragment f = active.getFragment();
        if (mActive.put(f.mWho, active) == null) {
            if (f.mRetainInstanceChangedWhileDetached) {
                if (f.mRetainInstance) {
                    mViewModel.addRetainedFragment(f);
                } else {
                    mViewModel.removeRetainedFragment(f);
                }
                f.mRetainInstanceChangedWhileDetached = false;
            }
        }
    }

    void addFragment(@Nonnull Fragment fragment) {
        if (mAdded.contains(fragment)) {
            throw new IllegalStateException("Fragment already added: " + fragment);
        }
        synchronized (mAdded) {
            mAdded.add(fragment);
        }
        fragment.mAdded = true;
    }

    void dispatchStateChange(int state) {
        for (var manager : mActive.values()) {
            if (manager != null) {
                manager.setFragmentManagerState(state);
            }
        }
    }

    void moveToExpectedState() {
        // Must add them in the proper order. mActive fragments may be out of order
        for (Fragment f : mAdded) {
            FragmentStateManager fragmentStateManager = mActive.get(f.mWho);
            if (fragmentStateManager != null) {
                fragmentStateManager.moveToExpectedState();
            }
        }

        // Now iterate through all active fragments. These will include those that are removed
        // and detached.
        for (FragmentStateManager fragmentStateManager : mActive.values()) {
            if (fragmentStateManager != null) {
                fragmentStateManager.moveToExpectedState();

                Fragment f = fragmentStateManager.getFragment();
                boolean beingRemoved = f.mRemoving && !f.isInBackStack();
                if (beingRemoved) {
                    makeInactive(fragmentStateManager);
                }
            }
        }
    }

    void removeFragment(@Nonnull Fragment fragment) {
        synchronized (mAdded) {
            mAdded.remove(fragment);
        }
        fragment.mAdded = false;
    }

    void makeInactive(@Nonnull FragmentStateManager inactive) {
        Fragment f = inactive.getFragment();

        if (f.mRetainInstance) {
            mViewModel.removeRetainedFragment(f);
        }

        // Don't remove yet. That happens in burpActive(). This prevents
        // concurrent modification while iterating over mActive
        if (mActive.put(f.mWho, null) != null) {
            if (FragmentManager.DEBUG) {
                LOGGER.debug(MARKER, "Removed fragment from active set " + f);
            }
        }
        // else It was already removed, so there's nothing more to do
    }

    /**
     * To prevent list modification errors, mActive sets values to null instead of
     * removing them when the Fragment becomes inactive. This cleans up the list at the
     * end of executing the transactions.
     */
    void burpActive() {
        Collection<FragmentStateManager> values = mActive.values();
        // values() provides a view into the map, so removing elements from it
        // removes the relevant pairs in the Map
        values.removeAll(Collections.singleton(null));
    }

    @Nullable
    ArrayList<String> saveAddedFragments() {
        synchronized (mAdded) {
            if (mAdded.isEmpty()) {
                return null;
            }
            ArrayList<String> added = new ArrayList<>(mAdded.size());
            for (var f : mAdded) {
                added.add(f.mWho);
                if (FragmentManager.DEBUG) {
                    LOGGER.debug(MARKER, "saveAllState: adding fragment (" + f.mWho
                            + "): " + f);
                }
            }
            return added;
        }
    }

    @Nonnull
    List<FragmentStateManager> getActiveFragmentStateManagers() {
        ArrayList<FragmentStateManager> list = new ArrayList<>();
        for (var manager : mActive.values()) {
            if (manager != null) {
                list.add(manager);
            }
        }
        return list;
    }

    @Nonnull
    List<Fragment> getFragments() {
        if (mAdded.isEmpty()) {
            return Collections.emptyList();
        }
        synchronized (mAdded) {
            return new ArrayList<>(mAdded);
        }
    }

    @Nonnull
    List<Fragment> getActiveFragments() {
        ArrayList<Fragment> list = new ArrayList<>();
        for (var manager : mActive.values()) {
            if (manager != null) {
                list.add(manager.getFragment());
            } else {
                list.add(null);
            }
        }
        return list;
    }

    int getActiveFragmentCount() {
        return mActive.size();
    }

    @Nullable
    Fragment findFragmentById(int id) {
        Fragment f;
        // First look through added fragments.
        for (int i = mAdded.size() - 1; i >= 0; i--) {
            f = mAdded.get(i);
            if (f.mFragmentId == id) {
                return f;
            }
        }
        // Now for any known fragment.
        for (var manager : mActive.values()) {
            if (manager != null) {
                f = manager.getFragment();
                if (f.mFragmentId == id) {
                    return f;
                }
            }
        }
        return null;
    }

    @Nullable
    Fragment findFragmentByTag(@Nullable String tag) {
        if (tag == null) {
            return null;
        }
        Fragment f;
        // First look through added fragments.
        for (int i = mAdded.size() - 1; i >= 0; i--) {
            f = mAdded.get(i);
            if (tag.equals(f.mTag)) {
                return f;
            }
        }
        // Now for any known fragment.
        for (var manager : mActive.values()) {
            if (manager != null) {
                f = manager.getFragment();
                if (tag.equals(f.mTag)) {
                    return f;
                }
            }
        }
        return null;
    }

    boolean containsActiveFragment(@Nonnull String who) {
        return mActive.get(who) != null;
    }

    @Nullable
    FragmentStateManager getFragmentStateManager(@Nonnull String who) {
        return mActive.get(who);
    }

    @Nullable
    Fragment findFragmentByWho(@Nonnull String who) {
        for (var manager : mActive.values()) {
            if (manager != null) {
                Fragment f = manager.getFragment();
                if ((f = f.findFragmentByWho(who)) != null) {
                    return f;
                }
            }
        }
        return null;
    }

    @Nullable
    Fragment findActiveFragment(@Nonnull String who) {
        FragmentStateManager manager = mActive.get(who);
        if (manager != null) {
            return manager.getFragment();
        }
        return null;
    }

    /**
     * Find the index within the fragment's container that the given fragment's view should be
     * added at such that the order in the container matches the order in mAdded.
     * <p>
     * As an example, if mAdded has two Fragments with Views sharing the same container:
     * FragmentA
     * FragmentB
     * <p>
     * Then, when processing FragmentB, we return the index of FragmentA's view in the
     * shared container + 1 so that FragmentB is directly on top of FragmentA. In cases where
     * this is the first fragment in the container, this method returns -1 to signal that
     * the view should be added to the end of the container.
     *
     * @param f The fragment that may be on top of another fragment.
     * @return The correct index for the given fragment relative to other fragments in the same
     * container, or -1 if there are no fragments in the same container.
     */
    int findFragmentIndexInContainer(@Nonnull Fragment f) {
        final ViewGroup container = f.mContainer;

        if (container == null) {
            return -1;
        }
        final int fragmentIndex = mAdded.indexOf(f);
        // First search if there's a fragment that should be under this new fragment
        for (int i = fragmentIndex - 1; i >= 0; i--) {
            Fragment underFragment = mAdded.get(i);
            if (underFragment.mContainer == container && underFragment.mView != null) {
                // Found the fragment under this one
                int underIndex = container.indexOfChild(underFragment.mView);
                // The new fragment needs to go right after it
                return underIndex + 1;
            }
        }
        // Now search if there's a fragment that should be over this new fragment
        for (int i = fragmentIndex + 1; i < mAdded.size(); i++) {
            Fragment overFragment = mAdded.get(i);
            if (overFragment.mContainer == container && overFragment.mView != null) {
                // Found the fragment over this one
                // so the new fragment needs to go right under it
                return container.indexOfChild(overFragment.mView);
            }
        }
        // Else, there's no other fragments in this container, so we
        // should just add the fragment to the end
        return -1;
    }

    void dump(@Nonnull String prefix, @Nullable FileDescriptor fd,
              @Nonnull PrintWriter writer, @Nullable String... args) {
        String innerPrefix = prefix + "    ";

        if (!mActive.isEmpty()) {
            writer.print(prefix);
            writer.println("Active Fragments:");
            for (FragmentStateManager fragmentStateManager : mActive.values()) {
                writer.print(prefix);
                if (fragmentStateManager != null) {
                    Fragment f = fragmentStateManager.getFragment();
                    writer.println(f);
                    f.dump(innerPrefix, fd, writer, args);
                } else {
                    writer.println("null");
                }
            }
        }

        int count = mAdded.size();
        if (count > 0) {
            writer.print(prefix); writer.println("Added Fragments:");
            for (int i = 0; i < count; i++) {
                Fragment f = mAdded.get(i);
                writer.print(prefix);
                writer.print("  #");
                writer.print(i);
                writer.print(": ");
                writer.println(f.toString());
            }
        }
    }
}
