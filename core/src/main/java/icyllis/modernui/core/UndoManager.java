/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.core;

import icyllis.modernui.text.TextUtils;
import icyllis.modernui.util.ArrayMap;
import icyllis.modernui.util.BinaryIO;

import java.io.*;
import java.util.ArrayList;
import java.util.Objects;

public class UndoManager {
    // The common case is a single undo owner (e.g. for a TextView), so default to that capacity.
    private final ArrayMap<String, UndoOwner> mOwners =
            new ArrayMap<>(1 /* capacity */);
    private final ArrayList<UndoState> mUndos = new ArrayList<>();
    private final ArrayList<UndoState> mRedos = new ArrayList<>();
    private int mUpdateCount;
    private int mHistorySize = 20;
    private UndoState mWorking;
    private int mCommitId = 1;
    private boolean mInUndo;
    private boolean mMerged;

    private int mStateSeq;
    private int mNextSavedIdx;
    private UndoOwner[] mStateOwners;

    /**
     * Never merge with the last undo state.
     */
    public static final int MERGE_MODE_NONE = 0;

    /**
     * Allow merge with the last undo state only if it contains
     * operations with the caller's owner.
     */
    public static final int MERGE_MODE_UNIQUE = 1;

    /**
     * Always allow merge with the last undo state, if possible.
     */
    public static final int MERGE_MODE_ANY = 2;

    public UndoManager() {
    }

    public UndoOwner getOwner(String tag, Object data) {
        if (tag == null) {
            throw new NullPointerException("tag can't be null");
        }
        if (data == null) {
            throw new NullPointerException("data can't be null");
        }
        UndoOwner owner = mOwners.get(tag);
        if (owner != null) {
            if (owner.mData != data) {
                if (owner.mData != null) {
                    throw new IllegalStateException("Owner " + owner + " already exists with data "
                            + owner.mData + " but giving different data " + data);
                }
                owner.mData = data;
            }
            return owner;
        }

        owner = new UndoOwner(tag, this);
        owner.mData = data;
        mOwners.put(tag, owner);
        return owner;
    }

    void removeOwner(UndoOwner owner) {
        // XXX need to figure out how to prune.
        if (false) {
            mOwners.remove(owner.mTag);
        }
    }

    /**
     * Flatten the current undo state into a Parcel object, which can later be restored
     * with {@link #restoreInstanceState(java.io.DataInput, java.lang.ClassLoader)}.
     */
    public void saveInstanceState(DataOutput p) throws IOException {
        if (mUpdateCount > 0) {
            throw new IllegalStateException("Can't save state while updating");
        }
        mStateSeq++;
        if (mStateSeq <= 0) {
            mStateSeq = 0;
        }
        mNextSavedIdx = 0;
        p.writeInt(mHistorySize);
        p.writeInt(mOwners.size());
        // XXX eventually we need to be smart here about limiting the
        // number of undo states we write to not exceed X bytes.
        int i = mUndos.size();
        while (i > 0) {
            p.writeInt(1);
            i--;
            mUndos.get(i).writeToParcel(p);
        }
        i = mRedos.size();
        while (i > 0) {
            p.writeInt(2);
            i--;
            mRedos.get(i).writeToParcel(p);
        }
        p.writeInt(0);
    }

    void saveOwner(UndoOwner owner, DataOutput out) throws IOException {
        if (owner.mStateSeq == mStateSeq) {
            out.writeInt(owner.mSavedIdx);
        } else {
            owner.mStateSeq = mStateSeq;
            owner.mSavedIdx = mNextSavedIdx;
            out.writeInt(owner.mSavedIdx);
            BinaryIO.writeString(out, owner.mTag);
            out.writeInt(owner.mOpCount);
            mNextSavedIdx++;
        }
    }

    /**
     * Restore an undo state previously created with {@link #saveInstanceState(DataOutput)}.  This
     * will restore the UndoManager's state to almost exactly what it was at the point it had
     * been previously saved; the only information not restored is the data object
     * associated with each {@link UndoOwner}, which requires separate calls to
     * {@link #getOwner(String, Object)} to re-associate the owner with its data.
     */
    public void restoreInstanceState(DataInput p, ClassLoader loader) throws IOException {
        if (mUpdateCount > 0) {
            throw new IllegalStateException("Can't save state while updating");
        }
        forgetUndos(null, -1);
        forgetRedos(null, -1);
        mHistorySize = p.readInt();
        mStateOwners = new UndoOwner[p.readInt()];

        int stype;
        while ((stype = p.readInt()) != 0) {
            UndoState ustate = new UndoState(this, p, loader);
            if (stype == 1) {
                mUndos.add(0, ustate);
            } else {
                mRedos.add(0, ustate);
            }
        }
    }

    UndoOwner restoreOwner(DataInput in) throws IOException {
        int idx = in.readInt();
        UndoOwner owner = mStateOwners[idx];
        if (owner == null) {
            String tag = BinaryIO.readString(in);
            int opCount = in.readInt();
            owner = new UndoOwner(tag, this);
            owner.mOpCount = opCount;
            mStateOwners[idx] = owner;
            mOwners.put(tag, owner);
        }
        return owner;
    }

    /**
     * Set the maximum number of undo states that will be retained.
     */
    public void setHistorySize(int size) {
        mHistorySize = size;
        if (mHistorySize >= 0 && countUndos(null) > mHistorySize) {
            forgetUndos(null, countUndos(null) - mHistorySize);
        }
    }

    /**
     * Return the current maximum number of undo states.
     */
    public int getHistorySize() {
        return mHistorySize;
    }

    /**
     * Perform undo of last/top <var>count</var> undo states.  The states impacted
     * by this can be limited through <var>owners</var>.
     *
     * @param owners Optional set of owners that should be impacted.  If null, all
     *               undo states will be visible and available for undo.  If non-null, only those
     *               states that contain one of the owners specified here will be visible.
     * @param count  Number of undo states to pop.
     * @return Returns the number of undo states that were actually popped.
     */
    public int undo(UndoOwner[] owners, int count) {
        if (mWorking != null) {
            throw new IllegalStateException("Can't be called during an update");
        }

        int num = 0;
        int i = -1;

        mInUndo = true;

        UndoState us = getTopUndo(null);
        if (us != null) {
            us.makeExecuted();
        }

        while (count > 0 && (i = findPrevState(mUndos, owners, i)) >= 0) {
            UndoState state = mUndos.remove(i);
            state.undo();
            mRedos.add(state);
            count--;
            num++;
        }

        mInUndo = false;

        return num;
    }

    /**
     * Perform redo of last/top <var>count</var> undo states in the transient redo stack.
     * The states impacted by this can be limited through <var>owners</var>.
     *
     * @param owners Optional set of owners that should be impacted.  If null, all
     *               undo states will be visible and available for undo.  If non-null, only those
     *               states that contain one of the owners specified here will be visible.
     * @param count  Number of undo states to pop.
     * @return Returns the number of undo states that were actually redone.
     */
    public int redo(UndoOwner[] owners, int count) {
        if (mWorking != null) {
            throw new IllegalStateException("Can't be called during an update");
        }

        int num = 0;
        int i = -1;

        mInUndo = true;

        while (count > 0 && (i = findPrevState(mRedos, owners, i)) >= 0) {
            UndoState state = mRedos.remove(i);
            state.redo();
            mUndos.add(state);
            count--;
            num++;
        }

        mInUndo = false;

        return num;
    }

    /**
     * Returns true if we are currently inside of an undo/redo operation.  This is
     * useful for editors to know whether they should be generating new undo state
     * when they see edit operations happening.
     */
    public boolean isInUndo() {
        return mInUndo;
    }

    public int forgetUndos(UndoOwner[] owners, int count) {
        if (count < 0) {
            count = mUndos.size();
        }

        int removed = 0;
        int i = 0;
        while (i < mUndos.size() && removed < count) {
            UndoState state = mUndos.get(i);
            if (count > 0 && matchOwners(state, owners)) {
                state.destroy();
                mUndos.remove(i);
                removed++;
            } else {
                i++;
            }
        }

        return removed;
    }

    public int forgetRedos(UndoOwner[] owners, int count) {
        if (count < 0) {
            count = mRedos.size();
        }

        int removed = 0;
        int i = 0;
        while (i < mRedos.size() && removed < count) {
            UndoState state = mRedos.get(i);
            if (count > 0 && matchOwners(state, owners)) {
                state.destroy();
                mRedos.remove(i);
                removed++;
            } else {
                i++;
            }
        }

        return removed;
    }

    /**
     * Return the number of undo states on the undo stack.
     *
     * @param owners If non-null, only those states containing an operation with one of
     *               the owners supplied here will be counted.
     */
    public int countUndos(UndoOwner[] owners) {
        if (owners == null) {
            return mUndos.size();
        }

        int count = 0;
        int i = 0;
        while ((i = findNextState(mUndos, owners, i)) >= 0) {
            count++;
            i++;
        }
        return count;
    }

    /**
     * Return the number of redo states on the undo stack.
     *
     * @param owners If non-null, only those states containing an operation with one of
     *               the owners supplied here will be counted.
     */
    public int countRedos(UndoOwner[] owners) {
        if (owners == null) {
            return mRedos.size();
        }

        int count = 0;
        int i = 0;
        while ((i = findNextState(mRedos, owners, i)) >= 0) {
            count++;
            i++;
        }
        return count;
    }

    /**
     * Return the user-visible label for the top undo state on the stack.
     *
     * @param owners If non-null, will select the top-most undo state containing an
     *               operation with one of the owners supplied here.
     */
    public CharSequence getUndoLabel(UndoOwner[] owners) {
        UndoState state = getTopUndo(owners);
        return state != null ? state.getLabel() : null;
    }

    /**
     * Return the user-visible label for the top redo state on the stack.
     *
     * @param owners If non-null, will select the top-most undo state containing an
     *               operation with one of the owners supplied here.
     */
    public CharSequence getRedoLabel(UndoOwner[] owners) {
        UndoState state = getTopRedo(owners);
        return state != null ? state.getLabel() : null;
    }

    /**
     * Start creating a new undo state.  Multiple calls to this function will nest until
     * they are all matched by a later call to {@link #endUpdate}.
     *
     * @param label Optional user-visible label for this new undo state.
     */
    public void beginUpdate(CharSequence label) {
        if (mInUndo) {
            throw new IllegalStateException("Can't being update while performing undo/redo");
        }
        if (mUpdateCount <= 0) {
            createWorkingState();
            mMerged = false;
            mUpdateCount = 0;
        }

        mWorking.updateLabel(label);
        mUpdateCount++;
    }

    private void createWorkingState() {
        mWorking = new UndoState(this, mCommitId++);
        if (mCommitId < 0) {
            mCommitId = 1;
        }
    }

    /**
     * Returns true if currently inside of a {@link #beginUpdate}.
     */
    public boolean isInUpdate() {
        return mUpdateCount > 0;
    }

    /**
     * Forcibly set a new for the new undo state being built within a {@link #beginUpdate}.
     * Any existing label will be replaced with this one.
     */
    public void setUndoLabel(CharSequence label) {
        if (mWorking == null) {
            throw new IllegalStateException("Must be called during an update");
        }
        mWorking.setLabel(label);
    }

    /**
     * Set a new for the new undo state being built within a {@link #beginUpdate}, but
     * only if there is not a label currently set for it.
     */
    public void suggestUndoLabel(CharSequence label) {
        if (mWorking == null) {
            throw new IllegalStateException("Must be called during an update");
        }
        mWorking.updateLabel(label);
    }

    /**
     * Return the number of times {@link #beginUpdate} has been called without a matching
     * {@link #endUpdate} call.
     */
    public int getUpdateNestingLevel() {
        return mUpdateCount;
    }

    /**
     * Check whether there is an {@link UndoOperation} in the current {@link #beginUpdate}
     * undo state.
     *
     * @param owner Optional owner of the operation to look for.  If null, will succeed
     *              if there is any operation; if non-null, will only succeed if there is an operation
     *              with the given owner.
     * @return Returns true if there is a matching operation in the current undo state.
     */
    public boolean hasOperation(UndoOwner owner) {
        if (mWorking == null) {
            throw new IllegalStateException("Must be called during an update");
        }
        return mWorking.hasOperation(owner);
    }

    /**
     * Return the most recent {@link UndoOperation} that was added to the update.
     *
     * @param mergeMode May be either {@link #MERGE_MODE_NONE} or {@link #MERGE_MODE_ANY}.
     */
    public UndoOperation<?> getLastOperation(int mergeMode) {
        return getLastOperation(null, null, mergeMode);
    }

    /**
     * Return the most recent {@link UndoOperation} that was added to the update and
     * has the given owner.
     *
     * @param owner     Optional owner of last operation to retrieve.  If null, the last
     *                  operation regardless of owner will be retrieved; if non-null, the last operation
     *                  matching the given owner will be retrieved.
     * @param mergeMode May be either {@link #MERGE_MODE_NONE}, {@link #MERGE_MODE_UNIQUE},
     *                  or {@link #MERGE_MODE_ANY}.
     */
    public UndoOperation<?> getLastOperation(UndoOwner owner, int mergeMode) {
        return getLastOperation(null, owner, mergeMode);
    }

    /**
     * Return the most recent {@link UndoOperation} that was added to the update and
     * has the given owner.
     *
     * @param clazz     Optional class of the last operation to retrieve.  If null, the
     *                  last operation regardless of class will be retrieved; if non-null, the last
     *                  operation whose class is the same as the given class will be retrieved.
     * @param owner     Optional owner of last operation to retrieve.  If null, the last
     *                  operation regardless of owner will be retrieved; if non-null, the last operation
     *                  matching the given owner will be retrieved.
     * @param mergeMode May be either {@link #MERGE_MODE_NONE}, {@link #MERGE_MODE_UNIQUE},
     *                  or {@link #MERGE_MODE_ANY}.
     */
    public <T extends UndoOperation<?>> T getLastOperation(Class<T> clazz, UndoOwner owner,
                                                           int mergeMode) {
        if (mWorking == null) {
            throw new IllegalStateException("Must be called during an update");
        }
        if (mergeMode != MERGE_MODE_NONE && !mMerged && !mWorking.hasData()) {
            UndoState state = getTopUndo(null);
            UndoOperation<?> last;
            if (state != null && (mergeMode == MERGE_MODE_ANY || !state.hasMultipleOwners())
                    && state.canMerge() && (last = state.getLastOperation(clazz, owner)) != null) {
                if (last.allowMerge()) {
                    mWorking.destroy();
                    mWorking = state;
                    mUndos.remove(state);
                    mMerged = true;
                    return (T) last;
                }
            }
        }

        return mWorking.getLastOperation(clazz, owner);
    }

    /**
     * Add a new UndoOperation to the current update.
     *
     * @param op        The new operation to add.
     * @param mergeMode May be either {@link #MERGE_MODE_NONE}, {@link #MERGE_MODE_UNIQUE},
     *                  or {@link #MERGE_MODE_ANY}.
     */
    public void addOperation(UndoOperation<?> op, int mergeMode) {
        if (mWorking == null) {
            throw new IllegalStateException("Must be called during an update");
        }
        UndoOwner owner = op.getOwner();
        if (owner.mManager != this) {
            throw new IllegalArgumentException(
                    "Given operation's owner is not in this undo manager.");
        }
        if (mergeMode != MERGE_MODE_NONE && !mMerged && !mWorking.hasData()) {
            UndoState state = getTopUndo(null);
            if (state != null && (mergeMode == MERGE_MODE_ANY || !state.hasMultipleOwners())
                    && state.canMerge() && state.hasOperation(op.getOwner())) {
                mWorking.destroy();
                mWorking = state;
                mUndos.remove(state);
                mMerged = true;
            }
        }
        mWorking.addOperation(op);
    }

    /**
     * Finish the creation of an undo state, matching a previous call to
     * {@link #beginUpdate}.
     */
    public void endUpdate() {
        if (mWorking == null) {
            throw new IllegalStateException("Must be called during an update");
        }
        mUpdateCount--;

        if (mUpdateCount == 0) {
            pushWorkingState();
        }
    }

    private void pushWorkingState() {
        int N = mUndos.size() + 1;

        if (mWorking.hasData()) {
            mUndos.add(mWorking);
            forgetRedos(null, -1);
            mWorking.commit();
            if (N >= 2) {
                // The state before this one can no longer be merged, ever.
                // The only way to get back to it is for the user to perform
                // an undo.
                mUndos.get(N - 2).makeExecuted();
            }
        } else {
            mWorking.destroy();
        }
        mWorking = null;

        if (mHistorySize >= 0 && N > mHistorySize) {
            forgetUndos(null, N - mHistorySize);
        }
    }

    /**
     * Commit the last finished undo state.  This undo state can no longer be
     * modified with further {@link #MERGE_MODE_UNIQUE} or
     * {@link #MERGE_MODE_ANY} merge modes.  If called while inside of an update,
     * this will push any changes in the current update on to the undo stack
     * and result with a fresh undo state, behaving as if {@link #endUpdate()}
     * had been called enough to unwind the current update, then the last state
     * committed, and {@link #beginUpdate} called to restore the update nesting.
     *
     * @param owner The optional owner to determine whether to perform the commit.
     *              If this is non-null, the commit will only execute if the current top undo
     *              state contains an operation with the given owner.
     * @return Returns an integer identifier for the committed undo state, which
     * can later be used to try to uncommit the state to perform further edits on it.
     */
    public int commitState(UndoOwner owner) {
        if (mWorking != null && mWorking.hasData()) {
            if (owner == null || mWorking.hasOperation(owner)) {
                mWorking.setCanMerge(false);
                int commitId = mWorking.getCommitId();
                pushWorkingState();
                createWorkingState();
                mMerged = true;
                return commitId;
            }
        } else {
            UndoState state = getTopUndo(null);
            if (state != null && (owner == null || state.hasOperation(owner))) {
                state.setCanMerge(false);
                return state.getCommitId();
            }
        }
        return -1;
    }

    /**
     * Attempt to undo a previous call to {@link #commitState}.  This will work
     * if the undo state at the top of the stack has the given id, and has not been
     * involved in an undo operation.  Otherwise false is returned.
     *
     * @param commitId The identifier for the state to be uncommitted, as returned
     *                 by {@link #commitState}.
     * @param owner    Optional owner that must appear in the committed state.
     * @return Returns true if the uncommit is successful, else false.
     */
    public boolean uncommitState(int commitId, UndoOwner owner) {
        if (mWorking != null && mWorking.getCommitId() == commitId) {
            if (owner == null || mWorking.hasOperation(owner)) {
                return mWorking.setCanMerge(true);
            }
        } else {
            UndoState state = getTopUndo(null);
            if (state != null && (owner == null || state.hasOperation(owner))) {
                if (state.getCommitId() == commitId) {
                    return state.setCanMerge(true);
                }
            }
        }
        return false;
    }

    UndoState getTopUndo(UndoOwner[] owners) {
        if (mUndos.size() <= 0) {
            return null;
        }
        int i = findPrevState(mUndos, owners, -1);
        return i >= 0 ? mUndos.get(i) : null;
    }

    UndoState getTopRedo(UndoOwner[] owners) {
        if (mRedos.size() <= 0) {
            return null;
        }
        int i = findPrevState(mRedos, owners, -1);
        return i >= 0 ? mRedos.get(i) : null;
    }

    boolean matchOwners(UndoState state, UndoOwner[] owners) {
        if (owners == null) {
            return true;
        }
        for (int i = 0; i < owners.length; i++) {
            if (state.matchOwner(owners[i])) {
                return true;
            }
        }
        return false;
    }

    int findPrevState(ArrayList<UndoState> states, UndoOwner[] owners, int from) {
        final int N = states.size();

        if (from == -1) {
            from = N - 1;
        }
        if (from >= N) {
            return -1;
        }
        if (owners == null) {
            return from;
        }

        while (from >= 0) {
            UndoState state = states.get(from);
            if (matchOwners(state, owners)) {
                return from;
            }
            from--;
        }

        return -1;
    }

    int findNextState(ArrayList<UndoState> states, UndoOwner[] owners, int from) {
        final int N = states.size();

        if (from < 0) {
            from = 0;
        }
        if (from >= N) {
            return -1;
        }
        if (owners == null) {
            return from;
        }

        while (from < N) {
            UndoState state = states.get(from);
            if (matchOwners(state, owners)) {
                return from;
            }
            from++;
        }

        return -1;
    }

    final static class UndoState {
        private final UndoManager mManager;
        private final int mCommitId;
        private final ArrayList<UndoOperation<?>> mOperations = new ArrayList<>();
        private ArrayList<UndoOperation<?>> mRecent;
        private CharSequence mLabel;
        private boolean mCanMerge = true;
        private boolean mExecuted;

        UndoState(UndoManager manager, int commitId) {
            mManager = manager;
            mCommitId = commitId;
        }

        UndoState(UndoManager manager, DataInput p, ClassLoader loader) throws IOException {
            mManager = manager;
            mCommitId = p.readInt();
            mCanMerge = p.readInt() != 0;
            mExecuted = p.readInt() != 0;
            mLabel = TextUtils.read(p);
            final int N = p.readInt();
            for (int i = 0; i < N; i++) {
                UndoOwner owner = mManager.restoreOwner(p);
                UndoOperation<?> op = BinaryIO.readParcelable(p, loader, UndoOperation.class);
                Objects.requireNonNull(op).mOwner = owner;
                mOperations.add(op);
            }
        }

        void writeToParcel(DataOutput p) throws IOException {
            if (mRecent != null) {
                throw new IllegalStateException("Can't save state before committing");
            }
            p.writeInt(mCommitId);
            p.writeInt(mCanMerge ? 1 : 0);
            p.writeInt(mExecuted ? 1 : 0);
            TextUtils.write(p, mLabel);
            p.writeInt(mOperations.size());
            for (UndoOperation<?> op : mOperations) {
                mManager.saveOwner(op.mOwner, p);
                op.write(p);
            }
        }

        int getCommitId() {
            return mCommitId;
        }

        void setLabel(CharSequence label) {
            mLabel = label;
        }

        void updateLabel(CharSequence label) {
            if (mLabel != null) {
                mLabel = label;
            }
        }

        CharSequence getLabel() {
            return mLabel;
        }

        boolean setCanMerge(boolean state) {
            // Don't allow re-enabling of merging if state has been executed.
            if (state && mExecuted) {
                return false;
            }
            mCanMerge = state;
            return true;
        }

        void makeExecuted() {
            mExecuted = true;
        }

        boolean canMerge() {
            return mCanMerge && !mExecuted;
        }

        int countOperations() {
            return mOperations.size();
        }

        boolean hasOperation(UndoOwner owner) {
            if (owner == null) {
                return !mOperations.isEmpty();
            }
            for (UndoOperation<?> operation : mOperations) {
                if (operation.getOwner() == owner) {
                    return true;
                }
            }
            return false;
        }

        boolean hasMultipleOwners() {
            final int N = mOperations.size();
            if (N <= 1) {
                return false;
            }
            UndoOwner owner = mOperations.get(0).getOwner();
            for (int i = 1; i < N; i++) {
                if (mOperations.get(i).getOwner() != owner) {
                    return true;
                }
            }
            return false;
        }

        void addOperation(UndoOperation<?> op) {
            if (mOperations.contains(op)) {
                throw new IllegalStateException("Already holds " + op);
            }
            mOperations.add(op);
            if (mRecent == null) {
                mRecent = new ArrayList<>();
                mRecent.add(op);
            }
            op.mOwner.mOpCount++;
        }

        <T extends UndoOperation<?>> T getLastOperation(Class<T> clazz, UndoOwner owner) {
            final int N = mOperations.size();
            if (clazz == null && owner == null) {
                return N > 0 ? (T) mOperations.get(N - 1) : null;
            }
            // First look for the top-most operation with the same owner.
            for (int i = N - 1; i >= 0; i--) {
                UndoOperation<?> op = mOperations.get(i);
                if (owner != null && op.getOwner() != owner) {
                    continue;
                }
                // Return this operation if it has the same class that the caller wants.
                // Note that we don't search deeper for the class, because we don't want
                // to end up with a different order of operations for the same owner.
                if (clazz != null && op.getClass() != clazz) {
                    return null;
                }
                return (T) op;
            }

            return null;
        }

        boolean matchOwner(UndoOwner owner) {
            for (int i = mOperations.size() - 1; i >= 0; i--) {
                if (mOperations.get(i).matchOwner(owner)) {
                    return true;
                }
            }
            return false;
        }

        boolean hasData() {
            for (int i = mOperations.size() - 1; i >= 0; i--) {
                if (mOperations.get(i).hasData()) {
                    return true;
                }
            }
            return false;
        }

        void commit() {
            final int N = mRecent != null ? mRecent.size() : 0;
            for (int i = 0; i < N; i++) {
                mRecent.get(i).commit();
            }
            mRecent = null;
        }

        void undo() {
            for (int i = mOperations.size() - 1; i >= 0; i--) {
                mOperations.get(i).undo();
            }
        }

        void redo() {
            final int N = mOperations.size();
            for (int i = 0; i < N; i++) {
                mOperations.get(i).redo();
            }
        }

        void destroy() {
            for (int i = mOperations.size() - 1; i >= 0; i--) {
                UndoOwner owner = mOperations.get(i).mOwner;
                owner.mOpCount--;
                if (owner.mOpCount <= 0) {
                    if (owner.mOpCount < 0) {
                        throw new IllegalStateException("Underflow of op count on owner " + owner
                                + " in op " + mOperations.get(i));
                    }
                    mManager.removeOwner(owner);
                }
            }
        }
    }
}
