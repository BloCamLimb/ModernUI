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

public final class OpaqueType extends Type {

    private final Type mComponentType;
    private final int mDimensions;
    private final boolean mIsShadow;
    private final boolean mIsArrayed;
    private final boolean mIsMultisampled;
    private final boolean mIsSampled;
    private final boolean mIsSampler;

    OpaqueType(String name, String desc, Type componentType, int dimensions,
               boolean isShadow, boolean isArrayed, boolean isMultisampled,
               boolean isSampled, boolean isSampler) {
        super(name, desc, TYPE_KIND_OPAQUE);
        mComponentType = componentType;
        mDimensions = dimensions;
        mIsArrayed = isArrayed;
        mIsMultisampled = isMultisampled;
        mIsSampled = isSampled;
        mIsSampler = isSampler;
        mIsShadow = isShadow;
    }

    @Nonnull
    @Override
    public Type getComponentType() {
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
        return mIsSampled && mIsSampler;
    }

    @Override
    public boolean isPureSampler() {
        return !mIsSampled && mIsSampler;
    }
}
