/*
 * Modern UI.
 * Copyright (C) 2020-2025 BloCamLimb. All rights reserved.
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
 *   Copyright (C) 2018 The Android Open Source Project
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

package icyllis.modernui.fragment;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.util.LogWriter;
import icyllis.modernui.lifecycle.Lifecycle;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;

import java.io.PrintWriter;
import java.util.ArrayList;

import static icyllis.modernui.util.Log.LOGGER;

/**
 * Entry of an operation on the fragment back stack.
 */
final class BackStackRecord extends FragmentTransaction implements
        FragmentManager.BackStackEntry, FragmentManager.OpGenerator {

    final FragmentManager mManager;

    boolean mCommitted;
    int mIndex = -1;
    boolean mBeingSaved = false;

    BackStackRecord(@NonNull FragmentManager manager) {
        super(manager.getFragmentFactory());
        mManager = manager;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("BackStackEntry{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        if (mIndex >= 0) {
            sb.append(" #");
            sb.append(mIndex);
        }
        if (mName != null) {
            sb.append(" ");
            sb.append(mName);
        }
        sb.append("}");
        return sb.toString();
    }

    public void dump(String prefix, PrintWriter writer) {
        dump(prefix, writer, true);
    }

    public void dump(String prefix, PrintWriter writer, boolean full) {
        if (full) {
            writer.print(prefix);
            writer.print("mName=");
            writer.print(mName);
            writer.print(" mIndex=");
            writer.print(mIndex);
            writer.print(" mCommitted=");
            writer.println(mCommitted);
            if (mTransition != FragmentTransaction.TRANSIT_NONE) {
                writer.print(prefix);
                writer.print("mTransition=#");
                writer.print(Integer.toHexString(mTransition));
            }
            if (mEnterAnim != null || mExitAnim != null) {
                writer.print(prefix);
                writer.print("mEnterAnim=#");
                writer.print((mEnterAnim));
                writer.print(" mExitAnim=#");
                writer.println((mExitAnim));
            }
            if (mPopEnterAnim != null || mPopExitAnim != null) {
                writer.print(prefix);
                writer.print("mPopEnterAnim=#");
                writer.print((mPopEnterAnim));
                writer.print(" mPopExitAnim=#");
                writer.println((mPopExitAnim));
            }
        }

        if (!mOps.isEmpty()) {
            writer.print(prefix);
            writer.println("Operations:");
            final int numOps = mOps.size();
            for (int opNum = 0; opNum < numOps; opNum++) {
                final Op op = mOps.get(opNum);
                String cmdStr = switch (op.mCmd) {
                    case OP_NULL -> "NULL";
                    case OP_ADD -> "ADD";
                    case OP_REPLACE -> "REPLACE";
                    case OP_REMOVE -> "REMOVE";
                    case OP_HIDE -> "HIDE";
                    case OP_SHOW -> "SHOW";
                    case OP_DETACH -> "DETACH";
                    case OP_ATTACH -> "ATTACH";
                    case OP_SET_PRIMARY_NAV -> "SET_PRIMARY_NAV";
                    case OP_UNSET_PRIMARY_NAV -> "UNSET_PRIMARY_NAV";
                    case OP_SET_MAX_LIFECYCLE -> "OP_SET_MAX_LIFECYCLE";
                    default -> "cmd=" + op.mCmd;
                };
                writer.print(prefix);
                writer.print("  Op #");
                writer.print(opNum);
                writer.print(": ");
                writer.print(cmdStr);
                writer.print(" ");
                writer.println(op.mFragment);
                if (full) {
                    if (op.mEnterAnim != null || op.mExitAnim != null) {
                        writer.print(prefix);
                        writer.print("enterAnim=#");
                        writer.print((op.mEnterAnim));
                        writer.print(" exitAnim=#");
                        writer.println((op.mExitAnim));
                    }
                    if (op.mPopEnterAnim != null || op.mPopExitAnim != null) {
                        writer.print(prefix);
                        writer.print("popEnterAnim=#");
                        writer.print((op.mPopEnterAnim));
                        writer.print(" popExitAnim=#");
                        writer.println((op.mPopExitAnim));
                    }
                }
            }
        }
    }

    @Override
    public int getId() {
        return mIndex;
    }

    @Nullable
    @Override
    public String getName() {
        return mName;
    }

    @Override
    void doAddOp(int containerViewId, @NonNull Fragment fragment, @Nullable String tag, int cmd) {
        super.doAddOp(containerViewId, fragment, tag, cmd);
        fragment.mFragmentManager = mManager;
    }

    @NonNull
    @Override
    public FragmentTransaction remove(@NonNull Fragment fragment) {
        if (fragment.mFragmentManager != null && fragment.mFragmentManager != mManager) {
            throw new IllegalStateException("Cannot remove Fragment attached to "
                    + "a different FragmentManager. Fragment " + fragment + " is already"
                    + " attached to a FragmentManager.");
        }
        return super.remove(fragment);
    }

    @NonNull
    @Override
    public FragmentTransaction hide(@NonNull Fragment fragment) {
        if (fragment.mFragmentManager != null && fragment.mFragmentManager != mManager) {
            throw new IllegalStateException("Cannot hide Fragment attached to "
                    + "a different FragmentManager. Fragment " + fragment + " is already"
                    + " attached to a FragmentManager.");
        }
        return super.hide(fragment);
    }

    @NonNull
    @Override
    public FragmentTransaction show(@NonNull Fragment fragment) {
        if (fragment.mFragmentManager != null && fragment.mFragmentManager != mManager) {
            throw new IllegalStateException("Cannot show Fragment attached to "
                    + "a different FragmentManager. Fragment " + fragment + " is already"
                    + " attached to a FragmentManager.");
        }
        return super.show(fragment);
    }

    @NonNull
    @Override
    public FragmentTransaction detach(@NonNull Fragment fragment) {
        if (fragment.mFragmentManager != null && fragment.mFragmentManager != mManager) {
            throw new IllegalStateException("Cannot detach Fragment attached to "
                    + "a different FragmentManager. Fragment " + fragment + " is already"
                    + " attached to a FragmentManager.");
        }
        return super.detach(fragment);
    }

    @NonNull
    @Override
    public FragmentTransaction setPrimaryNavigationFragment(@Nullable Fragment fragment) {
        if (fragment != null
                && fragment.mFragmentManager != null && fragment.mFragmentManager != mManager) {
            throw new IllegalStateException("Cannot setPrimaryNavigation for Fragment attached to "
                    + "a different FragmentManager. Fragment " + fragment + " is already"
                    + " attached to a FragmentManager.");
        }
        return super.setPrimaryNavigationFragment(fragment);
    }

    @NonNull
    @Override
    public FragmentTransaction setMaxLifecycle(@NonNull Fragment fragment,
                                               @NonNull Lifecycle.State state) {
        if (fragment.mFragmentManager != mManager) {
            throw new IllegalArgumentException("Cannot setMaxLifecycle for Fragment not attached to"
                    + " FragmentManager " + mManager);
        }
        if (state == Lifecycle.State.INITIALIZED && fragment.mState > Fragment.INITIALIZING) {
            throw new IllegalArgumentException("Cannot set maximum Lifecycle to " + state
                    + " after the Fragment has been created");
        }
        if (state == Lifecycle.State.DESTROYED) {
            throw new IllegalArgumentException("Cannot set maximum Lifecycle to " + state + ". Use "
                    + "remove() to remove the fragment from the FragmentManager and trigger its "
                    + "destruction.");
        }
        return super.setMaxLifecycle(fragment, state);
    }

    void bumpBackStackNesting(int amt) {
        if (!mAddToBackStack) {
            return;
        }
        if (FragmentManager.DEBUG) {
            LOGGER.trace(FragmentManager.MARKER, "Bump nesting in " + this + " by " + amt);
        }
        // Enhanced for: Safe
        for (final Op op : mOps) {
            if (op.mFragment != null) {
                op.mFragment.mBackStackNesting += amt;
                if (FragmentManager.DEBUG) {
                    LOGGER.trace(FragmentManager.MARKER, "Bump nesting of "
                            + op.mFragment + " to " + op.mFragment.mBackStackNesting);
                }
            }
        }
    }

    public void runOnCommitRunnables() {
        if (mCommitRunnables != null) {
            // Enhanced for: Safe
            for (Runnable runnable : mCommitRunnables) {
                runnable.run();
            }
            mCommitRunnables = null;
        }
    }

    @Override
    public int commit() {
        return commitInternal(false);
    }

    @Override
    public int commitAllowingStateLoss() {
        return commitInternal(true);
    }

    @Override
    public void commitNow() {
        disallowAddToBackStack();
        mManager.execSingleAction(this, false);
    }

    @Override
    public void commitNowAllowingStateLoss() {
        disallowAddToBackStack();
        mManager.execSingleAction(this, true);
    }

    int commitInternal(boolean allowStateLoss) {
        if (mCommitted) throw new IllegalStateException("commit already called");
        if (FragmentManager.TRACE) {
            LOGGER.info(FragmentManager.MARKER, "Commit: " + this);
            var w = new PrintWriter(new LogWriter(FragmentManager.MARKER), true);
            try (w) {
                dump("  ", w);
            }
        }
        mCommitted = true;
        if (mAddToBackStack) {
            mIndex = mManager.allocBackStackIndex();
        } else {
            mIndex = -1;
        }
        mManager.enqueueAction(this, allowStateLoss);
        return mIndex;
    }

    /**
     * Implementation of {@link FragmentManager.OpGenerator}.
     * This operation is added to the list of pending actions during {@link #commit()}, and
     * will be executed on the UI thread to run this FragmentTransaction.
     *
     * @param records     Modified to add this BackStackRecord
     * @param isRecordPop Modified to add a false (this isn't a pop)
     * @return true always because the records and isRecordPop will always be changed
     */
    @Override
    public boolean generateOps(@NonNull ArrayList<BackStackRecord> records,
                               @NonNull BooleanArrayList isRecordPop) {
        if (FragmentManager.TRACE) {
            LOGGER.info(FragmentManager.MARKER, "Run: " + this);
        }

        records.add(this);
        isRecordPop.add(false);
        if (mAddToBackStack) {
            mManager.addBackStackState(this);
        }
        return true;
    }

    /**
     * Executes the operations contained within this transaction.
     */
    void executeOps() {
        // Enhanced for: Safe
        for (final Op op : mOps) {
            final Fragment f = op.mFragment;
            if (f != null) {
                f.mBeingSaved = mBeingSaved;
                f.setPopDirection(false);
                f.setNextTransition(mTransition);
                f.setSharedElementNames(mSharedElementSourceNames, mSharedElementTargetNames);
            }
            switch (op.mCmd) {
                case OP_ADD -> {
                    assert f != null;
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.setExitAnimationOrder(f, false);
                    mManager.addFragment(f);
                }
                case OP_REMOVE -> {
                    assert f != null;
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.removeFragment(f);
                }
                case OP_HIDE -> {
                    assert f != null;
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.hideFragment(f);
                }
                case OP_SHOW -> {
                    assert f != null;
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.setExitAnimationOrder(f, false);
                    mManager.showFragment(f);
                }
                case OP_DETACH -> {
                    assert f != null;
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.detachFragment(f);
                }
                case OP_ATTACH -> {
                    assert f != null;
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.setExitAnimationOrder(f, false);
                    mManager.attachFragment(f);
                }
                case OP_SET_PRIMARY_NAV -> mManager.setPrimaryNavigationFragment(f);
                case OP_UNSET_PRIMARY_NAV -> mManager.setPrimaryNavigationFragment(null);
                case OP_SET_MAX_LIFECYCLE -> {
                    assert f != null;
                    mManager.setMaxLifecycle(f, op.mCurrentMaxState);
                }
                default -> throw new IllegalArgumentException("Unknown cmd: " + op.mCmd);
            }
        }
    }

    /**
     * Reverses the execution of the operations within this transaction.
     */
    void executePopOps() {
        for (int opNum = mOps.size() - 1; opNum >= 0; opNum--) {
            final Op op = mOps.get(opNum);
            final Fragment f = op.mFragment;
            if (f != null) {
                f.mBeingSaved = mBeingSaved;
                f.setPopDirection(true);
                f.setNextTransition(FragmentManager.reverseTransit(mTransition));
                // Reverse the target and source names for pop operations
                f.setSharedElementNames(mSharedElementTargetNames, mSharedElementSourceNames);
            }
            switch (op.mCmd) {
                case OP_ADD -> {
                    assert f != null;
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.setExitAnimationOrder(f, true);
                    mManager.removeFragment(f);
                }
                case OP_REMOVE -> {
                    assert f != null;
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.addFragment(f);
                }
                case OP_HIDE -> {
                    assert f != null;
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.showFragment(f);
                }
                case OP_SHOW -> {
                    assert f != null;
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.setExitAnimationOrder(f, true);
                    mManager.hideFragment(f);
                }
                case OP_DETACH -> {
                    assert f != null;
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.attachFragment(f);
                }
                case OP_ATTACH -> {
                    assert f != null;
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.setExitAnimationOrder(f, true);
                    mManager.detachFragment(f);
                }
                case OP_SET_PRIMARY_NAV -> mManager.setPrimaryNavigationFragment(null);
                case OP_UNSET_PRIMARY_NAV -> mManager.setPrimaryNavigationFragment(f);
                case OP_SET_MAX_LIFECYCLE -> {
                    assert f != null;
                    mManager.setMaxLifecycle(f, op.mOldMaxState);
                }
                default -> throw new IllegalArgumentException("Unknown cmd: " + op.mCmd);
            }
        }
    }

    /**
     * Expands all meta-ops into their more primitive equivalents. This must be called prior to
     * {@link #executeOps()} or any other call that operations on mOps for forward navigation.
     * It should not be called for pop/reverse navigation operations.
     *
     * <p>Removes all OP_REPLACE ops and replaces them with the proper add and remove
     * operations that are equivalent to the replace.</p>
     *
     * <p>Adds OP_UNSET_PRIMARY_NAV ops to match OP_SET_PRIMARY_NAV, OP_REMOVE and OP_DETACH
     * ops so that we can restore the old primary nav fragment later. Since callers call this
     * method in a loop before running ops from several transactions at once, the caller should
     * pass the return value from this method as the oldPrimaryNav parameter for the next call.
     * The first call in such a loop should pass the value of
     * {@link FragmentManager#getPrimaryNavigationFragment()}.</p>
     *
     * @param added         Initialized to the fragments that are in the mManager.mAdded, this
     *                      will be modified to contain the fragments that will be in mAdded
     *                      after the execution ({@link #executeOps()}.
     * @param oldPrimaryNav The tracked primary navigation fragment as of the beginning of
     *                      this set of ops
     * @return the new oldPrimaryNav fragment after this record's ops would be run
     */
    Fragment expandOps(ArrayList<Fragment> added, Fragment oldPrimaryNav) {
        for (int opNum = 0; opNum < mOps.size(); opNum++) {
            final Op op = mOps.get(opNum);
            switch (op.mCmd) {
                case OP_ADD, OP_ATTACH -> added.add(op.mFragment);
                case OP_REMOVE, OP_DETACH -> {
                    added.remove(op.mFragment);
                    if (op.mFragment == oldPrimaryNav) {
                        mOps.add(opNum, new Op(OP_UNSET_PRIMARY_NAV, op.mFragment));
                        opNum++;
                        oldPrimaryNav = null;
                    }
                }
                case OP_REPLACE -> {
                    final Fragment f = op.mFragment;
                    final int containerId = f.mContainerId;
                    boolean alreadyAdded = false;
                    for (int i = added.size() - 1; i >= 0; i--) {
                        final Fragment old = added.get(i);
                        if (old.mContainerId == containerId) {
                            if (old == f) {
                                alreadyAdded = true;
                            } else {
                                // This is duplicated from above since we only make
                                // a single pass for expanding ops. Unset any outgoing primary nav.
                                if (old == oldPrimaryNav) {
                                    mOps.add(opNum, new Op(OP_UNSET_PRIMARY_NAV, old, true));
                                    opNum++;
                                    oldPrimaryNav = null;
                                }
                                final Op removeOp = new Op(OP_REMOVE, old, true);
                                removeOp.mEnterAnim = op.mEnterAnim;
                                removeOp.mPopEnterAnim = op.mPopEnterAnim;
                                removeOp.mExitAnim = op.mExitAnim;
                                removeOp.mPopExitAnim = op.mPopExitAnim;
                                mOps.add(opNum, removeOp);
                                added.remove(old);
                                opNum++;
                            }
                        }
                    }
                    if (alreadyAdded) {
                        mOps.remove(opNum);
                        opNum--;
                    } else {
                        op.mCmd = OP_ADD;
                        op.mFromExpandedOp = true;
                        added.add(f);
                    }
                }
                case OP_SET_PRIMARY_NAV -> {
                    // It's ok if this is null, that means we will restore to no active
                    // primary navigation fragment on a pop.
                    mOps.add(opNum, new Op(OP_UNSET_PRIMARY_NAV, oldPrimaryNav, true));
                    op.mFromExpandedOp = true;
                    opNum++;
                    // Will be set by the OP_SET_PRIMARY_NAV we inserted before when run
                    oldPrimaryNav = op.mFragment;
                }
            }
        }
        return oldPrimaryNav;
    }

    /**
     * Removes fragments that are added or removed during a pop operation.
     *
     * @param added         Initialized to the fragments that are in the mManager.mAdded, this
     *                      will be modified to contain the fragments that will be in mAdded
     *                      after the execution ({@link #executeOps()}.
     * @param oldPrimaryNav The tracked primary navigation fragment as of the beginning of
     *                      this set of ops
     * @return the new oldPrimaryNav fragment after this record's ops would be popped
     */
    Fragment trackAddedFragmentsInPop(ArrayList<Fragment> added, Fragment oldPrimaryNav) {
        for (int opNum = mOps.size() - 1; opNum >= 0; opNum--) {
            final Op op = mOps.get(opNum);
            switch (op.mCmd) {
                case OP_ADD, OP_ATTACH -> added.remove(op.mFragment);
                case OP_REMOVE, OP_DETACH -> added.add(op.mFragment);
                case OP_UNSET_PRIMARY_NAV -> oldPrimaryNav = op.mFragment;
                case OP_SET_PRIMARY_NAV -> oldPrimaryNav = null;
                case OP_SET_MAX_LIFECYCLE -> op.mCurrentMaxState = op.mOldMaxState;
            }
        }
        return oldPrimaryNav;
    }

    /**
     * Removes any Ops expanded by {@link #expandOps(ArrayList, Fragment)},
     * reverting them back to their collapsed form.
     */
    void collapseOps() {
        for (int opNum = mOps.size() - 1; opNum >= 0; opNum--) {
            final Op op = mOps.get(opNum);
            if (!op.mFromExpandedOp) {
                continue;
            }
            if (op.mCmd == OP_SET_PRIMARY_NAV) {
                // OP_SET_PRIMARY_NAV is always expanded to two ops:
                // 1. The OP_SET_PRIMARY_NAV we want to keep
                op.mFromExpandedOp = false;
                // And the OP_UNSET_PRIMARY_NAV we want to remove
                mOps.remove(opNum - 1);
                opNum--;
            } else {
                // Handle the collapse of an OP_REPLACE, which could start
                // with either an OP_ADD (the usual case) or an OP_REMOVE
                // (if the dev explicitly called add() earlier in the transaction)
                int containerId = op.mFragment.mContainerId;
                // Swap this expanded op with a replace
                op.mCmd = OP_REPLACE;
                op.mFromExpandedOp = false;
                // And remove all other expanded ops with the same containerId
                for (int replaceOpNum = opNum - 1; replaceOpNum >= 0; replaceOpNum--) {
                    final Op potentialReplaceOp = mOps.get(replaceOpNum);
                    if (potentialReplaceOp.mFromExpandedOp
                            && potentialReplaceOp.mFragment.mContainerId == containerId) {
                        mOps.remove(replaceOpNum);
                        opNum--;
                    }
                }
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return mOps.isEmpty();
    }
}
