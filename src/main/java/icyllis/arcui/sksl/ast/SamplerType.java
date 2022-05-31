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

import javax.annotation.Nonnull;

public final class SamplerType extends Type {

    private final TextureType mTextureType;

    SamplerType(String name, Type textureType) {
        super(name, "Z", KIND_SAMPLER);
        mTextureType = (TextureType) textureType;
    }

    @Nonnull
    @Override
    public TextureType getTextureType() {
        return mTextureType;
    }

    @Override
    public int getDimensions() {
        return mTextureType.getDimensions();
    }

    @Override
    public boolean isDepth() {
        return mTextureType.isDepth();
    }

    @Override
    public boolean isLayered() {
        return mTextureType.isLayered();
    }

    @Override
    public boolean isMultisampled() {
        return mTextureType.isMultisampled();
    }

    @Override
    public boolean isSampled() {
        return mTextureType.isSampled();
    }
}
