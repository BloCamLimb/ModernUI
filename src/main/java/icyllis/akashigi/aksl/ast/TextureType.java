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

package icyllis.akashigi.aksl.ast;

public final class TextureType extends Type {

    private final int mDimensions;
    private final boolean mIsDepth;
    private final boolean mIsArrayed;
    private final boolean mIsMultisampled;
    private final boolean mIsSampled;

    TextureType(String name, int dimensions, boolean isDepth, boolean isArrayed,
                boolean isMultisampled, boolean isSampled) {
        super(name, "T", TYPE_KIND_TEXTURE);
        mDimensions = dimensions;
        mIsDepth = isDepth;
        mIsArrayed = isArrayed;
        mIsMultisampled = isMultisampled;
        mIsSampled = isSampled;
    }

    @Override
    public int dimensions() {
        return mDimensions;
    }

    @Override
    public boolean isDepthTexture() {
        return mIsDepth;
    }

    @Override
    public boolean isArrayedTexture() {
        return mIsArrayed;
    }

    @Override
    public boolean isMultisampledTexture() {
        return mIsMultisampled;
    }

    @Override
    public boolean isSampledTexture() {
        return mIsSampled;
    }
}
