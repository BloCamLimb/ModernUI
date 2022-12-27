/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.slang.ir;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.List;

/**
 * A fully-resolved intermediate representation of a program (shader stage), ready for code generation.
 */
public class Program implements Iterable<Element> {

    private final List<Element> mUniqueElements;
    private final List<Element> mSharedElements;

    public Program(List<Element> uniqueElements,
                   List<Element> sharedElements) {
        mUniqueElements = uniqueElements;
        mSharedElements = sharedElements;
    }

    @Nonnull
    @Override
    public Iterator<Element> iterator() {
        return new ElementIterator();
    }

    // shared first, then owned
    private class ElementIterator implements Iterator<Element> {

        private Iterator<Element> mCurrIter = mSharedElements.iterator();
        private boolean mSharedEnded = false;

        @Override
        public boolean hasNext() {
            forward();
            return mCurrIter.hasNext();
        }

        @Override
        public Element next() {
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
