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

import icyllis.arc3d.compiler.*;
import icyllis.arc3d.compiler.analysis.SymbolUsage;
import icyllis.arc3d.compiler.analysis.TreeVisitor;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * A fully-resolved AST of a single shader module, ready for code generation.
 */
public final class TranslationUnit extends Node implements Iterable<TopLevelElement> {

    private final char[] mSource;

    private final ShaderKind mKind;
    private final CompileOptions mOptions;
    private final boolean mIsBuiltin;
    private final boolean mIsModule;

    private final BuiltinTypes mTypes;

    private final SymbolTable mSymbolTable;

    private final ArrayList<TopLevelElement> mUniqueElements;
    private final ArrayList<TopLevelElement> mSharedElements;

    private final SymbolUsage mUsage;

    public TranslationUnit(int position,
                           char[] source,
                           ShaderKind kind,
                           CompileOptions options,
                           boolean isBuiltin,
                           boolean isModule,
                           BuiltinTypes types,
                           SymbolTable symbolTable,
                           ArrayList<TopLevelElement> uniqueElements) {
        super(position);
        mSource = source;
        mKind = kind;
        mOptions = options;
        mIsBuiltin = isBuiltin;
        mIsModule = isModule;
        mTypes = types;
        mSymbolTable = symbolTable;
        mUniqueElements = uniqueElements;
        mSharedElements = new ArrayList<>();
        mUsage = new SymbolUsage();
        mUsage.add(this);
    }

    public char[] getSource() {
        return mSource;
    }

    public ShaderKind getKind() {
        return mKind;
    }

    public CompileOptions getOptions() {
        return mOptions;
    }

    public boolean isBuiltin() {
        return mIsBuiltin;
    }

    public boolean isModule() {
        return mIsModule;
    }

    public BuiltinTypes getTypes() {
        return mTypes;
    }

    public SymbolTable getSymbolTable() {
        return mSymbolTable;
    }

    public ArrayList<TopLevelElement> getUniqueElements() {
        return mUniqueElements;
    }

    public ArrayList<TopLevelElement> getSharedElements() {
        return mSharedElements;
    }

    public SymbolUsage getUsage() {
        return mUsage;
    }

    @Nonnull
    @Override
    public Iterator<TopLevelElement> iterator() {
        return new ElementIterator();
    }

    @Override
    public boolean accept(@Nonnull TreeVisitor visitor) {
        for (TopLevelElement e : this) {
            if (e.accept(visitor)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (TopLevelElement e : this) {
            s.append(e.toString());
            s.append('\n');
        }
        return s.toString();
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
