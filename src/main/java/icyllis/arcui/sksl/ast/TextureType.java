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

package icyllis.arcui.sksl.ast;

public final class TextureType extends Type {

    private final int mDimensions;
    private final boolean mIsDepth;
    private final boolean mIsLayered;
    private final boolean mIsMultisampled;
    private final boolean mIsSampled;

    TextureType(String name, int dimensions, boolean isDepth, boolean isLayered,
                boolean isMultisampled, boolean isSampled) {
        super(name, "T", KIND_TEXTURE);
        mDimensions = dimensions;
        mIsDepth = isDepth;
        mIsLayered = isLayered;
        mIsMultisampled = isMultisampled;
        mIsSampled = isSampled;
    }

    @Override
    public int getDimensions() {
        return mDimensions;
    }

    @Override
    public boolean isDepth() {
        return mIsDepth;
    }

    @Override
    public boolean isLayered() {
        return mIsLayered;
    }

    @Override
    public boolean isMultisampled() {
        return mIsMultisampled;
    }

    @Override
    public boolean isSampled() {
        return mIsSampled;
    }
}
