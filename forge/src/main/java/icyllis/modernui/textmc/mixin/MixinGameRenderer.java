/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.textmc.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import icyllis.modernui.textmc.TextLayoutEngine;
import icyllis.modernui.textmc.TextRenderType;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Transition if we are rendering 3D world or 2D.
 */
@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void renderLevelStart(float partialTick, long frameTimeNanos, PoseStack pStack, CallbackInfo ci) {
        TextRenderType.sUseDistanceField = TextLayoutEngine.sUseDistanceField;
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void renderLevelEnd(float partialTick, long frameTimeNanos, PoseStack pStack, CallbackInfo ci) {
        TextRenderType.sUseDistanceField = false;
    }
}
