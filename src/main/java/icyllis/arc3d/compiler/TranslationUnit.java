/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.compiler;

import icyllis.arc3d.compiler.analysis.SymbolUsage;
import icyllis.arc3d.compiler.analysis.TreeVisitor;
import icyllis.arc3d.compiler.tree.*;
import org.jspecify.annotations.NonNull;

import java.util.*;

/**
 * A fully-resolved AST of a single shader executable, ready for code generation.
 */
public final class TranslationUnit extends Node implements Iterable<TopLevelElement> {

    private final String mSource;

    private final ShaderKind mKind;
    private final CompileOptions mOptions;

    private final BuiltinTypes mTypes;

    private final SymbolTable mSymbolTable;

    private final ArrayList<TopLevelElement> mUniqueElements;
    private final ArrayList<TopLevelElement> mSharedElements;

    private final List<Map.Entry<String, String>> mExtensions;

    private final SymbolUsage mUsage;

    public TranslationUnit(String source,
                           ShaderKind kind,
                           CompileOptions options,
                           BuiltinTypes types,
                           SymbolTable symbolTable,
                           ArrayList<TopLevelElement> uniqueElements,
                           List<Map.Entry<String, String>> extensions) {
        super(Position.NO_POS);
        mSource = source;
        mKind = kind;
        mOptions = options;
        mTypes = types;
        mSymbolTable = symbolTable;
        mUniqueElements = uniqueElements;
        mExtensions = extensions;
        mSharedElements = new ArrayList<>();
        mUsage = new SymbolUsage();
        mUsage.add(this);
    }

    public String getSource() {
        return mSource;
    }

    public ShaderKind getKind() {
        return mKind;
    }

    public CompileOptions getOptions() {
        return mOptions;
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

    /**
     * A list of (extension_name, behavior) pairs. This list is mutable,
     * you can add or remove elements.
     */
    public List<Map.Entry<String, String>> getExtensions() {
        return mExtensions;
    }

    public SymbolUsage getUsage() {
        return mUsage;
    }

    @NonNull
    @Override
    public Iterator<TopLevelElement> iterator() {
        class ElementIterator implements Iterator<TopLevelElement> {
            // shared first, then unique
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
        return new ElementIterator();
    }

    @Override
    public boolean accept(@NonNull TreeVisitor visitor) {
        for (TopLevelElement e : this) {
            if (e.accept(visitor)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (TopLevelElement e : this) {
            s.append(e.toString());
            s.append('\n');
        }
        return s.toString();
    }
}
