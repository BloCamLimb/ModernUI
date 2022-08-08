/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.engine;

import icyllis.arcui.core.SLType;

/**
 * Represents a variable in a shader
 */
public class ShaderVar {

    public static final byte
            TYPE_MODIFIER_NONE = 0,
            TYPE_MODIFIER_OUT = 1,
            TYPE_MODIFIER_IN = 2,
            TYPE_MODIFIER_IN_OUT = 3,
            TYPE_MODIFIER_UNIFORM = 4;

    /**
     * Values for array count that have special meaning. We allow 1-sized arrays.
     */
    public static final int
            NON_ARRAY = 0; // not an array

    private byte mType;
    private byte mTypeModifier;
    private int mCount;

    private String mName;
    private String mLayoutQualifier;
    private String mExtraModifiers;

    public ShaderVar(String name, byte type) {
        this(name, type, TYPE_MODIFIER_NONE, NON_ARRAY);
    }

    public ShaderVar(String name, byte type, int arrayCount) {
        this(name, type, TYPE_MODIFIER_NONE, arrayCount);
    }

    public ShaderVar(String name, byte type, byte typeModifier) {
        this(name, type, typeModifier, NON_ARRAY);
    }

    public ShaderVar(String name, byte type, byte typeModifier, int arrayCount) {
        this(name, type, typeModifier, arrayCount, "", "");
    }

    public ShaderVar(String name, byte type, byte typeModifier, int arrayCount,
                     String layoutQualifier, String extraModifier) {
        mType = type;
        mTypeModifier = typeModifier;
        mCount = arrayCount;
        mName = name;
        mLayoutQualifier = layoutQualifier;
        mExtraModifiers = extraModifier;
    }

    /**
     * Is the var an array.
     */
    public boolean isArray() {
        return mCount != NON_ARRAY;
    }

    /**
     * Get the array length.
     */
    public int getArrayCount() {
        return mCount;
    }

    /**
     * Get the name.
     */
    public String getName() {
        return mName;
    }

    /**
     * Get the type.
     *
     * @see SLType
     */
    public byte getType() {
        return mType;
    }

    public byte getTypeModifier() {
        return mTypeModifier;
    }

    public void setTypeModifier(byte typeModifier) {
        mTypeModifier = typeModifier;
    }

    /**
     * Appends to the layout qualifier.
     */
    public void addLayoutQualifier(String layoutQualifier) {
        if (layoutQualifier == null || layoutQualifier.isEmpty()) {
            return;
        }
        if (mLayoutQualifier.isEmpty()) {
            mLayoutQualifier = layoutQualifier;
        } else {
            mLayoutQualifier += ", " + layoutQualifier;
        }
    }

    public void addModifier(String modifier) {
        if (modifier == null || modifier.isEmpty()) {
            return;
        }
        if (mExtraModifiers.isEmpty()) {
            mExtraModifiers = modifier;
        } else {
            mExtraModifiers += " " + modifier;
        }
    }

    /**
     * Write a declaration of this variable to out.
     */
    public void appendDecl(StringBuilder out) {
        if (!mLayoutQualifier.isEmpty()) {
            out.append("layout(");
            out.append(mLayoutQualifier);
            out.append(") ");
        }
        if (!mExtraModifiers.isEmpty()) {
            out.append(mExtraModifiers);
            out.append(" ");
        }
        if (mTypeModifier != TYPE_MODIFIER_NONE) {
            out.append(switch (mTypeModifier) {
                case TYPE_MODIFIER_OUT -> "out";
                case TYPE_MODIFIER_IN -> "in";
                case TYPE_MODIFIER_IN_OUT -> "inout";
                case TYPE_MODIFIER_UNIFORM -> "uniform";
                default -> "";
            });
            out.append(" ");
        }
        byte type = getType();
        if (isArray()) {
            assert getArrayCount() > 0;
            out.append(SLType.typeString(type));
            out.append(" ");
            out.append(getName());
            out.append("[");
            out.append(getArrayCount());
            out.append("]");
        } else {
            out.append(SLType.typeString(type));
            out.append(" ");
            out.append(getName());
        }
    }
}
