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

package icyllis.akashigi.engine;

import icyllis.akashigi.core.SLType;

/**
 * Represents a variable in a shader.
 */
public class ShaderVar {

    /**
     * TypeModifiers
     */
    public static final byte
            TypeModifier_None = 0,
            TypeModifier_Out = 1,
            TypeModifier_In = 2,
            TypeModifier_InOut = 3,
            TypeModifier_Uniform = 4;

    /**
     * Values for array count that have special meaning. We allow 1-sized arrays.
     */
    public static final int NonArray = 0; // not an array

    private byte mType;
    private byte mTypeModifier;
    private final int mCount;

    private String mName;
    private String mLayoutQualifier;
    private String mExtraModifiers;

    /**
     * Defaults to a void with no type modifier or layout qualifier.
     */
    public ShaderVar() {
        this("", SLType.kVoid, TypeModifier_None, NonArray, "", "");
    }

    public ShaderVar(String name, byte type) {
        this(name, type, TypeModifier_None, NonArray, "", "");
    }

    public ShaderVar(String name, byte type, int arrayCount) {
        this(name, type, TypeModifier_None, arrayCount, "", "");
    }

    public ShaderVar(String name, byte type, byte typeModifier) {
        this(name, type, typeModifier, NonArray, "", "");
    }

    public ShaderVar(String name, byte type, byte typeModifier, int arrayCount) {
        this(name, type, typeModifier, arrayCount, "", "");
    }

    public ShaderVar(String name, byte type, byte typeModifier, int arrayCount,
                     String layoutQualifier, String extraModifier) {
        assert (name != null);
        assert (SLType.checkSLType(type));
        assert (typeModifier >= TypeModifier_None && typeModifier <= TypeModifier_Uniform);
        assert (arrayCount == NonArray || arrayCount > 0);
        assert (layoutQualifier != null && extraModifier != null);
        mType = type;
        mTypeModifier = typeModifier;
        mCount = arrayCount;
        mName = name;
        mLayoutQualifier = layoutQualifier;
        mExtraModifiers = extraModifier;
    }

    /**
     * Sets as a non-array. Internally used with the default constructor.
     */
    public void set(String name, byte type) {
        assert (type != SLType.kVoid);
        mType = type;
        mName = name;
    }

    /**
     * Is the var an array.
     */
    public boolean isArray() {
        return mCount != NonArray;
    }

    /**
     * Get the array length. May be {@link #NonArray}.
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
        assert (typeModifier >= TypeModifier_None && typeModifier <= TypeModifier_Uniform);
        mTypeModifier = typeModifier;
    }

    /**
     * Appends to the layout qualifier.
     */
    public void addLayoutQualifier(String layoutQualifier) {
        assert (layoutQualifier != null && !layoutQualifier.isEmpty());
        if (mLayoutQualifier.isEmpty()) {
            mLayoutQualifier = layoutQualifier;
        } else {
            mLayoutQualifier += ", " + layoutQualifier;
        }
    }

    /**
     * Appends to the modifiers.
     */
    public void addModifier(String modifier) {
        assert (modifier != null && !modifier.isEmpty());
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
        if (mTypeModifier != TypeModifier_None) {
            out.append(switch (mTypeModifier) {
                case TypeModifier_Out -> "out ";
                case TypeModifier_In -> "in ";
                case TypeModifier_InOut -> "inout ";
                case TypeModifier_Uniform -> "uniform ";
                default -> throw new IllegalStateException();
            });
        }
        byte type = getType();
        if (isArray()) {
            assert (getArrayCount() > 0);
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
