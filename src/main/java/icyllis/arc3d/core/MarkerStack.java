/*
 * Arc 3D.
 * Copyright (C) 2022-2023 BloCamLimb. All rights reserved.
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.core;

import javax.annotation.Nullable;
import java.util.ArrayList;

public final class MarkerStack {

    private final ArrayList<Rec> mStack = new ArrayList<>();

    public MarkerStack() {
    }

    public void setMarker(int id, Matrix4 mat, int boundary) {
        for (int i = mStack.size() - 1; i >= 0; i--) {
            Rec it = mStack.get(i);
            if (it.mBoundary != boundary) {
                break;
            }
            if (it.mID == id) {
                it.setMatrix(mat);
                return;
            }
        }
        mStack.add(new Rec(id, mat, boundary));
    }

    public boolean findMarker(int id, Matrix4 out) {
        Matrix4 mat = findMarker(id);
        if (mat != null) {
            out.set(mat);
            return true;
        }
        return false;
    }

    @Nullable
    public Matrix4 findMarker(int id) {
        for (int i = mStack.size() - 1; i >= 0; i--) {
            Rec it = mStack.get(i);
            if (it.mID == id) {
                return it.mMatrix;
            }
        }
        return null;
    }

    public boolean findMarkerInverse(int id, Matrix4 out) {
        Matrix4 mat = findMarkerInverse(id);
        if (mat != null) {
            out.set(mat);
            return true;
        }
        return false;
    }

    @Nullable
    public Matrix4 findMarkerInverse(int id) {
        for (int i = mStack.size() - 1; i >= 0; i--) {
            Rec it = mStack.get(i);
            if (it.mID == id) {
                return it.mMatrixInverse;
            }
        }
        return null;
    }

    public void restore(int boundary) {
        for (int i = mStack.size() - 1; i >= 0; i--) {
            Rec it = mStack.get(i);
            if (it.mBoundary == boundary) {
                mStack.remove(i);
            } else {
                break;
            }
        }
    }

    private static final class Rec {

        int mID;
        final Matrix4 mMatrix = new Matrix4();
        final Matrix4 mMatrixInverse = new Matrix4();
        int mBoundary;

        Rec(int id, Matrix4 mat, int boundary) {
            mID = id;
            setMatrix(mat);
            mBoundary = boundary;
        }

        void setMatrix(Matrix4 mat) {
            mMatrix.set(mat);
            if (!mat.invert(mMatrixInverse)) {
                throw new IllegalStateException();
            }
        }
    }
}
