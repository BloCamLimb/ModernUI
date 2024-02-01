/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.compiler.tree;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * A fully-resolved intermediate representation of a program (shader stage), ready for code generation.
 */
public final class TranslationUnit implements Iterable<TopLevelElement> {

    private final ArrayList<TopLevelElement> mUniqueElements;
    private final ArrayList<TopLevelElement> mSharedElements;

    public TranslationUnit(ArrayList<TopLevelElement> uniqueElements,
                           ArrayList<TopLevelElement> sharedElements) {
        mUniqueElements = uniqueElements;
        mSharedElements = sharedElements;
    }

    @Nonnull
    @Override
    public Iterator<TopLevelElement> iterator() {
        return new ElementIterator();
    }

    // shared first, then unique
    private class ElementIterator implements Iterator<TopLevelElement> {

        private Iterator<TopLevelElement> mCurrIter = mSharedElements.iterator();
        private boolean mSharedEnded = false;

        @Override
        public boolean hasNext() {
            forward();
            return mCurrIter.hasNext();
        }

        @Override
        public TopLevelElement next() {
            forward();
            return mCurrIter.next();
        }

        private void forward() {
            while (!mCurrIter.hasNext() && !mSharedEnded) {
                mCurrIter = mUniqueElements.iterator();
                mSharedEnded = true;
            }
        }
    }
}
