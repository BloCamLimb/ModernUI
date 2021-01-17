/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.forge.mixin;

import com.mojang.math.Matrix4f;
import icyllis.modernui.forge.extension.IMixinMatrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Matrix4f.class)
public class MixinMatrix4f implements IMixinMatrix4f {

    @Shadow
    protected float m00;

    @Shadow
    protected float m01;

    @Shadow
    protected float m02;

    @Shadow
    protected float m03;

    @Shadow
    protected float m10;

    @Shadow
    protected float m11;

    @Shadow
    protected float m12;

    @Shadow
    protected float m13;

    @Shadow
    protected float m20;

    @Shadow
    protected float m21;

    @Shadow
    protected float m22;

    @Shadow
    protected float m23;

    @Shadow
    protected float m30;

    @Shadow
    protected float m31;

    @Shadow
    protected float m32;

    @Shadow
    protected float m33;

    @Override
    public boolean isIdentity() {
        return m00 == 1.0f && m01 == 0.0f && m02 == 0.0f && m03 == 0.0f &&
                m10 == 0.0f && m11 == 1.0f && m12 == 0.0f && m13 == 0.0f &&
                m20 == 0.0f && m21 == 0.0f && m22 == 1.0f && m23 == 0.0f &&
                m30 == 0.0f && m31 == 0.0f && m32 == 0.0f && m33 == 1.0f;
    }
}
