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

// modified from Android
public class UndoOwner {
    final String mTag;
    final UndoManager mManager;

    Object mData;
    int mOpCount;

    // For saving/restoring state.
    int mStateSeq;
    int mSavedIdx;

    UndoOwner(String tag, UndoManager manager) {
        if (tag == null) {
            throw new NullPointerException("tag can't be null");
        }
        if (manager == null) {
            throw new NullPointerException("manager can't be null");
        }
        mTag = tag;
        mManager = manager;
    }

    /**
     * Return the unique tag name identifying this owner.  This is the tag
     * supplied to {@link UndoManager#getOwner(String, Object) UndoManager.getOwner}
     * and is immutable.
     */
    public String getTag() {
        return mTag;
    }

    /**
     * Return the actual data object of the owner.  This is the data object
     * supplied to {@link UndoManager#getOwner(String, Object) UndoManager.getOwner}.  An
     * owner may have a null data if it was restored from a previously saved state with
     * no getOwner call to associate it with its data.
     */
    public Object getData() {
        return mData;
    }

    @Override
    public String toString() {
        return "UndoOwner:[mTag=" + mTag +
                " mManager=" + mManager +
                " mData=" + mData +
                " mData=" + mData +
                " mOpCount=" + mOpCount +
                " mStateSeq=" + mStateSeq +
                " mSavedIdx=" + mSavedIdx + "]";
    }
}
