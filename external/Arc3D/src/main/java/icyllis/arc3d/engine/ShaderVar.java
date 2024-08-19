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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.SLDataType;

/**
 * Represents a variable in a shader.
 */
public class ShaderVar {

    /**
     * TypeModifiers
     */
    public static final byte
            kNone_TypeModifier = 0,
            kOut_TypeModifier = 1,
            kIn_TypeModifier = 2,
            kInOut_TypeModifier = 3,
            kUniform_TypeModifier = 4;

    /**
     * Values for array length that have special meaning. We allow 1-sized arrays.
     */
    public static final int kNonArray = 0; // not an array

    private byte mType;
    private byte mTypeModifier;
    private final int mArraySize;

    private String mName;
    private String mLayoutQualifier;
    private String mExtraModifiers;

    /**
     * Defaults to a void with no type modifier or layout qualifier.
     */
    public ShaderVar() {
        this("", SLDataType.kVoid, kNone_TypeModifier, kNonArray, "", "");
    }

    public ShaderVar(String name, byte type) {
        this(name, type, kNone_TypeModifier, kNonArray, "", "");
    }

    public ShaderVar(String name, byte type, int arraySize) {
        this(name, type, kNone_TypeModifier, arraySize, "", "");
    }

    public ShaderVar(String name, byte type, byte typeModifier) {
        this(name, type, typeModifier, kNonArray, "", "");
    }

    public ShaderVar(String name, byte type, byte typeModifier, int arraySize) {
        this(name, type, typeModifier, arraySize, "", "");
    }

    public ShaderVar(String name, byte type, byte typeModifier, int arraySize,
                     String layoutQualifier, String extraModifier) {
        assert (name != null);
        assert (SLDataType.checkSLType(type));
        assert (typeModifier >= kNone_TypeModifier && typeModifier <= kUniform_TypeModifier);
        assert (arraySize == kNonArray || arraySize > 0);
        assert (layoutQualifier != null && extraModifier != null);
        mType = type;
        mTypeModifier = typeModifier;
        mArraySize = arraySize;
        mName = name;
        mLayoutQualifier = layoutQualifier;
        mExtraModifiers = extraModifier;
    }

    /**
     * Sets as a non-array. Internally used with the default constructor.
     */
    public void set(String name, byte type) {
        assert (type != SLDataType.kVoid);
        mType = type;
        mName = name;
    }

    /**
     * Is the var an array.
     */
    public boolean isArray() {
        return mArraySize != kNonArray;
    }

    /**
     * Get the array length. May be {@link #kNonArray}.
     */
    public int getArraySize() {
        return mArraySize;
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
     * @see SLDataType
     */
    public byte getType() {
        return mType;
    }

    public byte getTypeModifier() {
        return mTypeModifier;
    }

    public void setTypeModifier(byte typeModifier) {
        assert (typeModifier >= kNone_TypeModifier && typeModifier <= kUniform_TypeModifier);
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
            mLayoutQualifier += ',' + layoutQualifier;
        }
    }

    /**
     * Appends to the layout qualifier.
     */
    public void addLayoutQualifier(String qualifier, int value) {
        addLayoutQualifier(qualifier + '=' + value);
    }

    /**
     * Appends to the modifiers.
     */
    public void addModifier(String modifier) {
        assert (modifier != null && !modifier.isEmpty());
        if (mExtraModifiers.isEmpty()) {
            mExtraModifiers = modifier;
        } else {
            mExtraModifiers += ' ' + modifier;
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
        if (mTypeModifier != kNone_TypeModifier) {
            out.append(switch (mTypeModifier) {
                case kOut_TypeModifier -> "out ";
                case kIn_TypeModifier -> "in ";
                case kInOut_TypeModifier -> "inout ";
                case kUniform_TypeModifier -> "uniform ";
                default -> throw new IllegalStateException();
            });
        }
        byte type = getType();
        if (isArray()) {
            assert (getArraySize() > 0);
            out.append(SLDataType.typeString(type));
            out.append(" ");
            out.append(getName());
            out.append("[");
            out.append(getArraySize());
            out.append("]");
        } else {
            out.append(SLDataType.typeString(type));
            out.append(" ");
            out.append(getName());
        }
    }
}
