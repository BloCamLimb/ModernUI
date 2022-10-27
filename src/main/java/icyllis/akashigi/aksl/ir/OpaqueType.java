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

package icyllis.akashigi.aksl.ir;

import javax.annotation.Nonnull;

public final class OpaqueType extends Type {

    private final ScalarType mComponentType;
    private final int mDimensions;
    private final boolean mIsShadow;
    private final boolean mIsArrayed;
    private final boolean mIsMultisampled;
    private final boolean mIsSampled;
    private final boolean mHasSampler;

    OpaqueType(String name, Type componentType, int dimensions, boolean isShadow,
               boolean isArrayed, boolean isMultisampled, boolean isSampled,
               boolean hasSampler) {
        super(name, "T", TYPE_KIND_OPAQUE); //TODO detailed abbrev
        mComponentType = (ScalarType) componentType;
        mDimensions = dimensions;
        mIsArrayed = isArrayed;
        mIsMultisampled = isMultisampled;
        mIsSampled = isSampled;
        mHasSampler = hasSampler;
        mIsShadow = isShadow;
    }

    @Nonnull
    @Override
    public ScalarType componentType() {
        return mComponentType;
    }

    @Override
    public int dimensions() {
        return mDimensions;
    }

    @Override
    public boolean isShadow() {
        return mIsShadow;
    }

    @Override
    public boolean isArrayed() {
        return mIsArrayed;
    }

    @Override
    public boolean isMultisampled() {
        return mIsMultisampled;
    }

    @Override
    public boolean isSampled() {
        return mIsSampled;
    }

    @Override
    public boolean isCombinedSampler() {
        return mIsSampled && mHasSampler;
    }

    @Override
    public boolean isPureSampler() {
        return !mIsSampled && mHasSampler;
    }
}
